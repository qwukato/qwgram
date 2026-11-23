package app.qwgram.next.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Shortcut
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.qwgram.next.Notifier
import app.qwgram.next.Prefs
import app.qwgram.next.td.Td
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.Texts
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.ui.focus.onFocusChanged
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.outlined.Poll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.sp
import org.drinkless.tdlib.TdApi
import java.io.File
import kotlin.math.roundToInt

/** Messages of one open chat, newest first. Lives as long as the chat screen. */
class ChatController(val chatId: Long) {
    val messages = mutableStateListOf<TdApi.Message>()
    val replies = mutableStateMapOf<Long, TdApi.Message>()
    var loading by mutableStateOf(false)
    var reachedEnd by mutableStateOf(false)
    var pinned by mutableStateOf<TdApi.Message?>(null)

    /** true after jumping into older history: newer messages are not loaded yet */
    var hasNewer by mutableStateOf(false)

    /** loads history around [id] so it can be scrolled to; returns false if the message is gone */
    suspend fun jumpTo(id: Long): Boolean {
        if (messages.any { it.id == id }) return true
        loading = true
        val r = Td.callOrNull(TdApi.GetChatHistory(chatId, id, -20, 50, false))
        loading = false
        if (r == null || r.messages.isEmpty()) return false
        messages.clear(); reachedEnd = false
        addOlder(r.messages.toList())
        hasNewer = TdStore.chats.peek(chatId)?.lastMessage?.id?.let { last -> messages.none { it.id == last } } ?: false
        return messages.any { it.id == id }
    }

    suspend fun loadNewer() {
        if (loading || !hasNewer || messages.isEmpty()) return
        loading = true
        val r = Td.callOrNull(TdApi.GetChatHistory(chatId, messages.first().id, -49, 50, false))
        loading = false
        val have = messages.mapTo(HashSet()) { it.id }
        val newer = r?.messages?.filter { it.id !in have && it.id > messages.first().id }.orEmpty().sortedByDescending { it.id }
        messages.addAll(0, newer)
        newer.forEach { fetchReply(it) }
        val last = TdStore.chats.peek(chatId)?.lastMessage?.id
        if (newer.isEmpty() || messages.any { it.id == last }) hasNewer = false
    }

    suspend fun reset() {
        messages.clear(); hasNewer = false; reachedEnd = false
        loadInitial()
    }

    suspend fun loadInitial() {
        loading = true
        var from = 0L
        var tries = 0
        // TDLib may answer with only a few cached messages at first; ask until we have a screenful
        while (messages.size < 40 && tries < 4) {
            val r = Td.callOrNull(TdApi.GetChatHistory(chatId, from, 0, 50, false)) ?: break
            if (r.messages.isEmpty()) {
                if (tries > 0) reachedEnd = true
                tries++
                continue
            }
            addOlder(r.messages.toList())
            from = messages.last().id
            tries++
        }
        loading = false
        Td.callOrNull(TdApi.GetChatPinnedMessage(chatId))?.let { pinned = it }
    }

    suspend fun loadOlder() {
        if (loading || reachedEnd || messages.isEmpty()) return
        loading = true
        val r = Td.callOrNull(TdApi.GetChatHistory(chatId, messages.last().id, 0, 50, false))
        if (r == null || r.messages.isEmpty()) reachedEnd = true else addOlder(r.messages.toList())
        loading = false
    }

    private fun addOlder(list: List<TdApi.Message>) {
        val have = messages.mapTo(HashSet()) { it.id }
        messages.addAll(list.filter { it.id !in have })
        list.forEach { fetchReply(it) }
    }

    fun fetchReply(m: TdApi.Message) {
        val r = m.replyTo as? TdApi.MessageReplyToMessage ?: return
        if (r.messageId == 0L || replies.containsKey(m.id)) return
        Td.send(TdApi.GetRepliedMessage(m.chatId, m.id)) { res -> if (res is TdApi.Message) replies[m.id] = res }
    }

    fun onEvent(u: TdApi.Update) {
        when (u) {
            is TdApi.UpdateNewMessage -> if (u.message.chatId == chatId && !hasNewer && messages.none { it.id == u.message.id }) {
                messages.add(0, u.message)
                fetchReply(u.message)
            }
            is TdApi.UpdateMessageSendSucceeded -> if (u.message.chatId == chatId) replace(u.oldMessageId, u.message)
            is TdApi.UpdateMessageSendFailed -> if (u.message.chatId == chatId) replace(u.oldMessageId, u.message)
            is TdApi.UpdateDeleteMessages -> if (u.chatId == chatId && u.isPermanent) {
                val ids = u.messageIds.toSet()
                messages.removeAll { it.id in ids }
            }
            is TdApi.UpdateMessageContent -> if (u.chatId == chatId) mutate(u.messageId) { it.content = u.newContent }
            is TdApi.UpdateMessageEdited -> if (u.chatId == chatId) mutate(u.messageId) { it.editDate = u.editDate }
            is TdApi.UpdateMessageInteractionInfo -> if (u.chatId == chatId) mutate(u.messageId) { it.interactionInfo = u.interactionInfo }
            is TdApi.UpdateMessageIsPinned -> if (u.chatId == chatId) {
                mutate(u.messageId) { it.isPinned = u.isPinned }
                Td.send(TdApi.GetChatPinnedMessage(chatId)) { r -> pinned = r as? TdApi.Message }
            }
            else -> Unit
        }
    }

    private fun replace(oldId: Long, m: TdApi.Message) {
        val i = messages.indexOfFirst { it.id == oldId }
        if (i >= 0) messages[i] = m else if (messages.none { it.id == m.id }) messages.add(0, m)
    }

    private fun mutate(id: Long, block: (TdApi.Message) -> Unit) {
        val i = messages.indexOfFirst { it.id == id }
        if (i < 0) return
        val m = messages[i]
        block(m)
        messages[i] = copy(m)
    }

    private fun copy(m: TdApi.Message): TdApi.Message = TdApi.Message().also { n ->
        n.id = m.id; n.senderId = m.senderId; n.chatId = m.chatId; n.sendingState = m.sendingState
        n.isOutgoing = m.isOutgoing; n.isPinned = m.isPinned; n.isChannelPost = m.isChannelPost
        n.date = m.date; n.editDate = m.editDate; n.forwardInfo = m.forwardInfo; n.interactionInfo = m.interactionInfo
        n.replyTo = m.replyTo; n.content = m.content; n.replyMarkup = m.replyMarkup; n.mediaAlbumId = m.mediaAlbumId
        n.authorSignature = m.authorSignature; n.viaBotUserId = m.viaBotUserId; n.containsUnreadMention = m.containsUnreadMention
    }
}

private sealed interface Entry {
    val key: String
    data class Msg(val m: TdApi.Message, val grp: Grp) : Entry { override val key get() = "m${m.id}" }
    data class Day(val date: Int) : Entry { override val key get() = "d$date" }
    data object Unread : Entry { override val key get() = "unread" }
}

/** position of a message inside a run of messages from the same sender */
enum class Grp { Single, Top, Middle, Bottom }

