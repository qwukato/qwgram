package app.qwgram.next

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.qwgram.next.td.TdStore
import app.qwgram.next.ui.AppRoot
import app.qwgram.next.ui.Nav
import app.qwgram.next.ui.QwTheme
import app.qwgram.next.ui.Route
import app.qwgram.next.ui.isAppDark
import org.drinkless.tdlib.TdApi

class MainActivity : ComponentActivity() {
    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // keep the system splash until TDLib reports whether we are logged in
        splash.setKeepOnScreenCondition { TdStore.auth == null || TdStore.auth is TdApi.AuthorizationStateWaitTdlibParameters }
        handleIntent(intent)
        setContent {
            QwTheme {
                val dark = isAppDark()
                LaunchedEffect(dark) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
                        navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
                    )
                }
                AppRoot()
            }
        }
        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        KeepAliveService.sync(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        val id = i?.getLongExtra("chat_id", 0L) ?: 0L
        if (id != 0L) {
            Nav.home()
            Nav.push(Route.Chat(id))
        }
    }

    // "online" is the client's job in TDLib: on while visible, off in background; ghost mode keeps it off
    override fun onResume() {
        super.onResume()
        app.qwgram.next.ui.applyOnline(true)
    }

    override fun onPause() {
        super.onPause()
        app.qwgram.next.ui.applyOnline(false)
    }

}
