package app.qwgram.next

import android.app.Application
import app.qwgram.next.td.Td

class App : Application() {
    companion object { lateinit var instance: App; private set }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Prefs.init(this)
        // emoji style: system font, or the bundled Google Noto set replacing every emoji (needs an app restart to switch)
        if (Prefs.emojiStyle.value == "noto") runCatching {
            androidx.emoji2.text.EmojiCompat.init(androidx.emoji2.bundled.BundledEmojiCompatConfig(this).setReplaceAll(true))
        }
        Notifier.init(this)
        Td.init(this)
    }
}
