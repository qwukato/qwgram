package app.qwgram.next.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.qwgram.next.Prefs
import app.qwgram.next.td.Images
import app.qwgram.next.td.Texts
import app.qwgram.next.td.rememberFileBitmap
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** 0 = mini player, 1 = full player: one value drives the whole container transform */
object PlayerUi {
    val expand = Animatable(0f, visibilityThreshold = 0.0005f)
}

private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun lerpDp(a: Dp, b: Dp, t: Float) = a + (b - a) * t

/** average colour of the cover: tints the full player (computed once per cover) */
@Composable
private fun rememberCoverTint(bmp: ImageBitmap?): Color? = remember(bmp) {
    bmp?.let { b ->
        runCatching {
            val px = android.graphics.Bitmap.createScaledBitmap(b.asAndroidBitmap().copy(android.graphics.Bitmap.Config.ARGB_8888, false), 1, 1, true).getPixel(0, 0)
            Color(px)
        }.getOrNull()
    }
}

/**
 * The mini player (a floating pill under the top bar) and the full player are ONE container: tapping (or pulling
 * down) the pill grows it into the full screen on a spring — the cover flies from the little circle to the big
 * rounded square, the controls cross-fade, the colours blend; pulling the full player down shrinks it back.
 * Swipe the pill sideways for next / previous.
 */