@Composable
fun ChatScreen(chatId: Long) {
    val chat = TdStore.chats[chatId]
    if (chat == null) {
        LaunchedEffect(chatId) { Td.send(TdApi.GetChat(chatId)) { r -> if (r is TdApi.Chat) TdStore.onUpdate(TdApi.UpdateNewChat(r)) } }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        return
    }
    val ctx = LocalContext.current
    val ctl = remember(chatId) { ChatController(chatId) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val firstUnreadId = remember(chatId) { if (chat.unreadCount > 0) chat.lastReadInboxMessageId else 0L }

    var text by remember(chatId) { mutableStateOf((chat.draftMessage?.content as? TdApi.DraftMessageContentText)?.text?.text ?: "") }
    var replyTo by remember(chatId) { mutableStateOf<TdApi.Message?>(null) }
    var editing by remember(chatId) { mutableStateOf<TdApi.Message?>(null) }
    var menuFor by remember { mutableStateOf<TdApi.Message?>(null) }
    var forward by remember { mutableStateOf<List<TdApi.Message>?>(null) }
    var deleteFor by remember { mutableStateOf<List<TdApi.Message>?>(null) }
    var viewer by remember { mutableStateOf<TdApi.Photo?>(null) }
    val selected = remember { mutableStateListOf<Long>() }
    var searching by remember { mutableStateOf(false) }
    val search = remember(chatId) { ChatSearch(chatId) }
    var panel by remember { mutableStateOf(false) }
    // keyboard ⇄ panel without jumps: the panel takes the keyboard's height and stays until the keyboard is fully up
    var panelHolding by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val imeNow = WindowInsets.ime.getBottom(density)
    val imeTarget = WindowInsets.imeAnimationTarget.getBottom(density)
    LaunchedEffect(imeTarget) { if (imeTarget > with(density) { 180.dp.roundToPx() }) Prefs.kbHeight.value = imeTarget }
    LaunchedEffect(imeNow, panelHolding) {
        val kb = Prefs.kbHeight.value
        if (panelHolding && kb > 0 && imeNow >= kb * 0.92f) { panel = false; panelHolding = false }
    }
    val panelHeight = with(density) {
        val kb = Prefs.kbHeight.value
        if (kb > 0) kb.toDp() else 300.dp + WindowInsets.navigationBars.getBottom(this).toDp()
    }
    var menuAnchor by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var highlight by remember { mutableStateOf(0L) }

    DisposableEffect(chatId) {
        Td.send(TdApi.OpenChat(chatId))
        Notifier.openChatId = chatId
        Notifier.clear(chatId)
        onDispose {
            Td.send(TdApi.CloseChat(chatId))
            if (Notifier.openChatId == chatId) Notifier.openChatId = 0L
            Player.stop()
            // keep what the user typed as a Telegram draft
            val t = text
            val d = if (t.isBlank() && replyTo == null) null else TdApi.DraftMessage().apply {
                replyTo?.let { r -> this.replyTo = TdApi.InputMessageReplyToMessage().apply { messageId = r.id } }
                date = (System.currentTimeMillis() / 1000).toInt()
                content = TdApi.DraftMessageContentText().apply { this.text = TdApi.FormattedText(t, arrayOf()) }
            }
            if (editing == null) Td.send(TdApi.SetChatDraftMessage(chatId, null, d))
        }
    }
    LaunchedEffect(chatId) {
        launch { TdStore.messageEvents.collect { ctl.onEvent(it) } }
        ctl.loadInitial()
        val idx = if (firstUnreadId != 0L) ctl.messages.indexOfFirst { it.id <= firstUnreadId } else -1
        if (idx > 2) listState.scrollToItem(idx)
    }
    // load older history near the top, newer near the bottom (after a jump)
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (last >= listState.layoutInfo.totalItemsCount - 8) ctl.loadOlder()
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { first -> if (first < 4) ctl.loadNewer() }
    }
    // mark visible messages read
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.key } }.debounce(250).collect { keys ->
            if (!Prefs.markReadOnOpen.value) return@collect
            val ids = keys.mapNotNull { (it as? String)?.takeIf { k -> k.startsWith("m") }?.drop(1)?.toLongOrNull() }
            if (ids.isNotEmpty()) Td.send(TdApi.ViewMessages(chatId, ids.toLongArray(), TdApi.MessageSourceChatHistory(), true))
        }
    }
    // keep the newest message in view when something arrives and we are at the bottom
    val newest = ctl.messages.firstOrNull()?.id
    LaunchedEffect(newest) {
        if (!ctl.hasNewer && listState.firstVisibleItemIndex <= 1) listState.animateScrollToItem(0)
    }

    val entries by remember(ctl) {
        derivedStateOf {
            val ms = ctl.messages
            val out = ArrayList<Entry>(ms.size + 8)
            for (i in ms.indices) {
                val m = ms[i]
                val newer = ms.getOrNull(i - 1)
                val older = ms.getOrNull(i + 1)
                fun same(o: TdApi.Message?) = o != null && Texts.service(o) == null && Texts.service(m) == null &&
                    TdStore.senderId(o.senderId) == TdStore.senderId(m.senderId) && o.isOutgoing == m.isOutgoing &&
                    Texts.sameDay(o.date, m.date) && kotlin.math.abs(o.date - m.date) < 600
                val up = same(older)
                val down = same(newer)
                val g = when {
                    up && down -> Grp.Middle
                    up -> Grp.Bottom
                    down -> Grp.Top
                    else -> Grp.Single
                }
                out.add(Entry.Msg(m, g))
                if (firstUnreadId != 0L && m.id > firstUnreadId && (older == null || older.id <= firstUnreadId) && !m.isOutgoing) out.add(Entry.Unread)
                if (older == null || !Texts.sameDay(older.date, m.date)) out.add(Entry.Day(m.date))
            }
            out
        }
    }

    /** scroll to a message, loading history around it if needed, and flash it */
    fun jump(id: Long) {
        scope.launch {
            if (!ctl.jumpTo(id)) { Toast.makeText(ctx, "Сообщение не найдено", Toast.LENGTH_SHORT).show(); return@launch }
            val i = entries.indexOfFirst { it is Entry.Msg && it.m.id == id }
            if (i >= 0) listState.animateScrollToItem(i, -200)
            highlight = id
            delay(1400)
            if (highlight == id) highlight = 0L
        }
    }

    // opened from search / media: scroll to that message once history is there
    val hasHistory = ctl.messages.isNotEmpty()
    val routeOnTop = Nav.top == Route.Chat(chatId)
    LaunchedEffect(chatId, hasHistory, routeOnTop) {
        val j = Nav.jumpTo
        if (hasHistory && routeOnTop && j != null && j.first == chatId) { Nav.jumpTo = null; jump(j.second) }
    }
    // live in-chat search
    LaunchedEffect(search.query) {
        if (!searching) return@LaunchedEffect
        delay(350)
        search.run()
        search.current?.let { jump(it.id) }
    }

    val canSend = TdStore.canSend(chat)
    val group = TdStore.isGroup(chat)
    val selecting = selected.isNotEmpty()

    BackHandler(enabled = selecting || searching || panel) {
        when {
            selecting -> selected.clear()
            panel -> { panel = false; panelHolding = false }
            else -> { searching = false; search.query = ""; search.results = emptyList() }
        }
    }

    val menuOpen = menuFor != null
    val blur by androidx.compose.animation.core.animateDpAsState(if (menuOpen) 10.dp else 0.dp, Springs.effects(), label = "blur")
    Box(Modifier.fillMaxSize().then(if (blur > 0.dp) Modifier.blur(blur) else Modifier)) {
        ChatWallpaper()
        var topH by remember { mutableStateOf(0.dp) }
        var bottomH by remember { mutableStateOf(0.dp) }
        val fade = wallpaperBase()
        Box(Modifier.fillMaxSize().then(if (panel) Modifier else Modifier.imePadding())) {
            // ---- top: bars over a fade (content scrolls underneath, like Telegram)
            Column(
                Modifier.zIndex(2f).align(Alignment.TopCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(0f to fade, 0.7f to fade.copy(alpha = 0.8f), 1f to fade.copy(alpha = 0f)))
                    .onSizeChanged { topH = with(density) { it.height.toDp() } },
            ) {
            AnimatedContent(
                targetState = when { selecting -> 2; searching -> 1; else -> 0 },
                transitionSpec = { fadeIn(Springs.effects()).togetherWith(fadeOut(Springs.exit())) },
                label = "topbar",
            ) { mode ->
                when (mode) {
                    2 -> SelectionBar(
                        selected.size,
                        onClose = { selected.clear() },
                        onCopy = {
                            val ms = ctl.messages.filter { it.id in selected }.sortedBy { it.id }
                            copy(ctx, ms.joinToString("\n\n") { Texts.caption(it.content)?.text ?: Texts.preview(it) })
                            Toast.makeText(ctx, "Скопировано", Toast.LENGTH_SHORT).show()
                            selected.clear()
                        },
                        onForward = { forward = ctl.messages.filter { it.id in selected }.sortedBy { it.id }; selected.clear() },
                        onDelete = { deleteFor = ctl.messages.filter { it.id in selected } },
                    )
                    1 -> ChatSearchBar(
                        search,
                        onClose = { searching = false; search.query = ""; search.results = emptyList() },
                        onSubmit = { scope.launch { search.run(); search.current?.let { jump(it.id) } } },
                        onStep = { d -> search.index = (search.index + d).coerceIn(0, search.results.size - 1); search.current?.let { jump(it.id) } },
                    )
                    else -> ChatTopBar(chat, onSearch = { searching = true })
                }
            }
            ctl.pinned?.let { p ->
                PinnedBar(p, onClick = { jump(p.id) }, onClose = { Td.send(TdApi.UnpinChatMessage(chatId, p.id)) })
            }
            Spacer(Modifier.height(18.dp))
            }
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 8.dp, end = 8.dp, top = (topH - 12.dp).coerceAtLeast(8.dp), bottom = (bottomH - 10.dp).coerceAtLeast(8.dp)),
                ) {
                    items(entries, key = { it.key }, contentType = { it::class }) { e ->
                        when (e) {
                            is Entry.Day -> DayChip(Texts.dayHeader(e.date))
                            Entry.Unread -> UnreadDivider()
                            is Entry.Msg -> {
                                val id = e.m.id
                                MessageItem(
                                    m = e.m, grp = e.grp, chat = chat, group = group,
                                    reply = ctl.replies[id],
                                    modifier = Modifier.animateItem(fadeInSpec = Springs.effects(), placementSpec = Springs.smooth(), fadeOutSpec = Springs.exit()),
                                    selection = if (selecting) (id in selected) else null,
                                    highlighted = highlight == id,
                                    onTap = { rect ->
                                        if (selecting) { if (id in selected) selected.remove(id) else selected.add(id) }
                                        else { menuAnchor = rect; keyboard?.hide(); menuFor = e.m }
                                    },
                                    onLong = {
                                        haptic(HapticFeedbackType.LongPress)
                                        if (id in selected) selected.remove(id) else selected.add(id)
                                    },
                                    onDouble = {
                                        if (!selecting) when (Prefs.doubleTap.value) {
                                            "react" -> react(e.m, Prefs.doubleTapEmoji.value)
                                            "reply" -> if (canSend) { replyTo = e.m; editing = null }
                                        }
                                    },
                                    onSwipeReply = { if (canSend && !selecting) { haptic(HapticFeedbackType.TextHandleMove); replyTo = e.m; editing = null } },
                                    onPhoto = { viewer = it },
                                    onReplyClick = { r -> jump(r) },
                                )
                            }
                        }
                    }
                    if (ctl.loading) item(key = "loading") {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { LoadingIndicator(Modifier.size(36.dp)) }
                    }
                }
                // "@" and "♥" jump to the next unread mention / reaction (Telegram)
                Column(Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = bottomH + 64.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        Triple(chat.unreadMentionCount, "@", TdApi.SearchMessagesFilterUnreadMention() as TdApi.SearchMessagesFilter),
                        Triple(chat.unreadReactionCount, "❤", TdApi.SearchMessagesFilterUnreadReaction()),
                    ).forEach { (count, label, filter) ->
                        androidx.compose.animation.AnimatedVisibility(
                            count > 0, enter = scaleIn(Springs.fastSpatial()) + fadeIn(Springs.effects()), exit = scaleOut(Springs.exit()) + fadeOut(Springs.exit()),
                        ) {
                            Box {
                                MorphBox(
                                    onClick = {
                                        scope.launch {
                                            val r = Td.callOrNull(TdApi.SearchChatMessages(chatId, null, "", null, 0, 0, 100, filter))
                                            val target = r?.messages?.lastOrNull() ?: return@launch
                                            jump(target.id)
                                            Td.send(TdApi.ViewMessages(chatId, longArrayOf(target.id), TdApi.MessageSourceChatHistory(), true))
                                        }
                                    },
                                    modifier = Modifier.size(48.dp), rest = 24.dp, pressed = 12.dp,
                                    color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, elevation = 3.dp,
                                ) { Text(label, style = MaterialTheme.typography.titleMedium) }
                                Badge(Modifier.align(Alignment.TopEnd)) { Text(count.toString()) }
                            }
                        }
                    }
                }
                val showDown by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 || ctl.hasNewer } }
                androidx.compose.animation.AnimatedVisibility(
                    showDown, Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = bottomH + 4.dp),
                    enter = scaleIn(Springs.fastSpatial()) + fadeIn(Springs.effects()), exit = scaleOut(Springs.exit()) + fadeOut(Springs.exit()),
                ) {
                    Box {
                        MorphBox(
                            onClick = { scope.launch { if (ctl.hasNewer) ctl.reset(); listState.animateScrollToItem(0) } },
                            modifier = Modifier.size(48.dp), rest = 24.dp, pressed = 12.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh, elevation = 3.dp,
                        ) { Icon(Icons.Filled.KeyboardArrowDown, "Вниз") }
                        if (chat.unreadCount > 0) Badge(Modifier.align(Alignment.TopEnd)) { Text(chat.unreadCount.toString()) }
                    }
                }
            }
            // ---- bottom: composer (+ panel) over a fade
            Column(
                Modifier.zIndex(2f).align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(0f to fade.copy(alpha = 0f), 0.3f to fade.copy(alpha = 0.8f), 1f to fade))
                    .onSizeChanged { bottomH = with(density) { it.height.toDp() } },
            ) {
            Spacer(Modifier.height(18.dp))
            if (canSend) {
                fun send(silent: Boolean? = null, at: Int = 0) {
                    val t = text.trim()
                    if (t.isEmpty()) return
                    val ed = editing
                    if (ed != null) {
                        Td.send(TdApi.EditMessageText(chatId, ed.id, null, inputText(t)))
                        editing = null
                    } else {
                        sendContent(chatId, inputText(t), replyTo, silent, at)
                        replyTo = null
                        if (at > 0) Toast.makeText(ctx, "Сообщение запланировано", Toast.LENGTH_SHORT).show()
                    }
                    text = ""
                    haptic(HapticFeedbackType.Confirm)
                    if (at == 0) scope.launch { if (ctl.hasNewer) ctl.reset(); listState.animateScrollToItem(0) }
                }
                Composer(
                    chat = chat, text = text, onText = { text = it; typing(chatId) },
                    replyTo = replyTo, editing = editing,
                    panelOpen = panel,
                    onTogglePanel = {
                        if (panel) { panelHolding = true; keyboard?.show() } else { panel = true; keyboard?.hide() }
                    },
                    onFocusText = { if (panel) panelHolding = true },
                    onCancel = { if (editing != null) text = ""; replyTo = null; editing = null },
                    onSend = { send() },
                    onSendSilent = { send(silent = true) },
                    onSchedule = { at -> send(at = at) },
                )
                androidx.compose.animation.AnimatedVisibility(
                    panel,
                    // from the keyboard: appear in place (same height); otherwise slide up smoothly
                    enter = if (imeNow > 0) androidx.compose.animation.EnterTransition.None else expandVertically(Springs.smooth()) + fadeIn(Springs.effects()),
                    exit = if (panelHolding || imeNow > 0) androidx.compose.animation.ExitTransition.None else shrinkVertically(Springs.exit()) + fadeOut(Springs.exit()),
                ) {
                    StickerPanel(
                        chatId, panelHeight,
                        onEmoji = { text += it },
                        onSticker = { s -> sendContent(chatId, stickerContent(s), replyTo); replyTo = null; panel = false },
                        onGif = { a -> sendContent(chatId, gifContent(a), replyTo); replyTo = null; panel = false },
                    )
                }
            } else ChannelBar(chat)
            }
        }
    }

    menuFor?.let { m ->
        MessageMenu(
            m, chat, menuAnchor,
            onDismiss = { menuFor = null },
            onReply = { replyTo = m; editing = null },
            onEdit = {
                editing = m; replyTo = null
                text = (m.content as? TdApi.MessageText)?.text?.text ?: Texts.caption(m.content)?.text.orEmpty()
            },
            onForward = { forward = listOf(m) },
            onDelete = { deleteFor = listOf(m) },
            onCopy = { copy(ctx, Texts.caption(m.content)?.text.orEmpty()); Toast.makeText(ctx, "Скопировано", Toast.LENGTH_SHORT).show() },
            onSelect = { selected.add(m.id) },
            onCopyLink = {
                Td.send(TdApi.GetMessageLink(m.chatId, m.id, 0, 0, "", false, false)) { r ->
                    if (r is TdApi.MessageLink) { copy(ctx, r.link); Toast.makeText(ctx, "Ссылка скопирована", Toast.LENGTH_SHORT).show() }
                }
            },
            onSave = {
                TdStore.me?.let { me -> Td.send(TdApi.ForwardMessages(me.id, null, m.chatId, longArrayOf(m.id), null, false, false)) }
                Toast.makeText(ctx, "Сохранено в Избранное", Toast.LENGTH_SHORT).show()
            },
        )
    }
    forward?.let { list -> ForwardSheet(list, onDismiss = { forward = null }) }
    deleteFor?.let { list -> DeleteDialog(list, chat) { deleteFor = null; selected.clear() } }
    viewer?.let { p -> PhotoViewer(p) { viewer = null } }
}

