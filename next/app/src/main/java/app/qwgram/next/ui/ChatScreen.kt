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
import androidx.compose.material3.toShape
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
            is TdApi.UpdateNewMessage -> if (u.message.chatId == chatId && messages.none { it.id == u.message.id }) {
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
    val firstUnreadId = remember(chatId) { if (chat.unreadCount > 0) chat.lastReadInboxMessageId else 0L }

    var text by remember(chatId) { mutableStateOf((chat.draftMessage?.content as? TdApi.DraftMessageContentText)?.text?.text ?: "") }
    var replyTo by remember(chatId) { mutableStateOf<TdApi.Message?>(null) }
    var editing by remember(chatId) { mutableStateOf<TdApi.Message?>(null) }
    var menuFor by remember { mutableStateOf<TdApi.Message?>(null) }
    var forward by remember { mutableStateOf<List<TdApi.Message>?>(null) }
    var deleteFor by remember { mutableStateOf<TdApi.Message?>(null) }
    var viewer by remember { mutableStateOf<TdApi.Photo?>(null) }

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
    // load older history near the top
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (last >= listState.layoutInfo.totalItemsCount - 8) ctl.loadOlder()
        }
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
        if (listState.firstVisibleItemIndex <= 1) listState.animateScrollToItem(0)
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

    val canSend = TdStore.canSend(chat)
    val group = TdStore.isGroup(chat)

    Box(Modifier.fillMaxSize()) {
        ChatWallpaper()
        Column(Modifier.fillMaxSize().imePadding()) {
            ChatTopBar(chat)
            ctl.pinned?.let { p ->
                PinnedBar(p, onClick = {
                    val i = entries.indexOfFirst { it is Entry.Msg && it.m.id == p.id }
                    if (i >= 0) scope.launch { listState.animateScrollToItem(i) }
                }, onClose = { Td.send(TdApi.UnpinChatMessage(chatId, p.id)) })
            }
            Box(Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                ) {
                    items(entries, key = { it.key }, contentType = { it::class }) { e ->
                        when (e) {
                            is Entry.Day -> DayChip(Texts.dayHeader(e.date))
                            Entry.Unread -> UnreadDivider()
                            is Entry.Msg -> MessageItem(
                                m = e.m, grp = e.grp, chat = chat, group = group,
                                reply = ctl.replies[e.m.id],
                                modifier = Modifier.animateItem(),
                                onLong = { haptic(HapticFeedbackType.LongPress); menuFor = e.m },
                                onDouble = {
                                    when (Prefs.doubleTap.value) {
                                        "react" -> react(e.m, Prefs.doubleTapEmoji.value)
                                        "reply" -> if (canSend) { replyTo = e.m; editing = null }
                                    }
                                },
                                onSwipeReply = { if (canSend) { haptic(HapticFeedbackType.TextHandleMove); replyTo = e.m; editing = null } },
                                onPhoto = { viewer = it },
                            )
                        }
                    }
                    if (ctl.loading) item(key = "loading") {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { LoadingIndicator(Modifier.size(36.dp)) }
                    }
                }
                val showDown by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }
                androidx.compose.animation.AnimatedVisibility(
                    showDown, Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    enter = scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(), exit = scaleOut() + fadeOut(),
                ) {
                    Box {
                        SmallFloatingActionButton(onClick = { scope.launch { listState.animateScrollToItem(0) } }, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Icon(Icons.Filled.KeyboardArrowDown, "Вниз")
                        }
                        if (chat.unreadCount > 0) Badge(Modifier.align(Alignment.TopEnd)) { Text(chat.unreadCount.toString()) }
                    }
                }
            }
            if (canSend) {
                Composer(
                    chat = chat, text = text, onText = { text = it; typing(chatId) },
                    replyTo = replyTo, editing = editing,
                    onCancel = { if (editing != null) text = ""; replyTo = null; editing = null },
                    onSend = {
                        val t = text.trim()
                        if (t.isEmpty()) return@Composer
                        val e = editing
                        if (e != null) {
                            Td.send(TdApi.EditMessageText(chatId, e.id, null, inputText(t)))
                            editing = null
                        } else {
                            sendContent(chatId, inputText(t), replyTo)
                            replyTo = null
                        }
                        text = ""
                        haptic(HapticFeedbackType.Confirm)
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                )
            } else ChannelBar(chat)
        }
    }

    menuFor?.let { m ->
        MessageMenu(
            m, chat,
            onDismiss = { menuFor = null },
            onReply = { replyTo = m; editing = null },
            onEdit = {
                editing = m; replyTo = null
                text = (m.content as? TdApi.MessageText)?.text?.text ?: Texts.caption(m.content)?.text.orEmpty()
            },
            onForward = { forward = listOf(m) },
            onDelete = { deleteFor = m },
            onCopy = { copy(ctx, Texts.caption(m.content)?.text.orEmpty()) },
        )
    }
    forward?.let { list -> ForwardSheet(list, onDismiss = { forward = null }) }
    deleteFor?.let { m -> DeleteDialog(m, chat) { deleteFor = null } }
    viewer?.let { p -> PhotoViewer(p) { viewer = null } }
}

