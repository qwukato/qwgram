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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.sp
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
    fun <T> pressIn(): FiniteAnimationSpec<T> = if (off) snap() else tween((150 / Prefs.motionSpeed.value).toInt(), easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))

    /** release of a press morph: soft spring with a gentle overshoot (smoother than fastSpatial) */
    fun <T> morphRelease(): FiniteAnimationSpec<T> = if (off) snap() else spring(if (calm) 0.9f else 0.62f, k(if (calm) 900f else 450f))

    /** exits: quick, no bounce */
    fun <T> exit(): FiniteAnimationSpec<T> = if (off) snap() else tween(180, easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f))
}

/** Drives the press morph: the container's corner radius rest → pressed (smooth), back on a soft spring. */
class PressMorph(val radius: Animatable<Dp, *>, val scale: Animatable<Float, *>)

/** how strongly buttons morph when pressed (Appearance → Animations) */
fun pressFactor(): Float = when (Prefs.pressMorph.value) { "off" -> 0f; "soft" -> 0.5f; "strong" -> 1.35f; else -> 1f }

@Composable
fun rememberPressMorph(interaction: MutableInteractionSource, rest: Dp, pressed: Dp, squish: Float = 0.97f): PressMorph {
    val radius = remember { Animatable(rest, Dp.VectorConverter) }
    val scale = remember { Animatable(1f) }
    val f = pressFactor()
    val target = (rest + (pressed - rest) * f).coerceAtLeast(0.dp)
    val sq = 1f - (1f - squish) * f
    LaunchedEffect(rest) { if (!radius.isRunning) radius.snapTo(rest) }
    LaunchedEffect(interaction, rest, target) {
        interaction.interactions.collect { i ->
            when (i) {
                is PressInteraction.Press -> {
                    launch { radius.animateTo(target, Springs.pressIn()) }
                    launch { scale.animateTo(sq, Springs.pressIn()) }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    launch { radius.animateTo(rest, Springs.morphRelease()) }
                    launch { scale.animateTo(1f, Springs.morphRelease()) }
                }
            }
        }
    }
    return PressMorph(radius, scale)
}

/**
 * Generic expressive pressable. Only the CONTAINER morphs: its corners change while pressed and the background
 * gives a slight squeeze; the icon/text inside stay still. All animated values are read in layer/draw lambdas,
 * so pressing never recomposes the content.
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
    squish: Float = 0.97f,
    onLongClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val m = rememberPressMorph(interaction, rest, pressed, squish)
    Box(
        modifier
            .graphicsLayer {
                shape = RoundedCornerShape(m.radius.value)
                clip = true
                shadowElevation = elevation.toPx()
            }
            .drawBehind {
                val s = m.scale.value
                val r = m.radius.value.toPx().coerceAtMost(size.minDimension / 2f)
                scale(s) { drawRoundRect(color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)) }
            }
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


/** one button in an M3 standard button group */
class GroupButton(val icon: ImageVector? = null, val label: String? = null, val selected: Boolean = false, val desc: String? = label, val onClick: () -> Unit)

/**
 * M3 Expressive *standard button group*: round buttons side by side. The pressed one stretches and squeezes its
 * neighbours, its corners tighten; a selected (toggled) button morphs from round to a rounded square and fills
 * with the primary colour. Labeled buttons are wider pills, icon-only ones are circles.
 */
@Composable
fun ButtonGroupRow(items: List<GroupButton>, modifier: Modifier = Modifier, height: Dp = 56.dp) {
    val cs = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().height(height), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
        items.forEach { b ->
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val base = when { b.label != null && b.icon != null -> 1.7f; b.label != null -> 1.4f; else -> 1f }
            val w by animateFloatAsState(base * if (pressed) 1f + 0.22f * pressFactor() else 1f, if (pressed) Springs.pressIn() else Springs.morphRelease(), label = "gw")
            val r by animateDpAsState(
                when { b.selected -> height * 0.3f; pressed -> height * (0.5f - 0.24f * pressFactor()); else -> height / 2 },
                if (pressed) Springs.pressIn() else Springs.morphRelease(), label = "gr",
            )
            val bg by animateColorAsState(if (b.selected) cs.primary else cs.secondaryContainer, Springs.effects(), label = "gb")
            val fg = if (b.selected) cs.onPrimary else cs.onSecondaryContainer
            Box(
                Modifier.weight(w).fillMaxHeight()
                    .graphicsLayer { shape = RoundedCornerShape(r); clip = true }
                    .background(bg)
                    .clickable(interactionSource = interaction, indication = ripple(), onClick = b.onClick)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (b.icon != null) Icon(b.icon, b.desc, Modifier.size(22.dp), tint = fg)
                    if (b.icon != null && b.label != null) Spacer(Modifier.width(8.dp))
                    if (b.label != null) androidx.compose.foundation.text.BasicText(
                        b.label, maxLines = 1, style = MaterialTheme.typography.labelLarge.copy(color = fg),
                        autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp, stepSize = 0.5.sp),
                    )
                }
            }
        }
    }
}

