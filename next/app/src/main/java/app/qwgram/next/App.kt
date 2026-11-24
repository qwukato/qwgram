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
        Prefs.init(this)
        startEmoji = Prefs.emojiStyle.value
        // emoji style: system font, or the bundled Google Noto set replacing every emoji (needs an app restart to switch)
        if (Prefs.emojiStyle.value == "noto") runCatching {
            androidx.emoji2.text.EmojiCompat.init(androidx.emoji2.bundled.BundledEmojiCompatConfig(this).setReplaceAll(true))
        }
        Notifier.init(this)
        Td.init(this)
    }
}
