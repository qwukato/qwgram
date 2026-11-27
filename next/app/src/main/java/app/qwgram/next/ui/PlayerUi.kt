package app.qwgram.next.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.qwgram.next.Prefs
import app.qwgram.next.td.Images
import app.qwgram.next.td.Td
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.Texts
import app.qwgram.next.td.rememberFileBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/** 0 = mini player, 1 = full player: one value drives the whole container transform */
object PlayerUi {
    /** 0 = closed, 1 = the full player on screen (it slides up from the bottom) */
    val expand = Animatable(0f, visibilityThreshold = 0.0005f)
    /** what fills the stage of the full player: 0 the cover, 1 lyrics, 2 the queue */
    var panel by mutableIntStateOf(0)
    // the UI dispatcher carries a frame clock: animateTo needs one (a plain MainScope has none — that crashed)
    private val scope = kotlinx.coroutines.CoroutineScope(androidx.compose.ui.platform.AndroidUiDispatcher.Main + kotlinx.coroutines.SupervisorJob())
    // no bounce, a calm glide (the sheet is big: overshoot reads as a jolt)
    private val spec get() = if (Prefs.motion.value == "off") tween<Float>(0) else spring(1f, 240f * Prefs.motionSpeed.value * Prefs.motionSpeed.value, visibilityThreshold = 0.0005f)

    fun open() { scope.launch { expand.animateTo(1f, spec) } }
    fun close() { scope.launch { expand.animateTo(0f, spec); panel = 0 } }
    fun settle(open: Boolean, velocity: Float = 0f) { scope.launch { expand.animateTo(if (open) 1f else 0f, spec, initialVelocity = velocity); if (!open) panel = 0 } }
    // applied right away (no dispatch hop): the sheet sticks to the finger
    fun dragBy(fraction: Float) { scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { expand.snapTo((expand.value + fraction).coerceIn(0f, 1f)) } }
}

private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun lerpDp(a: Dp, b: Dp, t: Float) = a + (b - a) * t

/**
 * The mini player: its own block at the top of every screen (under the header), not a layer flying over them.
 * It slides in when music starts; closed, it folds away and everything below moves back up.
 */
@Composable
fun MiniPlayerBar(modifier: Modifier = Modifier) {
    val t = Player.track
    val holder = remember { arrayOfNulls<Track>(1) }
    if (t != null) holder[0] = t
    AnimatedVisibility(
        t != null && !Player.dismissed, modifier,
        enter = androidx.compose.animation.expandVertically(Springs.spatial(), expandFrom = Alignment.Top) + fadeIn(Springs.effects()),
        exit = androidx.compose.animation.shrinkVertically(Springs.exit(), shrinkTowards = Alignment.Top) + fadeOut(Springs.exit()),
    ) {
        val tr = Player.track ?: holder[0]
        if (tr != null) MiniPill(tr)
    }
}

/** the best cover there is right now: the picture inside the file, the album thumbnail, the blurry mini thumbnail */
@Composable
fun rememberTrackArt(t: Track, maxSide: Int = 1200): ImageBitmap? {
    val thumb = rememberFileBitmap(t.cover, maxSide)
    val alt = rememberFileBitmap(t.altCover, maxSide)
    val mini = remember(t.file.id) { Images.mini(t.mini) }
    val live = TdStore.files[t.file.id]
    LaunchedEffect(t.file.id, live?.local?.isDownloadingCompleted) { (live ?: t.file).let { if (it.local.isDownloadingCompleted) TrackArt.ensure(it) } }
    return TrackArt.art[t.file.id] ?: thumb ?: alt ?: mini
}

// ---------------------------------------------------------------- colours from the cover

private class PColors(
    val bgTop: Color, val bgBottom: Color, val accent: Color, val onAccent: Color,
    val container: Color, val onContainer: Color, val text: Color, val sub: Color,
)

private class Art(val blur: ImageBitmap, val hue: Float, val sat: Float)

private fun hsl(h: Float, s: Float, l: Float) = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))))

/** a tiny copy of the cover (drawn huge = a soft blur) and its main colour: vivid pixels count most */
private fun analyse(b: ImageBitmap): Art? = runCatching {
    val src = b.asAndroidBitmap().let { if (it.config == android.graphics.Bitmap.Config.HARDWARE) it.copy(android.graphics.Bitmap.Config.ARGB_8888, false) else it }
    val small = android.graphics.Bitmap.createScaledBitmap(src, 24, 24, true)
    var x = 0.0; var y = 0.0; var ws = 0.0; var sum = 0.0
    val hsv = FloatArray(3)
    for (i in 0 until 24) for (j in 0 until 24) {
        android.graphics.Color.colorToHSV(small.getPixel(i, j), hsv)
        val w = (hsv[1] * (1f - abs(hsv[2] - 0.6f))).toDouble().coerceAtLeast(0.0)
        x += kotlin.math.cos(Math.toRadians(hsv[0].toDouble())) * w
        y += kotlin.math.sin(Math.toRadians(hsv[0].toDouble())) * w
        ws += hsv[1] * w; sum += w
    }
    val hue = ((Math.toDegrees(kotlin.math.atan2(y, x)) + 360) % 360).toFloat()
    val sat = if (sum < 8.0) 0f else (ws / sum).toFloat()
    val blur = android.graphics.Bitmap.createScaledBitmap(src, 16, 16, true).asImageBitmap()
    Art(blur, hue, sat)
}.getOrNull()

