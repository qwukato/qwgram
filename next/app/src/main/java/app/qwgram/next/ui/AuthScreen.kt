package app.qwgram.next.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.qwgram.next.R
import app.qwgram.next.td.Td
import app.qwgram.next.td.TdException
import app.qwgram.next.td.TdStore
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloat
import org.drinkless.tdlib.TdApi

@Composable
fun AppRoot() {
    val auth = TdStore.auth
    AnimatedContent(
        targetState = when (auth) {
            null, is TdApi.AuthorizationStateWaitTdlibParameters, is TdApi.AuthorizationStateClosed, is TdApi.AuthorizationStateClosing -> 0
            is TdApi.AuthorizationStateReady -> 2
            is TdApi.AuthorizationStateLoggingOut -> 0
            else -> 1
        },
        transitionSpec = { fadeIn(Springs.effects()) togetherWith fadeOut(Springs.exit()) },
        label = "root",
    ) { s ->
        when (s) {
            0 -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) { LoadingIndicator() }
            1 -> AuthScreen()
            else -> NavHost { route -> RouteScreen(route) }
        }
    }
}

@Composable
fun RouteScreen(route: Route) {
    when (route) {
        Route.Home -> HomeScreen()
        is Route.Chat -> androidx.compose.runtime.key(route.chatId, route.topic, route.thread) { ChatScreen(route.chatId, route.topic, route.thread) }
        is Route.Profile -> ProfileScreen(route.chatId)
        Route.Me -> ProfileScreen(0L)
        Route.Search -> SearchScreen()
        Route.Archive -> ArchiveScreen()
        is Route.Page -> SettingsPage(route.id)
    }
}

