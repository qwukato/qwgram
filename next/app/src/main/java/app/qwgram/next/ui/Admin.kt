package app.qwgram.next.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.qwgram.next.td.Td
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.Texts
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

private fun shortDate(unix: Int): String = java.text.SimpleDateFormat("d MMM", java.util.Locale("ru")).format(java.util.Date(unix * 1000L))

private fun toast(ctx: Context, s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()

private fun shareText(ctx: Context, s: String) {
    val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, s)
    ctx.startActivity(Intent.createChooser(i, "Поделиться").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

// =====================================================================================================================
//  Invite links & join requests (admins)
// =====================================================================================================================

@Composable
fun InviteLinksPage(chatId: Long) {
    val ctx = LocalContext.current
    val chat = TdStore.chats[chatId]
    var links by remember { mutableStateOf<List<TdApi.ChatInviteLink>?>(null) }
    var revoked by remember { mutableStateOf<List<TdApi.ChatInviteLink>>(emptyList()) }
    var tick by remember { mutableIntStateOf(0) }
    var create by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<TdApi.ChatInviteLink?>(null) }
    LaunchedEffect(chatId, tick) {
        val me = TdStore.me?.id ?: 0L
        // the primary link is made on first ask when the chat has none yet
        Td.callOrNull(TdApi.GetChatInviteLinks(chatId, me, false, 0, "", 1)).let { r -> if (r == null || r.inviteLinks.none { it.isPrimary }) Td.callOrNull(TdApi.ReplacePrimaryChatInviteLink(chatId)) }
        links = Td.callOrNull(TdApi.GetChatInviteLinks(chatId, me, false, 0, "", 100))?.inviteLinks?.sortedByDescending { it.isPrimary }.orEmpty()
        revoked = Td.callOrNull(TdApi.GetChatInviteLinks(chatId, me, true, 0, "", 50))?.inviteLinks?.toList().orEmpty()
    }
    PageScaffold("Ссылки-приглашения") {
        val l = links
        if (l == null) { item { Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { LoadingIndicator() } }; return@PageScaffold }
        val pending = chat?.pendingJoinRequests?.totalCount ?: 0
        if (pending > 0) group {
            nav("Заявки на вступление", "$pending " + Texts.plural(pending, "заявка", "заявки", "заявок"), Icons.Outlined.PersonAdd) { Nav.push(Route.Page("requests:$chatId")) }
        }
        group("Активные", footer = "Кто перейдёт по ссылке, вступит в чат (или подаст заявку, если она так настроена)") {
            l.forEach { link -> row { m -> LinkRow(link, m) { picked = link } } }
            action("Создать ссылку", icon = Icons.Outlined.Add) { create = true }
        }
        if (revoked.isNotEmpty()) group("Отозванные") {
            revoked.forEach { link -> row { m -> LinkRow(link, m, dim = true) {} } }
            action("Удалить все отозванные", icon = Icons.Outlined.LinkOff, danger = true) {
                Td.send(TdApi.DeleteAllRevokedChatInviteLinks(chatId, TdStore.me?.id ?: 0L)) { tick++ }
            }
        }
    }
    picked?.let { link ->
        QSheet(onDismiss = { picked = null }, wrap = true) { sheet ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(link.name.ifBlank { if (link.isPrimary) "Основная ссылка" else "Ссылка" }, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
                Text(link.inviteLink, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
                val acts = listOf(
                    Triple("Копировать", Icons.Outlined.ContentCopy) { copyText(ctx, link.inviteLink); sheet.close() },
                    Triple("Поделиться", Icons.Outlined.Share) { shareText(ctx, link.inviteLink); sheet.close() },
                    Triple(if (link.isPrimary) "Заменить новой" else "Отозвать", Icons.Outlined.LinkOff) {
                        sheet.close()
                        val f: TdApi.Function<*> = if (link.isPrimary) TdApi.ReplacePrimaryChatInviteLink(chatId) else TdApi.RevokeChatInviteLink(chatId, link.inviteLink)
                        Td.send(f) { r -> if (r is TdApi.Error) toast(ctx, "Не удалось: ${r.message}"); tick++ }
                    },
                )
                acts.forEachIndexed { i, (t, ic, a) ->
                    Box(Modifier.segment(i, acts.size)) { BaseRow(t, icon = ic, danger = i == 2, onClick = a) }
                    if (i < acts.lastIndex) Spacer(Modifier.height(2.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    if (create) CreateLinkDialog(chatId, onDone = { create = false; tick++ })
}

@Composable
private fun LinkRow(link: TdApi.ChatInviteLink, modifier: Modifier, dim: Boolean = false, onClick: () -> Unit) {
    val parts = buildList {
        add(if (link.memberCount == 0) "никто не вступил" else "${link.memberCount} " + Texts.plural(link.memberCount, "вступил", "вступили", "вступили"))
        if (link.memberLimit > 0) add("лимит ${link.memberLimit}")
        if (link.createsJoinRequest) add("по заявке" + if (link.pendingJoinRequestCount > 0) " (${link.pendingJoinRequestCount})" else "")
        if (link.expirationDate > 0) add(if (link.expirationDate * 1000L < System.currentTimeMillis()) "истекла" else "до " + shortDate(link.expirationDate))
    }
    Box(modifier) {
        BaseRow(
            link.name.ifBlank { link.inviteLink.removePrefix("https://") },
            sub = (if (link.isPrimary) "Основная · " else "") + parts.joinToString(" · "),
            icon = if (dim) Icons.Outlined.LinkOff else Icons.Outlined.Link,
            onClick = onClick,
        )
    }
}

@Composable
private fun CreateLinkDialog(chatId: Long, onDone: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var requests by remember { mutableStateOf(false) }
    var limit by remember { mutableIntStateOf(0) }
    var period by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Новая ссылка") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it.take(32) }, label = { Text("Название (видно только админам)") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Заявки на вступление")
                        Text("Админы одобряют каждого", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    QSwitch(requests) { requests = it; if (it) limit = 0 }
                }
                if (!requests) ChoiceRow("Лимит", listOf(0 to "нет", 1 to "1", 10 to "10", 100 to "100"), limit) { limit = it }
                ChoiceRow("Срок", listOf(0 to "нет", 3600 to "1 ч", 86400 to "1 д", 604800 to "7 д"), period) { period = it }
            }
        },
        confirmButton = {
            QTextButton(onClick = {
                val exp = if (period > 0) (System.currentTimeMillis() / 1000).toInt() + period else 0
                Td.send(TdApi.CreateChatInviteLink(chatId, name.trim(), exp, if (requests) 0 else limit, requests)) { r ->
                    if (r is TdApi.ChatInviteLink) { copyText(ctx, r.inviteLink) } else if (r is TdApi.Error) toast(ctx, "Не удалось: ${r.message}")
                    onDone()
                }
            }) { Text("Создать") }
        },
        dismissButton = { QTextButton(onClick = onDone) { Text("Отмена") } },
    )
}

@Composable
private fun ChoiceRow(title: String, options: List<Pair<Int, String>>, value: Int, onPick: (Int) -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, (v, label) ->
                SegmentedButton(
                    selected = v == value, onClick = { onPick(v) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    icon = {},
                ) { Text(label, maxLines = 1) }
            }
        }
    }
}

@Composable
fun JoinRequestsPage(chatId: Long) {
    val ctx = LocalContext.current
    val reqs = remember { mutableStateListOf<TdApi.ChatJoinRequest>() }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(chatId) {
        Td.callOrNull(TdApi.GetChatJoinRequests(chatId, "", "", null, 100))?.requests?.let { r ->
            r.forEach { q -> if (TdStore.users[q.userId] == null) Td.callOrNull(TdApi.GetUser(q.userId)) }
            reqs.clear(); reqs.addAll(r)
        }
        loaded = true
    }
    fun process(q: TdApi.ChatJoinRequest, ok: Boolean) {
        reqs.remove(q)
        Td.send(TdApi.ProcessChatJoinRequest(chatId, q.userId, ok)) { r -> if (r is TdApi.Error) { toast(ctx, "Не удалось: ${r.message}"); reqs.add(q) } }
    }
    PageScaffold("Заявки", actions = {
        if (reqs.isNotEmpty()) QTextButton(onClick = {
            Td.send(TdApi.ProcessChatJoinRequests(chatId, "", true)) { r -> if (r is TdApi.Error) toast(ctx, "Не удалось: ${r.message}") else reqs.clear() }
        }) { Text("Принять всех") }
    }) {
        if (!loaded) item { Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { LoadingIndicator() } }
        else if (reqs.isEmpty()) item { Text("Новых заявок нет", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) }
        else {
            item { Spacer(Modifier.height(10.dp)) }
            itemsIndexed(reqs.toList(), key = { _, q -> q.userId }) { i, q ->
                Column(Modifier.segment(i, reqs.size).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(TdStore.users[q.userId], 44.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(TdStore.userName(q.userId).ifBlank { "Пользователь" }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (q.bio.isNotBlank()) q.bio else "заявка " + shortDate(q.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { process(q, true) }, modifier = Modifier.weight(1f)) { Text("Принять") }
                        OutlinedButton(onClick = { process(q, false) }, modifier = Modifier.weight(1f)) { Text("Отклонить") }
                    }
                }
            }
        }
    }
}

// =====================================================================================================================
//  Administrator rights
// =====================================================================================================================

private fun copyRights(r: TdApi.ChatAdministratorRights) = TdApi.ChatAdministratorRights().also { n ->
    n.canManageChat = r.canManageChat; n.canChangeInfo = r.canChangeInfo; n.canPostMessages = r.canPostMessages
    n.canEditMessages = r.canEditMessages; n.canDeleteMessages = r.canDeleteMessages; n.canInviteUsers = r.canInviteUsers
    n.canRestrictMembers = r.canRestrictMembers; n.canPinMessages = r.canPinMessages; n.canManageTopics = r.canManageTopics
    n.canPromoteMembers = r.canPromoteMembers; n.canManageVideoChats = r.canManageVideoChats; n.canPostStories = r.canPostStories
    n.canEditStories = r.canEditStories; n.canDeleteStories = r.canDeleteStories; n.canManageDirectMessages = r.canManageDirectMessages
    n.canManageTags = r.canManageTags; n.canSendWelcomeMessages = r.canSendWelcomeMessages; n.isAnonymous = r.isAnonymous
}

/** Telegram's defaults for a new admin */
private fun defaultRights(channel: Boolean) = TdApi.ChatAdministratorRights().also { n ->
    n.canManageChat = true; n.canChangeInfo = true; n.canDeleteMessages = true; n.canInviteUsers = true
    n.canManageVideoChats = true
    if (channel) { n.canPostMessages = true; n.canEditMessages = true; n.canPostStories = true; n.canEditStories = true; n.canDeleteStories = true }
    else { n.canRestrictMembers = true; n.canPinMessages = true; n.canManageTopics = true }
}

@Composable
fun AdminRightsPage(chatId: Long, userId: Long) {
    val ctx = LocalContext.current
    val chat = TdStore.chats[chatId] ?: return
    val channel = TdStore.isChannel(chat)
    var rights by remember { mutableStateOf<TdApi.ChatAdministratorRights?>(null) }
    var wasAdmin by remember { mutableStateOf(false) }
    var canEdit by remember { mutableStateOf(true) }
    val sender = remember(userId) { TdApi.MessageSenderUser(userId) }
    LaunchedEffect(chatId, userId) {
        val m = Td.callOrNull(TdApi.GetChatMember(chatId, sender))
        val st = m?.status
        if (st is TdApi.ChatMemberStatusAdministrator) { rights = copyRights(st.rights); wasAdmin = true; canEdit = st.canBeEdited }
        else rights = defaultRights(channel)
    }
    fun change(f: TdApi.ChatAdministratorRights.() -> Unit) { rights = rights?.let { copyRights(it).apply(f) } }
    fun save() {
        val r = rights ?: return
        Td.send(TdApi.SetChatMemberStatus(chatId, sender, TdApi.ChatMemberStatusAdministrator(true, r))) { x ->
            if (x is TdApi.Error) toast(ctx, "Не удалось: ${x.message}") else { toast(ctx, "Права сохранены"); Nav.pop() }
        }
    }
    PageScaffold(if (wasAdmin) "Права администратора" else "Новый администратор", actions = {
        if (canEdit) QTextButton(onClick = ::save) { Text("Сохранить") }
    }) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(TdStore.users[userId], 52.dp)
                Spacer(Modifier.width(14.dp))
                Text(TdStore.userName(userId).ifBlank { "Пользователь" }, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        val r = rights
        if (r == null) { item { Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { LoadingIndicator() } }; return@PageScaffold }
        if (!canEdit) item { Text("Вы не можете изменить права этого администратора", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp)) }
        group("Что может этот администратор") {
            switchState("Изменять профиль ${if (channel) "канала" else "группы"}", r.canChangeInfo) { v -> change { canChangeInfo = v } }
            if (channel) {
                switchState("Публиковать сообщения", r.canPostMessages) { v -> change { canPostMessages = v } }
                switchState("Изменять чужие публикации", r.canEditMessages) { v -> change { canEditMessages = v } }
            }
            switchState("Удалять сообщения", r.canDeleteMessages) { v -> change { canDeleteMessages = v } }
            if (!channel) {
                switchState("Блокировать пользователей", r.canRestrictMembers) { v -> change { canRestrictMembers = v } }
                switchState("Закреплять сообщения", r.canPinMessages) { v -> change { canPinMessages = v } }
                if (TdStore.isForum(chat)) switchState("Управлять темами", r.canManageTopics) { v -> change { canManageTopics = v } }
            }
            switchState("Пригласительные ссылки", r.canInviteUsers) { v -> change { canInviteUsers = v } }
            switchState("Управлять трансляциями", r.canManageVideoChats) { v -> change { canManageVideoChats = v } }
            switchState("Публиковать истории", r.canPostStories) { v -> change { canPostStories = v } }
            switchState("Изменять истории", r.canEditStories) { v -> change { canEditStories = v } }
            switchState("Удалять истории", r.canDeleteStories) { v -> change { canDeleteStories = v } }
            switchState("Назначать администраторов", r.canPromoteMembers) { v -> change { canPromoteMembers = v } }
            if (!channel) switchState("Анонимность", r.isAnonymous) { v -> change { isAnonymous = v } }
        }
        if (wasAdmin && canEdit) group {
            action("Снять права администратора", icon = Icons.Outlined.LinkOff, danger = true) {
                Td.send(TdApi.SetChatMemberStatus(chatId, sender, TdApi.ChatMemberStatusMember(0))) { x ->
                    if (x is TdApi.Error) toast(ctx, "Не удалось: ${x.message}") else { toast(ctx, "Права сняты"); Nav.pop() }
                }
            }
        }
    }
}

// =====================================================================================================================
//  Shared folders (t.me/addlist/…)
// =====================================================================================================================

@Composable
fun FolderInviteDialog(link: String, info: TdApi.ChatFolderInviteLinkInfo, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val missing = info.missingChatIds.toList()
    val chosen = remember { mutableStateListOf<Long>().apply { addAll(missing) } }
    val name = info.chatFolderInfo.name.text.text
    QSheet(onDismiss = onDone, wrap = true) { sheet ->
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(Modifier.padding(start = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(name.ifBlank { "Папка" }, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                when {
                    missing.isEmpty() -> "Все чаты этой папки у вас уже есть"
                    info.chatFolderInfo.id != 0 -> "Добавить в папку ${missing.size} " + Texts.plural(missing.size, "чат", "чата", "чатов")
                    else -> "Папка и ${missing.size} " + Texts.plural(missing.size, "чат", "чата", "чатов")
                },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
            )
            androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                itemsIndexed(missing) { i, id ->
                    var chat by remember { mutableStateOf(TdStore.chats[id]) }
                    LaunchedEffect(id) { if (chat == null) chat = Td.callOrNull(TdApi.GetChat(id)) }
                    Row(
                        Modifier.segment(i, missing.size).clickable { if (id in chosen) chosen.remove(id) else chosen.add(id) }.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        chat?.let { ChatAvatar(it, 40.dp) }
                        Spacer(Modifier.width(12.dp))
                        Text(chat?.title ?: "…", Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        androidx.compose.material3.Checkbox(id in chosen, { v -> if (v) chosen.add(id) else chosen.remove(id) })
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                enabled = missing.isEmpty() || chosen.isNotEmpty(),
                onClick = {
                    if (missing.isEmpty()) { sheet.close(); return@Button }
                    Td.send(TdApi.AddChatFolderByInviteLink(link, chosen.toLongArray())) { r ->
                        toast(ctx, if (r is TdApi.Error) "Не удалось: ${r.message}" else "Папка добавлена")
                    }
                    sheet.close()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(if (missing.isEmpty()) "Готово" else "Добавить") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// =====================================================================================================================
//  Posting your own story
// =====================================================================================================================

@Composable
fun StoryComposerHost() {
    if (!Stories.compose) return
    val ctx = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u -> if (u == null) Stories.compose = false else uri = u }
    LaunchedEffect(Unit) {
        val me = TdStore.me?.id ?: run { Stories.compose = false; return@LaunchedEffect }
        when (val r = Td.callOrNull(TdApi.CanPostStory(me))) {
            is TdApi.CanPostStoryResultOk, null -> pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            is TdApi.CanPostStoryResultPremiumNeeded -> { toast(ctx, "Нужен Telegram Premium"); Stories.compose = false }
            is TdApi.CanPostStoryResultActiveStoryLimitExceeded -> { toast(ctx, "Слишком много активных историй"); Stories.compose = false }
            is TdApi.CanPostStoryResultWeeklyLimitExceeded, is TdApi.CanPostStoryResultMonthlyLimitExceeded -> { toast(ctx, "Лимит историй исчерпан, попробуйте позже"); Stories.compose = false }
            else -> pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }
    }
    uri?.let { u -> StoryComposeSheet(u) { Stories.compose = false; uri = null } }
}

@Composable
private fun StoryComposeSheet(uri: Uri, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val isVideo = remember(uri) { ctx.contentResolver.getType(uri)?.startsWith("video") == true }
    var caption by remember { mutableStateOf("") }
    var privacy by remember { mutableStateOf("all") }
    var busy by remember { mutableStateOf(false) }
    val preview = remember(uri) {
        runCatching {
            if (isVideo) android.media.MediaMetadataRetriever().run { setDataSource(ctx, uri); val b = getFrameAtTime(0); release(); b }
            else ctx.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }) }
        }.getOrNull()?.asImageBitmap()
    }
    QSheet(onDismiss = onDone, wrap = true) { sheet ->
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Новая история", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
            Row {
                Box(Modifier.width(96.dp).height(170.dp).clip(RoundedCornerShape(16.dp))) {
                    if (preview != null) Image(preview, null, Modifier.fillMaxWidth().height(170.dp), contentScale = ContentScale.Crop)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(caption, { caption = it.take(1024) }, placeholder = { Text("Подпись") }, minLines = 3, maxLines = 5, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Кто увидит", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
            val opts = listOf("all" to "Все", "contacts" to "Контакты", "close" to "Близкие друзья")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                opts.forEachIndexed { i, (k, label) ->
                    SegmentedButton(selected = privacy == k, onClick = { privacy = k }, shape = SegmentedButtonDefaults.itemShape(i, opts.size)) { Text(label, maxLines = 1) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                enabled = !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        val me = TdStore.me?.id ?: return@launch
                        val f = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { uriToCacheFile(ctx, uri, if (isVideo) "mp4" else "jpg") }
                        if (f == null) { busy = false; toast(ctx, "Не удалось прочитать файл"); return@launch }
                        val content: TdApi.InputStoryContent = if (isVideo) {
                            val dur = runCatching { android.media.MediaMetadataRetriever().run { setDataSource(f.path); val d = extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toDouble() ?: 0.0; release(); d / 1000.0 } }.getOrDefault(0.0)
                            TdApi.InputStoryContentVideo(TdApi.InputFileLocal(f.path), intArrayOf(), dur, 0.0, false)
                        } else TdApi.InputStoryContentPhoto(TdApi.InputFileLocal(f.path), intArrayOf())
                        val priv: TdApi.StoryPrivacySettings = when (privacy) {
                            "contacts" -> TdApi.StoryPrivacySettingsContacts(longArrayOf())
                            "close" -> TdApi.StoryPrivacySettingsCloseFriends()
                            else -> TdApi.StoryPrivacySettingsEveryone(longArrayOf())
                        }
                        Td.send(TdApi.PostStory(me, content, TdApi.InputStoryAreas(arrayOf()), parseFormatting(caption.trim()), null, priv, intArrayOf(), 86400, null, true, false)) { r ->
                            busy = false
                            if (r is TdApi.Error) toast(ctx, "Не удалось: ${r.message}") else { toast(ctx, "История публикуется…"); sheet.close() }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(if (busy) "Загрузка…" else "Опубликовать") }
            Spacer(Modifier.height(8.dp))
        }
    }
}