@Composable
private fun playerColors(art: Art?): PColors {
    val cs = MaterialTheme.colorScheme
    val dark = cs.surface.luminance() < 0.5f
    val target = if (art == null) PColors(cs.surfaceContainer, cs.surface, cs.primary, cs.onPrimary, cs.secondaryContainer, cs.onSecondaryContainer, cs.onSurface, cs.onSurfaceVariant)
    else {
        val h = art.hue
        val s = art.sat.coerceIn(0f, 0.85f)
        val vivid = if (s < 0.08f) 0f else s.coerceAtLeast(0.45f)
        PColors(
            bgTop = hsl(h, s * 0.55f, if (dark) 0.17f else 0.88f),
            bgBottom = hsl(h, s * 0.3f, if (dark) 0.07f else 0.96f),
            accent = hsl(h, vivid, if (dark) 0.76f else 0.38f),
            onAccent = hsl(h, vivid * 0.6f, if (dark) 0.14f else 0.97f),
            container = hsl(h, s * 0.5f, if (dark) 0.25f else 0.84f),
            onContainer = hsl(h, s * 0.5f, if (dark) 0.9f else 0.16f),
            text = if (dark) Color(0xFFF4F1EE) else Color(0xFF1B1B1F),
            sub = (if (dark) Color(0xFFF4F1EE) else Color(0xFF1B1B1F)).copy(alpha = 0.68f),
        )
    }
    val spec = tween<Color>(650)
    val a by animateColorAsState(target.bgTop, spec, label = "c1")
    val b by animateColorAsState(target.bgBottom, spec, label = "c2")
    val c by animateColorAsState(target.accent, spec, label = "c3")
    val d by animateColorAsState(target.onAccent, spec, label = "c4")
    val e by animateColorAsState(target.container, spec, label = "c5")
    val f by animateColorAsState(target.onContainer, spec, label = "c6")
    return PColors(a, b, c, d, e, f, target.text, target.sub)
}

// ---------------------------------------------------------------- the full player (a sheet over everything)

/**
 * The full player: a sheet that slides up from the bottom over the app (opened from any mini player) and follows the
 * finger back down when you pull it (anywhere above the controls, or past the top of the queue / lyrics).
 * Inside: the cover swipes sideways between tracks (the neighbours peek in); lyrics and the queue take the
 * cover's place while it flies down into the title row.
 */
@Composable
fun PlayerHost() {
    val track = Player.track
    if (track == null || Player.dismissed) {
        LaunchedEffect(Unit) { PlayerUi.expand.snapTo(0f); PlayerUi.panel = 0 }
        return
    }
    val p = PlayerUi.expand.value
    BackHandler(enabled = PlayerUi.expand.targetValue > 0.5f) { if (PlayerUi.panel != 0) PlayerUi.panel = 0 else PlayerUi.close() }
    if (p <= 0f && !PlayerUi.expand.isRunning) return
    val density = LocalDensity.current
    val k = Prefs.motionSpeed.value * Prefs.motionSpeed.value
    val off = Prefs.motion.value == "off"

    val art = rememberTrackArt(track)
    val analysed by produceState<Art?>(null, art) { value = art?.let { withContext(Dispatchers.Default) { analyse(it) } } }
    val col = playerColors(analysed)
    val panelT by animateFloatAsState(if (PlayerUi.panel != 0) 1f else 0f, if (off) tween(0) else spring(0.82f, 380f * k), label = "panel")

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val top = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
        val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
        val range = with(density) { h.toPx() }.coerceAtLeast(1f)
        val radius = screenCornerRadius()

        // full layout: header · stage (cover / lyrics / queue) · title row · seek · transport · toolbar
        val stageTop = top + 72.dp
        val controlsTop = h - (318.dp + bottomInset)
        val stageH = (controlsTop - stageTop - 8.dp).coerceAtLeast(120.dp)
        val big = minOf(w - 104.dp, stageH)   // room for the carousel's side pills (16 + 28 + 8 each side)
        val bigX = (w - big) / 2
        val bigY = stageTop + (stageH - big) / 2
        val thumb = 56.dp
        val thumbX = 20.dp
        val thumbY = controlsTop + 8.dp

        // lists (queue, lyrics) hand an overscroll at their top to the sheet: it follows down and closes
        val conn = remember(range) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (PlayerUi.expand.value < 1f && available.y < 0f && source == NestedScrollSource.UserInput) {
                        PlayerUi.dragBy(-available.y / range); return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (available.y > 0f && source == NestedScrollSource.UserInput) { PlayerUi.dragBy(-available.y / range); return Offset(0f, available.y) }
                    return Offset.Zero
                }
                override suspend fun onPreFling(available: Velocity): Velocity {
                    val e = PlayerUi.expand.value
                    if (e < 0.999f) { PlayerUi.settle(!(available.y > 900f || e < 0.75f)); return available }
                    return Velocity.Zero
                }
            }
        }

        // the screen under it dims a little
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = p.coerceIn(0f, 1f) }.background(Color.Black.copy(alpha = 0.45f)))
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    translationY = (1f - p) * size.height
                    shape = RoundedCornerShape(radius); clip = true
                }
                .background(col.bgTop)
                .nestedScroll(conn)
                .pointerInput(range) {
                    // pull down ANYWHERE to close: watched before the children (Initial pass) and taken from them once
                    // it is clearly a downward drag (buttons then don't fire); the queue / lyrics lists close it
                    // through nested scroll instead, once they are at their top
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        val inLists = PlayerUi.panel != 0 && down.position.y > stageTop.toPx() && down.position.y < controlsTop.toPx()
                        val vt = VelocityTracker()
                        var y = 0f        // the finger's travel in screen terms (the sheet itself moves under it)
                        var acc = Offset.Zero
                        var mode = 0
                        vt.addPosition(down.uptimeMillis, Offset(0f, 0f))
                        while (true) {
                            val ev = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                            if (!c.pressed) break
                            val d = c.position - c.previousPosition
                            y += d.y
                            vt.addPosition(c.uptimeMillis, Offset(0f, y))
                            if (mode == 0) {
                                acc += d
                                if (abs(acc.y) > slop && abs(acc.y) > abs(acc.x) * 1.3f) {
                                    mode = if (acc.y > 0f && !inLists) 1 else -1
                                } else if (abs(acc.x) > slop) mode = -1
                                if (mode == -1) break
                            }
                            if (mode == 1) { c.consume(); PlayerUi.dragBy(-d.y / range) }
                        }
                        if (mode == 1) {
                            val v = vt.calculateVelocity().y
                            PlayerUi.settle(!(v > 700f || PlayerUi.expand.value < 0.7f), -v / range)
                        }
                    }
                },
        ) {
            // backdrop: the cover blown up into a soft haze, under a gradient of its colour
            Crossfade(analysed?.blur, animationSpec = tween(650), label = "bb") { b ->
                if (b != null) Image(b, null, Modifier.fillMaxSize().graphicsLayer { alpha = 0.55f; scaleX = 1.3f; scaleY = 1.3f }, contentScale = ContentScale.Crop, filterQuality = FilterQuality.Low)
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to col.bgTop.copy(alpha = 0.55f), 0.55f to col.bgBottom.copy(alpha = 0.86f), 1f to col.bgBottom)))
            // a grab handle at the top: pull down to close
            Box(Modifier.align(Alignment.TopCenter).padding(top = top + 2.dp).size(36.dp, 4.dp).clip(CircleShape).background(col.text.copy(alpha = 0.3f)))

            CompositionLocalProvider(LocalContentColor provides col.text) {
                FullHeader(track, col, top) { PlayerUi.close() }
                if (panelT > 0.01f || PlayerUi.panel != 0) Box(
                    Modifier.offset(y = stageTop).fillMaxWidth().height((controlsTop - stageTop).coerceAtLeast(0.dp))
                        .graphicsLayer { alpha = panelT; translationY = (1f - panelT) * 40.dp.toPx() },
                ) {
                    AnimatedContent(PlayerUi.panel.takeIf { it != 0 } ?: 1, transitionSpec = { (fadeIn(tween(220)) + scaleIn(initialScale = 0.96f)).togetherWith(fadeOut(tween(150))) }, label = "pn") { pn ->
                        if (pn == 2) QueuePanel(col) else LyricsPanel(track, col)
                    }
                }
                FullControls(track, col, controlsTop, thumb, panelT, bottomInset)
            }

            // the cover: the big square → a thumbnail in the title row (lyrics / queue)
            val full = panelT < 0.02f
            val cx = lerpDp(bigX, thumbX, panelT)
            val cy = lerpDp(bigY, thumbY, panelT)
            val cs2 = lerpDp(big, thumb, panelT)
            val bigR by animateDpAsState(if (Player.playing) 28.dp else 52.dp, spring(0.6f, 300f * k), label = "cr")
            val pausedScale by animateFloatAsState(if (Player.playing || !full) 1f else 0.9f, spring(0.55f, 260f * k), label = "cs")
            val coverR = lerpDp(bigR, 16.dp, panelT)
            // full width while it is the carousel; shrinks to the thumbnail's own box for lyrics / the queue
            val lw = lerpDp(w, cs2, panelT)
            val lx = lerpDp(0.dp, cx, panelT)
            CoverStack(
                track, full, cs2, col,
                Modifier.layout { m, _ ->
                    val ww = lw.roundToPx().coerceAtLeast(1)
                    val hh = cs2.roundToPx().coerceAtLeast(1)
                    val pl = m.measure(Constraints.fixed(ww, hh))
                    layout(ww, hh) { pl.place(lx.roundToPx(), cy.roundToPx()) }
                },
                shapeOf = { coverR },
                scale = { pausedScale },
                onTap = { if (PlayerUi.panel != 0) PlayerUi.panel = 0 },
            )
        }
    }
}

