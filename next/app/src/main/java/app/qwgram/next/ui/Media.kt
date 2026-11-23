package app.qwgram.next.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import app.qwgram.next.td.Images
import app.qwgram.next.td.Td
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.Texts
import org.drinkless.tdlib.TdApi
import java.io.File

/** One shared player for voice notes and audio files. */
object Player {
    var fileId by mutableIntStateOf(0)
        private set
    var playing by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    private var mp: MediaPlayer? = null
    private val h = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            val p = mp ?: return
            try {
                if (p.duration > 0) progress = p.currentPosition / p.duration.toFloat()
            } catch (_: Exception) {
            }
            if (playing) h.postDelayed(this, 50)
        }
    }

    fun toggle(file: TdApi.File) {
        val f = Images.current(file) ?: return
        if (fileId == f.id && mp != null) {
            if (playing) { mp?.pause(); playing = false } else { mp?.start(); playing = true; h.post(tick) }
            return
        }
        if (!f.local.isDownloadingCompleted) {
            Td.send(TdApi.DownloadFile(f.id, 32, 0, 0, true)) { r ->
                if (r is TdApi.File && r.local.isDownloadingCompleted) start(r)
            }
            fileId = f.id
            return
        }
        start(f)
    }

    private fun start(f: TdApi.File) {
        stop()
        try {
            val p = MediaPlayer()
            p.setDataSource(f.local.path)
            p.setOnCompletionListener { playing = false; progress = 0f }
            p.prepare()
            p.start()
            mp = p
            fileId = f.id
            playing = true
            h.post(tick)
        } catch (e: Exception) {
            playing = false
        }
    }

    fun stop() {
        try {
            mp?.release()
        } catch (_: Exception) {
        }
        mp = null
        playing = false
        progress = 0f
    }
}

/** Voice note recorder: OGG/Opus, the format Telegram expects (Android 10+). */
class VoiceRecorder(private val context: Context) {
    private var rec: MediaRecorder? = null
    private var file: File? = null
    private var started = 0L

    val supported: Boolean get() = Build.VERSION.SDK_INT >= 29

    fun start(): Boolean {
        if (!supported) return false
        return try {
            val f = File(context.cacheDir, "voice_${System.currentTimeMillis()}.ogg")
            @Suppress("DEPRECATION")
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.OGG)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
            r.setAudioSamplingRate(48000)
            r.setAudioEncodingBitRate(32000)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
            rec = r
            file = f
            started = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            rec = null
            false
        }
    }

    /** returns (file, seconds) or null if cancelled / too short */
    fun stop(cancel: Boolean): Pair<File, Int>? {
        val r = rec ?: return null
        rec = null
        val secs = ((System.currentTimeMillis() - started) / 1000).toInt()
        try {
            r.stop()
        } catch (_: Exception) {
        }
        r.release()
        val f = file ?: return null
        if (cancel || secs < 1) {
            f.delete()
            return null
        }
        return f to secs
    }

    fun elapsed(): Int = if (rec == null) 0 else ((System.currentTimeMillis() - started) / 1000).toInt()
}

object Opener {
    fun url(context: Context, raw: String) {
        val url = Texts.cleanUrl(if (raw.contains("://")) raw else "https://$raw")
        val tme = Regex("^https?://(?:t\\.me|telegram\\.me)/([A-Za-z0-9_]{4,})/?$").find(url)
        if (tme != null) {
            username(context, tme.groupValues[1])
            return
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Нет приложения для ссылки", Toast.LENGTH_SHORT).show()
        }
    }

    fun username(context: Context, name: String) {
        Td.send(TdApi.SearchPublicChat(name.removePrefix("@"))) { r ->
            if (r is TdApi.Chat) Nav.push(Route.Chat(r.id)) else Toast.makeText(context, "Не найдено: @$name", Toast.LENGTH_SHORT).show()
        }
    }

    /** downloads (if needed) and opens a file with another app */
    fun file(context: Context, file: TdApi.File, mime: String?) {
        val f = Images.current(file) ?: return
        if (!f.local.isDownloadingCompleted) {
            Toast.makeText(context, "Загрузка…", Toast.LENGTH_SHORT).show()
            Td.send(TdApi.DownloadFile(f.id, 32, 0, 0, true)) { r -> if (r is TdApi.File && r.local.isDownloadingCompleted) open(context, r, mime) }
            return
        }
        open(context, f, mime)
    }

    private fun open(context: Context, f: TdApi.File, mime: String?) {
        try {
            val file = File(f.local.path)
            val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
            val type = mime?.takeIf { it.isNotBlank() } ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
            context.startActivity(
                Intent(Intent.ACTION_VIEW).setDataAndType(uri, type)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось открыть файл", Toast.LENGTH_SHORT).show()
        }
    }

    fun location(context: Context, lat: Double, lon: Double) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }

    @Suppress("unused")
    fun chatTitle(id: Long) = TdStore.chats[id]?.title
}
