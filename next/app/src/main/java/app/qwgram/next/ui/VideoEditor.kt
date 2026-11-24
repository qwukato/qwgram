package app.qwgram.next.ui

import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
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
    var tool by remember(req) { mutableIntStateOf(0) }       // 0 trim, 1 frame, 2 colour, 3 text & stickers
    val layers = remember(req) { androidx.compose.runtime.mutableStateListOf<EditLayer>() }
    var textDraft by remember(req) { mutableStateOf("") }
    var textColor by remember(req) { mutableStateOf(Color.White) }
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

    /** the frame size after rotation and crop (what the exported video will be) */
    fun outSize(): Pair<Int, Int> {
        val (w, h) = if (rotation % 180 == 0) videoW to videoH else videoH to videoW
        if (aspect <= 0f) return w to h
        val cur = w.toFloat() / h
        return if (cur > aspect) (h * aspect).toInt() to h else w to (w / aspect).toInt()
    }

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
    // live preview: only the colour goes through the player's effects; turning and the frame are drawn by the UI
    // (exactly the maths the export uses), so they always show — and stay correct while you edit
    LaunchedEffect(bright, contrast, sat) {
        runCatching {
            player.setVideoEffects(buildList<Effect> {
                if (bright != 0f) add(Brightness(bright * 0.5f))
                if (contrast != 0f) add(Contrast(contrast * 0.6f))
                if (sat != 0f) add(HslAdjustment.Builder().adjustSaturation(sat * 100f).build())
            })
        }
    }
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
        // the real length (the player may not have reported it yet when the user is quick)
        val total = if (durMs > 0) durMs else player.duration.takeIf { it > 0 } ?: 0L
        val trimmed = total > 0 && (trim.start > 0.001f || trim.endInclusive < 0.999f)
        val item = MediaItem.Builder().setUri(req.first).apply {
            if (trimmed) setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs((trim.start * total).toLong())
                    .setEndPositionMs(if (trim.endInclusive >= 0.999f) androidx.media3.common.C.TIME_END_OF_SOURCE else (trim.endInclusive * total).toLong())
                    .build(),
            )
        }.build()
        val fx = effects().toMutableList()
        if (layers.isNotEmpty()) runCatching {
            // text and stickers: one transparent picture the size of the frame, laid over every frame
            val (ow, oh) = outSize()
            val bmp = android.graphics.Bitmap.createBitmap(ow.coerceAtLeast(2), oh.coerceAtLeast(2), android.graphics.Bitmap.Config.ARGB_8888)
            drawLayers(android.graphics.Canvas(bmp), layers.toList(), bmp.width, bmp.height)
            fx.add(androidx.media3.effect.OverlayEffect(com.google.common.collect.ImmutableList.of<androidx.media3.effect.TextureOverlay>(androidx.media3.effect.BitmapOverlay.createStaticBitmapOverlay(bmp))))
        }
        val edited = EditedMediaItem.Builder(item).setRemoveAudio(muted).setEffects(Effects(emptyList(), fx)).build()
        val transformer = Transformer.Builder(ctx)
            // frame-accurate trim: the cut starts exactly at the chosen moment, not at the previous keyframe
            .experimentalSetTrimOptimizationEnabled(trimmed && fx.isEmpty())
            .addListener(object : Transformer.Listener {
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
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().padding(top = 96.dp, bottom = 230.dp).statusBarsPadding().clipToBounds()) {
                // the exported frame (after turning and cropping), fitted and centred — layers live in its proportions
                val (ow, oh) = outSize()
                val W = constraints.maxWidth.toFloat(); val H = constraints.maxHeight.toFloat()
                val sc = minOf(W / ow.coerceAtLeast(1), H / oh.coerceAtLeast(1))
                val dw = ow * sc; val dh = oh * sc
                val ox = (W - dw) / 2f; val oy = (H - dh) / 2f
                // the whole video at the same scale, turned around the frame's centre (a TextureView follows
                // rotation); what is cropped away stays visible but dimmed, like Telegram's crop
                val turn by androidx.compose.animation.core.animateFloatAsState(-rotation.toFloat(), androidx.compose.animation.core.spring(0.8f, 300f), label = "turn")
                AndroidView(
                    factory = { c -> android.view.TextureView(c) },
                    update = { tv -> player.setVideoTextureView(tv) },
                    modifier = Modifier
                        .layout { m, c ->
                            val w = (videoW * sc).toInt().coerceAtLeast(1); val h = (videoH * sc).toInt().coerceAtLeast(1)
                            val pl = m.measure(androidx.compose.ui.unit.Constraints.fixed(w, h))
                            layout(c.maxWidth, c.maxHeight) { pl.place((ox + dw / 2f - w / 2f).toInt(), (oy + dh / 2f - h / 2f).toInt()) }
                        }
                        .graphicsLayer { rotationZ = turn },
                )
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val dim = Color.Black.copy(alpha = 0.72f)
                    drawRect(dim, androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Size(size.width, oy))
                    drawRect(dim, androidx.compose.ui.geometry.Offset(0f, oy + dh), androidx.compose.ui.geometry.Size(size.width, size.height - oy - dh))
                    drawRect(dim, androidx.compose.ui.geometry.Offset(0f, oy), androidx.compose.ui.geometry.Size(ox, dh))
                    drawRect(dim, androidx.compose.ui.geometry.Offset(ox + dw, oy), androidx.compose.ui.geometry.Size(size.width - ox - dw, dh))
                    if (tool == 1) drawRect(Color.White, androidx.compose.ui.geometry.Offset(ox, oy), androidx.compose.ui.geometry.Size(dw, dh), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                }
                val dens = androidx.compose.ui.platform.LocalDensity.current
                layers.forEach { l ->
                    androidx.compose.runtime.key(l) {
                        val appear = remember { androidx.compose.animation.core.Animatable(0.4f) }
                        LaunchedEffect(Unit) { appear.animateTo(1f, androidx.compose.animation.core.spring(0.5f, 420f)) }
                        val fontPx = l.base * l.scale * dw
                        Box(
                            Modifier
                                .layout { m, c ->
                                    val pl = m.measure(androidx.compose.ui.unit.Constraints())
                                    layout(c.maxWidth, c.maxHeight) { pl.place((ox + l.x * dw - pl.width / 2f).toInt(), (oy + l.y * dh - pl.height / 2f).toInt()) }
                                }
                                .graphicsLayer { rotationZ = l.rot; scaleX = appear.value; scaleY = appear.value }
                                .pointerInput(l, dw, dh) {
                                    detectTransformGestures { _, pan, zoom, rot ->
                                        val a = Math.toRadians(l.rot.toDouble())
                                        val px = (pan.x * kotlin.math.cos(a) - pan.y * kotlin.math.sin(a)).toFloat() * l.scale
                                        val py = (pan.x * kotlin.math.sin(a) + pan.y * kotlin.math.cos(a)).toFloat() * l.scale
                                        l.x = (l.x + px / dw).coerceIn(0f, 1f); l.y = (l.y + py / dh).coerceIn(0f, 1f)
                                        l.scale = (l.scale * zoom).coerceIn(0.3f, 6f); l.rot += rot
                                    }
                                }
                                .padding(horizontal = with(dens) { (fontPx * 0.35f).toDp() }, vertical = with(dens) { (fontPx * 0.2f).toDp() }),
                        ) {
                            if (l.sticker != null) Box(Modifier.size(with(dens) { fontPx.toDp() })) { StickerContent(l.sticker, Modifier.fillMaxSize()) }
                            else Text(
                                l.text, color = l.color, fontSize = with(dens) { fontPx.toSp() }, lineHeight = with(dens) { (fontPx * 1.17f).toSp() },
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = if (!l.emoji) androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(0f, fontPx * 0.04f), fontPx * 0.08f)) else androidx.compose.ui.text.TextStyle.Default,
                            )
                        }
                    }
                }
            }
            Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(tool, transitionSpec = { fadeIn(Springs.effects()).togetherWith(fadeOut(Springs.exit())) }, label = "vt") { t ->
                    Column(Modifier.fillMaxWidth().height(140.dp), verticalArrangement = Arrangement.Center) {
                        when (t) {
                            0 -> {
                                val s = (trim.start * durMs / 1000).toInt(); val e = (trim.endInclusive * durMs / 1000).toInt()
                                Text("${app.qwgram.next.td.Texts.duration(s)} – ${app.qwgram.next.td.Texts.duration(e)}  ·  ${app.qwgram.next.td.Texts.duration(e - s)}", color = Color.White, style = MaterialTheme.typography.titleSmall)
                                // the preview jumps to whichever end you move, so you see exactly where it cuts
                                RangeSlider(trim, { nt ->
                                    val movedEnd = kotlin.math.abs(nt.endInclusive - trim.endInclusive) > kotlin.math.abs(nt.start - trim.start)
                                    trim = nt
                                    player.pause()
                                    player.seekTo(((if (movedEnd) nt.endInclusive else nt.start) * durMs).toLong())
                                }, onValueChangeFinished = { player.seekTo((trim.start * durMs).toLong()); player.play() }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth().noSwipeBack())
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
                            3 -> Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    androidx.compose.material3.TextField(
                                        textDraft, { textDraft = it }, placeholder = { Text("Текст", maxLines = 1) },
                                        shape = RoundedCornerShape(24.dp), singleLine = true,
                                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                                            focusedContainerColor = Color.White.copy(alpha = 0.14f), unfocusedContainerColor = Color.White.copy(alpha = 0.14f),
                                            focusedTextColor = textColor, unfocusedTextColor = textColor,
                                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                        ),
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    MorphIconButton(Icons.Outlined.Add, "Добавить", {
                                        if (textDraft.isNotBlank()) { layers.add(EditLayer(textDraft.trim(), textColor, emoji = false)); textDraft = "" }
                                    }, color = cs.primary, tint = cs.onPrimary)
                                    if (layers.isNotEmpty()) { Spacer(Modifier.width(6.dp)); MorphIconButton(Icons.AutoMirrored.Outlined.Undo, "Убрать последний", { layers.removeAt(layers.lastIndex) }, color = Color.White.copy(alpha = 0.14f), tint = Color.White) }
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    listOf(0xFFFFFFFF, 0xFF000000, 0xFFFF4D4D, 0xFFFFE14D, 0xFF4CD964, 0xFF34AADC, 0xFFFF2D95).forEach { c ->
                                        val col = Color(c); val sel = textColor == col
                                        Box(Modifier.size(if (sel) 32.dp else 26.dp).clip(CircleShape).background(col).border(if (sel) 3.dp else 1.dp, Color.White, CircleShape).clickable { textColor = col })
                                    }
                                }
                            }
                            4 -> EditorStickerPicker(
                                onEmoji = { e -> layers.add(EditLayer(e, Color.White, emoji = true)) },
                                onSticker = { st -> layers.add(EditLayer(st.emoji, Color.White, emoji = true, sticker = st)) },
                            )
                            else -> Column {
                                @Composable fun adj(name: String, v: Float, set: (Float) -> Unit) = Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(name, color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(104.dp), maxLines = 1, softWrap = false)
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
                EditorTools(listOf(Triple(Icons.Outlined.ContentCut, "Длина", 0), Triple(Icons.Outlined.Crop, "Кадр", 1), Triple(Icons.Outlined.Tune, "Цвет", 2), Triple(Icons.Outlined.TextFields, "Текст", 3), Triple(Icons.Outlined.EmojiEmotions, "Стикеры", 4)), tool) { tool = it }
            }
        }
    }
}
