package app.qwgram.next

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Energy saving as the app sees it: switched on by hand, or by itself on a low battery (when that's allowed and the
 * phone isn't charging). Everything that saves energy asks [on], not the switch alone.
 */
object Power {
    var battery by mutableIntStateOf(100)
        private set
    var charging by mutableStateOf(false)
        private set

    val auto: Boolean get() = Prefs.psAuto.value && !charging && battery <= Prefs.psAutoLevel.value
    val on: Boolean get() = Prefs.powerSave.value || auto

    private var app: Context? = null
    private var wasOn = false

    fun init(ctx: Context) {
        app = ctx.applicationContext
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) = read(i)
        }
        ctx.applicationContext.registerReceiver(r, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let { read(it) }
    }

    private fun read(i: Intent) {
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        if (level >= 0) battery = level * 100 / scale
        val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
        val now = on
        if (now != wasOn) changed()
    }

    /** background work follows the saving state when the user asked for that (called on any related change) */
    fun changed() {
        wasOn = on
        app?.let { KeepAliveService.sync(it) }
    }
}
