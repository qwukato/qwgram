package app.qwgram.next.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Gif
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import app.qwgram.next.td.Td
import app.qwgram.next.td.rememberFileBitmap
import org.drinkless.tdlib.TdApi
import java.util.Calendar

// ------------------------------------------------------------------ emoji / stickers / GIF panel

private val EMOJI = linkedMapOf(
    "Недавние" to "",
    "Смайлы" to "😀😃😄😁😆😅🤣😂🙂🙃🫠😉😊😇🥰😍🤩😘😗☺😚😙🥲😋😛😜🤪😝🤑🤗🤭🫢🫣🤫🤔🫡🤐🤨😐😑😶🫥😏😒🙄😬🤥😌😔😪🤤😴😷🤒🤕🤢🤮🤧🥵🥶🥴😵🤯🤠🥳🥸😎🤓🧐😕🫤😟🙁☹😮😯😲😳🥺🥹😦😧😨😰😥😢😭😱😖😣😞😓😩😫🥱😤😡😠🤬😈👿💀☠💩🤡👹👺👻👽👾🤖😺😸😹😻😼😽🙀😿😾",
    "Жесты" to "👋🤚🖐✋🖖🫱🫲🫳🫴👌🤌🤏✌🤞🫰🤟🤘🤙👈👉👆🖕👇☝🫵👍👎✊👊🤛🤜👏🙌🫶👐🤲🤝🙏✍💅🤳💪🦾🦵🦶👂🦻👃🧠🫀🫁🦷🦴👀👁👅👄🫦",
    "Сердца" to "❤🧡💛💚💙💜🖤🤍🤎💔❣💕💞💓💗💖💘💝💟♥🔥✨🌟⭐💫💥💯💢💤💦💨🎉🎊🎈🎁🏆🥇",
    "Природа" to "🐶🐱🐭🐹🐰🦊🐻🐼🐨🐯🦁🐮🐷🐸🐵🙈🙉🙊🐔🐧🐦🐤🦆🦅🦉🦇🐺🐗🐴🦄🐝🐛🦋🐌🐞🐢🐍🦖🐙🦑🦀🐠🐟🐬🐳🦈🌵🎄🌲🌳🌴🌱🌿🍀🍁🍂🌷🌹🥀🌺🌸🌼🌻🌞🌝🌚🌙⭐☀⛅☁🌧⛈❄☃🌈🌊",
    "Еда" to "🍏🍎🍐🍊🍋🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅🥑🥦🥒🌶🌽🥕🥔🥐🍞🥖🧀🥚🍳🥞🧇🥓🍗🍖🌭🍔🍟🍕🥪🌮🌯🥗🍝🍜🍣🍱🥟🍤🍙🍚🍰🎂🧁🍫🍬🍭🍩🍪☕🍵🧃🥤🧋🍺🍻🥂🍷🍸🍹",
    "Предметы" to "⚽🏀🏈⚾🎾🏐🎱🏓🎮🕹🎲🧩🎯🎳🎸🎹🥁🎤🎧📱💻⌨🖥📷📸📹🎥📺📻⏰⌛💡🔦📚📖✏📝📌📎🔑🔒💰💳💎🔧🔨⚙🧲💊🚗🚕🚌🚀✈🚲🏠🏡🏢🗺🧭⛺",
)

private fun graphemes(s: String): List<String> {
    val it = android.icu.text.BreakIterator.getCharacterInstance()
    it.setText(s)
    val out = ArrayList<String>()
    var start = it.first()
    var end = it.next()
    while (end != android.icu.text.BreakIterator.DONE) {
        out.add(s.substring(start, end)); start = end; end = it.next()
    }
    return out
}

object RecentEmoji {
    private val sp by lazy { app.qwgram.next.App.instance.getSharedPreferences("recent_emoji", 0) }
    fun get(): List<String> = sp.getString("list", "😂❤👍🔥😭🙏😍🥰")!!.let { graphemes(it) }
    fun add(e: String) {
        val l = (listOf(e) + get().filter { it != e }).take(32)
        sp.edit().putString("list", l.joinToString("")).apply()
    }
}

