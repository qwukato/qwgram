package app.qwgram.next.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.qwgram.next.td.Td
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.Texts
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Lock
import java.io.File

fun copyText(ctx: Context, s: String, toast: String = "Скопировано") {
    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("qwgram", s))
    Toast.makeText(ctx, toast, Toast.LENGTH_SHORT).show()
}

/** copies a content:// uri into the app cache so TDLib can read it as a local file */
fun uriToCacheFile(ctx: Context, uri: Uri, ext: String = "jpg"): File? = try {
    val f = File(ctx.cacheDir, "up_${System.currentTimeMillis()}.$ext")
    ctx.contentResolver.openInputStream(uri)?.use { input -> f.outputStream().use { input.copyTo(it) } }
    f
} catch (e: Exception) {
    null
}

// ------------------------------------------------------------------ chat folders: list + editor

@Composable
fun FoldersListPage() = PageScaffold("Папки с чатами") {
    group("Ваши папки", footer = "Папки синхронизируются со всеми вашими устройствами Telegram.") {
        if (TdStore.folders.isEmpty()) row { BaseRow("Папок пока нет") }
        TdStore.folders.forEach { f -> nav(f.name.text.text, null, Icons.Outlined.FolderOpen) { Nav.push(Route.Page("folder:${f.id}")) } }
    }
    group {
        nav("Создать папку", null, Icons.Outlined.Add, 0) { Nav.push(Route.Page("folder:new")) }
        nav("Вид панели папок", null, Icons.Outlined.FolderOpen) { Nav.push(Route.Page("ap-folders")) }
    }
}

