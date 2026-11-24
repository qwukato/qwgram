package app.qwgram.next.ui

import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import app.qwgram.next.Prefs
import app.qwgram.next.td.Images
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.rememberFileBitmap
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.zip.GZIPInputStream

/** TGS files are gzipped Lottie JSON: unpacked once and kept (small) */
private object TgsCache {
    val json = LruCache<String, String>(96)
    fun load(path: String): String? = json.get(path) ?: runCatching {
        GZIPInputStream(File(path).inputStream().buffered()).use { it.readBytes().toString(Charsets.UTF_8) }
    }.getOrNull()?.also { json.put(path, it) }
}

/** the local path of a Telegram file once it is downloaded (starts the download) */
@Composable
fun rememberLocalPath(file: TdApi.File?): String? {
    if (file == null) return null
    val live = TdStore.files[file.id] ?: file
    LaunchedEffect(file.id) { Images.download(live) }
    return if (live.local.isDownloadingCompleted) live.local.path else null
}

/** an animated (TGS / Lottie) sticker, emoji or status, looping; nothing until it is ready */
@Composable
fun TgsImage(file: TdApi.File, modifier: Modifier = Modifier, loop: Boolean = true, placeholder: @Composable () -> Unit = {}) {
    val path = rememberLocalPath(file)
    var json by remember(path) { mutableStateOf(path?.let { TgsCache.json.get(it) }) }
    LaunchedEffect(path) { if (path != null && json == null) json = withContext(Dispatchers.IO) { TgsCache.load(path) } }
    val j = json
    if (j == null) { Box(modifier) { placeholder() }; return }
    val comp by rememberLottieComposition(LottieCompositionSpec.JsonString(j), cacheKey = "tgs:$path")
    if (comp == null) { Box(modifier) { placeholder() }; return }
    LottieAnimation(
        comp, modifier = modifier,
        iterations = if (loop) LottieConstants.IterateForever else 1,
        isPlaying = Prefs.motion.value != "off",
        contentScale = ContentScale.Fit,
        enableMergePaths = true,
    )
}

/**
 * Any Telegram sticker / custom emoji: animated TGS plays, static WebP shows the picture; video (WebM) stickers
 * show their preview frame (playing transparent VP9 video needs a decoder Android doesn't ship).
 */
@Composable
fun StickerContent(s: TdApi.Sticker, modifier: Modifier = Modifier, placeholder: @Composable () -> Unit = {}) {
    when (s.format) {
        is TdApi.StickerFormatTgs -> {
            val thumb = rememberFileBitmap(s.thumbnail?.file, maxSide = 256)
            TgsImage(s.sticker, modifier) { if (thumb != null) Image(thumb, s.emoji, modifier, contentScale = ContentScale.Fit) else placeholder() }
        }
        else -> {
            val static = s.format is TdApi.StickerFormatWebp
            val bmp = rememberFileBitmap(if (static) s.sticker else (s.thumbnail?.file ?: s.sticker), maxSide = 512)
            if (bmp != null) Image(bmp, s.emoji, modifier, contentScale = ContentScale.Fit) else Box(modifier) { placeholder() }
        }
    }
}