fun inputText(t: String): TdApi.InputMessageText = TdApi.InputMessageText().apply {
    text = TdApi.FormattedText(t, arrayOf())
    clearDraft = true
}

fun sendContent(chatId: Long, content: TdApi.InputMessageContent, replyTo: TdApi.Message? = null, silent: Boolean? = null, scheduleAt: Int = 0) {
    Td.send(TdApi.SendMessage().apply {
        this.chatId = chatId
        this.replyTo = replyTo?.let { r -> TdApi.InputMessageReplyToMessage().apply { messageId = r.id } }
        options = TdApi.MessageSendOptions().apply {
            disableNotification = silent ?: Prefs.silentByDefault.value
            if (scheduleAt > 0) schedulingState = TdApi.MessageSchedulingStateSendAtDate(scheduleAt, 0)
        }
        inputMessageContent = content
    })
}

private var lastTyping = 0L
private fun typing(chatId: Long) {
    val now = System.currentTimeMillis()
    if (now - lastTyping < 4500) return
    lastTyping = now
    Td.send(TdApi.SendChatAction(chatId, null, "", TdApi.ChatActionTyping()))
}

fun react(m: TdApi.Message, emoji: String) {
    val chosen = m.interactionInfo?.reactions?.reactions?.firstOrNull { it.isChosen && (it.type as? TdApi.ReactionTypeEmoji)?.emoji == emoji }
    if (chosen != null) Td.send(TdApi.RemoveMessageReaction(m.chatId, m.id, TdApi.ReactionTypeEmoji(emoji)))
    else Td.send(TdApi.AddMessageReaction(m.chatId, m.id, TdApi.ReactionTypeEmoji(emoji), false, true))
}

