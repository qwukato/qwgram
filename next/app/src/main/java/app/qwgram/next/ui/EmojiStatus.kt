package app.qwgram.next.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.qwgram.next.td.Td
import app.qwgram.next.td.rememberFileBitmap
import org.drinkless.tdlib.TdApi

/** custom emoji (stickers) by id, fetched once */
object CustomEmoji {
    val stickers = mutableStateMapOf<Long, TdApi.Sticker>()
    private val asked = HashSet<Long>()
    fun request(id: Long) {
        if (id == 0L || !asked.add(id)) return
        Td.send(TdApi.GetCustomEmojiStickers(longArrayOf(id))) { r -> (r as? TdApi.Stickers)?.stickers?.forEach { stickers[id] = it } }
    }
}

/** a custom emoji picture: the sticker itself when static, its preview frame when animated */
@Composable
fun CustomEmojiImage(id: Long, size: Dp, modifier: Modifier = Modifier) {
    LaunchedEffect(id) { CustomEmoji.request(id) }
    val s = CustomEmoji.stickers[id] ?: return
    val static = s.format is TdApi.StickerFormatWebp
    val bmp = rememberFileBitmap(if (static) s.sticker else (s.thumbnail?.file ?: s.sticker), maxSide = 160) ?: return
    Image(bmp, s.emoji, modifier.size(size), contentScale = ContentScale.Fit)
}

/**
 * The user's emoji status exactly where Telegram shows it (after the name): their chosen custom emoji or the
 * collectible gift model; Premium without a status shows the Premium star; nothing for others.
 */
@Composable
fun EmojiStatusMark(user: TdApi.User?, size: Dp = 20.dp, tint: Color = MaterialTheme.colorScheme.primary, gap: Dp = 4.dp) {
    if (user == null) return
    val st = user.emojiStatus
    val expired = st != null && st.expirationDate != 0 && st.expirationDate < System.currentTimeMillis() / 1000
    val id = when (val t = st?.type) {
        is TdApi.EmojiStatusTypeCustomEmoji -> t.customEmojiId
        is TdApi.EmojiStatusTypeUpgradedGift -> t.modelCustomEmojiId
        else -> 0L
    }
    if (id != 0L && !expired) {
        Spacer(Modifier.width(gap))
        Box(Modifier.size(size)) { CustomEmojiImage(id, size) }
    } else if (user.isPremium) {
        Spacer(Modifier.width(gap))
        Icon(Icons.Filled.Star, "Premium", Modifier.size(size * 0.85f), tint = tint)
    }
}