// ---------------------------------------------------------------- the cover

/**
 * Covers as an M3 carousel: the current cover big in the middle, the previous / next as narrow tall pills at the
 * edges. Drag sideways and the masks morph — the big one narrows into a pill while the neighbour widens into the
 * big cover (the picture stays put inside its mask, like Material 3's carousel). Let go past a third, flick, or tap
 * a side pill and that track plays.
 */
@Composable
private fun CoverStack(
    track: Track, swipeable: Boolean, size: Dp, col: PColors, modifier: Modifier,
    shapeOf: () -> Dp, scale: () -> Float, onTap: () -> Unit, small: Dp = 28.dp, gap: Dp = 8.dp,
) {
    val scope = rememberCoroutineScope()
    val dx = remember { Animatable(0f) }
    val density = LocalDensity.current
    val bigPx = with(density) { size.toPx() }
    val smallPx = with(density) { small.toPx() }
    val gapPx = with(density) { gap.toPx() }
    val step = bigPx / 2 + gapPx + smallPx / 2
    // the tracks around the current one (in play order), drawn from ONE loop keyed by the track
    val items = buildList {
        add(0 to track)
        if (swipeable) for (k in listOf(-2, -1, 1, 2)) {
            Player.peek(k)?.let { Player.queue.getOrNull(it) }?.takeIf { t -> t !== track && none { it.second === t } }?.let { add(k to it) }
        }
    }
    fun go(dir: Int) = scope.launch {
        if (Player.peek(dir) == null) { dx.animateTo(0f, spring(0.7f, 420f)); return@launch }
        dx.animateTo(-dir * step, spring(1f, 520f))
        if (dir > 0) Player.next() else Player.previous(force = true)
        dx.snapTo(0f)
    }
    BoxWithConstraints(
        modifier.pointerInput(swipeable, step) {
            if (!swipeable) { detectTapGestures { onTap() }; return@pointerInput }
            val slop = viewConfiguration.touchSlop
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val vt = VelocityTracker()
                var acc = Offset.Zero
                var horizontal = false
                var last = down.position
                while (true) {
                    val ev = awaitPointerEvent()
                    val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                    if (!c.pressed) break
                    val d = c.position - last
                    last = c.position
                    vt.addPosition(c.uptimeMillis, c.position)
                    if (!horizontal) {
                        acc += d
                        if (abs(acc.x) > slop && abs(acc.x) > abs(acc.y) * 1.2f) horizontal = true
                        else if (abs(acc.y) > slop) return@awaitEachGesture   // a vertical pull: the player's
                    }
                    if (horizontal) { c.consume(); scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { dx.snapTo((dx.value + d.x).coerceIn(-step * 1.2f, step * 1.2f)) } }
                }
                if (!horizontal) {
                    // a tap on a side pill plays it
                    val cx = this@pointerInput.size.width / 2f
                    when {
                        down.position.x < cx - bigPx / 2 -> go(-1)
                        down.position.x > cx + bigPx / 2 -> go(1)
                        else -> onTap()
                    }
                    return@awaitEachGesture
                }
                val v = vt.calculateVelocity().x
                val cur = dx.value
                if (abs(cur) > step * 0.3f || abs(v) > 900f) go(if (cur < 0) 1 else -1)
                else scope.launch { dx.animateTo(0f, spring(0.7f, 420f), initialVelocity = v) }
            }
        },
    ) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        items.forEach { (side, t) ->
            key(t.file.id) {
                CoverImage(t, col, Modifier
                    .layout { m, c ->
                        // position in the carousel: 0 = centre, ±1 = the side pills, ±2 = coming in from the edge
                        val pos = side + dx.value / step
                        val a = abs(pos)
                        val mw = when {
                            a <= 1f -> bigPx + (smallPx - bigPx) * a
                            else -> smallPx * (2f - a).coerceIn(0f, 1f)
                        }.coerceAtLeast(1f)
                        val dist = if (a <= 1f) step * a else step + (a - 1f) * (smallPx + gapPx)
                        val centre = wPx / 2f + (if (pos < 0) -dist else dist)
                        val pl = m.measure(Constraints.fixed(mw.roundToInt(), hPx.roundToInt().coerceAtLeast(1)))
                        layout(mw.roundToInt(), hPx.roundToInt()) { pl.place((centre - mw / 2f).roundToInt(), 0) }
                    }
                    .graphicsLayer {
                        val a = abs(side + dx.value / step).coerceAtMost(1f)
                        val r = shapeOf().toPx() + (smallPx / 2f - shapeOf().toPx()) * a
                        shape = RoundedCornerShape(r.coerceAtLeast(0f)); clip = true
                        val sc = if (side == 0) scale() else 1f
                        scaleX = sc; scaleY = sc
                    },
                    contentSize = size,
                )
            }
        }
    }
}

