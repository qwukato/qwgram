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
        val offset = Animatable(if (entering) 1f else 0f)
        var closing = false
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
            events.collect { ev -> if (Prefs.predictiveBack.value) e.offset.snapTo(ev.progress * 0.4f) }
            Nav.pop()
        } catch (c: CancellationException) {
            scope.launch { e.offset.animateTo(0f, Springs.smooth()) }
            throw c
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
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
                            if (isTop) {
                                translationX = e.offset.value * w
                                shadowElevation = if (e.offset.value > 0.001f && entries.size > 1) 24.dp.toPx() else 0f
                            } else {
                                // parallax of the screen underneath (Material shared-axis feel)
                                val t = top.offset.value
                                translationX = -0.28f * w * (1f - t)
                                alpha = 0.85f + 0.15f * t
                            }
                        }
                        .background(MaterialTheme.colorScheme.surface)
                        .then(if (isTop && entries.size > 1) Modifier.swipeBack(e, scope, fullWidth = e.route !is Route.Page) else Modifier),
                ) {
                    holder.SaveableStateProvider(e.id) { content(e.route) }
                }
            }
        }
    }
}

/** horizontal drag to the right closes the screen; vertical or leftward drags are left to the content */
private fun Modifier.swipeBack(e: Nav.Entry, scope: CoroutineScope, fullWidth: Boolean): Modifier = pointerInput(e.id, fullWidth) {
    val w = size.width.toFloat().coerceAtLeast(1f)
    val edge = 48.dp.toPx()
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (!fullWidth && down.position.x > edge) return@awaitEachGesture
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        var dx = 0f
        var dy = 0f
        var dragging = false
        while (true) {
            val ev = awaitPointerEvent(PointerEventPass.Initial)
            val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
            if (!ch.pressed) break
            tracker.addPosition(ch.uptimeMillis, ch.position)
            val d = ch.positionChange()
            if (!dragging) {
                dx += d.x; dy += d.y
                if (abs(dy) > slop && abs(dy) > abs(dx)) return@awaitEachGesture
                if (dx < -slop) return@awaitEachGesture
                if (dx > slop && dx > abs(dy) * 1.2f) dragging = true else continue
            }
            ch.consume()
            val v = (e.offset.value + d.x / w).coerceIn(0f, 1f)
            scope.launch { e.offset.snapTo(v) }
        }
        if (dragging) {
            val vx = tracker.calculateVelocity().x
            if (e.offset.value > 0.22f || vx > 600f) Nav.pop()
            else scope.launch { e.offset.animateTo(0f, Springs.smooth()) }
        }
    }
}
