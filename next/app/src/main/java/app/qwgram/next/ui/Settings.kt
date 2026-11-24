package app.qwgram.next.ui

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Dock
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Interests
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import app.qwgram.next.BuildConfig
import app.qwgram.next.KeepAliveService
import app.qwgram.next.Prefs
import app.qwgram.next.td.Td
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.Texts
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloat
import org.drinkless.tdlib.TdApi

// ------------------------------------------------------------------ settings tab

/**
 * Settings header card: tonal gradient with a slowly turning expressive shape behind, big avatar with a ring,
 * name, @username / phone, Premium mark; tap opens your profile, the pencil edits the account.
 */
/** our own sessions read "qwgram <version>" whatever title the API id was registered with; old builds sent "qwgram x.y" as the version */
private fun sessionTitle(s: TdApi.Session): String =
    if (s.apiId == BuildConfig.TG_APP_ID) "qwgram " + s.applicationVersion.removePrefix("qwgram ").trim()
    else (s.applicationName + " " + s.applicationVersion).trim()

@Composable
private fun ProfileCard(me: TdApi.User?) {
    val cs = MaterialTheme.colorScheme
    MorphBox(
        onClick = { Nav.push(Route.Me) }, rest = 32.dp, pressed = 32.dp, squish = 0.985f,
        color = cs.primaryContainer, contentColor = cs.onPrimaryContainer, contentAlignment = Alignment.TopStart,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp.rs())).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(cs.primaryContainer, cs.tertiaryContainer)))) {
            // the decorative shape eases into place once (no endless rotation: it kept the screen redrawing)
            val turn = remember { androidx.compose.animation.core.Animatable(-40f) }
            LaunchedEffect(Unit) { turn.animateTo(20f, androidx.compose.animation.core.spring(0.7f, 60f)) }
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 60.dp, y = (-50).dp).size(200.dp)
                    .graphicsLayer { rotationZ = if (Prefs.motion.value == "off") 20f else turn.value; alpha = 0.16f }
                    .clip(androidx.compose.material3.MaterialShapes.Cookie12Sided.asShape()).background(cs.primary),
            )
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(84.dp).clip(CircleShape).background(cs.surface.copy(alpha = 0.6f)).padding(4.dp)) { UserAvatar(me, 76.dp) }
                    Spacer(Modifier.weight(1f))
                    MorphIconButton(Icons.Outlined.Edit, "Изменить", { Nav.push(Route.Page("account")) }, color = cs.surface.copy(alpha = 0.55f), tint = cs.onSurface)
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(me?.let { TdStore.fullName(it) } ?: "", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f, fill = false))
                    if (me?.isPremium == true) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.Star, "Premium", tint = cs.primary, modifier = Modifier.size(22.dp))
                    }
                }
                val line = listOfNotNull(
                    me?.usernames?.activeUsernames?.firstOrNull()?.let { "@$it" },
                    if (Prefs.hidePhone.value) null else me?.phoneNumber?.takeIf { it.isNotBlank() }?.let { "+$it" },
                ).joinToString(" · ")
                if (line.isNotEmpty()) Text(line, style = MaterialTheme.typography.bodyLarge, color = cs.onPrimaryContainer.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun SettingsHome(pad: PaddingValues) {
    val me = TdStore.me
    var logout by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var headerH by remember { mutableStateOf(96.dp) }
    Box(Modifier.fillMaxSize()) {
        FadeHeader("Настройки", Modifier.zIndex(1f).onSizeChanged { headerH = with(density) { it.height.toDp() } }, onBack = null) {
            SearchButton()
        }
        LazyColumn(
            Modifier.fillMaxSize().blurSource(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = headerH - headerOverlap(), bottom = pad.calculateBottomPadding() + 32.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item { ProfileCard(me) }
            item { Spacer(Modifier.height(10.dp)) }
            item { QuickTiles2() }
            group("Аккаунт") {
                nav("Мой профиль", "Как вас видят другие", Icons.Outlined.AccountCircle, 0) { Nav.push(Route.Me) }
                nav("Мой аккаунт", "Имя, о себе, @username", Icons.Outlined.Person) { Nav.push(Route.Page("account")) }
                nav("Устройства", "Активные сеансы", Icons.Outlined.Devices) { Nav.push(Route.Page("sessions")) }
            }
            group("Чаты") {
                nav("Уведомления и звуки", "Чаты, группы, каналы", Icons.Outlined.Notifications) { Nav.push(Route.Page("notifications")) }
                nav("Конфиденциальность", "Кто видит номер, время, фото", Icons.Outlined.Lock) { Nav.push(Route.Page("privacy")) }
                nav("Данные и память", "Кэш, автозагрузка", Icons.Outlined.DataUsage) { Nav.push(Route.Page("data")) }
                nav("Папки с чатами", "Создать и настроить", Icons.Outlined.FolderOpen) { Nav.push(Route.Page("folders")) }
                nav("Заблокированные", "Пользователи и боты", Icons.Outlined.Block) { Nav.push(Route.Page("blocked")) }
            }
            group("Сеть") {
                nav("Прокси", "SOCKS5, MTProto, HTTP", Icons.Outlined.VpnKey) { Nav.push(Route.Page("proxy")) }
            }
            group {
                nav("О qwgram", "Версия ${BuildConfig.VERSION_NAME}", Icons.Outlined.Info) { Nav.push(Route.Page("about")) }
                action("Выйти из аккаунта", null, Icons.Outlined.Logout, danger = true) { logout = true }
            }
        }
    }
    if (logout) AlertDialog(
        onDismissRequest = { logout = false },
        icon = { Icon(Icons.Outlined.Logout, null) },
        title = { WindowBlurBehind();  Text("Выйти из аккаунта?") },
        text = { Text("Чаты останутся в облаке Telegram. Чтобы вернуться, понадобится код подтверждения.") },
        confirmButton = { QTextButton(onClick = { logout = false; Td.send(TdApi.LogOut()) }) { Text("Выйти", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { QTextButton(onClick = { logout = false }) { Text("Отмена") } },
    )
}

@Composable
fun SettingsPage(id: String) {
    when {
        id.startsWith("folder:") -> { FolderEditPage(id.removePrefix("folder:").toIntOrNull()); return }
        id.startsWith("sched:") -> { ScheduledPage(id.removePrefix("sched:").toLong()); return }
        id.startsWith("compose:") -> { NewMessagePage(id.removePrefix("compose:")); return }
    }
    when (id) {
        "appearance" -> AppearanceHub2()
        "ap-theme" -> ThemePage()
        "ap-shapes" -> ShapesPage()
        "ap-list" -> ListPage2()
        "ap-folders" -> FoldersStylePage()
        "ap-chat" -> ChatStylePage2()
        "ap-nav" -> NavPage2()
        "ap-motion" -> MotionPage2()
        "ap-text" -> TextPage2()
        "ap-emoji" -> EmojiPage()
        "ap-blur" -> BlurPage()
        "features" -> FeaturesPage2()
        "account" -> AccountPage()
        "notifications" -> NotificationsPage()
        "privacy" -> PrivacyPage()
        "data" -> DataPage()
        "sessions" -> SessionsPage()
        "folders" -> FoldersListPage()
        "proxy" -> ProxyPage()
        "about" -> AboutPage()
        "compose" -> NewMessagePage()
        "blocked" -> BlockedPage()
        else -> PageScaffold("Настройки") {}
    }
}

@Composable
private fun ResetAction() {
    var ask by remember { mutableStateOf(false) }
    MorphIconButton(Icons.Outlined.RestartAlt, "Сбросить", { ask = true })
    if (ask) AlertDialog(
        onDismissRequest = { ask = false },
        icon = { Icon(Icons.Outlined.RestartAlt, null) },
        title = { WindowBlurBehind();  Text("Сбросить оформление?") },
        text = { Text("Цвета, формы, панели и анимации вернутся к стандартным.") },
        confirmButton = { QTextButton(onClick = { Prefs.resetAppearance(); ask = false }) { Text("Сбросить") } },
        dismissButton = { QTextButton(onClick = { ask = false }) { Text("Отмена") } },
    )
}

@Composable
fun Preview(list: Boolean = true, chat: Boolean = true, framed: Boolean = true) {
    val cs = MaterialTheme.colorScheme
    val now = remember { (System.currentTimeMillis() / 1000).toInt() }
    val demo = remember {
        TdApi.Chat().apply {
            id = 1; title = "Аня"; type = TdApi.ChatTypePrivate(0); positions = arrayOf(); unreadCount = 2
            notificationSettings = TdApi.ChatNotificationSettings()
            lastMessage = TdApi.Message().apply { id = 3; date = now; content = TdApi.MessageText().apply { text = TdApi.FormattedText("Как тебе новый вид?", arrayOf()) }; senderId = TdApi.MessageSenderUser(0) }
        }
    }
    fun msg(id: Long, out: Boolean, t: String) = TdApi.Message().apply {
        this.id = id; isOutgoing = out; date = now; senderId = TdApi.MessageSenderUser(0); chatId = 1
        content = TdApi.MessageText().apply { text = TdApi.FormattedText(t, arrayOf()) }
    }
    val msgs = remember { listOf(msg(1, false, "Привет! 👋") to Grp.Top, msg(2, false, "Как тебе новый вид?") to Grp.Bottom, msg(3, true, "Очень плавно ✨") to Grp.Single) }
    val inner: @Composable () -> Unit = {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
            val area = maxWidth - 16.dp
            Column(Modifier.androidx_blockTouches().clip(RoundedCornerShape(20.dp))) {
                if (list) Box(Modifier.padding(vertical = 4.dp)) { ChatRow(demo, null) {} }
                if (chat) Box(Modifier.fillMaxWidth().background(cs.surfaceContainerLow).padding(horizontal = 8.dp, vertical = 10.dp)) {
                    androidx.compose.runtime.CompositionLocalProvider(LocalBubbleArea provides area) {
                        Column {
                            msgs.forEach { (m, g) -> MessageItem(m, g, demo, false, null, onLong = {}, onDouble = {}, onSwipeReply = {}, onPhoto = {}) }
                        }
                    }
                }
            }
        }
    }
    if (!framed) { inner(); return }
    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)) {
        Text("Предпросмотр", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
        Surface(shape = RoundedCornerShape(28.dp), color = cs.surface, border = androidx.compose.foundation.BorderStroke(1.dp, cs.outlineVariant), modifier = Modifier.fillMaxWidth()) { inner() }
    }
}

/** preview is display-only: swallow every touch before the real components see it */
private fun Modifier.androidx_blockTouches(): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }
    },
)