@Composable
fun PlayerHost() {
    val track = Player.track
    if (track == null || Player.dismissed) {
        LaunchedEffect(Unit) { PlayerUi.expand.snapTo(0f) }
        return
    }
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val k = Prefs.motionSpeed.value * Prefs.motionSpeed.value
    val off = Prefs.motion.value == "off"
    val springOpen = spring<Float>(dampingRatio = 0.78f, stiffness = 300f * k)
    fun animateTo(v: Float) = scope.launch { if (off) PlayerUi.expand.snapTo(v) else PlayerUi.expand.animateTo(v, springOpen) }
    val p = PlayerUi.expand.value
    BackHandler(enabled = PlayerUi.expand.targetValue > 0.5f) { animateTo(0f) }

    val coverBmp = rememberFileBitmap(track.cover, maxSide = 800)
    val miniBmp = remember(track) { Images.mini(track.mini) }
    val cover = coverBmp ?: miniBmp
    val tint = rememberCoverTint(cover)
    val fullBg by animateColorAsState(tint?.let { lerp(cs.surfaceContainer, it, 0.35f) } ?: cs.surfaceContainer, label = "bg")

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val top = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
        val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
        // mini: pill under the top bar; full: the whole screen
        val miniTop = top + 76.dp
        val x = lerpDp(12.dp, 0.dp, p)
        val y = lerpDp(miniTop, 0.dp, p)
        val cw = lerpDp(w - 24.dp, w, p)
        val ch = lerpDp(64.dp, h, p)
        val radius = lerpDp(32.dp, screenCornerRadius(), p).coerceAtLeast(0.dp)
        var swipeX by remember { mutableFloatStateOf(0f) }
        val swipeAnim = remember { Animatable(0f) }
        val miniGlass = p < 0.02f && blurOn(Prefs.blurBars) && BlurHost.top != null
        Box(
            Modifier
                .layout { m, c ->
                    val pl = m.measure(Constraints.fixed(cw.roundToPx().coerceAtLeast(1), ch.roundToPx().coerceAtLeast(1)))
                    layout(c.maxWidth, c.maxHeight) { pl.place(x.roundToPx() + (swipeX + swipeAnim.value).toInt(), y.roundToPx()) }
                }
                .graphicsLayer {
                    shape = RoundedCornerShape(radius); clip = true
                    shadowElevation = if (miniGlass) 0f else with(density) { lerpF(6.dp.toPx(), 0f, p) }
                    alpha = 1f - (abs(swipeX + swipeAnim.value) / (size.width * 0.9f)).coerceIn(0f, 0.6f)
                }
                // the mini player is frosted glass over the screen under it (Appearance → Blur); the full player stays solid
                .then(if (miniGlass) Modifier.glass(RoundedCornerShape(radius), cs.surfaceContainerHigh, state = BlurHost.top) else Modifier)
                .background(if (miniGlass) Color.Transparent else lerp(cs.surfaceContainerHigh, fullBg, p))
                // gestures: on the pill — tap opens, sideways = next/prev, pull down opens; on the full player — pull down closes
                .pointerInput(Unit) {
                    val slop = viewConfiguration.touchSlop
                    val fullH = size.height.toFloat()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startP = PlayerUi.expand.value
                        val vt = VelocityTracker()
                        vt.addPosition(down.uptimeMillis, down.position)
                        var mode = 0  // 1 = vertical (expand/collapse), 2 = horizontal (mini: next/prev)
                        var acc = Offset.Zero
                        var last = down.position
                        while (true) {
                            val ev = awaitPointerEvent()
                            val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                            if (!c.pressed) break
                            vt.addPosition(c.uptimeMillis, c.position)
                            val d = c.position - last
                            last = c.position
                            if (mode == 0) {
                                acc += d
                                if (abs(acc.y) > slop && abs(acc.y) > abs(acc.x)) mode = 1
                                else if (abs(acc.x) > slop && startP < 0.5f) mode = 2
                                // full player: only pulling down from the upper part drags it (controls keep their taps)
                                if (mode == 1 && startP > 0.5f && (acc.y < 0f || down.position.y > fullH * 0.62f)) { mode = -1; break }
                            }
                            when (mode) {
                                1 -> {
                                    c.consume()
                                    val range = with(density) { (h - miniTop).toPx() }
                                    scope.launch { PlayerUi.expand.snapTo((PlayerUi.expand.value + d.y / range * (if (startP > 0.5f) -1f else 1f)).coerceIn(0f, 1f)) }
                                }
                                2 -> { c.consume(); swipeX += d.x }
                            }
                        }
                        val v = vt.calculateVelocity()
                        when (mode) {
                            0 -> if (startP < 0.5f) animateTo(1f)
                            1 -> {
                                val cur = PlayerUi.expand.value
                                val openIt = if (startP > 0.5f) !(v.y > 900f || cur < 0.72f) else (v.y > 600f || cur > 0.25f)
                                animateTo(if (openIt) 1f else 0f)
                            }
                            2 -> {
                                val dx = swipeX
                                val thr = size.width * 0.28f
                                scope.launch {
                                    swipeAnim.snapTo(dx); swipeX = 0f
                                    if (abs(dx) > thr || abs(v.x) > 1200f) {
                                        if (dx < 0) Player.next() else Player.previous()
                                        swipeAnim.animateTo(if (dx < 0) -size.width.toFloat() else size.width.toFloat(), tween(140))
                                        swipeAnim.snapTo(if (dx < 0) size.width * 0.5f else -size.width * 0.5f)
                                    }
                                    swipeAnim.animateTo(0f, spring(0.75f, 420f))
                                }
                            }
                        }
                    }
                },
        ) {
            // the cover: circle in the pill → big rounded square in the full player
            val big = w - 48.dp
            val coverSize = lerpDp(44.dp, big, p)
            val coverX = lerpDp(10.dp, 24.dp, p)
            val coverY = lerpDp(10.dp, top + 84.dp, p)
            val coverR = lerpDp(22.dp, 32.dp, p)
            // turns only while it plays in the pill: a stopped animation draws nothing (battery)
            val spin = loopPhase(p < 0.02f && Player.playing && !off, 16000, 360f)
            Box(
                Modifier
                    .layout { m, _ ->
                        val s = coverSize.roundToPx()
                        val pl = m.measure(Constraints.fixed(s, s))
                        layout(s, s) { pl.place(coverX.roundToPx(), coverY.roundToPx()) }
                    }
                    .graphicsLayer {
                        // the pill's round cover turns slowly while playing (like a record)
                        rotationZ = if (p < 0.02f && Player.playing && !off) spin.value else 0f
                        shape = RoundedCornerShape(coverR); clip = true
                    }
                    .background(cs.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (cover != null) Image(cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Outlined.MusicNote, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(lerpDp(22.dp, 96.dp, p)))
            }
            // mini controls
            if (p < 0.35f) Row(
                Modifier.fillMaxWidth().height(64.dp).padding(start = 64.dp, end = 6.dp).graphicsLayer { alpha = (1f - p * 3f).coerceIn(0f, 1f) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    AnimatedContent(track, transitionSpec = { (slideInHorizontally { it / 3 } + fadeIn()).togetherWith(slideOutHorizontally { -it / 3 } + fadeOut()) }, label = "mt") { t ->
                        Column {
                            Text(t.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = cs.onSurface)
                            if (t.performer.isNotBlank()) Text(t.performer, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = cs.onSurfaceVariant)
                        }
                    }
                }
                MorphIconButton(Icons.Filled.SkipPrevious, "Предыдущий", { Player.previous() }, size = 40.dp, tint = cs.primary)
                Box(contentAlignment = Alignment.Center) {
                    WavyRing(Player.progress, Player.playing, cs.primary, cs.primary.copy(alpha = 0.18f), Modifier.size(46.dp))
                    PlayMorphButton(Player.playing, 36.dp, cs.primaryContainer, cs.onPrimaryContainer) { Player.playPause() }
                }
                MorphIconButton(Icons.Filled.SkipNext, "Следующий", { Player.next() }, size = 40.dp, tint = cs.primary)
                MorphIconButton(Icons.Outlined.Close, "Закрыть", { Player.close() }, size = 36.dp, tint = cs.onSurfaceVariant)
            }
            // full player
            if (p > 0.4f) FullControls(track, big, top, bottomInset, Modifier.graphicsLayer { alpha = ((p - 0.45f) / 0.55f).coerceIn(0f, 1f); translationY = (1f - p) * 80.dp.toPx() }) { animateTo(0f) }
        }
    }
}

