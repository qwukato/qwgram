package app.qwgram.next.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.qwgram.next.td.Images
import app.qwgram.next.td.Texts
import app.qwgram.next.td.rememberFileBitmap
import org.drinkless.tdlib.TdApi

/** width / height of an album item (clamped like Telegram so nothing is a sliver) */
private fun ratioOf(c: TdApi.MessageContent): Float {
    val (w, h) = when (c) {
        is TdApi.MessagePhoto -> c.photo.sizes.maxByOrNull { it.width * it.height }?.let { it.width to it.height } ?: (1 to 1)
        is TdApi.MessageVideo -> c.video.width to c.video.height
        else -> 1 to 1
    }
    return (w.toFloat() / h.coerceAtLeast(1)).let { if (it <= 0f) 1f else it }.coerceIn(0.6f, 2f)
}

/** how many items go in each row (Telegram's mosaic, simplified): 2–10 items */
private fun rowsOf(ratios: List<Float>): List<Int> = when (ratios.size) {
    1 -> listOf(1)
    2 -> if (ratios.all { it > 1.25f }) listOf(1, 1) else listOf(2)
    3 -> if (ratios[0] > 1.1f) listOf(1, 2) else listOf(3)
    4 -> if (ratios[0] > 1.2f) listOf(1, 3) else listOf(2, 2)
    5 -> listOf(2, 3)
    6 -> listOf(3, 3)
    7 -> listOf(2, 2, 3)
    8 -> listOf(2, 3, 3)
    9 -> listOf(3, 3, 3)
    else -> listOf(3, 3, 4)
}

/**
 * Several photos / videos sent together, as one mosaic (Telegram): rows whose items share a height, each row as
 * tall as its items' proportions ask; the corners follow the bubble.
 */
@Composable
fun AlbumView(items: List<TdApi.Message>, shapeTop: Dp, onPhoto: (TdApi.Photo) -> Unit) {
    // oldest first, the order they were picked in
    val list = remember(items) { items.sortedBy { it.id } }
    val ratios = remember(list) { list.map { ratioOf(it.content) } }
    val rows = remember(ratios) {
        var i = 0
        rowsOf(ratios).map { n -> val r = (i until (i + n).coerceAtMost(list.size)).toList(); i += n; r }.filter { it.isNotEmpty() }
    }
    val gap = 2.dp
    BoxWithConstraints(
        Modifier.fillMaxWidth().widthIn(min = 240.dp)
            .clip(RoundedCornerShape(topStart = shapeTop, topEnd = shapeTop, bottomStart = 6.dp, bottomEnd = 6.dp)),
    ) {
        val w = maxWidth
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            rows.forEach { idx ->
                val sum = idx.sumOf { ratios[it].toDouble() }.toFloat()
                // the row's height makes its items fill the width at their proportions
                val h = ((w - gap * (idx.size - 1)) / sum).coerceIn(72.dp, w * 1.1f)
                Row(Modifier.fillMaxWidth().height(h), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    idx.forEach { i -> AlbumCell(list[i], Modifier.weight(ratios[i]).fillMaxHeight(), onPhoto) }
                }
            }
        }
    }
}

@Composable
private fun AlbumCell(m: TdApi.Message, modifier: Modifier, onPhoto: (TdApi.Photo) -> Unit) {
    val c = m.content
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
        when (c) {
            is TdApi.MessagePhoto -> {
                val size = c.photo.sizes.filter { maxOf(it.width, it.height) <= 1280 }.maxByOrNull { it.width * it.height } ?: c.photo.sizes.minByOrNull { it.width * it.height }
                val bmp = rememberFileBitmap(size?.photo, maxSide = 720)
                val mini = remember(c.photo) { Images.mini(c.photo.minithumbnail) }
                Box(Modifier.fillMaxSize().clickable { onPhoto(c.photo) }) {
                    if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else if (mini != null) Image(mini, null, Modifier.fillMaxSize().blur(12.dp), contentScale = ContentScale.Crop)
                }
            }
            is TdApi.MessageVideo -> {
                val v = c.video
                val bmp = rememberFileBitmap(v.thumbnail?.file, maxSide = 512)
                val mini = remember(v) { Images.mini(v.minithumbnail) }
                Box(Modifier.fillMaxSize().clickable {
                    VideoViewer.open(VideoItem(v.video, v.thumbnail?.file, v.minithumbnail, v.width, v.height, v.duration))
                }, contentAlignment = Alignment.Center) {
                    (bmp ?: mini)?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PlayArrow, "Смотреть", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.45f), contentColor = Color.White, modifier = Modifier.align(Alignment.TopStart).padding(6.dp)) {
                        Text(Texts.duration(v.duration), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                    }
                }
            }
            else -> Text(Texts.preview(m), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(6.dp))
        }
    }
}

/** the message of an album that carries the caption (Telegram shows it under the mosaic), else the newest */
fun albumLead(items: List<TdApi.Message>): TdApi.Message =
    items.firstOrNull { Texts.caption(it.content)?.text?.isNotBlank() == true } ?: items.maxBy { it.id }