private val SEEDS = listOf(0xFF6750A4, 0xFF3D6CB5, 0xFF0B6B5E, 0xFF3F6B2A, 0xFF6B7A10, 0xFF8C5A00, 0xFFB4502C, 0xFFB3261E, 0xFF984061, 0xFF4A55A2, 0xFF006874, 0xFF5B6170).map { it.toInt() }

@Composable
private fun ThemePage() {
    var hex by remember { mutableStateOf(false) }
    PageScaffold("Тема и цвета", actions = { ResetAction() }) {
        item { ThemePreview() }
        group("Режим") {
            segmented("Тема", Prefs.themeMode, listOf("light" to "Светлая", "dark" to "Тёмная", "system" to "Системная"))
            switch("Чёрная AMOLED-тема", Prefs.amoled, "Настоящий чёрный фон в тёмной теме")
        }
        group("Цвет", footer = "Вся палитра строится из одного цвета по алгоритму HCT — так же, как Material You в Android.") {
            if (Build.VERSION.SDK_INT >= 31) switch("Цвета обоев (Material You)", Prefs.dynamicColor, "Брать акцент из обоев устройства")
            custom {
                Text("Акцентный цвет", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                Swatches(enabled = !Prefs.dynamicColor.value) { hex = true }
            }
            pick("Стиль палитры", Prefs.paletteStyle, listOf(0 to "Тональная", 1 to "Яркая", 2 to "Выразительная", 3 to "Нейтральная", 4 to "Монохром"))
            segmented("Контрастность", Prefs.contrast, listOf(0 to "Обычная", 1 to "Средняя", 2 to "Высокая"))
        }
    }
    if (hex) HexDialog { hex = false }
}

@Composable
private fun Swatches(enabled: Boolean, onCustom: () -> Unit) {
    val dark = LocalDark.current
    val variant = Prefs.paletteStyle.value
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SEEDS.forEach { seed ->
            val p = remember(seed, variant) { SeedScheme.palettes(seed, variant) }
            val sel = Prefs.seed.value == seed && enabled
            val radius by androidx.compose.animation.core.animateDpAsState(if (sel) 14.dp else 24.dp, Springs.fastSpatial(), label = "sw")
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(radius)).clickable(enabled = enabled) { Prefs.seed.value = seed; Prefs.dynamicColor.value = false },
            ) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().weight(1f).background(Color(p.primary.tone(if (dark) 80.0 else 40.0))))
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        Box(Modifier.weight(1f).fillMaxSize().background(Color(p.secondary.tone(70.0))))
                        Box(Modifier.weight(1f).fillMaxSize().background(Color(p.tertiary.tone(if (dark) 70.0 else 60.0))))
                    }
                }
                if (sel) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
            }
        }
        Surface(onClick = onCustom, enabled = enabled, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Colorize, "Свой цвет") }
        }
    }
}

