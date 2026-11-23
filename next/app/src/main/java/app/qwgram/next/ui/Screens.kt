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
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloat
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
    val pickPhoto = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val f = uriToCacheFile(ctx, uri)
            if (f != null) Td.send(TdApi.SetProfilePhoto(TdApi.InputChatPhotoStatic(TdApi.InputFileLocal(f.absolutePath)), false)) { r ->
                Toast.makeText(ctx, if (r is TdApi.Error) "Не удалось: ${r.message}" else "Фото обновляется…", Toast.LENGTH_SHORT).show()
            }
        }
    }
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
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { MorphIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", { Nav.pop() }) },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = 32.dp), modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (me) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            UserAvatar(user, 120.dp, Modifier.clip(CircleShape).clickable { pickPhoto.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                            Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.PhotoCamera, "Сменить фото", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    } else if (chat == null) UserAvatar(user, 120.dp) else ChatAvatar(chat, 120.dp)
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
                            val uid = TdStore.privateUserId(chat)
                            if (uid != 0L && chat.type is TdApi.ChatTypePrivate && !TdStore.isBot(chat)) ProfileAction(Icons.Outlined.Lock, "Секретный", false, Modifier.weight(1f)) {
                                scope.launch { Td.callOrNull(TdApi.CreateNewSecretChat(uid))?.let { Nav.push(Route.Chat(it.id)) } ?: Toast.makeText(ctx, "Не удалось начать секретный чат", Toast.LENGTH_SHORT).show() }
                            }
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
                    if (chat != null && (TdStore.isGroup(chat) || TdStore.isChannel(chat))) {
                        Spacer(Modifier.height(12.dp))
                        GroupInfo(chat)
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
                    modifier = Modifier.clickable { Nav.jumpTo = m.chatId to m.id; if (Nav.routes.getOrNull(Nav.routes.size - 2) == Route.Chat(m.chatId)) Nav.pop() else Nav.push(Route.Chat(m.chatId)) },
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
            androidx.compose.foundation.text.BasicText(
                label, maxLines = 1, style = MaterialTheme.typography.labelMedium.copy(color = if (active) cs.onSecondaryContainer else cs.onSurface),
                autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 12.sp, stepSize = 0.5.sp),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
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
    Box(Modifier.aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable { Nav.jumpTo = m.chatId to m.id; if (Nav.routes.getOrNull(Nav.routes.size - 2) == Route.Chat(m.chatId)) Nav.pop() else Nav.push(Route.Chat(m.chatId)) }) {
        bmp?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

// ------------------------------------------------------------------ search

private enum class SearchFilter(val title: String) { All("Все"), People("Люди"), Groups("Группы"), Channels("Каналы"), Bots("Боты"), Messages("Сообщения") }

private fun matches(c: TdApi.Chat, f: SearchFilter): Boolean = when (f) {
    SearchFilter.All -> true
    SearchFilter.People -> c.type is TdApi.ChatTypePrivate && !TdStore.isBot(c)
    SearchFilter.Groups -> TdStore.isGroup(c)
    SearchFilter.Channels -> TdStore.isChannel(c)
    SearchFilter.Bots -> TdStore.isBot(c)
    SearchFilter.Messages -> false
}

/** segmented card item: rounded outer corners for the first/last row of a block, 2dp gaps */
private fun segShape(i: Int, n: Int) = RoundedCornerShape(
    topStart = if (i == 0) 20.dp else 4.dp, topEnd = if (i == 0) 20.dp else 4.dp,
    bottomStart = if (i == n - 1) 20.dp else 4.dp, bottomEnd = if (i == n - 1) 20.dp else 4.dp,
)

@Composable
fun SearchScreen() {
    var q by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(SearchFilter.All) }
    val local = remember { mutableStateListOf<Long>() }
    val global = remember { mutableStateListOf<Long>() }
    var messages by remember { mutableStateOf<List<TdApi.Message>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var recent by remember { mutableStateOf<List<Long>>(emptyList()) }
    var top by remember { mutableStateOf<List<Long>>(emptyList()) }
    LaunchedEffect(Unit) {
        recent = Td.callOrNull(TdApi.SearchRecentlyFoundChats("", null, 20))?.chatIds?.toList().orEmpty()
        top = Td.callOrNull(TdApi.GetTopChats(TdApi.TopChatCategoryUsers(), 12))?.chatIds?.toList().orEmpty()
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(250); try { focus.requestFocus() } catch (_: Exception) {} }
    LaunchedEffect(q) {
        val query = q.trim()
        if (query.isEmpty()) { local.clear(); global.clear(); messages = emptyList(); return@LaunchedEffect }
        delay(250)
        busy = true
        val q2 = query.removePrefix("@")
        Td.callOrNull(TdApi.SearchChats(query, null, 30))?.let { local.clear(); local.addAll(it.chatIds.toList()) }
        // contacts by name, then public chats / bots / channels by @username, then server-side chat search
        Td.callOrNull(TdApi.SearchContacts(q2, 20))?.userIds?.forEach { uid ->
            Td.callOrNull(TdApi.CreatePrivateChat(uid, true))?.let { c -> if (c.id !in local) local.add(c.id) }
        }
        val g = LinkedHashSet<Long>()
        if (q2.length >= 3) Td.callOrNull(TdApi.SearchPublicChats(q2, null))?.chatIds?.forEach { g.add(it) }
        Td.callOrNull(TdApi.SearchChatsOnServer(query, null, 30))?.chatIds?.forEach { g.add(it) }
        global.clear(); global.addAll(g.filter { it !in local })
        Td.callOrNull(TdApi.SearchMessages().apply { this.query = query; offset = ""; limit = 50; chatList = null })?.let { messages = it.messages.toList() }
        busy = false
    }
    val cs = MaterialTheme.colorScheme
    fun open(c: TdApi.Chat) { Td.send(TdApi.AddRecentlyFoundChat(c.id)); Nav.push(Route.Chat(c.id)) }
    Column(Modifier.fillMaxSize().background(cs.surface)) {
        // floating search pill + filters
        Column(Modifier.statusBarsPadding().padding(top = 6.dp)) {
            Surface(shape = RoundedCornerShape(28.dp), color = cs.surfaceContainerHigh, shadowElevation = 2.dp, modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth().height(56.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                    MorphIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", { Nav.pop() })
                    BasicTextField(
                        value = q, onValueChange = { q = it }, singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
                        cursorBrush = SolidColor(cs.primary),
                        modifier = Modifier.weight(1f).focusRequester(focus),
                        decorationBox = { inner -> if (q.isEmpty()) Text("Чаты, люди, сообщения", color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge); inner() },
                    )
                    androidx.compose.animation.AnimatedVisibility(q.isNotEmpty(), enter = androidx.compose.animation.scaleIn(Springs.fastSpatial()) + androidx.compose.animation.fadeIn(Springs.effects()), exit = androidx.compose.animation.scaleOut(Springs.exit()) + androidx.compose.animation.fadeOut(Springs.exit())) {
                        MorphIconButton(Icons.Filled.Close, "Очистить", { q = "" })
                    }
                    if (busy) LoadingIndicator(Modifier.size(36.dp).padding(end = 4.dp))
                }
            }
            androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SearchFilter.entries.toList()) { f ->
                    androidx.compose.material3.FilterChip(
                        selected = filter == f, onClick = { filter = f }, label = { Text(f.title) },
                        leadingIcon = if (filter == f) ({ Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) }) else null,
                        shape = RoundedCornerShape(if (filter == f) 16.dp else 8.dp),
                    )
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (q.isBlank()) {
                val recentChats = recent.mapNotNull { TdStore.chats[it] }.filter { matches(it, filter) }
                val topChats = top.mapNotNull { TdStore.chats[it] }.filter { matches(it, filter) }
                if (topChats.isNotEmpty()) {
                    item { SectionHeader("Часто пишете") }
                    item {
                        Surface(shape = RoundedCornerShape(24.dp), color = cs.surfaceContainerLow) {
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                topChats.forEach { c ->
                                    Column(Modifier.width(72.dp).clip(RoundedCornerShape(20.dp)).clickable { open(c) }.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        ChatAvatar(c, 52.dp)
                                        Spacer(Modifier.height(6.dp))
                                        Text(c.title.substringBefore(' '), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
                if (recentChats.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) { SectionHeader("Недавние") }
                            QTextButton(onClick = { Td.send(TdApi.ClearRecentlyFoundChats()); recent = emptyList() }) { Text("Очистить") }
                        }
                    }
                    itemsIndexed(recentChats, key = { _, c -> "r${c.id}" }) { i, c ->
                        Box(Modifier.clip(segShape(i, recentChats.size)).background(cs.surfaceContainerLow)) { ChatRow(c, null) { open(c) } }
                    }
                }
                if (topChats.isEmpty() && recentChats.isEmpty()) item { EmptyState(Icons.Outlined.Search, "Начните вводить", "Ищите чаты, людей, каналы по @username и сообщения") }
            } else {
                val loc = local.mapNotNull { TdStore.chats[it] }.filter { matches(it, filter) }
                val glo = global.mapNotNull { TdStore.chats[it] }.filter { matches(it, filter) }
                val showChats = filter != SearchFilter.Messages
                val showMessages = filter == SearchFilter.All || filter == SearchFilter.Messages
                if (showChats && loc.isNotEmpty()) {
                    item { SectionHeader("Чаты и контакты") }
                    itemsIndexed(loc, key = { _, c -> "l${c.id}" }) { i, c -> Box(Modifier.clip(segShape(i, loc.size)).background(cs.surfaceContainerLow)) { ChatRow(c, null) { open(c) } } }
                }
                if (showChats && glo.isNotEmpty()) {
                    item { SectionHeader("Глобальный поиск") }
                    itemsIndexed(glo, key = { _, c -> "g${c.id}" }) { i, c -> Box(Modifier.clip(segShape(i, glo.size)).background(cs.surfaceContainerLow)) { ChatRow(c, null) { open(c) } } }
                }
                val msgs = if (showMessages) messages.filter { TdStore.chats[it.chatId] != null } else emptyList()
                if (msgs.isNotEmpty()) {
                    item { SectionHeader("Сообщения") }
                    itemsIndexed(msgs, key = { _, m -> "m${m.chatId}_${m.id}" }) { i, m ->
                        val c = TdStore.chats[m.chatId] ?: return@itemsIndexed
                        Row(
                            Modifier.fillMaxWidth().clip(segShape(i, msgs.size)).background(cs.surfaceContainerLow)
                                .clickable { Nav.jumpTo = c.id to m.id; Nav.push(Route.Chat(c.id)) }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ChatAvatar(c, 44.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(c.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    Text(Texts.listDate(m.date), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                                }
                                Text(highlight(Texts.preview(m), q.trim(), cs.primary), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                if (!busy && (if (showChats) loc.isEmpty() && glo.isEmpty() else true) && msgs.isEmpty()) item {
                    EmptyState(Icons.Outlined.SearchOff, "Ничего не найдено", "Попробуйте другой запрос или фильтр")
                }
            }
        }
    }
}

private fun highlight(text: String, q: String, color: androidx.compose.ui.graphics.Color) = androidx.compose.ui.text.buildAnnotatedString {
    append(text)
    if (q.isNotEmpty()) {
        var i = text.indexOf(q, ignoreCase = true)
        while (i >= 0) {
            addStyle(androidx.compose.ui.text.SpanStyle(color = color, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), i, i + q.length)
            i = text.indexOf(q, i + q.length, ignoreCase = true)
        }
    }
}

/** expressive empty state: a morphing-cornered shape with an icon, title and hint */
@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String) {
    val cs = MaterialTheme.colorScheme
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "es")
    val rot by t.animateFloat(-8f, 8f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(2400), androidx.compose.animation.core.RepeatMode.Reverse), label = "r")
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(112.dp).graphicsLayer { rotationZ = rot }.clip(androidx.compose.material3.MaterialShapes.Cookie9Sided.asShape()).background(cs.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(40.dp).graphicsLayer { rotationZ = -rot }, tint = cs.onSecondaryContainer)
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(sub, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
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
            item {
                var addContact by remember { mutableStateOf(false) }
                MorphBox(
                    onClick = { addContact = true }, rest = 20.dp, pressed = 12.dp, squish = 0.98f,
                    color = MaterialTheme.colorScheme.surfaceContainerLow, contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        RowIcon(Icons.Outlined.PersonAdd, 0)
                        Spacer(Modifier.width(16.dp))
                        Text("Добавить контакт", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (addContact) AddContactDialog { addContact = false }
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
