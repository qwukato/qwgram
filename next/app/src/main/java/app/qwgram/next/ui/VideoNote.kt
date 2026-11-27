package app.qwgram.next.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.Rational
import android.view.Surface
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import app.qwgram.next.Prefs
import app.qwgram.next.td.Td
import app.qwgram.next.td.Texts
import org.drinkless.tdlib.TdApi
import java.io.File
import kotlin.math.roundToInt

/**
 * Round video messages, like Telegram: tap the mic and it turns into a camera; hold it to record. A round camera
 * view rises in the middle of the screen with a ring that fills over the 60 s; let go to send, slide left to
 * cancel, slide up to lock (then: delete · switch camera · send). The video is cropped square in the camera itself.
 */
object VideoNote {
    /** the composer's button records video (true) or voice (false); kept for the session, like Telegram */
    var videoMode by mutableStateOf(false)

    var active by mutableStateOf(false)
        private set
    var locked by mutableStateOf(false)
    var front by mutableStateOf(true)
    var seconds by mutableIntStateOf(0)
        internal set
    /** the finger's pull while holding (for the "cancel" hint) */
    var dragX by mutableFloatStateOf(0f)
    var dragY by mutableFloatStateOf(0f)
    /** bumped on every new recording: the host binds the camera for it */
    var session by mutableIntStateOf(0)
        private set

    internal var chatId = 0L
    internal var replyTo: TdApi.Message? = null
    internal var capture: VideoCapture<Recorder>? = null
    internal var recording: Recording? = null
    internal var provider: ProcessCameraProvider? = null
    internal var file: File? = null
    private var send = false
    internal var startedAt = 0L

    const val LIMIT_MS = 60_000L

    fun start(chatId: Long, replyTo: TdApi.Message?) {
        if (active) return
        this.chatId = chatId; this.replyTo = replyTo
        locked = false; seconds = 0; dragX = 0f; dragY = 0f; send = false
        capture = null; recording = null
        active = true
        session++
        Td.send(TdApi.SendChatAction(chatId, null, "", TdApi.ChatActionRecordingVideoNote()))
    }

    /** stop: [sendIt] sends it, otherwise it is thrown away */
    fun finish(sendIt: Boolean) {
        if (!active) return
        send = sendIt
        val r = recording
        if (r != null) r.stop() else end()
    }

    internal fun onFinal(e: VideoRecordEvent.Finalize) {
        val f = file
        val secs = (e.recordingStats.recordedDurationNanos / 1_000_000_000L).toInt()
        val okError = e.error == VideoRecordEvent.Finalize.ERROR_NONE || e.error == VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED
        if (send && okError && f != null && f.exists() && f.length() > 0 && secs >= 1) {
            sendContent(chatId, TdApi.InputMessageVideoNote(TdApi.InputVideoNote(TdApi.InputFileLocal(f.absolutePath), null, secs, 384), null), replyTo)
        } else f?.delete()
        // the limit stops it by itself: that one is sent
        end()
    }

    private fun end() {
        runCatching { provider?.unbindAll() }
        recording = null; capture = null
        active = false; locked = false; dragX = 0f; dragY = 0f
        Td.send(TdApi.SendChatAction(chatId, null, "", TdApi.ChatActionCancel()))
    }

    @SuppressLint("MissingPermission")
    @androidx.annotation.OptIn(androidx.camera.video.ExperimentalPersistentRecording::class)
    internal fun record(ctx: Context, vc: VideoCapture<Recorder>) {
        val f = File(ctx.cacheDir, "round_${System.currentTimeMillis()}.mp4")
        file = f
        val audio = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val pending = vc.output.prepareRecording(ctx, FileOutputOptions.Builder(f).setDurationLimitMillis(LIMIT_MS).build())
        startedAt = System.currentTimeMillis()
        // persistent: switching the camera mid-recording keeps the same recording going
        recording = (if (audio) pending.withAudioEnabled() else pending).asPersistentRecording()
            .start(ContextCompat.getMainExecutor(ctx)) { e ->
                if (e is VideoRecordEvent.Status) seconds = (e.recordingStats.recordedDurationNanos / 1_000_000_000L).toInt()
                if (e is VideoRecordEvent.Finalize) onFinal(e)
            }
    }
}