@Composable
private fun HexDialog(onDismiss: () -> Unit) {
    var v by remember { mutableStateOf("#%06X".format(Prefs.seed.value and 0xFFFFFF)) }
    val ok = Regex("^#?[0-9a-fA-F]{6}$").matches(v.trim())
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Colorize, null) },
        title = { WindowBlurBehind();  Text("Свой цвет") },
        text = { OutlinedTextField(v, { v = it.take(7) }, label = { Text("HEX, например #6750A4") }, isError = !ok, singleLine = true) },
        confirmButton = {
            QTextButton(enabled = ok, onClick = {
                Prefs.seed.value = (0xFF000000 or v.trim().removePrefix("#").toLong(16)).toInt(); Prefs.dynamicColor.value = false; onDismiss()
            }) { Text("Применить") }
        },
        dismissButton = { QTextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun ShapesPage() = PageScaffold("Формы и аватарки", actions = { ResetAction() }) {
    item { ShapePicker() }
    group("Размеры") {
        sliderInt("Аватарки в списке чатов", Prefs.avatarSize, 40..64, 2) { "$it dp" }
        sliderFloat("Скругление всего интерфейса", Prefs.cornerScale, 0f..2f, 19) { if (it < 0.05f) "Квадратное" else "${(it * 100).toInt()}%" }
    }
    group("Детали") {
        switch("Значок «в сети»", Prefs.showOnlineDot)
    }
}

/** big avatar morphing between the chosen shapes on the M3 spatial spring + the grid of shapes */
@Composable
private fun ShapePicker() {
    val cs = MaterialTheme.colorScheme
    val current = Prefs.avatarShape.value
    var from by remember { mutableStateOf(current) }
    var to by remember { mutableStateOf(current) }
    val progress = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val morph = remember(from, to) { Morph(AvatarShapes.polygon(from), AvatarShapes.polygon(to)) }
    Surface(shape = RoundedCornerShape(28.dp), color = cs.surfaceContainerLow, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(112.dp).clip(MorphShape(morph, progress.value)).background(cs.primaryContainer), contentAlignment = Alignment.Center) {
                Text(initials(TdStore.me?.let { TdStore.fullName(it) } ?: "qw"), style = MaterialTheme.typography.headlineMedium, color = cs.onPrimaryContainer)
            }
            Spacer(Modifier.height(10.dp))
            Text(AvatarShapes.title(current), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                AvatarShapes.all.forEach { (key, title, _) ->
                    val sel = key == current
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(if (sel) 22.dp else 14.dp))
                            .background(if (sel) cs.secondaryContainer else Color.Transparent)
                            .clickable {
                                if (key == current) return@clickable
                                from = current; to = key
                                Prefs.avatarShape.value = key
                                scope.launch { progress.snapTo(0f); progress.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 380f)) }
                            }
                            .padding(8.dp),
                    ) {
                        Box(Modifier.fillMaxSize().clip(shapeFor(key)).background(if (sel) cs.primary else cs.outlineVariant))
                    }
                }
            }
        }
    }
}

