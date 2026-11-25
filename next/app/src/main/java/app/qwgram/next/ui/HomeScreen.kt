package app.qwgram.next.ui

import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.SideEffect
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.runtime.DisposableEffect
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
        var fabOpen by remember { mutableStateOf(false) }
        var addContact by remember { mutableStateOf(false) }
        val floatingNav = Prefs.navStyle.value == "floating"
        val showFab = Prefs.fabStyle.value != "none" && HomeState.tab != HomeTab.Settings
        val fabActions = listOf(
            FabAction("Новое сообщение", Icons.Outlined.Edit) { Nav.push(Route.Page("compose")) },
            FabAction("Новая группа", Icons.Outlined.GroupAdd) { Nav.push(Route.Page("compose:group")) },
            FabAction("Новый канал", Icons.Outlined.Campaign) { Nav.push(Route.Page("compose:channel")) },
            FabAction("Новый контакт", Icons.Outlined.PersonAdd) { addContact = true },
        )
        val contacts = HomeState.tab == HomeTab.Contacts
        // one FAB for every tab (same size, only the icon changes), so nothing jumps when switching
        val searchFab = Prefs.fabAction.value == "search" && !contacts
        var fabRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
        val fabButton: @Composable () -> Unit = {
            Box(Modifier.onGloballyPositioned { fabRect = it.boundsInRoot() }) {
                FabMenu(
                    if (contacts || searchFab) false else fabOpen,
                    { when { contacts -> addContact = true; searchFab -> SearchMorph.open(fabRect); else -> fabOpen = it } },
                    when { contacts -> Icons.Outlined.PersonAdd; searchFab -> Icons.Outlined.Search; else -> Icons.Outlined.Edit },
                    emptyList(),
                )
            }
        }
        val fab: @Composable () -> Unit = {
            androidx.compose.animation.AnimatedVisibility(
                showFab,
                enter = androidx.compose.animation.scaleIn(Springs.spatial()) + androidx.compose.animation.fadeIn(Springs.effects()),
                exit = androidx.compose.animation.scaleOut(Springs.exit()) + androidx.compose.animation.fadeOut(Springs.exit()),
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    FabMenu(fabOpen && !contacts, { fabOpen = it }, Icons.Outlined.Edit, fabActions, showButton = false)
                    fabButton()
                }
            }
        }
        if (addContact) AddContactDialog { addContact = false }
        androidx.activity.compose.BackHandler(enabled = fabOpen) { fabOpen = false }
        LaunchedEffect(HomeState.tab) { fabOpen = false }
        Scaffold(
            bottomBar = { if (floatingNav) Spacer(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).height(84.dp)) else BottomNav(Modifier) },
            floatingActionButton = { if (!floatingNav) fab() },
            contentWindowInsets = WindowInsets(0),
            containerColor = if (appWallpaperOn()) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.surface,
        ) { pad ->
            Box(Modifier.fillMaxSize().nestedScroll(fabConn)) {
                // the tabs stay alive once opened (no rebuild on every switch: no stall, scroll kept); switching is a
                // fade-through on the shared X axis — the old tab dissolves fast, the new one drifts in from 97%
                val visited = remember { androidx.compose.runtime.mutableStateListOf(HomeState.tab) }
                if (HomeState.tab !in visited) visited.add(HomeState.tab)
                var lastTab by remember { mutableStateOf(HomeState.tab) }
                val dir = if (HomeState.tab.ordinal >= lastTab.ordinal) 1 else -1
                SideEffect { lastTab = HomeState.tab }
                HomeTab.entries.forEach { t ->
                    if (t !in visited) return@forEach
                    androidx.compose.runtime.key(t) {
                        val sel = HomeState.tab == t
                        val k = Prefs.motionSpeed.value
                        val off = Prefs.motion.value == "off"
                        val a by androidx.compose.animation.core.animateFloatAsState(
                            if (sel) 1f else 0f,
                            if (off) androidx.compose.animation.core.snap()
                            else if (sel) androidx.compose.animation.core.tween((300 / k).toInt(), delayMillis = (60 / k).toInt(), easing = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f))
                            else androidx.compose.animation.core.tween((130 / k).toInt(), easing = androidx.compose.animation.core.LinearEasing),
                            label = "tab",
                        )
                        val enterDir = remember { mutableStateOf(1) }
                        if (sel) SideEffect { enterDir.value = dir }
                        Box(
                            Modifier.fillMaxSize().zIndex(if (sel) 1f else 0f)
                                // a hidden tab is kept laid out but placed off screen: it gets no touches
                                .layout { m, c ->
                                    val pl = m.measure(c)
                                    layout(pl.width, pl.height) { if (!sel && a < 0.01f) pl.place(-pl.width * 4, 0) else pl.place(0, 0) }
                                }
                                .graphicsLayer {
                                    alpha = a
                                    if (sel && a < 1f) {
                                        val sc = 0.97f + 0.03f * a
                                        scaleX = sc; scaleY = sc
                                        translationX = (1f - a) * enterDir.value * 28.dp.toPx()
                                    }
                                },
                        ) {
                            when (t) {
                                HomeTab.Chats -> ChatsTab(pad, drawer)
                                HomeTab.Contacts -> ContactsTab(pad)
                                HomeTab.Settings -> SettingsHome(pad)
                            }
                        }
                    }
                }
                // scrim behind the open FAB menu
                val scrimA by androidx.compose.animation.core.animateFloatAsState(if (fabOpen) 1f else 0f, Springs.effects(), label = "fs")
                if (scrimA > 0.01f) Box(
                    Modifier.fillMaxSize().graphicsLayer { alpha = scrimA }
                        .glassScrim(MaterialTheme.colorScheme.surface)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = if (blurOn(Prefs.blurMenus)) 0.35f else 0.75f))
                        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { fabOpen = false },
                )
                if (floatingNav) {
                    // the action list floats above the bar on its own, so it never pushes the pill around
                    FabMenu(
                        fabOpen && !contacts, { fabOpen = it }, Icons.Outlined.Edit, fabActions, showButton = false,
                        modifier = Modifier.align(Alignment.BottomEnd).windowInsetsPadding(WindowInsets.navigationBars).padding(end = 22.dp, bottom = 104.dp),
                    )
                    FloatingNavRow(Modifier.align(Alignment.BottomCenter), showFab, fabButton)
                }
            }
        }
    }
}

