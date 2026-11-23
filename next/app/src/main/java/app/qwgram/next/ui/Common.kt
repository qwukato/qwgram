package app.qwgram.next.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.qwgram.next.Prefs
import app.qwgram.next.td.Images
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.rememberFileBitmap
import org.drinkless.tdlib.TdApi

/** tonal avatar colours derived from the scheme (no hardcoded palette) */
@Composable
fun avatarColors(seed: Long): Pair<Color, Color> {
    val cs = MaterialTheme.colorScheme
    return when (((seed % 3) + 3) % 3) {
        0L -> cs.primaryContainer to cs.onPrimaryContainer
        1L -> cs.secondaryContainer to cs.onSecondaryContainer
        else -> cs.tertiaryContainer to cs.onTertiaryContainer
    }
}

fun initials(title: String): String {
    val parts = title.trim().split(' ', '-', '_').filter { it.isNotBlank() && it.first().isLetterOrDigit() }
    return when {
        parts.isEmpty() -> title.trim().take(1).uppercase()
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

@Composable
fun Avatar(
    title: String,
    seed: Long,
    small: TdApi.File?,
    mini: TdApi.Minithumbnail?,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = avatarShape(),
    saved: Boolean = false,
) {
    val (bg, fg) = avatarColors(seed)
    val bmp = rememberFileBitmap(small, maxSide = 256)
    val miniBmp = remember(mini) { Images.mini(mini) }
    Box(modifier.size(size).clip(shape).background(if (saved) MaterialTheme.colorScheme.primary else bg), contentAlignment = Alignment.Center) {
        when {
            saved -> Icon(Icons.Filled.Bookmark, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(size * 0.5f))
            bmp != null -> Image(bmp, null, Modifier.size(size), contentScale = ContentScale.Crop)
            miniBmp != null -> Image(miniBmp, null, Modifier.size(size), contentScale = ContentScale.Crop)
            else -> Text(initials(title), color = fg, fontSize = (size.value * 0.38f).sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ChatAvatar(chat: TdApi.Chat, size: Dp, modifier: Modifier = Modifier) {
    Avatar(chat.title, chat.id, chat.photo?.small, chat.photo?.minithumbnail, size, modifier, saved = TdStore.isSaved(chat))
}

@Composable
fun UserAvatar(user: TdApi.User?, size: Dp, modifier: Modifier = Modifier) {
    if (user == null) {
        Avatar("?", 0, null, null, size, modifier)
        return
    }
    Avatar(TdStore.fullName(user), user.id, user.profilePhoto?.small, user.profilePhoto?.minithumbnail, size, modifier)
}

@Composable
fun SenderAvatar(sender: TdApi.MessageSender?, size: Dp, modifier: Modifier = Modifier) {
    when (sender) {
        is TdApi.MessageSenderUser -> UserAvatar(TdStore.users[sender.userId], size, modifier)
        is TdApi.MessageSenderChat -> TdStore.chats[sender.chatId]?.let { ChatAvatar(it, size, modifier) } ?: Avatar("?", 0, null, null, size, modifier)
        else -> Avatar("?", 0, null, null, size, modifier)
    }
}

@Composable
fun rememberHaptic(): (HapticFeedbackType) -> Unit {
    val h = LocalHapticFeedback.current
    return { t -> if (Prefs.haptics.value) h.performHapticFeedback(t) }
}

/** accent colour for a sender name in groups, chosen from scheme roles */
@Composable
fun nameColor(id: Long): Color {
    val cs = MaterialTheme.colorScheme
    return when ((((id % 4) + 4) % 4).toInt()) {
        0 -> cs.primary
        1 -> cs.tertiary
        2 -> cs.secondary
        else -> cs.error
    }
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(androidx.compose.foundation.shape.CircleShape).background(color))
}

/** The full qwgram logo (never cropped), tinted with the theme; springs in when first shown. */
@Composable
fun QwLogo(width: Dp, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val scale = remember { androidx.compose.animation.core.Animatable(0.7f) }
    val rot = remember { androidx.compose.animation.core.Animatable(-30f) }
    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, Springs.spatial()) }
        rot.animateTo(0f, Springs.spatial())
    }
    androidx.compose.foundation.Image(
        androidx.compose.ui.res.painterResource(app.qwgram.next.R.drawable.qw_logo), "qwgram",
        modifier.width(width).aspectRatio(160f / 99.74f).graphicsLayer { scaleX = scale.value; scaleY = scale.value; rotationZ = rot.value },
        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(color),
    )
}
