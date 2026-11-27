package app.qwgram.next.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMade
import androidx.compose.material.icons.automirrored.outlined.CallMissed
import androidx.compose.material.icons.automirrored.outlined.CallReceived
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.PhoneMissed
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.qwgram.next.td.Td
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.Texts
import org.drinkless.tdlib.TdApi

private fun callUser(m: TdApi.Message): Long = (TdStore.chats[m.chatId]?.type as? TdApi.ChatTypePrivate)?.userId ?: m.chatId

/** Calls: your call history (all / missed), loaded page by page from Telegram */
@Composable
fun CallsPage() {
    var missed by remember { mutableStateOf(false) }
    val items = remember(missed) { mutableStateListOf<TdApi.Message>() }
    var offset by remember(missed) { mutableStateOf("") }
    var end by remember(missed) { mutableStateOf(false) }
    var loading by remember(missed) { mutableStateOf(false) }
    suspend fun more() {
        if (loading || end) return
        loading = true
        val r = Td.callOrNull(TdApi.SearchCallMessages(offset, 50, missed))
        if (r != null) { items += r.messages; offset = r.nextOffset; if (r.nextOffset.isEmpty() || r.messages.isEmpty()) end = true } else end = true
        loading = false
    }
    LaunchedEffect(missed) { more() }
    PageScaffold("Звонки") {
        item {
            // all / missed: a connected pair (M3)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf(false to "Все", true to "Пропущенные").forEachIndexed { i, (v, t) ->
                    ConnectedTile(Seat(0, 1, i, 2), onClick = { missed = v }, modifier = Modifier.weight(1f).height(48.dp), selected = missed == v, big = 24.dp) {
                        Text(t, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        if (items.isEmpty() && !loading) item {
            Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val cs = MaterialTheme.colorScheme
                Box(Modifier.size(104.dp).clip(remember { MorphShape(androidx.graphics.shapes.Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Cookie9Sided), 0f) }).background(cs.secondaryContainer), contentAlignment = Alignment.Center) {
                    Icon(if (missed) Icons.Outlined.PhoneMissed else Icons.AutoMirrored.Outlined.CallReceived, null, Modifier.size(42.dp), tint = cs.onSecondaryContainer)
                }
                Spacer(Modifier.height(16.dp))
                Text(if (missed) "Пропущенных нет" else "Звонков пока нет", style = MaterialTheme.typography.titleLarge)
            }
        }
        items.forEachIndexed { i, m ->
            item(key = m.chatId.toString() + ":" + m.id) {
                if (i >= items.size - 5) LaunchedEffect(Unit) { more() }
                CallRow(m, Modifier.segment(i, items.size))
            }
        }
        if (loading) item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { androidx.compose.material3.LoadingIndicator() } }
    }
}

