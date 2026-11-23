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
import androidx.compose.material3.toShape
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
import org.drinkless.tdlib.TdApi

@Composable
fun AppRoot() {
    val auth = TdStore.auth
    val motion = MaterialTheme.motionScheme
    AnimatedContent(
        targetState = when (auth) {
            null, is TdApi.AuthorizationStateWaitTdlibParameters, is TdApi.AuthorizationStateClosed, is TdApi.AuthorizationStateClosing -> 0
            is TdApi.AuthorizationStateReady -> 2
            is TdApi.AuthorizationStateLoggingOut -> 0
            else -> 1
        },
        transitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()) },
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
        is Route.Chat -> ChatScreen(route.chatId)
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
    val motion = MaterialTheme.motionScheme
    fun submit(f: TdApi.Function<*>) {
        busy = true
        error = null
        scope.launch {
            try {
                @Suppress("UNCHECKED_CAST")
                Td.call(f as TdApi.Function<TdApi.Object>)
            } catch (e: TdException) {
                error = humanError(e.error.message)
            } catch (e: Exception) {
                error = e.message
            }
            busy = false
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).safeDrawingPadding().imePadding()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            Box(
                Modifier.size(112.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(Color(0xFF6750A4)),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ic_splash), null, Modifier.size(150.dp))
            }
            Spacer(Modifier.height(28.dp))
            AnimatedContent(
                targetState = state,
                contentKey = { it?.javaClass },
                transitionSpec = {
                    (slideInHorizontally(motion.defaultSpatialSpec()) { it / 4 } + fadeIn(motion.defaultEffectsSpec()))
                        .togetherWith(slideOutHorizontally(motion.defaultSpatialSpec()) { -it / 4 } + fadeOut(motion.fastEffectsSpec()))
                },
                label = "auth",
            ) { st ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    when (st) {
                        is TdApi.AuthorizationStateWaitPhoneNumber -> PhoneStep(busy) { phone ->
                            submit(TdApi.SetAuthenticationPhoneNumber(phone, null))
                        }
                        is TdApi.AuthorizationStateWaitCode -> CodeStep(st, busy, onBack = { submit(TdApi.LogOut()) }) { code ->
                            submit(TdApi.CheckAuthenticationCode(code))
                        }
                        is TdApi.AuthorizationStateWaitPassword -> PasswordStep(st.passwordHint, busy) { pw ->
                            submit(TdApi.CheckAuthenticationPassword(pw))
                        }
                        is TdApi.AuthorizationStateWaitRegistration -> RegisterStep(busy) { f, l ->
                            submit(TdApi.RegisterUser(f, l, false))
                        }
                        is TdApi.AuthorizationStateWaitEmailAddress -> SingleFieldStep(
                            "Электронная почта", "Telegram просит привязать почту для входа.", "Почта", KeyboardType.Email, busy,
                        ) { submit(TdApi.SetAuthenticationEmailAddress(it)) }
                        is TdApi.AuthorizationStateWaitEmailCode -> SingleFieldStep(
                            "Код из письма", "Мы отправили код на ${st.codeInfo.emailAddressPattern}", "Код", KeyboardType.Number, busy,
                        ) { submit(TdApi.CheckAuthenticationEmailCode(TdApi.EmailAddressAuthenticationCode(it))) }
                        is TdApi.AuthorizationStateWaitOtherDeviceConfirmation -> {
                            Text("Подтвердите вход", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(12.dp))
                            Text("Откройте Telegram на другом устройстве: Настройки → Устройства → Подключить устройство.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        else -> ContainedLoadingIndicator()
                    }
                }
            }
            error?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
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
    Button(onClick = onClick, enabled = enabled && !busy, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        if (busy) LoadingIndicator(Modifier.size(28.dp), color = MaterialTheme.colorScheme.onPrimary) else Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun PhoneStep(busy: Boolean, onSubmit: (String) -> Unit) {
    var phone by remember { mutableStateOf("+7") }
    Title("Добро пожаловать в qwgram", "Клиент Telegram в стиле Material 3 Expressive. Введите номер телефона, к которому привязан Telegram.")
    OutlinedTextField(
        value = phone, onValueChange = { v -> phone = "+" + v.filter { it.isDigit() } },
        label = { Text("Номер телефона") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (phone.length > 6) onSubmit(phone) }),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(20.dp))
    PrimaryButton("Далее", busy, phone.length > 6) { onSubmit(phone) }
}

@Composable
private fun CodeStep(st: TdApi.AuthorizationStateWaitCode, busy: Boolean, onBack: () -> Unit, onSubmit: (String) -> Unit) {
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
    }
    var code by remember { mutableStateOf("") }
    Title("Код подтверждения", "Отправили код $where на ${info.phoneNumber}")
    OutlinedTextField(
        value = code,
        onValueChange = { v ->
            code = v.filter { it.isDigit() }.take(8)
            if (code.length == len && !busy) onSubmit(code)
        },
        label = { Text("Код") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit(code) }),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(20.dp))
    PrimaryButton("Войти", busy, code.length >= 4) { onSubmit(code) }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onBack) { Text("Другой номер") }
        if (info.nextType != null) TextButton(onClick = { Td.send(TdApi.ResendAuthenticationCode(null)) }) { Text("Отправить иначе") }
    }
}

@Composable
private fun PasswordStep(hint: String, busy: Boolean, onSubmit: (String) -> Unit) {
    var pw by remember { mutableStateOf("") }
    Title("Облачный пароль", if (hint.isNotBlank()) "Подсказка: $hint" else "У аккаунта включена двухэтапная проверка")
    OutlinedTextField(
        value = pw, onValueChange = { pw = it }, label = { Text("Пароль") }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
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
    OutlinedTextField(first, { first = it }, label = { Text("Имя") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(last, { last = it }, label = { Text("Фамилия (необязательно)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(20.dp))
    PrimaryButton("Создать аккаунт", busy, first.isNotBlank()) { onSubmit(first, last) }
}

@Composable
private fun SingleFieldStep(title: String, sub: String, label: String, type: KeyboardType, busy: Boolean, onSubmit: (String) -> Unit) {
    var v by remember { mutableStateOf("") }
    Title(title, sub)
    OutlinedTextField(v, { v = it }, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = type), modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(20.dp))
    PrimaryButton("Далее", busy, v.isNotBlank()) { onSubmit(v.trim()) }
    Spacer(Modifier.width(1.dp))
}
