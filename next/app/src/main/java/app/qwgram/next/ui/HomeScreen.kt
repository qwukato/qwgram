package app.qwgram.next.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.qwgram.next.Prefs
import app.qwgram.next.td.Td
import app.qwgram.next.td.TdStore
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.outlined.Forum
import org.drinkless.tdlib.TdApi

enum class HomeTab { Chats, Contacts, Settings }

object HomeState {
    var tab by mutableStateOf(HomeTab.Chats)
}

@Composable
fun HomeScreen() {
    val drawer = rememberDrawerControl()
    val scope = rememberCoroutineScope()
    // the bottom bar never moves; only the extended FAB folds into an icon while scrolling down (M3 spec)
    var fabExpanded by remember { mutableStateOf(true) }
    val fabConn = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -4f) fabExpanded = false else if (available.y > 4f) fabExpanded = true
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(HomeState.tab) { fabExpanded = true }

    PushDrawer(drawer, drawerContent = { AppDrawer { drawer.close() } }) {
        Scaffold(
            bottomBar = { BottomNav(Modifier) },
            floatingActionButton = {
                androidx.compose.animation.AnimatedVisibility(
                    HomeState.tab == HomeTab.Chats && Prefs.fabStyle.value != "none",
                    enter = androidx.compose.animation.scaleIn(Springs.spatial()) + androidx.compose.animation.fadeIn(Springs.effects()),
                    exit = androidx.compose.animation.scaleOut(Springs.exit()) + androidx.compose.animation.fadeOut(Springs.exit()),
                ) {
                    MorphFab(
                        icon = Icons.Filled.Edit, text = "Написать",
                        expanded = fabExpanded && Prefs.fabStyle.value == "extended",
                        onClick = { Nav.push(Route.Page("compose")) },
                    )
                }
            },
            contentWindowInsets = WindowInsets(0),
        ) { pad ->
            Box(Modifier.fillMaxSize().nestedScroll(fabConn)) {
                AnimatedContent(
                    targetState = HomeState.tab,
                    transitionSpec = {
                        // M3 fade-through between top-level destinations
                        (fadeIn(Springs.smooth()) + androidx.compose.animation.scaleIn(Springs.smooth(), initialScale = 0.96f))
                            .togetherWith(fadeOut(Springs.exit()))
                    },
                    label = "tabs",
                ) { t ->
                    when (t) {
                        HomeTab.Chats -> ChatsTab(pad, drawer)
                        HomeTab.Contacts -> ContactsTab(pad)
                        HomeTab.Settings -> SettingsHome(pad)
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNav(modifier: Modifier) {
    val unread = TdStore.unreadChats
    val tabs = buildList {
        add(Triple(HomeTab.Chats, "Чаты", Icons.Filled.Chat to Icons.AutoMirrored.Outlined.Chat))
        if (Prefs.showContactsTab.value) add(Triple(HomeTab.Contacts, "Контакты", Icons.Filled.Person to Icons.Outlined.Person))
        add(Triple(HomeTab.Settings, "Настройки", Icons.Filled.Settings to Icons.Outlined.Settings))
    }
    val labels = Prefs.navLabels.value
    if (Prefs.navStyle.value == "floating") {
        Box(modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 12.dp), contentAlignment = Alignment.Center) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 3.dp, shadowElevation = 3.dp) {
                Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tabs.forEach { (t, title, icons) ->
                        val sel = HomeState.tab == t
                        val w by animateDpAsState(if (sel && labels) 120.dp else 56.dp, Springs.fastSpatial(), label = "w")
                        Surface(
                            onClick = { HomeState.tab = t },
                            shape = CircleShape,
                            color = if (sel) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.height(52.dp).width(w),
                        ) {
                            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                NavIcon(t, unread, if (sel) icons.first else icons.second, title)
                                if (sel && labels) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 1, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        ShortNavigationBar(modifier) {
            tabs.forEach { (t, title, icons) ->
                val sel = HomeState.tab == t
                ShortNavigationBarItem(
                    selected = sel,
                    onClick = { HomeState.tab = t },
                    icon = { NavIcon(t, unread, if (sel) icons.first else icons.second, title) },
                    label = { if (labels) Text(title) },
                )
            }
        }
    }
}

@Composable
private fun NavIcon(t: HomeTab, unread: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    if (t == HomeTab.Chats && unread > 0) {
        BadgedBox(badge = { Badge { Text(if (unread > 99) "99+" else unread.toString()) } }) { Icon(icon, title) }
    } else Icon(icon, title)
}

// ------------------------------------------------------------------ chats tab

private data class FolderTab(val title: String, val icon: String, val list: TdApi.ChatList)

@Composable
private fun folderTabs(): List<FolderTab> {
    val folders = TdStore.folders.toList()
    val all = FolderTab("Все чаты", "", TdApi.ChatListMain())
    val f = folders.map { FolderTab(it.name.text.text, it.icon?.name ?: "", TdApi.ChatListFolder(it.id)) }
    if (Prefs.hideAllChats.value && f.isNotEmpty()) return f
    val pos = TdStore.mainListPosition.coerceIn(0, f.size)
    return f.toMutableList().also { it.add(pos, all) }
}

@Composable
private fun ChatsTab(pad: PaddingValues, drawer: DrawerControl) {
    val openDrawer: () -> Unit = { drawer.open() }
    val tabs = folderTabs()
    val pager = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var actionChat by remember { mutableStateOf<TdApi.Chat?>(null) }

    LaunchedEffect(pager.currentPage, tabs.size) {
        tabs.getOrNull(pager.currentPage)?.let { Td.send(TdApi.LoadChats(it.list, 100)) }
    }

    val canOpenDrawer = Prefs.drawer.value && (tabs.size <= 1 || pager.currentPage == 0)
    Column(
        Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)
            .drawerSwipe(enabled = canOpenDrawer, edgeOnly = Prefs.listSwipe.value == "actions", drawer = drawer),
    ) {
        val scrolled = scrollBehavior.state.overlappedFraction > 0.01f
        val headerColor by androidx.compose.animation.animateColorAsState(if (scrolled) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surface, Springs.effects(), label = "hc")
        Column(Modifier.background(headerColor)) {
            if (Prefs.topBar.value == "search") {
                SearchBarHeader(openDrawer)
            } else {
                TopAppBar(
                    title = { ConnectionTitle(tabs.getOrNull(pager.currentPage)?.title) },
                    navigationIcon = { if (Prefs.drawer.value) MorphIconButton(Icons.Outlined.Menu, "Меню", openDrawer) },
                    actions = { MorphIconButton(Icons.Outlined.Search, "Поиск", { Nav.push(Route.Search) }) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = headerColor, scrolledContainerColor = headerColor),
                    scrollBehavior = scrollBehavior,
                )
            }
            if (tabs.size > 1) FolderBar(tabs, pager.currentPage) { i -> scope.launch { pager.animateScrollToPage(i, animationSpec = MaterialTheme_springPager()) } }
        }
        HorizontalPager(
            state = pager,
            userScrollEnabled = Prefs.folderSwipe.value && Prefs.listSwipe.value != "actions",
            modifier = Modifier.fillMaxSize(),
            key = { tabs.getOrNull(it)?.title ?: it },
            beyondViewportPageCount = 1,
        ) { page ->
            val tab = tabs.getOrNull(page) ?: return@HorizontalPager
            ChatList(tab.list, pad, showArchive = tab.list is TdApi.ChatListMain && Prefs.showArchiveRow.value, onLong = { actionChat = it })
        }
    }
    actionChat?.let { c -> ChatActionsSheet(c) { actionChat = null } }
}

private fun MaterialTheme_springPager() = androidx.compose.animation.core.spring<Float>(dampingRatio = 0.85f, stiffness = 500f)

@Composable
private fun SearchBarHeader(openDrawer: () -> Unit) {
    val me = TdStore.me
    val cs = MaterialTheme.colorScheme
    val conn = TdStore.connection
    val connecting = conn !is TdApi.ConnectionStateReady
    Box(Modifier.windowInsetsPadding(TopAppBarDefaults.windowInsets).padding(horizontal = 12.dp, vertical = 8.dp)) {
        // M3 search bar: full pill, leading menu, trailing account avatar; shows connection state in place of the hint
        MorphBox(
            onClick = { Nav.push(Route.Search) }, rest = 28.dp, pressed = 16.dp, squish = 0.985f,
            color = cs.surfaceContainerHigh, elevation = 1.dp, contentAlignment = Alignment.CenterStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                if (Prefs.drawer.value) MorphIconButton(Icons.Outlined.Menu, "Меню", openDrawer) else { Spacer(Modifier.width(12.dp)); Icon(Icons.Outlined.Search, null, tint = cs.onSurfaceVariant); Spacer(Modifier.width(12.dp)) }
                AnimatedContent(connecting, transitionSpec = { (fadeIn(Springs.effects()) + androidx.compose.animation.slideInVertically(Springs.smooth()) { it / 2 }).togetherWith(fadeOut(Springs.exit())) }, label = "sb", modifier = Modifier.weight(1f)) { c ->
                    if (c) Row(verticalAlignment = Alignment.CenterVertically) {
                        LoadingIndicator(Modifier.size(26.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (conn is TdApi.ConnectionStateWaitingForNetwork) "Ожидание сети…" else if (conn is TdApi.ConnectionStateUpdating) "Обновление…" else "Соединение…", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                    } else Text("Поиск в qwgram", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                }
                UserAvatar(me, 34.dp, Modifier.clip(CircleShape).clickable { HomeState.tab = HomeTab.Settings })
                Spacer(Modifier.width(10.dp))
            }
        }
    }
}

@Composable
fun ConnectionTitle(folderTitle: String?) {
    val conn = TdStore.connection
    val text = when (conn) {
        is TdApi.ConnectionStateWaitingForNetwork -> "Ожидание сети…"
        is TdApi.ConnectionStateConnecting, is TdApi.ConnectionStateConnectingToProxy -> "Соединение…"
        is TdApi.ConnectionStateUpdating -> "Обновление…"
        else -> null
    }
    AnimatedContent(text, transitionSpec = { fadeIn(Springs.effects()) togetherWith fadeOut(Springs.exit()) }, label = "conn") { t ->
        if (t != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LoadingIndicator(Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text(t, style = MaterialTheme.typography.titleLarge)
            }
        } else {
            val title = when (Prefs.titleText.value) {
                "name" -> TdStore.me?.firstName ?: "qwgram"
                "chats" -> "Чаты"
                "folder" -> folderTitle ?: "Чаты"
                else -> "qwgram"
            }
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Folder bar. "group" is the Material 3 Expressive connected button group: segments with small inner
 * corners and round outer ends; the selected one becomes a full pill in secondary container.
 */
@Composable
private fun FolderBar(tabs: List<FolderTab>, selected: Int, onSelect: (Int) -> Unit) {
    val counts = tabs.map { t -> TdStore.unreadIn(t.list) }
    val rowState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(selected) { rowState.animateScrollToItem((selected - 1).coerceAtLeast(0)) }
    when (Prefs.folderStyle.value) {
        "tabs" -> PrimaryScrollableTabRow(selectedTabIndex = selected, edgePadding = 8.dp, containerColor = androidx.compose.ui.graphics.Color.Transparent, divider = {}) {
            tabs.forEachIndexed { i, t ->
                Tab(selected = i == selected, onClick = { onSelect(i) }, text = { FolderLabel(t.title, counts[i], i == selected) })
            }
        }
        "chips" -> androidx.compose.foundation.lazy.LazyRow(state = rowState, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(tabs) { i, t ->
                FilterChip(selected = i == selected, onClick = { onSelect(i) }, label = { FolderLabel(t.title, counts[i], i == selected) })
            }
        }
        else -> androidx.compose.foundation.lazy.LazyRow(state = rowState, contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            itemsIndexed(tabs) { i, t ->
                val sel = i == selected
                val outer = 20.dp
                val inner by animateDpAsState(if (sel) outer else 8.dp, Springs.fastSpatial(), label = "r")
                val start = if (i == 0 || sel) outer else inner
                val end = if (i == tabs.lastIndex || sel) outer else inner
                val bg by androidx.compose.animation.animateColorAsState(if (sel) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, Springs.effects(), label = "fb")
                val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val squeeze by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.94f else 1f, if (pressed) Springs.pressIn() else Springs.fastSpatial(), label = "fs")
                Surface(
                    onClick = { onSelect(i) },
                    shape = RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end),
                    color = bg,
                    contentColor = if (sel) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    interactionSource = interaction,
                    modifier = Modifier.height(40.dp).graphicsLayer { scaleX = squeeze; scaleY = squeeze },
                ) {
                    Box(Modifier.padding(horizontal = if (sel) 20.dp else 16.dp), contentAlignment = Alignment.Center) {
                        FolderLabel(t.title, counts[i], sel)
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderLabel(title: String, count: Int, selected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        if (count > 0 && Prefs.folderCounters.value) {
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.height(20.dp).clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.surface)
            }
        }
    }
}

@Composable
fun ChatList(list: TdApi.ChatList, pad: PaddingValues, showArchive: Boolean, onLong: (TdApi.Chat) -> Unit) {
    val state = rememberLazyListState()
    val chats by remember(list) { derivedStateOf { TdStore.chatsIn(list) } }
    val archived by remember(showArchive) { derivedStateOf { if (showArchive) TdStore.chatsIn(TdApi.ChatListArchive()) else emptyList() } }
    LaunchedEffect(state, list) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (last >= state.layoutInfo.totalItemsCount - 10) TdStore.loadMore(list)
        }
    }
    if (chats.isEmpty() && TdStore.connection !is TdApi.ConnectionStateReady) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        return
    }
    LazyColumn(
        state = state,
        contentPadding = PaddingValues(bottom = pad.calculateBottomPadding() + 96.dp, top = 4.dp),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(if (Prefs.listStyle.value == "cards") 2.dp else 0.dp),
    ) {
        if (archived.isNotEmpty()) {
            item(key = "archive") { ArchiveRow(archived) }
        }
        items(chats, key = { it.id }, contentType = { 0 }) { c0 ->
            // read the live chat by key: only this row recomposes when its counters change
            val c = TdStore.chats[c0.id] ?: c0
            SwipeChatRow(c, list, enabled = Prefs.listSwipe.value == "actions", modifier = Modifier.animateItem(fadeInSpec = Springs.effects(), placementSpec = Springs.smooth(), fadeOutSpec = Springs.exit())) {
                ChatRow(c, list, onLong = { onLong(c) }) { Nav.push(Route.Chat(c.id)) }
            }
        }
        if (chats.isEmpty()) {
            item {
                EmptyState(Icons.Outlined.Forum, "Здесь пока пусто", if (list is TdApi.ChatListFolder) "В этой папке нет чатов" else "Начните переписку кнопкой «Написать»")
            }
        }
    }
}

@Composable
private fun ArchiveRow(archived: List<TdApi.Chat>) {
    val unread = archived.sumOf { it.unreadCount }
    androidx.compose.material3.ListItem(
        headlineContent = { Text("Архив", style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(archived.take(5).joinToString(", ") { it.title }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = {
            Box(
                Modifier.size(Prefs.avatarSize.value.dp).clip(avatarShape()).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Archive, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        trailingContent = { if (unread > 0) CountBadge(unread, muted = true) },
        modifier = Modifier.clickable { Nav.push(Route.Archive) },
    )
}

// ------------------------------------------------------------------ drawer

@Composable
private fun AppDrawer(close: () -> Unit) {
    val me = TdStore.me
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        androidx.compose.foundation.lazy.LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item {
                // account card
                MorphBox(
                    onClick = { close(); Nav.push(Route.Me) }, rest = 28.dp, pressed = 16.dp, squish = 0.98f,
                    color = cs.primaryContainer, contentColor = cs.onPrimaryContainer, modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            UserAvatar(me, 64.dp)
                            Spacer(Modifier.weight(1f))
                            val dark = isAppDark()
                            AnimatedContent(dark, transitionSpec = { (scaleIn(Springs.fastSpatial()) + fadeIn(Springs.effects())).togetherWith(scaleOut(Springs.exit()) + fadeOut(Springs.exit())) }, label = "theme") { d ->
                                MorphIconButton(
                                    if (d) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, "Ночной режим",
                                    { Prefs.themeMode.value = if (d) "light" else "dark" },
                                    color = cs.surface.copy(alpha = 0.35f),
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(me?.let { TdStore.fullName(it) } ?: "", style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (Prefs.hidePhone.value) "Номер скрыт" else "+" + (me?.phoneNumber ?: ""),
                            style = MaterialTheme.typography.bodyMedium, color = cs.onPrimaryContainer.copy(alpha = 0.75f),
                        )
                    }
                }
            }
            drawerGroup(
                listOf(
                    DrawerEntry("Мой профиль", Icons.Outlined.Person) { Nav.push(Route.Me) },
                    DrawerEntry("Новая группа", Icons.Outlined.GroupAdd) { Nav.push(Route.Page("compose")) },
                    DrawerEntry("Контакты", Icons.Outlined.Contacts) { HomeState.tab = HomeTab.Contacts },
                    DrawerEntry("Избранное", Icons.Outlined.Bookmark) {
                        val id = me?.id ?: return@DrawerEntry
                        scope.launch { Td.callOrNull(TdApi.CreatePrivateChat(id, false))?.let { Nav.push(Route.Chat(it.id)) } }
                    },
                    DrawerEntry("Архив", Icons.Outlined.Archive) { Nav.push(Route.Archive) },
                ), close,
            )
            drawerGroup(
                listOf(
                    DrawerEntry("Настройки", Icons.Outlined.Settings) { HomeState.tab = HomeTab.Settings },
                    DrawerEntry("Внешний вид", Icons.Outlined.Palette) { Nav.push(Route.Page("appearance")) },
                    DrawerEntry("Функции qwgram", Icons.Outlined.AutoAwesome) { Nav.push(Route.Page("features")) },
                    DrawerEntry("Папки", Icons.Outlined.FolderOpen) { Nav.push(Route.Page("folders")) },
                ), close,
            )
            drawerGroup(
                listOf(
                    DrawerEntry("Пригласить друзей", Icons.Outlined.Share) {
                        val i = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(android.content.Intent.EXTRA_TEXT, "Пиши мне в Telegram" + (me?.usernames?.activeUsernames?.firstOrNull()?.let { ": https://t.me/$it" } ?: ""))
                        ctx.startActivity(android.content.Intent.createChooser(i, null))
                    },
                ), close,
            )
        }
    }
}

private class DrawerEntry(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val action: () -> Unit)

/** segmented group like in settings: one tonal card per section, 2dp gaps, big outer corners */
private fun androidx.compose.foundation.lazy.LazyListScope.drawerGroup(items: List<DrawerEntry>, close: () -> Unit) {
    item { Spacer(Modifier.height(10.dp)) }
    items.forEachIndexed { i, e ->
        item {
            val top = if (i == 0) 20.dp else 4.dp
            val bottom = if (i == items.lastIndex) 20.dp else 4.dp
            val cs = MaterialTheme.colorScheme
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(top, top, bottom, bottom)).background(cs.surfaceContainerHigh)
                    .clickable { close(); e.action() }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(e.icon, null, tint = cs.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Text(e.title, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** state of the push drawer: 0 = closed, 1 = open; driven by fingers or springs */
class DrawerControl(val progress: androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D>, val scope: kotlinx.coroutines.CoroutineScope) {
    var widthPx = 1f
    fun open() { scope.launch { progress.animateTo(1f, Springs.smooth()) } }
    fun close() { scope.launch { progress.animateTo(0f, Springs.smooth()) } }
    fun drag(deltaPx: Float) { scope.launch { progress.snapTo((progress.value + deltaPx / widthPx).coerceIn(0f, 1f)) } }
    fun settle(velocityX: Float) { if (velocityX > 500f || (velocityX > -500f && progress.value > 0.4f)) open() else close() }
}

@Composable
fun rememberDrawerControl(): DrawerControl {
    val scope = rememberCoroutineScope()
    return remember { DrawerControl(androidx.compose.animation.core.Animatable(0f), scope) }
}

/**
 * Immersive "push" drawer: the whole app slides right, shrinks a little and rounds its corners, revealing the
 * menu underneath with a parallax. Follows the finger both ways; tap or swipe the moved app to close.
 */
@Composable
fun PushDrawer(drawer: DrawerControl, drawerContent: @Composable () -> Unit, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val open by remember { derivedStateOf { drawer.progress.value > 0.001f } }
    androidx.activity.compose.BackHandler(enabled = open) { drawer.close() }
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().background(cs.surfaceContainerLow)) {
        val w = minOf(maxWidth * 0.8f, 320.dp)
        val density = androidx.compose.ui.platform.LocalDensity.current
        drawer.widthPx = with(density) { w.toPx() }
        Box(
            Modifier.width(w).fillMaxSize().graphicsLayer {
                val p = drawer.progress.value
                translationX = -(1f - p) * drawer.widthPx * 0.35f
                alpha = 0.3f + 0.7f * p
            },
        ) { if (open) drawerContent() }
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val p = drawer.progress.value
                translationX = p * drawer.widthPx
                val sc = 1f - 0.07f * p
                scaleX = sc; scaleY = sc
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                shape = RoundedCornerShape((32f * p).dp)
                clip = p > 0.001f
                shadowElevation = 24.dp.toPx() * p
            },
        ) {
            content()
            if (open) Box(
                Modifier.matchParentSize()
                    .graphicsLayer { alpha = drawer.progress.value }
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.10f))
                    .pointerInput(Unit) {
                        val slop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            val tracker = androidx.compose.ui.input.pointer.util.VelocityTracker()
                            tracker.addPosition(down.uptimeMillis, down.position)
                            var moved = 0f
                            while (true) {
                                val ev = awaitPointerEvent()
                                val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                                if (!ch.pressed) break
                                tracker.addPosition(ch.uptimeMillis, ch.position)
                                val dx = ch.positionChange().x
                                moved += kotlin.math.abs(dx)
                                ch.consume()
                                if (moved > slop) drawer.drag(dx * (1f / (1f - 0.07f * drawer.progress.value)))
                            }
                            if (moved <= slop) drawer.close() else drawer.settle(tracker.calculateVelocity().x)
                        }
                    },
            )
        }
    }
}

/**
 * Swipe right over the chat list pulls the side menu out with the finger (Telegram behaviour): on the first folder
 * from anywhere, or only from the left edge when list rows use swipe actions. Vertical scrolls and left swipes are untouched.
 */
private fun Modifier.drawerSwipe(enabled: Boolean, edgeOnly: Boolean, drawer: DrawerControl): Modifier =
    if (!enabled) this else pointerInput(edgeOnly) {
        val slop = viewConfiguration.touchSlop
        val edge = 48.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (edgeOnly && down.position.x > edge) return@awaitEachGesture
            val tracker = androidx.compose.ui.input.pointer.util.VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            var dx = 0f
            var dy = 0f
            var dragging = false
            while (true) {
                val ev = awaitPointerEvent(PointerEventPass.Initial)
                val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                if (!ch.pressed) break
                tracker.addPosition(ch.uptimeMillis, ch.position)
                val d = ch.positionChange()
                if (!dragging) {
                    dx += d.x; dy += d.y
                    if (kotlin.math.abs(dy) > slop && kotlin.math.abs(dy) > kotlin.math.abs(dx)) return@awaitEachGesture
                    if (dx < -slop) return@awaitEachGesture
                    if (dx > slop && dx > kotlin.math.abs(dy) * 1.2f) dragging = true else continue
                }
                ch.consume()
                drawer.drag(d.x)
            }
            if (dragging) drawer.settle(tracker.calculateVelocity().x)
        }
    }
