package app.qwgram.next.ui

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.RotateLeft
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.Crop
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import java.io.File

object VideoEditor {
    var request by mutableStateOf<Pair<Uri, (Uri) -> Unit>?>(null)
    fun open(uri: Uri, onDone: (Uri) -> Unit) { request = uri to onDone }
}

/**
 * Video editor matching the photo one (M3 Expressive, Telegram-like): trim with a range slider, frame
 * (rotate, crop to 1:1 / 4:3 / 16:9 / 9:16), colour (brightness, contrast, saturation — previewed live on the
 * playing video) and sound on/off. "Готово" exports a new MP4 with Media3 Transformer.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoEditorHost() {
    val req = VideoEditor.request ?: return
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var tool by remember(req) { mutableIntStateOf(0) }       // 0 trim, 1 frame, 2 colour
    var durMs by remember(req) { mutableLongStateOf(0L) }
    var trim by remember(req) { mutableStateOf(0f..1f) }
    var rotation by remember(req) { mutableIntStateOf(0) }
    var aspect by remember(req) { mutableFloatStateOf(0f) }
    var bright by remember(req) { mutableFloatStateOf(0f) }
    var contrast by remember(req) { mutableFloatStateOf(0f) }
    var sat by remember(req) { mutableFloatStateOf(0f) }
    var muted by remember(req) { mutableStateOf(false) }
    var exporting by remember { mutableStateOf<Float?>(null) }
    var videoW by remember(req) { mutableIntStateOf(1) }
    var videoH by remember(req) { mutableIntStateOf(1) }
    fun close() { VideoEditor.request = null }

    fun effects(): List<Effect> = buildList {
        if (rotation != 0) add(ScaleAndRotateTransformation.Builder().setRotationDegrees(rotation.toFloat()).build())
        if (aspect > 0f) {
            // centre crop in normalised device coordinates (-1…1), after rotation
            val (w, h) = if (rotation % 180 == 0) videoW.toFloat() to videoH.toFloat() else videoH.toFloat() to videoW.toFloat()
            val cur = w / h
            if (cur > aspect) { val k = aspect / cur; add(Crop(-k, k, -1f, 1f)) } else { val k = cur / aspect; add(Crop(-1f, 1f, -k, k)) }
        }
        if (bright != 0f) add(Brightness(bright * 0.5f))
        if (contrast != 0f) add(Contrast(contrast * 0.6f))
        if (sat != 0f) add(HslAdjustment.Builder().adjustSaturation(sat * 100f).build())
    }

    val player = remember(req) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(req.first))
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
            playWhenReady = true
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                    if (videoSize.width > 0) { videoW = videoSize.width; videoH = videoSize.height }
                }
            })
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    // live preview of the colour/frame effects and the sound
    LaunchedEffect(rotation, aspect, bright, contrast, sat, videoW, videoH) { runCatching { player.setVideoEffects(effects()) } }
    LaunchedEffect(muted) { player.volume = if (muted) 0f else 1f }
    // keep playback inside the trimmed range
    LaunchedEffect(player) {
        while (true) {
            if (player.duration > 0) durMs = player.duration
            if (durMs > 0) {
                val s = (trim.start * durMs).toLong(); val e = (trim.endInclusive * durMs).toLong()
                if (player.currentPosition < s - 200 || player.currentPosition > e) player.seekTo(s)
            }
            delay(80)
        }
    }

    fun export() {
        exporting = 0f
        val out = File(File(ctx.cacheDir, "edited").apply { mkdirs() }, "video_${System.currentTimeMillis()}.mp4")
        val item = MediaItem.Builder().setUri(req.first).setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs((trim.start * durMs).toLong())
                .setEndPositionMs(if (trim.endInclusive >= 0.999f) Long.MIN_VALUE else (trim.endInclusive * durMs).toLong())
                .build(),
        ).build()
        val edited = EditedMediaItem.Builder(item).setRemoveAudio(muted).setEffects(Effects(emptyList(), effects())).build()
        val transformer = Transformer.Builder(ctx).addListener(object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                exporting = null
                req.second(Uri.fromFile(out)); close()
            }
            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                exporting = null
                android.widget.Toast.makeText(ctx, "Не удалось сохранить видео: ${exportException.errorCodeName}", android.widget.Toast.LENGTH_LONG).show()
            }
        }).build()
        transformer.start(edited, out.absolutePath)
        // progress
        val h = android.os.Handler(android.os.Looper.getMainLooper())
        val holder = androidx.media3.transformer.ProgressHolder()
        h.post(object : Runnable {
            override fun run() {
                if (exporting == null) return
                if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) exporting = holder.progress / 100f
                h.postDelayed(this, 200)
            }
        })
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (exporting == null) close() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Row(Modifier.statusBarsPadding().padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MorphIconButton(Icons.Outlined.Close, "Отмена", { if (exporting == null) close() }, color = Color.White.copy(alpha = 0.14f), tint = Color.White)
                Spacer(Modifier.weight(1f))
                MorphIconButton(if (muted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp, "Звук", { muted = !muted },
                    color = if (muted) cs.errorContainer else Color.White.copy(alpha = 0.14f), tint = if (muted) cs.onErrorContainer else Color.White)
                Spacer(Modifier.width(6.dp))
                MorphBox(onClick = { if (exporting == null) export() }, rest = 24.dp, pressed = 12.dp, color = cs.primary, contentColor = cs.onPrimary, modifier = Modifier.height(48.dp)) {
                    Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        val e = exporting
                        if (e != null) { WavyRing(e.coerceAtLeast(0.03f), true, cs.onPrimary, cs.onPrimary.copy(alpha = 0.25f), Modifier.size(26.dp)); Spacer(Modifier.width(6.dp)); Text("${(e * 100).toInt()}%", style = MaterialTheme.typography.titleSmall) }
                        else { Icon(Icons.Outlined.Check, null); Spacer(Modifier.width(6.dp)); Text("Готово", style = MaterialTheme.typography.titleSmall) }
                    }
                }
            }
            AndroidView(
                factory = { c -> PlayerView(c).apply { useController = false; setShutterBackgroundColor(android.graphics.Color.BLACK); this.player = player } },
                modifier = Modifier.fillMaxSize().padding(top = 96.dp, bottom = 230.dp).statusBarsPadding(),
            )
            Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(tool, transitionSpec = { fadeIn(Springs.effects()).togetherWith(fadeOut(Springs.exit())) }, label = "vt") { t ->
                    Column(Modifier.fillMaxWidth().height(140.dp), verticalArrangement = Arrangement.Center) {
                        when (t) {
                            0 -> {
                                val s = (trim.start * durMs / 1000).toInt(); val e = (trim.endInclusive * durMs / 1000).toInt()
                                Text("${app.qwgram.next.td.Texts.duration(s)} – ${app.qwgram.next.td.Texts.duration(e)}  ·  ${app.qwgram.next.td.Texts.duration(e - s)}", color = Color.White, style = MaterialTheme.typography.titleSmall)
                                RangeSlider(trim, { trim = it; player.seekTo((it.start * durMs).toLong()) }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth().noSwipeBack())
                            }
                            1 -> Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                MorphIconButton(Icons.Outlined.RotateLeft, "Повернуть", { rotation = (rotation + 90) % 360 }, color = Color.White.copy(alpha = 0.14f), tint = Color.White)
                                listOf(0f to "Исходный", 1f to "1:1", 4f / 3f to "4:3", 16f / 9f to "16:9", 9f / 16f to "9:16").forEach { (a, label) ->
                                    val sel = aspect == a
                                    val bg by animateColorAsState(if (sel) cs.primary else Color.White.copy(alpha = 0.14f), label = "a")
                                    Box(Modifier.clip(RoundedCornerShape(if (sel) 14.dp else 22.dp)).background(bg).clickable { aspect = a }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                        Text(label, color = if (sel) cs.onPrimary else Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                    }
                                }
                            }
                            else -> Column {
                                @Composable fun adj(name: String, v: Float, set: (Float) -> Unit) = Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(name, color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(110.dp), maxLines = 1)
                                    Slider(v, set, valueRange = -1f..1f, modifier = Modifier.weight(1f).height(32.dp).noSwipeBack())
                                }
                                adj("Яркость", bright) { bright = it }
                                adj("Контраст", contrast) { contrast = it }
                                adj("Насыщенность", sat) { sat = it }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(Triple(Icons.Outlined.ContentCut, "Обрезка", 0), Triple(Icons.Outlined.Crop, "Кадр", 1), Triple(Icons.Outlined.Tune, "Цвет", 2)).forEachIndexed { i, (ic, label, id) ->
                        val sel = tool == id
                        val bg by animateColorAsState(if (sel) cs.primary else Color.White.copy(alpha = 0.12f), label = "t")
                        MorphBox(
                            onClick = { tool = id }, rest = if (sel) 26.dp else if (i == 0 || i == 2) 22.dp else 8.dp, pressed = 14.dp,
                            color = bg, contentColor = if (sel) cs.onPrimary else Color.White, modifier = Modifier.height(52.dp),
                        ) {
                            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(ic, null); Spacer(Modifier.width(8.dp)); Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
