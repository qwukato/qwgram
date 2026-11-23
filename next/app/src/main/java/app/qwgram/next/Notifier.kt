package app.qwgram.next

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.Texts
import org.drinkless.tdlib.TdApi

/** Local notifications for new messages while the app is in the background (TDLib keeps the connection). */
object Notifier {
    const val CH_MESSAGES = "messages"
    const val CH_SERVICE = "service"
    private lateinit var ctx: Context
    var openChatId: Long = 0L

    fun init(context: Context) {
        ctx = context.applicationContext
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CH_MESSAGES, "Сообщения", NotificationManager.IMPORTANCE_HIGH))
            nm.createNotificationChannel(NotificationChannel(CH_SERVICE, "Работа в фоне", NotificationManager.IMPORTANCE_MIN).apply { setShowBadge(false) })
        }
    }

    private fun foreground(): Boolean = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    fun onNewMessage(m: TdApi.Message) {
        if (m.isOutgoing) return
        val chat = TdStore.chats[m.chatId] ?: return
        if (TdStore.isMuted(chat)) return
        if (foreground() && openChatId == m.chatId) return
        if (foreground()) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val text = if (Prefs.notifyPreview.value) Texts.preview(m, withSender = TdStore.isGroup(chat)) else "Новое сообщение"
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("chat_id", m.chatId)
        }
        val pi = PendingIntent.getActivity(ctx, m.chatId.toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, CH_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(chat.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(Prefs.seed.value)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup("chat${m.chatId}")
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(m.chatId.hashCode(), n)
        } catch (_: SecurityException) {
        }
    }

    fun clear(chatId: Long) {
        try {
            NotificationManagerCompat.from(ctx).cancel(chatId.hashCode())
        } catch (_: Exception) {
        }
    }
}