@Composable
private fun FoldersStylePage() = PageScaffold("Папки") {
    group("Стиль", footer = "«Группа кнопок» — соединённые кнопки из Material 3 Expressive: выбранная папка становится полностью скруглённой.") {
        pick("Вид папок", Prefs.folderStyle, listOf("group" to "Группа кнопок (Expressive)", "tabs" to "Вкладки, как в Telegram", "chips" to "Фильтр-чипы"))
        switch("Счётчики непрочитанных", Prefs.folderCounters)
    }
    group("Поведение") {
        switch("Смена папок свайпом", Prefs.folderSwipe, "Листайте список влево и вправо")
        switch("Скрыть «Все чаты»", Prefs.hideAllChats)
    }
}

@Composable
private fun AccountPage() {
    val me = TdStore.me
    var editName by remember { mutableStateOf(false) }
    var editBio by remember { mutableStateOf(false) }
    var editUser by remember { mutableStateOf(false) }
    var bio by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    LaunchedEffect(me?.id) { me?.let { Td.callOrNull(TdApi.GetUserFullInfo(it.id))?.let { f -> bio = f.bio?.text.orEmpty() } } }
    PageScaffold("Мой аккаунт") {
        group {
            value("Имя", me?.let { TdStore.fullName(it) } ?: "", onClick = { editName = true })
            value("О себе", bio.ifBlank { "Не указано" }, onClick = { editBio = true })
            value("Телефон", if (Prefs.hidePhone.value) "Скрыт" else "+" + (me?.phoneNumber ?: ""))
            value("Имя пользователя", me?.usernames?.editableUsername?.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "Нет", onClick = { editUser = true })
            value("ID", me?.id?.toString() ?: "")
        }
    }
    if (editName) TextDialog("Имя", listOf("Имя" to (me?.firstName ?: ""), "Фамилия" to (me?.lastName ?: ""))) { v ->
        editName = false
        if (v != null) Td.send(TdApi.SetName(v[0], v[1]))
    }
    if (editUser) TextDialog("Имя пользователя", listOf("username, без @" to (me?.usernames?.editableUsername ?: ""))) { v ->
        editUser = false
        if (v != null) Td.send(TdApi.SetUsername(v[0].removePrefix("@"))) { r ->
            if (r is TdApi.Error) Toast.makeText(ctx, if (r.message.contains("OCCUPIED")) "Имя занято" else "Не удалось: ${r.message}", Toast.LENGTH_SHORT).show()
        }
    }
    if (editBio) TextDialog("О себе", listOf("О себе" to bio)) { v ->
        editBio = false
        if (v != null) { Td.send(TdApi.SetBio(v[0])); bio = v[0] }
    }
}

