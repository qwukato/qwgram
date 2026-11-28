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
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.asImageBitmap

/** a music track for the player; [chatId]/[messageId] point at the message it came from (0 = none) */
class Track(
    val file: TdApi.File, val title: String, val performer: String, val durationSec: Int, val cover: TdApi.File?, val mini: TdApi.Minithumbnail?,
    val audio: TdApi.Audio? = null, val chatId: Long = 0, val messageId: Long = 0,
    /** a second cover source when the first can't be loaded (the album thumbnail vs. an external cover) */
    val altCover: TdApi.File? = null,
) {
    companion object {
        fun of(a: TdApi.Audio, chatId: Long = 0, messageId: Long = 0) = Track(
            a.audio, a.title.ifBlank { a.fileName.substringBeforeLast('.').ifBlank { "Без названия" } }, a.performer, a.duration,
            // the biggest cover there is: the album thumbnail, or the largest external album cover
            (a.externalAlbumCovers?.toList().orEmpty() + listOfNotNull(a.albumCoverThumbnail)).maxByOrNull { it.width * it.height }?.file,
            a.albumCoverMinithumbnail, a, chatId, messageId,
            (listOfNotNull(a.albumCoverThumbnail) + a.externalAlbumCovers?.toList().orEmpty()).map { it.file }.firstOrNull { it.id != (a.externalAlbumCovers?.toList().orEmpty() + listOfNotNull(a.albumCoverThumbnail)).maxByOrNull { t -> t.width * t.height }?.file?.id },
        )
        fun of(m: TdApi.Message): Track? = (m.content as? TdApi.MessageAudio)?.audio?.let { of(it, m.chatId, m.id) }
    }
}

/** full-quality covers: the picture embedded in the audio file itself (read once the file is downloaded) */
object TrackArt {
    val art = androidx.compose.runtime.mutableStateMapOf<Int, androidx.compose.ui.graphics.ImageBitmap>()
    private val tried = HashSet<Int>()

    fun ensure(f: TdApi.File) {
        if (!f.local.isDownloadingCompleted || !tried.add(f.id)) return
        val path = f.local.path
        Player.io.launch {
            val bmp = runCatching {
                val r = android.media.MediaMetadataRetriever()
                try {
                    r.setDataSource(path)
                    val bytes = r.embeddedPicture ?: return@runCatching null
                    val o = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
                    var sample = 1
                    while (maxOf(o.outWidth, o.outHeight) / (sample * 2) >= 1200) sample *= 2
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })
                } finally { runCatching { r.release() } }
            }.getOrNull()
            if (bmp != null) { val ib = bmp.asImageBitmap(); h.post { art[f.id] = ib } }
        }
    }
    private val h = Handler(Looper.getMainLooper())
}

/** song lyrics from LRCLIB (free, open): synced lines when there are any, otherwise plain text */
object Lyrics {
    class Result(val synced: List<Pair<Int, String>>, val plain: String?) { val found get() = synced.isNotEmpty() || !plain.isNullOrBlank() }
    private val cache = HashMap<String, Result>()

