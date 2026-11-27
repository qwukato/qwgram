package app.qwgram.next

import android.app.Application
import app.qwgram.next.td.Td

/** emoji style the process started with (Noto is set up once per process, so changing it needs a restart) */
var startEmoji: String = "system"

class App : Application() {
    companion object { lateinit var instance: App; private set }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // a crash leaves its stack trace behind: the next launch shows it (with a copy button) so it can be reported
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                java.io.File(filesDir, "last_crash.txt").writeText(
                    "qwgram Next ${runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull()} · Android ${android.os.Build.VERSION.RELEASE} · ${android.os.Build.MODEL}\n" +
                        "thread: ${t.name}\n\n" + android.util.Log.getStackTraceString(e),
                )
            }
            prev?.uncaughtException(t, e)
        }
        Prefs.init(this)
        startEmoji = Prefs.emojiStyle.value
        // emoji style: system font, or the bundled Google Noto set replacing every emoji (needs an app restart to switch)
        if (Prefs.emojiStyle.value == "noto") runCatching {
            androidx.emoji2.text.EmojiCompat.init(androidx.emoji2.bundled.BundledEmojiCompatConfig(this).setReplaceAll(true))
        }
        Power.init(this)
        Notifier.init(this)
        Td.init(this)
    }
}
