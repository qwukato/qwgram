package app.qwgram.next.ui

import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.togetherWith
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.qwgram.next.td.Images
import app.qwgram.next.td.Td
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.rememberFileBitmap
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

private fun argb(c: Int) = Color(0xFF000000.toInt() or c)

/**
 * Profile header in the Telegram Premium style: an edge-to-edge banner in the profile's own colours (the ones the
 * person picked in Telegram; the theme's tones otherwise), a faint star pattern for Premium, the big avatar,
 * the name with the Premium star / verified mark, the status. Reaches up under the header and fades into the page.
 */
@Composable
fun ProfileBanner(
    chat: TdApi.Chat?, user: TdApi.User?, me: Boolean, title: String, status: String, topInset: Dp,
    onAvatar: () -> Unit, onCamera: (() -> Unit)?,
    /** action buttons drawn inside the banner (Telegram-like); `onColor` = the banner is coloured */
    actions: @Composable (onColor: Boolean) -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    val dark = cs.surface.luminance() < 0.5f
    // Telegram's order: a worn collectible gift (emoji status) paints the profile with its backdrop; else the
    // gift-based profile colours; else the profile colour picked in Premium settings; else a plain page
    val giftStatus = user?.emojiStatus?.type as? TdApi.EmojiStatusTypeUpgradedGift
    val colorId = user?.profileAccentColorId ?: chat?.profileAccentColorId ?: -1
    val pc = if (colorId >= 0) TdStore.profileColors[colorId] else null
    val giftColors = (user?.upgradedGiftColors ?: chat?.upgradedGiftColors)?.let { g -> (if (dark) g.darkThemeColors else g.lightThemeColors).map(::argb) }?.takeIf { it.isNotEmpty() }
    val bannerColors: List<Color>? = giftStatus?.backdropColors?.let { listOf(argb(it.centerColor), argb(it.edgeColor)) }
        ?: giftColors
        ?: pc?.let { (if (dark) it.darkThemeColors else it.lightThemeColors).backgroundColors.map(::argb) }?.takeIf { it.isNotEmpty() }
    // the little pattern around the avatar: the gift's symbol, or the profile background emoji the person chose
    val patternId = giftStatus?.symbolCustomEmojiId?.takeIf { it != 0L }
        ?: user?.profileBackgroundCustomEmojiId?.takeIf { it != 0L } ?: chat?.profileBackgroundCustomEmojiId?.takeIf { it != 0L }
    val verified = (user?.verificationStatus?.isVerified == true)
    val onBanner = if (bannerColors != null) Color.White else cs.onSurface
    // the whole header settles in: the avatar springs up from 85%, the texts follow a beat later
    val appear = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, androidx.compose.animation.core.spring(0.7f, 260f)) }
    Box(
        Modifier
            // full width (out of the 16dp page padding) and up under the header and the status bar
            .layout { m, c ->
                val extraW = 32.dp.roundToPx()
                val up = (topInset + 4.dp).roundToPx()
                val pl = m.measure(Constraints(minWidth = c.maxWidth + extraW, maxWidth = c.maxWidth + extraW))
                layout(c.maxWidth, (pl.height - up).coerceAtLeast(0)) { pl.place(-extraW / 2, -up) }
            }
            .then(
                if (bannerColors == null) Modifier
                else Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 36.dp.rs(), bottomEnd = 36.dp.rs()))
                    // Telegram draws the profile colour as a soft radial glow: inner colour around the avatar, outer to the edges
                    .drawBehind {
                        val inner = bannerColors.first()
                        val outer = bannerColors.getOrNull(1) ?: bannerColors.first()
                        drawRect(outer)
                        drawRect(Brush.radialGradient(listOf(inner, outer), center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.45f), radius = size.maxDimension * 0.6f))
                    },
            )
            .padding(top = (topInset - 14.dp).coerceAtLeast(0.dp), bottom = if (bannerColors != null) 14.dp else 6.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                // pattern: rings of the chosen emoji around the avatar, fading outwards (Telegram Premium profile)
                if (patternId != null && bannerColors != null && app.qwgram.next.Prefs.profilePattern.value) {
                    // two rings like Telegram: an inner one close to the avatar, a fainter outer one
                    val ring = remember {
                        buildList {
                            for (i in 0 until 8) { val a = Math.toRadians(i * 45.0 + 22.5); add(Triple((cos(a) * 82).toFloat(), (sin(a) * 72).toFloat(), 0.5f)) }
                            for (i in 0 until 12) { val a = Math.toRadians(i * 30.0); add(Triple((cos(a) * 140).toFloat(), (sin(a) * 100).toFloat(), 0.26f)) }
                        }
                    }
                    ring.forEach { (x, y, a) ->
                        Box(Modifier.size(22.dp).graphicsLayer {
                            translationX = x.dp.toPx() * (0.7f + 0.3f * appear.value); translationY = y.dp.toPx() * (0.7f + 0.3f * appear.value)
                            alpha = a * appear.value
                        }) { CustomEmojiImage(patternId, 22.dp) }
                    }
                }
                Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.padding(top = 10.dp, bottom = 14.dp).graphicsLayer { val sc = 0.85f + 0.15f * appear.value; scaleX = sc; scaleY = sc }) {
                    Box(Modifier.size(92.dp).clip(CircleShape).clickable(onClick = onAvatar)) {
                        if (chat == null) UserAvatar(user, 92.dp) else ChatAvatar(chat, 92.dp)
                    }
                    if (onCamera != null) MorphIconButton(Icons.Outlined.PhotoCamera, "Сменить фото", onCamera, size = 42.dp, color = cs.primary, tint = cs.onPrimary, elevation = 3.dp)
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = ((appear.value - 0.2f) / 0.8f).coerceIn(0f, 1f); translationY = (1f - appear.value) * 16.dp.toPx() },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = onBanner, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (verified) { Spacer(Modifier.width(6.dp)); Icon(Icons.Filled.Verified, "Подтверждён", tint = if (bannerColors != null) Color.White else cs.primary, modifier = Modifier.size(22.dp)) }
                    EmojiStatusMark(user, 26.dp, tint = if (bannerColors != null) Color.White else cs.primary, gap = 6.dp)
                }
                // the status line rolls when it changes ("в сети" ⇄ "был(а) …")
                androidx.compose.animation.AnimatedContent(status, transitionSpec = {
                    (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220, 60)) + androidx.compose.animation.slideInVertically { it / 2 })
                        .togetherWith(androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)) + androidx.compose.animation.slideOutVertically { -it / 2 })
                }, label = "st") { st ->
                    if (st.isNotBlank()) Text(st, style = MaterialTheme.typography.bodyLarge, color = onBanner.copy(alpha = if (st == "в сети") 1f else 0.8f), textAlign = TextAlign.Center)
                }
            }
            // the actions sit inside the banner, a beat after the name
            Box(Modifier.padding(top = 14.dp).fillMaxWidth().appearIn(0)) { actions(bannerColors != null) }
        }
    }
}

