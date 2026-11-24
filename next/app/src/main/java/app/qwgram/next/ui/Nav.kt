package app.qwgram.next.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp
import app.qwgram.next.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

sealed interface Route {
    data object Home : Route
    data class Chat(val chatId: Long) : Route
    data class Profile(val chatId: Long) : Route
    data object Me : Route
    data object Search : Route
    data object Archive : Route
    data class Page(val id: String) : Route
}

object Nav {
    /** one screen in the stack; [offset] is how far it is pushed off to the right (0 = in place, 1 = gone) */
    class Entry(val route: Route, val id: Long, entering: Boolean) {
        // tiny visibility threshold: the default (0.01 of the width ≈ 10 px) made the end of every slide "jump"
        val offset = Animatable(if (entering) 1f else 0f, visibilityThreshold = 0.0002f)
        /** finger position while dragging (-1 = not dragging); updated synchronously, no coroutine lag */
        var drag by androidx.compose.runtime.mutableFloatStateOf(-1f)
        val shown: Float get() = if (drag >= 0f) drag else offset.value
        var closing = false
        /** moved by a finger / back gesture at least once: from then on it always slides */
        var dragged = false
    }

    private var seq = 0L
    val stack = mutableStateListOf(Entry(Route.Home, seq++, false))
    val routes: List<Route> get() = stack.map { it.route }
    val top: Route get() = stack.last().route

    internal var scope: CoroutineScope? = null

    /** (chatId, messageId) to scroll to when that chat screen is shown (search results, media, profile) */
    var jumpTo: Pair<Long, Long>? = null

    fun push(r: Route) {
        if (stack.last().route == r) return
        // reopening a screen that is already in the stack returns to it instead of stacking a copy
        val i = stack.indexOfFirst { it.route == r && !it.closing }
        if (i >= 0) {
            while (stack.size > i + 1) stack.removeAt(stack.lastIndex)
        } else stack.add(Entry(r, seq++, entering = Prefs.motion.value != "off"))
    }

    /** animates the top screen away (smooth spring, no bounce), then drops it */
    fun pop(): Boolean {
        val e = stack.lastOrNull { !it.closing } ?: return false
        if (stack.indexOf(e) == 0) return false
        e.closing = true
        val s = scope
        if (s == null) { stack.remove(e); return true }
        s.launch {
            if (e.drag >= 0f) { e.offset.snapTo(e.drag); e.drag = -1f }
            e.offset.animateTo(1f, Springs.smooth())
            stack.remove(e)
        }
        return true
    }