@Composable
private fun AuthScreen() {
    val state = TdStore.auth
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var errorTick by remember { mutableStateOf(0) }
    var started by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    fun submit(f: TdApi.Function<*>) {
        busy = true
        error = null
        scope.launch {
            try {
                @Suppress("UNCHECKED_CAST")
                Td.call(f as TdApi.Function<TdApi.Object>)
            } catch (e: TdException) {
                error = humanError(e.error.message); errorTick++
            } catch (e: Exception) {
                error = e.message; errorTick++
            }
            busy = false
        }
    }
    val step = when (state) {
        is TdApi.AuthorizationStateWaitPhoneNumber -> if (started) 1 else 0
        is TdApi.AuthorizationStateWaitCode, is TdApi.AuthorizationStateWaitEmailCode, is TdApi.AuthorizationStateWaitOtherDeviceConfirmation -> 2
        is TdApi.AuthorizationStateWaitPassword, is TdApi.AuthorizationStateWaitRegistration, is TdApi.AuthorizationStateWaitEmailAddress -> 3
        else -> 1
    }
    val imeOpen = WindowInsets.isImeVisible
    // error shake
    val shake = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(errorTick) {
        if (errorTick > 0) for (x in listOf(18f, -14f, 10f, -6f, 3f, 0f)) shake.animateTo(x, androidx.compose.animation.core.tween(45))
    }
    androidx.activity.compose.BackHandler(enabled = step == 1) { started = false }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        AuthBackdrop()
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val heroTop by androidx.compose.animation.core.animateDpAsState(if (imeOpen) 8.dp else 40.dp, Springs.smooth(), label = "ht")
            Spacer(Modifier.height(heroTop))
            AuthHero(compact = imeOpen || step >= 2)
            Spacer(Modifier.height(24.dp))
            androidx.compose.animation.AnimatedVisibility(step > 0, enter = fadeIn(Springs.effects()) + androidx.compose.animation.expandVertically(Springs.smooth()), exit = fadeOut(Springs.exit()) + androidx.compose.animation.shrinkVertically(Springs.exit())) {
                StepPills(step - 1, 3, Modifier.padding(bottom = 20.dp))
            }
            AnimatedContent(
                targetState = step to state,
                contentKey = { it.first to it.second?.javaClass },
                transitionSpec = {
                    val fwd = targetState.first >= initialState.first
                    (slideInHorizontally(Springs.smooth()) { if (fwd) it / 3 else -it / 3 } + fadeIn(Springs.effects()) + androidx.compose.animation.scaleIn(Springs.smooth(), initialScale = 0.96f))
                        .togetherWith(slideOutHorizontally(Springs.exit()) { if (fwd) -it / 4 else it / 4 } + fadeOut(Springs.exit()))
                },
                label = "auth",
            ) { (stp, st) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().graphicsLayer { translationX = shake.value.dp.toPx() }) {
                    when {
                        stp == 0 -> WelcomeStep { started = true }
                        st is TdApi.AuthorizationStateWaitPhoneNumber -> PhoneStep(busy) { phone -> submit(TdApi.SetAuthenticationPhoneNumber(phone, null)) }
                        st is TdApi.AuthorizationStateWaitCode -> CodeStep(st, busy, error != null, onBack = { submit(TdApi.LogOut()) }) { code -> submit(TdApi.CheckAuthenticationCode(code)) }
                        st is TdApi.AuthorizationStateWaitPassword -> PasswordStep(st.passwordHint, busy) { pw -> submit(TdApi.CheckAuthenticationPassword(pw)) }
                        st is TdApi.AuthorizationStateWaitRegistration -> RegisterStep(busy) { f, l -> submit(TdApi.RegisterUser(f, l, false)) }
                        st is TdApi.AuthorizationStateWaitEmailAddress -> SingleFieldStep(
                            "Электронная почта", "Telegram просит привязать почту для входа.", "Почта", KeyboardType.Email, busy,
                        ) { submit(TdApi.SetAuthenticationEmailAddress(it)) }
                        st is TdApi.AuthorizationStateWaitEmailCode -> SingleFieldStep(
                            "Код из письма", "Мы отправили код на ${st.codeInfo.emailAddressPattern}", "Код", KeyboardType.Number, busy,
                        ) { submit(TdApi.CheckAuthenticationEmailCode(TdApi.EmailAddressAuthenticationCode(it))) }
                        st is TdApi.AuthorizationStateWaitOtherDeviceConfirmation -> {
                            Title("Подтвердите вход", "Откройте Telegram на другом устройстве: Настройки → Устройства → Подключить устройство.")
                            ContainedLoadingIndicator()
                        }
                        else -> ContainedLoadingIndicator()
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(error != null, enter = fadeIn(Springs.effects()) + androidx.compose.animation.expandVertically(Springs.spatial()), exit = fadeOut(Springs.exit()) + androidx.compose.animation.shrinkVertically(Springs.exit())) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.padding(top = 16.dp).fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(12.dp))
                        Text(error ?: "", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** big expressive shape that keeps morphing between M3 shapes and slowly turns, with the full logo on top */
@Composable
private fun AuthHero(compact: Boolean) {
    val cs = MaterialTheme.colorScheme
    val size by androidx.compose.animation.core.animateDpAsState(if (compact) 120.dp else 208.dp, Springs.spatial(), label = "hs")
    val shapes = remember {
        listOf(MaterialShapes.Cookie9Sided, MaterialShapes.Sunny, MaterialShapes.Clover8Leaf, MaterialShapes.SoftBurst, MaterialShapes.Cookie12Sided, MaterialShapes.Flower, MaterialShapes.Pentagon)
    }
    var idx by remember { mutableStateOf(0) }
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1600)
            progress.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 120f))
            idx = (idx + 1) % shapes.size
            progress.snapTo(0f)
        }
    }
    val morph = remember(idx) { androidx.graphics.shapes.Morph(shapes[idx], shapes[(idx + 1) % shapes.size]) }
    val spin = androidx.compose.animation.core.rememberInfiniteTransition(label = "spin")
    val rot by spin.animateFloat(0f, 360f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(24000, easing = androidx.compose.animation.core.LinearEasing)), label = "r")
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(size).graphicsLayer { rotationZ = rot }.clip(MorphShape(morph, progress.value))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(cs.primaryContainer, cs.tertiaryContainer))),
        )
        QwLogo(size * 0.95f, color = cs.primary)
    }
}

/** decorative shapes drifting in the corners (very low contrast) */
@Composable
private fun AuthBackdrop() {
    val cs = MaterialTheme.colorScheme
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "bg")
    val a by t.animateFloat(0f, 360f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(40000, easing = androidx.compose.animation.core.LinearEasing)), label = "a")
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        Box(Modifier.size(w * 0.7f).offset(x = -w * 0.25f, y = -w * 0.2f).graphicsLayer { rotationZ = a }
            .clip(MaterialShapes.Cookie9Sided.asShape()).background(cs.secondaryContainer.copy(alpha = 0.35f)))
        Box(Modifier.size(w * 0.6f).align(Alignment.BottomEnd).offset(x = w * 0.22f, y = w * 0.18f).graphicsLayer { rotationZ = -a * 0.8f }
            .clip(MaterialShapes.Clover4Leaf.asShape()).background(cs.tertiaryContainer.copy(alpha = 0.3f)))
    }
}