    suspend fun load(t: Track): Result {
        val key = t.performer + "|" + t.title
        cache[key]?.let { return it }
        val r = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            fun enc(v: String) = java.net.URLEncoder.encode(v, "UTF-8")
            fun get(url: String): String? = runCatching {
                val c = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                c.connectTimeout = 8000; c.readTimeout = 8000
                c.setRequestProperty("User-Agent", "qwgram Next (https://github.com/qwukato/qwgram)")
                try { if (c.responseCode == 200) c.inputStream.bufferedReader().readText() else null } finally { c.disconnect() }
            }.getOrNull()
            fun parse(o: org.json.JSONObject): Result {
                val synced = o.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" }?.let { parseLrc(it) }.orEmpty()
                val plain = o.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" }
                return Result(synced, plain)
            }
            val exact = if (t.performer.isNotBlank()) get("https://lrclib.net/api/get?track_name=${enc(t.title)}&artist_name=${enc(t.performer)}&duration=${t.durationSec}")
                ?.let { runCatching { parse(org.json.JSONObject(it)) }.getOrNull() } else null
            if (exact?.found == true) exact
            else get("https://lrclib.net/api/search?q=${enc((t.performer + " " + t.title).trim())}")?.let { body ->
                runCatching {
                    val arr = org.json.JSONArray(body)
                    val all = (0 until arr.length()).map { arr.getJSONObject(it) }
                    // the closest by length, preferring synced lyrics
                    val best = all.sortedWith(compareBy<org.json.JSONObject> { if (it.optString("syncedLyrics").let { s -> s.isNotBlank() && s != "null" }) 0 else 1 }
                        .thenBy { kotlin.math.abs(it.optDouble("duration", 0.0) - t.durationSec) }).firstOrNull { kotlin.math.abs(it.optDouble("duration", 0.0) - t.durationSec) < 8 || t.durationSec == 0 }
                    best?.let { parse(it) }
                }.getOrNull()
            } ?: Result(emptyList(), null)
        }
        cache[key] = r
        return r
    }

    private fun parseLrc(s: String): List<Pair<Int, String>> {
        val re = Regex("""\[(\d+):(\d+)(?:[.:](\d+))?]""")
        val out = ArrayList<Pair<Int, String>>()
        s.lineSequence().forEach { line ->
            val tags = re.findAll(line).toList()
            if (tags.isEmpty()) return@forEach
            val text = line.substring(tags.last().range.last + 1).trim()
            tags.forEach { m ->
                val frac = m.groupValues[3].let { if (it.isEmpty()) 0 else (it + "00").take(3).toInt() }
                out += (m.groupValues[1].toInt() * 60_000 + m.groupValues[2].toInt() * 1000 + frac) to text
            }
        }
        return out.sortedBy { it.first }
    }
}

/**
 * One shared player. Voice notes play on their own; music plays from a queue (a chat's audio, profile music) with
 * next/previous, shuffle (a fixed shuffled order, so the next cover can be shown ahead), repeat, speed, seeking,
 * reordering, a sleep timer, audio focus and "headphones unplugged" handling.
 */