/** Telegram-style panel that replaces the keyboard: emoji grid, installed sticker sets and saved GIFs. */
@Composable
fun StickerPanel(chatId: Long, height: Dp, onEmoji: (String) -> Unit, onSticker: (TdApi.Sticker) -> Unit, onGif: (TdApi.Animation) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().background(cs.surfaceContainer).navigationBarsPadding().height(height)) {
        PrimaryTabRow(selectedTabIndex = tab, containerColor = cs.surfaceContainer) {
            listOf(Icons.Outlined.EmojiEmotions to "Эмодзи", Icons.Outlined.StickyNote2 to "Стикеры", Icons.Outlined.Gif to "GIF").forEachIndexed { i, (ic, t) ->
                Tab(selected = tab == i, onClick = { tab = i }, text = {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(ic, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(t) }
                })
            }
        }
        AnimatedContent(tab, transitionSpec = { fadeIn(Springs.effects()).togetherWith(fadeOut(Springs.exit())) }, label = "panel") { t ->
            when (t) {
                0 -> EmojiGrid(onEmoji)
                1 -> StickerGrid(onSticker)
                else -> GifGrid(onGif)
            }
        }
    }
}

private val EMOJI_ICONS = listOf(
    Icons.Outlined.History, Icons.Outlined.EmojiEmotions, Icons.Outlined.PanTool, Icons.Outlined.FavoriteBorder,
    Icons.Outlined.Pets, Icons.Outlined.Restaurant, Icons.Outlined.SportsSoccer,
)

