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
import androidx.compose.material.icons.filled.Check

/** Settings page frame: large collapsing app bar + lazy content. */
@Composable
fun PageScaffold(title: String, actions: @Composable () -> Unit = {}, content: LazyListScope.() -> Unit) {
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { MorphIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", { Nav.pop() }) },
                actions = { actions() },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = 32.dp + pad.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) { content() }
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

/** segmented button row (≤ 4 options) */
fun <T> GroupScope.segmented(title: String, pref: Prefs.Item<T>, options: List<Pair<T, String>>, sub: String? = null) = row {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (sub != null) Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, (v, label) ->
                SegmentedButton(
                    selected = pref.state.value == v,
                    onClick = { pref.value = v },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
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
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
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
            value = local, onValueChange = { local = it; pref.value = it.toInt() },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = ((range.last - range.first) / step - 1).coerceAtLeast(0),
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
        Slider(value = local, onValueChange = { local = it }, onValueChangeFinished = { pref.value = local }, valueRange = range, steps = steps)
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