@Composable
fun FolderEditPage(folderId: Int?) {
    val ctx = LocalContext.current
    var loaded by remember { mutableStateOf(folderId == null) }
    var base by remember { mutableStateOf<TdApi.ChatFolder?>(null) }
    var name by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf(false) }
    var nonContacts by remember { mutableStateOf(false) }
    var groups by remember { mutableStateOf(false) }
    var channels by remember { mutableStateOf(false) }
    var bots by remember { mutableStateOf(false) }
    var exMuted by remember { mutableStateOf(false) }
    var exRead by remember { mutableStateOf(false) }
    var exArchived by remember { mutableStateOf(false) }
    val included = remember { mutableStateListOf<Long>() }
    var picker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(folderId) {
        if (folderId != null) Td.callOrNull(TdApi.GetChatFolder(folderId))?.let { f ->
            base = f; name = f.name.text.text
            contacts = f.includeContacts; nonContacts = f.includeNonContacts; groups = f.includeGroups; channels = f.includeChannels; bots = f.includeBots
            exMuted = f.excludeMuted; exRead = f.excludeRead; exArchived = f.excludeArchived
            included.clear(); included.addAll(f.includedChatIds.toList())
        }
        loaded = true
    }
    fun save() {
        if (name.isBlank()) { Toast.makeText(ctx, "Введите название", Toast.LENGTH_SHORT).show(); return }
        if (!(contacts || nonContacts || groups || channels || bots) && included.isEmpty()) {
            Toast.makeText(ctx, "Добавьте чаты или типы чатов", Toast.LENGTH_SHORT).show(); return
        }
        val f = (base ?: TdApi.ChatFolder().apply { colorId = -1; pinnedChatIds = longArrayOf(); excludedChatIds = longArrayOf() }).apply {
            this.name = TdApi.ChatFolderName(TdApi.FormattedText(name.trim().take(12), arrayOf()), false)
            includeContacts = contacts; includeNonContacts = nonContacts; includeGroups = groups; includeChannels = channels; includeBots = bots
            excludeMuted = exMuted; excludeRead = exRead; excludeArchived = exArchived
            includedChatIds = included.toLongArray()
            pinnedChatIds = pinnedChatIds.filter { it in included }.toLongArray()
        }
        val fn: TdApi.Function<TdApi.ChatFolderInfo> = if (folderId == null) TdApi.CreateChatFolder(f) else TdApi.EditChatFolder(folderId, f)
        Td.send(fn) { r ->
            if (r is TdApi.Error) Toast.makeText(ctx, "Не удалось: ${r.message}", Toast.LENGTH_LONG).show() else Nav.pop()
        }
    }
    PageScaffold(if (folderId == null) "Новая папка" else "Изменить папку", actions = { QTextButton(onClick = { save() }) { Text("Сохранить") } }) {
        if (!loaded) { item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { LoadingIndicator() } }; return@PageScaffold }
        item {
            OutlinedTextField(name, { name = it.take(12) }, label = { Text("Название папки") }, singleLine = true,
                supportingText = { Text("${name.length}/12") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(16.dp))
        }
        group("Типы чатов") {
            switchState("Контакты", contacts) { contacts = it }
            switchState("Не контакты", nonContacts) { nonContacts = it }
            switchState("Группы", groups) { groups = it }
            switchState("Каналы", channels) { channels = it }
            switchState("Боты", bots) { bots = it }
        }
        group("Не показывать") {
            switchState("Без звука", exMuted) { exMuted = it }
            switchState("Прочитанные", exRead) { exRead = it }
            switchState("Архивированные", exArchived) { exArchived = it }
        }
        group("Чаты в папке") {
            nav("Добавить чаты", "${included.size} выбрано", Icons.Outlined.Add, 0) { picker = true }
            included.toList().forEach { id ->
                val c = TdStore.chats[id]
                row {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (c != null) ChatAvatar(c, 40.dp)
                        Spacer(Modifier.width(16.dp))
                        Text(c?.title ?: "Чат", Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        MorphIconButton(Icons.Outlined.Close, "Убрать", { included.remove(id) })
                    }
                }
            }
        }
        if (folderId != null) group { action("Удалить папку", null, Icons.Outlined.Delete, danger = true) { confirmDelete = true } }
    }
    if (picker) ChatPickerSheet(included.toSet(), onDismiss = { picker = false }) { set -> included.clear(); included.addAll(set) }
    if (confirmDelete && folderId != null) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { WindowBlurBehind();  Text("Удалить папку «$name»?") },
        text = { Text("Чаты останутся, удалится только папка.") },
        confirmButton = { QTextButton(onClick = { Td.send(TdApi.DeleteChatFolder(folderId, longArrayOf())); confirmDelete = false; Nav.pop() }) { Text("Удалить", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { QTextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
    )
}

@Composable
fun ChatPickerSheet(initial: Set<Long>, onDismiss: () -> Unit, onDone: (Set<Long>) -> Unit) {
    val sel = remember { mutableStateListOf<Long>().apply { addAll(initial) } }
    val chats = remember { TdStore.chatsIn(TdApi.ChatListMain()) }
    QSheet(onDismiss = { onDone(sel.toSet()); onDismiss() }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Выберите чаты", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            QTextButton(onClick = { onDone(sel.toSet()); onDismiss() }) { Text("Готово") }
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
            items(chats, key = { it.id }) { c ->
                val on = c.id in sel
                ListItem(
                    headlineContent = { Text(if (TdStore.isSaved(c)) "Избранное" else c.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingContent = { ChatAvatar(c, 40.dp) },
                    trailingContent = { Checkbox(on, { if (it) sel.add(c.id) else sel.remove(c.id) }) },
                    modifier = Modifier.clickable { if (on) sel.remove(c.id) else sel.add(c.id) },
                )
            }
        }
    }
}

// ------------------------------------------------------------------ scheduled messages

@Composable
fun ScheduledPage(chatId: Long) {
    var list by remember { mutableStateOf<List<TdApi.Message>?>(null) }
    val scope = rememberCoroutineScope()
    suspend fun reload() { list = Td.callOrNull(TdApi.GetChatScheduledMessages(chatId))?.messages?.toList().orEmpty() }
    LaunchedEffect(chatId) { reload() }
    PageScaffold("Отложенные сообщения") {
        val l = list
        if (l == null) { item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { LoadingIndicator() } }; return@PageScaffold }
        group(footer = "Долгое нажатие на «Отправить» в чате — чтобы запланировать сообщение.") {
            if (l.isEmpty()) row { BaseRow("Нет отложенных сообщений", icon = Icons.Outlined.Schedule) }
            l.forEach { m ->
                val at = (m.schedulingState as? TdApi.MessageSchedulingStateSendAtDate)?.sendDate ?: 0
                row {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(Texts.preview(m), maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                            Text("Отправится " + Texts.dayHeader(at) + " в " + Texts.time(at), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        MorphIconButton(Icons.Outlined.Send, "Отправить сейчас", { scope.launch { Td.callOrNull(TdApi.EditMessageSchedulingState(chatId, m.id, null)); reload() } })
                        MorphIconButton(Icons.Outlined.Delete, "Удалить", { scope.launch { Td.callOrNull(TdApi.DeleteMessages(chatId, longArrayOf(m.id), true)); reload() } }, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ proxy

@Composable
fun ProxyPage() {
    var list by remember { mutableStateOf<List<TdApi.AddedProxy>?>(null) }
    var add by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    suspend fun reload() { list = Td.callOrNull(TdApi.GetProxies())?.proxies?.toList().orEmpty() }
    LaunchedEffect(Unit) { reload() }
    PageScaffold("Прокси") {
        val l = list
        if (l == null) { item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { LoadingIndicator() } }; return@PageScaffold }
        group("Соединение", footer = "Прокси помогает, если Telegram недоступен в вашей сети. Включённый прокси используется сразу.") {
            if (l.isEmpty()) row { BaseRow("Прокси не добавлены", icon = Icons.Outlined.VpnKey) }
            l.forEach { p ->
                val type = when (p.proxy.type) { is TdApi.ProxyTypeMtproto -> "MTProto"; is TdApi.ProxyTypeHttp -> "HTTP"; else -> "SOCKS5" }
                row {
                    BaseRow("${p.proxy.server}:${p.proxy.port}", type + if (p.isEnabled) " · подключён" else "", Icons.Outlined.VpnKey, if (p.isEnabled) 0 else 1) {
                        QSwitch(p.isEnabled) { on ->
                            scope.launch { if (on) Td.callOrNull(TdApi.EnableProxy(p.id)) else Td.callOrNull(TdApi.DisableProxy()); reload() }
                        }
                        MorphIconButton(Icons.Outlined.Delete, "Удалить", { scope.launch { Td.callOrNull(TdApi.RemoveProxy(p.id)); reload() } })
                    }
                }
            }
        }
        group { action("Добавить прокси", null, Icons.Outlined.Add) { add = true } }
    }
    if (add) AddProxyDialog(onDismiss = { add = false }) { proxy ->
        add = false
        Td.send(TdApi.AddProxy(proxy, true, "")) { scope.launch { reload() } }
    }
}

@Composable
private fun AddProxyDialog(onDismiss: () -> Unit, onAdd: (TdApi.Proxy) -> Unit) {
    var type by remember { mutableStateOf("socks5") }
    var server by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    val ok = server.isNotBlank() && port.toIntOrNull() != null && (type != "mtproto" || secret.isNotBlank())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { WindowBlurBehind();  Text("Новый прокси") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ConnectedGroup(listOf("socks5" to "SOCKS5", "mtproto" to "MTProto", "http" to "HTTP"), type) { type = it }
                OutlinedTextField(server, { server = it.trim() }, label = { Text("Сервер") }, singleLine = true)
                OutlinedTextField(port, { port = it.filter(Char::isDigit).take(5) }, label = { Text("Порт") }, singleLine = true)
                if (type == "mtproto") OutlinedTextField(secret, { secret = it.trim() }, label = { Text("Секрет") }, singleLine = true)
                else {
                    OutlinedTextField(user, { user = it }, label = { Text("Логин (необязательно)") }, singleLine = true)
                    OutlinedTextField(pass, { pass = it }, label = { Text("Пароль (необязательно)") }, singleLine = true)
                }
            }
        },
        confirmButton = {
            QTextButton(enabled = ok, onClick = {
                val t: TdApi.ProxyType = when (type) {
                    "mtproto" -> TdApi.ProxyTypeMtproto(secret)
                    "http" -> TdApi.ProxyTypeHttp(user, pass, false)
                    else -> TdApi.ProxyTypeSocks5(user, pass)
                }
                onAdd(TdApi.Proxy(server, port.toInt(), t))
            }) { Text("Добавить") }
        },
        dismissButton = { QTextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

// ------------------------------------------------------------------ polls

@Composable
fun PollDialog(onDismiss: () -> Unit, onSend: (TdApi.InputMessageContent) -> Unit) {
    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }
    var anonymous by remember { mutableStateOf(true) }
    var multiple by remember { mutableStateOf(false) }
    var quiz by remember { mutableStateOf(false) }
    var correct by remember { mutableStateOf(-1) }
    var explanation by remember { mutableStateOf("") }
    val valid = question.isNotBlank() && options.count { it.isNotBlank() } >= 2 && (!quiz || options.getOrNull(correct)?.isNotBlank() == true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { WindowBlurBehind();  Text("Новый опрос") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(question, { question = it.take(255) }, label = { Text("Вопрос") })
                options.forEachIndexed { i, o ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // quiz: the right answer is marked with the radio button
                        if (quiz) androidx.compose.material3.RadioButton(selected = correct == i, onClick = { correct = i })
                        OutlinedTextField(o, { options[i] = it.take(100) }, label = { Text("Вариант ${i + 1}") }, singleLine = true, modifier = Modifier.weight(1f))
                        if (options.size > 2) MorphIconButton(Icons.Outlined.Close, "Убрать", { options.removeAt(i); if (correct == i) correct = -1 else if (correct > i) correct-- })
                    }
                }
                if (options.size < 10) QTextButton(onClick = { options.add("") }) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Добавить вариант") }
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Анонимный", Modifier.weight(1f)); QSwitch(anonymous) { anonymous = it } }
                if (!quiz) Row(verticalAlignment = Alignment.CenterVertically) { Text("Несколько ответов", Modifier.weight(1f)); QSwitch(multiple) { multiple = it } }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Викторина")
                        Text("Один правильный ответ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    QSwitch(quiz) { quiz = it; if (it) multiple = false }
                }
                if (quiz) OutlinedTextField(explanation, { explanation = it.take(200) }, label = { Text("Пояснение (необязательно)") })
            }
        },
        confirmButton = {
            QTextButton(enabled = valid, onClick = {
                onSend(TdApi.InputMessagePoll().apply {
                    this.question = TdApi.FormattedText(question.trim(), arrayOf())
                    this.options = options.filter { it.isNotBlank() }.map { TdApi.InputPollOption(TdApi.FormattedText(it.trim(), arrayOf()), null) }.toTypedArray()
                    description = TdApi.FormattedText("", arrayOf())
                    isAnonymous = anonymous
                    allowsMultipleAnswers = multiple && !quiz
                    countryCodes = arrayOf()
                    // the right answer's index among the options actually sent (blank ones are dropped)
                    val correctIdx = options.take(correct.coerceAtLeast(0)).count { it.isNotBlank() }
                    type = if (quiz) TdApi.InputPollTypeQuiz(intArrayOf(correctIdx), TdApi.FormattedText(explanation.trim(), arrayOf()), null)
                        else TdApi.InputPollTypeRegular(false)
                })
            }) { Text("Отправить") }
        },
        dismissButton = { QTextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

// ------------------------------------------------------------------ add contact

@Composable
fun AddContactDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { WindowBlurBehind();  Text("Новый контакт") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(first, { first = it }, label = { Text("Имя") }, singleLine = true)
                OutlinedTextField(last, { last = it }, label = { Text("Фамилия (необязательно)") }, singleLine = true)
                OutlinedTextField(phone, { phone = "+" + it.filter(Char::isDigit) }, label = { Text("Телефон") }, singleLine = true)
            }
        },
        confirmButton = {
            QTextButton(enabled = first.isNotBlank() && phone.length > 5, onClick = {
                Td.send(TdApi.ImportContacts(arrayOf(TdApi.ImportedContact(phone, first.trim(), last.trim(), TdApi.FormattedText("", arrayOf()))))) { r ->
                    val id = (r as? TdApi.ImportedContacts)?.userIds?.firstOrNull() ?: 0L
                    if (id == 0L) Toast.makeText(ctx, "Этого номера нет в Telegram", Toast.LENGTH_SHORT).show()
                    else Td.send(TdApi.CreatePrivateChat(id, false)) { c -> if (c is TdApi.Chat) Nav.push(Route.Chat(c.id)) }
                }
                onDismiss()
            }) { Text("Добавить") }
        },
        dismissButton = { QTextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

// ------------------------------------------------------------------ group / channel info

/** members, invite link and edit for groups & channels (shown inside the profile) */
@Composable
fun GroupInfo(chat: TdApi.Chat) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var members by remember { mutableStateOf<List<TdApi.ChatMember>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var link by remember { mutableStateOf<String?>(null) }
    var desc by remember { mutableStateOf("") }
    var edit by remember { mutableStateOf(false) }
    val t = chat.type
    val status = when (t) {
        is TdApi.ChatTypeSupergroup -> TdStore.supergroups[t.supergroupId]?.status
        is TdApi.ChatTypeBasicGroup -> TdStore.basicGroups[t.basicGroupId]?.status
        else -> null
    }
    val admin = status is TdApi.ChatMemberStatusCreator || status is TdApi.ChatMemberStatusAdministrator
    var canStats by remember { mutableStateOf(false) }
    LaunchedEffect(chat.id) {
        when (t) {
            is TdApi.ChatTypeBasicGroup -> Td.callOrNull(TdApi.GetBasicGroupFullInfo(t.basicGroupId))?.let { f ->
                members = f.members.toList(); total = f.members.size; link = f.inviteLink?.inviteLink; desc = f.description
            }
            is TdApi.ChatTypeSupergroup -> {
                val f = Td.callOrNull(TdApi.GetSupergroupFullInfo(t.supergroupId))
                link = f?.inviteLink?.inviteLink; desc = f?.description.orEmpty(); total = f?.memberCount ?: 0
                canStats = f?.canGetStatistics == true
                if (f?.canGetMembers == true && !t.isChannel) {
                    members = Td.callOrNull(TdApi.GetSupergroupMembers(t.supergroupId, null, 0, 200))?.members?.toList().orEmpty()
                }
            }
            else -> Unit
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (admin) Surface(onClick = { edit = true }, shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 4.dp), color = cs.surfaceContainerLow) {
            ListItem(headlineContent = { Text("Изменить название и описание") }, leadingContent = { RowIcon(Icons.Outlined.Add, 0) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent))
        }
        val l = link
        Surface(
            onClick = {
                if (l != null) copyText(ctx, l, "Ссылка скопирована")
                else if (admin) Td.send(TdApi.CreateChatInviteLink(chat.id, "", 0, 0, false)) { r -> if (r is TdApi.ChatInviteLink) { link = r.inviteLink; copyText(ctx, r.inviteLink, "Ссылка создана и скопирована") } }
            },
            shape = RoundedCornerShape(if (admin) 4.dp else 20.dp, if (admin) 4.dp else 20.dp, 20.dp, 20.dp), color = cs.surfaceContainerLow,
        ) {
            ListItem(
                headlineContent = { Text(l ?: if (admin) "Создать ссылку-приглашение" else "Ссылки нет", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text("Ссылка-приглашение") },
                leadingContent = { RowIcon(Icons.Outlined.Link) },
                trailingContent = { if (l != null) Icon(Icons.Outlined.ContentCopy, "Копировать") },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
        if (admin && t !is TdApi.ChatTypePrivate) {
            // admin tools, one segmented container (links & requests for every chat, member rights for groups)
            val pending = chat.pendingJoinRequests?.totalCount ?: 0
            val rows = buildList {
                add(AdminRow("Фото ${if (TdStore.isChannel(chat)) "канала" else "группы"}", "Выбрать новое из галереи", Icons.Outlined.AddAPhoto) { AdminTools.photoFor = chat.id })
                add(AdminRow(if (TdStore.isChannel(chat)) "Добавить подписчиков" else "Добавить участников", "Из ваших контактов", Icons.Outlined.PersonAdd) { AdminTools.addTo = chat.id })
                if (canStats) add(AdminRow("Статистика", "Подписчики, просмотры, рост", Icons.Outlined.BarChart) { Nav.push(Route.Page("stats:${chat.id}")) })
                add(AdminRow("Ссылки-приглашения", "Создать, отозвать, лимиты и сроки", Icons.Outlined.Link) { Nav.push(Route.Page("links:${chat.id}")) })
                if (pending > 0) add(AdminRow("Заявки на вступление", Texts.plural(pending, "заявка", "заявки", "заявок"), Icons.Outlined.PersonAdd) { Nav.push(Route.Page("requests:${chat.id}")) })
                if (!TdStore.isChannel(chat)) add(AdminRow("Разрешения", "Что могут участники, медленный режим", Icons.Outlined.Lock) { Nav.push(Route.Page("perm:${chat.id}")) })
                add(AdminRow("Автоудаление сообщений", if (chat.messageAutoDeleteTime > 0) autoDeleteLabel(chat.messageAutoDeleteTime) else "Выключено", Icons.Outlined.Timer) { Tg.autoDelete = chat })
            }
            Spacer(Modifier.height(10.dp))
            rows.forEachIndexed { i, (title, sub, icon, act) ->
                Box(Modifier.segment(i, rows.size)) {
                    ListItem(
                        headlineContent = { Text(title) }, supportingContent = { Text(sub) }, leadingContent = { RowIcon(icon) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable(onClick = act),
                    )
                }
            }
        }
        if (members.isNotEmpty()) {
            Text(
                "Участники · $total", style = MaterialTheme.typography.titleSmall, color = cs.primary,
                modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 8.dp),
            )
            members.take(200).forEachIndexed { i, mem ->
                val top = if (i == 0) 20.dp else 4.dp
                val bottom = if (i == members.lastIndex.coerceAtMost(199)) 20.dp else 4.dp
                val uid = (mem.memberId as? TdApi.MessageSenderUser)?.userId
                val u = uid?.let { TdStore.users[it] }
                val role = when (mem.status) {
                    is TdApi.ChatMemberStatusCreator -> "владелец"
                    is TdApi.ChatMemberStatusAdministrator -> "администратор"
                    else -> null
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(top, top, bottom, bottom)).background(cs.surfaceContainerLow)
                        // admins get member actions (remove, ban); everyone else opens the profile
                        .clickable(enabled = uid != null) { if (admin) Tg.member = chat to mem else Td.send(TdApi.CreatePrivateChat(uid!!, false)) { r -> if (r is TdApi.Chat) Nav.push(Route.Profile(r.id)) } }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SenderAvatar(mem.memberId, 40.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(TdStore.senderName(mem.memberId).ifBlank { "Участник" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(Texts.status(u), style = MaterialTheme.typography.bodySmall, color = if (Texts.isOnline(u)) cs.primary else cs.onSurfaceVariant)
                    }
                    if (role != null) Text(role, style = MaterialTheme.typography.labelMedium, color = cs.primary)
                }
            }
        }
    }
    if (edit) {
        var title by remember { mutableStateOf(chat.title) }
        var d by remember { mutableStateOf(desc) }
        AlertDialog(
            onDismissRequest = { edit = false },
            title = { WindowBlurBehind();  Text("Изменить") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("Название") }, singleLine = true)
                    OutlinedTextField(d, { d = it }, label = { Text("Описание") })
                }
            },
            confirmButton = {
                QTextButton(enabled = title.isNotBlank(), onClick = {
                    if (title != chat.title) Td.send(TdApi.SetChatTitle(chat.id, title.trim()))
                    if (d != desc) { Td.send(TdApi.SetChatDescription(chat.id, d.trim())); desc = d }
                    edit = false
                }) { Text("Сохранить") }
            },
            dismissButton = { QTextButton(onClick = { edit = false }) { Text("Отмена") } },
        )
    }
}

// ------------------------------------------------------------------ bot inline keyboards & link previews

@Composable
fun InlineKeyboard(m: TdApi.Message, maxWidth: androidx.compose.ui.unit.Dp) {
    val kb = m.replyMarkup as? TdApi.ReplyMarkupInlineKeyboard ?: return
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    // the bot's buttons as one connected group (M3): rows 2 dp apart, big outer corners, small touching ones
    Column(Modifier.widthIn(max = maxWidth).padding(top = 3.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        kb.rows.forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEachIndexed { c, b ->
                    ConnectedTile(
                        Seat(r, kb.rows.size, c, row.size), onClick = { pressInline(ctx, m, b) },
                        modifier = Modifier.weight(1f).heightIn(min = 40.dp), big = 18.dp, small = 5.dp,
                        color = cs.secondaryContainer.copy(alpha = 0.9f), contentColor = cs.onSecondaryContainer,
                    ) {
                        Text(b.text, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
                    }
                }
            }
        }
    }
}

private fun pressInline(ctx: Context, m: TdApi.Message, b: TdApi.InlineKeyboardButton) {
    when (val t = b.type) {
        is TdApi.InlineKeyboardButtonTypeUrl -> Opener.url(ctx, t.url)
        is TdApi.InlineKeyboardButtonTypeCallback -> Td.send(TdApi.GetCallbackQueryAnswer(m.chatId, m.id, TdApi.CallbackQueryPayloadData(t.data))) { r ->
            if (r is TdApi.CallbackQueryAnswer) {
                if (r.text.isNotBlank()) Toast.makeText(ctx, r.text, if (r.showAlert) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
                if (r.url.isNotBlank()) Opener.url(ctx, r.url)
            }
        }
        else -> Toast.makeText(ctx, "Эта кнопка пока не поддерживается", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun LinkPreviewCard(p: TdApi.LinkPreview, fg: Color, modifier: Modifier) {
    val ctx = LocalContext.current
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(fg.copy(alpha = 0.08f)).clickable { Opener.url(ctx, p.url) }
            .padding(end = 10.dp),
    ) {
        Box(Modifier.width(3.dp).height(64.dp).background(MaterialTheme.colorScheme.primary))
        Column(Modifier.padding(start = 10.dp, top = 6.dp, bottom = 6.dp)) {
            if (p.siteName.isNotBlank()) Text(p.siteName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1)
            if (p.title.isNotBlank()) Text(p.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val d = p.description?.text.orEmpty()
            if (d.isNotBlank()) Text(d, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.8f), maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}


private data class AdminRow(val title: String, val sub: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val act: () -> Unit)