private fun copy(ctx: Context, s: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("message", s))
}

@Composable
private fun wallpaperBase(): Color {
    val cs = MaterialTheme.colorScheme
    return when (Prefs.wallpaper.value) { "tonal" -> cs.surfaceContainer; "plain" -> cs.surface; else -> cs.surfaceContainerLow }
}

@Composable
private fun ChatWallpaper() {
    val cs = MaterialTheme.colorScheme
    val mode = Prefs.wallpaper.value
    Box(Modifier.fillMaxSize().background(if (mode == "tonal") cs.surfaceContainer else if (mode == "plain") cs.surface else cs.surfaceContainerLow)) {
        if (mode == "shapes") WallpaperShapes()
        if (mode == "dots") androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val step = 18.dp.toPx()
            val r = 1.4.dp.toPx()
            var y = step / 2
            while (y < size.height) {
                var x = step / 2
                while (x < size.width) {
                    drawCircle(cs.outlineVariant, r, androidx.compose.ui.geometry.Offset(x, y)); x += step
                }
                y += step
            }
        }
    }
}

@Composable
private fun WallpaperShapes() {
    val shapes = remember {
        listOf(
            androidx.compose.material3.MaterialShapes.Cookie9Sided, androidx.compose.material3.MaterialShapes.Flower,
            androidx.compose.material3.MaterialShapes.Sunny, androidx.compose.material3.MaterialShapes.Clover4Leaf,
            androidx.compose.material3.MaterialShapes.Puffy, androidx.compose.material3.MaterialShapes.SoftBurst,
            androidx.compose.material3.MaterialShapes.Pentagon, androidx.compose.material3.MaterialShapes.Heart,
        ).map { shapeFor2(it) }
    }
    val c = MaterialTheme.colorScheme.surfaceContainerHigh
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        for (i in 0 until 14) {
            val s = (60 + (i * 37) % 70).dp
            val x = w * (((i * 53) % 100) / 100f) - s / 2
            val y = h * (((i * 29 + 7) % 100) / 100f) - s / 2
            Box(
                Modifier.offset(x, y).size(s).graphicsLayer { rotationZ = ((i * 41) % 360).toFloat() }
                    .clip(shapes[i % shapes.size]).background(c.copy(alpha = 0.75f)),
            )
        }
    }
}

