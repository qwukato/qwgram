package app.qwgram.next.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.qwgram.next.Prefs

/** one entry the side menu can show; [group]: 0 chats, 1 app, 2 other (the menu draws them as three cards) */
class DrawerItem(val id: String, val title: String, val icon: ImageVector, val group: Int)

/** everything that can live in the side menu (Settings → Side menu) */
val DRAWER_CATALOG = listOf(
    DrawerItem("profile", "Мой профиль", Icons.Outlined.Person, 0),
    DrawerItem("compose", "Новое сообщение", Icons.Outlined.Edit, 0),
    DrawerItem("group", "Новая группа", Icons.Outlined.GroupAdd, 0),
    DrawerItem("channel", "Новый канал", Icons.Outlined.Campaign, 0),
    DrawerItem("contacts", "Контакты", Icons.Outlined.Contacts, 0),
    DrawerItem("saved", "Избранное", Icons.Outlined.Bookmark, 0),
    DrawerItem("archive", "Архив", Icons.Outlined.Archive, 0),
    DrawerItem("settings", "Настройки", Icons.Outlined.Settings, 1),
    DrawerItem("appearance", "Внешний вид", Icons.Outlined.Palette, 1),
    DrawerItem("features", "Функции qwgram", Icons.Outlined.AutoAwesome, 1),
    DrawerItem("folders", "Папки", Icons.Outlined.FolderOpen, 1),
    DrawerItem("notifications", "Уведомления", Icons.Outlined.Notifications, 1),
    DrawerItem("privacy", "Конфиденциальность", Icons.Outlined.Lock, 1),
    DrawerItem("data", "Данные и память", Icons.Outlined.DataUsage, 1),
    DrawerItem("power", "Энергосбережение", Icons.Outlined.BatterySaver, 1),
    DrawerItem("sessions", "Устройства", Icons.Outlined.Devices, 1),
    DrawerItem("proxy", "Прокси", Icons.Outlined.VpnKey, 1),
    DrawerItem("invite", "Пригласить друзей", Icons.Outlined.Share, 2),
)

const val DRAWER_DEFAULT = "profile,group,contacts,saved,archive,settings,appearance,features,folders,invite"

/** the menu's items, in the user's order */
fun drawerItems(): List<DrawerItem> {
    val ids = Prefs.drawerItems.value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    return ids.mapNotNull { id -> DRAWER_CATALOG.firstOrNull { it.id == id } }
}

private fun save(ids: List<String>) { Prefs.drawerItems.value = ids.joinToString(",") }

/**
 * Settings → Side menu. Sections match the menu's cards (Chats / App / Other); every entry has a switch (shown or
 * not), and the shown ones can be moved up and down inside their section. A live preview of the menu sits on top.
 */
@Composable
fun DrawerEditorPage() = PageScaffold("Боковое меню") {
    val chosen = drawerItems()
    val ids = chosen.map { it.id }
    item { DrawerPreview(chosen) }
    listOf(0 to "Чаты", 1 to "Приложение", 2 to "Прочее").forEach { (g, title) ->
        val on = chosen.filter { it.group == g }
        val off = DRAWER_CATALOG.filter { it.group == g && it.id !in ids }
        group(title, footer = if (g == 2) "Включённые пункты можно двигать стрелками внутри раздела." else null) {
            on.forEachIndexed { i, it ->
                custom { EditorRow(it, true, canUp = i > 0, canDown = i < on.lastIndex,
                    onUp = { val j = ids.indexOf(on[i - 1].id); save(ids.toMutableList().apply { remove(it.id); add(j, it.id) }) },
                    onDown = { val j = ids.indexOf(on[i + 1].id); save(ids.toMutableList().apply { remove(it.id); add(j, it.id) }) },
                    onToggle = { save(ids - it.id) }) }
            }
            off.forEach { it -> custom { EditorRow(it, false, false, false, {}, {}, onToggle = { save(ids + it.id) }) } }
        }
    }
    group {
        action("Вернуть как было", icon = Icons.Outlined.RestartAlt) { Prefs.drawerItems.value = DRAWER_DEFAULT }
    }
}

@Composable
private fun EditorRow(it: DrawerItem, on: Boolean, canUp: Boolean, canDown: Boolean, onUp: () -> Unit, onDown: () -> Unit, onToggle: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        RowIcon(it.icon)
        Spacer(Modifier.width(16.dp))
        Text(it.title, style = MaterialTheme.typography.bodyLarge, color = if (on) cs.onSurface else cs.onSurfaceVariant, modifier = Modifier.weight(1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        // arrows only for shown items; they slide in when the switch turns on
        androidx.compose.animation.AnimatedVisibility(on, enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(), exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally()) {
            Row {
                MorphIconButton(Icons.Outlined.KeyboardArrowUp, "Выше", onUp, enabled = canUp, size = 40.dp, tint = if (canUp) cs.onSurfaceVariant else cs.outlineVariant)
                MorphIconButton(Icons.Outlined.KeyboardArrowDown, "Ниже", onDown, enabled = canDown, size = 40.dp, tint = if (canDown) cs.onSurfaceVariant else cs.outlineVariant)
            }
        }
        Spacer(Modifier.width(6.dp))
        QSwitch(on, { onToggle() })
    }
}

/** the menu as it will look: its cards and entries, updated live */
@Composable
private fun DrawerPreview(items: List<DrawerItem>) {
    val cs = MaterialTheme.colorScheme
    androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp.rs()), color = cs.surfaceContainerLow, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Так будет выглядеть меню", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
            listOf(0, 1, 2).forEach { g ->
                val part = items.filter { it.group == g }
                if (part.isNotEmpty()) androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp.rs()), color = cs.surfaceContainerHigh) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        part.forEach { it ->
                            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(it.icon, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(14.dp))
                                Text(it.title, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            if (items.isEmpty()) Text("Меню пустое", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.padding(4.dp))
        }
    }
}