@Composable
private fun CoverImage(t: Track, col: PColors, modifier: Modifier, contentSize: Dp? = null) {
    val art = rememberTrackArt(t)
    Box(modifier.background(col.container), contentAlignment = Alignment.Center) {
        // in the carousel the picture keeps the big cover's size and the mask cuts it (it doesn't squash)
        val inner = if (contentSize != null) Modifier.requiredSize(contentSize) else Modifier.fillMaxSize()
        Crossfade(art, animationSpec = tween(400), modifier = inner, label = "art") { b ->
            if (b != null) Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, filterQuality = FilterQuality.High)
            else BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.MusicNote, null, tint = col.onContainer, modifier = Modifier.size(maxWidth * 0.42f))
            }
        }
    }
}

// ---------------------------------------------------------------- mini pill

/** the pill: a turning disc cover, the title, ⏮ ⏯ ⏭ (✕ while paused). Tap or pull up = the full player; sideways = next / previous */
@Composable
private fun MiniPill(track: Track) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val off = Prefs.motion.value == "off"
    val swipe = remember { Animatable(0f) }
    val density = LocalDensity.current
    val screenH = with(density) { androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val art = rememberTrackArt(track, 300)
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).height(64.dp)
            .graphicsLayer {
                translationX = swipe.value
                alpha = 1f - (abs(swipe.value) / (size.width * 0.9f)).coerceIn(0f, 0.6f)
                shape = CircleShape; clip = true
            }
            .background(cs.surfaceContainerHigh)
            .pointerInput(Unit) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val vt = VelocityTracker()
                    var mode = 0   // 1 = up (open the player), 2 = sideways (next / previous)
                    var acc = Offset.Zero
                    var last = down.position
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                        // a button inside took it (its tap ends consumed): never opens the player
                        if (mode == 0 && c.isConsumed) { mode = -1; break }
                        if (!c.pressed) break
                        vt.addPosition(c.uptimeMillis, c.position)
                        val d = c.position - last
                        last = c.position
                        if (mode == 0) {
                            acc += d
                            if (abs(acc.x) > slop && abs(acc.x) > abs(acc.y)) mode = 2
                            else if (acc.y < -slop) mode = 1
                            else if (acc.y > slop) { mode = -1; break }
                        }
                        when (mode) {
                            1 -> { c.consume(); PlayerUi.dragBy(-d.y / screenH) }
                            2 -> { c.consume(); scope.launch { swipe.snapTo(swipe.value + d.x) } }
                        }
                    }
                    val v = vt.calculateVelocity()
                    when (mode) {
                        0 -> PlayerUi.open()
                        1 -> PlayerUi.settle(v.y < -500f || PlayerUi.expand.value > 0.2f, -v.y / screenH)
                        2 -> scope.launch {
                            val dx = swipe.value
                            val w = size.width.toFloat()
                            if (abs(dx) > w * 0.28f || abs(v.x) > 1200f) {
                                if (dx < 0) Player.next() else Player.previous(force = true)
                                swipe.animateTo(if (dx < 0) -w else w, tween(140))
                                swipe.snapTo(if (dx < 0) w * 0.5f else -w * 0.5f)
                            }
                            swipe.animateTo(0f, spring(0.75f, 420f))
                        }
                    }
                }
            },
    ) {
        Row(Modifier.fillMaxSize().padding(start = 10.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(cs.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Outlined.MusicNote, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                AnimatedContent(track, transitionSpec = { (slideInHorizontally { it / 3 } + fadeIn()).togetherWith(slideOutHorizontally { -it / 3 } + fadeOut()) }, label = "mt") { t ->
                    Column {
                        Text(t.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = cs.onSurface)
                        if (t.performer.isNotBlank()) Text(t.performer, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = cs.onSurfaceVariant)
                    }
                }
            }
            MorphIconButton(Icons.Filled.SkipPrevious, "Предыдущий", { Player.previous() }, size = 40.dp, tint = cs.primary)
            // M3 wavy progress ring around a round play button: the wave flattens when paused, spins while loading
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                val amp by animateFloatAsState(if (Player.playing) 1f else 0f, tween(400), label = "ra")
                if (Player.loading) androidx.compose.material3.CircularWavyProgressIndicator(Modifier.size(52.dp), color = cs.primary, trackColor = cs.primary.copy(alpha = 0.15f))
                else androidx.compose.material3.CircularWavyProgressIndicator(
                    progress = { Player.progress }, modifier = Modifier.size(52.dp), color = cs.primary, trackColor = cs.primary.copy(alpha = 0.15f),
                    amplitude = { amp },
                )
                val pi = remember { MutableInteractionSource() }
                val pp by pi.collectIsPressedAsState()
                val sc by animateFloatAsState(if (pp) 0.88f else 1f, if (pp) Springs.pressIn() else Springs.morphRelease(), label = "ps")
                Box(
                    Modifier.size(34.dp).graphicsLayer { scaleX = sc; scaleY = sc }.clip(CircleShape).background(cs.primaryContainer)
                        .clickable(pi, indication = null) { Player.playPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(Player.playing, transitionSpec = { (scaleIn(spring(0.5f, 600f), initialScale = 0.4f) + fadeIn()).togetherWith(scaleOut(targetScale = 0.4f) + fadeOut()) }, label = "mp") { pl ->
                        Icon(if (pl) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (pl) "Пауза" else "Играть", tint = cs.onPrimaryContainer, modifier = Modifier.size(20.dp))
                    }
                }
            }
            MorphIconButton(Icons.Filled.SkipNext, "Следующий", { Player.next() }, size = 40.dp, tint = cs.primary)
            // close: only while paused (playing music isn't closed by a stray tap)
            AnimatedVisibility(!Player.playing && !Player.loading, enter = expandHorizontally(Springs.spatial()) + fadeIn(), exit = shrinkHorizontally(Springs.exit()) + fadeOut()) {
                MorphIconButton(Icons.Outlined.Close, "Закрыть", { Player.close() }, size = 40.dp, tint = cs.onSurfaceVariant)
            }
        }
    }
}

