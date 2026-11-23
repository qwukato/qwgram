package app.qwgram.next.td

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import app.qwgram.next.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TdException(val error: TdApi.Error) : Exception("${error.code}: ${error.message}")

/**
 * The single TDLib client. Every update is delivered to [TdStore] on the main thread, so Compose
 * state is only ever mutated from one thread.
 */
object Td {
    private const val TAG = "qwgram-td"
    private var client: Client? = null
    private val main = Handler(Looper.getMainLooper())
    lateinit var appContext: Context
        private set

    fun init(context: Context) {
        if (client != null) return
        appContext = context.applicationContext
        try {
            Client.execute(TdApi.SetLogVerbosityLevel(1))
        } catch (e: Throwable) {
            Log.w(TAG, "log level", e)
        }
        client = Client.create({ obj -> main.post { TdStore.onUpdate(obj) } }, { e -> Log.e(TAG, "update", e) }, { e -> Log.e(TAG, "default", e) })
    }

    fun parameters(): TdApi.SetTdlibParameters {
        val dir = File(appContext.filesDir, "td").absolutePath
        return TdApi.SetTdlibParameters().apply {
            useTestDc = false
            databaseDirectory = dir
            filesDirectory = dir
            databaseEncryptionKey = ByteArray(0)
            useFileDatabase = true
            useChatInfoDatabase = true
            useMessageDatabase = true
            useSecretChats = false
            apiId = BuildConfig.TG_APP_ID
            apiHash = BuildConfig.TG_APP_HASH
            systemLanguageCode = Locale.getDefault().language.ifEmpty { "ru" }
            deviceModel = (Build.MANUFACTURER.replaceFirstChar { it.uppercase() } + " " + Build.MODEL).trim()
            systemVersion = "Android " + Build.VERSION.RELEASE
            applicationVersion = "qwgram " + BuildConfig.VERSION_NAME
        }
    }

    /** fire and forget; errors are logged */
    fun send(function: TdApi.Function<*>, onResult: ((TdApi.Object) -> Unit)? = null) {
        val c = client ?: return
        c.send(function, { result ->
            if (result is TdApi.Error) Log.w(TAG, "${function.javaClass.simpleName}: ${result.code} ${result.message}")
            if (onResult != null) main.post { onResult(result) }
        })
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <R : TdApi.Object> call(function: TdApi.Function<R>): R = suspendCancellableCoroutine { cont ->
        val c = client
        if (c == null) {
            cont.resumeWithException(IllegalStateException("TDLib is not started"))
            return@suspendCancellableCoroutine
        }
        c.send(function, { result ->
            if (result is TdApi.Error) {
                if (cont.isActive) cont.resumeWithException(TdException(result))
            } else {
                if (cont.isActive) cont.resume(result as R)
            }
        })
    }

    /** like [call] but returns null instead of throwing */
    suspend fun <R : TdApi.Object> callOrNull(function: TdApi.Function<R>): R? = try {
        call(function)
    } catch (e: Exception) {
        Log.w(TAG, "${function.javaClass.simpleName}: ${e.message}")
        null
    }
}
