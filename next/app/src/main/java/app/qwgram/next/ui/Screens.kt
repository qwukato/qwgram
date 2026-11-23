package app.qwgram.next.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.qwgram.next.Prefs
import app.qwgram.next.td.Td
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.Texts
import app.qwgram.next.td.rememberFileBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.runtime.getValue
import org.drinkless.tdlib.TdApi

// ------------------------------------------------------------------ profile

@Composable
fun ProfileScreen(chatId: Long) {
    val me = chatId == 0L
    val chat = if (me) null else TdStore.chats[chatId]
    val user = if (me) TdStore.me else chat?.let { TdStore.users[TdStore.privateUserId(it)] }
    val ctx = LocalContext.current
    var bio by remember { mutableStateOf("") }
    var media by remember { mutableStateOf<List<TdApi.Message>>(emptyList()) }
    var tab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(chatId) {
        if (user != null) Td.callOrNull(TdApi.GetUserFullInfo(user.id))?.let { bio = it.bio?.text.orEmpty() }
        chat?.type?.let { t ->
            if (t is TdApi.ChatTypeSupergroup) Td.callOrNull(TdApi.GetSupergroupFullInfo(t.supergroupId))?.let { bio = it.description }
            if (t is TdApi.ChatTypeBasicGroup) Td.callOrNull(TdApi.GetBasicGroupFullInfo(t.basicGroupId))?.let { bio = it.description }
        }
    }
    LaunchedEffect(chatId, tab) {
        if (chat != null) {
            val f: TdApi.SearchMessagesFilter = when (tab) {
                1 -> TdApi.SearchMessagesFilterDocument()
                2 -> TdApi.SearchMessagesFilterUrl()
                3 -> TdApi.SearchMessagesFilterVoiceNote()
                else -> TdApi.SearchMessagesFilterPhotoAndVideo()
            }
            val r = Td.callOrNull(TdApi.SearchChatMessages().apply {
                this.chatId = chat.id; query = ""; fromMessageId = 0; offset = 0; limit = 90; filter = f
            })
            media = r?.messages?.toList().orEmpty()
        }
    }
    val cleanBio = bio.replace(Regex("[\\u200B-\\u200F\\u2060-\\u2064\\uFEFF\\u3164\\u115F\\u1160\\u00AD]"), "").trim()
    val title = when {
        me -> user?.let { TdStore.fullName(it) } ?: ""
        chat != null && TdStore.isSaved(chat) -> "Избранное"
        else -> chat?.title ?: ""
    }
    val status = when {
        me -> "в сети"
        chat != null -> Texts.chatStatus(chat)
        else -> ""
    }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { MorphIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", { Nav.pop() }) },
            )
        },
    ) { pad ->
        LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = 32.dp), modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (me || chat == null) UserAvatar(user, 120.dp) else ChatAvatar(chat, 120.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status == "в сети") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (chat != null && !TdStore.isSaved(chat)) {
                        Spacer(Modifier.height(20.dp))
                        val muted = TdStore.isMuted(chat)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProfileAction(Icons.AutoMirrored.Outlined.Chat, "Сообщение", false, Modifier.weight(1f)) {
                                if (Nav.routes.getOrNull(Nav.routes.size - 2) == Route.Chat(chat.id)) Nav.pop() else Nav.push(Route.Chat(chat.id))
                            }
                            ProfileAction(if (muted) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications, if (muted) "Без звука" else "Звук", muted, Modifier.weight(1f)) { setMuted(chat, !muted) }
                            val un = (user?.usernames?.activeUsernames?.firstOrNull()
                                ?: (chat.type as? TdApi.ChatTypeSupergroup)?.let { TdStore.supergroups[it.supergroupId]?.usernames?.activeUsernames?.firstOrNull() })
                            if (un != null) ProfileAction(Icons.Outlined.Share, "Поделиться", false, Modifier.weight(1f)) {
                                val i = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, "https://t.me/$un")
                                ctx.startActivity(android.content.Intent.createChooser(i, null))
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            val rows = buildList {
                                if (user != null && user.phoneNumber.isNotBlank()) add(Triple(Icons.Outlined.Phone, if (me && Prefs.hidePhone.value) "Скрыт" else "+" + user.phoneNumber, "Телефон"))
                                val un = user?.usernames?.activeUsernames?.firstOrNull()
                                    ?: (chat?.type as? TdApi.ChatTypeSupergroup)?.let { TdStore.supergroups[it.supergroupId]?.usernames?.activeUsernames?.firstOrNull() }
                                if (un != null) add(Triple(Icons.Outlined.AlternateEmail, "@$un", "Имя пользователя"))
                                if (cleanBio.isNotEmpty()) add(Triple(Icons.Outlined.Info, cleanBio, if (user != null) "О себе" else "Описание"))
                                if (un != null) add(Triple(Icons.Outlined.Link, "t.me/$un", "Ссылка"))
                                if (user != null) add(Triple(Icons.Outlined.Tag, user.id.toString(), "ID"))
                            }
                            rows.forEach { (icon, v, label) ->
                                ListItem(
                                    headlineContent = { Text(v) },
                                    supportingContent = { Text(label) },
                                    leadingContent = { Icon(icon, null) },
                                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                )
                            }
                            if (chat != null && !TdStore.isSaved(chat)) {
                                val muted = TdStore.isMuted(chat)
                                ListItem(
                                    headlineContent = { Text("Уведомления") },
                                    supportingContent = { Text(if (muted) "Выключены" else "Включены") },
                                    leadingContent = { Icon(Icons.Outlined.Notifications, null) },
                                    trailingContent = { QSwitch(!muted) { setMuted(chat, !it) } },
                                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                )
                            }
                        }
                    }
                    if (chat != null) {
                        Spacer(Modifier.height(16.dp))
                        androidx.compose.material3.PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = 0.dp, containerColor = MaterialTheme.colorScheme.surface) {
                            listOf("Медиа", "Файлы", "Ссылки", "Голосовые").forEachIndexed { i, t ->
                                androidx.compose.material3.Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
                            }
                        }
                        if (media.isEmpty()) Text("Пока ничего нет", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp))
                    }
                }
            }
            if (tab == 0) items(media, key = { it.id }) { m -> MediaTile(m) }
            else items(media, key = { it.id }, span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) { m ->
                ListItem(
                    headlineContent = { Text(Texts.preview(m), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text(Texts.listDate(m.date)) },
                    leadingContent = {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                            Icon(when (tab) { 1 -> Icons.Outlined.Description; 2 -> Icons.Outlined.Link; else -> Icons.Outlined.Mic }, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    },
                    modifier = Modifier.clickable { if (Nav.routes.getOrNull(Nav.routes.size - 2) == Route.Chat(m.chatId)) Nav.pop() else Nav.push(Route.Chat(m.chatId)) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                )
            }
        }
    }
}

