package app.qwgram.next.ui

import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.layout.onGloballyPositioned
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player as MPlayer
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.qwgram.next.td.Images
import app.qwgram.next.td.Texts
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.rememberFileBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

/** a video to show full-screen */
class VideoItem(val file: TdApi.File, val thumb: TdApi.File?, val mini: TdApi.Minithumbnail?, val width: Int, val height: Int, val duration: Int)

object VideoViewer {
    var current by mutableStateOf<VideoItem?>(null)
    /** bounds of the bubble the video was opened from: the viewer grows out of it and returns into it */
    var origin: androidx.compose.ui.geometry.Rect? = null
    fun open(v: VideoItem) { Player.pause(); current = v }
}

/** a looping, muted ExoPlayer surface for GIFs / inline previews (released with the composable) */
@Composable
fun LoopingVideo(path: String, modifier: Modifier = Modifier, crop: Boolean = true) {
    val ctx = LocalContext.current
    // energy saving: GIFs wait for a tap instead of playing on their own
    val paused = Prefs.powerSave.value && Prefs.psGifs.value
    var tapped by remember(path) { mutableStateOf(false) }
    val player = remember(path) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(path))))
            repeatMode = MPlayer.REPEAT_MODE_ALL
            volume = 0f
            playWhenReady = !paused
            prepare()
        }
    }
    LaunchedEffect(paused, tapped) { player.playWhenReady = !paused || tapped }
    DisposableEffect(player) { onDispose { player.release() } }
    if (paused && !tapped) Box(modifier.pointerInput(Unit) { detectTapGestures(onTap = { tapped = true }) }) {
        AndroidView(factory = { c -> PlayerView(c).apply { useController = false; setShutterBackgroundColor(android.graphics.Color.TRANSPARENT); resizeMode = if (crop) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT; this.player = player } }, modifier = Modifier.matchParentSize())
        Icon(Icons.Filled.PlayArrow, "GIF", tint = Color.White, modifier = Modifier.align(Alignment.Center).size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).padding(6.dp))
        return
    }
    AndroidView(
        factory = { c ->
            PlayerView(c).apply {
                useController = false
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                resizeMode = if (crop) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                this.player = player
            }
        },
        modifier = modifier,
    )
}

/**
 * Full-screen video, Telegram-style with M3 Expressive controls: plays as soon as it's downloaded (progress shown
 * over the preview), tap shows/hides the controls, double-tap left/right jumps 10 s, the big play button morphs
 * circle ⇄ squircle, wavy seek bar, speed and sound toggles; pull down to close.
 */