class FabAction(val title: String, val icon: ImageVector, val onClick: () -> Unit)

/** the FAB container shape chosen in Appearance (expressive polygon or plain) */
fun fabPolygon(): androidx.graphics.shapes.RoundedPolygon = when (Prefs.fabShape.value) {
    "sunny" -> androidx.compose.material3.MaterialShapes.Sunny
    "circle" -> androidx.compose.material3.MaterialShapes.Circle
    "square" -> androidx.compose.material3.MaterialShapes.Square
    else -> androidx.compose.material3.MaterialShapes.Cookie9Sided
}

/**
 * M3 Expressive FAB menu: the FAB morphs from its expressive shape into a circle with a close icon and the actions
 * spring out above it one after another as tonal pills. Tap outside (handled by the caller's scrim) to close.
 */
@Composable
fun FabMenu(expanded: Boolean, onToggle: (Boolean) -> Unit, icon: ImageVector, actions: List<FabAction>, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val cs = MaterialTheme.colorScheme
    Column(modifier, horizontalAlignment = Alignment.End, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        actions.forEachIndexed { i, a ->
            val delayIdx = if (expanded) actions.size - 1 - i else i
            val p = remember { Animatable(0f) }
            LaunchedEffect(expanded) {
                kotlinx.coroutines.delay(28L * delayIdx)
                p.animateTo(if (expanded) 1f else 0f, if (expanded) Springs.spatial() else Springs.exit())
            }
            if (p.value > 0.01f || expanded) MorphBox(
                onClick = { onToggle(false); a.onClick() }, rest = 28.dp, pressed = 14.dp,
                color = cs.primaryContainer, contentColor = cs.onPrimaryContainer, elevation = 3.dp,
                modifier = Modifier.height(56.dp).graphicsLayer {
                    val v = p.value
                    alpha = v.coerceIn(0f, 1f)
                    translationY = (1f - v) * 24.dp.toPx()
                    scaleX = 0.7f + 0.3f * v; scaleY = 0.7f + 0.3f * v
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)
                },
            ) {
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(a.icon, null)
                    Spacer(Modifier.width(12.dp))
                    Text(a.title, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        val morph = remember(Prefs.fabShape.value) { androidx.graphics.shapes.Morph(fabPolygon(), androidx.compose.material3.MaterialShapes.Circle) }
        val prog by animateFloatAsState(if (expanded) 1f else 0f, Springs.fastSpatial(), label = "fm")
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val press by animateFloatAsState(if (pressed) 1f else 0f, if (pressed) Springs.pressIn() else Springs.morphRelease(), label = "fp")
        val bg by animateColorAsState(if (expanded) cs.primary else cs.primaryContainer, Springs.effects(), label = "fb")
        Box(
            Modifier.size(size)
                .graphicsLayer {
                    // pressing pulls the shape towards the circle a little; open = full circle
                    val v = (prog + press * 0.35f * pressFactor()).coerceIn(0f, 1f)
                    shape = MorphShape(morph, v); clip = true; shadowElevation = 6.dp.toPx()
                    rotationZ = (1f - prog) * 0f
                }
                .background(bg)
                .clickable(interactionSource = interaction, indication = ripple()) { onToggle(!expanded) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (expanded) androidx.compose.material.icons.Icons.Rounded.Close else icon, if (expanded) "Закрыть" else "Меню",
                tint = if (expanded) cs.onPrimary else cs.onPrimaryContainer,
                modifier = Modifier.size(26.dp).graphicsLayer { rotationZ = prog * 90f },
            )
        }
    }
}
