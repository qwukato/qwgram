package app.qwgram.next.ui

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.qwgram.next.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import java.io.File

/**
 * Telegram video stickers / emoji are WebM files with two VP9 streams: the picture, and its transparency stored
 * as a second VP9 stream in each block's "BlockAdditional". Android's decoder knows VP9 but not that alpha layout,
 * so we read the WebM ourselves, decode both streams with MediaCodec and join them into transparent frames.
 */
class WebmAnim(val frames: List<ImageBitmap>, val durationsMs: IntArray) {
    val bytes: Int = frames.sumOf { it.width * it.height * 4 }
}

private object WebmCache {
    // decoded animations, by file + size; ~48 MB of frames at most
    val cache = object : LruCache<String, WebmAnim>(48 * 1024 * 1024) { override fun sizeOf(key: String, value: WebmAnim) = value.bytes }
    val gate = Semaphore(2)
    val failed = HashSet<String>()
}

private class WebmData(val width: Int, val height: Int, val frames: List<Pair<ByteArray, ByteArray?>>, val timesMs: LongArray)

/** minimal EBML/WebM reader: video size, then every block of the first video track (+ its alpha) with its time */
private fun parseWebm(bytes: ByteArray): WebmData? {
    var w = 0; var h = 0
    var videoTrack = 1L
    val frames = ArrayList<Pair<ByteArray, ByteArray?>>()
    val times = ArrayList<Long>()
    var clusterTime = 0L
    var scaleNs = 1_000_000L

    fun vint(pos: Int, keepMarker: Boolean): Pair<Long, Int>? {
        if (pos >= bytes.size) return null
        val first = bytes[pos].toInt() and 0xFF
        if (first == 0) return null
        var len = 1
        while (len <= 8 && first and (0x80 ushr (len - 1)) == 0) len++
        if (len > 8 || pos + len > bytes.size) return null
        var v = (if (keepMarker) first else first and (0xFF ushr len)).toLong()
        for (i in 1 until len) v = (v shl 8) or (bytes[pos + i].toLong() and 0xFF)
        // all-ones size = unknown
        if (!keepMarker && v == (1L shl (7 * len)) - 1) v = -1
        return v to len
    }
    fun uint(pos: Int, size: Int): Long { var v = 0L; for (i in 0 until size) v = (v shl 8) or (bytes[pos + i].toLong() and 0xFF); return v }

    var pendingBlock: ByteArray? = null
    var pendingAlpha: ByteArray? = null
    var pendingTime = 0L

    fun blockFrame(pos: Int, size: Int): Pair<Long, ByteArray>? {
        val (track, tl) = vint(pos, false) ?: return null
        if (track != videoTrack) return null
        val rel = ((bytes[pos + tl].toInt() shl 8) or (bytes[pos + tl + 1].toInt() and 0xFF)).toShort().toLong()
        val flags = bytes[pos + tl + 2].toInt()
        if (flags and 0x06 != 0) return null // laced blocks: not used for video stickers
        val start = pos + tl + 3
        return (clusterTime + rel) to bytes.copyOfRange(start, pos + size)
    }

    fun walk(start: Int, end: Int) {
        var p = start
        while (p < end) {
            val (id, il) = vint(p, true) ?: return
            val (sz0, sl) = vint(p + il, false) ?: return
            val dataStart = p + il + sl
            val size = if (sz0 < 0) end - dataStart else sz0.toInt().coerceAtMost(end - dataStart)
            when (id) {
                0x18538067L, 0x1F43B675L, 0x1654AE6BL, 0x75A1L, 0xA6L -> walk(dataStart, dataStart + size) // Segment, Cluster, Tracks, BlockAdditions, BlockMore
                0xAEL -> { // TrackEntry: remember the video track number and size
                    var tn = 0L; var type = 0L
                    var q = dataStart
                    while (q < dataStart + size) {
                        val (cid, cil) = vint(q, true) ?: break
                        val (csz, csl) = vint(q + cil, false) ?: break
                        val cs = q + cil + csl
                        when (cid) {
                            0xD7L -> tn = uint(cs, csz.toInt())
                            0x83L -> type = uint(cs, csz.toInt())
                            0xE0L -> { // Video
                                var r = cs
                                while (r < cs + csz) {
                                    val (vid, vil) = vint(r, true) ?: break
                                    val (vsz, vsl) = vint(r + vil, false) ?: break
                                    val vs = r + vil + vsl
                                    if (vid == 0xB0L) w = uint(vs, vsz.toInt()).toInt()
                                    if (vid == 0xBAL) h = uint(vs, vsz.toInt()).toInt()
                                    r = vs + vsz.toInt()
                                }
                            }
                        }
                        q = cs + csz.toInt()
                    }
                    if (type == 1L && tn > 0) videoTrack = tn
                }
                0x2AD7B1L -> scaleNs = uint(dataStart, size)                // TimecodeScale
                0xE7L -> clusterTime = uint(dataStart, size)                 // cluster Timecode
                0xA3L -> blockFrame(dataStart, size)?.let { (t, f) -> frames.add(f to null); times.add(t) } // SimpleBlock
                0xA0L -> { // BlockGroup: Block + optional alpha
                    pendingBlock = null; pendingAlpha = null
                    walk(dataStart, dataStart + size)
                    pendingBlock?.let { frames.add(it to pendingAlpha); times.add(pendingTime) }
                }
                0xA1L -> blockFrame(dataStart, size)?.let { (t, f) -> pendingBlock = f; pendingTime = t }
                0xA5L -> pendingAlpha = bytes.copyOfRange(dataStart, dataStart + size) // BlockAdditional (alpha)
            }
            if (size < 0) return
            p = dataStart + size
        }
    }
    runCatching { walk(0, bytes.size) }
    if (frames.isEmpty() || w <= 0 || h <= 0) return null
    val ms = LongArray(times.size) { times[it] * scaleNs / 1_000_000L }
    return WebmData(w, h, frames, ms)
}

