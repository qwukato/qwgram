package app.qwgram.next.ui

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
    DrawerItem("blur", "Размытие фона", Icons.Outlined.BlurOn, 1),
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

/** Settings → Side menu: remove, add and reorder the menu's entries */
@Composable
fun DrawerEditorPage() = PageScaffold("Боковое меню") {
    val items = drawerItems()
    val ids = items.map { it.id }
    group("В меню", footer = "Стрелками меняется порядок. Пункты сами группируются: чаты, приложение, прочее.") {
        if (items.isEmpty()) custom { Text("Меню пустое — добавьте пункты ниже", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp)) }
        items.forEachIndexed { i, it ->
            custom {
                Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(it.icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(16.dp))
                    Text(it.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1)
                    MorphIconButton(Icons.Outlined.KeyboardArrowUp, "Выше", { if (i > 0) save(ids.toMutableList().apply { add(i - 1, removeAt(i)) }) }, enabled = i > 0)
                    MorphIconButton(Icons.Outlined.KeyboardArrowDown, "Ниже", { if (i < ids.lastIndex) save(ids.toMutableList().apply { add(i + 1, removeAt(i)) }) }, enabled = i < ids.lastIndex)
                    MorphIconButton(Icons.Outlined.RemoveCircleOutline, "Убрать", { save(ids - it.id) }, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    val rest = DRAWER_CATALOG.filter { it.id !in ids }
    if (rest.isNotEmpty()) group("Добавить") {
        rest.forEach { it ->
            custom {
                Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(it.icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(16.dp))
                    Text(it.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1)
                    MorphIconButton(Icons.Outlined.Add, "Добавить", { save(ids + it.id) }, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
    group {
        action("Вернуть как было", icon = Icons.Outlined.RestartAlt) { Prefs.drawerItems.value = DRAWER_DEFAULT }
    }
}
