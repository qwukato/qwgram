package app.qwgram.next.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardHide
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.qwgram.next.Prefs
import app.qwgram.next.td.Td
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.Texts
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

// =====================================================================================================================
//  Opening chats & Telegram links inside the app
// =====================================================================================================================

/** opens a chat the way Telegram does: forums show their topics first */
fun openChat(chat: TdApi.Chat) {
    if (TdStore.isForum(chat)) Nav.push(Route.Page("topics:${chat.id}")) else Nav.push(Route.Chat(chat.id))
}

/** the topic of the forum chat screen on top (messages sent from it go there) */
fun currentTopic(chatId: Long): TdApi.MessageTopic? {
    val r = (Nav.top as? Route.Chat)?.takeIf { it.chatId == chatId } ?: return null
    return when {
        r.topic != 0 -> TdApi.MessageTopicForum(r.topic)
        r.thread != 0L -> TdApi.MessageTopicThread(r.thread)
        else -> null
    }
}

/** opens the comments of a channel post (its thread in the discussion group) */
fun openComments(ctx: Context, m: TdApi.Message) {
    Td.send(TdApi.GetMessageThread(m.chatId, m.id)) { r ->
        if (r is TdApi.MessageThreadInfo) Td.send(TdApi.GetChat(r.chatId)) { c ->
            if (c is TdApi.Chat) { TdStore.onUpdate(TdApi.UpdateNewChat(c)); Nav.push(Route.Chat(r.chatId, thread = r.messageThreadId)) }
        } else Toast.makeText(ctx, "Комментарии недоступны", Toast.LENGTH_SHORT).show()
    }
}

