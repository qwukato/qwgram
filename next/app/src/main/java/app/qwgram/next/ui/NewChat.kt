package app.qwgram.next.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.qwgram.next.td.Td
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.Texts
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

/** "Новое сообщение": search contacts / global usernames, start a group or a channel. */
@Composable
fun NewMessagePage() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var ids by remember { mutableStateOf<List<Long>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<List<TdApi.Chat>>(emptyList()) }
    var mode by remember { mutableStateOf(0) } // 0 message, 1 pick group members
    val picked = remember { mutableStateListOf<Long>() }
    var askTitle by remember { mutableStateOf(false) }
    var newChannel by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { Td.callOrNull(TdApi.GetContacts())?.let { ids = it.userIds.toList() } }
    LaunchedEffect(q) {
        found = if (q.length >= 3 && mode == 0) {
            kotlinx.coroutines.delay(300)
            Td.callOrNull(TdApi.SearchPublicChats(q, null))?.chatIds?.toList()?.mapNotNull { id -> TdStore.chats.peek(id) ?: Td.callOrNull(TdApi.GetChat(id)) }.orEmpty()
        } else emptyList()
    }
    val users = ids.mapNotNull { TdStore.users[it] }
        .filter { q.isBlank() || TdStore.fullName(it).contains(q, true) || it.usernames?.activeUsernames?.any { u -> u.contains(q, true) } == true }
        .sortedBy { TdStore.fullName(it).lowercase() }

    fun openUser(u: TdApi.User) = scope.launch { Td.callOrNull(TdApi.CreatePrivateChat(u.id, false))?.let { Nav.pop(); Nav.push(Route.Chat(it.id)) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mode == 1) "Участники группы" else "Новое сообщение") },
                navigationIcon = { MorphIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", { if (mode == 1) { mode = 0; picked.clear() } else Nav.pop() }) },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(mode == 1 && picked.isNotEmpty(), enter = scaleIn(Springs.fastSpatial()) + fadeIn(Springs.effects()), exit = scaleOut(Springs.exit()) + fadeOut(Springs.exit())) {
                MorphFab(Icons.AutoMirrored.Filled.ArrowForward, "Далее", expanded = true, onClick = { askTitle = true }, modifier = Modifier.navigationBarsPadding())
            }
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = 120.dp)) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).height(52.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    BasicTextField(q, { q = it }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.weight(1f),
                        decorationBox = { inner -> if (q.isEmpty()) Text(if (mode == 1) "Кого добавить?" else "Имя или @username", color = MaterialTheme.colorScheme.onSurfaceVariant); inner() })
                }
            }
            if (mode == 1 && picked.isNotEmpty()) item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(picked.toList(), key = { it }) { id ->
                        val u = TdStore.users[id]
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp).clickable { picked.remove(id) }) {
                            UserAvatar(u, 52.dp)
                            Text(u?.firstName ?: "", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            if (mode == 0 && q.isBlank()) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        ActionCard("Создать группу", "До 200 000 участников", Icons.Outlined.GroupAdd, top = true) { mode = 1 }
                        ActionCard("Создать канал", "Публикации для подписчиков", Icons.Outlined.Campaign, top = false) { newChannel = true }
                    }
                }
            }
            if (found.isNotEmpty()) {
                item { SectionHeader("Глобальный поиск") }
                items(found, key = { "g" + it.id }) { c ->
                    ListItem(
                        headlineContent = { Text(c.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(Texts.chatStatus(c), maxLines = 1) },
                        leadingContent = { ChatAvatar(c, 48.dp) },
                        modifier = Modifier.clickable { Nav.pop(); Nav.push(Route.Chat(c.id)) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    )
                }
            }
            item { SectionHeader("Контакты") }
            items(users, key = { it.id }) { u ->
                val on = u.id in picked
                ListItem(
                    headlineContent = { Text(TdStore.fullName(u), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text(Texts.status(u), color = if (Texts.isOnline(u)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingContent = { UserAvatar(u, 48.dp) },
                    trailingContent = if (mode == 1) ({ Checkbox(on, { if (it) picked.add(u.id) else picked.remove(u.id) }) }) else null,
                    modifier = Modifier.clickable { if (mode == 1) { if (on) picked.remove(u.id) else picked.add(u.id) } else openUser(u) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                )
            }
        }
    }

    if (askTitle) NameDialog("Новая группа", "Название группы", withDescription = false, onDismiss = { askTitle = false }) { title, _ ->
        askTitle = false
        scope.launch {
            val r = Td.callOrNull(TdApi.CreateNewBasicGroupChat(picked.toLongArray(), title, 0))
            if (r != null) { Nav.pop(); Nav.push(Route.Chat(r.chatId)) } else Toast.makeText(ctx, "Не удалось создать группу", Toast.LENGTH_SHORT).show()
        }
    }
    if (newChannel) NameDialog("Новый канал", "Название канала", withDescription = true, onDismiss = { newChannel = false }) { title, desc ->
        newChannel = false
        scope.launch {
            val r = Td.callOrNull(TdApi.CreateNewSupergroupChat(title, false, true, desc, null, 0, false))
            if (r != null) { Nav.pop(); Nav.push(Route.Chat(r.id)) } else Toast.makeText(ctx, "Не удалось создать канал", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun ActionCard(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, top: Boolean, onClick: () -> Unit) {
    MorphBox(
        onClick = onClick, modifier = Modifier.fillMaxWidth(), rest = if (top) 20.dp else 20.dp, pressed = 12.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow, contentAlignment = Alignment.CenterStart, squish = 0.98f,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon, 0)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun NameDialog(title: String, label: String, withDescription: Boolean, onDismiss: () -> Unit, onDone: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(label) }, singleLine = true)
                if (withDescription) OutlinedTextField(desc, { desc = it }, label = { Text("Описание (необязательно)") })
            }
        },
        confirmButton = { QTextButton(enabled = name.isNotBlank(), onClick = { onDone(name.trim(), desc.trim()) }) { Text("Создать") } },
        dismissButton = { QTextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