/** decodes a VP9 stream; for every shown frame calls [onFrame] with the decoder's Image */
private fun decodeVp9(w: Int, h: Int, packets: List<ByteArray>, onFrame: (android.media.Image) -> Unit) {
    val codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_VP9)
    try {
        val fmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_VP9, w, h)
        fmt.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        codec.configure(fmt, null, null, 0)
        codec.start()
        val info = MediaCodec.BufferInfo()
        var inIdx = 0
        var eosSent = false
        var done = false
        var guard = 0
        while (!done && guard++ < 100_000) {
            if (!eosSent) {
                val ib = codec.dequeueInputBuffer(5_000)
                if (ib >= 0) {
                    if (inIdx < packets.size) {
                        val buf = codec.getInputBuffer(ib)!!
                        buf.clear(); buf.put(packets[inIdx])
                        codec.queueInputBuffer(ib, 0, packets[inIdx].size, inIdx * 33_000L, 0)
                        inIdx++
                    } else {
                        codec.queueInputBuffer(ib, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        eosSent = true
                    }
                }
            }
            val ob = codec.dequeueOutputBuffer(info, 5_000)
            if (ob >= 0) {
                if (info.size > 0) codec.getOutputImage(ob)?.let { img -> img.use(onFrame) }
                codec.releaseOutputBuffer(ob, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) done = true
            }
        }
    } finally {
        runCatching { codec.stop() }
        codec.release()
    }
}