/**
 * Floating navigation: a pill with the destinations and the FAB as a separate expressive button beside it,
 * over a fade so the list dissolves under them.
 */
@Composable
private fun FloatingNavRow(modifier: Modifier, showFab: Boolean, fab: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    // 0..1: how much room the FAB takes. The pill re-centres smoothly as the FAB slot grows / shrinks.
    val p by androidx.compose.animation.core.animateFloatAsState(
        if (showFab) 1f else 0f,
        if (Prefs.motion.value == "off") androidx.compose.animation.core.snap() else androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 320f * Prefs.motionSpeed.value * Prefs.motionSpeed.value),
        label = "fabslot",
    )
    Box(
        modifier.fillMaxWidth()
            .then(
                if (fadeGlassOn(Prefs.blurBars)) Modifier
                else Modifier.background(androidx.compose.ui.graphics.Brush.verticalGradient(0f to cs.surface.copy(alpha = 0f), 0.45f to cs.surface.copy(alpha = 0.85f), 1f to cs.surface)),
            ),
    ) {
    // glass: progressive blur rising from the bottom edge (a background layer: the pill and FAB stay crisp above)
    GlassFadeLayer(FadeEdge.Top, cs.surface, Prefs.blurBars)
    Box(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(top = 28.dp, bottom = 12.dp, start = 10.dp, end = 10.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            // the pill gives way if the screen is narrow; the FAB keeps its full size
            Box(Modifier.weight(1f, fill = false)) { FloatingNavPill() }
            androidx.compose.ui.layout.Layout(
                content = {
                    if (p > 0.002f || showFab) Box(Modifier.graphicsLayer {
                        scaleX = 0.4f + 0.6f * p; scaleY = scaleX; alpha = p.coerceIn(0f, 1f)
                    }) { fab() }
                },
            ) { ms, c ->
                val gap = 4.dp.roundToPx()
                val pl = ms.firstOrNull()?.measure(c.copy(minWidth = 0, minHeight = 0))
                val w = (((pl?.width ?: 0) + gap) * p.coerceIn(0f, 1.05f)).toInt().coerceAtLeast(0)
                val h = pl?.height ?: 0
                layout(w, h) { pl?.place(w - pl.width, 0) }
            }
        }
    }
    }
}

