package app.qwgram.next

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/** Keeps the process (and TDLib's connection) alive so messages arrive without Google push. */
class KeepAliveService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, Notifier.CH_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("qwgram на связи")
            .setContentText("Получаем сообщения в фоне. Скрыть: удерживайте уведомление → Настройки.")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        val type = when {
            Build.VERSION.SDK_INT >= 34 -> ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
            Build.VERSION.SDK_INT >= 29 -> ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            else -> 0
        }
        try {
            ServiceCompat.startForeground(this, 1, n, type)
        } catch (e: Exception) {
            stopSelf()
        }
        return START_STICKY
    }

    companion object {
        fun sync(context: Context) {
            val i = Intent(context, KeepAliveService::class.java)
            if (Prefs.keepAlive.value) {
                try {
                    ContextCompat.startForegroundService(context, i)
                } catch (_: Exception) {
                }
            } else context.stopService(i)
        }
    }
}