// ---------------------------------------------------------------- full player parts

@Composable
private fun FullHeader(track: Track, col: PColors, top: Dp, onCollapse: () -> Unit) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var forward by remember { mutableStateOf<TdApi.Message?>(null) }
    Row(Modifier.padding(top = top + 4.dp, start = 10.dp, end = 10.dp).fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
        MorphIconButton(Icons.Outlined.KeyboardArrowDown, "Свернуть", onCollapse, size = 48.dp, color = col.container, tint = col.onContainer)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Player.source.ifBlank { "Сейчас играет" }, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (Player.queue.size > 1) AnimatedContent(Player.index, transitionSpec = { (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut()) }, label = "ti") { i ->
                Text("Трек ${i + 1} из ${Player.queue.size}", style = MaterialTheme.typography.labelMedium, color = col.sub)
            }
        }
        Box {
            MorphIconButton(Icons.Outlined.MoreVert, "Ещё", { menu = true }, size = 48.dp, color = col.container, tint = col.onContainer)
            QMenu(menu, { menu = false }) {
                val scope = rememberCoroutineScope()
                if (track.chatId != 0L) {
                    DropdownMenuItem(text = { Text("К сообщению") }, leadingIcon = { Icon(Icons.Outlined.ChatBubbleOutline, null) }, onClick = {
                        menu = false
                        PlayerUi.close()
                        Nav.jumpTo = track.chatId to track.messageId
                        Nav.push(Route.Chat(track.chatId))
                    })
                    DropdownMenuItem(text = { Text("Переслать") }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Forward, null) }, onClick = {
                        menu = false
                        scope.launch { forward = Td.callOrNull(TdApi.GetMessage(track.chatId, track.messageId)) }
                    })
                }
                DropdownMenuItem(text = { Text("Поделиться") }, leadingIcon = { Icon(Icons.Outlined.Share, null) }, onClick = {
                    menu = false
                    Opener.share(ctx, track.file, track.audio?.mimeType ?: "audio/*")
                })
                DropdownMenuItem(text = { Text("Сохранить в «Музыку»") }, leadingIcon = { Icon(Icons.Outlined.LibraryMusic, null) }, onClick = {
                    menu = false
                    Toast.makeText(ctx, "Сохраняю…", Toast.LENGTH_SHORT).show()
                    Player.saveToMusic(track) { ok -> Toast.makeText(ctx, if (ok) "Сохранено в Музыка/qwgram" else "Не удалось сохранить", Toast.LENGTH_SHORT).show() }
                })
            }
        }
    }
    forward?.let { m -> ForwardSheet(listOf(m)) { forward = null } }
}

@Composable
private fun FullControls(track: Track, col: PColors, controlsTop: Dp, thumb: Dp, panelT: Float, bottomInset: Dp) {
    Column(Modifier.offset(y = controlsTop).fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = bottomInset + 16.dp)) {
        // title row: the cover lands at its start when lyrics / the queue are open
        Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width((thumb + 14.dp) * panelT.coerceIn(0f, 1f)))
            Column(Modifier.weight(1f)) {
                AnimatedContent(track, transitionSpec = { (slideInHorizontally(spring(0.8f, 380f)) { it / 4 } + fadeIn()).togetherWith(slideOutHorizontally { -it / 4 } + fadeOut()) }, label = "ft") { t ->
                    Column {
                        Text(t.title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black), maxLines = 1, modifier = Modifier.basicMarquee(initialDelayMillis = 2000))
                        Text(t.performer.ifBlank { "Неизвестный исполнитель" }, style = MaterialTheme.typography.titleMedium, color = col.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            ProfileHeart(track, col)
        }
        Spacer(Modifier.height(6.dp))
        var seeking by remember { mutableStateOf<Float?>(null) }
        WavySeekBar(Player.progress, Player.playing, col.accent, col.text.copy(alpha = 0.2f), Modifier.fillMaxWidth().height(40.dp).noSwipeBack(), onDrag = { seeking = it }) { Player.seekTo(it) }
        val dur = maxOf(Player.durationMs / 1000, track.durationSec)
        Row(Modifier.fillMaxWidth()) {
            val pos = seeking?.let { (it * dur).toInt() } ?: (Player.positionMs / 1000)
            Text(Texts.duration(pos), style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (seeking != null) FontWeight.Bold else null), color = if (seeking != null) col.accent else col.sub)
            Spacer(Modifier.weight(1f))
            Text("−" + Texts.duration((dur - pos).coerceAtLeast(0)), style = MaterialTheme.typography.labelMedium, color = col.sub)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ToggleTool(Icons.Outlined.Shuffle, "Перемешать", Player.shuffle, col) { Player.shuffle = !Player.shuffle }
            Spacer(Modifier.width(8.dp))
            TransportGroup(col, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            ToggleTool(if (Player.repeat == 2) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat, "Повтор", Player.repeat != 0, col) { Player.repeat = (Player.repeat + 1) % 3 }
        }
        Spacer(Modifier.height(18.dp))
        Toolbar(col)
    }
}

