package app.qwgram.next.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas as UiCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.RotateLeft
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.FormatColorText
import androidx.compose.material.icons.outlined.Add
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** a text or emoji-sticker layer on the picture; position is in image proportions, size relative to the image width */
class EditLayer(val text: String, val color: Color, val emoji: Boolean, val bg: Boolean = false) {
    var x by mutableFloatStateOf(0.5f)
    var y by mutableFloatStateOf(0.5f)
    var scale by mutableFloatStateOf(1f)
    var rot by mutableFloatStateOf(0f)
    /** base glyph size, fraction of the image width */
    val base get() = if (emoji) 0.16f else 0.075f
}

/** draws [layers] onto the full-size picture (same maths as the on-screen preview) */
internal fun drawLayers(c: Canvas, layers: List<EditLayer>, w: Int, h: Int) {
    layers.forEach { l ->
        val p = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = l.base * l.scale * w
            color = l.color.toArgb()
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            if (!l.emoji && !l.bg) setShadowLayer(textSize * 0.08f, 0f, textSize * 0.04f, 0x80000000.toInt())
        }
        c.save()
        c.translate(l.x * w, l.y * h); c.rotate(l.rot)
        val fm = p.fontMetrics
        val lines = l.text.split('\n')
        val lh = fm.descent - fm.ascent
        val total = lh * lines.size
        if (l.bg) {
            val bw = lines.maxOf { p.measureText(it) } + p.textSize * 0.7f
            val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = (if (l.color.luminanceApprox() > 0.6f) 0xFF000000 else 0xFFFFFFFF).toInt() }
            c.drawRoundRect(-bw / 2, -total / 2 - p.textSize * 0.2f, bw / 2, total / 2 + p.textSize * 0.2f, p.textSize * 0.4f, p.textSize * 0.4f, bp)
        }
        lines.forEachIndexed { i, s -> c.drawText(s, 0f, -total / 2 + i * lh - fm.ascent, p) }
        c.restore()
    }
}

private fun Color.luminanceApprox() = 0.299f * red + 0.587f * green + 0.114f * blue

/** open the editor for a picture; [onDone] gets the edited copy */
object PhotoEditor {
    var request by mutableStateOf<Pair<Uri, (Uri) -> Unit>?>(null)
    fun open(uri: Uri, onDone: (Uri) -> Unit) { request = uri to onDone }
}

/** photo or video: the matching editor */
fun openMediaEditor(uri: Uri, isVideo: Boolean, onDone: (Uri) -> Unit) {
    if (isVideo) VideoEditor.open(uri, onDone) else PhotoEditor.open(uri, onDone)
}

private class Line(val color: Color, val width: Float, val points: MutableList<Offset>)   // points: 0..1 of the image

private fun adjustMatrix(bright: Float, contrast: Float, sat: Float, warm: Float): android.graphics.ColorMatrix {
    val m = android.graphics.ColorMatrix()
    m.setSaturation(1f + sat)
    val c = 1f + contrast
    val t = 128f * (1f - c) + 255f * bright * 0.35f
    m.postConcat(android.graphics.ColorMatrix(floatArrayOf(c, 0f, 0f, 0f, t + 22f * warm, 0f, c, 0f, 0f, t, 0f, 0f, c, 0f, t - 22f * warm, 0f, 0f, 0f, 1f, 0f)))
    return m
}

private fun loadBitmap(ctx: android.content.Context, uri: Uri, max: Int = 2560): Bitmap? = runCatching {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
    var s = 1
    while (o.outWidth / s > max || o.outHeight / s > max) s *= 2
    val b = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = s }) } ?: return null
    // respect the camera's orientation tag
    val rot = runCatching {
        ctx.contentResolver.openInputStream(uri)?.use { androidx.exifinterface.media.ExifInterface(it).rotationDegrees } ?: 0
    }.getOrDefault(0)
    if (rot == 0) b else Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(rot.toFloat()) }, true)
}.getOrNull()

/**
 * Telegram-style photo editor in M3 Expressive: crop (free / square / 4:3 / 16:9, rotate, mirror), adjustments
 * (brightness, contrast, saturation, warmth — live), and drawing (colour palette, brush size, undo). The tools sit
 * in a floating connected button group; "Готово" renders the result at full quality.
 */