private fun shapeFor2(p: androidx.graphics.shapes.RoundedPolygon) = p.asShape()

@Composable
private fun ChatTopBar(chat: TdApi.Chat, onSearch: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmBlock by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    val status = Texts.chatStatus(chat)
    val typingNow = !TdStore.actions[chat.id].isNullOrEmpty()
    val privateUser = TdStore.privateUserId(chat).takeIf { it != 0L && !TdStore.isSaved(chat) }
    val floating = Prefs.floatingBars.value
    val cs = MaterialTheme.colorScheme
    val titleArea: @Composable (Modifier) -> Unit = { mod ->
            Row(
                mod.clip(RoundedCornerShape(28.dp)).clickable { Nav.push(Route.Profile(chat.id)) }.padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChatAvatar(chat, 40.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(if (TdStore.isSaved(chat)) "Избранное" else chat.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    AnimatedContent(status, transitionSpec = { fadeIn(Springs.effects()).togetherWith(fadeOut(Springs.exit())) }, label = "status") { st ->
                        if (st.isNotBlank()) Text(
                            st, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = if (typingNow || st == "в сети") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
    }
    val actions: @Composable () -> Unit = {
            MorphIconButton(Icons.Outlined.Search, "Поиск в чате", onSearch)
            Box {
                MorphIconButton(Icons.Outlined.MoreVert, "Ещё", { menu = true })
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(16.dp)) {
                    val muted = TdStore.isMuted(chat)
                    DropdownMenuItem(
                        text = { Text(if (muted) "Включить звук" else "Без звука") },
                        leadingIcon = { Icon(if (muted) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff, null) },
                        onClick = { menu = false; setMuted(chat, !muted) },
                    )
                    DropdownMenuItem(text = { Text("Профиль") }, leadingIcon = { Icon(Icons.Outlined.Info, null) }, onClick = { menu = false; Nav.push(Route.Profile(chat.id)) })
                    DropdownMenuItem(text = { Text("Отложенные") }, leadingIcon = { Icon(Icons.Outlined.Schedule, null) }, onClick = { menu = false; Nav.push(Route.Page("sched:${chat.id}")) })
                    DropdownMenuItem(
                        text = { Text(if (chat.isMarkedAsUnread) "Прочитано" else "Отметить непрочитанным") },
                        leadingIcon = { Icon(Icons.Outlined.MarkChatUnread, null) },
                        onClick = { menu = false; Td.send(TdApi.ToggleChatIsMarkedAsUnread(chat.id, !chat.isMarkedAsUnread)) },
                    )
                    DropdownMenuItem(text = { Text("Очистить историю") }, leadingIcon = { Icon(Icons.Outlined.DeleteSweep, null) }, onClick = { menu = false; confirmClear = true })
                    if (privateUser != null && !TdStore.isBot(chat)) DropdownMenuItem(
                        text = { Text("Заблокировать") }, leadingIcon = { Icon(Icons.Outlined.Block, null) },
                        onClick = { menu = false; confirmBlock = true },
                    )
                    if (chat.type is TdApi.ChatTypeSupergroup || chat.type is TdApi.ChatTypeBasicGroup) DropdownMenuItem(
                        text = { Text(if (TdStore.isChannel(chat)) "Покинуть канал" else "Покинуть группу", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; confirmLeave = true },
                    ) else DropdownMenuItem(
                        text = { Text("Удалить чат", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; deleteChat(chat); Nav.pop() },
                    )
                }
            }
    }
    if (floating) {
        // M3 Expressive: separate floating parts — back button, title pill, actions pill
        Row(
            Modifier.statusBarsPadding().padding(start = 8.dp, end = 8.dp, top = 6.dp).height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = cs.surfaceContainerHigh, shadowElevation = 3.dp) {
                MorphIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", { Nav.pop() }, size = 56.dp)
            }
            Surface(shape = RoundedCornerShape(28.dp), color = cs.surfaceContainerHigh, shadowElevation = 3.dp, modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(contentAlignment = Alignment.CenterStart) { titleArea(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) }
            }
            Surface(shape = RoundedCornerShape(28.dp), color = cs.surfaceContainerHigh, shadowElevation = 3.dp, modifier = Modifier.fillMaxHeight()) {
                Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) { actions() }
            }
        }
    } else {
        Surface(color = cs.surfaceContainer) {
            Row(Modifier.statusBarsPadding().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                MorphIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", { Nav.pop() })
                titleArea(Modifier.weight(1f))
                actions()
            }
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        icon = { Icon(Icons.Outlined.DeleteSweep, null) },
        title = { Text("Очистить историю?") },
        text = { Text("Все сообщения в этом чате будут удалены у вас.") },
        confirmButton = { QTextButton(onClick = { Td.send(TdApi.DeleteChatHistory(chat.id, false, false)); confirmClear = false }) { Text("Очистить") } },
        dismissButton = { QTextButton(onClick = { confirmClear = false }) { Text("Отмена") } },
    )
    if (confirmBlock && privateUser != null) AlertDialog(
        onDismissRequest = { confirmBlock = false },
        icon = { Icon(Icons.Outlined.Block, null) },
        title = { Text("Заблокировать ${chat.title}?") },
        text = { Text("Пользователь не сможет писать вам и звонить.") },
        confirmButton = {
            QTextButton(onClick = {
                Td.send(TdApi.SetMessageSenderBlockList(TdApi.MessageSenderUser(privateUser), TdApi.BlockListMain())); confirmBlock = false
            }) { Text("Заблокировать", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { QTextButton(onClick = { confirmBlock = false }) { Text("Отмена") } },
    )
    if (confirmLeave) AlertDialog(
        onDismissRequest = { confirmLeave = false },
        icon = { Icon(Icons.AutoMirrored.Outlined.Logout, null) },
        title = { Text(if (TdStore.isChannel(chat)) "Покинуть канал?" else "Покинуть группу?") },
        confirmButton = {
            QTextButton(onClick = { Td.send(TdApi.LeaveChat(chat.id)); confirmLeave = false; Nav.pop() }) { Text("Покинуть", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { QTextButton(onClick = { confirmLeave = false }) { Text("Отмена") } },
    )
}

@Composable
private fun PinnedBar(m: TdApi.Message, onClick: () -> Unit, onClose: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(3.dp).height(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Закреплённое сообщение", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(Texts.preview(m), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            MorphIconButton(Icons.Filled.Close, "Открепить", onClose)
        }
    }
}

@Composable
private fun DayChip(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
            Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun UnreadDivider() {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
        Text("Непрочитанные сообщения", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(6.dp))
    }
}

@Composable
private fun ChannelBar(chat: TdApi.Chat) {
    val muted = TdStore.isMuted(chat)
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), contentAlignment = Alignment.Center) {
            QButton(onClick = { setMuted(chat, !muted) }, tonal = true, modifier = Modifier.fillMaxWidth(0.7f).height(48.dp)) {
                Icon(if (muted) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff, null)
                Spacer(Modifier.width(8.dp))
                Text(if (muted) "Включить звук" else "Выключить звук")
            }
        }
    }
}

// ------------------------------------------------------------------ composer

@Composable
private fun Composer(
    chat: TdApi.Chat,
    text: String,
    onText: (String) -> Unit,
    replyTo: TdApi.Message?,
    editing: TdApi.Message?,
    panelOpen: Boolean,
    onTogglePanel: () -> Unit,
    onFocusText: () -> Unit,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    onSendSilent: () -> Unit,
    onSchedule: (Int) -> Unit,
) {
    var sendOptions by remember { mutableStateOf(false) }
    var poll by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val haptic = rememberHaptic()
    var attach by remember { mutableStateOf(false) }
    val recorder = remember { VoiceRecorder(ctx.applicationContext) }
    var recording by remember { mutableStateOf(false) }
    var recSecs by remember { mutableIntStateOf(0) }
    val dragX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        uris.forEach { sendUri(ctx, chat.id, it, asDocument = false, replyTo) }
        if (uris.isNotEmpty()) onCancel()
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { sendUri(ctx, chat.id, it, asDocument = true, replyTo) }
        if (uris.isNotEmpty()) onCancel()
    }
    LaunchedEffect(recording) {
        while (recording) { recSecs = recorder.elapsed(); delay(200) }
    }
    val radius = Prefs.bubbleRadius.value.dp

    val floating = Prefs.floatingBars.value
    Surface(color = if (floating) Color.Transparent else cs.surfaceContainer) {
        Column(Modifier.then(if (panelOpen) Modifier else Modifier.navigationBarsPadding()).padding(horizontal = 8.dp, vertical = 6.dp)) {
            AnimatedVisibility(replyTo != null || editing != null, enter = expandVertically(Springs.smooth()) + fadeIn(Springs.effects()), exit = shrinkVertically(Springs.exit()) + fadeOut(Springs.exit())) {
                val m = editing ?: replyTo
                Surface(shape = RoundedCornerShape(20.dp), color = cs.surfaceContainerHigh, shadowElevation = if (floating) 2.dp else 0.dp, modifier = Modifier.padding(bottom = 6.dp)) {
                    Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (editing != null) Icons.Outlined.Edit else Icons.AutoMirrored.Filled.Reply, null, tint = cs.primary)
                        Spacer(Modifier.width(12.dp))
                        Box(Modifier.width(2.dp).height(36.dp).background(cs.primary))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (editing != null) "Редактирование" else "В ответ " + (if (m?.isOutgoing == true) "себе" else TdStore.senderName(m?.senderId)),
                                style = MaterialTheme.typography.labelLarge, color = cs.primary, maxLines = 1,
                            )
                            if (m != null) Text(Texts.preview(m), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        MorphIconButton(Icons.Filled.Close, "Отменить", onCancel)
                    }
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Surface(
                    shape = RoundedCornerShape(26.dp), color = if (floating) cs.surfaceContainerHigh else cs.surfaceContainerHigh,
                    shadowElevation = if (floating) 3.dp else 0.dp,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) {
                    Box {
                        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(horizontal = 4.dp)) {
                            AnimatedContent(panelOpen, transitionSpec = { (scaleIn(Springs.fastSpatial()) + fadeIn(Springs.effects())).togetherWith(scaleOut(Springs.exit()) + fadeOut(Springs.exit())) }, label = "emo", modifier = Modifier.padding(bottom = 2.dp)) { open ->
                                MorphIconButton(if (open) Icons.Outlined.Keyboard else Icons.Outlined.EmojiEmotions, if (open) "Клавиатура" else "Эмодзи и стикеры", onTogglePanel, tint = cs.onSurfaceVariant)
                            }
                            BasicTextField(
                                value = text, onValueChange = onText,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
                                cursorBrush = SolidColor(cs.primary),
                                maxLines = 8,
                                modifier = Modifier.weight(1f).padding(top = 14.dp, bottom = 14.dp, end = 4.dp)
                                    .onFocusChanged { if (it.isFocused) onFocusText() },
                                keyboardOptions = if (Prefs.sendByEnter.value) androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send) else androidx.compose.foundation.text.KeyboardOptions.Default,
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { onSend() }),
                                decorationBox = { inner ->
                                    if (text.isEmpty()) Text("Сообщение", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                                    inner()
                                },
                            )
                            MorphIconButton(Icons.Outlined.AttachFile, "Вложение", { attach = true }, modifier = Modifier.padding(bottom = 2.dp), tint = cs.onSurfaceVariant)
                        }
                        androidx.compose.animation.AnimatedVisibility(recording, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.matchParentSize()) {
                            Row(
                                Modifier.fillMaxSize().background(cs.surfaceContainerHigh).padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Dot(cs.error, 10.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(Texts.duration(recSecs), style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.weight(1f))
                                Text("‹ Влево — отмена", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.offset { IntOffset((dragX.value / 2).roundToInt(), 0) })
                            }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                val ready = text.isNotBlank() || editing != null
                val corner by animateDpAsState(if (ready) 16.dp else 26.dp, Springs.fastSpatial(), label = "send")
                val scale by androidx.compose.animation.core.animateFloatAsState(if (recording) 1.35f else 1f, Springs.fastSpatial(), label = "rec")
                val sendMorph = remember { androidx.graphics.shapes.Morph(androidx.compose.material3.MaterialShapes.Circle, androidx.compose.material3.MaterialShapes.Cookie6Sided) }
                val morphP by androidx.compose.animation.core.animateFloatAsState(if (ready) 1f else 0f, Springs.fastSpatial(), label = "sm")
                val spin by androidx.compose.animation.core.animateFloatAsState(if (ready) 60f else 0f, Springs.spatial(), label = "spin")
                Surface(
                    shape = MorphShape(sendMorph, morphP),
                    shadowElevation = if (floating) 3.dp else 0.dp,
                    color = if (ready || recording) cs.primary else cs.primaryContainer,
                    contentColor = if (ready || recording) cs.onPrimary else cs.onPrimaryContainer,
                    modifier = Modifier.size(52.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale; translationX = dragX.value; rotationZ = spin }
                        .pointerInput(ready) {
                            if (ready) {
                                detectTapGestures(
                                    onTap = { onSend() },
                                    onLongPress = { if (editing == null) { haptic(HapticFeedbackType.LongPress); sendOptions = true } },
                                )
                            } else {
                                detectTapGestures(
                                    onTap = { Toast.makeText(ctx, "Удерживайте, чтобы записать голосовое", Toast.LENGTH_SHORT).show() },
                                    onLongPress = {
                                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                            micPermission.launch(Manifest.permission.RECORD_AUDIO)
                                        } else if (!recorder.supported) {
                                            Toast.makeText(ctx, "Голосовые доступны на Android 10 и новее", Toast.LENGTH_SHORT).show()
                                        } else if (recorder.start()) {
                                            recording = true
                                            haptic(HapticFeedbackType.LongPress)
                                            Td.send(TdApi.SendChatAction(chat.id, null, "", TdApi.ChatActionRecordingVoiceNote()))
                                        }
                                    },
                                )
                            }
                        }
                        .pointerInput(recording) {
                            if (recording) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val ev = awaitPointerEvent()
                                        val ch = ev.changes.firstOrNull() ?: continue
                                        if (!ch.pressed) {
                                            val res = recorder.stop(false)
                                            recording = false
                                            scope.launch { dragX.animateTo(0f) }
                                            if (res != null) {
                                                sendVoice(chat.id, res.first, res.second, replyTo)
                                                onCancel()
                                            }
                                            break
                                        }
                                        val dx = (ch.position.x - size.width / 2f).coerceAtMost(0f)
                                        scope.launch { dragX.snapTo(dx) }
                                        if (dx < -160f) {
                                            recorder.stop(true)
                                            recording = false
                                            haptic(HapticFeedbackType.Reject)
                                            scope.launch { dragX.animateTo(0f) }
                                            break
                                        }
                                    }
                                }
                            }
                        },
                ) {
                    Box(Modifier.graphicsLayer { rotationZ = -spin }, contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = when { editing != null -> 2; ready -> 1; else -> 0 },
                            transitionSpec = { (scaleIn(Springs.fastSpatial()) + fadeIn(Springs.effects())).togetherWith(scaleOut(Springs.exit()) + fadeOut(Springs.exit())) },
                            label = "sendicon",
                        ) { s ->
                            when (s) {
                                2 -> Icon(Icons.Rounded.Check, "Сохранить")
                                1 -> Icon(Icons.AutoMirrored.Filled.Send, "Отправить")
                                else -> Icon(Icons.Filled.Mic, "Голосовое")
                            }
                        }
                    }
                }
            }
        }
    }

    if (poll) PollDialog(onDismiss = { poll = false }) { content -> sendContent(chat.id, content, replyTo); poll = false; onCancel() }
    if (sendOptions) SendOptionsSheet(onDismiss = { sendOptions = false }, onSilent = onSendSilent, onSchedule = onSchedule)
    if (attach) {
        ModalBottomSheet(onDismissRequest = { attach = false }) {
            Text("Отправить", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                AttachAction("Галерея", Icons.Outlined.Image, androidx.compose.material3.MaterialShapes.Cookie9Sided, 0) {
                    attach = false
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }
                AttachAction("Файл", Icons.Outlined.Description, androidx.compose.material3.MaterialShapes.Flower, 1) {
                    attach = false
                    pickFile.launch(arrayOf("*/*"))
                }
                if (chat.type !is TdApi.ChatTypeSecret) AttachAction("Опрос", Icons.Outlined.Poll, androidx.compose.material3.MaterialShapes.Flower, 2) {
                    attach = false
                    poll = true
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AttachAction(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, shape: androidx.graphics.shapes.RoundedPolygon, tone: Int, onClick: () -> Unit) {
    val (bg, fg) = avatarColors(tone.toLong())
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
        MorphBox(onClick = onClick, modifier = Modifier.size(64.dp), rest = 32.dp, pressed = 16.dp, color = bg, contentColor = fg) { Icon(icon, null) }
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.labelLarge)
    }
}

private fun copyToCache(ctx: Context, uri: Uri): File? = try {
    val name = (ctx.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    } ?: "file").replace('/', '_')
    val dir = File(ctx.cacheDir, "send/${System.nanoTime()}").apply { mkdirs() }
    val f = File(dir, name)
    ctx.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
    f
} catch (e: Exception) {
    null
}

private fun sendUri(ctx: Context, chatId: Long, uri: Uri, asDocument: Boolean, replyTo: TdApi.Message?) {
    val f = copyToCache(ctx, uri) ?: return
    val mime = ctx.contentResolver.getType(uri).orEmpty()
    val input = TdApi.InputFileLocal(f.absolutePath)
    val content: TdApi.InputMessageContent = when {
        !asDocument && mime.startsWith("image/") && !mime.contains("gif") -> TdApi.InputMessagePhoto().apply { photo = TdApi.InputPhoto().apply { photo = input } }
        !asDocument && mime.startsWith("video/") -> TdApi.InputMessageVideo().apply { video = TdApi.InputVideo().apply { video = input; supportsStreaming = true } }
        else -> TdApi.InputMessageDocument().apply { document = TdApi.InputDocument().apply { document = input } }
    }
    sendContent(chatId, content, replyTo)
}

private fun sendVoice(chatId: Long, f: File, secs: Int, replyTo: TdApi.Message?) {
    sendContent(chatId, TdApi.InputMessageVoiceNote().apply {
        voiceNote = TdApi.InputVoiceNote().apply { voiceNote = TdApi.InputFileLocal(f.absolutePath); duration = secs; waveform = ByteArray(0) }
    }, replyTo)
}

// ------------------------------------------------------------------ menus & sheets

private class MenuItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val danger: Boolean = false, val action: () -> Unit)

/**
 * Telegram-style context menu, Material 3 Expressive look: floats next to the message (below it, or above when
 * there is no room), reactions in a pill on top, actions split into separate cards. Springs out of the message;
 * the chat behind is blurred and dimmed a little. Tap outside or Back to close.
 */
@Composable
private fun MessageMenu(
    m: TdApi.Message, chat: TdApi.Chat, anchor: androidx.compose.ui.geometry.Rect, onDismiss: () -> Unit,
    onReply: () -> Unit, onEdit: () -> Unit, onForward: () -> Unit, onDelete: () -> Unit, onCopy: () -> Unit,
    onSelect: () -> Unit, onCopyLink: () -> Unit, onSave: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var props by remember { mutableStateOf<TdApi.MessageProperties?>(null) }
    var reactions by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(m.id) {
        props = Td.callOrNull(TdApi.GetMessageProperties(m.chatId, m.id))
        val av = Td.callOrNull(TdApi.GetMessageAvailableReactions(m.chatId, m.id, 8))
        reactions = av?.let { (it.topReactions + it.recentReactions + it.popularReactions).mapNotNull { r -> (r.type as? TdApi.ReactionTypeEmoji)?.emoji }.distinct().take(24) }.orEmpty()
    }
    val appear = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, Springs.spatial()) }
    var closing by remember { mutableStateOf(false) }
    fun close(then: () -> Unit = {}) {
        if (closing) return
        closing = true
        scope.launch { appear.animateTo(0f, Springs.exit()); onDismiss(); then() }
    }
    BackHandler { close() }

    val p = props
    val canSend = TdStore.canSend(chat)
    val main = buildList {
        if (canSend && (p == null || p.canBeReplied)) add(MenuItem("Ответить", Icons.AutoMirrored.Filled.Reply) { onReply() })
        if (Texts.caption(m.content)?.text?.isNotEmpty() == true) add(MenuItem("Копировать", Icons.Outlined.ContentCopy) { onCopy() })
        if (p?.canGetLink == true) add(MenuItem("Ссылка на сообщение", Icons.Outlined.Link) { onCopyLink() })
        if (p?.canBeEdited == true && m.content is TdApi.MessageText) add(MenuItem("Изменить", Icons.Outlined.Edit) { onEdit() })
        if (p?.canBePinned == true) add(MenuItem(if (m.isPinned) "Открепить" else "Закрепить", Icons.Outlined.PushPin) {
            if (m.isPinned) Td.send(TdApi.UnpinChatMessage(m.chatId, m.id)) else Td.send(TdApi.PinChatMessage(m.chatId, m.id, true, false))
        })
        if (p?.canBeForwarded != false) add(MenuItem("Переслать", Icons.Outlined.Shortcut) { onForward() })
        if (p?.canBeForwarded != false && !TdStore.isSaved(chat)) add(MenuItem("В Избранное", Icons.Outlined.BookmarkBorder) { onSave() })
    }
    val extra = listOf(MenuItem("Выбрать", Icons.Outlined.CheckCircle) { onSelect() })
    val danger = if (p == null || p.canBeDeletedOnlyForSelf || p.canBeDeletedForAllUsers) listOf(MenuItem("Удалить", Icons.Outlined.Delete, danger = true) { onDelete() }) else emptyList()
    val out = m.isOutgoing && !m.isChannelPost
    var origin by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var below by remember { mutableStateOf(true) }
    val topInset = WindowInsets.statusBars.getTop(density)
    val bottomInset = WindowInsets.navigationBars.getBottom(density)

    Box(
        Modifier.fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .graphicsLayer { alpha = appear.value.coerceIn(0f, 1f) }
            .background(Color.Black.copy(alpha = 0.22f))
            .pointerInput(Unit) { detectTapGestures { close() } },
    )
    androidx.compose.ui.layout.Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            Column(
                Modifier.width(IntrinsicSize.Max).widthIn(min = 220.dp, max = 300.dp).graphicsLayer {
                    val v = appear.value
                    val sc = 0.82f + 0.18f * v
                    scaleX = sc; scaleY = sc
                    alpha = v.coerceIn(0f, 1f)
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (out) 1f else 0f, if (below) 0f else 1f)
                },
                horizontalAlignment = if (out) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (reactions.isNotEmpty()) Surface(shape = RoundedCornerShape(28.dp), color = cs.surfaceContainerHigh, shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.lazy.LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(reactions) { e ->
                            MorphBox(onClick = { react(m, e); close() }, modifier = Modifier.size(44.dp), rest = 22.dp, pressed = 12.dp, color = Color.Transparent, squish = 0.8f) {
                                Text(e, fontSize = 24.sp)
                            }
                        }
                    }
                }
                var index = 0
                listOf(main, extra, danger).filter { it.isNotEmpty() }.forEach { group ->
                    Surface(shape = RoundedCornerShape(20.dp), color = cs.surfaceContainerHigh, shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(vertical = 6.dp)) {
                            group.forEach { item ->
                                val i = index++
                                val enter = remember { androidx.compose.animation.core.Animatable(0f) }
                                LaunchedEffect(Unit) { kotlinx.coroutines.delay(18L * i); enter.animateTo(1f, Springs.spatial()) }
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                        .graphicsLayer { alpha = enter.value.coerceIn(0f, 1f); translationY = (1f - enter.value) * 10.dp.toPx() }
                                        .clickable { close(item.action) }.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val tint = if (item.danger) cs.error else cs.onSurfaceVariant
                                    Icon(item.icon, null, tint = tint, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(16.dp))
                                    Text(item.title, style = MaterialTheme.typography.bodyLarge, color = if (item.danger) cs.error else cs.onSurface, maxLines = 1)
                                }
                            }
                        }
                    }
                }
                Surface(shape = CircleShape, color = cs.surfaceContainerHigh, shadowElevation = 2.dp) {
                    Text(
                        Texts.time(m.date) + if (m.editDate > 0) " · изменено" else "",
                        style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        },
    ) { ms, c ->
        val menu = ms[0].measure(androidx.compose.ui.unit.Constraints(maxWidth = c.maxWidth, maxHeight = c.maxHeight))
        val pad = 12.dp.roundToPx()
        val gap = 8.dp.roundToPx()
        val a = anchor.translate(-origin.x, -origin.y)
        val w = c.maxWidth
        val h = c.maxHeight
        val hasAnchor = anchor != androidx.compose.ui.geometry.Rect.Zero
        val fitsBelow = a.bottom + gap + menu.height <= h - bottomInset - pad
        below = !hasAnchor || fitsBelow || a.top - gap - menu.height < topInset + pad
        val yRaw = when {
            !hasAnchor -> (h - menu.height) / 2
            fitsBelow -> a.bottom.toInt() + gap
            else -> a.top.toInt() - gap - menu.height
        }
        val y = yRaw.coerceIn(topInset + pad, (h - bottomInset - pad - menu.height).coerceAtLeast(topInset + pad))
        val xRaw = if (!hasAnchor) (w - menu.width) / 2 else if (out) a.right.toInt() - menu.width else a.left.toInt()
        val x = xRaw.coerceIn(pad, (w - pad - menu.width).coerceAtLeast(pad))
        layout(w, h) { menu.place(x, y) }
    }
}

