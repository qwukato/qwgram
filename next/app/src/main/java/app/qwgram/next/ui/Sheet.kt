package app.qwgram.next.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import app.qwgram.next.Prefs
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** what a [QSheet]'s content and overlay can do */
class QSheetScope internal constructor(val close: () -> Unit, val expand: () -> Unit, val progress: () -> Float)

/**
 * qwgram's bottom sheet (Telegram behaviour, M3 look): opens half-way; dragging the handle or scrolling the list up
 * lifts it to full height first and only then scrolls the list; at the top of the list, dragging down lowers the
 * sheet; a fling settles it at full / half / closed. One smooth, non-bouncy spring, no hiccups between the sheet
 * and the list. [overlay] is drawn over everything at the bottom of the screen (e.g. a "Send" button).
 */
@Composable
fun QSheet(
    onDismiss: () -> Unit,
    halfFirst: Boolean = true,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    overlay: @Composable BoxScope.(QSheetScope) -> Unit = {},
    content: @Composable ColumnScope.(QSheetScope) -> Unit,
) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false, dismissOnBackPress = false)) {
        // our own scrim (animated with the sheet) instead of the window's instant dim
        val view = LocalView.current
        SideEffect { (view.parent as? DialogWindowProvider)?.window?.let { w -> w.setDimAmount(0f); w.setWindowAnimations(0) } }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val H = constraints.maxHeight.toFloat()
            val fullY = WindowInsets.statusBars.getTop(density) + with(density) { 8.dp.toPx() }
            val halfY = if (halfFirst) (H * 0.40f).coerceAtLeast(fullY) else fullY
            val k = Prefs.motionSpeed.value * Prefs.motionSpeed.value
            var off by remember { mutableFloatStateOf(H) }
            val scope = rememberCoroutineScope()
            var job by remember { mutableStateOf<Job?>(null) }
            var closing by remember { mutableStateOf(false) }

            fun animateTo(target: Float, velocity: Float = 0f, then: () -> Unit = {}) {
                job?.cancel()
                job = scope.launch {
                    animate(off, target, velocity, spring(dampingRatio = 1f, stiffness = 520f * k, visibilityThreshold = 0.5f)) { v, _ -> off = v }
                    then()
                }
            }
            fun close() { if (closing) return; closing = true; animateTo(H) { onDismiss() } }
            fun expand() { if (!closing) animateTo(fullY) }
            fun settle(v: Float) {
                val anchors = listOf(fullY, halfY, H).distinct().sorted()
                val target = when {
                    v < -900f -> anchors.lastOrNull { it < off - 1f } ?: anchors.first()
                    v > 900f -> anchors.firstOrNull { it > off + 1f } ?: anchors.last()
                    else -> anchors.minBy { kotlin.math.abs(it - off) }
                }
                if (target >= H - 1f) close() else animateTo(target, v)
            }
            LaunchedEffect(Unit) { animateTo(halfY) }
            androidx.activity.compose.BackHandler { close() }

            val conn = remember(fullY, H) {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        // finger up: lift the sheet to full height before the list scrolls
                        if (available.y < 0f && off > fullY + 0.5f && !closing) {
                            job?.cancel()
                            val d = maxOf(available.y, fullY - off)
                            off += d
                            return Offset(0f, d)
                        }
                        return Offset.Zero
                    }
                    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                        // the list is at its top and the finger goes down: the sheet follows
                        if (available.y > 0f && source == NestedScrollSource.UserInput && !closing) {
                            job?.cancel()
                            off = (off + available.y).coerceAtMost(H)
                            return Offset(0f, available.y)
                        }
                        return Offset.Zero
                    }
                    override suspend fun onPreFling(available: Velocity): Velocity {
                        if (off > fullY + 0.5f && !closing) { settle(available.y); return available }
                        return Velocity.Zero
                    }
                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                        if (off > fullY + 0.5f && !closing) { settle(available.y); return available }
                        return Velocity.Zero
                    }
                }
            }
            val sheetScope = remember { QSheetScope({ close() }, { expand() }, { ((H - off) / (H - halfY).coerceAtLeast(1f)).coerceIn(0f, 1f) }) }
            val progress = ((H - off) / (H - halfY).coerceAtLeast(1f)).coerceIn(0f, 1f)
            // the app behind the sheet frosts as it rises (Appearance → Blur → windows)
            WindowBlurBehind { ((H - off) / (H - halfY).coerceAtLeast(1f)).coerceIn(0f, 1f) }
            // scrim
            Box(
                Modifier.fillMaxSize().graphicsLayer { alpha = progress }.background(Color.Black.copy(alpha = if (blurOn(Prefs.blurWindows)) 0.24f else 0.42f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() },
            )
            val sheetH = with(density) { (H - fullY).toDp() }
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp.rs(), topEnd = 28.dp.rs()), color = color,
                modifier = Modifier.offset { IntOffset(0, off.toInt()) }.fillMaxWidth().height(sheetH).nestedScroll(conn),
            ) {
                Column(Modifier.fillMaxSize()) {
                    // handle: drag the sheet itself
                    Box(
                        Modifier.fillMaxWidth().height(28.dp)
                            .draggable(
                                rememberDraggableState { d -> if (!closing) { job?.cancel(); off = (off + d).coerceIn(fullY, H) } },
                                Orientation.Vertical,
                                onDragStopped = { v -> if (!closing) settle(v) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
                    }
                    content(sheetScope)
                }
            }
            Box(Modifier.fillMaxSize().padding(top = 0.dp)) { overlay(sheetScope) }
        }
    }
}