object Player {
    internal val io = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    private val main = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)

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
    private var shuffleOn by mutableStateOf(false)
    var shuffle: Boolean
        get() = shuffleOn
        set(v) { shuffleOn = v; rebuildOrder() }
    var repeat by mutableIntStateOf(0)        // 0 off, 1 all, 2 one
    var speed by mutableFloatStateOf(1f)
        private set
    /** the mini player was closed by the user (until the next track is started) */
    var dismissed by mutableStateOf(false)
    /** where the queue comes from ("Музыка профиля", a chat title) */
    var source by mutableStateOf("")
        private set
    /** audio messages of the open chat: tapping one plays through the rest */
    var contextQueue: List<TdApi.Message> = emptyList()
    /** sleep timer: wall-clock end (0 = off), or -1 = at the end of this track */
    var sleepAt by androidx.compose.runtime.mutableLongStateOf(0L)
        private set

    val track: Track? get() = queue.getOrNull(index)

    private var order = IntArray(0)
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
            if (playing) h.postDelayed(this, 40)
        }
    }
    private val sleeper = Runnable { if (sleepAt > 0) { sleepAt = 0; pause() } }

    /** voice notes (and single files) */
    fun toggle(file: TdApi.File) {
        val f = Images.current(file) ?: return
        if (fileId == f.id && mp != null) { if (playing) pause() else resume(); return }
        queue = emptyList()
        load(f)
    }

    /** an audio message: plays it within the chat's audio (all of it is fetched in the background) */
    fun toggleAudio(a: TdApi.Audio) {
        if (fileId == a.audio.id && mp != null) { if (playing) pause() else resume(); return }
        val ctxList = contextQueue.filter { it.content is TdApi.MessageAudio }
        val hit = ctxList.firstOrNull { (it.content as TdApi.MessageAudio).audio.audio.id == a.audio.id }
        val list = if (hit != null) ctxList.mapNotNull { Track.of(it) } else listOf(Track.of(a))
        val chatId = hit?.chatId ?: 0L
        playQueue(list, list.indexOfFirst { it.file.id == a.audio.id }.coerceAtLeast(0), TdStore.chats[chatId]?.title.orEmpty())
        if (chatId != 0L) main.launch {
            // the whole chat's music, oldest first, like Telegram's playlist
            val r = Td.callOrNull(TdApi.SearchChatMessages(chatId, null, "", null, 0, 0, 100, TdApi.SearchMessagesFilterAudio())) ?: return@launch
            val all = r.messages.toList().asReversed().mapNotNull { Track.of(it) }
            val cur = track ?: return@launch
            if (all.size <= queue.size || all.none { it.messageId == cur.messageId }) return@launch
            queue = all
            index = all.indexOfFirst { it.messageId == cur.messageId }
            rebuildOrder()
        }
    }

    fun playQueue(tracks: List<Track>, start: Int, from: String = "") {
        if (tracks.isEmpty()) return
        queue = tracks
        index = start.coerceIn(0, tracks.lastIndex)
        source = from
        dismissed = false
        rebuildOrder()
        load(tracks[index].file)
    }

    fun playAt(i: Int) { if (i in queue.indices) { index = i; dismissed = false; load(queue[i].file) } }

    fun pause() { try { mp?.pause() } catch (_: Exception) {}; playing = false; noisy(false) }
    fun resume() {
        val p = mp ?: return
        if (!focus()) return
        try { p.setVolume(1f, 1f); p.start(); playing = true; noisy(true); h.post(tick) } catch (_: Exception) {}
    }
    fun playPause() { if (playing) pause() else if (mp != null) resume() else track?.let { load(it.file) } }

    // --- order (shuffle keeps one fixed order: the next cover is known before it plays) ---
    private fun rebuildOrder() {
        val n = queue.size
        order = if (!shuffleOn || n < 2) IntArray(n) { it } else (listOf(index) + (0 until n).filter { it != index }.shuffled()).toIntArray()
    }
    private fun pos() = order.indexOf(index).coerceAtLeast(0)
    fun nextIndex(): Int = if (queue.size < 2) index else order.getOrElse((pos() + 1) % order.size) { 0 }
    /** the track [k] steps away in play order from [base] (the cover carousel), or null */
    fun peek(k: Int, base: Int = index): Int? {
        if (queue.isEmpty() || order.isEmpty()) return null
        if (k == 0) return base
        if (queue.size < 2) return null
        val bp = order.indexOf(base).coerceAtLeast(0)
        val p = (bp + k).let { if (repeat == 0 && !shuffleOn && (it < 0 || it > order.lastIndex)) return null else ((it % order.size) + order.size) % order.size }
        return order.getOrNull(p)
    }
    fun prevIndex(): Int = if (queue.size < 2) index else order.getOrElse((pos() - 1 + order.size) % order.size) { 0 }

    fun next(user: Boolean = true) {
        if (queue.isEmpty()) return
        if (!user && repeat == 0 && pos() >= order.lastIndex) { pause(); seekTo(0f); return }
        index = nextIndex(); load(queue[index].file)
    }

    /** previous track; in the first 3 s of a track — or always when [force] (a swipe) — otherwise back to its start */
    fun previous(force: Boolean = false) {
        if (queue.isEmpty()) return
        if (!force && positionMs > 3000) { seekTo(0f); return }
        index = prevIndex()
        load(queue[index].file)
    }

    fun move(from: Int, to: Int) {
        if (from !in queue.indices || to !in queue.indices || from == to) return
        val cur = track
        val l = queue.toMutableList(); l.add(to, l.removeAt(from))
        queue = l
        index = l.indexOf(cur).coerceAtLeast(0)
        rebuildOrder()
    }

    fun removeAt(i: Int) {
        if (i !in queue.indices || queue.size < 2) return
        if (i == index) { val l = queue.toMutableList(); l.removeAt(i); queue = l; index = i.coerceAtMost(l.lastIndex); rebuildOrder(); load(l[index].file); return }
        val cur = track
        queue = queue.toMutableList().also { it.removeAt(i) }
        index = queue.indexOf(cur).coerceAtLeast(0)
        rebuildOrder()
    }

    fun sortQueue(by: Int) {
        val cur = track ?: return
        queue = when (by) { 1 -> queue.sortedBy { it.title.lowercase() }; 2 -> queue.sortedBy { it.performer.lowercase() }; else -> queue.shuffled() }
        index = queue.indexOf(cur).coerceAtLeast(0)
        rebuildOrder()
    }

    fun seekTo(fraction: Float) {
        val p = mp ?: return
        try { val d = p.duration; if (d > 0) { p.seekTo((d * fraction.coerceIn(0f, 1f)).toInt()); positionMs = p.currentPosition; progress = fraction.coerceIn(0f, 1f) } } catch (_: Exception) {}
    }
    fun seekMs(ms: Int) { val d = mp?.duration ?: return; if (d > 0) seekTo(ms / d.toFloat()) }

    fun setSpeedTo(v: Float) { speed = v; applySpeed() }
    fun cycleSpeed() = setSpeedTo(when (speed) { 1f -> 1.25f; 1.25f -> 1.5f; 1.5f -> 2f; 2f -> 0.75f; else -> 1f })

    private fun applySpeed() {
        val p = mp ?: return
        try { val was = p.isPlaying; p.playbackParams = p.playbackParams.setSpeed(speed); if (!was) p.pause() } catch (_: Exception) {}
    }

    /** minutes > 0 — pause after that long; -1 — at the end of this track; 0 — off */
    fun sleep(minutes: Int) {
        h.removeCallbacks(sleeper)
        sleepAt = when { minutes > 0 -> System.currentTimeMillis() + minutes * 60_000L; minutes < 0 -> -1L; else -> 0L }
        if (minutes > 0) h.postDelayed(sleeper, minutes * 60_000L)
    }

    fun close() { stop(); queue = emptyList(); dismissed = true; sleep(0) }

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
            p.setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(if (queue.isEmpty()) android.media.AudioAttributes.CONTENT_TYPE_SPEECH else android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build())
            p.setDataSource(f.local.path)
            p.setOnCompletionListener {
                if (queue.isNotEmpty()) {
                    when {
                        sleepAt == -1L -> { sleepAt = 0; pause(); seekTo(0f) }
                        repeat == 2 -> { seekTo(0f); resume() }
                        else -> next(user = false)
                    }
                } else { playing = false; progress = 0f; noisy(false) }
            }
            mp = p
            fileId = f.id
            if (queue.isNotEmpty()) TrackArt.ensure(f)
            // prepared off the main thread: switching tracks (the cover carousel) never stalls a frame
            loading = true
            p.setOnPreparedListener {
                if (mp !== p) return@setOnPreparedListener
                loading = false
                durationMs = p.duration
                applySpeed()
                if (!focus()) return@setOnPreparedListener
                try { p.start(); playing = true; noisy(true); h.post(tick) } catch (_: Exception) {}
            }
            p.setOnErrorListener { _, _, _ -> if (mp === p) { loading = false; playing = false }; true }
            p.prepareAsync()
        } catch (e: Exception) {
            playing = false; loading = false
        }
    }

    fun stop() {
        // released off the main thread: releasing a player can take a while and would cost a frame
        val old = mp
        if (old != null) io.launch { try { old.release() } catch (_: Exception) {} }
        mp = null
        playing = false
        progress = 0f
        positionMs = 0
        noisy(false)
    }

    // --- audio focus: other apps' sound pauses us (and resumes after a short interruption), a ducking one lowers us ---
    private var resumeOnGain = false
    private val focusListener = android.media.AudioManager.OnAudioFocusChangeListener { c ->
        h.post {
            when (c) {
                android.media.AudioManager.AUDIOFOCUS_LOSS -> { resumeOnGain = false; if (playing) pause() }
                android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (playing) { resumeOnGain = true; pause() }
                android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> runCatching { mp?.setVolume(0.3f, 0.3f) }
                android.media.AudioManager.AUDIOFOCUS_GAIN -> { runCatching { mp?.setVolume(1f, 1f) }; if (resumeOnGain) { resumeOnGain = false; resume() } }
            }
        }
    }
    private val focusReq by lazy {
        android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener(focusListener, h).build()
    }
    private fun focus(): Boolean = runCatching {
        val am = Td.appContext.getSystemService(android.media.AudioManager::class.java)
        am.requestAudioFocus(focusReq) != android.media.AudioManager.AUDIOFOCUS_REQUEST_FAILED
    }.getOrDefault(true)

    // --- headphones unplugged / bluetooth gone: pause (Android's "becoming noisy") ---
    private var noisyOn = false
    private val noisyReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { if (playing) pause() }
    }
    private fun noisy(on: Boolean) {
        if (on == noisyOn) return
        runCatching {
            if (on) androidx.core.content.ContextCompat.registerReceiver(Td.appContext, noisyReceiver, android.content.IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY), androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
            else Td.appContext.unregisterReceiver(noisyReceiver)
            noisyOn = on
        }
    }

    /** the track's file in the phone's Music folder (MediaStore) */
    fun saveToMusic(t: Track, done: (Boolean) -> Unit) {
        val f = Images.current(t.file) ?: return done(false)
        fun copy(path: String) = io.launch {
            val ok = runCatching {
                val ctx = Td.appContext
                val name = t.audio?.fileName?.takeIf { it.isNotBlank() } ?: (listOf(t.performer, t.title).filter { it.isNotBlank() }.joinToString(" - ") + ".mp3")
                val v = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Audio.Media.DISPLAY_NAME, name)
                    put(android.provider.MediaStore.Audio.Media.MIME_TYPE, t.audio?.mimeType?.takeIf { it.isNotBlank() } ?: "audio/mpeg")
                    if (Build.VERSION.SDK_INT >= 29) put(android.provider.MediaStore.Audio.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_MUSIC + "/qwgram")
                }
                val uri = ctx.contentResolver.insert(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, v) ?: error("no uri")
                ctx.contentResolver.openOutputStream(uri)!!.use { out -> File(path).inputStream().use { it.copyTo(out) } }
                true
            }.getOrDefault(false)
            h.post { done(ok) }
        }
        if (f.local.isDownloadingCompleted) copy(f.local.path)
        else Td.send(TdApi.DownloadFile(f.id, 16, 0, 0, true)) { r -> if (r is TdApi.File && r.local.isDownloadingCompleted) copy(r.local.path) else h.post { done(false) } }
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
            // into the store first: the chat screen finds it at once (people → their profile, like Telegram)
            if (r is TdApi.Chat) { TdStore.onUpdate(TdApi.UpdateNewChat(r)); Nav.push(if (r.type is TdApi.ChatTypePrivate) Route.Profile(r.id) else Route.Chat(r.id)) }
            else Toast.makeText(context, "Не найдено: @$name", Toast.LENGTH_SHORT).show()
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

    /** downloads (if needed) and shares a file through the system share sheet */
    fun share(context: Context, file: TdApi.File, mime: String) {
        fun go(f: TdApi.File) = Handler(Looper.getMainLooper()).post {
            try {
                val uri = FileProvider.getUriForFile(context, context.packageName + ".files", File(f.local.path))
                val i = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(i, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                Toast.makeText(context, "Не удалось поделиться", Toast.LENGTH_SHORT).show()
            }
        }
        val f = Images.current(file) ?: return
        if (f.local.isDownloadingCompleted) go(f)
        else Td.send(TdApi.DownloadFile(f.id, 16, 0, 0, true)) { r -> if (r is TdApi.File && r.local.isDownloadingCompleted) go(r) }
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
