package app.qwgram.next.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.qwgram.next.Prefs
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Check

/** header action: a floating round button with its own container and press morph, or a plain icon in classic mode */
@Composable
fun HeaderIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    if (Prefs.floatingBars.value) MorphIconButton(icon, desc, onClick, modifier.padding(2.dp), size = 46.dp, color = cs.surfaceContainerHigh, tint = cs.onSurface, elevation = 3.dp)
    else MorphIconButton(icon, desc, onClick, modifier)
}

/**
 * M3 top app bar. Floating style: the title lies on a fade (surface → transparent) and the buttons float as
 * separate round containers; content scrolls underneath. Classic style: a flat surfaceContainer bar.
 * Titles can be centred (Appearance → Panels).
 */
@Composable
fun FadeHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = { Nav.pop() },
    titleAlpha: () -> Float = { 1f },
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    val floating = Prefs.floatingBars.value
    val center = Prefs.centerTitle.value
    Box(
        modifier.fillMaxWidth()
            .then(
                if (floating) Modifier.background(androidx.compose.ui.graphics.Brush.verticalGradient(0f to cs.surface, 0.62f to cs.surface.copy(alpha = 0.92f), 1f to cs.surface.copy(alpha = 0f)))
                else Modifier.background(cs.surfaceContainer),
            )
            .statusBarsPadding().padding(bottom = if (floating) 18.dp else 0.dp),
    ) {
        val titleBlock: @Composable (Modifier) -> Unit = { m ->
            Column(m.graphicsLayer { alpha = titleAlpha() }, horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (center) {
            CenteredBar(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 6.dp),
                start = { if (onBack != null) HeaderIcon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", onBack) },
                title = { titleBlock(Modifier.padding(horizontal = 8.dp)) },
                end = { Row(verticalAlignment = Alignment.CenterVertically) { actions() } },
            )
        } else {
            Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) HeaderIcon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", onBack) else Spacer(Modifier.width(10.dp))
                titleBlock(Modifier.weight(1f).padding(horizontal = 10.dp))
                actions()
            }
        }
    }
}

/**
 * Bar with a truly centred title: the title gets all the width that is left after reserving the wider of the two
 * side slots on both sides, so it is centred and only ellipsized when it really doesn't fit.
 */
@Composable
fun CenteredBar(modifier: Modifier, start: @Composable () -> Unit, title: @Composable () -> Unit, end: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(
        contents = listOf({ start() }, { title() }, { end() }),
        modifier = modifier,
    ) { (s, t, e), c ->
        val loose = c.copy(minWidth = 0, minHeight = 0)
        val sp = s.map { it.measure(loose) }
        val ep = e.map { it.measure(loose) }
        val sw = sp.sumOf { it.width }
        val ew = ep.sumOf { it.width }
        val side = maxOf(sw, ew)
        val tw = (c.maxWidth - 2 * side).coerceAtLeast(0)
        // if one side is empty and the title is long, let it use the free side too (still visually near centre)
        val tMax = if (tw < c.maxWidth / 2) (c.maxWidth - sw - ew).coerceAtLeast(0) else tw
        val tp = t.map { it.measure(loose.copy(maxWidth = tMax)) }
        val h = c.maxHeight.takeIf { it != androidx.compose.ui.unit.Constraints.Infinity } ?: listOf(sp, tp, ep).flatten().maxOfOrNull { it.height } ?: 0
        layout(c.maxWidth, h) {
            var x = 0
            sp.forEach { it.place(x, (h - it.height) / 2); x += it.width }
            var xe = c.maxWidth
            ep.forEach { xe -= it.width; it.place(xe, (h - it.height) / 2) }
            tp.forEach {
                val cx = ((c.maxWidth - it.width) / 2).coerceIn(sw, (c.maxWidth - ew - it.width).coerceAtLeast(sw))
                it.place(cx, (h - it.height) / 2)
            }
        }
    }
}

/** Settings page frame: content scrolls under a fading M3 header. */
@Composable
fun PageScaffold(title: String, actions: @Composable () -> Unit = {}, content: LazyListScope.() -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    var headerH by remember { mutableStateOf(96.dp) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = headerH - 12.dp, bottom = 32.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) { content() }
        FadeHeader(title, Modifier.onSizeChanged { headerH = with(density) { it.height.toDp() } }, actions = { actions() })
    }
}