@Composable
private fun TextDialog(title: String, fields: List<Pair<String, String>>, onDone: (List<String>?) -> Unit) {
    val values = remember { fields.map { mutableStateOf(it.second) } }
    AlertDialog(
        onDismissRequest = { onDone(null) },
        title = { WindowBlurBehind();  Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                fields.forEachIndexed { i, (label, _) -> OutlinedTextField(values[i].value, { values[i].value = it }, label = { Text(label) }) }
            }
        },
        confirmButton = { QTextButton(onClick = { onDone(values.map { it.value.trim() }) }) { Text("Сохранить") } },
        dismissButton = { QTextButton(onClick = { onDone(null) }) { Text("Отмена") } },
    )
}

@Composable
private fun NotificationsPage() {
    val scopes = listOf(
        Triple("Личные чаты", TdApi.NotificationSettingsScopePrivateChats() as TdApi.NotificationSettingsScope, 0),
        Triple("Группы", TdApi.NotificationSettingsScopeGroupChats(), 1),
        Triple("Каналы", TdApi.NotificationSettingsScopeChannelChats(), 2),
    )
    val settings = remember { mutableStateOf(arrayOfNulls<TdApi.ScopeNotificationSettings>(3)) }
    LaunchedEffect(Unit) {
        val arr = arrayOfNulls<TdApi.ScopeNotificationSettings>(3)
        scopes.forEach { (_, s, i) -> arr[i] = Td.callOrNull(TdApi.GetScopeNotificationSettings(s)) }
        settings.value = arr
    }
    PageScaffold("Уведомления и звуки") {
        group("Уведомления из чатов") {
            scopes.forEach { (title, scope, i) ->
                val s = settings.value[i]
                switchState(title, s != null && s.muteFor == 0, if (s == null) "Загрузка…" else if (s.muteFor == 0) "Включены" else "Выключены") { on ->
                    val cur = settings.value[i] ?: return@switchState
                    cur.muteFor = if (on) 0 else Int.MAX_VALUE
                    Td.send(TdApi.SetScopeNotificationSettings(scope, cur))
                    settings.value = settings.value.copyOf().also { it[i] = cur }
                }
            }
        }
        group("В приложении") {
            switch("Текст сообщения в уведомлении", Prefs.notifyPreview)
        }
    }
}

