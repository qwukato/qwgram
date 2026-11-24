package app.qwgram.next.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.qwgram.next.Prefs
import app.qwgram.next.td.Images
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import java.io.File

/** Own photos as wallpapers: for every chat, for one chat, or behind the whole interface. */
object Wallpapers {
    /** copies the picked photo into app files (the picker's uri doesn't live forever) */
    suspend fun import(ctx: Context, uri: Uri, name: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(ctx.filesDir, "wallpapers").apply { mkdirs() }
            val f = File(dir, "$name-${System.currentTimeMillis()}.jpg")
            ctx.contentResolver.openInputStream(uri)!!.use { i -> f.outputStream().use { i.copyTo(it) } }
            // drop older copies with the same name
            dir.listFiles()?.filter { it.name.startsWith("$name-") && it != f }?.forEach { it.delete() }
            f.absolutePath
        }.getOrNull()
    }
}

/** a photo picker that imports the chosen picture and hands back its path */
@Composable
fun rememberWallpaperPicker(name: String, onPicked: (String) -> Unit): () -> Unit {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { Wallpapers.import(ctx, uri, name)?.let(onPicked) }
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

@Composable
fun rememberPathBitmap(path: String?, maxSide: Int = 1600): ImageBitmap? {
    if (path.isNullOrEmpty()) return null
    var bmp by remember(path) { mutableStateOf(Images.cached(path, maxSide)?.asImageBitmap()) }
    LaunchedEffect(path) { if (bmp == null) bmp = withContext(Dispatchers.IO) { Images.decode(path, maxSide)?.asImageBitmap() } }
    return bmp
}

/** a photo filling the area, with a theme-coloured scrim on top so text stays readable */
@Composable
fun PhotoWallpaper(bmp: ImageBitmap, dim: Float, modifier: Modifier = Modifier, blurred: Boolean = false) {
    Box(modifier.fillMaxSize()) {
        Image(bmp, null, Modifier.fillMaxSize().then(if (blurred) Modifier.blur(18.dp) else Modifier), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = dim.coerceIn(0f, 0.95f))))
    }
}

/** true when the own photo should be drawn behind every screen */
@Composable
fun appWallpaperOn(): Boolean = Prefs.wallpaperEverywhere.value && Prefs.wallpaperImage.value.isNotEmpty()

/** the app-wide photo behind a screen (whole-interface mode) */
@Composable
fun AppWallpaper(modifier: Modifier = Modifier) {
    val bmp = rememberPathBitmap(Prefs.wallpaperImage.value)
    if (bmp != null) PhotoWallpaper(bmp, Prefs.wallpaperDim.value.coerceAtLeast(0.25f), modifier)
    else Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
}

private fun argb(c: Int) = Color(0xFF000000.toInt() or c)

private fun fillBrush(f: TdApi.BackgroundFill?): Brush? = when (f) {
    is TdApi.BackgroundFillSolid -> Brush.linearGradient(listOf(argb(f.color), argb(f.color)))
    is TdApi.BackgroundFillGradient -> {
        val a = Math.toRadians(f.rotationAngle.toDouble())
        Brush.linearGradient(
            listOf(argb(f.topColor), argb(f.bottomColor)),
            start = androidx.compose.ui.geometry.Offset(0.5f + 0.5f * Math.sin(a).toFloat(), 0f),
            end = androidx.compose.ui.geometry.Offset.Infinite,
        )
    }
    is TdApi.BackgroundFillFreeformGradient -> Brush.linearGradient(f.colors.map { argb(it) }.ifEmpty { listOf(Color.Gray, Color.Gray) }.let { if (it.size == 1) it + it else it })
    else -> null
}

/**
 * Chat wallpaper, in priority: this chat's own photo → the wallpaper set in Telegram for the chat →
 * the app-wide photo → the built-in patterns ([fallback]).
 */
@Composable
fun ChatBackdrop(chat: TdApi.Chat?, fallback: @Composable () -> Unit) {
    Prefs.chatWallpaperVersion.intValue // re-read after a change
    val own = chat?.let { Prefs.chatWallpaper(it.id) }
    val ownBmp = rememberPathBitmap(own)
    if (ownBmp != null) { PhotoWallpaper(ownBmp, Prefs.wallpaperDim.value * 0.6f); return }
    val bg = chat?.background?.background
    if (Prefs.tgWallpapers.value && bg != null) {
        when (val t = bg.type) {
            is TdApi.BackgroundTypeWallpaper -> {
                val bmp = rememberFileBitmap(bg.document?.document, maxSide = 1600)
                if (bmp != null) { PhotoWallpaper(bmp, (chat.background.darkThemeDimming / 100f) * 0.8f, blurred = t.isBlurred); return }
            }
            is TdApi.BackgroundTypeFill -> fillBrush(t.fill)?.let { b -> Box(Modifier.fillMaxSize().background(b)); return }
            is TdApi.BackgroundTypePattern -> fillBrush(t.fill)?.let { b ->
                Box(Modifier.fillMaxSize().background(b)) {
                    // the pattern's preview picture, at Telegram's intensity
                    val thumb = rememberFileBitmap(bg.document?.thumbnail?.file, maxSide = 1600)
                    if (thumb != null) Image(
                        thumb, null, Modifier.fillMaxSize().graphicsLayer { alpha = (kotlin.math.abs(t.intensity) / 100f).coerceIn(0.1f, 0.6f) },
                        contentScale = ContentScale.Crop,
                    )
                }
                return
            }
            else -> {}
        }
    }
    if (Prefs.wallpaper.value == "image") {
        val bmp = rememberPathBitmap(Prefs.wallpaperImage.value)
        if (bmp != null) { PhotoWallpaper(bmp, Prefs.wallpaperDim.value * 0.6f); return }
    }
    fallback()
}