/** Expressive segmented list group: one card per section, rows separated by 2dp gaps. */
class GroupScope {
    val rows = mutableListOf<@Composable (Modifier) -> Unit>()
    fun row(content: @Composable (Modifier) -> Unit) { rows.add(content) }
}

fun LazyListScope.group(header: String? = null, footer: String? = null, build: GroupScope.() -> Unit) {
    val g = GroupScope().apply(build)
    if (header != null) item { GroupHeader(header) } else item { Spacer(Modifier.height(10.dp)) }
    val n = g.rows.size
    g.rows.forEachIndexed { i, r ->
        item {
            val big = (when (Prefs.uiRound.value) { 0 -> 12; 2 -> 28; else -> 20 }).dp
            val top = if (i == 0) big else 4.dp
            val bottom = if (i == n - 1) big else 4.dp
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(top, top, bottom, bottom)).background(rowColor())) {
                r(Modifier)
            }
        }
    }
    if (footer != null) item {
        Text(footer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@Composable
private fun rowColor(): Color = if (Prefs.amoled.value && LocalDark.current) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLow

@Composable
fun GroupHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
}

@Composable
fun RowIcon(icon: ImageVector, tone: Int = 1) {
    val cs = MaterialTheme.colorScheme
    val (bg, fg) = when (tone) {
        0 -> cs.primaryContainer to cs.onPrimaryContainer
        2 -> cs.tertiaryContainer to cs.onTertiaryContainer
        3 -> cs.errorContainer to cs.onErrorContainer
        else -> cs.secondaryContainer to cs.onSecondaryContainer
    }
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) { Icon(icon, null, tint = fg) }
}

@Composable
fun BaseRow(title: String, sub: String? = null, icon: ImageVector? = null, tone: Int = 1, danger: Boolean = false, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { RowIcon(icon, if (danger) 3 else tone); Spacer(Modifier.width(16.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            if (!sub.isNullOrBlank()) Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

fun GroupScope.nav(title: String, sub: String? = null, icon: ImageVector? = null, tone: Int = 1, onClick: () -> Unit) = row {
    BaseRow(title, sub, icon, tone, onClick = onClick) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
}

fun GroupScope.action(title: String, sub: String? = null, icon: ImageVector? = null, danger: Boolean = false, onClick: () -> Unit) = row {
    BaseRow(title, sub, icon, danger = danger, onClick = onClick)
}

fun GroupScope.value(title: String, value: String, icon: ImageVector? = null, onClick: (() -> Unit)? = null) = row {
    BaseRow(title, null, icon, onClick = onClick) { Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
}

fun GroupScope.switch(title: String, pref: Prefs.B, sub: String? = null, icon: ImageVector? = null, onChange: (Boolean) -> Unit = {}) = row {
    val v = pref.state.value
    BaseRow(title, sub, icon, onClick = { pref.value = !v; onChange(!v) }) {
        QSwitch(v) { pref.value = it; onChange(it) }
    }
}

fun GroupScope.switchState(title: String, checked: Boolean, sub: String? = null, icon: ImageVector? = null, onChange: (Boolean) -> Unit) = row {
    BaseRow(title, sub, icon, onClick = { onChange(!checked) }) { QSwitch(checked, onChange) }
}

/** connected button group (M3 Expressive): selected one morphs into a pill and widens, labels never get cut */
fun <T> GroupScope.segmented(title: String, pref: Prefs.Item<T>, options: List<Pair<T, String>>, sub: String? = null) = row {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (sub != null) Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        ConnectedGroup(options, pref.state.value) { pref.value = it }
    }
}

@Composable
fun <T> ConnectedGroup(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberHaptic()
    Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEachIndexed { i, (v, label) ->
            val sel = v == selected
            val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val weight by androidx.compose.animation.core.animateFloatAsState(
                when { pressed -> 1.18f; sel -> 1.12f; else -> 1f }, if (pressed) Springs.pressIn() else Springs.fastSpatial(), label = "w",
            )
            val inner by androidx.compose.animation.core.animateDpAsState(when { sel -> 24.dp; pressed -> 4.dp; else -> 8.dp }, Springs.fastSpatial(), label = "r")
            val start = if (i == 0) 24.dp else inner.coerceAtLeast(0.dp)
            val end = if (i == options.lastIndex) 24.dp else inner.coerceAtLeast(0.dp)
            val bg by androidx.compose.animation.animateColorAsState(if (sel) cs.secondaryContainer else cs.surfaceContainerHighest, Springs.effects(), label = "bg")
            val fg = if (sel) cs.onSecondaryContainer else cs.onSurfaceVariant
            Box(
                Modifier.weight(weight).fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = start, topEnd = end, bottomEnd = end, bottomStart = start))
                    .background(bg)
                    .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple()) { haptic(androidx.compose.ui.hapticfeedback.HapticFeedbackType.SegmentTick); onSelect(v) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.animation.AnimatedVisibility(
                        sel,
                        enter = androidx.compose.animation.expandHorizontally(Springs.fastSpatial()) + androidx.compose.animation.scaleIn(Springs.fastSpatial()),
                        exit = androidx.compose.animation.shrinkHorizontally(Springs.exit()) + androidx.compose.animation.scaleOut(Springs.exit()),
                    ) {
                        Row { Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = fg); Spacer(Modifier.width(4.dp)) }
                    }
                    androidx.compose.foundation.text.BasicText(
                        label, maxLines = 1,
                        style = MaterialTheme.typography.labelLarge.copy(color = fg),
                        autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp, stepSize = 0.5.sp),
                    )
                }
            }
        }
    }
}