/** grid with a category strip on top: tap scrolls to the section, the current one is highlighted (springy pill) */
@Composable
private fun CategoryStrip(count: Int, current: Int, onPick: (Int) -> Unit, icon: @Composable (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val rowState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(current) { if (count > 0) rowState.animateScrollToItem((current - 2).coerceAtLeast(0)) }
    LazyRow(state = rowState, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items(count) { i ->
            val sel = i == current
            val bg by androidx.compose.animation.animateColorAsState(if (sel) cs.secondaryContainer else Color.Transparent, Springs.effects(), label = "c")
            val r by androidx.compose.animation.core.animateDpAsState(if (sel) 14.dp else 20.dp, Springs.fastSpatial(), label = "r")
            MorphBox(onClick = { onPick(i) }, modifier = Modifier.size(40.dp), rest = r, pressed = 10.dp, color = bg,
                contentColor = if (sel) cs.onSecondaryContainer else cs.onSurfaceVariant, squish = 0.9f) { icon(i) }
        }
    }
}

private fun currentSection(state: LazyGridState, headers: List<Int>): Int {
    val first = state.firstVisibleItemIndex
    var cur = 0
    headers.forEachIndexed { i, h -> if (h <= first + 1) cur = i }
    return cur
}

@Composable
private fun EmojiGrid(onEmoji: (String) -> Unit) {
    val sections = remember { EMOJI.map { (k, v) -> k to if (k == "Недавние") RecentEmoji.get() else graphemes(v) } }
    val headers = remember(sections) {
        var idx = 0
        sections.map { (_, l) -> val h = idx; idx += 1 + l.size; h }
    }
    val state = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val current by remember { derivedStateOf { currentSection(state, headers) } }
    Column(Modifier.fillMaxSize()) {
        CategoryStrip(sections.size, current, { i -> scope.launch { state.animateScrollToItem(headers[i]) } }) { i ->
            Icon(EMOJI_ICONS.getOrElse(i) { Icons.Outlined.EmojiEmotions }, sections[i].first, Modifier.size(22.dp))
        }
        LazyVerticalGrid(GridCells.Adaptive(46.dp), Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
            sections.forEach { (title, list) ->
                item(span = { GridItemSpan(maxLineSpan) }) { PanelHeader(title) }
                items(list) { e ->
                    MorphBox(onClick = { RecentEmoji.add(e); onEmoji(e) }, modifier = Modifier.size(46.dp), rest = 14.dp, pressed = 23.dp, color = Color.Transparent, squish = 0.85f) {
                        Text(e, fontSize = 27.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerGrid(onSticker: (TdApi.Sticker) -> Unit) {
    var recent by remember { mutableStateOf<List<TdApi.Sticker>>(emptyList()) }
    var sets by remember { mutableStateOf<List<TdApi.StickerSet>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        recent = Td.callOrNull(TdApi.GetRecentStickers(false))?.stickers?.toList().orEmpty().take(20)
        val infos = Td.callOrNull(TdApi.GetInstalledStickerSets(TdApi.StickerTypeRegular()))?.sets?.toList().orEmpty()
        loading = false
        val out = ArrayList<TdApi.StickerSet>()
        for (i in infos.take(60)) {
            Td.callOrNull(TdApi.GetStickerSet(i.id))?.let { out.add(it); if (out.size % 4 == 0) sets = out.toList() }
        }
        sets = out.toList()
    }
    if (loading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }; return }
    if (recent.isEmpty() && sets.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Нет установленных стикеров", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }
    // section header positions inside the grid
    val headers = remember(recent, sets) {
        val h = ArrayList<Int>()
        var idx = 0
        if (recent.isNotEmpty()) { h.add(idx); idx += 1 + recent.size }
        sets.forEach { st -> h.add(idx); idx += 1 + st.stickers.size }
        h
    }
    val state = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val current by remember(headers) { derivedStateOf { currentSection(state, headers) } }
    Column(Modifier.fillMaxSize()) {
        CategoryStrip(headers.size, current, { i -> scope.launch { state.animateScrollToItem(headers[i]) } }) { i ->
            val setIndex = if (recent.isNotEmpty()) i - 1 else i
            if (setIndex < 0) Icon(Icons.Outlined.History, "Недавние", Modifier.size(22.dp))
            else {
                val st = sets.getOrNull(setIndex)
                val cover = st?.thumbnail?.file ?: st?.stickers?.firstOrNull()?.let { if (it.format is TdApi.StickerFormatWebp) it.sticker else it.thumbnail?.file }
                val bmp = rememberFileBitmap(cover, 96)
                if (bmp != null) Image(bmp, st?.title, Modifier.size(30.dp), contentScale = ContentScale.Fit)
                else Text(st?.stickers?.firstOrNull()?.emoji ?: "•", fontSize = 18.sp)
            }
        }
        LazyVerticalGrid(GridCells.Adaptive(78.dp), Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
            if (recent.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "hr") { PanelHeader("Недавние") }
                items(recent, key = { "r" + it.id }) { StickerCell(it, onSticker) }
            }
            sets.forEach { set ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "h" + set.id) { PanelHeader(set.title) }
                items(set.stickers.toList(), key = { "s" + set.id + "_" + it.id }) { StickerCell(it, onSticker) }
            }
        }
    }
}

@Composable
private fun PanelHeader(t: String) =
    Text(t, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 6.dp))

@Composable
private fun StickerCell(s: TdApi.Sticker, onSticker: (TdApi.Sticker) -> Unit) {
    // static .webp stickers decode directly; animated/video ones show their webp thumbnail
    val file = if (s.format is TdApi.StickerFormatWebp) s.sticker else s.thumbnail?.file
    val bmp = rememberFileBitmap(file, 192)
    MorphBox(onClick = { onSticker(s) }, modifier = Modifier.aspectRatio(1f).padding(3.dp), rest = 16.dp, pressed = 28.dp, color = Color.Transparent, squish = 0.88f) {
        if (bmp != null) Image(bmp, s.emoji, Modifier.fillMaxSize().padding(2.dp), contentScale = ContentScale.Fit)
        else Text(s.emoji, fontSize = 28.sp)
    }
}

@Composable
private fun GifGrid(onGif: (TdApi.Animation) -> Unit) {
    var gifs by remember { mutableStateOf<List<TdApi.Animation>?>(null) }
    LaunchedEffect(Unit) { gifs = Td.callOrNull(TdApi.GetSavedAnimations())?.animations?.toList().orEmpty() }
    val list = gifs
    if (list == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }; return }
    if (list.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Нет сохранённых GIF", color = MaterialTheme.colorScheme.onSurfaceVariant) }; return }
    LazyVerticalGrid(GridCells.Adaptive(110.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(6.dp)) {
        items(list, key = { it.animation.id }) { a ->
            val bmp = rememberFileBitmap(a.thumbnail?.file, 256)
            MorphBox(onClick = { onGif(a) }, modifier = Modifier.aspectRatio(1f).padding(3.dp), rest = 14.dp, pressed = 24.dp, color = MaterialTheme.colorScheme.surfaceContainerHigh, squish = 0.92f) {
                if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
    }
}

fun stickerContent(s: TdApi.Sticker): TdApi.InputMessageContent =
    TdApi.InputMessageSticker(TdApi.InputSticker(TdApi.InputFileRemote(s.sticker.remote.id), null, s.width, s.height), s.emoji)

fun gifContent(a: TdApi.Animation): TdApi.InputMessageContent = TdApi.InputMessageAnimation().apply {
    animation = TdApi.InputAnimation(TdApi.InputFileRemote(a.animation.remote.id), null, intArrayOf(), a.duration, a.width, a.height)
    caption = TdApi.FormattedText("", arrayOf())
}

// ------------------------------------------------------------------ send options (long-press on send)

@Composable
fun SendOptionsSheet(onDismiss: () -> Unit, onSilent: () -> Unit, onSchedule: (Int) -> Unit) {
    var pickDate by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        ListItem(headlineContent = { Text("Отправить без звука") }, supportingContent = { Text("Получатель не услышит уведомление") },
            leadingContent = { Icon(Icons.Outlined.NotificationsOff, null) }, modifier = Modifier.clickable { onSilent(); onDismiss() })
        ListItem(headlineContent = { Text("Отправить позже") }, supportingContent = { Text("Выбрать дату и время") },
            leadingContent = { Icon(Icons.Outlined.Schedule, null) }, modifier = Modifier.clickable { pickDate = true })
        val quick = listOf("Через час" to 3600, "Через 3 часа" to 3 * 3600)
        quick.forEach { (t, secs) ->
            ListItem(headlineContent = { Text(t) }, leadingContent = { Spacer(Modifier.width(24.dp)) },
                modifier = Modifier.clickable { onSchedule((System.currentTimeMillis() / 1000).toInt() + secs); onDismiss() })
        }
        Spacer(Modifier.height(24.dp))
    }
    if (pickDate) ScheduleDialog(onDismiss = { pickDate = false }) { onSchedule(it); pickDate = false; onDismiss() }
}

@Composable
private fun ScheduleDialog(onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val now = Calendar.getInstance()
    val date = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val time = rememberTimePickerState(now.get(Calendar.HOUR_OF_DAY) + 1 , 0, true)
    if (step == 0) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = { QTextButton(onClick = { step = 1 }) { Text("Далее") } },
            dismissButton = { QTextButton(onClick = onDismiss) { Text("Отмена") } },
        ) { DatePicker(date) }
    } else {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Время отправки") },
            text = { TimePicker(time) },
            confirmButton = {
                QTextButton(onClick = {
                    val c = Calendar.getInstance().apply {
                        timeInMillis = date.selectedDateMillis ?: System.currentTimeMillis()
                        // DatePicker returns UTC midnight; rebuild in local time
                        val y = get(Calendar.YEAR); val m = get(Calendar.MONTH); val d = get(Calendar.DAY_OF_MONTH)
                        timeZone = java.util.TimeZone.getDefault()
                        set(y, m, d, time.hour % 24, time.minute, 0)
                    }
                    val t = (c.timeInMillis / 1000).toInt()
                    onPick(maxOf(t, (System.currentTimeMillis() / 1000).toInt() + 60))
                }) { Text("Запланировать") }
            },
            dismissButton = { QTextButton(onClick = onDismiss) { Text("Отмена") } },
        )
    }
}

// ------------------------------------------------------------------ selection & search bars

@Composable
fun SelectionBar(count: Int, onClose: () -> Unit, onCopy: () -> Unit, onForward: () -> Unit, onDelete: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.statusBarsPadding().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            MorphIconButton(Icons.Rounded.Close, "Отменить", onClose)
            AnimatedContent(count, transitionSpec = { (fadeIn(Springs.effects())).togetherWith(fadeOut(Springs.exit())) }, label = "cnt", modifier = Modifier.weight(1f).padding(start = 8.dp)) { n ->
                Text("Выбрано: $n", style = MaterialTheme.typography.titleLarge)
            }
            MorphIconButton(Icons.Outlined.ContentCopy, "Копировать", onCopy)
            MorphIconButton(Icons.AutoMirrored.Outlined.Forward, "Переслать", onForward)
            MorphIconButton(Icons.Outlined.Delete, "Удалить", onDelete, tint = MaterialTheme.colorScheme.error)
        }
    }
}

/** search inside a chat: query → TDLib SearchChatMessages, arrows step through results */
class ChatSearch(val chatId: Long) {
    var query by mutableStateOf("")
    var results by mutableStateOf<List<TdApi.Message>>(emptyList())
    var total by mutableIntStateOf(0)
    var index by mutableIntStateOf(0)
    var busy by mutableStateOf(false)

    suspend fun run() {
        val q = query.trim()
        if (q.isEmpty()) { results = emptyList(); total = 0; return }
        busy = true
        val r = Td.callOrNull(TdApi.SearchChatMessages(chatId, null, q, null, 0, 0, 100, null))
        results = r?.messages?.toList().orEmpty()
        total = r?.totalCount ?: 0
        index = 0
        busy = false
    }

    val current: TdApi.Message? get() = results.getOrNull(index)
}

@Composable
fun ChatSearchBar(s: ChatSearch, onClose: () -> Unit, onSubmit: () -> Unit, onStep: (Int) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.surfaceContainer) {
        Row(Modifier.statusBarsPadding().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            MorphIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", onClose)
            Box(Modifier.weight(1f).padding(horizontal = 4.dp).height(48.dp).clip(CircleShape).background(cs.surfaceContainerHighest).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                BasicTextField(
                    s.query, { s.query = it }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface), cursorBrush = SolidColor(cs.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    decorationBox = { inner -> if (s.query.isEmpty()) Text("Поиск в чате", color = cs.onSurfaceVariant); inner() },
                )
            }
            if (s.results.isNotEmpty()) {
                Text("${s.index + 1}/${s.total}", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                MorphIconButton(Icons.Outlined.KeyboardArrowUp, "Раньше", { onStep(1) }, enabled = s.index < s.results.size - 1)
                MorphIconButton(Icons.Outlined.KeyboardArrowDown, "Позже", { onStep(-1) }, enabled = s.index > 0)
            } else if (s.busy) LoadingIndicator(Modifier.size(36.dp))
        }
    }
}

/** horizontal reactions picker for the message menu, with the chat's allowed reactions */
@Composable
fun ReactionStrip(emojis: List<String>, onPick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
        items(emojis) { e ->
            MorphBox(onClick = { onPick(e) }, modifier = Modifier.size(48.dp), rest = 24.dp, pressed = 12.dp, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Text(e, fontSize = 24.sp)
            }
        }
    }
}
