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
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
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
import androidx.compose.ui.unit.isSpecified
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
class PressMorph(val radius: Animatable<Dp, *>, val scale: Animatable<Float, *>, val press: Animatable<Float, *>)

/** how strongly buttons morph when pressed (Appearance → Animations) */
fun pressFactor(): Float = when (Prefs.pressMorph.value) { "off" -> 0f; "soft" -> 0.5f; "strong" -> 1.35f; else -> 1f }

@Composable
fun rememberPressMorph(interaction: MutableInteractionSource, rest: Dp, pressed: Dp, squish: Float = 0.97f): PressMorph {
    val radius = remember { Animatable(rest, Dp.VectorConverter) }
    val scale = remember { Animatable(1f) }
    val press = remember { Animatable(0f) }
    val hf = androidx.compose.ui.platform.LocalHapticFeedback.current
    val f = pressFactor()
    val target = (rest + (pressed - rest) * f).coerceAtLeast(0.dp)
    val sq = 1f - (1f - squish) * f
    LaunchedEffect(rest) { if (!radius.isRunning) radius.snapTo(rest) }
    LaunchedEffect(interaction, rest, target) {
        interaction.interactions.collect { i ->
            when (i) {
                is PressInteraction.Press -> {
                    // a light tick under the finger on every expressive button
                    if (Prefs.haptics.value) hf.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.SegmentFrequentTick)
                    launch { radius.animateTo(target, Springs.pressIn()) }
                    launch { scale.animateTo(sq, Springs.pressIn()) }
                    launch { press.animateTo(1f, androidx.compose.animation.core.tween(90)) }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    launch { radius.animateTo(rest, Springs.morphRelease()) }
                    launch { scale.animateTo(1f, Springs.morphRelease()) }
                    launch { press.animateTo(0f, androidx.compose.animation.core.tween(260)) }
                }
            }
        }
    }
    return PressMorph(radius, scale, press)
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
    /** floating chrome: becomes frosted glass when the background blur is on (Appearance → Blur) */
    glass: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val m = rememberPressMorph(interaction, rest.rs(), pressed.rs(), squish)
    val glassy = glass && color.alpha > 0f && blurOn(Prefs.blurBars) && LocalHaze.current != null && inMainWindow()
    Box(
        modifier
            .graphicsLayer {
                shape = RoundedCornerShape(m.radius.value.coerceAtLeast(0.dp))
                clip = true
                // glass has no drop shadow (it would show through the frosted body)
                shadowElevation = if (glassy) 0f else elevation.toPx()
            }
            .then(if (glassy) Modifier.glass(RoundedCornerShape(m.radius.value.coerceAtLeast(0.dp)), color) else Modifier)
            .drawBehind {
                val s = m.scale.value
                val r = m.radius.value.toPx().coerceIn(0f, size.minDimension / 2f)
                scale(s) {
                    if (!glassy) drawRoundRect(color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r))
                    // the press state layer lives inside the (squeezed) container, never outside it
                    val pa = m.press.value
                    if (pa > 0.001f) drawRoundRect(contentColor.copy(alpha = 0.12f * pa), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r))
                }
            }
            .combinedClickable(
                interactionSource = interaction, indication = null, enabled = enabled,
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
    glass: Boolean = false,
) {
    MorphBox(
        onClick = onClick, modifier = modifier.defaultMinSize(56.dp, 56.dp), rest = 16.dp, pressed = 28.dp,
        color = color, contentColor = contentColor, elevation = 3.dp, glass = glass,
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
    glass: Boolean = false,
) {
    MorphBox(
        onClick = onClick, onLongClick = onLongClick, enabled = enabled,
        modifier = modifier.size(size), rest = size / 2, pressed = size / 4,
        color = color, contentColor = tint, squish = 0.92f, elevation = elevation, glass = glass,
    ) { Icon(icon, desc, Modifier.size(24.dp)) }
}

/**
 * M3 Expressive popup menu that grows out of its button: springs from a small rounded blob at the anchor's corner
 * into the full menu (scale + corner morph), items fade in a beat later; shrinks back into the button on close.
 * Drop-in for DropdownMenu (same children: DropdownMenuItem …).
 */
@Composable
fun QMenu(expanded: Boolean, onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val state = remember { androidx.compose.animation.core.MutableTransitionState(false) }
    state.targetState = expanded
    if (!state.currentState && !state.targetState && state.isIdle) return
    val cs = MaterialTheme.colorScheme
    val k = Prefs.motionSpeed.value * Prefs.motionSpeed.value
    val off = Prefs.motion.value == "off"
    val density = androidx.compose.ui.platform.LocalDensity.current
    // the button the menu belongs to (the popup's anchor): the menu starts as exactly that shape
    var anchor by remember { mutableStateOf(androidx.compose.ui.unit.IntRect.Zero) }
    val margin = with(density) { 8.dp.roundToPx() }
    val provider = remember {
        object : androidx.compose.ui.window.PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: androidx.compose.ui.unit.IntRect, windowSize: androidx.compose.ui.unit.IntSize,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection, popupContentSize: androidx.compose.ui.unit.IntSize,
            ): androidx.compose.ui.unit.IntOffset {
                anchor = anchorBounds
                // top-right corner of the menu = top-right corner of the button; kept inside the window
                val x = (anchorBounds.right - popupContentSize.width).coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin))
                val y = anchorBounds.top.coerceIn(margin, (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin))
                return androidx.compose.ui.unit.IntOffset(x, y)
            }
        }
    }
    androidx.compose.ui.window.Popup(popupPositionProvider = provider, onDismissRequest = onDismissRequest, properties = androidx.compose.ui.window.PopupProperties(focusable = true)) {
        val t = androidx.compose.animation.core.rememberTransition(state, label = "menu")
        val p by t.animateFloat(
            transitionSpec = { if (off) androidx.compose.animation.core.snap() else if (targetState) androidx.compose.animation.core.spring(0.74f, 300f * k) else androidx.compose.animation.core.spring(0.95f, 520f * k) },
            label = "p",
        ) { if (it) 1f else 0f }
        val radiusEnd = 16.dp.rs()
        Column(
            // as wide as the longest item (no empty band on the right), within M3's 112–280dp
            modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Max).widthIn(min = 160.dp, max = 280.dp)
                .graphicsLayer {
                    // the whole menu — its fill AND its items together — unfolds from the button's corner on one
                    // spring and overshoots a little (nothing moves on its own inside it)
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)
                    val q = p.coerceAtLeast(0f)
                    scaleX = 0.7f + 0.3f * q
                    scaleY = 0.45f + 0.55f * q
                    alpha = (p * 3f).coerceIn(0f, 1f)
                    shape = RoundedCornerShape(radiusEnd)
                    clip = true
                    shadowElevation = 6.dp.toPx() * p.coerceIn(0f, 1f)
                }
                .background(androidx.compose.ui.graphics.lerp(cs.surfaceContainerHighest, cs.surfaceContainer, p.coerceIn(0f, 1f)))
                .padding(vertical = 4.dp),
        ) {
            // items arrive a beat after the container has grown
            Column(Modifier.graphicsLayer { alpha = ((p - 0.2f) / 0.5f).coerceIn(0f, 1f) }) {
                CompositionLocalProvider(LocalContentColor provides cs.onSurface) { content() }
            }
        }
    }
}


