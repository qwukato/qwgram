package app.qwgram.next

import android.app.Application
import app.qwgram.next.td.Td

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        Notifier.init(this)
        Td.init(this)
    }
}