/** the heart: the track in your profile's music (Telegram), bounces when it changes */
@Composable
private fun ProfileHeart(track: Track, col: PColors) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var on by remember(track.file.id) { mutableStateOf(false) }
    LaunchedEffect(track.file.id) { on = Td.callOrNull(TdApi.IsProfileAudio(track.file.id)) != null }
    val pop = remember { Animatable(1f) }
    MorphBox(
        onClick = {
            val now = !on
            on = now
            scope.launch { pop.snapTo(0.7f); pop.animateTo(1f, spring(0.35f, 500f)) }
            val f = Images.current(track.file) ?: track.file
            if (now) {
                val remote = f.remote?.id.orEmpty()
                if (remote.isEmpty()) { on = false; return@MorphBox }
                Td.send(TdApi.AddProfileAudio(TdApi.InputAudio(TdApi.InputFileRemote(remote), null, track.durationSec, track.title, track.performer))) { r ->
                    if (r is TdApi.Error) android.os.Handler(android.os.Looper.getMainLooper()).post { on = false; Toast.makeText(ctx, "Не удалось: ${r.message}", Toast.LENGTH_SHORT).show() }
                }
            } else Td.send(TdApi.RemoveProfileAudio(f.id))
        },
        rest = 24.dp, pressed = 14.dp, modifier = Modifier.size(48.dp),
        color = if (on) col.accent.copy(alpha = 0.18f) else Color.Transparent, contentColor = if (on) col.accent else col.text,
    ) {
        Icon(if (on) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder, if (on) "Убрать из профиля" else "В профиль",
            Modifier.size(26.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value })
    }
}

/**
 * previous · PLAY · next as one connected M3 button group: the pressed button widens and its neighbours make way,
 * the play button is a full pill while paused and a squarer, wider shape while playing.
 */
@Composable
private fun TransportGroup(col: PColors, modifier: Modifier) {
    val k = Prefs.motionSpeed.value * Prefs.motionSpeed.value
    val iPrev = remember { MutableInteractionSource() }
    val iPlay = remember { MutableInteractionSource() }
    val iNext = remember { MutableInteractionSource() }
    val pPrev by iPrev.collectIsPressedAsState()
    val pPlay by iPlay.collectIsPressedAsState()
    val pNext by iNext.collectIsPressedAsState()
    val sp = spring<Float>(0.55f, 650f * k)
    val wPrev by animateFloatAsState(if (pPrev) 1.3f else if (pPlay || pNext) 0.88f else 1f, sp, label = "w1")
    val wPlay by animateFloatAsState((if (Player.playing) 1.9f else 1.55f) + (if (pPlay) 0.3f else if (pPrev || pNext) -0.1f else 0f), sp, label = "w2")
    val wNext by animateFloatAsState(if (pNext) 1.3f else if (pPlay || pPrev) 0.88f else 1f, sp, label = "w3")
    val dsp = spring<Dp>(0.6f, 500f * k)
    val inner = 12.dp
    val full = 38.dp
    val rPrevIn by animateDpAsState(if (pPrev) full else inner, dsp, label = "r1")
    val rNextIn by animateDpAsState(if (pNext) full else inner, dsp, label = "r2")
    val rPlay by animateDpAsState(if (pPlay) full else if (Player.playing) 20.dp else full, dsp, label = "r3")
    Row(modifier.height(76.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Seg(Modifier.weight(wPrev), RoundedCornerShape(full, rPrevIn, rPrevIn, full), col.container, iPrev, { Player.previous() }) {
            Icon(Icons.Filled.SkipPrevious, "Предыдущий", Modifier.size(30.dp), tint = col.onContainer)
        }
        Seg(Modifier.weight(wPlay), RoundedCornerShape(rPlay), col.accent, iPlay, { Player.playPause() }) {
            if (Player.loading) androidx.compose.material3.LoadingIndicator(Modifier.size(40.dp), color = col.onAccent)
            else AnimatedContent(Player.playing, transitionSpec = { (scaleIn(spring(0.5f, 600f), initialScale = 0.3f) + fadeIn()).togetherWith(scaleOut(targetScale = 0.3f) + fadeOut()) }, label = "pi") { pl ->
                Icon(if (pl) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (pl) "Пауза" else "Играть", Modifier.size(38.dp), tint = col.onAccent)
            }
        }
        Seg(Modifier.weight(wNext), RoundedCornerShape(rNextIn, full, full, rNextIn), col.container, iNext, { Player.next() }) {
            Icon(Icons.Filled.SkipNext, "Следующий", Modifier.size(30.dp), tint = col.onContainer)
        }
    }
}

@Composable
private fun Seg(modifier: Modifier, shape: RoundedCornerShape, color: Color, i: MutableInteractionSource, onClick: () -> Unit, content: @Composable () -> Unit) {
    val pressed by i.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.96f else 1f, if (pressed) Springs.pressIn() else Springs.morphRelease(), label = "sq")
    Box(
        modifier.fillMaxHeight().graphicsLayer { scaleY = sc; this.shape = shape; clip = true }.background(color).clickable(i, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** a round toggle (M3 toggle button): the same tonal colour as the other buttons when off, the accent when on — the shape squares up a little when it switches on */
@Composable
private fun ToggleTool(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, on: Boolean, col: PColors, onClick: () -> Unit) {
    val i = remember { MutableInteractionSource() }
    val pressed by i.collectIsPressedAsState()
    val bg by animateColorAsState(if (on) col.accent else col.container, tween(220), label = "tb")
    val fg by animateColorAsState(if (on) col.onAccent else col.onContainer, tween(220), label = "tf")
    val r by animateDpAsState(if (pressed) 12.dp else if (on) 16.dp else 24.dp, spring(0.55f, 600f), label = "tr")
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(r)).background(bg).clickable(i, indication = androidx.compose.material3.ripple(), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(24.dp), tint = fg) }
}

/** the floating toolbar: speed · sleep timer · lyrics · queue */
@Composable
private fun Toolbar(col: PColors) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Row(
            Modifier.clip(CircleShape).background(col.container.copy(alpha = 0.7f)).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // speed: tap cycles, the number flips
            MorphBox(onClick = { Player.cycleSpeed() }, rest = 24.dp, pressed = 14.dp, modifier = Modifier.height(48.dp).width(64.dp),
                color = if (Player.speed != 1f) col.accent else Color.Transparent, contentColor = if (Player.speed != 1f) col.onAccent else col.onContainer) {
                AnimatedContent(Player.speed, transitionSpec = { (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut()) }, label = "sp") { s ->
                    Text((if (s % 1f == 0f) s.toInt().toString() else s.toString()) + "×", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
            SleepButton(col)
            ToolToggle(Icons.Outlined.Lyrics, "Текст", PlayerUi.panel == 1, col) { PlayerUi.panel = if (PlayerUi.panel == 1) 0 else 1 }
            ToolToggle(Icons.AutoMirrored.Outlined.QueueMusic, "Очередь", PlayerUi.panel == 2, col) { PlayerUi.panel = if (PlayerUi.panel == 2) 0 else 2 }
        }
    }
}

@Composable
private fun ToolToggle(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, on: Boolean, col: PColors, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) col.accent else Color.Transparent, label = "ttb")
    val fg by animateColorAsState(if (on) col.onAccent else col.onContainer, label = "ttf")
    val w by animateDpAsState(if (on) 64.dp else 48.dp, Springs.fastSpatial(), label = "ttw")
    MorphBox(onClick = onClick, rest = 24.dp, pressed = 14.dp, modifier = Modifier.height(48.dp).width(w), color = bg, contentColor = fg) {
        Icon(icon, desc, Modifier.size(24.dp))
    }
}

