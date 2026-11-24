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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    val m = rememberPressMorph(interaction, rest.rs(), pressed.rs(), squish)
    Box(
        modifier
            .graphicsLayer {
                shape = RoundedCornerShape(m.radius.value.coerceAtLeast(0.dp))
                clip = true
                shadowElevation = elevation.toPx()
            }
            .drawBehind {
                val s = m.scale.value
                val r = m.radius.value.toPx().coerceIn(0f, size.minDimension / 2f)
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
    elevation: Dp = 0.dp,
) {
    MorphBox(
        onClick = onClick, onLongClick = onLongClick, enabled = enabled,
        modifier = modifier.size(size), rest = size / 2, pressed = size / 4,
        color = color, contentColor = tint, squish = 0.92f, elevation = elevation,
    ) { Icon(icon, desc, Modifier.size(24.dp)) }
}

/** global corner radius (Appearance → Shapes): every container radius goes through this */
fun Dp.rs(): Dp = this * Prefs.cornerScale.value

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
 * M3 Expressive *standard button group*, compact: buttons are sized by their content (icon buttons are circles,
 * labeled ones are pills) and centred. The pressed one stretches by a few dp and its corners tighten; a toggled
 * button morphs from round to a rounded square and fills with the primary colour.
 */
@Composable
fun ButtonGroupRow(items: List<GroupButton>, modifier: Modifier = Modifier, height: Dp = 48.dp) {
    val cs = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { b ->
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val grow by animateDpAsState(if (pressed) (14f * pressFactor()).dp else 0.dp, if (pressed) Springs.pressIn() else Springs.morphRelease(), label = "gw")
            val r by animateDpAsState(
                when { b.selected -> height * 0.3f; pressed -> height * (0.5f - 0.2f * pressFactor()); else -> height / 2 },
                if (pressed) Springs.pressIn() else Springs.morphRelease(), label = "gr",
            )
            val bg by animateColorAsState(if (b.selected) cs.primary else cs.secondaryContainer, Springs.effects(), label = "gb")
            val fg = if (b.selected) cs.onPrimary else cs.onSecondaryContainer
            Box(
                Modifier.height(height).defaultMinSize(minWidth = height + grow)
                    .graphicsLayer { shape = RoundedCornerShape(r.rs().coerceAtLeast(0.dp)); clip = true }
                    .background(bg)
                    .clickable(interactionSource = interaction, indication = ripple(), onClick = b.onClick)
                    .padding(horizontal = if (b.label != null) 16.dp + grow / 2 else 0.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (b.icon != null) Icon(b.icon, b.desc, Modifier.size(20.dp), tint = fg)
                    if (b.icon != null && b.label != null) Spacer(Modifier.width(8.dp))
                    if (b.label != null) Text(b.label, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
                }
            }
        }
    }
}

/**
 * Telegram-style profile actions as an M3 Expressive connected button group: equal-width tonal tiles (icon over
 * label), big outer corners, small inner ones; the pressed tile rounds into a pill, a toggled one fills with primary.
 */
@Composable
fun ActionTiles(items: List<GroupButton>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
        items.forEachIndexed { i, b ->
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val outer = 22.dp.rs()
            val inner = 8.dp.rs()
            val pr by animateDpAsState(if (pressed) 30.dp else 0.dp, if (pressed) Springs.pressIn() else Springs.morphRelease(), label = "tp")
            val start = (if (i == 0) outer else inner) + pr * (if (i == 0) 0.3f else 1f)
            val end = (if (i == items.lastIndex) outer else inner) + pr * (if (i == items.lastIndex) 0.3f else 1f)
            val sc by animateFloatAsState(if (pressed) 0.95f else 1f, if (pressed) Springs.pressIn() else Springs.morphRelease(), label = "ts")
            val bg by animateColorAsState(if (b.selected) cs.primary else cs.surfaceContainerHigh, Springs.effects(), label = "tb")
            val fg = if (b.selected) cs.onPrimary else cs.primary
            Column(
                Modifier.weight(1f).height(66.dp)
                    .graphicsLayer {
                        scaleX = sc; scaleY = sc
                        shape = RoundedCornerShape(topStart = start.coerceAtLeast(0.dp), bottomStart = start.coerceAtLeast(0.dp), topEnd = end.coerceAtLeast(0.dp), bottomEnd = end.coerceAtLeast(0.dp))
                        clip = true
                    }
                    .background(bg)
                    .clickable(interactionSource = interaction, indication = ripple(), onClick = b.onClick)
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                if (b.icon != null) Icon(b.icon, b.desc ?: b.label, Modifier.size(22.dp), tint = fg)
                if (b.label != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(b.label, style = MaterialTheme.typography.labelMedium, color = if (b.selected) cs.onPrimary else cs.onSurface, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/**
 * Lets a horizontally-dragged control (slider, pager…) own the finger: marks the gesture on DOWN, before the
 * screen's swipe-back (which runs later, in the Main pass) decides anything.
 */
object SwipeGuard { @Volatile var blocked = false }

fun Modifier.noSwipeBack(): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
            SwipeGuard.blocked = true
            do {
                val ev = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
            } while (ev.changes.any { it.pressed })
            SwipeGuard.blocked = false
        }
    },
)

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
fun FabMenu(expanded: Boolean, onToggle: (Boolean) -> Unit, icon: ImageVector, actions: List<FabAction>, modifier: Modifier = Modifier, size: Dp = 64.dp, showButton: Boolean = true) {
    val cs = MaterialTheme.colorScheme
    Column(modifier, horizontalAlignment = Alignment.End, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
        actions.forEachIndexed { i, a ->
            // each action has its own visibility, switched with a small stagger (bottom one first)
            var shown by remember { mutableStateOf(false) }
            LaunchedEffect(expanded) {
                val order = if (expanded) actions.size - 1 - i else i
                kotlinx.coroutines.delay(35L * order)
                shown = expanded
            }
            androidx.compose.animation.AnimatedVisibility(
                shown,
                enter = androidx.compose.animation.fadeIn(Springs.effects()) +
                    androidx.compose.animation.scaleIn(Springs.spatial(), initialScale = 0.6f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)) +
                    androidx.compose.animation.slideInVertically(Springs.spatial()) { it / 2 },
                exit = androidx.compose.animation.fadeOut(Springs.exit()) +
                    androidx.compose.animation.scaleOut(Springs.exit(), targetScale = 0.8f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)),
            ) {
                MorphBox(
                    onClick = { onToggle(false); a.onClick() }, rest = 28.dp, pressed = 14.dp,
                    color = cs.primaryContainer, contentColor = cs.onPrimaryContainer, elevation = 3.dp,
                    modifier = Modifier.height(56.dp),
                ) {
                    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(a.icon, null)
                        Spacer(Modifier.width(12.dp))
                        Text(a.title, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
        if (!showButton) return@Column
        val morph = remember(Prefs.fabShape.value) { androidx.graphics.shapes.Morph(fabPolygon(), androidx.compose.material3.MaterialShapes.Circle) }
        val prog by animateFloatAsState(if (expanded) 1f else 0f, Springs.spatial(), label = "fm")
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val press by animateFloatAsState(if (pressed) 1f else 0f, if (pressed) Springs.pressIn() else Springs.morphRelease(), label = "fp")
        val bg by animateColorAsState(if (expanded) cs.primary else cs.primaryContainer, Springs.effects(), label = "fb")
        // padding leaves room for the shadow so it is never cut by parents
        Box(Modifier.padding(6.dp)) {
            Box(
                Modifier.size(size)
                    .graphicsLayer {
                        val v = (prog + press * 0.3f * pressFactor()).coerceIn(0f, 1f)
                        shape = MorphShape(morph, v); clip = true; shadowElevation = 6.dp.toPx()
                        rotationZ = prog * 45f
                    }
                    .background(bg)
                    .clickable(interactionSource = interaction, indication = ripple()) { onToggle(!expanded) },
                contentAlignment = Alignment.Center,
            ) {
                // icon counter-rotates and crossfades to the close icon
                androidx.compose.animation.Crossfade(expanded, animationSpec = Springs.effects(), label = "fi", modifier = Modifier.graphicsLayer { rotationZ = -prog * 45f + prog * 90f }) { open ->
                    Icon(
                        if (open) androidx.compose.material.icons.Icons.Rounded.Close else icon, if (open) "Закрыть" else "Меню",
                        tint = if (open) cs.onPrimary else cs.onPrimaryContainer, modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}