    fun home() {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
}

/**
 * Screen stack, Telegram-style: the previous screen stays composed underneath (instant, live back),
 * the top one slides in on a smooth spring. Swipe right drags the top screen away — from anywhere in a chat,
 * from the left edge elsewhere — and the system predictive back gesture drives the same motion.
 */
@Composable
fun NavHost(content: @Composable (Route) -> Unit) {
    val holder = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    Nav.scope = scope
    val entries = Nav.stack.toList()
    val top = entries.last()

    PredictiveBackHandler(enabled = entries.size > 1) { events ->
        val e = Nav.stack.last()
        try {
            events.collect { ev -> if (Prefs.predictiveBack.value) { e.dragged = true; e.drag = ev.progress * 0.4f } }
            Nav.pop()
        } catch (c: CancellationException) {
            scope.launch { if (e.drag >= 0f) { e.offset.snapTo(e.drag); e.drag = -1f }; e.offset.animateTo(0f, Springs.smooth()) }
            throw c
        }
    }

    val corner = screenCornerRadius()
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val wpAll = appWallpaperOn()
    // surface (not black) behind the stack: fade/scale transitions never flash a dark half-screen
    Box(Modifier.fillMaxSize().background(surface)) {
        entries.takeLast(2).forEach { e ->
            key(e.id) {
                val isTop = e === top
                // entering screens start at offset 1; read once, never per frame in composition
                LaunchedEffect(e.id) { if (!e.closing && e.offset.targetValue == 1f) e.offset.animateTo(0f, Springs.smooth()) }
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val w = size.width
                            val mode = Prefs.screenAnim.value
                            // search animates itself (the button grows into the search bar): no slide/fade from the stack
                            val selfMorph = top.route is Route.Search && !top.dragged
                            if (isTop && selfMorph) {
                                alpha = if (e.closing) (1f - e.shown * 1.6f).coerceIn(0f, 1f) else 1f
                            } else if (!isTop && selfMorph) {
                                // the screen underneath stays put
                            } else if (isTop) {
                                val o = e.shown
                                val moving = o > 0.0005f && entries.size > 1
                                val faded = mode != "slide" && !e.closing && !e.dragged
                                if (faded) {
                                    // fade-through: the new screen appears once the old one has faded out
                                    alpha = ((0.65f - o) / 0.65f).coerceIn(0f, 1f)
                                    if (mode == "scale") { val sc = 0.92f + 0.08f * (1f - o); scaleX = sc; scaleY = sc }
                                } else {
                                    translationX = o * w
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(if (moving) corner else 0.dp)
                                    clip = moving
                                    shadowElevation = if (moving) 24.dp.toPx() else 0f
                                }
                            } else {
                                val t = top.shown
                                val topFaded = mode != "slide" && !top.closing && !top.dragged
                                if (topFaded) {
                                    alpha = ((t - 0.35f) / 0.65f).coerceIn(0f, 1f)
                                    if (mode == "scale") { val sc = 1f + 0.04f * (1f - t); scaleX = sc; scaleY = sc }
                                } else {
                                    // parallax of the screen underneath (shared-axis feel)
                                    translationX = -0.28f * w * (1f - t)
                                }
                            }
                        }
                        .then(if (wpAll || e.route is Route.Search) Modifier else Modifier.background(surface))
                        .then(if (isTop && entries.size > 1) Modifier.swipeBack(e, scope) else Modifier),
                ) {
                    if (wpAll) AppWallpaper()
                    // text/icons get the right colour everywhere, even on screens without a Surface
                    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides onSurface) {
                        holder.SaveableStateProvider(e.id) { content(e.route) }
                    }
                }
            }
        }
        // music: the mini player floats over every screen and grows into the full player
        PlayerHost()
        VideoViewerHost()
        PhotoEditorHost()
    }
}

/**
 * Swipe right anywhere closes the screen. Runs in the Main pass after the content: if a child already handles
 * the drag (slider, pager, swipe row, reply swipe), the event is consumed and the screen stays put.
 * The screen follows the finger; release past 22% or a flick closes it, otherwise it springs back.
 */
private fun Modifier.swipeBack(e: Nav.Entry, scope: CoroutineScope): Modifier = pointerInput(e.id) {
    val w = size.width.toFloat().coerceAtLeast(1f)
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
        if (SwipeGuard.blocked) return@awaitEachGesture                 // slider / pager owns this finger
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        var dx = 0f
        var dy = 0f
        var dragging = false
        while (true) {
            val ev = awaitPointerEvent(PointerEventPass.Main)
            val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
            if (!ch.pressed) break
            tracker.addPosition(ch.uptimeMillis, ch.position)
            val d = ch.positionChange()
            if (!dragging) {
                if (ch.isConsumed) return@awaitEachGesture          // a child took this drag
                dx += d.x; dy += d.y
                if (abs(dy) > slop && abs(dy) > abs(dx)) return@awaitEachGesture
                if (dx < -slop) return@awaitEachGesture
                if (dx > slop * 0.5f && dx > abs(dy) * 0.9f) { dragging = true; e.dragged = true } else continue
            }
            ch.consume()
            if (e.drag < 0f) e.drag = e.offset.value
            e.drag = (e.drag + d.x / w).coerceIn(0f, 1f)
        }
        if (dragging) {
            val vx = tracker.calculateVelocity().x
            if (e.drag > 0.16f || vx > 250f) Nav.pop()
            else scope.launch { e.offset.snapTo(e.drag); e.drag = -1f; e.offset.animateTo(0f, Springs.smooth()) }
        }
    }
}
