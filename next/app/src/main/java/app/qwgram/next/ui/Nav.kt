package app.qwgram.next.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.qwgram.next.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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
    val stack = mutableStateListOf<Route>(Route.Home)
    var forward = true
        private set

    fun push(r: Route) {
        if (stack.lastOrNull() == r) return
        // reopening a chat that is already in the stack returns to it instead of stacking a copy
        val i = stack.indexOf(r)
        forward = true
        if (i >= 0) {
            while (stack.size > i + 1) stack.removeAt(stack.size - 1)
        } else stack.add(r)
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        forward = false
        stack.removeAt(stack.size - 1)
        return true
    }

    fun home() {
        forward = false
        while (stack.size > 1) stack.removeAt(stack.size - 1)
    }
}

/**
 * Screen stack with Material shared-axis X transitions and the M3 predictive back gesture:
 * while the user drags back the screen shrinks toward the gesture edge with rounded corners.
 */
@Composable
fun NavHost(content: @Composable (Route) -> Unit) {
    val holder = rememberSaveableStateHolder()
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeEdgeLeft = remember { booleanArrayOf(true) }
    val motion = MaterialTheme.motionScheme

    PredictiveBackHandler(enabled = Nav.stack.size > 1) { events ->
        try {
            events.collect { e ->
                swipeEdgeLeft[0] = e.swipeEdge == 0
                if (Prefs.predictiveBack.value) progress.snapTo(e.progress)
            }
            Nav.pop()
            progress.snapTo(0f)
        } catch (e: CancellationException) {
            scope.launch { progress.animateTo(0f) }
            throw e
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        val entries = Nav.stack.toList()
        AnimatedContent(
            targetState = entries.size to entries.last(),
            transitionSpec = {
                val fwd = targetState.first >= initialState.first
                val dist = with(density) { 96.dp.roundToPx() }
                val spec: ContentTransform = if (fwd) {
                    (slideInHorizontally(motion.defaultSpatialSpec<IntOffset>()) { dist } + fadeIn(motion.defaultEffectsSpec<Float>()))
                        .togetherWith(slideOutHorizontally(motion.defaultSpatialSpec<IntOffset>()) { -dist / 3 } + fadeOut(motion.fastEffectsSpec<Float>()))
                } else {
                    (slideInHorizontally(motion.defaultSpatialSpec<IntOffset>()) { -dist / 3 } + fadeIn(motion.defaultEffectsSpec<Float>()))
                        .togetherWith(slideOutHorizontally(motion.defaultSpatialSpec<IntOffset>()) { dist } + fadeOut(motion.fastEffectsSpec<Float>()))
                }
                spec.also { it.targetContentZIndex = targetState.first.toFloat() }
            },
            label = "nav",
        ) { (size, route) ->
            val isTop = size == Nav.stack.size
            val p = if (isTop) progress.value else 0f
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (p > 0f) {
                            val s = 1f - 0.1f * p
                            scaleX = s
                            scaleY = s
                            translationX = (if (swipeEdgeLeft[0]) 1f else -1f) * 24.dp.toPx() * p
                            transformOrigin = TransformOrigin(if (swipeEdgeLeft[0]) 1f else 0f, 0.5f)
                            shape = RoundedCornerShape((32 * p).dp)
                            clip = true
                        } else {
                            shape = RectangleShape
                            clip = false
                        }
                    }
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                holder.SaveableStateProvider("$size:$route") { content(route) }
            }
        }
    }
}