fun inputText(t: String): TdApi.InputMessageText = TdApi.InputMessageText().apply {
    text = TdApi.FormattedText(t, arrayOf())
    clearDraft = true
}

fun sendContent(chatId: Long, content: TdApi.InputMessageContent, replyTo: TdApi.Message? = null) {
    Td.send(TdApi.SendMessage().apply {
        this.chatId = chatId
        this.replyTo = replyTo?.let { r -> TdApi.InputMessageReplyToMessage().apply { messageId = r.id } }
        options = TdApi.MessageSendOptions().apply { disableNotification = Prefs.silentByDefault.value }
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

private fun shapeFor2(p: androidx.graphics.shapes.RoundedPolygon) = p.toShape()

@Composable
private fun ChatTopBar(chat: TdApi.Chat) {
    var menu by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val status = Texts.chatStatus(chat)
    val typingNow = !TdStore.actions[chat.id].isNullOrEmpty()
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.statusBarsPadding().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { Nav.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
            Row(
                Modifier.weight(1f).clip(CircleShape).clickable { Nav.push(Route.Profile(chat.id)) }.padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChatAvatar(chat, 40.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(if (TdStore.isSaved(chat)) "Избранное" else chat.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (status.isNotBlank()) Text(
                        status, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (typingNow || status == "в сети") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (chat.type is TdApi.ChatTypePrivate && !TdStore.isSaved(chat) && !TdStore.isBot(chat)) {
                IconButton(onClick = { Toast.makeText(ctx, "Звонки появятся в следующих версиях", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Outlined.Call, "Звонок") }
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Ещё") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    val muted = TdStore.isMuted(chat)
                    DropdownMenuItem(
                        text = { Text(if (muted) "Включить звук" else "Без звука") },
                        leadingIcon = { Icon(if (muted) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff, null) },
                        onClick = { menu = false; setMuted(chat, !muted) },
                    )
                    DropdownMenuItem(text = { Text("Очистить историю") }, leadingIcon = { Icon(Icons.Outlined.DeleteSweep, null) }, onClick = { menu = false; confirmClear = true })
                    DropdownMenuItem(
                        text = { Text("Удалить чат", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; deleteChat(chat); Nav.pop() },
                    )
                }
            }
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        icon = { Icon(Icons.Outlined.DeleteSweep, null) },
        title = { Text("Очистить историю?") },
        text = { Text("Все сообщения в этом чате будут удалены у вас.") },
        confirmButton = { TextButton(onClick = { Td.send(TdApi.DeleteChatHistory(chat.id, false, false)); confirmClear = false }) { Text("Очистить") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Отмена") } },
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
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Открепить") }
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
            FilledTonalButton(onClick = { setMuted(chat, !muted) }, modifier = Modifier.fillMaxWidth(0.7f).height(48.dp)) {
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
    onCancel: () -> Unit,
    onSend: () -> Unit,
) {
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

    Surface(color = if (Prefs.navStyle.value == "floating") cs.surface.copy(alpha = 0f) else cs.surfaceContainer) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp)) {
            AnimatedVisibility(replyTo != null || editing != null, enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                val m = editing ?: replyTo
                Surface(shape = RoundedCornerShape(20.dp), color = cs.surfaceContainerHigh, modifier = Modifier.padding(bottom = 6.dp)) {
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
                        IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, "Отменить") }
                    }
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Surface(shape = RoundedCornerShape(26.dp), color = cs.surfaceContainerHigh, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                    Box {
                        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(horizontal = 4.dp)) {
                            IconButton(onClick = { attach = true }, modifier = Modifier.padding(bottom = 2.dp)) { Icon(Icons.Outlined.AttachFile, "Вложение", tint = cs.onSurfaceVariant) }
                            BasicTextField(
                                value = text, onValueChange = onText,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
                                cursorBrush = SolidColor(cs.primary),
                                maxLines = 8,
                                modifier = Modifier.weight(1f).padding(top = 14.dp, bottom = 14.dp, end = 12.dp),
                                keyboardOptions = if (Prefs.sendByEnter.value) androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send) else androidx.compose.foundation.text.KeyboardOptions.Default,
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { onSend() }),
                                decorationBox = { inner ->
                                    if (text.isEmpty()) Text("Сообщение", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                                    inner()
                                },
                            )
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
                val corner by animateDpAsState(if (ready) 16.dp else 26.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "send")
                val scale by androidx.compose.animation.core.animateFloatAsState(if (recording) 1.35f else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "rec")
                Surface(
                    shape = RoundedCornerShape(corner),
                    color = if (ready || recording) cs.primary else cs.primaryContainer,
                    contentColor = if (ready || recording) cs.onPrimary else cs.onPrimaryContainer,
                    modifier = Modifier.size(52.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale; translationX = dragX.value }
                        .pointerInput(ready) {
                            if (ready) {
                                detectTapGestures(onTap = { onSend() })
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
                    Box(contentAlignment = Alignment.Center) {
                        val motion = MaterialTheme.motionScheme
                        AnimatedContent(
                            targetState = when { editing != null -> 2; ready -> 1; else -> 0 },
                            transitionSpec = { (scaleIn(motion.fastSpatialSpec()) + fadeIn()).togetherWith(scaleOut() + fadeOut()) },
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
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AttachAction(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, shape: androidx.graphics.shapes.RoundedPolygon, tone: Int, onClick: () -> Unit) {
    val (bg, fg) = avatarColors(tone.toLong())
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(12.dp)) {
        Box(Modifier.size(64.dp).clip(shape.toShape()).background(bg), contentAlignment = Alignment.Center) { Icon(icon, null, tint = fg) }
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

@Composable
private fun MessageMenu(
    m: TdApi.Message, chat: TdApi.Chat, onDismiss: () -> Unit,
    onReply: () -> Unit, onEdit: () -> Unit, onForward: () -> Unit, onDelete: () -> Unit, onCopy: () -> Unit,
) {
    var props by remember { mutableStateOf<TdApi.MessageProperties?>(null) }
    var reactions by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(m.id) {
        props = Td.callOrNull(TdApi.GetMessageProperties(m.chatId, m.id))
        val av = Td.callOrNull(TdApi.GetMessageAvailableReactions(m.chatId, m.id, 8))
        reactions = av?.let { (it.topReactions + it.recentReactions + it.popularReactions).mapNotNull { r -> (r.type as? TdApi.ReactionTypeEmoji)?.emoji }.distinct().take(16) }.orEmpty()
    }
    val canSend = TdStore.canSend(chat)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (reactions.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                reactions.forEach { e ->
                    Surface(onClick = { react(m, e); onDismiss() }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Text(e, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(10.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        val p = props
        val row: @Composable (String, androidx.compose.ui.graphics.vector.ImageVector, Boolean, () -> Unit) -> Unit = { t, icon, danger, action ->
            ListItem(
                headlineContent = { Text(t, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) },
                leadingContent = { Icon(icon, null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.clickable { onDismiss(); action() },
            )
        }
        if (canSend && (p == null || p.canBeReplied)) row("Ответить", Icons.AutoMirrored.Filled.Reply, false, onReply)
        if (Texts.caption(m.content)?.text?.isNotEmpty() == true) row("Копировать", Icons.Outlined.ContentCopy, false, onCopy)
        if (p?.canBeForwarded != false) row("Переслать", Icons.Outlined.Shortcut, false, onForward)
        if (p?.canBePinned == true) row(if (m.isPinned) "Открепить" else "Закрепить", Icons.Outlined.PushPin, false) {
            if (m.isPinned) Td.send(TdApi.UnpinChatMessage(m.chatId, m.id)) else Td.send(TdApi.PinChatMessage(m.chatId, m.id, true, false))
        }
        if (p?.canBeEdited == true && Texts.caption(m.content) != null && m.content is TdApi.MessageText) row("Изменить", Icons.Outlined.Edit, false, onEdit)
        if (p == null || p.canBeDeletedOnlyForSelf || p.canBeDeletedForAllUsers) row("Удалить", Icons.Outlined.Delete, true, onDelete)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DeleteDialog(m: TdApi.Message, chat: TdApi.Chat, onDone: () -> Unit) {
    var props by remember { mutableStateOf<TdApi.MessageProperties?>(null) }
    LaunchedEffect(m.id) { props = Td.callOrNull(TdApi.GetMessageProperties(m.chatId, m.id)) }
    val forAll = props?.canBeDeletedForAllUsers == true
    AlertDialog(
        onDismissRequest = onDone,
        icon = { Icon(Icons.Outlined.Delete, null) },
        title = { Text("Удалить сообщение?") },
        text = { Text(if (forAll) "Можно удалить только у себя или у всех участников." else "Сообщение будет удалено у вас.") },
        confirmButton = {
            Row {
                if (forAll) TextButton(onClick = { Td.send(TdApi.DeleteMessages(chat.id, longArrayOf(m.id), true)); onDone() }) {
                    Text("У всех", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = { Td.send(TdApi.DeleteMessages(chat.id, longArrayOf(m.id), false)); onDone() }) { Text(if (forAll) "Только у меня" else "Удалить") }
            }
        },
        dismissButton = { TextButton(onClick = onDone) { Text("Отмена") } },
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