@Composable
fun VideoViewerHost() {
    val v = VideoViewer.current ?: return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val appear = remember(v) { Animatable(0f) }
    val drag = remember(v) { Animatable(0f) }
    val origin = remember(v) { VideoViewer.origin.also { VideoViewer.origin = null } }
    LaunchedEffect(v) { appear.animateTo(1f, spring(0.86f, 300f)) }
    var closing by remember(v) { mutableStateOf(false) }
    fun close() {
        if (closing) return
        closing = true
        scope.launch { appear.animateTo(0f, spring(1f, 380f, visibilityThreshold = 0.001f)); VideoViewer.current = null }
    }
    var host by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val flyBmp = rememberFileBitmap(v.thumb, 720) ?: remember(v) { Images.mini(v.mini) }
    BackHandler { close() }

    val path = rememberLocalPath(v.file)
    val live = TdStore.files[v.file.id] ?: v.file
    val player = remember(v, path) {
        path?.let {
            ExoPlayer.Builder(ctx).build().apply {
                setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(it))))
                playWhenReady = true
                prepare()
            }
        }
    }
    DisposableEffect(player) { onDispose { player?.release() } }
    var playing by remember { mutableStateOf(true) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(v.duration * 1000L) }
    var muted by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(1f) }
    var chrome by remember { mutableStateOf(true) }
    var jump by remember { mutableIntStateOf(0) }   // -1 / +1: the "10 s" hint
    LaunchedEffect(player) {
        while (player != null) {
            pos = player.currentPosition
            if (player.duration > 0) dur = player.duration
            playing = player.isPlaying || (player.playWhenReady && player.playbackState == MPlayer.STATE_BUFFERING)
            delay(50)
        }
    }
    LaunchedEffect(chrome, playing) { if (chrome && playing) { delay(3000); chrome = false } }
    LaunchedEffect(jump) { if (jump != 0) { delay(600); jump = 0 } }

    val pull = (kotlin.math.abs(drag.value) / 900f).coerceIn(0f, 1f)
    val local = origin?.translate(-host.x, -host.y)
    val canFly = local != null && flyBmp != null
    val flying = canFly && appear.value < 0.999f
    val round = v.width == 240 && v.height == 240
    androidx.compose.foundation.layout.BoxWithConstraints(
        Modifier.fillMaxSize().onGloballyPositioned { host = it.positionOnScreen() }
            .background(Color.Black.copy(alpha = (appear.value * (1f - pull)).coerceIn(0f, 1f)))
            .pointerInput(v) {
                detectVerticalDragGestures(
                    onVerticalDrag = { c, dy -> c.consume(); scope.launch { drag.snapTo(drag.value + dy) } },
                    onDragEnd = { if (kotlin.math.abs(drag.value) > 220f) close() else scope.launch { drag.animateTo(0f, spring(0.8f, 500f)) } },
                )
            }
            .pointerInput(player) {
                detectTapGestures(
                    onTap = { chrome = !chrome },
                    onDoubleTap = { o ->
                        val p = player ?: return@detectTapGestures
                        val fwd = o.x > size.width / 2
                        p.seekTo((p.currentPosition + if (fwd) 10_000 else -10_000).coerceIn(0, p.duration.coerceAtLeast(0)))
                        jump = if (fwd) 1 else -1
                    },
                )
            },
    ) {
        val W = constraints.maxWidth.toFloat(); val H = constraints.maxHeight.toFloat()
        val fit = fitRect(W, H, v.width, v.height)
        Box(Modifier.fillMaxSize().graphicsLayer {
            if (flying) alpha = 0f   // the flying picture stands in while it moves
            else {
                translationY = drag.value
                val s = (1f - pull * 0.2f) * (if (canFly) 1f else 0.92f + 0.08f * appear.value)
                scaleX = s; scaleY = s
                alpha = if (canFly) 1f else appear.value
            }
        }, contentAlignment = Alignment.Center) {
            // a TextureView (not a SurfaceView) so the video follows transforms and alpha with the rest
            if (player != null) AndroidView(
                factory = { c -> android.view.TextureView(c) },
                update = { tv -> player.setVideoTextureView(tv) },
                modifier = Modifier.aspectRatio(v.width.toFloat() / v.height.coerceAtLeast(1), matchHeightConstraintsFirst = W / H > v.width.toFloat() / v.height.coerceAtLeast(1)),
            ) else {
                val bmp = rememberFileBitmap(v.thumb, 720) ?: remember(v) { Images.mini(v.mini) }
                if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                val f = if (live.size > 0) live.local.downloadedSize / live.size.toFloat() else 0f
                Box(contentAlignment = Alignment.Center) {
                    WavyRing(f.coerceIn(0.02f, 1f), true, Color.White, Color.White.copy(alpha = 0.25f), Modifier.size(72.dp))
                    Text("${(f * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (flying && local != null && flyBmp != null) FlyingMedia(flyBmp, local, fit.pulled(1f - pull * 0.2f, drag.value), appear.value, circle = round)
        // ±10 s hint
        AnimatedVisibility(jump != 0, Modifier.align(if (jump > 0) Alignment.CenterEnd else Alignment.CenterStart).padding(32.dp), enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(if (jump > 0) Icons.Outlined.Forward10 else Icons.Outlined.Replay10, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }
        val ca by animateFloatAsState(if (chrome && pull < 0.05f) 1f else 0f, label = "ch")
        if (ca > 0.01f) {
            Row(Modifier.statusBarsPadding().padding(10.dp).fillMaxWidth().graphicsLayer { alpha = ca * appear.value }, verticalAlignment = Alignment.CenterVertically) {
                MorphIconButton(Icons.Outlined.Close, "Закрыть", { close() }, color = Color.White.copy(alpha = 0.16f), tint = Color.White)
            }
            if (player != null) Box(Modifier.align(Alignment.Center).graphicsLayer { alpha = ca }) {
                PlayMorphButton(playing, 84.dp, Color.White.copy(alpha = 0.22f), Color.White) { if (player.isPlaying) player.pause() else { if (player.playbackState == MPlayer.STATE_ENDED) player.seekTo(0); player.play() }; chrome = true }
            }
            Column(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp).fillMaxWidth()
                    .graphicsLayer { alpha = ca * appear.value; translationY = (1f - ca) * 40.dp.toPx() }
                    .clip(RoundedCornerShape(28.dp.rs())).background(Color(0xFF1E1E22).copy(alpha = 0.88f)).padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                WavySeekBar(if (dur > 0) pos / dur.toFloat() else 0f, playing, cs.primary, Color.White.copy(alpha = 0.25f), Modifier.fillMaxWidth().height(32.dp).noSwipeBack()) { f ->
                    player?.seekTo((dur * f).toLong()); chrome = true
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${Texts.duration((pos / 1000).toInt())} / ${Texts.duration((dur / 1000).toInt())}", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.weight(1f))
                    QTextButton(onClick = {
                        speed = when (speed) { 1f -> 1.5f; 1.5f -> 2f; 2f -> 0.5f; else -> 1f }
                        player?.playbackParameters = PlaybackParameters(speed); chrome = true
                    }) { Text((if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()) + "×", color = Color.White) }
                    MorphIconButton(if (muted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp, "Звук", {
                        muted = !muted; player?.volume = if (muted) 0f else 1f; chrome = true
                    }, tint = Color.White)
                }
            }
        }
    }
}
