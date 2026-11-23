package app.qwgram.next.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.qwgram.next.Prefs
import kotlinx.coroutines.launch

/**
 * Spring tokens from the M3 Expressive motion spec (stiffness / damping), scaled by the user's speed setting.
 *  - fastSpatial 800/0.6 : small controls, press morph release (bouncy)
 *  - spatial     380/0.8 : indicators, expand/collapse
 *  - smooth      700/1.0 : screens, sheets, overlays (never bounce)
 */
object Springs {
    private fun k(stiffness: Float): Float { val s = Prefs.motionSpeed.value; return stiffness * s * s }
    private val off get() = Prefs.motion.value == "off"
    private val calm get() = Prefs.motion.value == "standard"

    fun <T> fastSpatial(): FiniteAnimationSpec<T> = if (off) snap() else spring(if (calm) 0.9f else 0.6f, k(if (calm) 1400f else 800f))
    fun <T> spatial(): FiniteAnimationSpec<T> = if (off) snap() else spring(if (calm) 0.9f else 0.8f, k(if (calm) 700f else 380f))
    fun <T> smooth(): FiniteAnimationSpec<T> = if (off) snap() else spring(Spring.DampingRatioNoBouncy, k(700f))
    fun <T> smoothSlow(): FiniteAnimationSpec<T> = if (off) snap() else spring(Spring.DampingRatioNoBouncy, k(380f))
    fun <T> effects(): FiniteAnimationSpec<T> = if (off) snap() else spring(Spring.DampingRatioNoBouncy, k(1600f))

    /** press-in: fast and monotonic (no spring), per the skill's smoothness rule #2 */
    fun <T> pressIn(): FiniteAnimationSpec<T> = if (off) snap() else tween(90, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))

    /** exits: quick, no bounce */
    fun <T> exit(): FiniteAnimationSpec<T> = if (off) snap() else tween(180, easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f))
}

/** Drives the press morph: corner radius rest → pressed (fast), back with a bouncy spring; plus a 0.96 squish. */
class PressMorph(val radius: Animatable<Dp, *>, val scale: Animatable<Float, *>)

@Composable
fun rememberPressMorph(interaction: MutableInteractionSource, rest: Dp, pressed: Dp, squish: Float = 0.96f): PressMorph {
    val radius = remember { Animatable(rest, Dp.VectorConverter) }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(rest) { radius.snapTo(rest) }
    LaunchedEffect(interaction, rest, pressed) {
        interaction.interactions.collect { i ->
            when (i) {
                is PressInteraction.Press -> {
                    launch { radius.animateTo(pressed, Springs.pressIn()) }
                    launch { scale.animateTo(squish, Springs.pressIn()) }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    launch { radius.animateTo(rest, Springs.fastSpatial()) }
                    launch { scale.animateTo(1f, Springs.fastSpatial()) }
                }
            }
        }
    }
    return morph(radius, scale)
}

private fun morph(r: Animatable<Dp, *>, s: Animatable<Float, *>) = PressMorph(r, s)

/**
 * Generic expressive pressable: background + ripple clipped by a shape whose corners morph while pressed,
 * the whole thing squishes and springs back. Use for cards, FABs, chips, nav items, composer buttons.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MorphBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rest: Dp = 16.dp,
    pressed: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    elevation: Dp = 0.dp,
    enabled: Boolean = true,
    squish: Float = 0.96f,
    onLongClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val m = rememberPressMorph(interaction, rest, pressed, squish)
    val shape = RoundedCornerShape(m.radius.value)
    Box(
        modifier
            .graphicsLayer { scaleX = m.scale.value; scaleY = m.scale.value }
            .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape, clip = false) else Modifier)
            .graphicsLayer { this.shape = shape; clip = true }
            .background(color)
            .combinedClickable(
                interactionSource = interaction, indication = ripple(), enabled = enabled,
                onClick = onClick, onLongClick = onLongClick,
            ),
        contentAlignment = contentAlignment,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

/** Extended FAB with press morph; collapses to the icon (spring on width) when [expanded] is false. */
@Composable
fun MorphFab(
    icon: ImageVector,
    text: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    MorphBox(
        onClick = onClick, modifier = modifier.defaultMinSize(56.dp, 56.dp), rest = 16.dp, pressed = 28.dp,
        color = color, contentColor = contentColor, elevation = 3.dp,
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, text, Modifier.size(24.dp))
            AnimatedVisibility(
                expanded,
                enter = expandHorizontally(Springs.spatial()) + fadeIn(Springs.effects()),
                exit = shrinkHorizontally(Springs.exit()) + fadeOut(Springs.exit()),
            ) {
                Row {
                    Spacer(Modifier.width(12.dp))
                    Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}

/** Round icon button that morphs to a squircle while pressed (M3 Expressive icon button press). */
@Composable
fun MorphIconButton(
    icon: ImageVector,
    desc: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = Color.Transparent,
    tint: Color = LocalContentColor.current,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
) {
    MorphBox(
        onClick = onClick, onLongClick = onLongClick, enabled = enabled,
        modifier = modifier.size(size), rest = size / 2, pressed = size / 4,
        color = color, contentColor = tint, squish = 0.92f,
    ) { Icon(icon, desc, Modifier.size(24.dp)) }
}

/** Text button with the expressive press morph (pill → squarer while pressed, springs back) */
@Composable
fun QTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    MorphBox(
        onClick = onClick, enabled = enabled, modifier = modifier.defaultMinSize(minHeight = 40.dp),
        rest = 20.dp, pressed = 8.dp, color = Color.Transparent,
        contentColor = if (enabled) cs.primary else cs.onSurface.copy(alpha = 0.38f), squish = 0.95f,
    ) {
        androidx.compose.material3.ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, content = content)
        }
    }
}

/** Filled / tonal button (56dp or 48dp) with the press morph */
@Composable
fun QButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, tonal: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val bg = when { !enabled -> cs.onSurface.copy(alpha = 0.12f); tonal -> cs.secondaryContainer; else -> cs.primary }
    val fg = when { !enabled -> cs.onSurface.copy(alpha = 0.38f); tonal -> cs.onSecondaryContainer; else -> cs.onPrimary }
    androidx.compose.foundation.layout.BoxWithConstraints(modifier) {
        val h = if (maxHeight != Dp.Infinity) maxHeight else 48.dp
        MorphBox(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxSize(), rest = h / 2, pressed = 12.dp, color = bg, contentColor = fg, squish = 0.97f) {
            androidx.compose.material3.ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center, content = content)
            }
        }
    }
}
