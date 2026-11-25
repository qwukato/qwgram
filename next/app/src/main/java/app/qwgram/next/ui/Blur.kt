package app.qwgram.next.ui

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import app.qwgram.next.Prefs
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.distinctUntilChanged

/*
 * Background blur ("glass") for qwgram, built on Haze (hardware RenderEffect blur, Android 12+).
 *
 *  - every screen gets its own HazeState ([LocalHaze], provided by NavHost);
 *  - only what scrolls behind the chrome is a *source*: lists, the chat history, wallpapers ([blurSource]);
 *  - chrome is the *effect*: headers and bottom bars blur progressively towards their open edge ([glassFade]),
 *    floating buttons / pills / the composer are shaped glass ([glass]);
 *  - dialogs, sheets and popups blur the whole screen behind their window ([WindowBlurBehind]).
 *
 * An effect is never placed inside its own source (that would draw itself), and effects only run in the app's
 * main window. With the master switch off nothing here does any work: no layers, no RenderEffects.
 */

/** sources at or above this z are chrome (bars that are also sources, for full-screen menu blurs) */
const val CHROME_Z = 2f

/** the current screen's blur state; null = nothing to blur here */
val LocalHaze: ProvidableCompositionLocal<HazeState?> = compositionLocalOf { null }

/** the top screen's state, for chrome floating over every screen (the mini player) */
object BlurHost { var top by mutableStateOf<HazeState?>(null) }

/** chat wallpaper only (dates, service plates and frosted bubbles blur it, not the messages around them) */
val LocalWallHaze: ProvidableCompositionLocal<HazeState?> = compositionLocalOf { null }

/** true in the app's own window (effects never run inside dialogs / popups: their sources live elsewhere) */
@Composable
fun inMainWindow(): Boolean {
    val v = LocalView.current
    val lp = v.rootView.layoutParams as? WindowManager.LayoutParams ?: return true
    return lp.type == WindowManager.LayoutParams.TYPE_BASE_APPLICATION || lp.type == WindowManager.LayoutParams.TYPE_APPLICATION_STARTING
}

/** the blur is on, supported, and this part of the interface uses it */
@Composable
fun blurOn(part: Prefs.B? = null): Boolean = BLUR_ENABLED && Prefs.blurOn.value && (part == null || part.value) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** the background blur is switched off for now (too costly); everything below is dormant and costs nothing */
const val BLUR_ENABLED = false

/** the surface colours the glass is tinted with (Appearance → Blur → Tone) */
@Composable
fun glassTone(base: Color): Color {
    val cs = MaterialTheme.colorScheme
    return when (Prefs.blurTone.value) {
        "container" -> cs.surfaceContainerHigh
        "accent" -> cs.primaryContainer
        "dark" -> Color(0xFF0E0E12)
        "light" -> Color(0xFFF7F7FA)
        "none" -> Color.Transparent
        else -> base
    }
}

private fun curve(): Easing = when (Prefs.blurCurve.value) {
    "linear" -> LinearEasing
    "sharp" -> EaseInCubic
    else -> EaseOutCubic
}

@OptIn(ExperimentalHazeApi::class)
private fun quality(): HazeInputScale = if (Prefs.powerSave.value && Prefs.psBlurLow.value) HazeInputScale.Fixed(0.33f) else when (Prefs.blurQuality.value) {
    "speed" -> HazeInputScale.Fixed(0.33f)
    "best" -> HazeInputScale.None
    else -> HazeInputScale.Fixed(0.5f)
}

/** marks content that chrome may blur (lists, history, wallpapers); [z] orders several sources of one screen */
@Composable
fun Modifier.blurSource(z: Float = 0f, state: HazeState? = LocalHaze.current): Modifier =
    if (state != null && blurOn() && inMainWindow()) this.hazeSource(state, zIndex = z) else this