@Composable
private fun FloatingNavPill() {
    val unread = TdStore.unreadChats
    val tabs = buildList {
        add(Triple(HomeTab.Chats, "Чаты", Icons.Filled.Chat to Icons.AutoMirrored.Outlined.Chat))
        if (Prefs.showContactsTab.value) add(Triple(HomeTab.Contacts, "Контакты", Icons.Filled.Person to Icons.Outlined.Person))
        add(Triple(HomeTab.Settings, "Настройки", Icons.Filled.Settings to Icons.Outlined.Settings))
    }
    val cs = MaterialTheme.colorScheme
    // the floating pill: the chosen tab widens into a pill with its label (springy), the others are icons.
    // The label's room is measured first, so the pill never grows past the space beside the FAB
    androidx.compose.foundation.layout.BoxWithConstraints {
        // room left for the label beside the icons (never below a short word's width)
        val labelMax = (maxWidth - 12.dp - 56.dp * tabs.size - 2.dp * (tabs.size - 1) - 24.dp).coerceAtLeast(72.dp)
        val labels = Prefs.navLabels.value
        val glassy = blurOn(Prefs.blurBars) && LocalHaze.current != null
        Surface(
            shape = RoundedCornerShape(32.dp.rs()), color = if (glassy) Color.Transparent else cs.surfaceContainerHigh, shadowElevation = if (glassy) 0.dp else 4.dp,
            modifier = Modifier.glass(RoundedCornerShape(32.dp.rs()), cs.surfaceContainerHigh),
        ) {
            Row(Modifier.padding(6.dp).height(52.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                tabs.forEach { (t, title, icons) ->
                    val sel = HomeState.tab == t
                    val haptic = rememberHaptic()
                    val bg by androidx.compose.animation.animateColorAsState(if (sel) cs.secondaryContainer else cs.surfaceContainerHigh.copy(alpha = 0f), Springs.effects(), label = "nb")
                    MorphBox(
                        onClick = { if (!sel) haptic(androidx.compose.ui.hapticfeedback.HapticFeedbackType.SegmentTick); HomeState.tab = t }, rest = 26.dp, pressed = 14.dp, color = bg,
                        contentColor = if (sel) cs.onSecondaryContainer else cs.onSurfaceVariant,
                        modifier = Modifier.fillMaxHeight().widthIn(min = 56.dp),
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            NavIcon(t, unread, if (sel) icons.first else icons.second, title)
                            // the label is measured at its full width and revealed by the expanding clip
                            androidx.compose.animation.AnimatedVisibility(
                                sel && labels,
                                enter = androidx.compose.animation.expandHorizontally(androidx.compose.animation.core.spring(0.82f, 420f), expandFrom = Alignment.Start, clip = true) + fadeIn(androidx.compose.animation.core.tween(180, 60)),
                                exit = androidx.compose.animation.shrinkHorizontally(androidx.compose.animation.core.spring(1f, 520f), shrinkTowards = Alignment.Start, clip = true) + fadeOut(androidx.compose.animation.core.tween(90)),
                            ) {
                                Row {
                                    Spacer(Modifier.width(8.dp))
                                    Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = labelMax))
                                }
                            }
                        }
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
            val g = blurOn(Prefs.blurBars) && LocalHaze.current != null
            Surface(shape = CircleShape, color = if (g) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = if (g) 0.dp else 3.dp, shadowElevation = if (g) 0.dp else 3.dp,
                modifier = Modifier.glass(CircleShape, MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tabs.forEach { (t, title, icons) ->
                        val sel = HomeState.tab == t
                        val w by animateDpAsState(if (sel && labels) 120.dp else 56.dp, Springs.fastSpatial(), label = "w")
                        Surface(
                            onClick = { HomeState.tab = t },
                            shape = CircleShape,
                            color = if (sel) MaterialTheme.colorScheme.secondaryContainer else if (g) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
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
        // square bar: glass across the whole bottom when the blur is on
        val g = blurOn(Prefs.blurBars) && LocalHaze.current != null
        ShortNavigationBar(
            modifier.then(if (g) Modifier.glass(androidx.compose.ui.graphics.RectangleShape, MaterialTheme.colorScheme.surfaceContainer, edge = false) else Modifier),
            containerColor = if (g) Color.Transparent else androidx.compose.material3.ShortNavigationBarDefaults.containerColor,
        ) {
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

    var cascadeGen by remember { mutableIntStateOf(0) }
    var cascadePage by remember { mutableIntStateOf(-1) }
    val canOpenDrawer = Prefs.drawer.value && (tabs.size <= 1 || pager.currentPage == 0)
    SideEffect { drawer.canOpen = canOpenDrawer }
    DisposableEffect(Unit) { onDispose { drawer.canOpen = false } }
    val exDensity = androidx.compose.ui.platform.LocalDensity.current
    val exW = with(exDensity) { 40.dp.toPx() }
    val exH = with(exDensity) { 200.dp.toPx() }
    val floating = Prefs.floatingBars.value
    // square header colour follows the list: page colour at the top, elevated container once scrolled
    val homeLift = androidx.compose.animation.core.animateFloatAsState(if (scrollBehavior.state.overlappedFraction > 0.01f || scrollBehavior.state.contentOffset < -4f) 1f else 0f, androidx.compose.animation.core.tween(220), label = "hl")
    val hd = androidx.compose.ui.platform.LocalDensity.current
    var headerH by remember { mutableStateOf(120.dp) }
    Box(
        Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)
            .then(if (canOpenDrawer) Modifier.systemGestureExclusion { c -> androidx.compose.ui.geometry.Rect(0f, c.size.height * 0.42f, exW, c.size.height * 0.42f + exH) } else Modifier),
    ) {
        // the list scrolls under the header (floating: over a fade; classic: under a solid bar)
        HorizontalPager(
            state = pager,
            userScrollEnabled = Prefs.folderSwipe.value && Prefs.listSwipe.value != "actions",
            modifier = Modifier.fillMaxSize().blurSource(),
            key = { tabs.getOrNull(it)?.title ?: it },
            beyondViewportPageCount = 1,
            // the page lands on a soft spring with a hint of overshoot: the trailing rows close up with a little bounce
            flingBehavior = androidx.compose.foundation.pager.PagerDefaults.flingBehavior(
                state = pager,
                snapAnimationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.78f, stiffness = 320f * Prefs.motionSpeed.value * Prefs.motionSpeed.value),
            ),
        ) { page ->
            val tab = tabs.getOrNull(page) ?: return@HorizontalPager
            Box(Modifier.fillMaxSize().clipToBounds()) {
            ChatList(tab.list, PaddingValues(top = headerH - headerOverlap(), bottom = pad.calculateBottomPadding()), showArchive = tab.list is TdApi.ChatListMain && Prefs.showArchiveRow.value, pageOffset = { page - (pager.currentPage + pager.currentPageOffsetFraction) },
                // content moves left when we go to a later page (position grows past the settled page), right otherwise
                dir = { val pos = pager.currentPage + pager.currentPageOffsetFraction; -kotlin.math.sign(pos - pager.settledPage) },
                onLong = { actionChat = it })
            }
        }
        val cs = MaterialTheme.colorScheme
        Box(
            Modifier.fillMaxWidth().onSizeChanged { headerH = with(hd) { it.height.toDp() } }
                .then(
                    if (fadeGlassOn(Prefs.blurHeaders)) Modifier
                    else if (floating) Modifier.topFade(cs.surface, shown = { fadeShown(homeLift.value) })
                    else Modifier.drawBehind { drawRect(androidx.compose.ui.graphics.lerp(cs.surface, cs.surfaceContainer, homeLift.value)) },
                ),
        ) {
        // glass: the chats blur under the header, fading out downwards (a background layer: buttons stay crisp)
        GlassFadeLayer(FadeEdge.Bottom, if (floating) cs.surface else androidx.compose.ui.graphics.lerp(cs.surface, cs.surfaceContainer, homeLift.value), Prefs.blurHeaders, fadeOut = floating, shown = { if (floating) fadeShown(homeLift.value) else 1f })
        Column(Modifier.fillMaxWidth()) {
            if (Prefs.topBar.value == "search") {
                SearchBarHeader(openDrawer, tabs.getOrNull(pager.currentPage)?.title)
            } else {
                Row(Modifier.statusBarsPadding().fillMaxWidth().height(64.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (Prefs.drawer.value) HeaderIcon(Icons.Outlined.Menu, "Меню", openDrawer) else Spacer(Modifier.width(48.dp))
                    Box(Modifier.weight(1f).padding(horizontal = 10.dp), contentAlignment = if (Prefs.centerTitle.value) Alignment.Center else Alignment.CenterStart) {
                        ConnectionTitle(tabs.getOrNull(pager.currentPage)?.title)
                    }
                    SearchButton()
                }
            }
            if (tabs.size > 1) FolderBar(tabs, pager.currentPage) { i ->
                // tap on a folder: the list switches in place and its rows cascade in on a spring (M3 expressive)
                scope.launch {
                    // the pages slide over (far folders: the pager jumps close first), rows trailing in the same wave
                    if (Prefs.motion.value == "off") pager.scrollToPage(i)
                    else pager.animateScrollToPage(i, animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 260f * Prefs.motionSpeed.value * Prefs.motionSpeed.value))
                }
            }
            if (floating) Spacer(Modifier.height(14.dp))
        }
        }
    }
    actionChat?.let { c -> ChatActionsSheet(c) { actionChat = null } }
}

private fun MaterialTheme_springPager() = androidx.compose.animation.core.spring<Float>(dampingRatio = 0.85f, stiffness = 500f)

@Composable
private fun SearchBarHeader(openDrawer: () -> Unit, folderTitle: String?) {
    // M3 Expressive top bar: menu, the big "qwgram" title (or the connection state), search that grows into the
    // search bar, and your avatar; on the fade in floating mode, on a solid bar in classic mode
    val me = TdStore.me
    val cs = MaterialTheme.colorScheme
    val conn = TdStore.connection
    val connecting = conn !is TdApi.ConnectionStateReady
    Row(
        Modifier.windowInsetsPadding(TopAppBarDefaults.windowInsets).fillMaxWidth().height(68.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (Prefs.drawer.value) HeaderIcon(Icons.Outlined.Menu, "Меню", openDrawer) else Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).padding(horizontal = 10.dp), contentAlignment = if (Prefs.centerTitle.value) Alignment.Center else Alignment.CenterStart) {
            ConnectionTitle(folderTitle)
        }
        SearchButton()
        Spacer(Modifier.width(4.dp))
        UserAvatar(me, 40.dp, Modifier.clip(CircleShape).clickable { HomeState.tab = HomeTab.Settings })
        Spacer(Modifier.width(6.dp))
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
            // the title text rolls to the new one (folder switch, setting change) instead of jumping
            AnimatedContent(title, transitionSpec = {
                (fadeIn(androidx.compose.animation.core.tween(220, 60)) + androidx.compose.animation.slideInVertically(androidx.compose.animation.core.spring(0.8f, 380f)) { it / 2 })
                    .togetherWith(fadeOut(androidx.compose.animation.core.tween(120)) + androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.spring(1f, 500f)) { -it / 2 })
            }, label = "title") { t ->
                Text(
                    t, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold),
                    color = if (t == "qwgram") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
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
        else -> androidx.compose.foundation.lazy.LazyRow(state = rowState, contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(if (Prefs.floatingBars.value) 4.dp else 2.dp)) {
            itemsIndexed(tabs) { i, t ->
                val sel = i == selected
                val cs = MaterialTheme.colorScheme
                val floating = Prefs.floatingBars.value
                // springy morph: corners swell into a pill with a small bounce, padding grows, a light pop on select
                val bouncy = androidx.compose.animation.core.spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.5f, stiffness = 420f * Prefs.motionSpeed.value * Prefs.motionSpeed.value)
                val outer = 20.dp
                val inner by animateDpAsState(if (sel) outer else 8.dp, if (Prefs.motion.value == "off") androidx.compose.animation.core.snap() else bouncy, label = "r")
                val padH by animateDpAsState(if (sel) 22.dp else 16.dp, if (Prefs.motion.value == "off") androidx.compose.animation.core.snap() else bouncy, label = "p")
                val pop = remember { androidx.compose.animation.core.Animatable(1f) }
                var first by remember { mutableStateOf(true) }
                LaunchedEffect(sel) {
                    if (!first && sel) { pop.snapTo(0.9f); pop.animateTo(1f, androidx.compose.animation.core.spring(0.42f, 500f)) }
                    first = false
                }
                val start = (if (i == 0 || sel) outer else inner).coerceAtLeast(0.dp)
                val end = (if (i == tabs.lastIndex || sel) outer else inner).coerceAtLeast(0.dp)
                val bg by androidx.compose.animation.animateColorAsState(if (sel) cs.secondaryContainer else cs.surfaceContainerHigh, Springs.effects(), label = "fb")
                val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val squeeze by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.94f else 1f, if (pressed) Springs.pressIn() else Springs.morphRelease(), label = "fs")
                Box(
                    Modifier.height(40.dp)
                        .graphicsLayer {
                            val sc = squeeze * pop.value
                            scaleX = sc; scaleY = sc
                            shape = RoundedCornerShape(topStart = start.rs(), bottomStart = start.rs(), topEnd = end.rs(), bottomEnd = end.rs())
                            clip = true
                            shadowElevation = if (floating && !(blurOn(Prefs.blurChipsFolders))) 2.dp.toPx() else 0f
                        }
                        // glass chips over the chat list (Appearance → Blur → folders)
                        .then(
                            if (blurOn(Prefs.blurChipsFolders) && LocalHaze.current != null)
                                Modifier.glass(RoundedCornerShape(topStart = start.rs(), bottomStart = start.rs(), topEnd = end.rs(), bottomEnd = end.rs()), bg, Prefs.blurChipsFolders, strength = if (sel) 1.5f else 1f)
                            else Modifier.background(bg),
                        )
                        .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple()) { onSelect(i) }
                        .padding(horizontal = padH),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides if (sel) cs.onSecondaryContainer else cs.onSurfaceVariant) {
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
fun ChatList(list: TdApi.ChatList, pad: PaddingValues, showArchive: Boolean, cascade: Int = 0, pageOffset: () -> Float = { 0f }, dir: () -> Float = { 0f }, onLong: (TdApi.Chat) -> Unit) {
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
    // hidden archive: pull the list down at the very top to reveal it (Telegram)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val hidden = Prefs.archiveHidden.value
    var revealed by remember { mutableStateOf(false) }
    var pull by remember { mutableStateOf(0f) }
    val pullMax = with(density) { 96.dp.toPx() }
    val ctx = LocalContext.current
    val pullConn = remember(hidden) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!hidden || revealed || source != NestedScrollSource.UserInput) return Offset.Zero
                if (available.y > 0f) { pull = (pull + available.y * 0.6f).coerceAtMost(pullMax * 1.2f); if (pull >= pullMax) { revealed = true; pull = 0f } }
                else if (consumed.y < 0f) pull = 0f
                return Offset.Zero
            }
            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity { pull = 0f; return androidx.compose.ui.unit.Velocity.Zero }
        }
    }
    val scrolledAway by remember { derivedStateOf { state.firstVisibleItemIndex > 3 } }
    LaunchedEffect(scrolledAway) { if (scrolledAway) revealed = false }
    LazyColumn(
        state = state,
        contentPadding = PaddingValues(bottom = pad.calculateBottomPadding() + 96.dp, top = pad.calculateTopPadding() + 4.dp),
        modifier = Modifier.fillMaxSize().nestedScroll(pullConn),
        verticalArrangement = Arrangement.spacedBy(if (Prefs.listStyle.value == "cards") 2.dp else 0.dp),
    ) {
        if (archived.isNotEmpty()) {
            if (hidden && !revealed) item(key = "archive-pull") {
                // grows with the pull: archive icon fills up, then the row springs out
                val p = (pull / pullMax).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().height((p * 56f).dp), contentAlignment = Alignment.Center) {
                    if (p > 0.05f) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.graphicsLayer { alpha = p; scaleX = 0.8f + 0.2f * p; scaleY = 0.8f + 0.2f * p }) {
                        androidx.compose.material3.CircularWavyProgressIndicator(progress = { p }, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(if (p < 1f) "Потяните, чтобы открыть архив" else "Отпустите", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item(key = "archive") {
                androidx.compose.animation.AnimatedVisibility(
                    !hidden || revealed,
                    enter = androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(520, easing = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f))) +
                        fadeIn(androidx.compose.animation.core.tween(420, 80)) + androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(520), initialScale = 0.92f),
                    exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(380, easing = androidx.compose.animation.core.CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f))) + fadeOut(androidx.compose.animation.core.tween(240)),
                ) {
                    ArchiveRow(archived, hidden, onToggleHidden = {
                        val nowHidden = !Prefs.archiveHidden.value
                        Prefs.archiveHidden.value = nowHidden
                        revealed = false
                        if (nowHidden) android.widget.Toast.makeText(ctx, "Архив скрыт. Потяните список вниз, чтобы увидеть его", android.widget.Toast.LENGTH_LONG).show()
                    })
                }
            }
        }
        itemsIndexed(chats, key = { _, it -> it.id }, contentType = { _, _ -> 0 }) { idx, c0 ->
            // read the live chat by key: only this row recomposes when its counters change
            val c = TdStore.chats[c0.id] ?: c0
            SwipeChatRow(c, list, enabled = Prefs.listSwipe.value == "actions", modifier = Modifier.followFinger(idx, state, pageOffset, dir).animateItem(fadeInSpec = Springs.effects(), placementSpec = Springs.smooth(), fadeOutSpec = Springs.exit())) {
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
private fun ArchiveRow(archived: List<TdApi.Chat>, hidden: Boolean, onToggleHidden: () -> Unit) {
    val unread = archived.sumOf { it.unreadCount }
    val cs = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth()
                .combinedClickable(onClick = { Nav.push(Route.Archive) }, onLongClick = { menu = true })
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(Prefs.avatarSize.value.dp).clip(androidx.compose.material3.MaterialShapes.Cookie9Sided.asShape()).background(cs.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Archive, null, tint = cs.onTertiaryContainer) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Архив", style = MaterialTheme.typography.titleMedium)
                Text(archived.take(5).joinToString(", ") { it.title }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
            if (unread > 0) CountBadge(unread, muted = true)
        }
        QMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(if (hidden) "Закрепить в списке" else "Скрыть архив") },
                leadingIcon = { Icon(if (hidden) Icons.Outlined.PushPin else Icons.Outlined.VisibilityOff, null) },
                onClick = { menu = false; onToggleHidden() },
            )
        }
    }
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
                ), close, "Чаты",
            )
            drawerGroup(
                listOf(
                    DrawerEntry("Настройки", Icons.Outlined.Settings) { HomeState.tab = HomeTab.Settings },
                    DrawerEntry("Внешний вид", Icons.Outlined.Palette) { Nav.push(Route.Page("appearance")) },
                    DrawerEntry("Функции qwgram", Icons.Outlined.AutoAwesome) { Nav.push(Route.Page("features")) },
                    DrawerEntry("Папки", Icons.Outlined.FolderOpen) { Nav.push(Route.Page("folders")) },
                ), close, "Приложение",
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

/** segmented group like in settings: caption, one tonal card per section, 2dp gaps, big outer corners */
private fun androidx.compose.foundation.lazy.LazyListScope.drawerGroup(items: List<DrawerEntry>, close: () -> Unit, title: String? = null) {
    item {
        if (title != null) Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 8.dp))
        else Spacer(Modifier.height(10.dp))
    }
    items.forEachIndexed { i, e ->
        item {
            val top = if (i == 0) 20.dp else 4.dp
            val bottom = if (i == items.lastIndex) 20.dp else 4.dp
            val cs = MaterialTheme.colorScheme
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(top, top, bottom, bottom)).background(cs.surfaceContainerHigh)
                    .clickable { close(); e.action() }.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(cs.secondaryContainer), contentAlignment = Alignment.Center) {
                    Icon(e.icon, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text(e.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** state of the push drawer: 0 = closed, 1 = open; driven by fingers or springs */
class DrawerControl(val progress: androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D>, val scope: kotlinx.coroutines.CoroutineScope) {
    var widthPx = 1f
    /** set by the chat list: the menu may be pulled out right now */
    var canOpen = false
    /** finger-driven value (-1 = not dragging): written synchronously, so the drawer sticks to the finger */
    var dragV by androidx.compose.runtime.mutableFloatStateOf(-1f)
    val value: Float get() = if (dragV >= 0f) dragV else progress.value
    private var lastDir = 0f
    private fun go(target: Float) {
        scope.launch {
            if (dragV >= 0f) { progress.snapTo(dragV); dragV = -1f }
            progress.animateTo(target, Springs.smoothSlow())
        }
    }
    fun open() = go(1f)
    fun close() = go(0f)
    fun drag(deltaPx: Float) {
        if (dragV < 0f) dragV = progress.value
        dragV = (dragV + deltaPx / widthPx).coerceIn(0f, 1f)
        if (deltaPx != 0f) lastDir = deltaPx
    }
    /** the direction of the last movement wins after a short pull; a flick always wins */
    fun settle(velocityX: Float) {
        val v = value
        val openIt = when {
            velocityX > 120f -> true
            velocityX < -120f -> false
            lastDir > 0f -> v > 0.25f
            lastDir < 0f -> v > 0.75f
            else -> v > 0.5f
        }
        if (openIt) open() else close()
    }
}

@Composable
fun rememberDrawerControl(): DrawerControl {
    val scope = rememberCoroutineScope()
    return remember { DrawerControl(androidx.compose.animation.core.Animatable(0f, visibilityThreshold = 0.0002f), scope) }
}

/**
 * Immersive "push" drawer: the whole app slides right, shrinks a little and rounds its corners, revealing the
 * menu underneath with a parallax. Follows the finger both ways; tap or swipe the moved app to close.
 */
@Composable
fun PushDrawer(drawer: DrawerControl, drawerContent: @Composable () -> Unit, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val open by remember { derivedStateOf { drawer.value > 0.001f } }
    androidx.activity.compose.BackHandler(enabled = open) { drawer.close() }
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().background(cs.surfaceContainerLow).drawerGestures(drawer)) {
        val w = minOf(maxWidth * 0.8f, 320.dp)
        val corner = screenCornerRadius()
        val density = androidx.compose.ui.platform.LocalDensity.current
        drawer.widthPx = with(density) { w.toPx() }
        Box(
            Modifier.width(w).fillMaxSize().graphicsLayer {
                val p = drawer.value
                translationX = -(1f - p) * drawer.widthPx * 0.35f
                alpha = 0.3f + 0.7f * p
            }
        ) { if (open) androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface) { drawerContent() } }
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val p = drawer.value
                translationX = p * drawer.widthPx
                val sc = 1f - 0.07f * p
                scaleX = sc; scaleY = sc
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                shape = RoundedCornerShape(corner)
                clip = p > 0.001f
                shadowElevation = 24.dp.toPx() * p
            },
        ) {
            if (appWallpaperOn()) AppWallpaper()
            content()
            if (open) Box(
                Modifier.matchParentSize()
                    .graphicsLayer { alpha = drawer.value }
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.10f))
                    .pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume(); do { val e = awaitPointerEvent(); e.changes.forEach { it.consume() } } while (e.changes.any { it.pressed }) } },
            )
        }
    }
}