private fun decodeWebm(path: String, target: Int): WebmAnim? {
    val data = parseWebm(File(path).readBytes()) ?: return null
    val n = data.frames.size
    // long animations keep every other frame (memory), with doubled durations
    val step = if (n > 60) 2 else 1
    val outW = minOf(target, data.width); val outH = (outW.toLong() * data.height / data.width).toInt().coerceAtLeast(1)
    val colors = ArrayList<IntArray>(n / step + 1)
    var idx = 0
    decodeVp9(data.width, data.height, data.frames.map { it.first }) { img ->
        if (idx++ % step != 0) return@decodeVp9
        val y = img.planes[0]; val u = img.planes[1]; val v = img.planes[2]
        val iw = img.width; val ih = img.height
        val px = IntArray(outW * outH)
        for (oy in 0 until outH) {
            val sy = oy * ih / outH
            for (ox in 0 until outW) {
                val sx = ox * iw / outW
                val yy = (y.buffer.get(sy * y.rowStride + sx * y.pixelStride).toInt() and 0xFF) - 16
                val uu = (u.buffer.get((sy / 2) * u.rowStride + (sx / 2) * u.pixelStride).toInt() and 0xFF) - 128
                val vv = (v.buffer.get((sy / 2) * v.rowStride + (sx / 2) * v.pixelStride).toInt() and 0xFF) - 128
                val c = 1192 * yy
                val r = ((c + 1634 * vv) shr 10).coerceIn(0, 255)
                val g = ((c - 833 * vv - 400 * uu) shr 10).coerceIn(0, 255)
                val b = ((c + 2066 * uu) shr 10).coerceIn(0, 255)
                px[oy * outW + ox] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        colors.add(px)
    }
    if (colors.isEmpty()) return null
    // the alpha stream: its luma is the transparency
    val hasAlpha = data.frames.all { it.second != null }
    if (hasAlpha) {
        var ai = 0; var kept = 0
        runCatching {
            decodeVp9(data.width, data.height, data.frames.map { it.second!! }) { img ->
                if (ai++ % step != 0) return@decodeVp9
                val px = colors.getOrNull(kept++) ?: return@decodeVp9
                val y = img.planes[0]; val iw = img.width; val ih = img.height
                for (oy in 0 until outH) {
                    val sy = oy * ih / outH
                    for (ox in 0 until outW) {
                        val sx = ox * iw / outW
                        val a = (((y.buffer.get(sy * y.rowStride + sx * y.pixelStride).toInt() and 0xFF) - 16) * 1192 shr 10).coerceIn(0, 255)
                        val i = oy * outW + ox
                        px[i] = (a shl 24) or (px[i] and 0xFFFFFF)
                    }
                }
            }
        }
    }
    val frames = colors.map { Bitmap.createBitmap(it, outW, outH, Bitmap.Config.ARGB_8888).asImageBitmap() }
    val durs = IntArray(frames.size) { i ->
        val a = data.timesMs.getOrElse(i * step) { 0L }
        val b = data.timesMs.getOrElse((i + 1) * step) { a + 33L * step }
        (b - a).toInt().coerceIn(16, 200)
    }
    return WebmAnim(frames, durs)
}

/** a looping WebM (video) sticker / emoji with transparency; [placeholder] until it is decoded */
@Composable
fun WebmImage(file: TdApi.File, modifier: Modifier = Modifier, placeholder: @Composable () -> Unit = {}) {
    val path = rememberLocalPath(file)
    BoxWithConstraints(modifier) {
        val side = (if (constraints.hasBoundedWidth) constraints.maxWidth else 256).coerceIn(48, 384)
        val target = when { side <= 96 -> 96; side <= 192 -> 192; side <= 288 -> 288; else -> 384 }
        val key = "$path@$target"
        var anim by remember(key) { mutableStateOf(if (path != null) WebmCache.cache.get(key) else null) }
        LaunchedEffect(key) {
            if (path == null || anim != null || key in WebmCache.failed) return@LaunchedEffect
            anim = withContext(Dispatchers.Default) {
                WebmCache.gate.withPermit {
                    WebmCache.cache.get(key) ?: runCatching { decodeWebm(path, target) }.getOrNull()?.also { WebmCache.cache.put(key, it) }
                }
            }
            if (anim == null) WebmCache.failed.add(key)
        }
        val a = anim
        if (a == null || a.frames.isEmpty()) { Box(Modifier.fillMaxSize()) { placeholder() }; return@BoxWithConstraints }
        var frame by remember(a) { mutableIntStateOf(0) }
        LaunchedEffect(a, Prefs.motion.value) {
            if (Prefs.motion.value == "off" || a.frames.size < 2) return@LaunchedEffect
            var acc = 0L
            var last = -1L
            while (true) {
                withFrameNanos { t ->
                    if (last >= 0) acc += (t - last) / 1_000_000L
                    last = t
                    var f = frame
                    while (acc >= a.durationsMs[f]) { acc -= a.durationsMs[f]; f = (f + 1) % a.frames.size }
                    if (f != frame) frame = f
                }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val img = a.frames[frame.coerceIn(0, a.frames.lastIndex)]
            // fit, centred
            val s = minOf(size.width / img.width, size.height / img.height)
            val dw = (img.width * s).toInt(); val dh = (img.height * s).toInt()
            drawImage(img, srcOffset = IntOffset.Zero, srcSize = IntSize(img.width, img.height),
                dstOffset = IntOffset(((size.width - dw) / 2).toInt(), ((size.height - dh) / 2).toInt()), dstSize = IntSize(dw, dh),
                filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium)
        }
    }
}

