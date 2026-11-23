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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
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
import org.drinkless.tdlib.TdApi

enum class HomeTab { Chats, Contacts, Settings }

object HomeState {
    var tab by mutableStateOf(HomeTab.Chats)
}

@Composable
fun HomeScreen() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var chromeVisible by remember { mutableStateOf(true) }
    val hideConn = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!Prefs.hideOnScroll.value) return Offset.Zero
                if (available.y < -6f) chromeVisible = false else if (available.y > 6f) chromeVisible = true
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(HomeState.tab) { chromeVisible = true }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = Prefs.drawer.value && HomeState.tab == HomeTab.Chats,
        drawerContent = { AppDrawer { scope.launch { drawerState.close() } } },
    ) {
        val navOffset by animateFloatAsState(if (chromeVisible) 0f else 1f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "chrome")
        Scaffold(
            bottomBar = {
                BottomNav(Modifier.graphicsLayer { translationY = size.height * navOffset; alpha = 1f - navOffset * 0.3f })
            },
            floatingActionButton = {
                if (HomeState.tab == HomeTab.Chats && Prefs.fabStyle.value != "none") {
                    val ctx = LocalContext.current
                    Box(Modifier.graphicsLayer { translationY = (size.height + 120.dp.toPx()) * navOffset }) {
                        if (Prefs.fabStyle.value == "extended") {
                            ExtendedFloatingActionButton(
                                onClick = { HomeState.tab = HomeTab.Contacts },
                                icon = { Icon(Icons.Filled.Edit, null) },
                                text = { Text("Написать") },
                            )
                        } else {
                            FloatingActionButton(onClick = { HomeState.tab = HomeTab.Contacts }) { Icon(Icons.Filled.Edit, "Написать") }
                        }
                    }
                }
            },
            contentWindowInsets = WindowInsets(0),
        ) { pad ->
            Box(Modifier.fillMaxSize().nestedScroll(hideConn)) {
                val motion = MaterialTheme.motionScheme
                AnimatedContent(
                    targetState = HomeState.tab,
                    transitionSpec = {
                        // M3 fade-through between top-level destinations
                        (fadeIn(motion.defaultEffectsSpec()) + androidx.compose.animation.scaleIn(motion.defaultSpatialSpec(), initialScale = 0.94f))
                            .togetherWith(fadeOut(motion.fastEffectsSpec()))
                    },
                    label = "tabs",
                ) { t ->
                    when (t) {
                        HomeTab.Chats -> ChatsTab(pad) { scope.launch { drawerState.open() } }
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
    val unread = TdStore.chatsIn(TdApi.ChatListMain()).count { (it.unreadCount > 0 || it.isMarkedAsUnread) && !TdStore.isMuted(it) }
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
                        val w by animateDpAsState(if (sel && labels) 120.dp else 56.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "w")
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
        NavigationBar(modifier) {
            tabs.forEach { (t, title, icons) ->
                val sel = HomeState.tab == t
                NavigationBarItem(
                    selected = sel,
                    onClick = { HomeState.tab = t },
                    icon = { NavIcon(t, unread, if (sel) icons.first else icons.second, title) },
                    label = if (labels) ({ Text(title) }) else null,
                    alwaysShowLabel = labels,
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
private fun ChatsTab(pad: PaddingValues, openDrawer: () -> Unit) {
    val tabs = folderTabs()
    val pager = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var actionChat by remember { mutableStateOf<TdApi.Chat?>(null) }

    LaunchedEffect(pager.currentPage, tabs.size) {
        tabs.getOrNull(pager.currentPage)?.let { Td.send(TdApi.LoadChats(it.list, 100)) }
    }

    Column(Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        val scrolled = scrollBehavior.state.overlappedFraction > 0.01f
        val headerColor = if (scrolled) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surface
        Column(Modifier.background(headerColor)) {
            if (Prefs.topBar.value == "search") {
                SearchBarHeader(openDrawer)
            } else {
                TopAppBar(
                    title = { ConnectionTitle(tabs.getOrNull(pager.currentPage)?.title) },
                    navigationIcon = { if (Prefs.drawer.value) IconButton(onClick = openDrawer) { Icon(Icons.Outlined.Menu, "Меню") } },
                    actions = { IconButton(onClick = { Nav.push(Route.Search) }) { Icon(Icons.Outlined.Search, "Поиск") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = headerColor, scrolledContainerColor = headerColor),
                    scrollBehavior = scrollBehavior,
                )
            }
            if (tabs.size > 1) FolderBar(tabs, pager.currentPage) { i -> scope.launch { pager.animateScrollToPage(i, animationSpec = MaterialTheme_springPager()) } }
        }
        HorizontalPager(
            state = pager,
            userScrollEnabled = Prefs.folderSwipe.value,
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
    Box(Modifier.windowInsetsPadding(TopAppBarDefaults.windowInsets).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Surface(
            onClick = { Nav.push(Route.Search) },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                if (Prefs.drawer.value) IconButton(onClick = openDrawer) { Icon(Icons.Outlined.Menu, "Меню") } else Spacer(Modifier.width(16.dp))
                Text("Поиск в qwgram", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                UserAvatar(me, 32.dp, Modifier.clickable { HomeState.tab = HomeTab.Settings })
                Spacer(Modifier.width(12.dp))
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
    val motion = MaterialTheme.motionScheme
    AnimatedContent(text, transitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()) }, label = "conn") { t ->
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
    val counts = tabs.map { t -> TdStore.chatsIn(t.list).count { (it.unreadCount > 0 || it.isMarkedAsUnread) && !TdStore.isMuted(it) } }
    when (Prefs.folderStyle.value) {
        "tabs" -> PrimaryScrollableTabRow(selectedTabIndex = selected, edgePadding = 8.dp, containerColor = androidx.compose.ui.graphics.Color.Transparent, divider = {}) {
            tabs.forEachIndexed { i, t ->
                Tab(selected = i == selected, onClick = { onSelect(i) }, text = { FolderLabel(t.title, counts[i], i == selected) })
            }
        }
        "chips" -> Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tabs.forEachIndexed { i, t ->
                FilterChip(selected = i == selected, onClick = { onSelect(i) }, label = { FolderLabel(t.title, counts[i], i == selected) })
            }
        }
        else -> Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            tabs.forEachIndexed { i, t ->
                val sel = i == selected
                val outer = 20.dp
                val inner by animateDpAsState(if (sel) outer else 8.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "r")
                val start = if (i == 0 || sel) outer else inner
                val end = if (i == tabs.lastIndex || sel) outer else inner
                Surface(
                    onClick = { onSelect(i) },
                    shape = RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end),
                    color = if (sel) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (sel) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.height(40.dp),
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
    val chats = TdStore.chatsIn(list)
    val archived = if (showArchive) TdStore.chatsIn(TdApi.ChatListArchive()) else emptyList()
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
        items(chats, key = { it.id }) { c ->
            ChatRow(c, list, Modifier.animateItem(), onLong = { onLong(c) }) { Nav.push(Route.Chat(c.id)) }
        }
        if (chats.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(96.dp).clip(androidx.compose.material3.MaterialShapes.Cookie9Sided.toShape()).background(MaterialTheme.colorScheme.secondaryContainer))
                    Spacer(Modifier.height(16.dp))
                    Text("Здесь пока пусто", style = MaterialTheme.typography.titleMedium)
                }
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
    ModalDrawerSheet {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                UserAvatar(me, 64.dp)
                Spacer(Modifier.weight(1f))
                val dark = isAppDark()
                IconButton(onClick = { Prefs.themeMode.value = if (dark) "light" else "dark" }) {
                    Icon(if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, "Ночной режим")
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(me?.let { TdStore.fullName(it) } ?: "", style = MaterialTheme.typography.titleMedium)
            Text(if (Prefs.hidePhone.value) "Номер скрыт" else "+" + (me?.phoneNumber ?: ""), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 4.dp))
        val item: @Composable (String, androidx.compose.ui.graphics.vector.ImageVector, () -> Unit) -> Unit = { label, icon, action ->
            NavigationDrawerItem(label = { Text(label) }, icon = { Icon(icon, null) }, selected = false, onClick = { close(); action() }, modifier = Modifier.padding(horizontal = 12.dp))
        }
        item("Мой профиль", Icons.Outlined.Person) { Nav.push(Route.Me) }
        item("Контакты", Icons.Outlined.Person) { HomeState.tab = HomeTab.Contacts }
        item("Избранное", Icons.Outlined.Bookmark) {
            val id = me?.id ?: return@item
            scope.launch { Td.callOrNull(TdApi.CreatePrivateChat(id, false))?.let { Nav.push(Route.Chat(it.id)) } }
        }
        item("Архив", Icons.Outlined.Archive) { Nav.push(Route.Archive) }
        item("Настройки", Icons.Outlined.Settings) { HomeState.tab = HomeTab.Settings }
        HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 4.dp))
        item("Внешний вид", Icons.Outlined.Palette) { Nav.push(Route.Page("appearance")) }
        item("Функции qwgram", Icons.Outlined.AutoAwesome) { Nav.push(Route.Page("features")) }
        item("Пригласить друзей", Icons.Outlined.Person) {
            Toast.makeText(ctx, "Ссылка t.me скопирована", Toast.LENGTH_SHORT).show()
        }
    }
}