@Composable
fun VideoNoteHost() {
    val vis = remember { MutableTransitionState(false) }
    vis.targetState = VideoNote.active
    if (!vis.currentState && !vis.targetState && vis.isIdle) return
    val ctx = LocalContext.current
    @Suppress("DEPRECATION") val owner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val cs = MaterialTheme.colorScheme
    val k = Prefs.motionSpeed.value * Prefs.motionSpeed.value
    val previewView = remember {
        PreviewView(ctx).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            // a TextureView: it can be clipped into the circle
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    // bind the camera for each recording and each camera switch (the recording itself keeps going)
    LaunchedEffect(VideoNote.session, VideoNote.front, VideoNote.active) {
        if (!VideoNote.active) return@LaunchedEffect
        val provider = runCatching { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { ProcessCameraProvider.getInstance(ctx).get() } }.getOrNull() ?: run { VideoNote.finish(false); return@LaunchedEffect }
        VideoNote.provider = provider
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val vc = VideoNote.capture ?: VideoCapture.Builder(
            Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.SD, FallbackStrategy.higherQualityOrLowerThan(Quality.SD))).build(),
        ).setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY).build().also { VideoNote.capture = it }
        val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
        val group = UseCaseGroup.Builder()
            .setViewPort(ViewPort.Builder(Rational(1, 1), rotation).build())   // square video, cropped by the camera
            .addUseCase(preview).addUseCase(vc).build()
        val selector = if (VideoNote.front && runCatching { provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false)) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        val bound = runCatching { provider.unbindAll(); provider.bindToLifecycle(owner, selector, group) }.isSuccess
        if (!bound) { VideoNote.finish(false); return@LaunchedEffect }
        if (VideoNote.recording == null) VideoNote.record(ctx, vc)
    }
    DisposableEffect(Unit) { onDispose { if (VideoNote.active) VideoNote.finish(false) } }
    BackHandler(VideoNote.active) { VideoNote.finish(false) }

    // the ring fills smoothly between the recorder's status ticks
    var now by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(VideoNote.active, VideoNote.session) {
        while (VideoNote.active) {
            androidx.compose.runtime.withFrameMillis { }
            now = if (VideoNote.recording != null) ((System.currentTimeMillis() - VideoNote.startedAt) / VideoNote.LIMIT_MS.toFloat()).coerceIn(0f, 1f) else 0f
        }
    }
    val scrim by animateFloatAsState(if (VideoNote.active) 1f else 0f, tween(260), label = "vs")
    Box(Modifier.fillMaxSize()) {
        // dim (also keeps taps off the chat while a locked recording runs)
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = scrim }
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.85f))))
                .then(if (VideoNote.locked) Modifier.pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume() } } else Modifier),
        )
        // top: what is happening and how long
        Box(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 16.dp).graphicsLayer { alpha = scrim }) {
            androidx.compose.material3.Surface(shape = CircleShape, color = cs.surfaceContainerHigh) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    val blink by animateFloatAsState(if (VideoNote.seconds % 2 == 0) 1f else 0.25f, tween(400), label = "bl")
                    Box(Modifier.size(10.dp).graphicsLayer { alpha = blink }.clip(CircleShape).background(cs.error))
                    Spacer(Modifier.width(10.dp))
                    Text("Видеосообщение", style = MaterialTheme.typography.labelLarge, color = cs.onSurface)
                    Spacer(Modifier.width(10.dp))
                    Text(Texts.duration(VideoNote.seconds) + " / 1:00", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = cs.primary)
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val d = minOf(maxWidth * 0.8f, maxHeight * 0.5f)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = (-30).dp)) {
                AnimatedVisibility(
                    vis,
                    // rises as a cookie that opens into a circle; leaves shrinking down towards the input
                    enter = scaleIn(spring(0.62f, 320f * k), initialScale = 0.4f) + fadeIn(tween(160)),
                    exit = scaleOut(tween(220), targetScale = 0.3f) + slideOutVertically(tween(260)) { it } + fadeOut(tween(200)),
                ) {
                    val open by animateFloatAsState(if (VideoNote.active) 1f else 0f, spring(0.7f, 260f * k), label = "vo")
                    val morph = remember { androidx.graphics.shapes.Morph(MaterialShapes.Cookie12Sided, MaterialShapes.Circle) }
                    Box(Modifier.size(d + 28.dp), contentAlignment = Alignment.Center) {
                        // the 60 s ring around the camera
                        val ring = cs.primary
                        val track = Color.White.copy(alpha = 0.16f)
                        Canvas(Modifier.fillMaxSize()) {
                            val st = 6.dp.toPx()
                            val inset = st / 2
                            val sz = Size(size.width - st, size.height - st)
                            drawArc(track, 0f, 360f, false, Offset(inset, inset), sz, style = Stroke(st))
                            drawArc(ring, -90f, 360f * now, false, Offset(inset, inset), sz, style = Stroke(st, cap = StrokeCap.Round))
                        }
                        Box(Modifier.size(d).clip(MorphShape(morph, open.coerceIn(0f, 1f))).background(Color.Black)) {
                            AndroidView({ previewView }, Modifier.fillMaxSize())
                        }
                        // switch camera: a small button sitting on the circle's edge
                        Box(Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 6.dp)) {
                            MorphIconButton(Icons.Outlined.Cameraswitch, "Сменить камеру", { VideoNote.front = !VideoNote.front }, size = 52.dp, color = cs.secondaryContainer, tint = cs.onSecondaryContainer)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                AnimatedContent(VideoNote.locked, transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(120))) }, label = "hint", modifier = Modifier.graphicsLayer { alpha = scrim }) { l ->
                    Text(
                        if (l) "Запись закреплена — можно убрать палец" else "Отпустите — отправить",
                        style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }
        // bottom
        Box(Modifier.fillMaxSize().navigationBarsPadding().graphicsLayer { alpha = scrim }) {
            AnimatedContent(VideoNote.locked, Modifier.align(Alignment.BottomCenter), transitionSpec = { (fadeIn(tween(220)) + slideInVertically(spring(0.7f, 400f)) { it / 2 }).togetherWith(fadeOut(tween(120))) }, label = "vl") { locked ->
                if (locked) Row(Modifier.padding(bottom = 26.dp), horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
                    MorphIconButton(Icons.Outlined.DeleteOutline, "Удалить", { VideoNote.finish(false) }, size = 60.dp, color = cs.errorContainer, tint = cs.onErrorContainer)
                    // send: a cookie that turns while pressed
                    MorphBox(onClick = { VideoNote.finish(true) }, rest = 36.dp, pressed = 22.dp, modifier = Modifier.size(76.dp), color = cs.primary, contentColor = cs.onPrimary) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Отправить", Modifier.size(30.dp))
                    }
                    Spacer(Modifier.size(60.dp))
                }
                else Box(Modifier.fillMaxWidth().height(200.dp)) {
                    // slide left to cancel: the chip follows the finger
                    Box(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 20.dp).offset { IntOffset((VideoNote.dragX / 2).roundToInt(), 0) }) {
                        val nudge = loopPhase(true, 1200, (2 * Math.PI).toFloat())
                        androidx.compose.material3.Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.14f), contentColor = Color.White) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, Modifier.size(20.dp).graphicsLayer { translationX = kotlin.math.sin(nudge.value) * 4.dp.toPx() })
                                Spacer(Modifier.width(4.dp))
                                Text("Влево — отмена", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    // slide up to lock: a rail right above the record button; it rises with the finger (tap works too)
                    val bob = loopPhase(true, 1400, (2 * Math.PI).toFloat())
                    val pull = (-VideoNote.dragY / 110f).coerceIn(0f, 1f)
                    Column(
                        Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 74.dp)
                            .graphicsLayer { translationY = -pull * 40.dp.toPx() }
                            .clip(CircleShape).background(androidx.compose.ui.graphics.lerp(cs.surfaceContainerHigh, cs.primaryContainer, pull))
                            .clickable { VideoNote.locked = true }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(if (pull > 0.95f) Icons.Outlined.Lock else Icons.Outlined.LockOpen, "Закрепить", tint = androidx.compose.ui.graphics.lerp(cs.onSurface, cs.onPrimaryContainer, pull), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(6.dp))
                        Icon(Icons.Outlined.KeyboardArrowUp, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp).graphicsLayer { translationY = kotlin.math.sin(bob.value) * 4.dp.toPx() })
                    }
                }
            }
        }
    }
}