/** profile sections slide up one after another when the page opens */
@Composable
fun Modifier.appearIn(order: Int): Modifier {
    val a = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(90L + order * 55L)
        a.animateTo(1f, androidx.compose.animation.core.spring(0.78f, 300f))
    }
    return graphicsLayer { alpha = a.value.coerceIn(0f, 1f); translationY = (1f - a.value) * 28.dp.toPx() }
}

/** ⋮ in the profile header: every less-common action, growing out of the button (M3 Expressive menu) */
@Composable
fun ProfileMenu(chat: TdApi.Chat?, user: TdApi.User?, me: Boolean) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    val un = user?.usernames?.activeUsernames?.firstOrNull()
        ?: (chat?.type as? TdApi.ChatTypeSupergroup)?.let { TdStore.supergroups[it.supergroupId]?.usernames?.activeUsernames?.firstOrNull() }
    val shareText = when {
        un != null -> "https://t.me/$un"
        user != null && user.phoneNumber.isNotBlank() -> TdStore.fullName(user) + "\n+" + user.phoneNumber
        else -> null
    }
    val isPrivate = chat?.type is TdApi.ChatTypePrivate && !TdStore.isBot(chat)
    Box {
        HeaderIcon(Icons.Outlined.MoreVert, "Ещё", { open = true })
        QMenu(expanded = open, onDismissRequest = { open = false }) {
            if (shareText != null) DropdownMenuItem(
                text = { Text(if (user != null) "Поделиться контактом" else "Поделиться") }, leadingIcon = { Icon(Icons.Outlined.Share, null) },
                onClick = { open = false; ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText), null)) },
            )
            if (un != null) DropdownMenuItem(
                text = { Text("Скопировать ссылку") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                onClick = {
                    open = false
                    (ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("link", "https://t.me/$un"))
                    Toast.makeText(ctx, "Ссылка скопирована", Toast.LENGTH_SHORT).show()
                },
            )
            if (!me && isPrivate && user != null && !user.isContact) DropdownMenuItem(
                text = { Text("Добавить в контакты") }, leadingIcon = { Icon(Icons.Outlined.PersonAdd, null) },
                onClick = {
                    open = false
                    Td.send(TdApi.AddContact(user.id, TdApi.ImportedContact(user.phoneNumber, user.firstName, user.lastName, null), false)) { r ->
                        android.os.Handler(android.os.Looper.getMainLooper()).post { Toast.makeText(ctx, if (r is TdApi.Error) "Не удалось: ${r.message}" else "Добавлен в контакты", Toast.LENGTH_SHORT).show() }
                    }
                },
            )
            if (!me && isPrivate && user != null) DropdownMenuItem(
                text = { Text("Начать секретный чат") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                onClick = {
                    open = false
                    scope.launch { Td.callOrNull(TdApi.CreateNewSecretChat(user.id))?.let { Nav.push(Route.Chat(it.id)) } ?: Toast.makeText(ctx, "Не удалось начать секретный чат", Toast.LENGTH_SHORT).show() }
                },
            )
            if (!me && isPrivate && user != null) {
                val blocked = chat?.blockList != null
                DropdownMenuItem(
                    text = { Text(if (blocked) "Разблокировать" else "Заблокировать", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Outlined.Block, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { open = false; Td.send(TdApi.SetMessageSenderBlockList(TdApi.MessageSenderUser(user.id), if (blocked) null else TdApi.BlockListMain())) },
                )
            }
        }
    }
}

/**
 * The person's profile music (Telegram: "music on profile"): a card with the first track's cover, title and
 * artist and the number of tracks; tap plays through them in the player.
 */
@Composable
fun ProfileMusic(userId: Long) {
    val cs = MaterialTheme.colorScheme
    var audios by remember(userId) { mutableStateOf<List<TdApi.Audio>>(emptyList()) }
    LaunchedEffect(userId) { audios = Td.callOrNull(TdApi.GetUserProfileAudios(userId, 0, 50))?.audios?.toList().orEmpty() }
    if (audios.isEmpty()) return
    val first = audios.first()
    val tracks = remember(audios) { audios.map { Track.of(it) } }
    val playingHere = Player.queue.isNotEmpty() && Player.track?.file?.id in tracks.map { it.file.id }
    Spacer(Modifier.height(12.dp))
    MorphBox(
        onClick = { if (playingHere) Player.playPause() else Player.playQueue(tracks, 0) },
        rest = 28.dp, pressed = 18.dp, squish = 0.98f,
        color = cs.secondaryContainer, contentColor = cs.onSecondaryContainer, contentAlignment = Alignment.CenterStart,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.fillMaxWidth()) {
            // faint notes in the background
            Icon(Icons.Outlined.MusicNote, null, Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).size(84.dp).graphicsLayer { alpha = 0.08f; rotationZ = -12f })
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val t = tracks.first()
                val bmp = rememberFileBitmap(t.cover, 320) ?: remember(t) { Images.mini(t.mini) }
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(cs.primaryContainer), contentAlignment = Alignment.Center) {
                    if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Outlined.MusicNote, null, tint = cs.onPrimaryContainer)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (first.performer.isNotBlank()) Text(first.performer, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = cs.onSecondaryContainer.copy(alpha = 0.8f))
                    if (audios.size > 1) Text("${audios.size} " + app.qwgram.next.td.Texts.plural(audios.size, "трек", "трека", "треков").substringAfter(' '), style = MaterialTheme.typography.labelMedium, color = cs.onSecondaryContainer.copy(alpha = 0.7f))
                }
                Box(contentAlignment = Alignment.Center) {
                    if (playingHere) WavyRing(Player.progress, Player.playing, cs.primary, cs.primary.copy(alpha = 0.2f), Modifier.size(52.dp))
                    PlayMorphButton(playingHere && Player.playing, 42.dp, cs.primary, cs.onPrimary) { if (playingHere) Player.playPause() else Player.playQueue(tracks, 0) }
                }
            }
        }
    }
}
