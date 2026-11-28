package app.qwgram.next.ui

import androidx.compose.ui.input.nestedscroll.nestedScroll

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import app.qwgram.next.Power
import app.qwgram.next.Prefs
import kotlinx.coroutines.delay

/**
 * The app-wide "rise" cascade, the same motion as the chat list's rows on a folder switch: whatever appears when a
 * screen (sheet, results…) opens fades in, lifts up and settles on a bouncy spring — the higher on screen, the sooner,
 * so every screen ripples in from the top.
 *
 * Only what appears in the first moments of a screen takes part: rows that scroll in later just are there (no
 * flicker while scrolling). Off with motion off, with the Appearance switch, and in energy saving.
 */
object Cascade {
    val on: Boolean
        get() = Prefs.motion.value != "off" && Prefs.cascadeAppear.value && !(Power.on && Prefs.psAppear.value)
}

/** when this screen (or its latest batch of content) appeared; elements born within [WINDOW] ms of it rise in */
@Stable
class RiseClock {
    var start = SystemClock.uptimeMillis()
        private set

    /** a fresh batch (new search results…): what appears now cascades again */
    fun restart() { start = SystemClock.uptimeMillis() }

    companion object { const val WINDOW = 650L }
}

val LocalRiseClock = staticCompositionLocalOf<RiseClock?> { null }

/** gives the content its own cascade clock (a screen, a sheet) */
@Composable
fun ProvideRise(clock: RiseClock = remember { RiseClock() }, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalRiseClock provides clock, content = content)

/**
 * One element of the cascade. Waits for its place on screen, then rises after a delay that grows with its height on
 * screen (≈ 300 ms from top to bottom), like the folder rows. [extra] shifts it later still (a second column…).
 */
@Composable
fun Modifier.rise(extra: Long = 0L, fromBottom: Boolean = false, lift: androidx.compose.ui.unit.Dp = 30.dp, quick: Boolean = false): Modifier {
    val clock = LocalRiseClock.current ?: return this
    val born = remember { SystemClock.uptimeMillis() }
    val animate = remember { Cascade.on && born - clock.start in 0..RiseClock.WINDOW }
    if (!animate) return this
    val a = remember { Animatable(0f) }
    var y by remember { mutableFloatStateOf(-1f) }
    val screenH = LocalWindowInfo.current.containerSize.height.toFloat().coerceAtLeast(1f)
    LaunchedEffect(y >= 0f) {
        if (y < 0f) return@LaunchedEffect
        val k = Prefs.motionSpeed.value
        // top → bottom over ~300 ms (like 26 ms per chat row), minus the time already gone since the screen opened
        val f = (y / screenH).coerceIn(0f, 1f).let { if (fromBottom) 1f - it else it }
        // quick: the whole wave fits in ~120 ms and snaps in on a stiff, bouncy spring (chat bubbles: never slows typing)
        val span = if (quick) 120f else 300f
        val wait = ((if (quick) 0L else 40L) + (f * span).toLong() + extra) / k.coerceAtLeast(0.3f)
        // counted from the screen's start (not from when this row happened to be composed): every row keeps its
        // place in the wave, so none jumps in early or late
        delay((clock.start + wait.toLong() - SystemClock.uptimeMillis()).coerceAtLeast(0L))
        a.animateTo(1f, if (quick) spring(dampingRatio = 0.6f, stiffness = 700f * k * k) else spring(dampingRatio = 0.72f, stiffness = 360f * k * k))
    }
    return this
        .onPlaced { if (y < 0f) y = it.positionInRoot().y.coerceAtLeast(0f) }
        .graphicsLayer {
            val v = a.value
            alpha = (if (quick) v * 1.6f else v).coerceIn(0f, 1f)
            translationY = (1f - v) * lift.toPx()
            val sc = 0.95f + 0.05f * v
            scaleX = sc; scaleY = sc
        }
}

/**
 * Motion blur on scrolling (Settings → Animations): the list blurs along the scroll direction in proportion to
 * its speed and clears by itself a moment after it stops (a GPU RenderEffect, Android 12+; nothing below that).
 * Works through nested scroll, so it sees finger drags and flings alike.
 */
@Composable
fun Modifier.motionBlur(): Modifier {
    if (android.os.Build.VERSION.SDK_INT < 31 || !app.qwgram.next.Prefs.motionBlur.value) return this
    val (mult, cap) = when (app.qwgram.next.Prefs.motionBlurStrength.value) { "low" -> 0.25f to 12f; "high" -> 0.75f to 45f; else -> 0.45f to 24f }
    val speed = androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val last = androidx.compose.runtime.remember { longArrayOf(0L) }
    // decay: no scroll for a few frames → the blur fades out
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            androidx.compose.runtime.withFrameMillis { now ->
                if (now - last[0] > 40 && speed.floatValue > 0.01f) speed.floatValue = (speed.floatValue * 0.72f).let { if (it < 0.3f) 0f else it }
            }
        }
    }
    val conn = androidx.compose.runtime.remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (consumed.y != 0f) {
                    // smoothed like the original: half the old speed, half the new step
                    speed.floatValue = speed.floatValue * 0.5f + kotlin.math.abs(consumed.y) * 0.5f
                    last[0] = android.os.SystemClock.uptimeMillis()
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }
    return this.nestedScroll(conn).graphicsLayer {
        val r = (speed.floatValue * mult).coerceAtMost(cap)
        renderEffect = if (r < 0.5f) null else androidx.compose.ui.graphics.BlurEffect(0.01f, r, androidx.compose.ui.graphics.TileMode.Clamp)
    }
}
