package app.qwgram.next.ui

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player as MPlayer
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.qwgram.next.td.Td
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.Texts
import app.qwgram.next.td.rememberFileBitmap
import kotlinx.coroutines.delay
import org.drinkless.tdlib.TdApi

/** active stories of chats (contacts, channels), as Telegram sends them */
object Stories {
    val active = androidx.compose.runtime.mutableStateMapOf<Long, TdApi.ChatActiveStories>()
    /** the chat whose stories are being watched */
    var open by mutableStateOf<Long?>(null)
    /** "new story" was asked for (picker → compose sheet) */
    var compose by mutableStateOf(false)
    private var loaded = false

    fun onUpdate(u: TdApi.UpdateChatActiveStories) {
        val s = u.activeStories
        if (s.stories.isEmpty() || s.list == null) active.remove(s.chatId) else active[s.chatId] = s
    }

    /** asks TDLib for the main story list (several pages) */
    fun load() {
        if (loaded) return
        loaded = true
        fun page(n: Int) { if (n < 6) Td.send(TdApi.LoadActiveStories(TdApi.StoryListMain())) { r -> if (r !is TdApi.Error) page(n + 1) } }
        page(0)
    }

    fun unread(s: TdApi.ChatActiveStories) = s.stories.any { it.storyId > s.maxReadStoryId }

    /** chats with stories in Telegram's order: unread first, then by the server's order */
    fun ordered(): List<TdApi.ChatActiveStories> = active.values.filter { it.list is TdApi.StoryListMain }
        .sortedWith(compareByDescending<TdApi.ChatActiveStories> { unread(it) }.thenByDescending { it.order })
}