@Composable
private fun SleepButton(col: PColors) {
    var menu by remember { mutableStateOf(false) }
    val on = Player.sleepAt != 0L
    var left by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Player.sleepAt) { while (Player.sleepAt > 0) { left = (Player.sleepAt - System.currentTimeMillis()).coerceAtLeast(0); delay(1000) } }
    Box {
        MorphBox(onClick = { menu = true }, rest = 24.dp, pressed = 14.dp, modifier = Modifier.height(48.dp).width(if (on) 84.dp else 48.dp),
            color = if (on) col.accent else Color.Transparent, contentColor = if (on) col.onAccent else col.onContainer) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Bedtime, "Таймер сна", Modifier.size(22.dp))
                if (on) {
                    Spacer(Modifier.width(4.dp))
                    Text(if (Player.sleepAt < 0) "трек" else "${(left / 60_000) + 1}м", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        QMenu(menu, { menu = false }) {
            listOf(15 to "15 минут", 30 to "30 минут", 45 to "45 минут", 60 to "1 час", -1 to "До конца трека", 0 to "Выключить").forEach { (m, t) ->
                if (m != 0 || on) DropdownMenuItem(text = { Text(t) }, onClick = { menu = false; Player.sleep(m) })
            }
        }
    }
}

// ---------------------------------------------------------------- lyrics

@Composable
private fun LyricsPanel(track: Track, col: PColors) {
    val res by produceState<Lyrics.Result?>(null, track.file.id) { value = null; value = Lyrics.load(track) }
    val r = res
    Box(Modifier.fillMaxSize().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        when {
            r == null -> androidx.compose.material3.LoadingIndicator(Modifier.size(56.dp), color = col.accent)
            r.synced.isNotEmpty() -> SyncedLyrics(r.synced, col)
            !r.plain.isNullOrBlank() -> Text(
                r.plain, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, lineHeight = MaterialTheme.typography.titleLarge.lineHeight * 1.15f),
                color = col.text, modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
            )
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(96.dp).clip(remember { MorphShape(androidx.graphics.shapes.Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Cookie9Sided), 0f) }).background(col.container), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Lyrics, null, Modifier.size(40.dp), tint = col.onContainer)
                }
                Spacer(Modifier.height(16.dp))
                Text("Текст не найден", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = col.text)
                Spacer(Modifier.height(4.dp))
                Text("Для этого трека нет текста в LRCLIB", style = MaterialTheme.typography.bodyMedium, color = col.sub, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SyncedLyrics(lines: List<Pair<Int, String>>, col: PColors) {
    val state = rememberLazyListState()
    val dragged by state.interactionSource.collectIsDraggedAsState()
    var lastTouch by remember { mutableLongStateOf(0L) }
    if (dragged) lastTouch = System.currentTimeMillis()
    val pos = Player.positionMs + 250
    val cur = lines.indexOfLast { it.first <= pos }.coerceAtLeast(0)
    LaunchedEffect(cur) {
        // follows the song unless you scrolled in the last 3 s
        if (System.currentTimeMillis() - lastTouch > 3000) state.animateScrollToItem(cur)
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pad = maxHeight * 0.32f
        LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = pad, bottom = (maxHeight - pad - 40.dp).coerceAtLeast(0.dp))) {
            itemsIndexed(lines) { i, (t, line) ->
                val active = i == cur
                val a by animateFloatAsState(if (active) 1f else if (i < cur) 0.32f else 0.5f, tween(300), label = "la")
                val s by animateFloatAsState(if (active) 1f else 0.94f, spring(0.6f, 400f), label = "ls")
                Text(
                    line.ifBlank { "♪" },
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = col.text,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { Player.seekMs(t); lastTouch = 0L }
                        .graphicsLayer { alpha = a; scaleX = s; scaleY = s; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- queue

@Composable
private fun QueuePanel(col: PColors) {
    var query by remember { mutableStateOf("") }
    var sortMenu by remember { mutableStateOf(false) }
    val state = rememberLazyListState()
    LaunchedEffect(Unit) { state.scrollToItem((Player.index - 2).coerceAtLeast(0)) }
    val density = LocalDensity.current
    val rowPx = with(density) { 66.dp.toPx() }
    var dragKey by remember { mutableStateOf<Int?>(null) }
    var dragOff by remember { mutableFloatStateOf(0f) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).height(48.dp).clip(CircleShape).background(col.container.copy(alpha = 0.7f)).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = col.onContainer)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Поиск в очереди", style = MaterialTheme.typography.bodyLarge, color = col.onContainer.copy(alpha = 0.6f))
                    BasicTextField(query, { query = it }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = col.onContainer), cursorBrush = SolidColor(col.accent), modifier = Modifier.fillMaxWidth())
                }
                if (query.isNotEmpty()) Icon(Icons.Outlined.Close, "Очистить", Modifier.size(20.dp).clip(CircleShape).clickable { query = "" }, tint = col.onContainer)
            }
            Spacer(Modifier.width(6.dp))
            Box {
                MorphIconButton(Icons.Outlined.SwapVert, "Сортировка", { sortMenu = true }, size = 48.dp, color = col.container.copy(alpha = 0.7f), tint = col.onContainer)
                QMenu(sortMenu, { sortMenu = false }) {
                    DropdownMenuItem(text = { Text("По названию") }, onClick = { sortMenu = false; Player.sortQueue(1) })
                    DropdownMenuItem(text = { Text("По исполнителю") }, onClick = { sortMenu = false; Player.sortQueue(2) })
                    DropdownMenuItem(text = { Text("Перемешать") }, leadingIcon = { Icon(Icons.Outlined.Shuffle, null) }, onClick = { sortMenu = false; Player.sortQueue(0) })
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        val q = Player.queue
        val shown = if (query.isBlank()) q.indices.toList() else q.indices.filter { (q[it].title + " " + q[it].performer).contains(query.trim(), ignoreCase = true) }
        LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            itemsIndexed(shown, key = { _, i -> System.identityHashCode(q[i]) }) { n, i ->
                val t = q[i]
                val kk = System.identityHashCode(t)
                val cur = i == Player.index
                val dragging = dragKey == kk
                Row(
                    Modifier
                        .then(if (dragging) Modifier.zIndex(1f).graphicsLayer { translationY = dragOff; shadowElevation = 8.dp.toPx(); shape = RoundedCornerShape(18.dp); clip = true } else Modifier.animateItem())
                        .segment(n, shown.size, if (cur || dragging) col.container else col.container.copy(alpha = 0.32f))
                        .clickable { Player.playAt(i) }.height(64.dp).padding(start = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))) {
                        CoverImage(t, col, Modifier.fillMaxSize())
                        if (cur) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                            EqBars(Player.playing, Color.White, Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (cur) FontWeight.Bold else null), color = if (cur) col.accent else col.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(t.performer.ifBlank { "—" }, style = MaterialTheme.typography.bodySmall, color = col.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(Texts.duration(t.durationSec), style = MaterialTheme.typography.labelMedium, color = col.sub)
                    // drag the handle to move the track (only in the full, unfiltered queue)
                    if (query.isBlank()) Box(
                        Modifier.size(44.dp).pointerInput(kk) {
                            detectVerticalDragGestures(
                                onDragStart = { dragKey = kk; dragOff = 0f },
                                onDragEnd = { dragKey = null; dragOff = 0f },
                                onDragCancel = { dragKey = null; dragOff = 0f },
                            ) { c, dy ->
                                c.consume()
                                dragOff += dy
                                val at = Player.queue.indexOfFirst { System.identityHashCode(it) == kk }
                                if (dragOff > rowPx / 2 && at < Player.queue.lastIndex) { Player.move(at, at + 1); dragOff -= rowPx }
                                else if (dragOff < -rowPx / 2 && at > 0) { Player.move(at, at - 1); dragOff += rowPx }
                            }
                        },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.DragHandle, "Переместить", tint = col.sub) }
                    else Spacer(Modifier.width(12.dp))
                }
            }
        }
    }
}