@Composable
private fun FullControls(track: Track, coverSize: Dp, top: Dp, bottomInset: Dp, modifier: Modifier, onCollapse: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var queueOpen by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalContentColor provides cs.onSurface) {
        Box(modifier.fillMaxSize()) {
            // header
            Row(Modifier.padding(top = top + 8.dp, start = 8.dp, end = 8.dp).fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                MorphIconButton(Icons.Outlined.KeyboardArrowDown, "Свернуть", onCollapse)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Сейчас играет", style = MaterialTheme.typography.titleMedium)
                    if (Player.queue.size > 1) Text("Трек ${Player.index + 1} из ${Player.queue.size}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
                MorphIconButton(Icons.Outlined.QueueMusic, "Очередь", { queueOpen = true })
            }
            Column(
                Modifier.fillMaxWidth().padding(top = top + 84.dp + coverSize + 24.dp, start = 24.dp, end = 24.dp, bottom = bottomInset + 16.dp),
            ) {
                AnimatedContent(track, transitionSpec = { (slideInHorizontally(spring(0.8f, 380f)) { it / 4 } + fadeIn()).togetherWith(slideOutHorizontally { -it / 4 } + fadeOut()) }, label = "ft") { t ->
                    Column {
                        Text(t.title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(t.performer.ifBlank { "Неизвестный исполнитель" }, style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(20.dp))
                WavySeekBar(Player.progress, Player.playing, cs.primary, cs.onSurface.copy(alpha = 0.2f), Modifier.fillMaxWidth().height(36.dp).noSwipeBack()) { Player.seekTo(it) }
                Row(Modifier.fillMaxWidth()) {
                    Text(Texts.duration(Player.positionMs / 1000), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text(Texts.duration(maxOf(Player.durationMs / 1000, track.durationSec)), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                }
                Spacer(Modifier.height(18.dp))
                // transport: shuffle · prev · PLAY (morphs circle ⇄ squircle) · next · repeat
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    ToggleIcon(Icons.Outlined.Shuffle, "Перемешать", Player.shuffle) { Player.shuffle = !Player.shuffle }
                    MorphIconButton(Icons.Filled.SkipPrevious, "Предыдущий", { Player.previous() }, size = 64.dp, color = cs.secondaryContainer, tint = cs.onSecondaryContainer)
                    PlayMorphButton(Player.playing, 96.dp, cs.primary, cs.onPrimary, wide = true) { Player.playPause() }
                    MorphIconButton(Icons.Filled.SkipNext, "Следующий", { Player.next() }, size = 64.dp, color = cs.secondaryContainer, tint = cs.onSecondaryContainer)
                    ToggleIcon(if (Player.repeat == 2) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat, "Повтор", Player.repeat != 0) { Player.repeat = (Player.repeat + 1) % 3 }
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    QTextButton(onClick = { Player.cycleSpeed() }) {
                        AnimatedContent(Player.speed, transitionSpec = { (scaleIn(spring(0.5f, 600f)) + fadeIn()).togetherWith(scaleOut() + fadeOut()) }, label = "sp") { s ->
                            Text((if (s % 1f == 0f) s.toInt().toString() else s.toString()) + "×", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (Player.loading) LoadingIndicatorSafe()
                }
            }
        }
    }
    if (queueOpen) QueueSheet { queueOpen = false }
}

@Composable
private fun LoadingIndicatorSafe() = androidx.compose.material3.LoadingIndicator(Modifier.size(32.dp))

@Composable
private fun ToggleIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, on: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (on) cs.tertiaryContainer else Color.Transparent, label = "tb")
    MorphIconButton(icon, desc, onClick, size = 48.dp, color = bg, tint = if (on) cs.onTertiaryContainer else cs.onSurfaceVariant)
}

/** play / pause: a circle while paused, a squircle while playing (M3 Expressive), the icon spins across */
@Composable
fun PlayMorphButton(playing: Boolean, size: Dp, bg: Color, fg: Color, wide: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val r by androidx.compose.animation.core.animateDpAsState(if (playing) size * 0.3f else size / 2, spring(0.5f, 420f), label = "pr")
    val sc by animateFloatAsState(if (pressed) 0.9f else 1f, if (pressed) Springs.pressIn() else Springs.morphRelease(), label = "ps")
    val width by androidx.compose.animation.core.animateDpAsState(if (wide && playing) size * 1.25f else size, spring(0.6f, 420f), label = "pw")
    Box(
        Modifier.size(width, size).graphicsLayer { scaleX = sc; scaleY = sc; shape = RoundedCornerShape((r - 0.dp * 1f).coerceAtLeast(0.dp)); clip = true }
            .background(bg).clickable(interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(playing, transitionSpec = { (scaleIn(spring(0.5f, 600f), initialScale = 0.4f) + fadeIn()).togetherWith(scaleOut(targetScale = 0.4f) + fadeOut()) }, label = "pi") { pl ->
            Icon(if (pl) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (pl) "Пауза" else "Играть", tint = fg, modifier = Modifier.size(size * 0.45f))
        }
    }
}

/** circular progress with a gentle wave while playing (expressive ring around the mini play button) */
@Composable
fun WavyRing(progress: Float, playing: Boolean, color: Color, track: Color, modifier: Modifier) {
    val amp by animateFloatAsState(if (playing) 1f else 0f, label = "amp")
    // the wave moves only while playing (and while it settles): no endless redraws when paused
    val phase = loopPhase(playing || amp > 0.01f, 1400, (2 * PI).toFloat())
    Canvas(modifier) {
        val stroke = 3.dp.toPx()
        val r = size.minDimension / 2f - stroke * 1.6f
        val c = center
        drawCircle(track, r, c, style = Stroke(stroke))
        val sweep = (progress.coerceIn(0f, 1f) * 2 * PI).toFloat()
        if (sweep * r < stroke * 2f) return@Canvas   // too short to draw: avoid a lone dot
        val path = Path()
        val steps = 90
        for (i in 0..steps) {
            val a = -PI.toFloat() / 2 + sweep * i / steps
            val rr = r + sin(a * 8f + phase.value) * 1.6.dp.toPx() * amp
            val pt = Offset(c.x + rr * kotlin.math.cos(a), c.y + rr * sin(a))
            if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

/** seek bar: the played part is a moving wave (still when paused), the rest a flat line, a tall thumb between */
@Composable
fun WavySeekBar(progress: Float, playing: Boolean, color: Color, track: Color, modifier: Modifier, onSeek: (Float) -> Unit) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val amp by animateFloatAsState(if (playing && dragging == null) 1f else 0f, label = "a")
    val phase = loopPhase(amp > 0.01f || (playing && dragging == null), 1600, (2 * PI).toFloat())
    Canvas(
        modifier
            .pointerInput(Unit) { detectTapGestures { onSeek((it.x / size.width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    dragging = (down.position.x / size.width).coerceIn(0f, 1f)
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!c.pressed) break
                        c.consume()
                        dragging = (c.position.x / size.width).coerceIn(0f, 1f)
                    }
                    dragging?.let(onSeek)
                    dragging = null
                }
            },
    ) {
        val pr = (dragging ?: progress).coerceIn(0f, 1f)
        val cy = size.height / 2f
        val stroke = 4.dp.toPx()
        val x = size.width * pr
        val gap = 6.dp.toPx()
        val trackStart = x + gap
        drawLine(track, Offset(trackStart.coerceAtMost(size.width), cy), Offset(size.width, cy), stroke, StrokeCap.Round)
        // no stray round-cap dot at the very start: the wave appears once there is a real length to draw
        val end = x - gap
        if (end > stroke) {
            // starts ON the wave (no vertical tick at x = 0) and the cap sits inside the bar (no stray pixel)
            fun waveY(px: Float) = cy + sin(px / 9.dp.toPx() + phase.value) * 3.dp.toPx() * amp
            val half = stroke / 2f
            val path = Path()
            var px = half
            path.moveTo(px, waveY(px))
            while (px < end - half) {
                px = (px + 2f).coerceAtMost(end - half)
                path.lineTo(px, waveY(px))
            }
            drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
        drawLine(color, Offset(x, cy - 12.dp.toPx()), Offset(x, cy + 12.dp.toPx()), 4.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun QueueSheet(onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    QSheet(onDismiss = onDismiss) {
        Text("Очередь", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(Player.queue.size) { i ->
                val t = Player.queue[i]
                val cur = i == Player.index
                Row(
                    Modifier.segment(i, Player.queue.size, if (cur) cs.secondaryContainer else cs.surfaceContainerHigh)
                        .clickable { Player.playQueue(Player.queue, i) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val b = rememberFileBitmap(t.cover, 160) ?: remember(t) { Images.mini(t.mini) }
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(cs.primaryContainer), contentAlignment = Alignment.Center) {
                        if (b != null) Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else Icon(Icons.Outlined.MusicNote, null)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(t.performer, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (cur && Player.playing) WavyRing(1f, true, cs.primary, Color.Transparent, Modifier.size(24.dp))
                }
            }
        }
    }
}