/** bottom sheets open and close calmly (standard motion, no bounce) while the rest of the app stays expressive */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalmMotion(content: @Composable () -> Unit) {
    androidx.compose.material3.MaterialExpressiveTheme(
        colorScheme = MaterialTheme.colorScheme, motionScheme = SheetMotion,
        shapes = MaterialTheme.shapes, typography = MaterialTheme.typography, content = content,
    )
}

/**
 * sheets: smooth and without bounce, but settling quickly — a long spring tail kept the sheet "moving" for a while
 * after it reached full height, and while it moves the sheet takes the scroll (the photos wouldn't scroll at once)
 */
private object SheetMotion : androidx.compose.material3.MotionScheme {
    private val std = androidx.compose.material3.MotionScheme.standard()
    override fun <T> defaultSpatialSpec(): androidx.compose.animation.core.FiniteAnimationSpec<T> = androidx.compose.animation.core.spring(1f, 620f)
    override fun <T> fastSpatialSpec(): androidx.compose.animation.core.FiniteAnimationSpec<T> = androidx.compose.animation.core.spring(1f, 900f)
    override fun <T> slowSpatialSpec(): androidx.compose.animation.core.FiniteAnimationSpec<T> = androidx.compose.animation.core.spring(1f, 420f)
    override fun <T> defaultEffectsSpec(): androidx.compose.animation.core.FiniteAnimationSpec<T> = std.defaultEffectsSpec()
    override fun <T> fastEffectsSpec(): androidx.compose.animation.core.FiniteAnimationSpec<T> = std.fastEffectsSpec()
    override fun <T> slowEffectsSpec(): androidx.compose.animation.core.FiniteAnimationSpec<T> = std.slowEffectsSpec()
}

/** where the search bar grows from: bounds (in root) of the button that opened search */
object SearchMorph {
    var origin: androidx.compose.ui.geometry.Rect? = null
    /** the bar lives at the bottom (opened from the FAB, like Telegram on iPhone) */
    var bottom = false
    fun open(from: androidx.compose.ui.geometry.Rect?, atBottom: Boolean = false) { origin = from; bottom = atBottom; Nav.push(Route.Search) }
}

/** search icon button that remembers its bounds, so the search bar grows out of it (springy container transform) */
@Composable
fun SearchButton(modifier: Modifier = Modifier, floating: Boolean = Prefs.floatingBars.value) {
    var rect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val cs = MaterialTheme.colorScheme
    val m = modifier.onGloballyPositioned { rect = it.boundsInRoot() }
    if (floating) MorphIconButton(Icons.Outlined.Search, "Поиск", { SearchMorph.open(rect) }, m.padding(2.dp), size = 46.dp, color = cs.surfaceContainerHigh, tint = cs.onSurface, elevation = 3.dp, glass = true)
    else MorphIconButton(Icons.Outlined.Search, "Поиск", { SearchMorph.open(rect) }, m)
}