/** the row of story circles above the chat list (Telegram) */
@Composable
fun StoriesStrip(modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { Stories.load() }
    val meId = TdStore.me?.id
    // your own stories lead the row; with none, the first circle posts one
    val list = Stories.ordered().sortedByDescending { it.chatId == meId }
    if (list.isEmpty()) return
    val cs = MaterialTheme.colorScheme
    LazyRow(modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (list.none { it.chatId == meId }) item(key = "new") {
            Column(
                Modifier.width(68.dp).clip(RoundedCornerShape(16.dp)).clickable { Stories.compose = true }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(60.dp).border(1.5.dp, cs.outlineVariant, CircleShape).padding(4.dp), contentAlignment = Alignment.Center) {
                    TdStore.me?.let { UserAvatar(it, 52.dp) }
                    Box(Modifier.align(Alignment.BottomEnd).size(20.dp).clip(CircleShape).background(cs.primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, "Новая история", Modifier.size(16.dp), tint = cs.onPrimary)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Ваша история", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = cs.onSurfaceVariant)
            }
        }
        items(list, key = { it.chatId }) { s ->
            val chat = TdStore.chats[s.chatId]
            val unread = Stories.unread(s)
            Column(
                Modifier.width(68.dp).clip(RoundedCornerShape(16.dp)).clickable { Stories.open = s.chatId }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val ring = if (unread) Brush.linearGradient(listOf(cs.primary, cs.tertiary)) else Brush.linearGradient(listOf(cs.outlineVariant, cs.outlineVariant))
                Box(Modifier.size(60.dp).border(if (unread) 2.5.dp else 1.5.dp, ring, CircleShape).padding(4.dp)) {
                    if (chat != null) ChatAvatar(chat, 52.dp, Modifier.clip(CircleShape))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    chat?.let { if (TdStore.isSaved(it)) "Вы" else it.title.substringBefore(' ') } ?: "",
                    style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (unread) cs.onSurface else cs.onSurfaceVariant, textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** full-screen story viewer: progress segments, tap left/right, hold to pause, pull down to close; goes on to the next chat */
@Composable
fun StoryViewerHost() {
    val chatId = Stories.open ?: return
    val order = remember { Stories.ordered().map { it.chatId } }
    var current by remember { mutableStateOf(chatId) }
    val s = Stories.active[current]
    Dialog(onDismissRequest = { Stories.open = null }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler { Stories.open = null }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (s == null) LaunchedEffect(Unit) { Stories.open = null }
            else StoryChat(s, current, order, onChat = { current = it })
        }
    }
}

@Composable
private fun StoryChat(s: TdApi.ChatActiveStories, current: Long, order: List<Long>, onChat: (Long) -> Unit) {
            val ids = s.stories.map { it.storyId }
            // start at the first story not seen yet
            var index by remember(current) { mutableIntStateOf(ids.indexOfFirst { it > s.maxReadStoryId }.takeIf { it >= 0 } ?: 0) }
            fun nextChat(dir: Int) {
                val i = order.indexOf(current) + dir
                val n = order.getOrNull(i)
                if (n != null && Stories.active[n] != null) onChat(n) else Stories.open = null
            }
            fun next() { if (index < ids.lastIndex) index++ else nextChat(1) }
            fun prev() { if (index > 0) index-- else nextChat(-1) }
            androidx.compose.runtime.key(current, index) {
                StoryPage(current, ids.getOrNull(index) ?: 0, index, ids.size, onNext = ::next, onPrev = ::prev, onClose = { Stories.open = null })
            }
}

@Composable
private fun StoryPage(chatId: Long, storyId: Int, index: Int, count: Int, onNext: () -> Unit, onPrev: () -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var story by remember { mutableStateOf<TdApi.Story?>(null) }
    var ready by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(5000L) }
    val progress = remember { Animatable(0f) }
    var pull by remember { mutableStateOf(0f) }
    var likedNow by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(storyId) { story = Td.callOrNull(TdApi.GetStory(chatId, storyId, false)) }
    DisposableEffect(storyId) {
        Td.send(TdApi.OpenStory(chatId, storyId))
        onDispose { Td.send(TdApi.CloseStory(chatId, storyId)) }
    }
    // the timer: runs while the story is shown and not held
    LaunchedEffect(ready, paused, duration) {
        if (!ready || paused) { progress.stop(); return@LaunchedEffect }
        val left = ((1f - progress.value) * duration).toLong().coerceAtLeast(1)
        progress.animateTo(1f, tween(left.toInt(), easing = LinearEasing))
        onNext()
    }
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { translationY = pull; val k = 1f - (pull / 3000f).coerceIn(0f, 0.15f); scaleX = k; scaleY = k }
            .pointerInput(storyId) {
                detectTapGestures(
                    onPress = { paused = true; tryAwaitRelease(); paused = false },
                    onTap = { o -> if (o.x < size.width / 3f) onPrev() else onNext() },
                )
            }
            .pointerInput(storyId) {
                detectVerticalDragGestures(
                    onDragEnd = { if (pull > 260f) onClose() else pull = 0f },
                    onDragCancel = { pull = 0f },
                ) { _, d -> pull = (pull + d).coerceAtLeast(0f) }
            },
    ) {
        val st = story
        when (val c = st?.content) {
            is TdApi.StoryContentPhoto -> {
                val big = c.photo.sizes.maxByOrNull { it.width * it.height }
                val bmp = rememberFileBitmap(big?.photo, maxSide = 1600)
                LaunchedEffect(bmp != null) { if (bmp != null) { duration = 5000L; ready = true } }
                if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else LoadingIndicator(Modifier.align(Alignment.Center), color = Color.White)
            }
            is TdApi.StoryContentVideo -> {
                val path = rememberLocalPath(c.video.video)
                if (path != null) StoryVideo(path, paused) { ms -> duration = ms.coerceAtLeast(1000L); ready = true }
                else LoadingIndicator(Modifier.align(Alignment.Center), color = Color.White)
            }
            null -> LoadingIndicator(Modifier.align(Alignment.Center), color = Color.White)
            else -> Text("Эта история пока не поддерживается", color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        // top: segments, who posted, when
        Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent))).statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(count) { i ->
                    val f = when { i < index -> 1f; i == index -> progress.value; else -> 0f }
                    Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f))) {
                        Box(Modifier.fillMaxWidth(f).height(3.dp).background(Color.White))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TdStore.chats[chatId]?.let { ChatAvatar(it, 36.dp) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(TdStore.chats[chatId]?.title ?: "", color = Color.White, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    st?.let { Text(Texts.time(it.date), color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium) }
                }
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Закрыть", tint = Color.White) }
            }
        }
        // bottom: caption and a heart
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))).navigationBarsPadding().padding(16.dp)) {
            st?.caption?.takeIf { it.text.isNotBlank() }?.let { RichText(it, Color.White) }
            if (st != null && st.canBeReplied) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                val liked = likedNow ?: (st.chosenReactionType != null)
                IconButton(onClick = {
                    val t: TdApi.ReactionType? = if (liked) null else TdApi.ReactionTypeEmoji("❤")
                    likedNow = !liked
                    Td.send(TdApi.SetStoryReaction(chatId, storyId, t, true)) { r -> if (r is TdApi.Error) likedNow = liked }
                    if (!liked) android.widget.Toast.makeText(ctx, "❤ отправлено", android.widget.Toast.LENGTH_SHORT).show()
                }) { Icon(if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "Нравится", tint = if (liked) Color(0xFFFF4D6D) else Color.White) }
            }
        }
    }
}

/** a story video with sound; reports its length once known; pauses while the story is held */
@Composable
private fun StoryVideo(path: String, paused: Boolean, onReady: (Long) -> Unit) {
    val ctx = LocalContext.current
    val player = remember(path) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(path))))
            repeatMode = MPlayer.REPEAT_MODE_OFF
            playWhenReady = true
            prepare()
        }
    }
    LaunchedEffect(player) {
        while (player.duration <= 0L) delay(50)
        onReady(player.duration)
    }
    LaunchedEffect(paused) { player.playWhenReady = !paused }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        factory = { c ->
            PlayerView(c).apply {
                useController = false
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                this.player = player
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
