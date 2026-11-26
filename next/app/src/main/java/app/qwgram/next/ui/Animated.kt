package app.qwgram.next.ui

import androidx.compose.ui.graphics.asImageBitmap
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import app.qwgram.next.Prefs
import app.qwgram.next.td.Images
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.rememberFileBitmap
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.zip.GZIPInputStream

/** TGS files are gzipped Lottie JSON: unpacked once and kept (small) */
private object TgsCache {
    val json = LruCache<String, String>(96)
    fun load(path: String): String? = json.get(path) ?: runCatching {
        GZIPInputStream(File(path).inputStream().buffered()).use { it.readBytes().toString(Charsets.UTF_8) }
    }.getOrNull()?.also { json.put(path, it) }
}

private val stillCache = object : android.util.LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(24 * 1024 * 1024) {
    override fun sizeOf(key: String, value: androidx.compose.ui.graphics.ImageBitmap) = value.width * value.height * 4
}

/**
 * A sticker as a still picture (first frame), decoded once off the main thread and cached. Far cheaper than an
 * animated sticker: for grids with many items (gifts) and for patterns drawn many times.
 */
@Composable
fun rememberStickerStill(s: TdApi.Sticker?, px: Int): androidx.compose.ui.graphics.ImageBitmap? {
    if (s == null) return null
    val path = rememberLocalPath(s.sticker)
    val key = "${s.sticker.id}@$px"
    var img by remember(key) { mutableStateOf(stillCache.get(key)) }
    LaunchedEffect(key, path) {
        if (img != null || path == null) return@LaunchedEffect
        img = withContext(Dispatchers.Default) {
            stillCache.get(key) ?: stickerBitmap(s, px)?.let { b ->
                val sc = px.toFloat() / maxOf(b.width, b.height)
                (if (sc < 1f) android.graphics.Bitmap.createScaledBitmap(b, (b.width * sc).toInt().coerceAtLeast(1), (b.height * sc).toInt().coerceAtLeast(1), true) else b).asImageBitmap()
            }?.also { stillCache.put(key, it) }
        }
    }
    return img
}

/** a still picture of any sticker (for drawing it into an edited photo / video); null if not downloaded yet */
fun stickerBitmap(s: TdApi.Sticker, px: Int): android.graphics.Bitmap? = runCatching {
    val f = TdStore.files[s.sticker.id] ?: s.sticker
    val path = f.local?.path?.takeIf { f.local.isDownloadingCompleted }
    when (s.format) {
        is TdApi.StickerFormatTgs -> {
            val json = path?.let { TgsCache.load(it) } ?: return@runCatching null
            val comp = com.airbnb.lottie.LottieCompositionFactory.fromJsonStringSync(json, "tgs:$path").value ?: return@runCatching null
            val d = com.airbnb.lottie.LottieDrawable()
            d.composition = comp
            d.progress = 0f
            val out = android.graphics.Bitmap.createBitmap(px, px, android.graphics.Bitmap.Config.ARGB_8888)
            d.setBounds(0, 0, px, px)
            d.draw(android.graphics.Canvas(out))
            out
        }
        is TdApi.StickerFormatWebp -> path?.let { android.graphics.BitmapFactory.decodeFile(it) }
        else -> {
            val t = s.thumbnail?.file?.let { TdStore.files[it.id] ?: it }
            t?.local?.path?.takeIf { t.local.isDownloadingCompleted }?.let { android.graphics.BitmapFactory.decodeFile(it) }
        }
    }
}.getOrNull()

/** the local path of a Telegram file once it is downloaded (starts the download) */
@Composable
fun rememberLocalPath(file: TdApi.File?): String? {
    if (file == null) return null
    val live = TdStore.files[file.id] ?: file
    LaunchedEffect(file.id) { Images.download(live) }
    return if (live.local.isDownloadingCompleted) live.local.path else null
}

/** an animated (TGS / Lottie) sticker, emoji or status, looping; nothing until it is ready */
@Composable
fun TgsImage(file: TdApi.File, modifier: Modifier = Modifier, loop: Boolean = true, placeholder: @Composable () -> Unit = {}) {
    val path = rememberLocalPath(file)
    var json by remember(path) { mutableStateOf(path?.let { TgsCache.json.get(it) }) }
    LaunchedEffect(path) { if (path != null && json == null) json = withContext(Dispatchers.IO) { TgsCache.load(path) } }
    val j = json
    if (j == null) { Box(modifier) { placeholder() }; return }
    val comp by rememberLottieComposition(LottieCompositionSpec.JsonString(j), cacheKey = "tgs:$path")
    if (comp == null) { Box(modifier) { placeholder() }; return }
    // energy saving: stickers can play once or stay still (Settings → Energy saving)
    val ps = if (app.qwgram.next.Power.on) Prefs.psStickers.value else "loop"
    LottieAnimation(
        comp, modifier = modifier,
        iterations = if (loop && ps == "loop") LottieConstants.IterateForever else 1,
        isPlaying = Prefs.motion.value != "off" && ps != "still",
        contentScale = ContentScale.Fit,
        enableMergePaths = true,
    )
}

/**
 * Any Telegram sticker / custom emoji: animated TGS plays, static WebP shows the picture; video (WebM) stickers
 * show their preview frame (playing transparent VP9 video needs a decoder Android doesn't ship).
 */
@Composable
fun StickerContent(s: TdApi.Sticker, modifier: Modifier = Modifier, placeholder: @Composable () -> Unit = {}) {
    when (s.format) {
        is TdApi.StickerFormatTgs -> {
            val thumb = rememberFileBitmap(s.thumbnail?.file, maxSide = 256)
            TgsImage(s.sticker, modifier) { if (thumb != null) Image(thumb, s.emoji, modifier, contentScale = ContentScale.Fit) else placeholder() }
        }
        // video stickers / emoji: decoded with their transparency (see Webm.kt); the preview frame until ready
        is TdApi.StickerFormatWebm -> {
            val thumb = rememberFileBitmap(s.thumbnail?.file, maxSide = 256)
            WebmImage(s.sticker, modifier) { if (thumb != null) Image(thumb, s.emoji, modifier, contentScale = ContentScale.Fit) else placeholder() }
        }
        else -> {
            val static = s.format is TdApi.StickerFormatWebp
            val bmp = rememberFileBitmap(if (static) s.sticker else (s.thumbnail?.file ?: s.sticker), maxSide = 512)
            if (bmp != null) Image(bmp, s.emoji, modifier, contentScale = ContentScale.Fit) else Box(modifier) { placeholder() }
        }
    }
}