/**
 * Folder swipe cascade. Pure geometry, no springs of its own (so it can't stutter, overlap or leave holes):
 * while the pages move, each row trails behind its page a little more than the row above it — the top row keeps
 * up with the finger, the lower ones follow like a soft wave — and when the page lands they all close up at the
 * same moment the page does (the pager's own slightly springy settle gives the bounce). At rest nothing moves.
 * [dir]: the direction the content travels (−1 left, +1 right, 0 still).
 */
@Composable
private fun Modifier.followFinger(index: Int, state: androidx.compose.foundation.lazy.LazyListState, pageOffset: () -> Float, dir: () -> Float): Modifier {
    if (Prefs.motion.value == "off") return this
    val rel by remember(index) { derivedStateOf { (index - state.firstVisibleItemIndex).coerceIn(0, 14) } }
    return graphicsLayer {
        val d = kotlin.math.abs(pageOffset()).coerceIn(0f, 1f)
        if (d < 0.0005f) return@graphicsLayer
        // trailing grows down the list, eased so the first rows stay close and the wave builds smoothly
        val f = (0.035f + 0.026f * rel).coerceAtMost(0.32f)
        // strongest mid-way, gone at both ends (on screen and fully off): a gentle bell, never a jump
        val bell = 4f * d * (1f - d)
        translationX = -dir() * f * bell * size.width
    }
}