@Composable
private fun CallRow(m: TdApi.Message, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val c = m.content as? TdApi.MessageCall ?: return
    val user = TdStore.users[callUser(m)]
    val missed = c.discardReason is TdApi.CallDiscardReasonMissed || c.discardReason is TdApi.CallDiscardReasonDeclined && !m.isOutgoing
    Row(
        modifier.fillMaxWidth().clickable { Nav.push(Route.Chat(m.chatId)) }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(user, 48.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(user?.let { TdStore.fullName(it) } ?: "", style = MaterialTheme.typography.titleMedium, color = if (missed) cs.error else cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when { missed -> Icons.AutoMirrored.Outlined.CallMissed; m.isOutgoing -> Icons.AutoMirrored.Outlined.CallMade; else -> Icons.AutoMirrored.Outlined.CallReceived },
                    null, Modifier.size(16.dp), tint = if (missed) cs.error else cs.primary,
                )
                Spacer(Modifier.width(6.dp))
                val kind = (if (m.isOutgoing) "Исходящий" else if (missed) "Пропущенный" else "Входящий") + if (c.isVideo) " видеозвонок" else ""
                Text(kind + (if (c.duration > 0) " · " + Texts.duration(c.duration) else ""), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(Texts.listDate(m.date), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            if (c.isVideo) Icon(Icons.Outlined.Videocam, null, Modifier.size(18.dp), tint = cs.onSurfaceVariant)
        }
        Spacer(Modifier.width(6.dp))
        MorphIconButton(Icons.AutoMirrored.Outlined.Chat, "Написать", { Nav.push(Route.Chat(m.chatId)) }, size = 44.dp, color = cs.secondaryContainer, tint = cs.onSecondaryContainer)
    }
}

/**
 * An incoming call over everything: the caller's avatar inside pulsing, slowly turning expressive shapes.
 * Voice itself needs Telegram's VoIP library (tgcalls), which this client does not have yet — so the screen
 * says so honestly and offers to decline (the call can be answered on another device).
 */
@Composable
fun CallHost() {
    val call = TdStore.call
    val incoming = call != null && !call.isOutgoing && call.state is TdApi.CallStatePending
    var hidden by remember(call?.id) { mutableStateOf(false) }
    val show = incoming && !hidden
    BackHandler(show) { hidden = true }
    AnimatedVisibility(show, enter = fadeIn(tween(220)) + slideInVertically(spring(0.8f, 300f)) { it / 6 }, exit = fadeOut(tween(200)) + slideOutVertically { it / 6 }) {
        val c = call ?: return@AnimatedVisibility
        val cs = MaterialTheme.colorScheme
        val user = TdStore.users[c.userId]
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(cs.primaryContainer, cs.surfaceContainerLowest)))) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(48.dp))
                Text(if (c.isVideo) "Входящий видеозвонок" else "Входящий звонок", style = MaterialTheme.typography.titleMedium, color = cs.onPrimaryContainer.copy(alpha = 0.8f))
                Spacer(Modifier.height(36.dp))
                // the avatar in two breathing, turning shapes
                val spin = loopPhase(true, 14000, 360f)
                val pulse = loopPhase(true, 1800, (2 * Math.PI).toFloat())
                Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                    val outer = remember { MorphShape(androidx.graphics.shapes.Morph(MaterialShapes.Cookie12Sided, MaterialShapes.Cookie12Sided), 0f) }
                    val inner = remember { MorphShape(androidx.graphics.shapes.Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Cookie9Sided), 0f) }
                    Box(Modifier.size(240.dp).graphicsLayer { rotationZ = spin.value; val sc = 0.92f + 0.08f * kotlin.math.sin(pulse.value); scaleX = sc; scaleY = sc }.clip(outer).background(cs.primary.copy(alpha = 0.16f)))
                    Box(Modifier.size(196.dp).graphicsLayer { rotationZ = -spin.value * 1.3f; val sc = 0.95f + 0.05f * kotlin.math.sin(pulse.value + 1.4f); scaleX = sc; scaleY = sc }.clip(inner).background(cs.primary.copy(alpha = 0.26f)))
                    UserAvatar(user, 150.dp)
                }
                Spacer(Modifier.height(28.dp))
                Text(user?.let { TdStore.fullName(it) } ?: "", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = cs.onSurface, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Голосовая связь в qwgram пока не поддерживается — ответьте с другого устройства или отклоните звонок.",
                    style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MorphBox(onClick = { Td.send(TdApi.DiscardCall(c.id, false, "", 0, c.isVideo, 0L)); hidden = true }, rest = 36.dp, pressed = 20.dp, modifier = Modifier.size(76.dp), color = cs.error, contentColor = cs.onError) {
                            Icon(Icons.Outlined.CallEnd, "Отклонить", Modifier.size(32.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Отклонить", style = MaterialTheme.typography.labelLarge)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MorphBox(onClick = { hidden = true }, rest = 36.dp, pressed = 20.dp, modifier = Modifier.size(76.dp), color = cs.secondaryContainer, contentColor = cs.onSecondaryContainer) {
                            Icon(Icons.AutoMirrored.Outlined.Chat, "Скрыть", Modifier.size(30.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Скрыть", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