/** opens a radio-list dialog (for longer option lists) */
fun <T> GroupScope.pick(title: String, pref: Prefs.Item<T>, options: List<Pair<T, String>>, icon: ImageVector? = null) = row {
    var open by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == pref.state.value }?.second ?: ""
    BaseRow(title, current, icon, onClick = { open = true }) { Icon(Icons.Filled.UnfoldMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    if (open) RadioDialog(title, options, pref.state.value, { pref.value = it; open = false }) { open = false }
}

@Composable
fun <T> RadioDialog(title: String, options: List<Pair<T, String>>, selected: T, onPick: (T) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (v, label) ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).selectable(selected = v == selected, role = Role.RadioButton) { onPick(v) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = v == selected, onClick = null)
                        Spacer(Modifier.width(14.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { QTextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

fun GroupScope.sliderInt(title: String, pref: Prefs.I, range: IntRange, step: Int = 1, format: (Int) -> String = { it.toString() }) = row {
    var local by remember(pref.state.value) { mutableFloatStateOf(pref.state.value.toFloat()) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(format(local.toInt()), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            modifier = Modifier.noSwipeBack(),
            value = local,
            onValueChange = { v ->
                val snapped = (range.first + Math.round((v - range.first) / step) * step).coerceIn(range.first, range.last)
                local = snapped.toFloat(); pref.value = snapped
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
        )
    }
}

fun GroupScope.sliderFloat(title: String, pref: Prefs.F, range: ClosedFloatingPointRange<Float>, steps: Int, format: (Float) -> String) = row {
    var local by remember(pref.state.value) { mutableFloatStateOf(pref.state.value) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(format(local), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        val stepSize = (range.endInclusive - range.start) / (steps + 1)
        Slider(
            modifier = Modifier.noSwipeBack(),
            value = local,
            onValueChange = { v -> local = (range.start + Math.round((v - range.start) / stepSize) * stepSize).coerceIn(range.start, range.endInclusive) },
            onValueChangeFinished = { pref.value = local }, valueRange = range,
        )
    }
}

fun GroupScope.custom(content: @Composable ColumnScope.() -> Unit) = row {
    Column(Modifier.fillMaxWidth().padding(16.dp)) { content() }
}

@Composable
fun BottomSpacer() = Spacer(Modifier.navigationBarsPadding().height(16.dp))

/** M3 switch with the check icon in the thumb (Expressive style) */
@Composable
fun QSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked, onCheckedChange = onChange,
        thumbContent = if (checked) ({ Icon(androidx.compose.material.icons.Icons.Filled.Check, null, Modifier.size(androidx.compose.material3.SwitchDefaults.IconSize)) }) else null,
    )
}