/** three little bars that bounce while the track plays */
@Composable
private fun EqBars(playing: Boolean, color: Color, modifier: Modifier) {
    val ph = loopPhase(playing, 900, (2 * PI).toFloat())
    Canvas(modifier) {
        val bw = size.width / 5f
        for (j in 0 until 3) {
            val f = if (playing) 0.35f + 0.65f * ((sin(ph.value + j * 2.1f) + 1f) / 2f) else 0.35f
            val hh = size.height * f
            drawRoundRect(color, Offset(bw * (j * 2), size.height - hh), androidx.compose.ui.geometry.Size(bw, hh), androidx.compose.ui.geometry.CornerRadius(bw / 2))
        }
    }
}

// ---------------------------------------------------------------- shared pieces (also used by the video player, profile music)

/** play / pause: a circle while paused, a squircle while playing (M3 Expressive), the icon spins across */
@Composable
fun PlayMorphButton(playing: Boolean, size: Dp, bg: Color, fg: Color, wide: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val r by animateDpAsState(if (playing) size * 0.3f else size / 2, spring(0.5f, 420f), label = "pr")
    val sc by animateFloatAsState(if (pressed) 0.9f else 1f, if (pressed) Springs.pressIn() else Springs.morphRelease(), label = "ps")
    val width by animateDpAsState(if (wide && playing) size * 1.25f else size, spring(0.6f, 420f), label = "pw")
    Box(
        Modifier.size(width, size).graphicsLayer { scaleX = sc; scaleY = sc; shape = RoundedCornerShape(r.coerceAtLeast(0.dp)); clip = true }
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
    val phase = loopPhase(playing || amp > 0.01f, 1400, (2 * PI).toFloat())
    Canvas(modifier) {
        val stroke = 3.dp.toPx()
        val r = size.minDimension / 2f - stroke * 1.6f
        val c = center
        drawCircle(track, r, c, style = Stroke(stroke))
        val sweep = (progress.coerceIn(0f, 1f) * 2 * PI).toFloat()
        if (sweep * r < stroke * 2f) return@Canvas
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

/**
 * M3 Expressive wavy seek bar: the played part is a moving wave (it flattens when paused or while you drag),
 * the rest a flat line, a tall thumb between that grows while held.
 */
@Composable
fun WavySeekBar(progress: Float, playing: Boolean, color: Color, track: Color, modifier: Modifier, onDrag: (Float?) -> Unit = {}, onSeek: (Float) -> Unit) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val amp by animateFloatAsState(if (playing && dragging == null) 1f else 0f, spring(0.7f, 300f), label = "a")
    val thumbH by animateDpAsState(if (dragging != null) 32.dp else 22.dp, spring(0.5f, 600f), label = "th")
    val phase = loopPhase(amp > 0.01f || (playing && dragging == null), 1600, (2 * PI).toFloat())
    Canvas(
        modifier
            .pointerInput(Unit) { detectTapGestures { onSeek((it.x / size.width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var moved = false
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!c.pressed) break
                        if (!moved && abs(c.position.x - down.position.x) < viewConfiguration.touchSlop / 2) continue
                        moved = true
                        c.consume()
                        dragging = (c.position.x / size.width).coerceIn(0f, 1f)
                        onDrag(dragging)
                    }
                    dragging?.let(onSeek)
                    dragging = null
                    onDrag(null)
                }
            },
    ) {
        val pr = (dragging ?: progress).coerceIn(0f, 1f)
        val cy = size.height / 2f
        val stroke = 5.dp.toPx()
        val x = size.width * pr
        val gap = 7.dp.toPx()
        val trackStart = x + gap
        if (trackStart < size.width) drawLine(track, Offset(trackStart, cy), Offset(size.width, cy), stroke, StrokeCap.Round)
        val end = x - gap
        if (end > stroke) {
            fun waveY(px: Float) = cy + sin(px / 8.dp.toPx() + phase.value) * 3.5.dp.toPx() * amp
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
        val th = thumbH.toPx()
        drawLine(color, Offset(x, cy - th / 2), Offset(x, cy + th / 2), 5.dp.toPx(), StrokeCap.Round)
    }
}