@Composable
fun PhotoEditorHost() {
    val req = PhotoEditor.request ?: return
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var src by remember(req) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(req) { src = withContext(Dispatchers.IO) { loadBitmap(ctx, req.first) } }
    var tool by remember(req) { mutableStateOf(0) }           // 0 crop, 1 adjust, 2 draw, 3 text, 4 stickers
    val layers = remember(req) { mutableStateListOf<EditLayer>() }
    var textDraft by remember(req) { mutableStateOf("") }
    var textColor by remember(req) { mutableStateOf(Color.White) }
    var textBg by remember(req) { mutableStateOf(false) }
    var crop by remember(req) { mutableStateOf(Rect(0f, 0f, 1f, 1f)) }
    var aspect by remember(req) { mutableStateOf(0f) }         // 0 = free
    var bright by remember(req) { mutableFloatStateOf(0f) }
    var contrast by remember(req) { mutableFloatStateOf(0f) }
    var sat by remember(req) { mutableFloatStateOf(0f) }
    var warm by remember(req) { mutableFloatStateOf(0f) }
    val lines = remember(req) { mutableStateListOf<Line>() }
    var brushColor by remember(req) { mutableStateOf(Color(0xFFFF4D4D)) }
    var brushW by remember(req) { mutableFloatStateOf(0.012f) }
    var saving by remember { mutableStateOf(false) }
    fun close() { PhotoEditor.request = null }
    BackHandler { close() }
    val bmp = src
    val image: ImageBitmap? = remember(bmp) { bmp?.asImageBitmap() }
    val filter = remember(bright, contrast, sat, warm) { ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(adjustMatrix(bright, contrast, sat, warm).array)) }

    fun setAspect(a: Float) {
        aspect = a
        val b = bmp ?: return
        if (a == 0f) return
        // largest centred rect of that aspect (in image proportions)
        val iw = b.width.toFloat(); val ih = b.height.toFloat()
        var w = iw; var h = iw / a
        if (h > ih) { h = ih; w = ih * a }
        crop = Rect((iw - w) / 2f / iw, (ih - h) / 2f / ih, (iw + w) / 2f / iw, (ih + h) / 2f / ih)
    }

    fun render() {
        val b = bmp ?: return
        saving = true
        scope.launch {
            val uri = withContext(Dispatchers.Default) {
                runCatching {
                    val out = Bitmap.createBitmap(b.width, b.height, Bitmap.Config.ARGB_8888)
                    val c = Canvas(out)
                    c.drawBitmap(b, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(adjustMatrix(bright, contrast, sat, warm)) })
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
                    lines.forEach { l ->
                        p.color = l.color.toArgb(); p.strokeWidth = l.width * b.width
                        val path = android.graphics.Path()
                        l.points.forEachIndexed { i, pt -> if (i == 0) path.moveTo(pt.x * b.width, pt.y * b.height) else path.lineTo(pt.x * b.width, pt.y * b.height) }
                        c.drawPath(path, p)
                    }
                    drawLayers(c, layers, b.width, b.height)
                    val x = (crop.left * b.width).toInt().coerceIn(0, b.width - 1)
                    val y = (crop.top * b.height).toInt().coerceIn(0, b.height - 1)
                    val w = (crop.width * b.width).toInt().coerceIn(1, b.width - x)
                    val h = (crop.height * b.height).toInt().coerceIn(1, b.height - y)
                    val cropped = Bitmap.createBitmap(out, x, y, w, h)
                    val dir = File(ctx.cacheDir, "edited").apply { mkdirs() }
                    val f = File(dir, "edit_${System.currentTimeMillis()}.jpg")
                    f.outputStream().use { cropped.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                    Uri.fromFile(f)
                }.getOrNull()
            }
            saving = false
            if (uri != null) { req.second(uri); close() }
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { close() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false),
    ) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // top bar
        Row(Modifier.statusBarsPadding().padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MorphIconButton(Icons.Outlined.Close, "Отмена", { close() }, color = Color.White.copy(alpha = 0.14f), tint = Color.White)
            Spacer(Modifier.weight(1f))
            if (tool == 2 && lines.isNotEmpty()) MorphIconButton(Icons.AutoMirrored.Outlined.Undo, "Отменить", { lines.removeAt(lines.lastIndex) }, color = Color.White.copy(alpha = 0.14f), tint = Color.White)
            if (tool >= 3 && layers.isNotEmpty()) MorphIconButton(Icons.AutoMirrored.Outlined.Undo, "Убрать последний", { layers.removeAt(layers.lastIndex) }, color = Color.White.copy(alpha = 0.14f), tint = Color.White)
            Spacer(Modifier.width(6.dp))
            MorphBox(onClick = { if (!saving) render() }, rest = 24.dp, pressed = 12.dp, color = cs.primary, contentColor = cs.onPrimary, modifier = Modifier.height(48.dp)) {
                Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (saving) androidx.compose.material3.LoadingIndicator(Modifier.size(24.dp), color = cs.onPrimary) else Icon(Icons.Outlined.Check, null)
                    Spacer(Modifier.width(6.dp)); Text("Готово", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        // the picture
        BoxWithConstraints(Modifier.fillMaxSize().padding(top = 96.dp, bottom = 220.dp, start = 16.dp, end = 16.dp).statusBarsPadding(), contentAlignment = Alignment.Center) {
            if (image == null || bmp == null) { androidx.compose.material3.LoadingIndicator(Modifier.size(48.dp)); return@BoxWithConstraints }
            val W = constraints.maxWidth.toFloat(); val H = constraints.maxHeight.toFloat()
            val sc = minOf(W / bmp.width, H / bmp.height)
            val dw = bmp.width * sc; val dh = bmp.height * sc
            val ox = (W - dw) / 2f; val oy = (H - dh) / 2f
            val img = Rect(ox, oy, ox + dw, oy + dh)
            Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit, colorFilter = filter)
            // drawing layer + its gesture
            var live by remember { mutableStateOf<Line?>(null) }
            UiCanvas(
                Modifier.fillMaxSize().then(if (tool == 2) Modifier.pointerInput(brushColor, brushW) {
                    detectDragGestures(
                        onDragStart = { o -> live = Line(brushColor, brushW, mutableListOf(Offset((o.x - ox) / dw, (o.y - oy) / dh))) },
                        onDrag = { c, _ -> c.consume(); live?.let { l -> l.points.add(Offset((c.position.x - ox) / dw, (c.position.y - oy) / dh)); live = Line(l.color, l.width, l.points) } },
                        onDragEnd = { live?.let { lines.add(it) }; live = null },
                    )
                } else Modifier),
            ) {
                (lines + listOfNotNull(live)).forEach { l ->
                    val path = Path()
                    l.points.forEachIndexed { i, p -> val x = ox + p.x * dw; val y = oy + p.y * dh; if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
                    drawPath(path, l.color, style = Stroke(l.width * dw, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                // crop: dim outside, frame + grid + corner handles
                val r = Rect(img.left + crop.left * dw, img.top + crop.top * dh, img.left + crop.right * dw, img.top + crop.bottom * dh)
                val dim = Color.Black.copy(alpha = if (tool == 0) 0.6f else 0.85f)
                drawRect(dim, Offset(img.left, img.top), Size(dw, r.top - img.top))
                drawRect(dim, Offset(img.left, r.bottom), Size(dw, img.bottom - r.bottom))
                drawRect(dim, Offset(img.left, r.top), Size(r.left - img.left, r.height))
                drawRect(dim, Offset(r.right, r.top), Size(img.right - r.right, r.height))
                if (tool == 0) {
                    drawRect(Color.White, r.topLeft, r.size, style = Stroke(2.dp.toPx()))
                    for (i in 1..2) {
                        drawLine(Color.White.copy(alpha = 0.4f), Offset(r.left + r.width * i / 3, r.top), Offset(r.left + r.width * i / 3, r.bottom), 1.dp.toPx())
                        drawLine(Color.White.copy(alpha = 0.4f), Offset(r.left, r.top + r.height * i / 3), Offset(r.right, r.top + r.height * i / 3), 1.dp.toPx())
                    }
                    val hl = 22.dp.toPx(); val hw = 4.dp.toPx()
                    listOf(r.topLeft to Offset(1f, 1f), r.topRight to Offset(-1f, 1f), r.bottomLeft to Offset(1f, -1f), r.bottomRight to Offset(-1f, -1f)).forEach { (pt, d) ->
                        drawLine(Color.White, pt, Offset(pt.x + hl * d.x, pt.y), hw, StrokeCap.Round)
                        drawLine(Color.White, pt, Offset(pt.x, pt.y + hl * d.y), hw, StrokeCap.Round)
                    }
                }
            }
            // text / sticker layers: drag to move, pinch to scale and rotate (any tool except crop and drawing)
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
                            .then(if (tool != 0 && tool != 2) Modifier.pointerInput(l, dw, dh) {
                                androidx.compose.foundation.gestures.detectTransformGestures { _, pan, zoom, rot ->
                                    // pan arrives in the rotated frame: turn it back into screen space
                                    val a = Math.toRadians(l.rot.toDouble())
                                    val px = (pan.x * kotlin.math.cos(a) - pan.y * kotlin.math.sin(a)).toFloat() * l.scale
                                    val py = (pan.x * kotlin.math.sin(a) + pan.y * kotlin.math.cos(a)).toFloat() * l.scale
                                    l.x = (l.x + px / dw).coerceIn(0f, 1f); l.y = (l.y + py / dh).coerceIn(0f, 1f)
                                    l.scale = (l.scale * zoom).coerceIn(0.3f, 6f); l.rot += rot
                                }
                            } else Modifier)
                            .then(if (l.bg) Modifier.clip(RoundedCornerShape(with(dens) { (fontPx * 0.4f).toDp() })).background(if (l.color.luminanceApprox() > 0.6f) Color.Black else Color.White) else Modifier)
                            .padding(horizontal = with(dens) { (fontPx * 0.35f).toDp() }, vertical = with(dens) { (fontPx * 0.2f).toDp() }),
                    ) {
                        Text(
                            l.text, color = l.color, fontSize = with(dens) { fontPx.toSp() }, lineHeight = with(dens) { (fontPx * 1.17f).toSp() },
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = if (!l.emoji && !l.bg) androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.5f), Offset(0f, fontPx * 0.04f), fontPx * 0.08f)) else androidx.compose.ui.text.TextStyle.Default,
                        )
                    }
                }
            }
            // crop gesture: drag a corner/edge to resize (keeps the aspect if one is set), drag inside to move
            if (tool == 0) Box(Modifier.fillMaxSize().pointerInput(aspect, bmp) {
                var mode = 0 // 1..4 corners, 5 move
                detectDragGestures(
                    onDragStart = { o ->
                        val r = Rect(img.left + crop.left * dw, img.top + crop.top * dh, img.left + crop.right * dw, img.top + crop.bottom * dh)
                        val t = 40.dp.toPx()
                        mode = when {
                            (o - r.topLeft).getDistance() < t -> 1
                            (o - r.topRight).getDistance() < t -> 2
                            (o - r.bottomLeft).getDistance() < t -> 3
                            (o - r.bottomRight).getDistance() < t -> 4
                            r.contains(o) -> 5
                            else -> 0
                        }
                    },
                    onDrag = { c, d ->
                        c.consume()
                        val dx = d.x / dw; val dy = d.y / dh
                        val min = 0.08f
                        var (l, t, rr, b) = listOf(crop.left, crop.top, crop.right, crop.bottom)
                        when (mode) {
                            1 -> { l += dx; t += dy }
                            2 -> { rr += dx; t += dy }
                            3 -> { l += dx; b += dy }
                            4 -> { rr += dx; b += dy }
                            5 -> { val w = rr - l; val h = b - t; l = (l + dx).coerceIn(0f, 1f - w); t = (t + dy).coerceIn(0f, 1f - h); rr = l + w; b = t + h }
                        }
                        l = l.coerceIn(0f, rr - min); t = t.coerceIn(0f, b - min); rr = rr.coerceIn(l + min, 1f); b = b.coerceIn(t + min, 1f)
                        if (aspect > 0f && mode in 1..4) {
                            // keep the aspect: height follows width (in pixels)
                            val wpx = (rr - l) * bmp.width
                            val hN = (wpx / aspect) / bmp.height
                            if (mode == 1 || mode == 2) t = (b - hN).coerceAtLeast(0f) else b = (t + hN).coerceAtMost(1f)
                        }
                        crop = Rect(l, t, rr, b)
                    },
                )
            })
        }
        // tool panel
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(tool, transitionSpec = { fadeIn(Springs.effects()).togetherWith(fadeOut(Springs.exit())) }, label = "tool") { t ->
                Column(Modifier.fillMaxWidth().height(132.dp), verticalArrangement = Arrangement.Center) {
                    when (t) {
                        0 -> Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            MorphIconButton(Icons.Outlined.RotateLeft, "Повернуть", {
                                bmp?.let { b -> src = Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(-90f) }, true) }
                                crop = Rect(0f, 0f, 1f, 1f); lines.clear(); layers.clear(); aspect = 0f
                            }, color = Color.White.copy(alpha = 0.14f), tint = Color.White)
                            MorphIconButton(Icons.Outlined.Flip, "Отразить", {
                                bmp?.let { b -> src = Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postScale(-1f, 1f) }, true) }
                                crop = Rect(1f - crop.right, crop.top, 1f - crop.left, crop.bottom); lines.clear(); layers.forEach { it.x = 1f - it.x; it.rot = -it.rot }
                            }, color = Color.White.copy(alpha = 0.14f), tint = Color.White)
                            listOf(0f to "Свободно", 1f to "1:1", 4f / 3f to "4:3", 3f / 4f to "3:4", 16f / 9f to "16:9").forEach { (a, label) ->
                                val sel = aspect == a
                                val bg by animateColorAsState(if (sel) cs.primary else Color.White.copy(alpha = 0.14f), label = "a")
                                Box(Modifier.clip(RoundedCornerShape(if (sel) 14.dp else 22.dp)).background(bg).clickable { setAspect(a); if (a == 0f) crop = Rect(0f, 0f, 1f, 1f) }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                    Text(label, color = if (sel) cs.onPrimary else Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                }
                            }
                        }
                        1 -> Column {
                            @Composable fun adj(name: String, v: Float, set: (Float) -> Unit) = Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(name, color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(96.dp), maxLines = 1)
                                Slider(v, set, valueRange = -1f..1f, modifier = Modifier.weight(1f).height(30.dp).noSwipeBack())
                                Text((v * 100).toInt().toString(), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(36.dp))
                            }
                            adj("Яркость", bright) { bright = it }
                            adj("Контраст", contrast) { contrast = it }
                            adj("Насыщенность", sat) { sat = it }
                            adj("Тепло", warm) { warm = it }
                        }
                        3 -> Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.TextField(
                                    textDraft, { textDraft = it }, placeholder = { Text("Текст", maxLines = 1) },
                                    shape = RoundedCornerShape(24.dp), maxLines = 3,
                                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                                        focusedContainerColor = Color.White.copy(alpha = 0.14f), unfocusedContainerColor = Color.White.copy(alpha = 0.14f),
                                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(8.dp))
                                MorphIconButton(if (textBg) Icons.Outlined.FormatColorFill else Icons.Outlined.FormatColorText, "Фон текста", { textBg = !textBg }, color = Color.White.copy(alpha = if (textBg) 0.3f else 0.14f), tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                MorphIconButton(Icons.Outlined.Add, "Добавить", {
                                    if (textDraft.isNotBlank()) { layers.add(EditLayer(textDraft.trim(), textColor, emoji = false, bg = textBg)); textDraft = "" }
                                }, color = cs.primary, tint = cs.onPrimary)
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(0xFFFFFFFF, 0xFF000000, 0xFFFF4D4D, 0xFFFF9F1C, 0xFFFFE14D, 0xFF4CD964, 0xFF34AADC, 0xFF5856D6, 0xFFFF2D95).forEach { c ->
                                    val col = Color(c)
                                    val sel = textColor == col
                                    val sz by androidx.compose.animation.core.animateDpAsState(if (sel) 36.dp else 28.dp, androidx.compose.animation.core.spring(0.5f, 500f), label = "tc")
                                    Box(Modifier.size(sz).clip(CircleShape).background(col).border(if (sel) 3.dp else 1.dp, Color.White, CircleShape).clickable { textColor = col })
                                }
                            }
                        }
                        4 -> androidx.compose.foundation.lazy.grid.LazyHorizontalGrid(
                            androidx.compose.foundation.lazy.grid.GridCells.Fixed(2), Modifier.fillMaxWidth().height(120.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(STICKER_EMOJI.size) { i ->
                                val e = STICKER_EMOJI[i]
                                Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).clickable { layers.add(EditLayer(e, Color.White, emoji = true)) }, contentAlignment = Alignment.Center) {
                                    Text(e, fontSize = androidx.compose.ui.unit.TextUnit(32f, androidx.compose.ui.unit.TextUnitType.Sp))
                                }
                            }
                        }
                        else -> Column {
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(0xFFFFFFFF, 0xFF000000, 0xFFFF4D4D, 0xFFFF9F1C, 0xFFFFE14D, 0xFF4CD964, 0xFF34AADC, 0xFF5856D6, 0xFFFF2D95).forEach { c ->
                                    val col = Color(c)
                                    val sel = brushColor == col
                                    val sz by androidx.compose.animation.core.animateDpAsState(if (sel) 40.dp else 32.dp, androidx.compose.animation.core.spring(0.5f, 500f), label = "c")
                                    Box(Modifier.size(sz).clip(CircleShape).background(col).border(if (sel) 3.dp else 1.dp, Color.White, CircleShape).clickable { brushColor = col })
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Кисть", color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(64.dp))
                                Slider(brushW, { brushW = it }, valueRange = 0.004f..0.04f, modifier = Modifier.weight(1f).noSwipeBack())
                                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { Box(Modifier.size((brushW * 900).dp.coerceAtMost(34.dp)).clip(CircleShape).background(brushColor)) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // tools: connected button group — the selected one widens and shows its name
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val tools = listOf(Triple(Icons.Outlined.Crop, "Обрезка", 0), Triple(Icons.Outlined.Tune, "Настройка", 1), Triple(Icons.Outlined.Brush, "Рисование", 2), Triple(Icons.Outlined.TextFields, "Текст", 3), Triple(Icons.Outlined.EmojiEmotions, "Стикеры", 4))
                tools.forEachIndexed { i, (ic, label, id) ->
                    val sel = tool == id
                    val bg by animateColorAsState(if (sel) cs.primary else Color.White.copy(alpha = 0.12f), label = "t")
                    MorphBox(
                        onClick = { tool = id }, rest = if (sel) 26.dp else if (i == 0 || i == tools.lastIndex) 22.dp else 8.dp, pressed = 14.dp,
                        color = bg, contentColor = if (sel) cs.onPrimary else Color.White, modifier = Modifier.height(52.dp),
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(ic, label)
                            androidx.compose.animation.AnimatedVisibility(sel, enter = androidx.compose.animation.expandHorizontally(Springs.smooth(), clip = true) + fadeIn(), exit = androidx.compose.animation.shrinkHorizontally(Springs.smooth(), clip = true) + fadeOut()) {
                                Row { Spacer(Modifier.width(8.dp)); Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false) }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

internal val STICKER_EMOJI = listOf(
    "😀","😂","🥰","😍","😎","🤩","😭","😡","🤯","🥳","😴","🤔","🙈","👍","👎","👏","🙏","💪","🔥","✨",
    "❤️","💔","💯","⭐","🌈","☀️","🌙","⚡","❄️","🎉","🎁","🎂","🍕","☕","🍓","🐶","🐱","🦊","🐼","🦄",
    "🌸","🌻","🍀","🚀","✈️","🏖️","🎵","📸","💡","👑","💎","🏆","⚽","🎮","❗","❓","➡️","✅","❌","💤",
)
