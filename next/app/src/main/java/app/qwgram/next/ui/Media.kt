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

/** a music track for the player */
class Track(val file: TdApi.File, val title: String, val performer: String, val durationSec: Int, val cover: TdApi.File?, val mini: TdApi.Minithumbnail?) {
    companion object {
        fun of(a: TdApi.Audio) = Track(
            a.audio, a.title.ifBlank { a.fileName.ifBlank { "Без названия" } }, a.performer, a.duration,
            a.externalAlbumCovers?.maxByOrNull { it.width * it.height }?.file ?: a.albumCoverThumbnail?.file, a.albumCoverMinithumbnail,
        )
    }
}

/**
 * One shared player. Voice notes play on their own; music plays from a queue (chat audio, profile music) with
 * next/previous, shuffle, repeat, speed and seeking, and shows the mini / full player.
 */
object Player {
    var fileId by mutableIntStateOf(0)
        private set
    var playing by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var positionMs by mutableIntStateOf(0)
        private set
    var durationMs by mutableIntStateOf(0)
        private set
    var loading by mutableStateOf(false)
        private set
    /** music queue; empty while a voice note plays */
    var queue by mutableStateOf<List<Track>>(emptyList())
        private set
    var index by mutableIntStateOf(0)
        private set
    var shuffle by mutableStateOf(false)
    var repeat by mutableIntStateOf(0)        // 0 off, 1 all, 2 one
    var speed by mutableFloatStateOf(1f)
        private set
    /** the mini player was closed by the user (until the next track is started) */
    var dismissed by mutableStateOf(false)
    /** audios of the open chat: tapping one plays through the rest */
    var contextQueue: List<TdApi.Audio> = emptyList()

    val track: Track? get() = queue.getOrNull(index)

    private var mp: MediaPlayer? = null
    private val h = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            val p = mp ?: return
            try {
                val d = p.duration
                if (d > 0) { durationMs = d; positionMs = p.currentPosition; progress = p.currentPosition / d.toFloat() }
            } catch (_: Exception) {
            }
            if (playing) h.postDelayed(this, 50)
        }
    }

    /** voice notes (and single files) */
    fun toggle(file: TdApi.File) {
        val f = Images.current(file) ?: return
        if (fileId == f.id && mp != null) { if (playing) pause() else resume(); return }
        queue = emptyList()
        load(f)
    }

    /** an audio message: plays it within the chat's audio list */
    fun toggleAudio(a: TdApi.Audio) {
        if (fileId == a.audio.id && mp != null) { if (playing) pause() else resume(); return }
        val list = contextQueue.takeIf { q -> q.any { it.audio.id == a.audio.id } } ?: listOf(a)
        playQueue(list.map { Track.of(it) }, list.indexOfFirst { it.audio.id == a.audio.id }.coerceAtLeast(0))
    }

    fun playQueue(tracks: List<Track>, start: Int) {
        if (tracks.isEmpty()) return
        queue = tracks
        index = start.coerceIn(0, tracks.lastIndex)
        dismissed = false
        load(tracks[index].file)
    }

    fun pause() { try { mp?.pause() } catch (_: Exception) {}; playing = false }
    fun resume() { try { mp?.start(); playing = true; h.post(tick) } catch (_: Exception) {} }
    fun playPause() { if (playing) pause() else if (mp != null) resume() else track?.let { load(it.file) } }

    fun next(user: Boolean = true) {
        if (queue.isEmpty()) return
        val n = when {
            shuffle && queue.size > 1 -> (queue.indices - index).random()
            index < queue.lastIndex -> index + 1
            repeat == 1 || user -> 0
            else -> { pause(); seekTo(0f); return }
        }
        index = n; load(queue[n].file)
    }

    fun previous() {
        if (queue.isEmpty()) return
        if (positionMs > 3000) { seekTo(0f); return }
        index = if (index > 0) index - 1 else queue.lastIndex
        load(queue[index].file)
    }

    fun seekTo(fraction: Float) {
        val p = mp ?: return
        try { val d = p.duration; if (d > 0) { p.seekTo((d * fraction.coerceIn(0f, 1f)).toInt()); positionMs = p.currentPosition; progress = fraction } } catch (_: Exception) {}
    }

    fun cycleSpeed() {
        speed = when (speed) { 1f -> 1.25f; 1.25f -> 1.5f; 1.5f -> 2f; 2f -> 0.75f; else -> 1f }
        applySpeed()
    }

    private fun applySpeed() {
        val p = mp ?: return
        try { if (Build.VERSION.SDK_INT >= 23) { val was = p.isPlaying; p.playbackParams = p.playbackParams.setSpeed(speed); if (!was) p.pause() } } catch (_: Exception) {}
    }

    fun close() { stop(); queue = emptyList(); dismissed = true }

    private fun load(file: TdApi.File) {
        val f = Images.current(file) ?: return
        stop()
        fileId = f.id
        if (f.local.isDownloadingCompleted) { start(f); return }
        loading = true
        Td.send(TdApi.DownloadFile(f.id, 32, 0, 0, true)) { r ->
            h.post {
                loading = false
                if (r is TdApi.File && r.local.isDownloadingCompleted && fileId == r.id) start(r)
            }
        }
    }

    private fun start(f: TdApi.File) {
        try {
            val p = MediaPlayer()
            p.setDataSource(f.local.path)
            p.setOnCompletionListener {
                if (queue.isNotEmpty()) {
                    if (repeat == 2) { seekTo(0f); resume() } else next(user = false)
                } else { playing = false; progress = 0f }
            }
            p.prepare()
            mp = p
            fileId = f.id
            durationMs = p.duration
            applySpeed()
            p.start()
            playing = true
            h.post(tick)
        } catch (e: Exception) {
            playing = false
        }
    }

    fun stop() {
        try { mp?.release() } catch (_: Exception) {}
        mp = null
        playing = false
        progress = 0f
        positionMs = 0
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
        // sticker set links open the set preview right here (add it in one tap)
        if (url.contains("/addstickers/") || url.startsWith("tg://addstickers")) { stickerSetName(url)?.let { StickerSetPreview.openName(it); return } }
        // every Telegram link (chats, invites, messages, bots, proxies…) opens right here
        if (Links.isTelegram(url)) { Links.open(context, url); return }
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