@Composable
private fun StepPills(current: Int, total: Int, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val w by androidx.compose.animation.core.animateDpAsState(if (i == current) 32.dp else 8.dp, Springs.fastSpatial(), label = "pw")
            val c by androidx.compose.animation.animateColorAsState(if (i <= current) cs.primary else cs.outlineVariant, Springs.effects(), label = "pc")
            Box(Modifier.height(8.dp).width(w).clip(CircleShape).background(c))
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Text("qwgram", style = MaterialTheme.typography.displayMedium, color = cs.onSurface)
    Spacer(Modifier.height(8.dp))
    Text("Telegram в стиле Material 3 Expressive", style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(28.dp))
    val features = listOf(
        Icons.Outlined.Palette to "Цвета и формы", Icons.Outlined.Animation to "Живые анимации",
        Icons.Outlined.Lock to "Приватность", Icons.Outlined.Bolt to "Быстрый TDLib",
    )
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        features.forEachIndexed { i, (icon, label) ->
            val appear = remember { androidx.compose.animation.core.Animatable(0f) }
            LaunchedEffect(Unit) { kotlinx.coroutines.delay(120L + i * 90L); appear.animateTo(1f, Springs.spatial()) }
            Surface(
                shape = RoundedCornerShape(16.dp), color = if (i % 2 == 0) cs.secondaryContainer else cs.tertiaryContainer,
                modifier = Modifier.graphicsLayer { alpha = appear.value.coerceIn(0f, 1f); val sc = 0.6f + 0.4f * appear.value; scaleX = sc; scaleY = sc },
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
    Spacer(Modifier.height(36.dp))
    QButton(onClick = onStart, modifier = Modifier.fillMaxWidth().height(64.dp)) {
        Text("Начать", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
    }
    Spacer(Modifier.height(12.dp))
    Text("Неофициальный клиент. Работает на серверах Telegram.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
}

private fun humanError(msg: String): String = when {
    msg.contains("PHONE_NUMBER_INVALID") -> "Неверный номер телефона"
    msg.contains("PHONE_CODE_INVALID") -> "Неверный код"
    msg.contains("PHONE_CODE_EXPIRED") -> "Код устарел, запросите новый"
    msg.contains("PASSWORD_HASH_INVALID") -> "Неверный пароль"
    msg.contains("FLOOD_WAIT") || msg.contains("Too Many Requests") -> "Слишком много попыток. Подождите немного"
    msg.contains("PHONE_NUMBER_BANNED") -> "Этот номер заблокирован в Telegram"
    msg.contains("API_ID") -> "Ключи API не подходят (TG_APP_ID / TG_APP_HASH)"
    else -> msg
}

@Composable
private fun Title(title: String, sub: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(10.dp))
    Text(sub, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(28.dp))
}

@Composable
private fun PrimaryButton(text: String, busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    QButton(onClick = onClick, enabled = enabled && !busy, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        AnimatedContent(busy, transitionSpec = { (fadeIn(Springs.effects()) + androidx.compose.animation.scaleIn(Springs.fastSpatial())).togetherWith(fadeOut(Springs.exit())) }, label = "pb") { b ->
            if (b) LoadingIndicator(Modifier.size(32.dp), color = MaterialTheme.colorScheme.onPrimary) else Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private val fieldShape = RoundedCornerShape(16.dp)

@Composable
private fun PhoneStep(busy: Boolean, onSubmit: (String) -> Unit) {
    var phone by remember { mutableStateOf("+7") }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(350); runCatching { focus.requestFocus() } }
    Title("Ваш номер", "Введите номер телефона, к которому привязан Telegram. Мы отправим код подтверждения.")
    OutlinedTextField(
        value = phone, onValueChange = { v -> phone = "+" + v.filter { it.isDigit() }.take(15) },
        label = { Text("Номер телефона") }, singleLine = true, shape = fieldShape,
        leadingIcon = { Icon(Icons.Outlined.Phone, null) },
        textStyle = MaterialTheme.typography.titleLarge,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (phone.length > 6) onSubmit(phone) }),
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
    )
    Spacer(Modifier.height(20.dp))
    PrimaryButton("Получить код", busy, phone.length > 6) { onSubmit(phone) }
}

@Composable
private fun CodeStep(st: TdApi.AuthorizationStateWaitCode, busy: Boolean, hasError: Boolean, onBack: () -> Unit, onSubmit: (String) -> Unit) {
    val info = st.codeInfo
    val where = when (info.type) {
        is TdApi.AuthenticationCodeTypeTelegramMessage -> "в Telegram на другом устройстве"
        is TdApi.AuthenticationCodeTypeSms -> "по SMS"
        is TdApi.AuthenticationCodeTypeCall -> "звонком"
        is TdApi.AuthenticationCodeTypeFragment -> "через Fragment"
        else -> ""
    }
    val len = when (val t = info.type) {
        is TdApi.AuthenticationCodeTypeTelegramMessage -> t.length
        is TdApi.AuthenticationCodeTypeSms -> t.length
        is TdApi.AuthenticationCodeTypeCall -> t.length
        else -> 5
    }.coerceIn(4, 8)
    var code by remember { mutableStateOf("") }
    LaunchedEffect(hasError) { if (hasError) code = "" }
    Title("Код подтверждения", "Отправили код $where на ${info.phoneNumber}")
    CodeCells(code, len, hasError) { v ->
        code = v
        if (code.length == len && !busy) onSubmit(code)
    }
    Spacer(Modifier.height(24.dp))
    PrimaryButton("Войти", busy, code.length >= 4) { onSubmit(code) }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QTextButton(onClick = onBack) { Text("Другой номер") }
        if (info.nextType != null) QTextButton(onClick = { Td.send(TdApi.ResendAuthenticationCode(null)) }) { Text("Отправить иначе") }
    }
}

/** one cell per digit: the active cell is outlined and rounder, digits pop in with a spring */
@Composable
private fun CodeCells(code: String, len: Int, error: Boolean, onChange: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(350); runCatching { focus.requestFocus() } }
    androidx.compose.foundation.text.BasicTextField(
        value = code,
        onValueChange = { onChange(it.filter(Char::isDigit).take(len)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.focusRequester(focus),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(len) { i ->
                    val ch = code.getOrNull(i)
                    val active = i == code.length
                    val r by androidx.compose.animation.core.animateDpAsState(if (active) 20.dp else if (ch != null) 14.dp else 10.dp, Springs.fastSpatial(), label = "cr")
                    val sc by androidx.compose.animation.core.animateFloatAsState(if (ch != null || active) 1f else 0.92f, Springs.fastSpatial(), label = "cs")
                    val bg by androidx.compose.animation.animateColorAsState(
                        when { error -> cs.errorContainer; ch != null -> cs.secondaryContainer; else -> cs.surfaceContainerHigh }, Springs.effects(), label = "cb",
                    )
                    Box(
                        Modifier.size(width = 46.dp, height = 58.dp).graphicsLayer { scaleX = sc; scaleY = sc }
                            .clip(RoundedCornerShape(r)).background(bg)
                            .then(if (active) Modifier.border(2.dp, cs.primary, RoundedCornerShape(r)) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(ch, transitionSpec = { (androidx.compose.animation.scaleIn(Springs.fastSpatial(), initialScale = 0.4f) + fadeIn(Springs.effects())).togetherWith(fadeOut(Springs.exit())) }, label = "d") { c ->
                            Text(c?.toString() ?: "", style = MaterialTheme.typography.headlineSmall, color = cs.onSurface)
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun PasswordStep(hint: String, busy: Boolean, onSubmit: (String) -> Unit) {
    var pw by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    Title("Облачный пароль", if (hint.isNotBlank()) "Подсказка: $hint" else "У аккаунта включена двухэтапная проверка")
    OutlinedTextField(
        value = pw, onValueChange = { pw = it }, label = { Text("Пароль") }, singleLine = true, shape = fieldShape,
        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
        trailingIcon = { MorphIconButton(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "Показать", { show = !show }) },
        visualTransformation = if (show) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit(pw) }),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(20.dp))
    PrimaryButton("Войти", busy, pw.isNotEmpty()) { onSubmit(pw) }
}

@Composable
private fun RegisterStep(busy: Boolean, onSubmit: (String, String) -> Unit) {
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    Title("Ваше имя", "Номер ещё не зарегистрирован. Как вас называть?")
    OutlinedTextField(first, { first = it }, label = { Text("Имя") }, singleLine = true, shape = fieldShape, leadingIcon = { Icon(Icons.Outlined.Person, null) }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(last, { last = it }, label = { Text("Фамилия (необязательно)") }, singleLine = true, shape = fieldShape, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(20.dp))
    PrimaryButton("Создать аккаунт", busy, first.isNotBlank()) { onSubmit(first, last) }
}

@Composable
private fun SingleFieldStep(title: String, sub: String, label: String, type: KeyboardType, busy: Boolean, onSubmit: (String) -> Unit) {
    var v by remember { mutableStateOf("") }
    Title(title, sub)
    OutlinedTextField(v, { v = it }, label = { Text(label) }, singleLine = true, shape = fieldShape, keyboardOptions = KeyboardOptions(keyboardType = type), modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(20.dp))
    PrimaryButton("Далее", busy, v.isNotBlank()) { onSubmit(v.trim()) }
}
