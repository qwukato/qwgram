package app.qwgram.next.ui

import app.qwgram.next.App
import app.qwgram.next.Prefs
import app.qwgram.next.td.Texts
import org.drinkless.tdlib.TdApi
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * "Save deleted messages", kept across restarts: messages that arrive or are loaded are remembered for a while
 * (the last few hundred per chat, in memory); when someone else deletes one, its text (and what kind of message it
 * was) is written to a small file. Opening the chat puts these back in their place, marked "deleted".
 * Our own deletions are never kept.
 */
object DeletedStore {
    private val recent = HashMap<Long, LinkedHashMap<Long, TdApi.Message>>()
    private val saved = HashMap<Long, MutableList<JSONObject>>()
    private var loaded = false
    private val file get() = File(App.instance.filesDir, "deleted_messages.json")

    @Synchronized
    fun remember(m: TdApi.Message) {
        if (!Prefs.keepDeleted.value || m.isOutgoing) return
        val map = recent.getOrPut(m.chatId) { object : LinkedHashMap<Long, TdApi.Message>() { override fun removeEldestEntry(e: MutableMap.MutableEntry<Long, TdApi.Message>?) = size > 400 } }
        map[m.id] = m
    }

    /** every update passes through here (TdStore): new messages are remembered, deletions by others are saved */
    fun onUpdate(u: TdApi.Object) {
        if (!Prefs.keepDeleted.value) return
        when (u) {
            is TdApi.UpdateNewMessage -> remember(u.message)
            is TdApi.UpdateDeleteMessages -> if (u.isPermanent && !u.fromCache) save(u.chatId, u.messageIds)
            else -> Unit
        }
    }

    @Synchronized
    private fun save(chatId: Long, ids: LongArray) {
        load()
        val map = recent[chatId] ?: return
        var changed = false
        for (id in ids) {
            if ((chatId xor id) in DeletedMessages.ours) continue
            val m = map.remove(id) ?: continue
            val list = saved.getOrPut(chatId) { ArrayList() }
            if (list.any { it.optLong("id") == id }) continue
            val (text, kind) = describe(m)
            list += JSONObject().apply {
                put("id", id); put("date", m.date)
                when (val s = m.senderId) { is TdApi.MessageSenderUser -> put("user", s.userId); is TdApi.MessageSenderChat -> put("chat", s.chatId); else -> Unit }
                put("text", text); put("kind", kind)
            }
            while (list.size > 300) list.removeAt(0)
            changed = true
        }
        if (changed) persist()
    }

    private fun describe(m: TdApi.Message): Pair<String, String> = when (val c = m.content) {
        is TdApi.MessageText -> c.text.text to ""
        is TdApi.MessagePhoto -> c.caption.text to "Фото"
        is TdApi.MessageVideo -> c.caption.text to "Видео"
        is TdApi.MessageDocument -> c.caption.text to "Файл"
        is TdApi.MessageAudio -> c.caption.text to "Музыка"
        is TdApi.MessageVoiceNote -> c.caption.text to "Голосовое"
        is TdApi.MessageAnimation -> c.caption.text to "GIF"
        else -> "" to Texts.preview(m)
    }

    /** the saved deleted messages of a chat, rebuilt as text messages (marked deleted) */
    @Synchronized
    fun forChat(chatId: Long): List<TdApi.Message> {
        if (!Prefs.keepDeleted.value) return emptyList()
        load()
        return saved[chatId].orEmpty().map { o ->
            val kind = o.optString("kind")
            val text = o.optString("text")
            TdApi.Message().apply {
                id = o.getLong("id"); this.chatId = chatId; date = o.optInt("date")
                senderId = if (o.has("user")) TdApi.MessageSenderUser(o.getLong("user")) else TdApi.MessageSenderChat(o.optLong("chat", chatId))
                isOutgoing = false; authorSignature = ""
                val body = when { kind.isEmpty() -> text; text.isBlank() -> "[$kind]"; else -> "[$kind] $text" }
                content = TdApi.MessageText(TdApi.FormattedText(body, arrayOf()), null, null)
            }.also { DeletedMessages.add(chatId, it.id) }
        }
    }

    private fun load() {
        if (loaded) return
        loaded = true
        runCatching {
            if (!file.exists()) return
            val o = JSONObject(file.readText())
            o.keys().forEach { k -> val a = o.getJSONArray(k); saved[k.toLong()] = (0 until a.length()).map { a.getJSONObject(it) }.toMutableList() }
        }
    }

    private fun persist() {
        val o = JSONObject()
        saved.forEach { (k, v) -> o.put(k.toString(), JSONArray(v)) }
        runCatching { file.writeText(o.toString()) }
    }

    @Synchronized
    fun clear() { saved.clear(); recent.clear(); runCatching { file.delete() } }
}
