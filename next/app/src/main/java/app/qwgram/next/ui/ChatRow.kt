package app.qwgram.next.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.qwgram.next.Prefs
import app.qwgram.next.td.Td
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.Texts
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

@Composable
fun CountBadge(n: Int, muted: Boolean, mention: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    val style = Prefs.badgeStyle.value
    if (style == "dot" && !mention) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(if (muted) cs.outline else cs.primary))
        return
    }
    val (bg, fg) = when {
        muted -> cs.outline to cs.surface
        style == "tonal" -> cs.primaryContainer to cs.onPrimaryContainer
        else -> cs.primary to cs.onPrimary
    }
    Box(
        Modifier.heightIn(min = 22.dp).clip(CircleShape).background(bg).padding(horizontal = if (mention) 6.dp else 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (mention) "@" else if (n > 999) "999+" else n.toString(), color = fg, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun ChatRow(chat: TdApi.Chat, list: TdApi.ChatList?, modifier: Modifier = Modifier, onLong: (() -> Unit)? = null, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberHaptic()
    val pinned = list?.let { TdStore.position(chat, it)?.isPinned } == true
    val muted = TdStore.isMuted(chat)
    val last = chat.lastMessage
    val draft = chat.draftMessage?.content as? TdApi.DraftMessageContentText
    val acts = TdStore.actions[chat.id]
    val user = TdStore.users[TdStore.privateUserId(chat)]
    val online = Prefs.showOnlineDot.value && Texts.isOnline(user) && user?.type !is TdApi.UserTypeBot
    val unread = chat.unreadCount > 0 || chat.isMarkedAsUnread
    val cards = Prefs.listStyle.value == "cards"
    val vpad = when (Prefs.density.value) { 0 -> 6.dp; 2 -> 14.dp; else -> 9.dp }
    val avSize = (Prefs.avatarSize.value + when (Prefs.density.value) { 0 -> -8; 2 -> 4; else -> 0 }).dp

    Row(
        modifier
            .fillMaxWidth()
            .then(if (cards) Modifier.padding(horizontal = 8.dp).clip(RoundedCornerShape(16.dp)).background(cs.surfaceContainerLow) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLong?.let { { haptic(HapticFeedbackType.LongPress); it() } })
            .padding(horizontal = 16.dp, vertical = vpad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            ChatAvatar(chat, avSize)
            if (online) Box(
                Modifier.align(Alignment.BottomEnd).size(15.dp).clip(CircleShape).background(cs.surface).padding(2.5.dp)
                    .clip(CircleShape).background(cs.tertiary),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // name takes all free space (icons stick to it), the time keeps its natural width
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (TdStore.isSaved(chat)) "Избранное" else chat.title,
                        style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (user?.verificationStatus?.isVerified == true) {
                        Spacer(Modifier.width(4.dp)); Icon(Icons.Filled.Verified, null, Modifier.size(16.dp), tint = cs.primary)
                    }
                    if (muted) {
                        Spacer(Modifier.width(4.dp)); Icon(Icons.Outlined.VolumeOff, null, Modifier.size(15.dp), tint = cs.outline)
                    }
                }
                Spacer(Modifier.width(8.dp))
                if (last != null && last.isOutgoing && draft == null) {
                    val read = last.id <= chat.lastReadOutboxMessageId
                    Icon(if (read) Icons.Rounded.DoneAll else Icons.Rounded.Check, null, Modifier.size(16.dp), tint = cs.primary)
                    Spacer(Modifier.width(3.dp))
                }
                Text(
                    Texts.listDate(draft?.let { chat.draftMessage?.date } ?: last?.date ?: 0),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (unread && !muted) cs.primary else cs.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val preview = buildAnnotatedString {
                    when {
                        !acts.isNullOrEmpty() -> withStyle(SpanStyle(color = cs.primary)) { append(Texts.chatStatus(chat)) }
                        draft != null && draft.text.text.isNotBlank() -> {
                            withStyle(SpanStyle(color = cs.error)) { append("Черновик: ") }
                            append(draft.text.text.replace('\n', ' '))
                        }
                        last != null -> {
                            val service = Texts.service(last)
                            if (service == null && (TdStore.isGroup(chat) || last.isOutgoing) && !TdStore.isSaved(chat)) {
                                val name = if (last.isOutgoing) "Вы" else TdStore.senderName(last.senderId).substringBefore(' ')
                                if (name.isNotBlank()) withStyle(SpanStyle(color = cs.onSurface)) { append("$name: ") }
                            }
                            append(Texts.preview(last))
                        }
                    }
                }
                Text(
                    preview, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                    maxLines = Prefs.previewLines.value, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (chat.unreadMentionCount > 0) CountBadge(1, muted = false, mention = true)
                    if (unread) CountBadge(if (chat.unreadCount > 0) chat.unreadCount else 1, muted)
                    else if (pinned) Icon(Icons.Rounded.PushPin, null, Modifier.size(18.dp), tint = cs.outline)
                }
            }
        }
    }
    if (!cards && Prefs.separators.value) HorizontalDivider(Modifier.padding(start = 16.dp + avSize + 14.dp), color = cs.outlineVariant.copy(alpha = 0.6f))
}

/** long-press actions for a chat: Telegram's set, as an M3 bottom sheet */
@Composable
fun ChatActionsSheet(chat: TdApi.Chat, onDismiss: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    val archived = chat.positions.any { it.list is TdApi.ChatListArchive }
    val pinned = chat.positions.any { it.isPinned && (it.list is TdApi.ChatListMain || it.list is TdApi.ChatListArchive) }
    val muted = TdStore.isMuted(chat)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            ChatAvatar(chat, 44.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(chat.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(Texts.chatStatus(chat), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val row: @Composable (String, ImageVector, Boolean, () -> Unit) -> Unit = { title, icon, danger, action ->
            ListItem(
                headlineContent = { Text(title, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) },
                leadingContent = { Icon(icon, null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.combinedClickable(onClick = action),
            )
        }
        val list: TdApi.ChatList = if (archived) TdApi.ChatListArchive() else TdApi.ChatListMain()
        row(if (pinned) "Открепить" else "Закрепить", Icons.Outlined.PushPin, false) {
            Td.send(TdApi.ToggleChatIsPinned(list, chat.id, !pinned)); onDismiss()
        }
        row(if (muted) "Включить уведомления" else "Без звука", if (muted) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff, false) {
            setMuted(chat, !muted); onDismiss()
        }
        if (chat.unreadCount > 0 || chat.isMarkedAsUnread) row("Отметить прочитанным", Icons.Outlined.DoneAll, false) {
            markRead(chat); onDismiss()
        } else row("Отметить непрочитанным", Icons.Outlined.MarkChatUnread, false) {
            Td.send(TdApi.ToggleChatIsMarkedAsUnread(chat.id, true)); onDismiss()
        }
        if (!TdStore.isSaved(chat)) row(if (archived) "Вернуть из архива" else "В архив", if (archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, false) {
            Td.send(TdApi.AddChatToList(chat.id, if (archived) TdApi.ChatListMain() else TdApi.ChatListArchive())); onDismiss()
        }
        row("Удалить чат", Icons.Outlined.Delete, true) { confirmDelete = true }
        Spacer(Modifier.size(24.dp))
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Outlined.Delete, null) },
            title = { Text("Удалить чат?") },
            text = { Text("«${chat.title}» исчезнет из списка чатов. Историю нельзя будет вернуть.") },
            confirmButton = {
                QTextButton(onClick = {
                    deleteChat(chat); confirmDelete = false; onDismiss()
                }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { QTextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    }
}

fun setMuted(chat: TdApi.Chat, mute: Boolean) {
    val s = chat.notificationSettings
    val n = TdApi.ChatNotificationSettings().apply {
        useDefaultMuteFor = false
        muteFor = if (mute) Int.MAX_VALUE else 0
        useDefaultSound = s.useDefaultSound; soundId = s.soundId
        useDefaultShowPreview = s.useDefaultShowPreview; showPreview = s.showPreview
        useDefaultMuteStories = s.useDefaultMuteStories; muteStories = s.muteStories
        useDefaultStorySound = s.useDefaultStorySound; storySoundId = s.storySoundId
        useDefaultShowStoryPoster = s.useDefaultShowStoryPoster; showStoryPoster = s.showStoryPoster
        useDefaultDisablePinnedMessageNotifications = s.useDefaultDisablePinnedMessageNotifications
        disablePinnedMessageNotifications = s.disablePinnedMessageNotifications
        useDefaultDisableMentionNotifications = s.useDefaultDisableMentionNotifications
        disableMentionNotifications = s.disableMentionNotifications
    }
    Td.send(TdApi.SetChatNotificationSettings(chat.id, n))
}

fun markRead(chat: TdApi.Chat) {
    chat.lastMessage?.let { Td.send(TdApi.ViewMessages(chat.id, longArrayOf(it.id), TdApi.MessageSourceChatList(), true)) }
    if (chat.isMarkedAsUnread) Td.send(TdApi.ToggleChatIsMarkedAsUnread(chat.id, false))
    if (chat.unreadMentionCount > 0) Td.send(TdApi.ReadAllChatMentions(chat.id))
}

fun deleteChat(chat: TdApi.Chat) {
    when (val t = chat.type) {
        is TdApi.ChatTypeSupergroup, is TdApi.ChatTypeBasicGroup -> {
            Td.send(TdApi.LeaveChat(chat.id))
            Td.send(TdApi.DeleteChatHistory(chat.id, true, false))
        }
        else -> Td.send(TdApi.DeleteChatHistory(chat.id, true, t is TdApi.ChatTypePrivate && chat.canBeDeletedForAllUsers))
    }
}

/**
 * Telegram-style swipe actions on a chat row: swipe left → archive / unarchive, swipe right → read / unread.
 * The coloured layer and icon grow with the drag; release past 30% to act, the row springs back.
 */
@Composable
fun SwipeChatRow(chat: TdApi.Chat, list: TdApi.ChatList?, enabled: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (!enabled) { Box(modifier) { content() }; return }
    val cs = MaterialTheme.colorScheme
    val dx = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val haptic = rememberHaptic()
    var widthPx by remember { mutableStateOf(1f) }
    var armed by remember { mutableStateOf(false) }
    val archived = list is TdApi.ChatListArchive
    val unread = chat.unreadCount > 0 || chat.isMarkedAsUnread
    Box(
        modifier
            .fillMaxWidth()
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(chat.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (armed) {
                            if (dx.value < 0) Td.send(TdApi.AddChatToList(chat.id, if (archived) TdApi.ChatListMain() else TdApi.ChatListArchive()))
                            else if (unread) markRead(chat) else Td.send(TdApi.ToggleChatIsMarkedAsUnread(chat.id, true))
                        }
                        armed = false
                        scope.launch { dx.animateTo(0f, Springs.fastSpatial()) }
                    },
                    onDragCancel = { armed = false; scope.launch { dx.animateTo(0f, Springs.fastSpatial()) } },
                ) { change, delta ->
                    change.consume()
                    val v = (dx.value + delta).coerceIn(-widthPx * 0.6f, widthPx * 0.6f)
                    scope.launch { dx.snapTo(v) }
                    val now = kotlin.math.abs(v) > widthPx * 0.3f
                    if (now != armed) { armed = now; if (now) haptic(HapticFeedbackType.TextHandleMove) }
                }
            },
    ) {
        val v = dx.value
        if (v != 0f) {
            val left = v < 0
            val bg = if (left) cs.tertiaryContainer else cs.primaryContainer
            val fg = if (left) cs.onTertiaryContainer else cs.onPrimaryContainer
            val icon = when {
                left && archived -> Icons.Outlined.Unarchive
                left -> Icons.Outlined.Archive
                unread -> Icons.Outlined.DoneAll
                else -> Icons.Outlined.MarkChatUnread
            }
            val p = (kotlin.math.abs(v) / (widthPx * 0.3f)).coerceIn(0f, 1f)
            Box(
                Modifier.matchParentSize().background(bg.copy(alpha = 0.4f + 0.6f * p)).padding(horizontal = 28.dp),
                contentAlignment = if (left) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                val sc by androidx.compose.animation.core.animateFloatAsState(if (armed) 1.25f else 0.8f + 0.2f * p, Springs.fastSpatial(), label = "ic")
                Icon(icon, null, tint = fg, modifier = Modifier.graphicsLayer { scaleX = sc; scaleY = sc })
            }
        }
        Box(Modifier.graphicsLayer { translationX = dx.value }.background(cs.surface)) { content() }
    }
}