/**
 * Shaped glass: the content behind this element, blurred and tinted with [base] (keep the element's own
 * background transparent while [blurOn]). [strength] scales the tint (0..1.5), [part] is the settings switch.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun Modifier.glass(
    shape: Shape,
    base: Color,
    part: Prefs.B = Prefs.blurBars,
    strength: Float = 1f,
    state: HazeState? = LocalHaze.current,
    edge: Boolean = true,
    /** exact tint alpha of [base] (e.g. bubbles keep their own colour), instead of the global tone & strength */
    tintAlpha: Float? = null,
): Modifier {
    if (state == null || !blurOn(part) || !inMainWindow()) return this
    val tone = if (tintAlpha != null) base else glassTone(base)
    val tint = (tintAlpha ?: (Prefs.blurBtnTint.value * strength)).coerceIn(0f, 1f)
    val radius = Prefs.blurBtnRadius.value.dp
    val noise = Prefs.blurBtnNoise.value
    val q = quality()
    val light = MaterialTheme.colorScheme.onSurface
    return this
        .clip(shape)
        .hazeEffect(state) {
            blurRadius = radius
            backgroundColor = base
            tints = if (tone.alpha == 0f) emptyList() else listOf(HazeTint(tone.copy(alpha = tint * tone.alpha)))
            noiseFactor = noise
            inputScale = q
            // only the content layers (wallpaper, lists): never a bar this button may sit in (no self-drawing)
            canDrawArea = { area -> area.zIndex < CHROME_Z }
        }
        .then(if (edge && Prefs.blurEdge.value) Modifier.border(0.75.dp, light.copy(alpha = 0.10f), shape) else Modifier)
}

/** full-area blur for scrims (behind menus): fade it in with the scrim's own alpha */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun Modifier.glassScrim(base: Color, part: Prefs.B = Prefs.blurMenus, state: HazeState? = LocalHaze.current): Modifier {
    if (state == null || !blurOn(part) || !inMainWindow()) return this
    val radius = Prefs.blurRadius.value.dp
    val q = quality()
    return this.hazeEffect(state) {
        blurRadius = radius
        backgroundColor = base
        tints = emptyList()
        noiseFactor = 0f
        inputScale = q
    }
}

/** which edge of a bar is open (the blur fades out towards it) */
enum class FadeEdge { Bottom, Top }

/**
 * Header / bottom-bar glass. Put it on a BACKGROUND layer only (a `matchParentSize()` sibling behind the bar's
 * content — never on the bar itself, or its buttons would be faded out with it). The blur and the tint are
 * strongest at the screen edge and melt away towards [open]; with a fade length over 100% the glass reaches past
 * the bar into the content, for a long, soft transition.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun Modifier.glassFade(open: FadeEdge, base: Color, part: Prefs.B = Prefs.blurHeaders, fadeOut: Boolean = true, state: HazeState? = LocalHaze.current): Modifier {
    if (state == null || !blurOn(part) || !inMainWindow()) return this
    val tone = glassTone(base)
    val tint = Prefs.blurFadeTint.value
    var h by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val radius = Prefs.blurFadeRadius.value.dp
    val noise = Prefs.blurNoise.value
    val fading = Prefs.blurFade.value && fadeOut
    val len = Prefs.blurFadeLen.value.coerceIn(0.3f, 3f)
    val ext = if (fading) maxOf(1f, len) else 1f      // the layer grows past the bar for long transitions
    val reach = if (fading) minOf(1f, len) else 1f      // a short transition ends inside the bar
    val ease = curve()
    val q = quality()
    val hh = h   // read here: a new size recomposes with the right gradient
    // the tint melts away with the blur: the whole glass is masked by a soft gradient towards the open edge
    val mask = if (fading) {
        // solid near the edge, then an eased fall-off (several stops: no visible banding)
        val n = 8
        val pts = (0..n).map { i ->
            val f = i / n.toFloat()
            val a = 1f - ease.transform(f)
            val pos = 0.35f * reach + f * 0.65f * reach
            pos to Color.Black.copy(alpha = a)
        }
        val stops = if (open == FadeEdge.Bottom) (listOf(0f to Color.Black) + pts + listOf(1f to Color.Transparent)).distinctBy { it.first }
        else (listOf(0f to Color.Transparent) + pts.reversed().map { (1f - it.first) to it.second } + listOf(1f to Color.Black)).distinctBy { it.first }
        androidx.compose.ui.graphics.Brush.verticalGradient(*stops.sortedBy { it.first }.toTypedArray())
    } else null
    return this
        .layout { m, c ->
            // extend the glass beyond the bar (downwards for a header, upwards for a bottom bar)
            val baseH = c.maxHeight.takeIf { it != androidx.compose.ui.unit.Constraints.Infinity } ?: c.minHeight
            val full = (baseH * ext).toInt().coerceAtLeast(1)
            val pl = m.measure(androidx.compose.ui.unit.Constraints.fixed(c.maxWidth, full))
            layout(c.maxWidth, baseH) { pl.place(0, if (open == FadeEdge.Top) baseH - full else 0) }
        }
        .then(if (mask != null) Modifier
            .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
            .drawWithContent { drawContent(); drawRect(mask, blendMode = androidx.compose.ui.graphics.BlendMode.DstIn) }
        else Modifier)
        .onSizeChanged { h = it.height.toFloat() }
        .hazeEffect(state) {
            blurRadius = radius
            backgroundColor = base
            tints = if (tone.alpha == 0f) emptyList() else listOf(HazeTint(tone.copy(alpha = tint * tone.alpha)))
            noiseFactor = noise
            inputScale = q
            // content layers only: never the bar this glass belongs to (bars can be sources for menu blurs)
            canDrawArea = { area -> area.zIndex < CHROME_Z }
            if (fading) {
                // the blur radius itself falls off too (true progressive blur), a little later than the mask
                progressive = if (open == FadeEdge.Bottom)
                    HazeProgressive.verticalGradient(easing = ease, startY = hh * 0.3f * reach, startIntensity = 1f, endY = (hh * reach).coerceAtLeast(1f), endIntensity = 0f, preferPerformance = true)
                else
                    HazeProgressive.verticalGradient(easing = ease, startY = hh * (1f - reach), startIntensity = 0f, endY = (hh * (1f - 0.3f * reach)).coerceAtLeast(1f), endIntensity = 1f, preferPerformance = true)
            }
        }
}

/** headers / bottom fades are glass (blur on, this part on, and "blurred fades" on) */
@Composable
fun fadeGlassOn(part: Prefs.B): Boolean = blurOn(part) && Prefs.blurFadeBlur.value && LocalHaze.current != null