@Composable
private fun PrivacyPage() {
    val items = listOf(
        "Номер телефона" to TdApi.UserPrivacySettingShowPhoneNumber() as TdApi.UserPrivacySetting,
        "Время захода" to TdApi.UserPrivacySettingShowStatus(),
        "Фото профиля" to TdApi.UserPrivacySettingShowProfilePhoto(),
        "О себе" to TdApi.UserPrivacySettingShowBio(),
        "Пересылка сообщений" to TdApi.UserPrivacySettingShowLinkInForwardedMessages(),
        "Звонки" to TdApi.UserPrivacySettingAllowCalls(),
        "Голосовые сообщения" to TdApi.UserPrivacySettingAllowPrivateVoiceAndVideoNoteMessages(),
        "Приглашения в группы" to TdApi.UserPrivacySettingAllowChatInvites(),
    )
    val values = remember { mutableStateOf(Array(items.size) { -1 }) }
    var editing by remember { mutableStateOf(-1) }
    LaunchedEffect(Unit) {
        val arr = Array(items.size) { -1 }
        items.forEachIndexed { i, (_, s) ->
            val r = Td.callOrNull(TdApi.GetUserPrivacySettingRules(s))
            arr[i] = when {
                r == null -> -1
                r.rules.any { it is TdApi.UserPrivacySettingRuleAllowAll } -> 0
                r.rules.any { it is TdApi.UserPrivacySettingRuleAllowContacts } -> 1
                else -> 2
            }
        }
        values.value = arr
    }
    val names = listOf("Все", "Мои контакты", "Никто")
    PageScaffold("Конфиденциальность") {
        group("Кто видит и может") {
            items.forEachIndexed { i, (title, _) ->
                value(title, names.getOrNull(values.value[i]) ?: "…", onClick = { editing = i })
            }
        }
        group("Безопасность") {
            nav("Заблокированные", null, Icons.Outlined.Block) { Nav.push(Route.Page("blocked")) }
            nav("Устройства", "Активные сеансы", Icons.Outlined.Devices) { Nav.push(Route.Page("sessions")) }
        }
    }
    if (editing >= 0) {
        val i = editing
        RadioDialog(items[i].first, names.indices.map { it to names[it] }, values.value[i], { v ->
            val rule: TdApi.UserPrivacySettingRule = when (v) {
                0 -> TdApi.UserPrivacySettingRuleAllowAll()
                1 -> TdApi.UserPrivacySettingRuleAllowContacts()
                else -> TdApi.UserPrivacySettingRuleRestrictAll()
            }
            Td.send(TdApi.SetUserPrivacySettingRules(items[i].second, TdApi.UserPrivacySettingRules(arrayOf(rule))))
            values.value = values.value.copyOf().also { it[i] = v }
            editing = -1
        }) { editing = -1 }
    }
}