/** label text: keeps its size, wraps between words (never inside one), ellipsis only as the very last resort */
@Composable
fun FitText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    maxLines: Int = 2,
    @Suppress("UNUSED_PARAMETER") minSize: androidx.compose.ui.unit.TextUnit = 9.sp,
    textAlign: androidx.compose.ui.text.style.TextAlign = androidx.compose.ui.text.style.TextAlign.Center,
) {
    Text(
        text, modifier, color = color, style = style.copy(lineBreak = androidx.compose.ui.text.style.LineBreak.Heading, hyphens = androidx.compose.ui.text.style.Hyphens.None),
        textAlign = textAlign, maxLines = maxLines, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
    )
}

/** one action of [AdaptiveTiles] */
class TileAction(val icon: ImageVector, val label: String, val selected: Boolean = false, val onClick: () -> Unit)

/**
 * M3 Expressive connected group of action tiles that never squeezes its labels: if every label (word by word)
 * fits a tall tile, the tiles sit in one row (icon over label); otherwise they become a 2-column grid of wide tiles
 * (icon beside a label of up to two lines). Text keeps its size. The press layer stays inside each tile.
 */
@Composable
fun AdaptiveTiles(items: List<TileAction>, modifier: Modifier = Modifier, height: Dp = 84.dp, container: Color? = null, content: Color? = null) {
    val cs = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.labelLarge
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxWidth()) {
        val gap = 4.dp
        val n = items.size.coerceAtLeast(1)
        val tileW = (maxWidth - gap * (n - 1)) / n
        val density = androidx.compose.ui.platform.LocalDensity.current
        // the longest single word of every label must fit the tile's inner width
        val fitsRow = items.all { it2 ->
            it2.label.split(' ').all { w -> with(density) { measurer.measure(w, style).size.width.toDp() } <= tileW - 12.dp }
        }
        @Composable
        fun tile(i: Int, a: TileAction, count: Int, mod: Modifier, wide: Boolean) {
            val outer = 24.dp
            val inner = 8.dp
            val first = i == 0
            val last = i == count - 1
            MorphBox(
                onClick = a.onClick, rest = if (first || last) outer else inner, pressed = 30.dp, squish = 0.95f,
                color = if (a.selected) (if (container != null) Color.White.copy(alpha = 0.34f) else cs.secondaryContainer) else container ?: cs.surfaceContainerHighest,
                contentColor = if (a.selected) (content ?: cs.onSecondaryContainer) else content ?: cs.onSurface,
                modifier = mod,
            ) {
                if (!wide) Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)) {
                    Icon(a.icon, null)
                    Spacer(Modifier.height(6.dp))
                    FitText(a.label, style)
                } else Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Icon(a.icon, null)
                    Spacer(Modifier.width(12.dp))
                    FitText(a.label, style, textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                }
            }
        }
        if (fitsRow) Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(gap)) {
            items.forEachIndexed { i, a -> tile(i, a, items.size, Modifier.weight(1f).heightIn(min = height), wide = false) }
        } else Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(gap)) {
            items.chunked(2).forEachIndexed { r, row ->
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(gap), modifier = Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                    row.forEachIndexed { c, a ->
                        val idx = r * 2 + c
                        tile(idx, a, items.size, Modifier.weight(1f).fillMaxHeight().heightIn(min = 60.dp), wide = true)
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
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
fun ActionTiles(items: List<GroupButton>, modifier: Modifier = Modifier, container: Color? = null, content: Color? = null) =
    AdaptiveTiles(items.map { b -> TileAction(b.icon ?: Icons.Outlined.Search, b.label ?: b.desc ?: "", b.selected, b.onClick) }, modifier, height = 64.dp, container = container, content = content)

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
                    color = cs.primaryContainer, contentColor = cs.onPrimaryContainer, elevation = 3.dp, glass = true,
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


/**
 * A 0→[max] linear loop that runs only while [running] (and motion is on). Unlike an infinite transition it
 * stops completely when not needed, so a paused player or a hidden card costs no frames at all.
 */
@Composable
fun loopPhase(running: Boolean, periodMs: Int, max: Float): androidx.compose.runtime.State<Float> {
    val a = remember { androidx.compose.animation.core.Animatable(0f) }
    // energy saving (Settings → Energy saving) stops it when idle; otherwise it keeps going like before
    val idleStop = Prefs.powerSave.value && Prefs.psIdleAnims.value
    val on = (running || !idleStop) && Prefs.motion.value != "off"
    LaunchedEffect(on) {
        if (!on) return@LaunchedEffect
        while (true) {
            val left = ((max - a.value) / max * periodMs).toInt().coerceAtLeast(16)
            a.animateTo(max, androidx.compose.animation.core.tween(left, easing = androidx.compose.animation.core.LinearEasing))
            a.snapTo(0f)
        }
    }
    return a.asState()
}