/**
 * How much of a bar's fade shows: with "only while scrolling" it appears once content is under the bar
 * ([scrolled] 0..1, animated by the caller), otherwise always.
 */
fun fadeShown(scrolled: Float): Float = if (Prefs.fadeOnScroll.value) scrolled.coerceIn(0f, 1f) else 1f

/** the glass background layer of a bar: a sibling behind its content (see [glassFade]) */
@Composable
fun androidx.compose.foundation.layout.BoxScope.GlassFadeLayer(open: FadeEdge, base: Color, part: Prefs.B = Prefs.blurHeaders, fadeOut: Boolean = true, shown: () -> Float = { 1f }) {
    if (!fadeGlassOn(part)) return
    Box(Modifier.matchParentSize().graphicsLayer { alpha = shown() }.glassFade(open, base, part, fadeOut))
}

/**
 * Inside a dialog / sheet / popup: blurs everything behind its window (Android 12+ cross-window blur), following
 * [progress] (0 = none, 1 = full radius) so it opens and closes with the window's own animation.
 */
@Composable
fun WindowBlurBehind(part: Prefs.B = Prefs.blurWindows, progress: () -> Float = { 1f }) {
    if (!blurOn(part)) return
    val view = LocalView.current
    val ctx = LocalContext.current
    val maxPx = with(LocalDensity.current) { Prefs.blurWindowRadius.value.dp.roundToPx() }
    LaunchedEffect(view, maxPx) {
        val wm = ctx.getSystemService(WindowManager::class.java) ?: return@LaunchedEffect
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !wm.isCrossWindowBlurEnabled) return@LaunchedEffect
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        snapshotFlow { (progress().coerceIn(0f, 1f) * maxPx / 2f).toInt() * 2 }   // even steps: fewer window updates
            .distinctUntilChanged()
            .collect { r ->
                runCatching {
                    if (dialogWindow != null) {
                        dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                        dialogWindow.attributes = dialogWindow.attributes.apply { blurBehindRadius = r }
                    }
                    // popups are skipped on purpose: their blur covers the popup's rectangle (a blurred square behind
                    // a round menu)
                }
            }
    }
}