@Composable
private fun DataPage() {
    var stats by remember { mutableStateOf<TdApi.StorageStatisticsFast?>(null) }
    var clearing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { stats = Td.callOrNull(TdApi.GetStorageStatisticsFast()) }
    PageScaffold("Данные и память") {
        group("Автозагрузка медиа", footer = "Фото и небольшие файлы скачиваются сами, когда вы открываете чат.") {
            switch("Через мобильную сеть", Prefs.autoDlMobile) { on -> applyAutoDownload(TdApi.NetworkTypeMobile(), on, 1) }
            switch("Через Wi-Fi", Prefs.autoDlWifi) { on -> applyAutoDownload(TdApi.NetworkTypeWiFi(), on, 2) }
        }
        group("Память устройства") {
            val s = stats
            value("Файлы и медиа", s?.let { Texts.size(it.filesSize) } ?: "…")
            value("База данных", s?.let { Texts.size(it.databaseSize) } ?: "…")
            action(if (clearing) "Очистка…" else "Очистить кэш", "Файлы снова скачаются из облака при открытии", Icons.Outlined.DataUsage) {
                if (clearing) return@action
                clearing = true
                scope.launch {
                    Td.callOrNull(TdApi.OptimizeStorage().apply { size = 0; ttl = 0; count = 0; immunityDelay = 0; returnDeletedFileStatistics = false; chatLimit = 0 })
                    stats = Td.callOrNull(TdApi.GetStorageStatisticsFast())
                    clearing = false
                    Toast.makeText(ctx, "Кэш очищен", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
private fun SessionsPage() {
    var sessions by remember { mutableStateOf<List<TdApi.Session>>(emptyList()) }
    val scope = rememberCoroutineScope()
    suspend fun reload() { sessions = Td.callOrNull(TdApi.GetActiveSessions())?.sessions?.toList().orEmpty() }
    LaunchedEffect(Unit) { reload() }
    PageScaffold("Устройства") {
        val cur = sessions.firstOrNull { it.isCurrent }
        if (cur != null) group("Это устройство") {
            row { BaseRow(sessionTitle(cur), "${cur.deviceModel} · ${cur.location}".trim(' ', '·'), Icons.Outlined.Devices, 0) }
        }
        val others = sessions.filterNot { it.isCurrent }
        if (others.isNotEmpty()) {
            group {
                action("Завершить другие сеансы", null, Icons.Outlined.Logout, danger = true) {
                    scope.launch { Td.callOrNull(TdApi.TerminateAllOtherSessions()); reload() }
                }
            }
            group("Активные сеансы") {
                others.forEach { s ->
                    row {
                        BaseRow(sessionTitle(s), "${s.deviceModel} · ${s.location} · ${Texts.listDate(s.lastActiveDate)}", Icons.Outlined.Devices) {
                            QTextButton(onClick = { scope.launch { Td.callOrNull(TdApi.TerminateSession(s.id)); reload() } }) { Text("Завершить", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FoldersPage() = PageScaffold("Папки с чатами") {
    group("Ваши папки", footer = "Создавать и редактировать папки пока можно в официальном приложении — они синхронизируются.") {
        if (TdStore.folders.isEmpty()) row { BaseRow("Папок пока нет") }
        TdStore.folders.forEach { f -> row { BaseRow(f.name.text.text, null, Icons.Outlined.FolderOpen) } }
    }
    group {
        nav("Вид панели папок", null, Icons.Outlined.Palette) { Nav.push(Route.Page("ap-folders")) }
    }
}

@Composable
private fun AboutPage() = PageScaffold("О qwgram") {
    item {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            QwLogo(220.dp)
            Spacer(Modifier.height(12.dp))
            Text("qwgram", style = MaterialTheme.typography.headlineMedium)
            Text("Версия ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    group(footer = "qwgram — неофициальный клиент Telegram на Jetpack Compose и Material 3 Expressive. Работает через официальную библиотеку TDLib на серверах Telegram.") {
        value("Библиотека", "TDLib")
        value("Интерфейс", "Material 3 Expressive")
    }
}

private fun applyAutoDownload(type: TdApi.NetworkType, on: Boolean, preset: Int) {
    Td.send(TdApi.GetAutoDownloadSettingsPresets()) { r ->
        if (r !is TdApi.AutoDownloadSettingsPresets) return@send
        val s = if (preset == 2) r.high else r.medium
        s.isAutoDownloadEnabled = on
        Td.send(TdApi.SetAutoDownloadSettings(s, type))
    }
}

@Composable
private fun BlockedPage() {
    var list by remember { mutableStateOf<List<TdApi.MessageSender>?>(null) }
    val scope = rememberCoroutineScope()
    suspend fun reload() { list = Td.callOrNull(TdApi.GetBlockedMessageSenders(TdApi.BlockListMain(), 0, 100))?.senders?.toList().orEmpty() }
    LaunchedEffect(Unit) { reload() }
    PageScaffold("Заблокированные") {
        val l = list
        if (l == null) item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { LoadingIndicator() } }
        else group(footer = "Заблокированные не могут писать вам, звонить и видеть ваш статус.") {
            if (l.isEmpty()) row { BaseRow("Список пуст") }
            l.forEach { s ->
                row {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        SenderAvatar(s, 40.dp)
                        Spacer(Modifier.width(16.dp))
                        Text(TdStore.senderName(s).ifBlank { "Пользователь" }, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        QTextButton(onClick = { scope.launch { Td.callOrNull(TdApi.SetMessageSenderBlockList(s, null)); reload() } }) { Text("Разблокировать") }
                    }
                }
            }
        }
    }
}
