package app.qwgram.next

import android.app.Application
import app.qwgram.next.td.Td

class App : Application() {
    companion object { lateinit var instance: App; private set }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Prefs.init(this)
        Notifier.init(this)
        Td.init(this)
    }
}