/** "N комментариев" under a channel post: who commented, then into the discussion */
@Composable
fun CommentsRow(m: TdApi.Message, ri: TdApi.MessageReplyInfo, fg: Color) {
    val ctx = LocalContext.current
    HorizontalDivider(color = fg.copy(alpha = 0.12f), modifier = Modifier.padding(top = 4.dp))
    Row(
        Modifier.fillMaxWidth().clickable { openComments(ctx, m) }.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (ri.recentReplierIds.isNotEmpty()) {
            Box(Modifier.width((20 + 14 * (ri.recentReplierIds.size.coerceAtMost(3) - 1)).dp).height(22.dp)) {
                ri.recentReplierIds.take(3).forEachIndexed { i, s -> SenderAvatar(s, 22.dp, Modifier.padding(start = (14 * i).dp)) }
            }
            Spacer(Modifier.width(8.dp))
        } else {
            Icon(Icons.AutoMirrored.Outlined.Chat, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
        }
        val n = ri.replyCount
        Text(
            if (n == 0) "Прокомментировать" else "$n " + Texts.plural(n, "комментарий", "комментария", "комментариев"),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f),
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
    }
}

object Links {
    /** a link that came from outside (another app) before the client was ready */
    var pending by mutableStateOf<String?>(null)
    var invite by mutableStateOf<Pair<String, TdApi.ChatInviteLinkInfo>?>(null)
    var proxy by mutableStateOf<TdApi.Proxy?>(null)
    var folder by mutableStateOf<Pair<String, TdApi.ChatFolderInviteLinkInfo>?>(null)

    private val tme = Regex("^(?:https?://)?(?:www\\.)?(?:t\\.me|telegram\\.me|telegram\\.dog)/", RegexOption.IGNORE_CASE)

    fun isTelegram(url: String): Boolean = url.startsWith("tg:", ignoreCase = true) || tme.containsMatchIn(url)

    private fun toast(ctx: Context, s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()

    /** a Telegram link opened in the app; anything the app can't show goes to the browser (never back to us) */
    fun open(ctx: Context, url: String) {
        Td.send(TdApi.GetInternalLinkType(url)) { r ->
            if (r is TdApi.InternalLinkType) handle(ctx, r, url) else browser(ctx, url)
        }
    }

    fun browser(ctx: Context, url: String) {
        if (url.startsWith("tg:", ignoreCase = true)) { toast(ctx, "Эта ссылка пока не поддерживается"); return }
        val uri = Uri.parse(if (url.contains("://")) url else "https://$url")
        // a browser explicitly: t.me links would otherwise come straight back to qwgram
        val i = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (isTelegram(url)) i.selector = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
        try { ctx.startActivity(i) } catch (e: ActivityNotFoundException) {
            try { ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e2: Exception) { toast(ctx, "Нет приложения для ссылки") }
        }
    }

    private fun publicChat(ctx: Context, name: String, then: (TdApi.Chat) -> Unit) {
        Td.send(TdApi.SearchPublicChat(name.removePrefix("@"))) { c ->
            if (c is TdApi.Chat) { TdStore.onUpdate(TdApi.UpdateNewChat(c)); then(c) } else toast(ctx, "Не найдено: @$name")
        }
    }

    private fun handle(ctx: Context, t: TdApi.InternalLinkType, url: String) {
        when (t) {
            is TdApi.InternalLinkTypePublicChat -> publicChat(ctx, t.chatUsername) { c ->
                if (t.openProfile) Nav.push(Route.Profile(c.id)) else openChat(c)
            }
            is TdApi.InternalLinkTypeChatInvite -> Td.send(TdApi.CheckChatInviteLink(t.inviteLink)) { i ->
                when {
                    i !is TdApi.ChatInviteLinkInfo -> toast(ctx, "Ссылка-приглашение недействительна")
                    // already a member (or the chat is open for preview): straight in
                    i.chatId != 0L && i.accessibleFor == 0 -> Td.send(TdApi.GetChat(i.chatId)) { c -> if (c is TdApi.Chat) { TdStore.onUpdate(TdApi.UpdateNewChat(c)); openChat(c) } }
                    else -> invite = t.inviteLink to i
                }
            }
            is TdApi.InternalLinkTypeMessage -> Td.send(TdApi.GetMessageLinkInfo(t.url)) { li ->
                if (li is TdApi.MessageLinkInfo && li.chatId != 0L) {
                    li.message?.let { Nav.jumpTo = li.chatId to it.id }
                    val topic = (li.topicId as? TdApi.MessageTopicForum)?.forumTopicId ?: 0
                    Nav.push(Route.Chat(li.chatId, topic))
                } else toast(ctx, "Сообщение недоступно")
            }
            is TdApi.InternalLinkTypeBotStart -> publicChat(ctx, t.botUsername) { c ->
                val uid = TdStore.privateUserId(c)
                if (uid != 0L && t.startParameter.isNotEmpty()) Td.send(TdApi.SendBotStartMessage(uid, c.id, t.startParameter))
                Nav.push(Route.Chat(c.id))
            }
            is TdApi.InternalLinkTypeStickerSet -> StickerSetPreview.openName(t.stickerSetName)
            is TdApi.InternalLinkTypeProxy -> proxy = t.proxy
            is TdApi.InternalLinkTypeChatFolderInvite -> Td.send(TdApi.CheckChatFolderInviteLink(t.inviteLink)) { i ->
                if (i is TdApi.ChatFolderInviteLinkInfo) folder = t.inviteLink to i else toast(ctx, "Ссылка на папку недействительна")
            }
            is TdApi.InternalLinkTypeUserPhoneNumber -> Td.send(TdApi.SearchUserByPhoneNumber(t.phoneNumber, false)) { u ->
                if (u is TdApi.User) Td.send(TdApi.CreatePrivateChat(u.id, false)) { c -> if (c is TdApi.Chat) Nav.push(if (t.openProfile) Route.Profile(c.id) else Route.Chat(c.id)) }
                else toast(ctx, "Пользователь не найден")
            }
            is TdApi.InternalLinkTypeSavedMessages -> TdStore.me?.let { me -> Td.send(TdApi.CreatePrivateChat(me.id, false)) { c -> if (c is TdApi.Chat) Nav.push(Route.Chat(c.id)) } }
            is TdApi.InternalLinkTypeSettings -> Nav.push(Route.Page("features"))
            is TdApi.InternalLinkTypeSearch -> Nav.push(Route.Search)
            is TdApi.InternalLinkTypeNewGroupChat, is TdApi.InternalLinkTypeNewChannelChat, is TdApi.InternalLinkTypeNewPrivateChat -> Nav.push(Route.Page("compose"))
            is TdApi.InternalLinkTypeVideoChat, is TdApi.InternalLinkTypeGroupCall -> toast(ctx, "Звонки и трансляции пока не поддерживаются")
            is TdApi.InternalLinkTypeAuthenticationCode -> toast(ctx, "Код входа: ${t.code}")
            else -> browser(ctx, url)
        }
    }
}

// =====================================================================================================================
//  Message & chat tools that open over everything (translation, report, viewers, auto-delete, member actions)
// =====================================================================================================================

object Tg {
    var translate by mutableStateOf<TdApi.FormattedText?>(null)
    var report by mutableStateOf<Pair<Long, LongArray>?>(null)
    var viewers by mutableStateOf<TdApi.Message?>(null)
    var autoDelete by mutableStateOf<TdApi.Chat?>(null)
    var member by mutableStateOf<Pair<TdApi.Chat, TdApi.ChatMember>?>(null)
}

/** text → Telegram formatting: **bold**, __italic__, ~~strike~~, ||spoiler||, `code`, ```code block```, [text](link) */
fun parseFormatting(t: String, extra: Array<TdApi.TextEntity> = arrayOf()): TdApi.FormattedText {
    // premium emoji go in as entities; markdown parsing keeps them and fixes their offsets
    val plain = TdApi.FormattedText(t, extra)
    if (!Prefs.markdown.value) return plain
    return runCatching { Client.execute(TdApi.ParseMarkdown(plain)) as? TdApi.FormattedText }.getOrNull() ?: plain
}

/** the reverse, for editing a sent message without losing its formatting */
fun toMarkdown(f: TdApi.FormattedText): String {
    if (!Prefs.markdown.value || f.entities.isEmpty()) return f.text
    return runCatching { (Client.execute(TdApi.GetMarkdownText(f)) as? TdApi.FormattedText)?.text }.getOrNull() ?: f.text
}

@Composable
fun TgHost() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    // a link handed over by another app
    val p = Links.pending
    LaunchedEffect(p) { if (p != null) { Links.pending = null; delay(300); Links.open(ctx, p) } }

    Links.invite?.let { (link, info) -> InviteDialog(link, info) { Links.invite = null } }
    Links.proxy?.let { px ->
        AlertDialog(
            onDismissRequest = { Links.proxy = null },
            title = { Text("Подключить прокси?") },
            text = { Text("${px.server}:${px.port}\n" + when (px.type) { is TdApi.ProxyTypeMtproto -> "MTProto"; is TdApi.ProxyTypeSocks5 -> "SOCKS5"; else -> "HTTP" }) },
            confirmButton = { QTextButton(onClick = { Td.send(TdApi.AddProxy(px, true, "")); Links.proxy = null; Toast.makeText(ctx, "Прокси включён", Toast.LENGTH_SHORT).show() }) { Text("Подключить") } },
            dismissButton = { QTextButton(onClick = { Links.proxy = null }) { Text("Отмена") } },
        )
    }
    Tg.translate?.let { TranslateDialog(it) { Tg.translate = null } }
    Tg.report?.let { (chatId, ids) -> ReportFlow(chatId, ids) { Tg.report = null } }
    Tg.viewers?.let { ViewersSheet(it) { Tg.viewers = null } }
    Tg.autoDelete?.let { c ->
        val opts = listOf(0 to "Выключено", 86400 to "1 день", 604800 to "1 неделя", 2678400 to "1 месяц")
        RadioDialog("Автоудаление сообщений", opts, c.messageAutoDeleteTime, onPick = { v ->
            Tg.autoDelete = null
            Td.send(TdApi.SetChatMessageAutoDeleteTime(c.id, v)) { r ->
                Toast.makeText(ctx, if (r is TdApi.Error) "Не удалось: ${r.message}" else if (v == 0) "Автоудаление выключено" else "Новые сообщения будут удаляться", Toast.LENGTH_SHORT).show()
            }
        }) { Tg.autoDelete = null }
    }
    Tg.member?.let { (chat, mem) -> MemberActions(chat, mem) { Tg.member = null } }
    StoryViewerHost()
    StoryComposerHost()
    Links.folder?.let { (link, info) -> FolderInviteDialog(link, info) { Links.folder = null } }
}

@Composable
private fun InviteDialog(link: String, info: TdApi.ChatInviteLinkInfo, onDone: () -> Unit) {
    val ctx = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    val channel = info.type is TdApi.InviteLinkChatTypeChannel
    QSheet(onDismiss = onDone, wrap = true) { sheet ->
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(info.title, info.chatId.takeIf { it != 0L } ?: info.title.hashCode().toLong(), info.photo?.small, info.photo?.minithumbnail, 88.dp)
            Spacer(Modifier.height(14.dp))
            Text(info.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                (if (channel) "Канал" else "Группа") + " · " + info.memberCount + " " + Texts.plural(info.memberCount, if (channel) "подписчик" else "участник", if (channel) "подписчика" else "участника", if (channel) "подписчиков" else "участников"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (info.description.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(info.description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, maxLines = 6, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                enabled = !busy,
                onClick = {
                    busy = true
                    Td.send(TdApi.JoinChatByInviteLink(link)) { r ->
                        busy = false
                        when (r) {
                            is TdApi.ChatJoinResultSuccess -> { sheet.close(); Td.send(TdApi.GetChat(r.chatId)) { c -> if (c is TdApi.Chat) { TdStore.onUpdate(TdApi.UpdateNewChat(c)); openChat(c) } } }
                            is TdApi.ChatJoinResultRequestSent -> { sheet.close(); Toast.makeText(ctx, "Заявка отправлена администраторам", Toast.LENGTH_SHORT).show() }
                            is TdApi.Error -> Toast.makeText(ctx, "Не удалось вступить: ${r.message}", Toast.LENGTH_SHORT).show()
                            else -> { sheet.close(); Toast.makeText(ctx, "Заявка отклонена", Toast.LENGTH_SHORT).show() }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(if (info.createsJoinRequest) "Подать заявку" else if (channel) "Подписаться" else "Вступить в группу") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TranslateDialog(text: TdApi.FormattedText, onDone: () -> Unit) {
    val ctx = LocalContext.current
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val lang = remember { java.util.Locale.getDefault().language.ifBlank { "ru" } }
    LaunchedEffect(text) {
        try { result = Td.call(TdApi.TranslateText(text, lang, "")).text }
        catch (e: Exception) { error = "Перевод недоступен" }
    }
    QSheet(onDismiss = onDone, wrap = true) { sheet ->
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Text("Перевод", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().heightIn(min = 64.dp, max = 420.dp)) {
                when {
                    error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                    result == null -> LoadingIndicator(Modifier.align(Alignment.Center))
                    else -> SelectionContainer { Text(result!!, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.verticalScroll(rememberScrollState())) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Оригинал", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(text.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { result?.let { copyText(ctx, it) } }, enabled = result != null, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Копировать")
                }
                Button(onClick = { sheet.close() }, modifier = Modifier.weight(1f)) { Text("Готово") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Telegram's report flow: the server sends the reasons step by step (choose a reason → maybe a comment) */
@Composable
private fun ReportFlow(chatId: Long, ids: LongArray, onDone: () -> Unit) {
    val ctx = LocalContext.current
    var step by remember { mutableStateOf<TdApi.ReportChatResult?>(null) }
    var option by remember { mutableStateOf(ByteArray(0)) }
    var comment by remember { mutableStateOf("") }
    fun send(opt: ByteArray, txt: String) {
        option = opt
        Td.send(TdApi.ReportChat(chatId, opt, ids, txt)) { r ->
            when (r) {
                is TdApi.ReportChatResultOk -> { onDone(); Toast.makeText(ctx, "Жалоба отправлена. Спасибо!", Toast.LENGTH_SHORT).show() }
                is TdApi.ReportChatResult -> step = r
                is TdApi.Error -> { onDone(); Toast.makeText(ctx, "Не удалось: ${r.message}", Toast.LENGTH_SHORT).show() }
                else -> onDone()
            }
        }
    }
    LaunchedEffect(Unit) { send(ByteArray(0), "") }
    when (val s = step) {
        is TdApi.ReportChatResultOptionRequired -> QSheet(onDismiss = onDone, wrap = true) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(s.title.ifBlank { "Пожаловаться" }, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
                s.options.forEachIndexed { i, o ->
                    Box(Modifier.segment(i, s.options.size).clickable { send(o.id, "") }.padding(horizontal = 16.dp, vertical = 16.dp)) {
                        Text(o.text, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (i < s.options.lastIndex) Spacer(Modifier.height(2.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        is TdApi.ReportChatResultTextRequired -> AlertDialog(
            onDismissRequest = onDone,
            title = { Text("Подробности") },
            text = { OutlinedTextField(comment, { comment = it }, placeholder = { Text(if (s.isOptional) "Комментарий (необязательно)" else "Опишите проблему") }, minLines = 3) },
            confirmButton = { QTextButton(enabled = s.isOptional || comment.isNotBlank(), onClick = { send(s.optionId, comment.trim()) }) { Text("Отправить") } },
            dismissButton = { QTextButton(onClick = onDone) { Text("Отмена") } },
        )
        is TdApi.ReportChatResultMessagesRequired -> LaunchedEffect(s) { onDone(); Toast.makeText(ctx, "Выберите сообщения и пожалуйтесь на них", Toast.LENGTH_SHORT).show() }
        else -> Unit
    }
}

/** who read the message (small groups) and who reacted, with the reaction */
@Composable
private fun ViewersSheet(m: TdApi.Message, onDone: () -> Unit) {
    var viewers by remember { mutableStateOf<List<TdApi.MessageViewer>?>(null) }
    var reactions by remember { mutableStateOf<List<TdApi.AddedReaction>>(emptyList()) }
    LaunchedEffect(m.id) {
        if (m.interactionInfo?.reactions?.canGetAddedReactions == true) {
            reactions = Td.callOrNull(TdApi.GetMessageAddedReactions(m.chatId, m.id, null, "", 100))?.reactions?.toList().orEmpty()
        }
        viewers = Td.callOrNull(TdApi.GetMessageViewers(m.chatId, m.id))?.viewers?.toList().orEmpty()
    }
    QSheet(onDismiss = onDone, wrap = true) { sheet ->
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            val v = viewers
            Text(
                when {
                    v == null -> "Просмотры"
                    v.isEmpty() && reactions.isNotEmpty() -> "Реакции · ${reactions.size}"
                    else -> "Прочитали · ${v.size}" + if (reactions.isNotEmpty()) " · реакций ${reactions.size}" else ""
                },
                style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
            )
            if (v == null) Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
            else {
                // everyone who reacted or read, once; the reaction next to the name
                val ids = (reactions.map { TdStore.senderId(it.senderId) } + v.map { it.userId }).distinct()
                if (ids.isEmpty()) Text("Пока никто не прочитал", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
                LazyColumn(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    itemsIndexed(ids) { i, id ->
                        val r = reactions.firstOrNull { TdStore.senderId(it.senderId) == id }
                        val seen = v.firstOrNull { it.userId == id }
                        Row(
                            Modifier.segment(i, ids.size).clickable {
                                sheet.close()
                                Td.send(TdApi.CreatePrivateChat(id, false)) { c -> if (c is TdApi.Chat) Nav.push(Route.Profile(c.id)) }
                            }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            UserAvatar(TdStore.users[id], 40.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(TdStore.userName(id).ifBlank { "Пользователь" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (seen != null) Text("прочитано " + Texts.time(seen.viewDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (r != null) ReactionGlyph(r.type, 24.dp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MemberActions(chat: TdApi.Chat, mem: TdApi.ChatMember, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val name = TdStore.senderName(mem.memberId).ifBlank { "Участник" }
    val uid = (mem.memberId as? TdApi.MessageSenderUser)?.userId
    var confirm by remember { mutableStateOf<String?>(null) }
    QSheet(onDismiss = onDone, wrap = true) { sheet ->
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(Modifier.padding(start = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                SenderAvatar(mem.memberId, 48.dp)
                Spacer(Modifier.width(14.dp))
                Text(name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val rows = buildList {
                if (uid != null) {
                    add(Triple("Профиль", Icons.Outlined.Person) { sheet.close(); Td.send(TdApi.CreatePrivateChat(uid, false)) { c -> if (c is TdApi.Chat) Nav.push(Route.Profile(c.id)) } })
                    add(Triple("Написать", Icons.AutoMirrored.Outlined.Chat) { sheet.close(); Td.send(TdApi.CreatePrivateChat(uid, false)) { c -> if (c is TdApi.Chat) Nav.push(Route.Chat(c.id)) } })
                }
                val canModerate = TdStore.isAdmin(chat) && mem.status !is TdApi.ChatMemberStatusCreator && uid != TdStore.me?.id
                if (canModerate && uid != null) add(Triple(if (mem.status is TdApi.ChatMemberStatusAdministrator) "Права администратора" else "Назначить администратором", Icons.Outlined.Shield) {
                    sheet.close(); Nav.push(Route.Page("admin:${chat.id}:$uid"))
                })
                if (canModerate) {
                    add(Triple(if (chat.type is TdApi.ChatTypeSupergroup && TdStore.isChannel(chat)) "Удалить из канала" else "Удалить из группы", Icons.Outlined.PersonRemove) { confirm = "kick" })
                    if (chat.type is TdApi.ChatTypeSupergroup) add(Triple("Заблокировать", Icons.Outlined.Block) { confirm = "ban" })
                }
            }
            rows.forEachIndexed { i, (title, icon, act) ->
                val danger = icon == Icons.Outlined.PersonRemove || icon == Icons.Outlined.Block
                Box(Modifier.segment(i, rows.size)) { BaseRow(title, icon = icon, danger = danger, onClick = act) }
                if (i < rows.lastIndex) Spacer(Modifier.height(2.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    confirm?.let { what ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (what == "ban") "Заблокировать $name?" else "Удалить $name?") },
            text = { Text(if (what == "ban") "Участник не сможет вернуться по ссылке, его сообщения будут удалены." else "Участник сможет вернуться по ссылке-приглашению.") },
            confirmButton = {
                QTextButton(onClick = {
                    val f: TdApi.Function<TdApi.Ok> = if (what == "ban") TdApi.BanChatMember(chat.id, mem.memberId, 0, true)
                        else TdApi.SetChatMemberStatus(chat.id, mem.memberId, TdApi.ChatMemberStatusLeft())
                    Td.send(f) { r -> Toast.makeText(ctx, if (r is TdApi.Error) "Не удалось: ${r.message}" else "Готово", Toast.LENGTH_SHORT).show() }
                    confirm = null; onDone()
                }) { Text(if (what == "ban") "Заблокировать" else "Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { QTextButton(onClick = { confirm = null }) { Text("Отмена") } },
        )
    }
}

// =====================================================================================================================
//  Join bar, bot start, mentions / commands autocomplete, bot keyboards
// =====================================================================================================================

/** a channel/group you are not in: join (or ask to join) */
@Composable
fun JoinBar(chat: TdApi.Chat) {
    val ctx = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    val channel = TdStore.isChannel(chat)
    Box(Modifier.fillMaxWidth().navigationBarsPaddingCompat().padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 4.dp)) {
        Button(
            enabled = !busy,
            onClick = {
                busy = true
                Td.send(TdApi.JoinChat(chat.id)) { r ->
                    busy = false
                    when (r) {
                        is TdApi.ChatJoinResultRequestSent -> Toast.makeText(ctx, "Заявка отправлена администраторам", Toast.LENGTH_SHORT).show()
                        is TdApi.Error -> Toast.makeText(ctx, "Не удалось: ${r.message}", Toast.LENGTH_SHORT).show()
                        else -> Unit
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(if (channel) "Подписаться" else "Вступить в группу", style = MaterialTheme.typography.titleSmall) }
    }
}

private fun Modifier.navigationBarsPaddingCompat(): Modifier = this.navigationBarsPadding()

/** a bot you never talked to: Telegram's big START button */
@Composable
fun BotStartBar(chat: TdApi.Chat) {
    Box(Modifier.fillMaxWidth().navigationBarsPaddingCompat().padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 4.dp)) {
        Button(
            onClick = { Td.send(TdApi.SendBotStartMessage(TdStore.privateUserId(chat), chat.id, "")) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("Запустить", style = MaterialTheme.typography.titleSmall) }
    }
}

private class Suggestion(val title: String, val sub: String, val insert: String, val send: Boolean, val user: TdApi.User?)

/**
 * Suggestions above the input: "@name" → members of the group with a username, "/" → the bots' commands.
 * Picking a command sends it right away (Telegram); picking a member completes the mention.
 */
@Composable
fun ComposerSuggestions(chat: TdApi.Chat, text: String, onText: (String) -> Unit, onSendCommand: (String) -> Unit) {
    val group = TdStore.isGroup(chat)
    val bot = TdStore.isBot(chat)
    var commands by remember(chat.id) { mutableStateOf<List<Pair<String, TdApi.BotCommand>>>(emptyList()) }
    LaunchedEffect(chat.id) {
        commands = when (val t = chat.type) {
            is TdApi.ChatTypePrivate -> if (bot) Td.callOrNull(TdApi.GetUserFullInfo(t.userId))?.botInfo?.commands?.map { "" to it }.orEmpty() else emptyList()
            is TdApi.ChatTypeSupergroup -> if (t.isChannel) emptyList() else Td.callOrNull(TdApi.GetSupergroupFullInfo(t.supergroupId))?.botCommands?.let { flat(it) }.orEmpty()
            is TdApi.ChatTypeBasicGroup -> Td.callOrNull(TdApi.GetBasicGroupFullInfo(t.basicGroupId))?.botCommands?.let { flat(it) }.orEmpty()
            else -> emptyList()
        }
    }
    val cmd = Regex("^/([A-Za-z0-9_]*)$").find(text)?.groupValues?.get(1)
    val mention = if (group) Regex("(?:^|\\s)@([A-Za-z0-9_]*)$").find(text)?.groupValues?.get(1) else null
    var users by remember { mutableStateOf<List<TdApi.User>>(emptyList()) }
    LaunchedEffect(mention) {
        if (mention == null) { users = emptyList(); return@LaunchedEffect }
        delay(150)
        val found = Td.callOrNull(TdApi.SearchChatMembers(chat.id, mention, 30, TdApi.ChatMembersFilterMention(currentTopic(chat.id))))?.members.orEmpty()
        users = found.mapNotNull { (it.memberId as? TdApi.MessageSenderUser)?.userId?.let { id -> TdStore.users[id] } }
            .filter { it.usernames?.activeUsernames?.isNotEmpty() == true && it.id != TdStore.me?.id }
    }
    val list: List<Suggestion> = when {
        cmd != null -> commands.filter { it.second.command.startsWith(cmd, ignoreCase = true) }.take(40).map { (botName, c) ->
            val full = "/" + c.command + if (botName.isNotEmpty() && commands.map { it.first }.distinct().size > 1) "@$botName" else ""
            Suggestion(full, c.description, full, true, null)
        }
        mention != null -> users.take(30).map { u ->
            val un = u.usernames!!.activeUsernames[0]
            Suggestion(TdStore.fullName(u), "@$un", text.dropLast(mention.length + 1) + "@$un ", false, u)
        }
        else -> emptyList()
    }
    AnimatedVisibility(list.isNotEmpty(), enter = expandVertically(Springs.smooth()) + fadeIn(Springs.effects()), exit = shrinkVertically(Springs.exit()) + fadeOut(Springs.exit())) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 3.dp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 232.dp)) {
                items(list) { s ->
                    Row(
                        Modifier.fillMaxWidth().clickable { if (s.send) onSendCommand(s.insert) else onText(s.insert) }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (s.user != null) { UserAvatar(s.user, 36.dp); Spacer(Modifier.width(12.dp)) }
                        Column(Modifier.weight(1f)) {
                            Text(s.title, style = MaterialTheme.typography.bodyLarge, fontWeight = if (s.send) FontWeight.Medium else null, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (s.sub.isNotBlank()) Text(s.sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

private fun flat(all: Array<TdApi.BotCommands>): List<Pair<String, TdApi.BotCommand>> =
    all.flatMap { b -> val n = TdStore.users[b.botUserId]?.usernames?.activeUsernames?.firstOrNull().orEmpty(); b.commands.map { n to it } }

/** the bot's reply keyboard (buttons instead of typing); a chip above the input hides / shows it */
@Composable
fun BotKeyboard(chat: TdApi.Chat, onSendText: (String) -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(chat.id, chat.replyMarkupMessageId) {
        val id = chat.replyMarkupMessageId
        if (id != 0L && TdStore.replyMarkups[chat.id]?.id != id) Td.callOrNull(TdApi.GetMessage(chat.id, id))?.let { TdStore.replyMarkups[chat.id] = it }
    }
    val msg = TdStore.replyMarkups[chat.id]?.takeIf { it.id == chat.replyMarkupMessageId }
    val kb = msg?.replyMarkup as? TdApi.ReplyMarkupShowKeyboard ?: return
    var shown by remember(msg.id) { mutableStateOf(true) }
    var askPhone by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Surface(onClick = { shown = !shown }, shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (shown) Icons.Outlined.KeyboardHide else Icons.Outlined.Keyboard, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (shown) "Скрыть кнопки" else "Кнопки бота", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        AnimatedVisibility(shown, enter = expandVertically(Springs.smooth()) + fadeIn(Springs.effects()), exit = shrinkVertically(Springs.exit()) + fadeOut(Springs.exit())) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.padding(top = 6.dp)) {
                Column(Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    kb.rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { b ->
                                FilledTonalButton(
                                    onClick = {
                                        when (val t = b.type) {
                                            is TdApi.KeyboardButtonTypeRequestPhoneNumber -> askPhone = b.text
                                            is TdApi.KeyboardButtonTypeWebApp -> Links.browser(ctx, t.url)
                                            is TdApi.KeyboardButtonTypeRequestLocation, is TdApi.KeyboardButtonTypeRequestPoll,
                                            is TdApi.KeyboardButtonTypeRequestUsers, is TdApi.KeyboardButtonTypeRequestChat ->
                                                Toast.makeText(ctx, "Эта кнопка пока не поддерживается", Toast.LENGTH_SHORT).show()
                                            else -> { onSendText(b.text); if (kb.oneTime) shown = false }
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                                ) { Text(b.text, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center) }
                            }
                        }
                    }
                }
            }
        }
    }
    askPhone?.let {
        AlertDialog(
            onDismissRequest = { askPhone = null },
            title = { Text("Отправить номер телефона?") },
            text = { Text("Бот получит ваш номер телефона.") },
            confirmButton = {
                QTextButton(onClick = {
                    askPhone = null
                    val me = TdStore.me ?: return@QTextButton
                    sendContent(chat.id, TdApi.InputMessageContact(TdApi.Contact(me.phoneNumber, me.firstName, me.lastName, "", me.id)))
                }) { Text("Отправить") }
            },
            dismissButton = { QTextButton(onClick = { askPhone = null }) { Text("Отмена") } },
        )
    }
}

// =====================================================================================================================
//  Forum topics
// =====================================================================================================================

@Composable
fun TopicsPage(chatId: Long) {
    val chat = TdStore.chats[chatId]
    val ctx = LocalContext.current
    var topics by remember { mutableStateOf<List<TdApi.ForumTopic>?>(null) }
    var tick by remember { mutableStateOf(0) }
    var create by remember { mutableStateOf(false) }
    LaunchedEffect(chatId, tick) {
        if (tick > 0) delay(400)
        topics = Td.callOrNull(TdApi.GetForumTopics(chatId, "", 0, false, false, 0, 0, 0, 100))?.topics?.toList() ?: topics.orEmpty()
    }
    LaunchedEffect(chatId) {
        TdStore.messageEvents.collect { u ->
            val hit = when (u) {
                is TdApi.UpdateForumTopicInfo -> u.info.chatId == chatId
                is TdApi.UpdateForumTopic -> u.chatId == chatId
                is TdApi.UpdateNewMessage -> u.message.chatId == chatId
                else -> false
            }
            if (hit) tick++
        }
    }
    DisposableEffect0(chatId)
    val canCreate = chat != null && (TdStore.isAdmin(chat) || chat.permissions.canCreateTopics)
    PageScaffold(chat?.title ?: "Темы", actions = {
        HeaderIcon(Icons.AutoMirrored.Outlined.Chat, "Все сообщения", { Nav.push(Route.Chat(chatId)) })
        if (canCreate) HeaderIcon(Icons.Outlined.Add, "Новая тема", { create = true })
    }) {
        val list = topics
        if (list == null) item { Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { LoadingIndicator() } }
        else if (list.isEmpty()) item { Text("Тем пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center) }
        else {
            item { Spacer(Modifier.height(10.dp)) }
            itemsIndexed(list, key = { _, t -> t.info.forumTopicId }) { i, t -> TopicRow(chatId, t, Modifier.segment(i, list.size)) }
        }
    }
    if (create) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { create = false },
            title = { Text("Новая тема") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true) },
            confirmButton = {
                QTextButton(enabled = name.isNotBlank(), onClick = {
                    create = false
                    val colors = intArrayOf(0x6FB9F0, 0xFFD67E, 0xCB86DB, 0x8EEE98, 0xFF93B2, 0xFB6F5F)
                    Td.send(TdApi.CreateForumTopic(chatId, name.trim(), false, TdApi.ForumTopicIcon(colors.random(), 0))) { r ->
                        if (r is TdApi.ForumTopicInfo) Nav.push(Route.Chat(chatId, r.forumTopicId))
                        else if (r is TdApi.Error) Toast.makeText(ctx, "Не удалось: ${r.message}", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Создать") }
            },
            dismissButton = { QTextButton(onClick = { create = false }) { Text("Отмена") } },
        )
    }
}

/** keeps the forum's chat open while its topic list is shown (TDLib sends topic updates only then) */
@Composable
private fun DisposableEffect0(chatId: Long) {
    androidx.compose.runtime.DisposableEffect(chatId) {
        Td.send(TdApi.OpenChat(chatId))
        onDispose { Td.send(TdApi.CloseChat(chatId)) }
    }
}

@Composable
private fun TopicRow(chatId: Long, t: TdApi.ForumTopic, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val info = t.info
    Row(
        modifier.clickable { Nav.push(Route.Chat(chatId, info.forumTopicId)) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopicIcon(info, 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(info.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (info.isClosed) { Spacer(Modifier.width(4.dp)); Icon(Icons.Outlined.Lock, "Закрыта", Modifier.size(16.dp), tint = cs.onSurfaceVariant) }
                if (t.isPinned) { Spacer(Modifier.width(4.dp)); Icon(Icons.Outlined.PushPin, "Закреплена", Modifier.size(16.dp), tint = cs.onSurfaceVariant) }
                Spacer(Modifier.weight(1f))
                t.lastMessage?.let { Text(Texts.time(it.date), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t.lastMessage?.let { Texts.preview(it, withSender = true) } ?: "", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (t.unreadMentionCount > 0) { Spacer(Modifier.width(6.dp)); Badge("@", cs.primary, cs.onPrimary) }
                if (t.unreadCount > 0) { Spacer(Modifier.width(6.dp)); Badge(t.unreadCount.toString(), cs.primary, cs.onPrimary) }
            }
        }
    }
}

@Composable
private fun Badge(text: String, bg: Color, fg: Color) {
    Box(Modifier.heightIn(min = 22.dp).clip(CircleShape).background(bg).padding(horizontal = 7.dp, vertical = 2.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

/** the topic's icon: its custom emoji, or a coloured circle with the first letter */
@Composable
fun TopicIcon(info: TdApi.ForumTopicInfo, size: androidx.compose.ui.unit.Dp) {
    val emojiId = info.icon.customEmojiId
    if (emojiId != 0L) {
        CustomEmojiSticker(emojiId, size)
        return
    }
    val c = Color(0xFF000000 or (info.icon.color.toLong() and 0xFFFFFF))
    Box(Modifier.size(size).clip(CircleShape).background(c.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
        Text(info.name.trim().take(1).uppercase().ifEmpty { "#" }, color = c, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

/** a custom emoji by id (premium emoji, topic icons) */
@Composable
fun CustomEmojiSticker(id: Long, size: androidx.compose.ui.unit.Dp) {
    var sticker by remember(id) { mutableStateOf<TdApi.Sticker?>(null) }
    LaunchedEffect(id) { sticker = Td.callOrNull(TdApi.GetCustomEmojiStickers(longArrayOf(id)))?.stickers?.firstOrNull() }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        sticker?.let { StickerContent(it, Modifier.size(size)) { } }
    }
}

/** the topic name for a topic chat screen's header */
@Composable
fun rememberTopicName(chatId: Long, topic: Int): String? {
    if (topic == 0) return null
    var name by remember(chatId, topic) { mutableStateOf<String?>(null) }
    LaunchedEffect(chatId, topic) { name = Td.callOrNull(TdApi.GetForumTopic(chatId, topic))?.info?.name }
    return name ?: "Тема"
}

/** not in this channel / group, and not banned from it: it can be joined */
fun canJoin(chat: TdApi.Chat): Boolean {
    if (TdStore.isMember(chat)) return false
    val st = when (val t = chat.type) {
        is TdApi.ChatTypeSupergroup -> TdStore.supergroups[t.supergroupId]?.status
        is TdApi.ChatTypeBasicGroup -> return false   // basic groups can't be joined without an invite
        else -> null
    }
    return st is TdApi.ChatMemberStatusLeft || (st is TdApi.ChatMemberStatusRestricted && !st.isMember)
}

fun autoDeleteLabel(secs: Int): String = when {
    secs <= 0 -> "выкл"
    secs < 86400 -> "${secs / 3600} ч"
    secs < 604800 -> "${secs / 86400} д"
    secs < 2678400 -> "${secs / 604800} нед"
    else -> "${secs / 2678400} мес"
}

/** what members of a group may do (admins), and the slow mode */
@Composable
fun PermissionsPage(chatId: Long) {
    val chat = TdStore.chats[chatId] ?: return
    val ctx = LocalContext.current
    val p = chat.permissions
    val sg = chat.type as? TdApi.ChatTypeSupergroup
    var slow by remember { mutableStateOf(0) }
    var pickSlow by remember { mutableStateOf(false) }
    LaunchedEffect(chatId) { if (sg != null) Td.callOrNull(TdApi.GetSupergroupFullInfo(sg.supergroupId))?.let { slow = it.slowModeDelay } }
    fun set(change: TdApi.ChatPermissions.() -> Unit) {
        val n = copyPermissions(p).apply(change)
        Td.send(TdApi.SetChatPermissions(chatId, n)) { r -> if (r is TdApi.Error) Toast.makeText(ctx, "Не удалось: ${r.message}", Toast.LENGTH_SHORT).show() }
    }
    PageScaffold("Разрешения") {
        group("Участники могут") {
            switchState("Писать сообщения", p.canSendBasicMessages) { v -> set { canSendBasicMessages = v } }
            switchState("Отправлять фото", p.canSendPhotos) { v -> set { canSendPhotos = v } }
            switchState("Отправлять видео", p.canSendVideos) { v -> set { canSendVideos = v } }
            switchState("Стикеры и GIF", p.canSendOtherMessages) { v -> set { canSendOtherMessages = v } }
            switchState("Музыку", p.canSendAudios) { v -> set { canSendAudios = v } }
            switchState("Файлы", p.canSendDocuments) { v -> set { canSendDocuments = v } }
            switchState("Голосовые сообщения", p.canSendVoiceNotes) { v -> set { canSendVoiceNotes = v } }
            switchState("Видеосообщения", p.canSendVideoNotes) { v -> set { canSendVideoNotes = v } }
            switchState("Опросы", p.canSendPolls) { v -> set { canSendPolls = v } }
            switchState("Предпросмотр ссылок", p.canAddLinkPreviews) { v -> set { canAddLinkPreviews = v } }
            switchState("Ставить реакции", p.canReactToMessages) { v -> set { canReactToMessages = v } }
        }
        group("Управление") {
            switchState("Добавлять участников", p.canInviteUsers) { v -> set { canInviteUsers = v } }
            switchState("Закреплять сообщения", p.canPinMessages) { v -> set { canPinMessages = v } }
            switchState("Менять название и фото", p.canChangeInfo) { v -> set { canChangeInfo = v } }
            if (TdStore.isForum(chat)) switchState("Создавать темы", p.canCreateTopics) { v -> set { canCreateTopics = v } }
        }
        if (sg != null) group(footer = "Как часто каждый участник может отправлять сообщения") {
            value("Медленный режим", slowLabel(slow), onClick = { pickSlow = true })
        }
    }
    if (pickSlow) RadioDialog("Медленный режим", listOf(0, 10, 30, 60, 300, 900, 3600).map { it to slowLabel(it) }, slow, onPick = { v ->
        pickSlow = false
        Td.send(TdApi.SetChatSlowModeDelay(chatId, v)) { r -> if (r is TdApi.Error) Toast.makeText(ctx, "Не удалось: ${r.message}", Toast.LENGTH_SHORT).show() else slow = v }
    }) { pickSlow = false }
}

private fun slowLabel(s: Int) = when { s <= 0 -> "Выключен"; s < 60 -> "$s с"; s < 3600 -> "${s / 60} мин"; else -> "${s / 3600} ч" }

private fun copyPermissions(p: TdApi.ChatPermissions) = TdApi.ChatPermissions().also { n ->
    n.canSendBasicMessages = p.canSendBasicMessages; n.canSendAudios = p.canSendAudios; n.canSendDocuments = p.canSendDocuments
    n.canSendPhotos = p.canSendPhotos; n.canSendVideos = p.canSendVideos; n.canSendVideoNotes = p.canSendVideoNotes
    n.canSendVoiceNotes = p.canSendVoiceNotes; n.canSendPolls = p.canSendPolls; n.canSendOtherMessages = p.canSendOtherMessages
    n.canAddLinkPreviews = p.canAddLinkPreviews; n.canReactToMessages = p.canReactToMessages; n.canEditTag = p.canEditTag
    n.canChangeInfo = p.canChangeInfo; n.canInviteUsers = p.canInviteUsers; n.canPinMessages = p.canPinMessages
    n.canCreateTopics = p.canCreateTopics
}

fun canCommentWithoutJoin(chat: TdApi.Chat): Boolean {
    val t = chat.type as? TdApi.ChatTypeSupergroup ?: return false
    val sg = TdStore.supergroups[t.supergroupId] ?: return false
    return sg.status is TdApi.ChatMemberStatusLeft && !sg.joinToSendMessages && chat.permissions.canSendBasicMessages
}

// =====================================================================================================================
//  Attachments: location, contact, dice; the dice message itself
// =====================================================================================================================

/** asks for location access, finds where you are and offers to send it */
@Composable
fun LocationSender(chatId: Long, replyTo: TdApi.Message?, onDone: () -> Unit) {
    val ctx = LocalContext.current
    var loc by remember { mutableStateOf<android.location.Location?>(null) }
    var failed by remember { mutableStateOf(false) }
    fun locate() {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val providers = listOf(android.location.LocationManager.GPS_PROVIDER, android.location.LocationManager.NETWORK_PROVIDER).filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        // the freshest known fix right away, then a current one when the system has it
        try {
            providers.mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }?.let { loc = it }
            if (providers.isEmpty()) { if (loc == null) failed = true; return }
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                lm.getCurrentLocation(providers.first(), null, ctx.mainExecutor) { l -> if (l != null) loc = l else if (loc == null) failed = true }
            } else if (loc == null) failed = true
        } catch (e: SecurityException) { failed = true }
    }
    val perm = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) locate() else { Toast.makeText(ctx, "Нет доступа к геопозиции", Toast.LENGTH_SHORT).show(); onDone() }
    }
    LaunchedEffect(Unit) {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) locate() else perm.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    QSheet(onDismiss = onDone, wrap = true) { sheet ->
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Геопозиция", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            val l = loc
            when {
                l != null -> {
                    Text("%.5f, %.5f".format(java.util.Locale.US, l.latitude, l.longitude), style = MaterialTheme.typography.titleMedium)
                    Text("точность ~${l.accuracy.toInt()} м", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                failed -> Text("Не удалось определить местоположение. Включите геолокацию и попробуйте снова.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> { LoadingIndicator(); Text("Определяем местоположение…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                enabled = l != null,
                onClick = {
                    val p = l ?: return@Button
                    sendContent(chatId, TdApi.InputMessageLocation(TdApi.Location(p.latitude, p.longitude, p.accuracy.toDouble())), replyTo)
                    sheet.close()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Отправить мою геопозицию") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** share one of your Telegram contacts */
@Composable
fun ContactPicker(chatId: Long, replyTo: TdApi.Message?, onDone: () -> Unit) {
    val ctx = LocalContext.current
    var users by remember { mutableStateOf<List<TdApi.User>?>(null) }
    var q by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val ids = Td.callOrNull(TdApi.GetContacts())?.userIds?.toList().orEmpty()
        users = ids.mapNotNull { TdStore.users[it] ?: Td.callOrNull(TdApi.GetUser(it)) }.sortedBy { TdStore.fullName(it).lowercase() }
    }
    QSheet(onDismiss = onDone, halfFirst = true) { sheet ->
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Отправить контакт", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
            OutlinedTextField(q, { q = it }, placeholder = { Text("Поиск") }, singleLine = true, shape = CircleShape, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            val list = users?.filter { q.isBlank() || TdStore.fullName(it).contains(q, ignoreCase = true) }
            if (list == null) Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
            else LazyColumn(Modifier.fillMaxWidth().weightFill(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                itemsIndexed(list, key = { _, u -> u.id }) { i, u ->
                    Row(
                        Modifier.segment(i, list.size).clickable {
                            if (u.phoneNumber.isBlank()) { Toast.makeText(ctx, "Номер этого контакта скрыт", Toast.LENGTH_SHORT).show(); return@clickable }
                            sendContent(chatId, TdApi.InputMessageContact(TdApi.Contact(u.phoneNumber, u.firstName, u.lastName, "", u.id)), replyTo)
                            sheet.close()
                        }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        UserAvatar(u, 40.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(TdStore.fullName(u), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (u.phoneNumber.isNotBlank()) "+" + u.phoneNumber else Texts.status(u), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.weightFill(): Modifier = this.heightIn(max = 560.dp)

private val DICE = listOf("🎲" to "Кубик", "🎯" to "Дартс", "🏀" to "Баскетбол", "⚽" to "Футбол", "🎳" to "Боулинг", "🎰" to "Слоты")

/** Telegram's dice games: the result is decided by the server and played as an animation */
@Composable
fun DicePicker(chatId: Long, replyTo: TdApi.Message?, onDone: () -> Unit) {
    QSheet(onDismiss = onDone, wrap = true) { sheet ->
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Игры", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
            DICE.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (e, name) ->
                        Surface(
                            onClick = { sendContent(chatId, TdApi.InputMessageDice(e, true), replyTo); sheet.close() },
                            shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.weight(1f),
                        ) {
                            Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(e, fontSize = androidx.compose.ui.unit.TextUnit(36f, androidx.compose.ui.unit.TextUnitType.Sp))
                                Text(name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** a dice message: the roll animation, then the result (slot machine = its five layers) */
@Composable
fun DiceView(d: TdApi.MessageDice) {
    val st = d.finalState ?: d.initialState
    val rolling = d.finalState == null
    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        when (st) {
            is TdApi.DiceStickersRegular -> DiceSticker(st.sticker, rolling, Modifier.size(150.dp))
            is TdApi.DiceStickersSlotMachine -> listOf(st.background, st.lever, st.leftReel, st.centerReel, st.rightReel).forEach { DiceSticker(it, rolling, Modifier.size(150.dp)) }
            else -> Text(d.emoji, fontSize = androidx.compose.ui.unit.TextUnit(72f, androidx.compose.ui.unit.TextUnitType.Sp))
        }
    }
}

@Composable
private fun DiceSticker(s: TdApi.Sticker, loop: Boolean, modifier: Modifier) {
    if (s.format is TdApi.StickerFormatTgs) TgsImage(s.sticker, modifier, loop = loop) else StickerContent(s, modifier)
}

// =====================================================================================================================
//  Inline bots: "@gif cats", "@pic sunset", "@wiki …" — results above the input, a tap sends one
// =====================================================================================================================

private val inlineQuery = Regex("^@([A-Za-z0-9_]{3,32}) ([\\s\\S]*)$")

@Composable
fun InlineBotResults(chat: TdApi.Chat, text: String, replyTo: TdApi.Message?, onSent: () -> Unit) {
    val ctx = LocalContext.current
    val match = inlineQuery.find(text)
    val botName = match?.groupValues?.get(1)
    val query = match?.groupValues?.get(2).orEmpty()
    var bot by remember { mutableStateOf<TdApi.User?>(null) }
    var results by remember { mutableStateOf<TdApi.InlineQueryResults?>(null) }
    LaunchedEffect(botName) {
        bot = null; results = null
        if (botName == null) return@LaunchedEffect
        val c = Td.callOrNull(TdApi.SearchPublicChat(botName)) ?: return@LaunchedEffect
        val u = TdStore.users[TdStore.privateUserId(c)] ?: Td.callOrNull(TdApi.GetUser(TdStore.privateUserId(c)))
        if ((u?.type as? TdApi.UserTypeBot)?.isInline == true) bot = u
    }
    val b = bot
    LaunchedEffect(b?.id, query) {
        if (b == null) return@LaunchedEffect
        delay(350)
        results = Td.callOrNull(TdApi.GetInlineQueryResults(b.id, chat.id, null, query, ""))
    }
    val r = results
    val show = b != null && botName != null && r != null && r.results.isNotEmpty()
    AnimatedVisibility(show, enter = expandVertically(Springs.smooth()) + fadeIn(Springs.effects()), exit = shrinkVertically(Springs.exit()) + fadeOut(Springs.exit())) {
        val res = r ?: return@AnimatedVisibility
        fun send(id: String) {
            Td.send(TdApi.SendInlineQueryResultMessage(chat.id, currentTopic(chat.id), replyTo?.let { TdApi.InputMessageReplyToMessage().apply { messageId = it.id } }, TdApi.MessageSendOptions(), res.inlineQueryId, id, false)) { x ->
                if (x is TdApi.Error) Toast.makeText(ctx, "Не удалось: ${x.message}", Toast.LENGTH_SHORT).show()
            }
            onSent()
        }
        val visual = res.results.all { it is TdApi.InlineQueryResultPhoto || it is TdApi.InlineQueryResultAnimation || it is TdApi.InlineQueryResultSticker || it is TdApi.InlineQueryResultVideo }
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 3.dp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            if (visual) {
                androidx.compose.foundation.lazy.LazyRow(Modifier.fillMaxWidth().height(132.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(res.results.toList()) { item ->
                        Box(Modifier.size(116.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).clickable { send(inlineId(item)) }) {
                            InlineThumb(item, Modifier.size(116.dp))
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 260.dp)) {
                    items(res.results.toList()) { item ->
                        val (title, sub) = inlineTitle(item)
                        Row(Modifier.fillMaxWidth().clickable { send(inlineId(item)) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) { InlineThumb(item, Modifier.size(44.dp)) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun inlineId(r: TdApi.InlineQueryResult): String = when (r) {
    is TdApi.InlineQueryResultArticle -> r.id
    is TdApi.InlineQueryResultContact -> r.id
    is TdApi.InlineQueryResultLocation -> r.id
    is TdApi.InlineQueryResultVenue -> r.id
    is TdApi.InlineQueryResultGame -> r.id
    is TdApi.InlineQueryResultAnimation -> r.id
    is TdApi.InlineQueryResultAudio -> r.id
    is TdApi.InlineQueryResultDocument -> r.id
    is TdApi.InlineQueryResultPhoto -> r.id
    is TdApi.InlineQueryResultSticker -> r.id
    is TdApi.InlineQueryResultVideo -> r.id
    is TdApi.InlineQueryResultVoiceNote -> r.id
    else -> ""
}

private fun inlineTitle(r: TdApi.InlineQueryResult): Pair<String, String> = when (r) {
    is TdApi.InlineQueryResultArticle -> r.title to r.description
    is TdApi.InlineQueryResultContact -> "${r.contact.firstName} ${r.contact.lastName}".trim() to r.contact.phoneNumber
    is TdApi.InlineQueryResultLocation -> r.title to ""
    is TdApi.InlineQueryResultVenue -> r.venue.title to r.venue.address
    is TdApi.InlineQueryResultGame -> r.game.title to r.game.description
    is TdApi.InlineQueryResultAnimation -> r.title.ifBlank { "GIF" } to ""
    is TdApi.InlineQueryResultAudio -> r.audio.title.ifBlank { r.audio.fileName } to r.audio.performer
    is TdApi.InlineQueryResultDocument -> r.title to r.description
    is TdApi.InlineQueryResultPhoto -> r.title.ifBlank { "Фото" } to r.description
    is TdApi.InlineQueryResultSticker -> "Стикер" to ""
    is TdApi.InlineQueryResultVideo -> r.title.ifBlank { "Видео" } to r.description
    is TdApi.InlineQueryResultVoiceNote -> r.title.ifBlank { "Голосовое" } to ""
    else -> "" to ""
}

/** the picture of an inline result: its thumbnail when it's a still image, the blurred mini thumbnail otherwise */
@Composable
private fun InlineThumb(r: TdApi.InlineQueryResult, modifier: Modifier) {
    if (r is TdApi.InlineQueryResultSticker) { StickerContent(r.sticker, modifier.padding(8.dp)); return }
    val (thumb, mini) = when (r) {
        is TdApi.InlineQueryResultArticle -> r.thumbnail to null
        is TdApi.InlineQueryResultContact -> r.thumbnail to null
        is TdApi.InlineQueryResultLocation -> r.thumbnail to null
        is TdApi.InlineQueryResultVenue -> r.thumbnail to null
        is TdApi.InlineQueryResultAnimation -> r.animation.thumbnail to r.animation.minithumbnail
        is TdApi.InlineQueryResultVideo -> r.video.thumbnail to r.video.minithumbnail
        is TdApi.InlineQueryResultPhoto -> null to r.photo.minithumbnail
        else -> null to null
    }
    val stillFile = when {
        r is TdApi.InlineQueryResultPhoto -> r.photo.sizes.minByOrNull { kotlin.math.abs(it.width - 320) }?.photo
        thumb != null && (thumb.format is TdApi.ThumbnailFormatJpeg || thumb.format is TdApi.ThumbnailFormatPng || thumb.format is TdApi.ThumbnailFormatWebp) -> thumb.file
        else -> null
    }
    val bmp = app.qwgram.next.td.rememberFileBitmap(stillFile, maxSide = 320)
    val miniBmp = remember(mini) { app.qwgram.next.td.Images.mini(mini) }
    val img = bmp ?: miniBmp
    if (img != null) androidx.compose.foundation.Image(img, null, modifier, contentScale = androidx.compose.ui.layout.ContentScale.Crop)
}
