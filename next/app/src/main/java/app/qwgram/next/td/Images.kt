package app.qwgram.next.td

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi

/** Small image loader on top of TDLib files: downloads on demand, decodes off the main thread, caches. */
object Images {
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 6).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun current(file: TdApi.File?): TdApi.File? = file?.let { TdStore.files[it.id] ?: it }

    fun download(file: TdApi.File?, priority: Int = 16) {
        val f = current(file) ?: return
        if (f.local.isDownloadingCompleted || f.local.isDownloadingActive || !f.local.canBeDownloaded) return
        Td.send(TdApi.DownloadFile(f.id, priority, 0, 0, false))
    }

    fun cached(path: String, maxSide: Int): Bitmap? = cache.get("$path#$maxSide")

    fun decode(path: String, maxSide: Int): Bitmap? {
        val key = "$path#$maxSide"
        cache.get(key)?.let { return it }
        return try {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, o)
            var sample = 1
            while (o.outWidth / (sample * 2) >= maxSide && o.outHeight / (sample * 2) >= maxSide) sample *= 2
            val b = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
            cache.put(key, b)
            b
        } catch (e: Throwable) {
            null
        }
    }

    fun mini(m: TdApi.Minithumbnail?): ImageBitmap? {
        if (m == null || m.data.isEmpty()) return null
        val key = "mini#" + m.data.contentHashCode()
        cache.get(key)?.let { return it.asImageBitmap() }
        return try {
            BitmapFactory.decodeByteArray(m.data, 0, m.data.size)?.also { cache.put(key, it) }?.asImageBitmap()
        } catch (e: Throwable) {
            null
        }
    }
}

/** Bitmap for a TDLib file; triggers the download and recomposes when the file arrives. */
@Composable
fun rememberFileBitmap(file: TdApi.File?, maxSide: Int = 512, autoDownload: Boolean = true): ImageBitmap? {
    if (file == null) return null
    val live = TdStore.files[file.id] ?: file
    val path = if (live.local.isDownloadingCompleted) live.local.path else null
    var bmp by remember(file.id, maxSide) { mutableStateOf(path?.let { Images.cached(it, maxSide)?.asImageBitmap() }) }
    LaunchedEffect(file.id, autoDownload) {
        if (autoDownload) Images.download(live)
    }
    LaunchedEffect(path, maxSide) {
        if (path != null && bmp == null) {
            bmp = withContext(Dispatchers.IO) { Images.decode(path, maxSide)?.asImageBitmap() }
        }
    }
    return bmp
}
