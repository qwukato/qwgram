package app.qwgram.next.td

import app.qwgram.next.Prefs
import org.drinkless.tdlib.TdApi
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Human-readable strings for chats, messages and statuses (Russian UI). */
object Texts {
    private val ru = Locale("ru")
    private val time = SimpleDateFormat("HH:mm", ru)
    private val timeSec = SimpleDateFormat("HH:mm:ss", ru)
    private val dayMonth = SimpleDateFormat("d MMM", ru)
    private val full = SimpleDateFormat("d MMMM yyyy", ru)
    private val dayMonthLong = SimpleDateFormat("d MMMM", ru)
    private val weekday = SimpleDateFormat("EE", ru)

    fun time(unix: Int): String = (if (Prefs.seconds.value) timeSec else time).format(Date(unix * 1000L))

    /** chat list: time today, weekday this week, date otherwise */
    fun listDate(unix: Int): String {
        if (unix == 0) return ""
        val now = Calendar.getInstance()
        val c = Calendar.getInstance().apply { timeInMillis = unix * 1000L }
        val days = (startOfDay(now) - startOfDay(c)) / 86_400_000L
        return when {
            days <= 0L -> time.format(c.time)
            days < 7L -> weekday.format(c.time).replaceFirstChar { it.uppercase() }
            c.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> dayMonth.format(c.time).trimEnd('.')
            else -> SimpleDateFormat("dd.MM.yy", ru).format(c.time)
        }
    }

    fun dayHeader(unix: Int): String {
        val now = Calendar.getInstance()
        val c = Calendar.getInstance().apply { timeInMillis = unix * 1000L }
        val days = (startOfDay(now) - startOfDay(c)) / 86_400_000L
        return when {
            days == 0L -> "Сегодня"
            days == 1L -> "Вчера"
            c.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> dayMonthLong.format(c.time)
            else -> full.format(c.time)
        }
    }

    fun sameDay(a: Int, b: Int): Boolean {
        val x = Calendar.getInstance().apply { timeInMillis = a * 1000L }
        val y = Calendar.getInstance().apply { timeInMillis = b * 1000L }
        return x.get(Calendar.YEAR) == y.get(Calendar.YEAR) && x.get(Calendar.DAY_OF_YEAR) == y.get(Calendar.DAY_OF_YEAR)
    }