/** rows of a freshly selected folder rise in one after another (first ~12 rows, 26 ms apart, bouncy spring) */
@Composable
private fun Modifier.cascadeIn(index: Int, gen: Int): Modifier {
    if (gen == 0 || index > 12) return this
    val v = remember(gen) { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(gen) {
        kotlinx.coroutines.delay(index * 26L)
        v.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.72f, stiffness = 360f * Prefs.motionSpeed.value * Prefs.motionSpeed.value))
    }
    return graphicsLayer {
        alpha = v.value.coerceIn(0f, 1f)
        translationY = (1f - v.value) * 36.dp.toPx()
        val sc = 0.96f + 0.04f * v.value
        scaleX = sc; scaleY = sc
    }
}

/**
 * The side menu's gestures live on the root, which never moves: positions measured on the sliding content would
 * shift with it and eat the drag (that's why the menu used to be almost impossible to pull out).
 *  - closed + chat list allows it: a light swipe right anywhere pulls it out with the finger;
 *  - open: a swipe left anywhere pushes it back, a tap on the app side closes it.
 * Initial pass: decided before the list / pager / rows see the movement.
 */
private fun Modifier.drawerGestures(drawer: DrawerControl): Modifier = pointerInput(drawer) {
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val startOpen = drawer.value > 0.5f
        if (!startOpen && (!drawer.canOpen || drawer.value > 0.01f)) return@awaitEachGesture
        val tracker = androidx.compose.ui.input.pointer.util.VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        var last = down.position
        var dx = 0f
        var dy = 0f
        var dragging = false
        var cancelled = false
        while (true) {
            val ev = awaitPointerEvent(PointerEventPass.Initial)
            val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
            if (!ch.pressed) break
            tracker.addPosition(ch.uptimeMillis, ch.position)
            val d = ch.position - last
            last = ch.position
            if (!dragging) {
                dx += d.x; dy += d.y
                if (kotlin.math.abs(dy) > slop * 1.2f && kotlin.math.abs(dy) > kotlin.math.abs(dx) * 1.2f) { cancelled = true; break }
                val wrongWay = if (startOpen) dx > slop else dx < -slop
                if (wrongWay) { cancelled = true; break }
                val go = if (startOpen) -dx > slop * 0.35f else dx > slop * 0.35f
                if (go && kotlin.math.abs(dx) > kotlin.math.abs(dy) * 0.7f) { dragging = true; drawer.drag(dx) } else continue
            } else drawer.drag(d.x)
            ch.consume()
        }
        if (dragging) drawer.settle(tracker.calculateVelocity().x)
        else if (!cancelled && startOpen && down.position.x > drawer.widthPx) drawer.close()   // tap on the app side
    }
}

