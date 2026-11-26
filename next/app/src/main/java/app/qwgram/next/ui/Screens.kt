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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.draw.drawBehind
import org.drinkless.tdlib.TdApi
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding

// ------------------------------------------------------------------ profile

@Composable
fun ProfileScreen(chatId: Long) {
    val me = chatId == 0L
    val chat = if (me) null else TdStore.chats[chatId]
    val user = if (me) TdStore.me else chat?.let { TdStore.users[TdStore.privateUserId(it)] }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var bio by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }
    val mediaCache = remember { androidx.compose.runtime.mutableStateMapOf<Int, List<TdApi.Message>>() }
    var shownTab by remember { mutableStateOf(0) }
    val media = mediaCache[shownTab].orEmpty()
    var gallery by remember { mutableStateOf<List<TdApi.Photo>?>(null) }
    val intro = rememberIntro()
    fun openAvatars() {
        scope.launch {
            val list = if (user != null) Td.callOrNull(TdApi.GetUserProfilePhotos(user.id, 0, 100))?.photos?.map { TdApi.Photo(false, it.minithumbnail, it.sizes) }.orEmpty()
            else emptyList()
            val fallback = chat?.photo?.let { ph -> listOf(TdApi.Photo(false, ph.minithumbnail, arrayOf(TdApi.PhotoSize("x", ph.big, 640, 640, intArrayOf())))) }.orEmpty()
            val all = list.ifEmpty { fallback }
            if (all.isNotEmpty()) gallery = all
        }
    }
    val pickPhoto = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val f = uriToCacheFile(ctx, uri)
            if (f != null) Td.send(TdApi.SetProfilePhoto(TdApi.InputChatPhotoStatic(TdApi.InputFileLocal(f.absolutePath)), false)) { r ->
                Toast.makeText(ctx, if (r is TdApi.Error) "Не удалось: ${r.message}" else "Фото обновляется…", Toast.LENGTH_SHORT).show()
            }
        }
    }
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
            if (mediaCache[tab] == null) {
                val r = Td.callOrNull(TdApi.SearchChatMessages().apply {
                    this.chatId = chat.id; query = ""; fromMessageId = 0; offset = 0; limit = 90; filter = f
                })
                mediaCache[tab] = r?.messages?.toList().orEmpty()
            }
            shownTab = tab
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
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    Scaffold(
        topBar = {
            FadeHeader(title, subtitle = status, titleAlpha = {
                // the name appears in the bar once the big one scrolls away
                if (gridState.firstVisibleItemIndex > 0) 1f else ((gridState.firstVisibleItemScrollOffset - 260f) / 140f).coerceIn(0f, 1f)
            }, backgroundAlpha = {
                if (gridState.firstVisibleItemIndex > 0) 1f else ((gridState.firstVisibleItemScrollOffset - 160f) / 140f).coerceIn(0f, 1f)
            }, actions = { ProfileMenu(chat, user, me) })
        },
    ) { pad ->
        LazyVerticalGrid(GridCells.Fixed(3), state = gridState, contentPadding = PaddingValues(top = pad.calculateTopPadding() - 12.dp, bottom = 32.dp), modifier = Modifier.fillMaxSize().blurSource(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProfileBanner(
                        chat, user, me, title, status, topInset = pad.calculateTopPadding(),
                        onAvatar = { openAvatars() },
                        onCamera = if (me) ({ pickPhoto.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) }) else null,
                        actions = { onColor ->
                            // one row of equal tiles (icon over label, never wrapping); on a coloured banner they turn translucent
                            val acts: List<Triple<androidx.compose.ui.graphics.vector.ImageVector, String, Pair<Boolean, () -> Unit>>> = when {
                                chat != null && !TdStore.isSaved(chat) -> {
                                    val liveMuted = TdStore.isMuted(TdStore.chats[chat.id] ?: chat)
                                    listOf(
                                        Triple(Icons.AutoMirrored.Outlined.Chat, "Написать", false to {
                                            if (Nav.routes.getOrNull(Nav.routes.size - 2) == Route.Chat(chat.id)) Nav.pop() else Nav.push(Route.Chat(chat.id))
                                        }),
                                        Triple(if (liveMuted) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications, if (liveMuted) "Включить" else "Без звука", liveMuted to { setMuted(TdStore.chats[chat.id] ?: chat, !liveMuted) }),
                                        Triple(Icons.Outlined.Search, "Поиск", false to {
                                            ChatSearchRequest.chatId = chat.id
                                            if (Nav.routes.getOrNull(Nav.routes.size - 2) == Route.Chat(chat.id)) Nav.pop() else Nav.push(Route.Chat(chat.id))
                                        }),
                                    )
                                }
                                me -> listOf(
                                    Triple(Icons.Outlined.PhotoCamera, "Фото", false to { pickPhoto.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) }),
                                    Triple(Icons.Outlined.Palette, "Стиль", false to { Nav.push(Route.Page("appearance")) }),
                                    Triple(Icons.Outlined.AddCircleOutline, "История", false to { Stories.compose = true }),
                                    Triple(Icons.Outlined.Settings, "Настройки", false to { Nav.pop(); HomeState.tab = HomeTab.Settings }),
                                )
                                else -> emptyList()
                            }
                            if (acts.isNotEmpty()) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                acts.forEach { (icon, label, st) -> ProfileAction(icon, label, st.first, Modifier.weight(1f), onColor, st.second) }
                            }
                        },
                    )
                    Spacer(Modifier.height(14.dp))
                    if (user != null && Prefs.profileMusic.value) Box(Modifier.appearIn(1)) { ProfileMusic(user.id) }
                    Spacer(Modifier.height(14.dp))
                    // gifts and collectibles (NFT) on the profile
                    val giftOwner = remember(user?.id, chat?.id) { if (user != null) TdApi.MessageSenderUser(user.id) else chat?.let { TdApi.MessageSenderChat(it.id) } }
                    if (giftOwner != null && Prefs.profileGifts.value) Box(Modifier.appearIn(2)) {
                        Column { ProfileGifts(giftOwner); Spacer(Modifier.height(14.dp)) }
                    }
                    // info as segments: one rounded tile per fact (tap copies it), the notifications switch last
                    run {
                        val un = user?.usernames?.activeUsernames?.firstOrNull()
                            ?: (chat?.type as? TdApi.ChatTypeSupergroup)?.let { TdStore.supergroups[it.supergroupId]?.usernames?.activeUsernames?.firstOrNull() }
                        val rows = buildList {
                            if (user != null && user.phoneNumber.isNotBlank()) add(Triple(Icons.Outlined.Phone, if (me && Prefs.hidePhone.value) "Скрыт" else "+" + user.phoneNumber, "Телефон"))
                            if (un != null) add(Triple(Icons.Outlined.AlternateEmail, "@$un", "Имя пользователя"))
                            if (cleanBio.isNotEmpty()) add(Triple(Icons.Outlined.Info, cleanBio, if (user != null) "О себе" else "Описание"))
                            if (un != null) add(Triple(Icons.Outlined.Link, "t.me/$un", "Ссылка"))
                            if (Prefs.showIds.value) {
                                if (user != null) add(Triple(Icons.Outlined.Tag, user.id.toString(), "ID"))
                                else if (chat != null) add(Triple(Icons.Outlined.Tag, chat.id.toString(), "ID чата"))
                            }
                        }
                        val withSwitch = chat != null && !TdStore.isSaved(chat)
                        val n = rows.size + if (withSwitch) 1 else 0
                        Column(Modifier.fillMaxWidth().appearIn(3), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            rows.forEachIndexed { i, (icon, v, label) ->
                                Row(
                                    Modifier.segment(i, n).clickable(enabled = !(me && Prefs.hidePhone.value && label == "Телефон")) { copyText(ctx, v) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RowIcon(icon)
                                    Spacer(Modifier.width(16.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(v, style = MaterialTheme.typography.bodyLarge)
                                        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            if (withSwitch) {
                                val live = TdStore.chats[chat!!.id] ?: chat
                                val muted = TdStore.isMuted(live)
                                Row(
                                    Modifier.segment(n - 1, n).clickable { setMuted(live, !muted) }.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RowIcon(if (muted) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications)
                                    Spacer(Modifier.width(16.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Уведомления", style = MaterialTheme.typography.bodyLarge)
                                        Text(if (muted) "Выключены" else "Включены", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    QSwitch(!muted) { setMuted(live, !it) }
                                }
                            }
                        }
                    }
                    // every block rises in, one after another (groups and channels too)
                    if (chat != null && (TdStore.isGroup(chat) || TdStore.isChannel(chat))) {
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.appearIn(4)) { GroupInfo(chat) }
                    }
                    if (chat != null) {
                        Spacer(Modifier.height(16.dp))
                        Box(Modifier.appearIn(5)) { ConnectedGroup(listOf(0 to "Медиа", 1 to "Файлы", 2 to "Ссылки", 3 to "Голос"), tab) { tab = it } }
                        androidx.compose.animation.AnimatedVisibility(mediaCache[shownTab]?.isEmpty() == true, enter = fadeIn(Springs.effects()), exit = fadeOut(Springs.exit())) {
                            Text("Пока ничего нет", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp))
                        }
                        if (tab != shownTab) LoadingIndicator(Modifier.padding(8.dp).size(36.dp))
                    }
                }
            }
            if (shownTab == 0) itemsIndexed(media, key = { _, it -> "t0_" + it.id }) { i, m ->
                Box(Modifier.animateItem(fadeInSpec = Springs.effects(), placementSpec = Springs.smooth(), fadeOutSpec = Springs.exit()).then(if (intro && i < 12) Modifier.appearIn(6 + i / 3) else Modifier)) { MediaTile(m) }
            }
            else itemsIndexed(media, key = { _, it -> "t${shownTab}_" + it.id }, span = { _, _ -> androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) { i, m ->
                ListItem(
                    headlineContent = { Text(Texts.preview(m), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text(Texts.listDate(m.date)) },
                    leadingContent = {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                            Icon(when (shownTab) { 1 -> Icons.Outlined.Description; 2 -> Icons.Outlined.Link; else -> Icons.Outlined.Mic }, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    },
                    modifier = Modifier.animateItem(fadeInSpec = Springs.effects(), placementSpec = Springs.smooth(), fadeOutSpec = Springs.exit())
                        .then(if (intro && i < 8) Modifier.appearIn(6 + i) else Modifier)
                        .clickable { Nav.jumpTo = m.chatId to m.id; if (Nav.routes.getOrNull(Nav.routes.size - 2) == Route.Chat(m.chatId)) Nav.pop() else Nav.push(Route.Chat(m.chatId)) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                )
            }
        }
    }
    gallery?.let { PhotoGallery(it, 0) { gallery = null } }
}

/** the first second of a profile: its first media tiles rise in with the rest (not when scrolled to later) */
@Composable
private fun rememberIntro(): Boolean {
    var on by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(1400); on = false }
    return on
}

@Composable
private fun ProfileAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, modifier: Modifier, onColor: Boolean = false, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    // active (e.g. muted) tiles get rounder and tonal; on a coloured banner everything is white-on-glass
    val rest by androidx.compose.animation.core.animateDpAsState(if (active) 30.dp else 22.dp, Springs.fastSpatial(), label = "pa")
    val bgT = when { onColor -> Color.White.copy(alpha = if (active) 0.32f else 0.18f); active -> cs.secondaryContainer; else -> cs.surfaceContainerHigh }
    val fgT = when { onColor -> Color.White; active -> cs.onSecondaryContainer; else -> cs.onSurface }
    val bg by androidx.compose.animation.animateColorAsState(bgT, Springs.effects(), label = "pb")
    MorphBox(onClick = onClick, modifier = modifier.height(76.dp), rest = rest, pressed = 14.dp, color = bg, contentColor = fgT) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = if (onColor) Color.White else if (active) cs.onSecondaryContainer else cs.primary)
            Spacer(Modifier.height(6.dp))
            androidx.compose.foundation.text.BasicText(
                label, maxLines = 1, style = MaterialTheme.typography.labelLarge.copy(color = fgT),
                autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 13.sp, stepSize = 0.5.sp),
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
    val keyboardCtl = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusMgr = androidx.compose.ui.platform.LocalFocusManager.current
    // the keyboard goes away as the chat opens
    fun open(c: TdApi.Chat) { keyboardCtl?.hide(); focusMgr.clearFocus(); Td.send(TdApi.AddRecentlyFoundChat(c.id)); openChat(c) }
    // opened from the FAB: the bar lives at the bottom (Telegram on iPhone) and grows out of the FAB there
    val fromBottom = remember { SearchMorph.bottom.also { SearchMorph.bottom = false } }
    val dens = androidx.compose.ui.platform.LocalDensity.current
    val winH = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.height.toFloat()
    val imeIns = WindowInsets.ime
    val navIns = WindowInsets.navigationBars
    // container transform: the bar grows out of the button that opened search (springy), the rest fades in after
    val origin = remember { SearchMorph.origin.also { SearchMorph.origin = null } }
    val grow = remember { androidx.compose.animation.core.Animatable(if (origin == null || Prefs.motion.value == "off") 1f else 0f) }
    LaunchedEffect(Unit) {
        grow.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.72f, stiffness = 340f * Prefs.motionSpeed.value * Prefs.motionSpeed.value))
    }
    var hostPos = androidx.compose.ui.geometry.Offset.Zero
    val statusTopPx = androidx.compose.foundation.layout.WindowInsets.statusBars.getTop(androidx.compose.ui.platform.LocalDensity.current).toFloat()
    val searchScope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    // closing: the bar shrinks back into the button it came from, then the screen goes
    fun closeSearch() {
        if (closing) return
        closing = true
        keyboardCtl?.hide()
        if (origin == null || Prefs.motion.value == "off") { Nav.pop(); return }
        searchScope.launch {
            grow.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 520f * Prefs.motionSpeed.value * Prefs.motionSpeed.value, visibilityThreshold = 0.01f))
            Nav.popNow()
        }
    }
    androidx.activity.compose.BackHandler { closeSearch() }
    // floating search pill + filters (above the results, or under them when opened from the FAB)
    val pillBlock: @Composable (Modifier) -> Unit = { mod -> Column(mod) {
        if (fromBottom) {
            androidx.compose.foundation.lazy.LazyRow(Modifier.graphicsLayer { alpha = ((grow.value - 0.5f) / 0.5f).coerceIn(0f, 1f); translationY = (1f - grow.value.coerceIn(0f, 1f)) * (if (fromBottom) 24.dp else (-24).dp).toPx() }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SearchFilter.entries.toList()) { f ->
                    androidx.compose.material3.FilterChip(
                        selected = filter == f, onClick = { filter = f }, label = { Text(f.title) },
                        leadingIcon = if (filter == f) ({ Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) }) else null,
                        shape = RoundedCornerShape(if (filter == f) 16.dp else 8.dp),
                    )
                }
            }
        }
            Surface(
                shape = RoundedCornerShape(28.dp.rs()), color = cs.surfaceContainerHigh, shadowElevation = 2.dp,
                modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth().height(56.dp)
                    .layout { m, c ->
                        // the bar's final spot is fixed (screen top, under the status bar): known from the first frame,
                        // so the grow starts exactly on the button with no jump
                        hostPos = if (!fromBottom) androidx.compose.ui.geometry.Offset(12.dp.toPx(), statusTopPx + 6.dp.toPx())
                        else androidx.compose.ui.geometry.Offset(12.dp.toPx(), winH - maxOf(imeIns.getBottom(dens), navIns.getBottom(dens)) - 8.dp.toPx() - 56.dp.toPx())
                        val fw = c.maxWidth; val fh = c.maxHeight.coerceAtMost(56.dp.roundToPx())
                        val p = grow.value
                        val o = origin
                        if (o == null) { val pl = m.measure(c); return@layout layout(pl.width, pl.height) { pl.place(0, 0) } }
                        fun lerp(a: Float, b: Float) = a + (b - a) * p
                        val w = lerp(o.width, fw.toFloat()).toInt().coerceAtLeast(1)
                        val h = lerp(o.height, fh.toFloat()).toInt().coerceAtLeast(1)
                        val pl = m.measure(androidx.compose.ui.unit.Constraints.fixed(w, h))
                        layout(fw, fh) { pl.place(lerp(o.left - hostPos.x, 0f).toInt(), lerp(o.top - hostPos.y, 0f).toInt()) }
                    },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp).graphicsLayer { alpha = ((grow.value - 0.35f) / 0.5f).coerceIn(0f, 1f) }) {
                    MorphIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", { closeSearch() })
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
                // as the pill shrinks back into the button it already shows the magnifier, so the button is "there" at once
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = (1f - grow.value * 2.4f).coerceIn(0f, 1f) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Search, null, tint = cs.onSurface)
                }
            }
        if (!fromBottom) {
            androidx.compose.foundation.lazy.LazyRow(Modifier.graphicsLayer { alpha = ((grow.value - 0.5f) / 0.5f).coerceIn(0f, 1f); translationY = (1f - grow.value.coerceIn(0f, 1f)) * (if (fromBottom) 24.dp else (-24).dp).toPx() }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SearchFilter.entries.toList()) { f ->
                    androidx.compose.material3.FilterChip(
                        selected = filter == f, onClick = { filter = f }, label = { Text(f.title) },
                        leadingIcon = if (filter == f) ({ Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) }) else null,
                        shape = RoundedCornerShape(if (filter == f) 16.dp else 8.dp),
                    )
                }
            }
        }
    } }
    Column(Modifier.fillMaxSize().graphicsLayer { }.drawBehind { drawRect(cs.surface.copy(alpha = grow.value.coerceIn(0f, 1f))) }) {
        if (!fromBottom) pillBlock(Modifier.statusBarsPadding().padding(top = 6.dp)) else Spacer(Modifier.statusBarsPadding())
        LazyColumn(Modifier.fillMaxWidth().weight(1f).graphicsLayer { alpha = ((grow.value - 0.55f) / 0.45f).coerceIn(0f, 1f) }, contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                                .clickable { keyboardCtl?.hide(); focusMgr.clearFocus(); Nav.jumpTo = c.id to m.id; Nav.push(Route.Chat(c.id)) }.padding(horizontal = 16.dp, vertical = 10.dp),
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
        // FAB search: the bar at the bottom, riding on the keyboard
        if (fromBottom) pillBlock(Modifier.imePadding().navigationBarsPadding().padding(bottom = 8.dp))
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
    // a few gentle wobbles when the empty state appears, then still (no endless redraws)
    val wob = remember { androidx.compose.animation.core.Animatable(-14f) }
    LaunchedEffect(Unit) { wob.animateTo(0f, androidx.compose.animation.core.spring(0.25f, 40f)) }
    val sway = loopPhase(true, 4800, (2 * Math.PI).toFloat())
    val rot = if (app.qwgram.next.Power.on && Prefs.psIdleAnims.value) wob.value else kotlin.math.sin(sway.value) * 8f
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
    val density = androidx.compose.ui.platform.LocalDensity.current
    var headerH by remember { mutableStateOf(96.dp) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        ChatList(TdApi.ChatListArchive(), PaddingValues(top = headerH - headerOverlap()), showArchive = false) { actionChat = it }
        FadeHeader("Архив", Modifier.onSizeChanged { headerH = with(density) { it.height.toDp() } }, subtitle = "Чаты без уведомлений о новых сообщениях")
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
    val density = androidx.compose.ui.platform.LocalDensity.current
    var headerH by remember { mutableStateOf(96.dp) }
    Box(Modifier.fillMaxSize()) {
        FadeHeader("Контакты", Modifier.zIndex(1f).onSizeChanged { headerH = with(density) { it.height.toDp() } }, subtitle = "${users.size} " + Texts.plural(users.size, "контакт", "контакта", "контактов"), onBack = null)
        LazyColumn(Modifier.fillMaxSize().blurSource(), contentPadding = PaddingValues(top = headerH - headerOverlap(), bottom = pad.calculateBottomPadding() + 24.dp)) {
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
