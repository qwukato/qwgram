package app.qwgram.next

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
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
                CrashReport()
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
        // t.me / tg:// links from other apps open here (handled once the client is ready)
        if (i?.action == Intent.ACTION_VIEW) i.data?.toString()?.let { app.qwgram.next.ui.Links.pending = it; return }
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

/** the stack trace of the last crash, shown once on the next launch */
@androidx.compose.runtime.Composable
private fun CrashReport() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val f = androidx.compose.runtime.remember { java.io.File(ctx.filesDir, "last_crash.txt") }
    var text by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(if (f.exists()) runCatching { f.readText() }.getOrNull() else null) }
    val t = text ?: return
    fun done() { f.delete(); text = null }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { done() },
        title = { androidx.compose.material3.Text("Приложение упало") },
        text = {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.Text("Скопируйте отчёт и отправьте разработчику:", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(8.dp))
                androidx.compose.material3.Text(
                    t, style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    modifier = androidx.compose.ui.Modifier.height(260.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                val cm = ctx.getSystemService(android.content.ClipboardManager::class.java)
                cm.setPrimaryClip(android.content.ClipData.newPlainText("qwgram crash", t))
                done()
            }) { androidx.compose.material3.Text("Скопировать") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { done() }) { androidx.compose.material3.Text("Закрыть") } },
    )
}