@Composable
private fun ProfileAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val rest by androidx.compose.animation.core.animateDpAsState(if (active) 32.dp else 20.dp, Springs.fastSpatial(), label = "pa")
    val bg by androidx.compose.animation.animateColorAsState(if (active) cs.secondaryContainer else cs.surfaceContainerHigh, Springs.effects(), label = "pb")
    MorphBox(onClick = onClick, modifier = modifier.height(72.dp), rest = rest, pressed = 12.dp, color = bg, contentColor = if (active) cs.onSecondaryContainer else cs.onSurface) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = if (active) cs.onSecondaryContainer else cs.primary)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun MediaTile(m: TdApi.Message) {
    val file = when (val c = m.content) {
        is TdApi.MessagePhoto -> c.photo.sizes.minByOrNull { kotlin.math.abs(it.width - 320) }?.photo
        is TdApi.MessageVideo -> c.video.thumbnail?.file
        else -> null
    }
    val bmp = rememberFileBitmap(file, maxSide = 320)
    Box(Modifier.aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable { Nav.push(Route.Chat(m.chatId)) }) {
        bmp?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

// ------------------------------------------------------------------ search

@Composable
fun SearchScreen() {
    var q by remember { mutableStateOf("") }
    val local = remember { mutableStateListOf<Long>() }
    val global = remember { mutableStateListOf<Long>() }
    var messages by remember { mutableStateOf<List<TdApi.Message>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(250); try { focus.requestFocus() } catch (_: Exception) {} }
    LaunchedEffect(q) {
        val query = q.trim()
        if (query.isEmpty()) { local.clear(); global.clear(); messages = emptyList(); return@LaunchedEffect }
        delay(250)
        busy = true
        Td.callOrNull(TdApi.SearchChats(query, null, 30))?.let { local.clear(); local.addAll(it.chatIds.toList()) }
        Td.callOrNull(TdApi.SearchChatsOnServer(query, null, 30))?.let { r -> global.clear(); global.addAll(r.chatIds.filter { it !in local }) }
        Td.callOrNull(TdApi.SearchMessages().apply { this.query = query; offset = ""; limit = 40; chatList = null })?.let { messages = it.messages.toList() }
        busy = false
    }
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize()) {
        Surface(color = cs.surfaceContainer) {
            Row(Modifier.statusBarsPadding().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                MorphIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", { Nav.pop() })
                BasicTextField(
                    value = q, onValueChange = { q = it }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
                    cursorBrush = SolidColor(cs.primary),
                    modifier = Modifier.weight(1f).focusRequester(focus),
                    decorationBox = { inner -> if (q.isEmpty()) Text("Поиск чатов и сообщений", color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge); inner() },
                )
                if (q.isNotEmpty()) MorphIconButton(Icons.Filled.Close, "Очистить", { q = "" })
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            if (q.isBlank()) {
                item { SectionHeader("Недавние чаты") }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TdStore.chatsIn(TdApi.ChatListMain()).take(12).forEach { c ->
                            Column(Modifier.width(68.dp).clip(RoundedCornerShape(16.dp)).clickable { Nav.push(Route.Chat(c.id)) }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                ChatAvatar(c, 56.dp)
                                Spacer(Modifier.height(6.dp))
                                Text(if (TdStore.isSaved(c)) "Избранное" else c.title.substringBefore(' '), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            if (busy) item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { LoadingIndicator(Modifier.size(36.dp)) } }
            if (local.isNotEmpty()) {
                item { SectionHeader("Чаты и контакты") }
                items(local.toList(), key = { "l$it" }) { id -> TdStore.chats[id]?.let { c -> ChatRow(c, null) { Nav.push(Route.Chat(c.id)) } } }
            }
            if (global.isNotEmpty()) {
                item { SectionHeader("Глобальный поиск") }
                items(global.toList(), key = { "g$it" }) { id -> TdStore.chats[id]?.let { c -> ChatRow(c, null) { Nav.push(Route.Chat(c.id)) } } }
            }
            if (messages.isNotEmpty()) {
                item { SectionHeader("Сообщения") }
                items(messages, key = { "m${it.chatId}_${it.id}" }) { m ->
                    val c = TdStore.chats[m.chatId] ?: return@items
                    ListItem(
                        headlineContent = { Text(c.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(Texts.preview(m), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { ChatAvatar(c, 48.dp) },
                        trailingContent = { Text(Texts.listDate(m.date), style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.clickable { Nav.push(Route.Chat(c.id)) },
                    )
                }
            }
            if (q.isNotBlank() && !busy && local.isEmpty() && global.isEmpty() && messages.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(96.dp).clip(androidx.compose.material3.MaterialShapes.Cookie9Sided.asShape()).background(cs.secondaryContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Search, null, tint = cs.onSecondaryContainer)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Ничего не найдено", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp))
}

// ------------------------------------------------------------------ archive

@Composable
fun ArchiveScreen() {
    var actionChat by remember { mutableStateOf<TdApi.Chat?>(null) }
    LaunchedEffect(Unit) { Td.send(TdApi.LoadChats(TdApi.ChatListArchive(), 100)) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Архив") },
                navigationIcon = { MorphIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", { Nav.pop() }) },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        Box(Modifier.padding(top = pad.calculateTopPadding())) {
            ChatList(TdApi.ChatListArchive(), PaddingValues(0.dp), showArchive = false) { actionChat = it }
        }
    }
    actionChat?.let { ChatActionsSheet(it) { actionChat = null } }
}

// ------------------------------------------------------------------ contacts

@Composable
fun ContactsTab(pad: PaddingValues) {
    var ids by remember { mutableStateOf<List<Long>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { Td.callOrNull(TdApi.GetContacts())?.let { ids = it.userIds.toList() } }
    val users = ids.mapNotNull { TdStore.users[it] }
        .filter { q.isBlank() || TdStore.fullName(it).contains(q, ignoreCase = true) }
        .sortedWith(compareByDescending<TdApi.User> { Texts.isOnline(it) }.thenByDescending { (it.status as? TdApi.UserStatusOffline)?.wasOnline ?: 0 })
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()
    Column(Modifier.fillMaxSize().nestedScroll(scroll.nestedScrollConnection)) {
        androidx.compose.material3.TopAppBar(title = { Text("Контакты") }, scrollBehavior = scroll)
        LazyColumn(contentPadding = PaddingValues(bottom = pad.calculateBottomPadding() + 24.dp)) {
            item {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).height(52.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        BasicTextField(q, { q = it }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.weight(1f),
                            decorationBox = { inner -> if (q.isEmpty()) Text("Поиск контактов", color = MaterialTheme.colorScheme.onSurfaceVariant); inner() })
                    }
                }
            }
            item { SectionHeader("Сортировка по времени захода") }
            items(users, key = { it.id }) { u ->
                ListItem(
                    headlineContent = { Text(TdStore.fullName(u), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text(Texts.status(u), color = if (Texts.isOnline(u)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingContent = { UserAvatar(u, 48.dp) },
                    modifier = Modifier.clickable {
                        scope.launch { Td.callOrNull(TdApi.CreatePrivateChat(u.id, false))?.let { Nav.push(Route.Chat(it.id)) } }
                    },
                )
            }
            if (users.isEmpty()) item {
                Text("Контактов пока нет", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center)
            }
        }
    }
}