    private fun startOfDay(c: Calendar): Long = (c.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun duration(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)

    fun size(bytes: Long): String = when {
        bytes >= 1L shl 30 -> "%.1f ГБ".format(bytes / (1L shl 30).toDouble())
        bytes >= 1L shl 20 -> "%.1f МБ".format(bytes / (1L shl 20).toDouble())
        bytes >= 1L shl 10 -> "%.0f КБ".format(bytes / (1L shl 10).toDouble())
        else -> "$bytes Б"
    }

    fun plural(n: Int, one: String, few: String, many: String): String {
        val m10 = n % 10
        val m100 = n % 100
        val w = when {
            m10 == 1 && m100 != 11 -> one
            m10 in 2..4 && m100 !in 12..14 -> few
            else -> many
        }
        return "$n $w"
    }

    fun status(u: TdApi.User?): String {
        if (u == null) return ""
        if (u.type is TdApi.UserTypeBot) return "бот"
        if (u.id == 777000L) return "служебные уведомления"
        return when (val s = u.status) {
            is TdApi.UserStatusOnline -> "в сети"
            is TdApi.UserStatusOffline -> lastSeen(s.wasOnline)
            is TdApi.UserStatusRecently -> "был(а) недавно"
            is TdApi.UserStatusLastWeek -> "был(а) на этой неделе"
            is TdApi.UserStatusLastMonth -> "был(а) в этом месяце"
            else -> "был(а) давно"
        }
    }

    fun isOnline(u: TdApi.User?): Boolean = u?.status is TdApi.UserStatusOnline

    private fun lastSeen(unix: Int): String {
        val diff = System.currentTimeMillis() / 1000 - unix
        return when {
            diff < 60 -> "был(а) только что"
            diff < 3600 -> "был(а) ${plural((diff / 60).toInt(), "минуту", "минуты", "минут")} назад"
            sameDay(unix, (System.currentTimeMillis() / 1000).toInt()) -> "был(а) в ${time.format(Date(unix * 1000L))}"
            diff < 2 * 86400 -> "был(а) вчера в ${time.format(Date(unix * 1000L))}"
            else -> "был(а) ${dayMonthLong.format(Date(unix * 1000L))}"
        }
    }

    fun chatStatus(chat: TdApi.Chat): String {
        if (TdStore.isSaved(chat)) return ""
        val acts = TdStore.actions[chat.id]
        if (!acts.isNullOrEmpty()) {
            val (who, a) = acts.entries.first()
            val verb = action(a)
            return if (TdStore.isGroup(chat)) TdStore.userName(who).substringBefore(' ') + " " + verb else verb
        }
        return when (val t = chat.type) {
            is TdApi.ChatTypePrivate, is TdApi.ChatTypeSecret -> status(TdStore.users[TdStore.privateUserId(chat)])
            is TdApi.ChatTypeBasicGroup -> plural(TdStore.memberCount(chat), "участник", "участника", "участников")
            is TdApi.ChatTypeSupergroup -> {
                val n = TdStore.memberCount(chat)
                if (n == 0) (if (t.isChannel) "канал" else "группа")
                else if (t.isChannel) plural(n, "подписчик", "подписчика", "подписчиков") else plural(n, "участник", "участника", "участников")
            }
            else -> ""
        }
    }

    fun action(a: TdApi.ChatAction): String = when (a) {
        is TdApi.ChatActionTyping -> "печатает…"
        is TdApi.ChatActionRecordingVoiceNote -> "записывает голосовое…"
        is TdApi.ChatActionUploadingVoiceNote -> "отправляет голосовое…"
        is TdApi.ChatActionUploadingPhoto -> "отправляет фото…"
        is TdApi.ChatActionUploadingVideo -> "отправляет видео…"
        is TdApi.ChatActionRecordingVideo, is TdApi.ChatActionRecordingVideoNote -> "записывает видео…"
        is TdApi.ChatActionUploadingDocument -> "отправляет файл…"
        is TdApi.ChatActionChoosingSticker -> "выбирает стикер…"
        is TdApi.ChatActionChoosingLocation -> "выбирает место…"
        is TdApi.ChatActionChoosingContact -> "выбирает контакт…"
        else -> "печатает…"
    }

    fun caption(c: TdApi.MessageContent): TdApi.FormattedText? = when (c) {
        is TdApi.MessageText -> c.text
        is TdApi.MessagePhoto -> c.caption
        is TdApi.MessageVideo -> c.caption
        is TdApi.MessageAnimation -> c.caption
        is TdApi.MessageDocument -> c.caption
        is TdApi.MessageAudio -> c.caption
        is TdApi.MessageVoiceNote -> c.caption
        else -> null
    }

    /** one-line description of a message for lists, replies and notifications */
    fun preview(m: TdApi.Message, withSender: Boolean = false): String {
        val c = m.content
        val cap = caption(c)?.text.orEmpty().replace('\n', ' ')
        val body = when (c) {
            is TdApi.MessageText -> cap
            is TdApi.MessagePhoto -> if (cap.isNotBlank()) "🖼 $cap" else "Фотография"
            is TdApi.MessageVideo -> if (cap.isNotBlank()) "📹 $cap" else "Видео"
            is TdApi.MessageAnimation -> if (cap.isNotBlank()) "GIF, $cap" else "GIF"
            is TdApi.MessageSticker -> (c.sticker.emoji + " Стикер").trim()
            is TdApi.MessageAnimatedEmoji -> c.emoji
            is TdApi.MessageVoiceNote -> "Голосовое сообщение"
            is TdApi.MessageVideoNote -> "Видеосообщение"
            is TdApi.MessageAudio -> listOf(c.audio.performer, c.audio.title).filter { it.isNotBlank() }.joinToString(" — ").ifBlank { "Аудио" }
            is TdApi.MessageDocument -> c.document.fileName.ifBlank { "Файл" }
            is TdApi.MessageLocation -> "Геопозиция"
            is TdApi.MessageContact -> "Контакт: " + listOf(c.contact.firstName, c.contact.lastName).joinToString(" ").trim()
            is TdApi.MessagePoll -> "📊 " + c.poll.question.text
            is TdApi.MessageCall -> if (c.isVideo) "Видеозвонок" else "Звонок"
            is TdApi.MessageGame -> "Игра"
            else -> service(m) ?: "Сообщение"
        }
        if (!withSender || m.isOutgoing || service(m) != null) return if (m.isOutgoing && withSender && service(m) == null) "Вы: $body" else body
        val name = TdStore.senderName(m.senderId).substringBefore(' ')
        return if (name.isBlank()) body else "$name: $body"
    }

    /** text of a service message, or null for regular content */
    fun service(m: TdApi.Message): String? {
        val who = TdStore.senderName(m.senderId)
        return when (val c = m.content) {
            is TdApi.MessageChatAddMembers -> if (c.memberUserIds.size == 1 && c.memberUserIds[0] == TdStore.senderId(m.senderId)) "$who вступил(а) в группу"
            else "$who добавил(а) " + c.memberUserIds.joinToString(", ") { TdStore.userName(it) }
            is TdApi.MessageChatJoinByLink -> "$who вступил(а) по ссылке"
            is TdApi.MessageChatDeleteMember -> if (c.userId == TdStore.senderId(m.senderId)) "$who покинул(а) группу" else "$who удалил(а) ${TdStore.userName(c.userId)}"
            is TdApi.MessageChatChangeTitle -> "$who изменил(а) название: «${c.title}»"
            is TdApi.MessageChatChangePhoto -> "$who изменил(а) фото"
            is TdApi.MessageChatDeletePhoto -> "$who удалил(а) фото"
            is TdApi.MessageBasicGroupChatCreate -> "$who создал(а) группу «${c.title}»"
            is TdApi.MessageSupergroupChatCreate -> "Создан(а) «${c.title}»"
            is TdApi.MessagePinMessage -> "$who закрепил(а) сообщение"
            is TdApi.MessageContactRegistered -> "$who теперь в Telegram"
            is TdApi.MessageScreenshotTaken -> "$who сделал(а) скриншот"
            is TdApi.MessageChatSetMessageAutoDeleteTime -> "Автоудаление: " + if (c.messageAutoDeleteTime == 0) "выключено" else duration(c.messageAutoDeleteTime)
            is TdApi.MessageVideoChatStarted -> "Начался видеочат"
            is TdApi.MessageVideoChatEnded -> "Видеочат завершён"
            is TdApi.MessageChatUpgradeTo, is TdApi.MessageChatUpgradeFrom -> "Группа стала супергруппой"
            is TdApi.MessageUnsupported -> "Это сообщение пока не поддерживается в qwgram"
            else -> null
        }
    }

    /** strips utm_* and other tracking parameters from a link (client feature) */
    fun cleanUrl(url: String): String {
        if (!Prefs.stripTracking.value) return url
        val q = url.indexOf('?')
        if (q < 0) return url
        val hashAt = url.indexOf('#', q)
        val base = url.substring(0, q)
        val query = url.substring(q + 1, if (hashAt > 0) hashAt else url.length)
        val tail = if (hashAt > 0) url.substring(hashAt) else ""
        val tracking = setOf("fbclid", "gclid", "yclid", "igshid", "si", "mc_cid", "mc_eid", "ref_src", "_hsenc", "_hsmi", "spm", "feature")
        val kept = query.split('&').filter { p ->
            val k = p.substringBefore('=').lowercase()
            k.isNotEmpty() && !k.startsWith("utm_") && k !in tracking
        }
        return base + (if (kept.isEmpty()) "" else "?" + kept.joinToString("&")) + tail
    }
}