@Composable
private fun DeleteDialog(list: List<TdApi.Message>, chat: TdApi.Chat, onDone: () -> Unit) {
    var forAll by remember { mutableStateOf(false) }
    var canAll by remember { mutableStateOf(false) }
    LaunchedEffect(list) {
        canAll = list.all { m -> Td.callOrNull(TdApi.GetMessageProperties(m.chatId, m.id))?.canBeDeletedForAllUsers == true }
        forAll = canAll && chat.type is TdApi.ChatTypePrivate
    }
    val n = list.size
    val other = if (chat.type is TdApi.ChatTypePrivate && !TdStore.isSaved(chat)) "у собеседника" else "у всех участников"
    AlertDialog(
        onDismissRequest = onDone,
        icon = { Icon(Icons.Outlined.Delete, null) },
        title = { Text(if (n == 1) "Удалить сообщение?" else "Удалить $n " + Texts.plural(n, "сообщение", "сообщения", "сообщений") + "?") },
        text = {
            if (canAll) Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { forAll = !forAll }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Checkbox(forAll, { forAll = it })
                Text("Также удалить $other")
            } else Text("Сообщения будут удалены у вас.")
        },
        confirmButton = {
            QTextButton(onClick = { Td.send(TdApi.DeleteMessages(chat.id, list.map { it.id }.toLongArray(), forAll)); onDone() }) {
                Text("Удалить", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { QTextButton(onClick = onDone) { Text("Отмена") } },
    )
}

@Composable
private fun ForwardSheet(list: List<TdApi.Message>, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val chats = TdStore.chatsIn(TdApi.ChatListMain()).filter { TdStore.canSend(it) }.take(60)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text("Переслать в…", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
            items(chats, key = { it.id }) { c ->
                ListItem(
                    headlineContent = { Text(if (TdStore.isSaved(c)) "Избранное" else c.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingContent = { ChatAvatar(c, 44.dp) },
                    modifier = Modifier.clickable {
                        val from = list.first().chatId
                        Td.send(TdApi.ForwardMessages().apply {
                            chatId = c.id; fromChatId = from; messageIds = list.map { it.id }.toLongArray()
                            options = TdApi.MessageSendOptions()
                        })
                        Toast.makeText(ctx, "Переслано в «${if (TdStore.isSaved(c)) "Избранное" else c.title}»", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                )
            }
        }
    }
}
