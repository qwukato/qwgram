package app.qwgram.next.ui

import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.StarBorder
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
import app.qwgram.next.Prefs
import app.qwgram.next.td.rememberFileBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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

/**
 * Telegram-style panel that replaces the keyboard (same height): emoji, stickers and GIFs as pages
 * you can swipe between; the tab indicator follows the swipe.
 */
@Composable
fun StickerPanel(chatId: Long, height: Dp, onEmoji: (String) -> Unit, onSticker: (TdApi.Sticker) -> Unit, onGif: (TdApi.Animation) -> Unit, onCustomEmoji: (TdApi.Sticker) -> Unit = {}) {
    val cs = MaterialTheme.colorScheme
    val pager = androidx.compose.foundation.pager.rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth().height(height).background(cs.surfaceContainer).navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
            ConnectedGroup(listOf(0 to "Эмодзи", 1 to "Стикеры", 2 to "GIF"), pager.currentPage) { i -> scope.launch { pager.animateScrollToPage(i, animationSpec = Springs.smooth()) } }
        }
        androidx.compose.foundation.pager.HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
            when (page) {
                0 -> EmojiGrid(onEmoji, onCustomEmoji)
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

/** installed premium emoji packs (with their emoji), loaded once */
object EmojiPacks {
    val sets = androidx.compose.runtime.mutableStateListOf<TdApi.StickerSet>()
    var loaded by mutableStateOf(false)
    private var loading = false
    suspend fun load() {
        if (loading || loaded) return
        loading = true
        val infos = Td.callOrNull(TdApi.GetInstalledStickerSets(TdApi.StickerTypeCustomEmoji()))?.sets?.toList().orEmpty()
        val out = ArrayList<TdApi.StickerSet>()
        for (i in infos) Td.callOrNull(TdApi.GetStickerSet(i.id))?.let { out.add(it) }
        sets.clear(); sets.addAll(out)
        // the emoji themselves, so they show right away
        out.forEach { st -> st.stickers.forEach { k -> (k.fullType as? TdApi.StickerFullTypeCustomEmoji)?.customEmojiId?.let { id -> if (id != 0L) CustomEmoji.stickers[id] = k } } }
        loaded = true; loading = false
    }
}

/** a grid cell: a plain emoji, or a premium one */
private sealed interface EmojiCell { data class Plain(val e: String) : EmojiCell; data class Custom(val s: TdApi.Sticker) : EmojiCell }

private fun customId(s: TdApi.Sticker) = (s.fullType as? TdApi.StickerFullTypeCustomEmoji)?.customEmojiId ?: 0L

@Composable
private fun EmojiGrid(onEmoji: (String) -> Unit, onCustom: (TdApi.Sticker) -> Unit) {
    LaunchedEffect(Unit) { EmojiPacks.load() }
    val cs = MaterialTheme.colorScheme
    val packs = EmojiPacks.sets.toList()
    val recentCustom = remember(EmojiPacks.loaded) { RecentCustomEmoji.get().mapNotNull { CustomEmoji.stickers[it] } }
    // recent (premium first), the standard categories, then every installed premium pack
    val sections: List<Triple<String, List<EmojiCell>, TdApi.Sticker?>> = remember(packs, recentCustom) {
        EMOJI.map { (k, v) ->
            val plain = (if (k == "Недавние") RecentEmoji.get() else graphemes(v)).map { EmojiCell.Plain(it) }
            Triple(k, if (k == "Недавние") recentCustom.map { EmojiCell.Custom(it) } + plain else plain, null)
        } + packs.map { st -> Triple(st.title, st.stickers.map { EmojiCell.Custom(it) }, st.stickers.firstOrNull()) }
    }
    val headers = remember(sections) {
        var idx = 0
        sections.map { (_, l, _) -> val h = idx; idx += 1 + l.size; h }
    }
    val state = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val current by remember(headers) { derivedStateOf { currentSection(state, headers) } }
    Column(Modifier.fillMaxSize()) {
        CategoryStrip(sections.size, current, { i -> scope.launch { state.animateScrollToItem(headers[i]) } }) { i ->
            val cover = sections[i].third
            if (cover != null) {
                val still = rememberStickerStill(cover, 72)
                if (still != null) Image(still, sections[i].first, Modifier.size(24.dp)) else Icon(Icons.Outlined.EmojiEmotions, sections[i].first, Modifier.size(22.dp))
            } else Icon(EMOJI_ICONS.getOrElse(i) { Icons.Outlined.EmojiEmotions }, sections[i].first, Modifier.size(22.dp))
        }
        LazyVerticalGrid(GridCells.Adaptive(46.dp), Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
            sections.forEach { (title, list, _) ->
                item(span = { GridItemSpan(maxLineSpan) }) { PanelHeader(title) }
                items(list) { cell ->
                    MorphBox(
                        onClick = {
                            when (cell) {
                                is EmojiCell.Plain -> { RecentEmoji.add(cell.e); onEmoji(cell.e) }
                                is EmojiCell.Custom -> onCustom(cell.s)
                            }
                        },
                        modifier = Modifier.size(46.dp), rest = 14.dp, pressed = 23.dp, color = Color.Transparent, squish = 0.85f,
                    ) {
                        when (cell) {
                            is EmojiCell.Plain -> Text(cell.e, fontSize = 27.sp)
                            is EmojiCell.Custom -> PremiumEmoji(customId(cell.s), cs.onSurface, 32.dp)
                        }
                    }
                }
            }
        }
    }
}

private class Sec(val key: String, val title: String, val stickers: List<TdApi.Sticker>, val set: TdApi.StickerSet?)

@Composable
private fun StickerGrid(onSticker: (TdApi.Sticker) -> Unit) {
    LaunchedEffect(Unit) { StickerLib.load() }
    val fav = StickerLib.favorites
    val recent = StickerLib.recent
    val sets = StickerLib.sets
    var preview by remember { mutableStateOf<TdApi.Sticker?>(null) }
    var adding by remember { mutableStateOf(false) }
    if (!StickerLib.loaded) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }; return }
    // sections: ★ favourites, recent, then every installed set
    val sections = remember(fav, recent, sets) {
        buildList {
            if (fav.isNotEmpty()) add(Sec("fav", "Избранные", fav, null))
            if (recent.isNotEmpty()) add(Sec("rec", "Недавние", recent, null))
            sets.forEach { add(Sec("s" + it.id, it.title, it.stickers.toList(), it)) }
        }
    }
    val headers = remember(sections) {
        var idx = 0
        sections.map { sec -> val h = idx; idx += 1 + sec.stickers.size; h }
    }
    val state = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val current by remember(headers) { derivedStateOf { currentSection(state, headers) } }
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                CategoryStrip(sections.size, current, { i -> scope.launch { state.animateScrollToItem(headers[i]) } }) { i ->
                    val sec = sections[i]
                    when (sec.key) {
                        "fav" -> Icon(Icons.Outlined.StarBorder, "Избранные", Modifier.size(22.dp))
                        "rec" -> Icon(Icons.Outlined.History, "Недавние", Modifier.size(22.dp))
                        else -> {
                            val st = sec.set
                            val cover = st?.thumbnail?.file ?: st?.stickers?.firstOrNull()?.let { if (it.format is TdApi.StickerFormatWebp) it.sticker else it.thumbnail?.file }
                            val bmp = rememberFileBitmap(cover, 96)
                            if (bmp != null) Image(bmp, st?.title, Modifier.size(30.dp), contentScale = ContentScale.Fit)
                            else Text(st?.stickers?.firstOrNull()?.emoji ?: "•", fontSize = 18.sp)
                        }
                    }
                }
            }
            // add someone's set by its link / name
            MorphIconButton(Icons.Outlined.Add, "Добавить набор", { adding = true }, modifier = Modifier.padding(end = 4.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (sections.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Нет стикеров — добавьте набор кнопкой +", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else LazyVerticalGrid(GridCells.Adaptive(78.dp), Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
            sections.forEach { sec ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "h" + sec.key) { PanelHeader(sec.title) }
                items(sec.stickers, key = { sec.key + "_" + it.id }) { StickerCell(it, onSticker) { preview = it } }
            }
        }
    }
    preview?.let { st -> StickerPreview(st, onDismiss = { preview = null }, onSend = { onSticker(st) }) }
    if (adding) AddStickerSetDialog { adding = false }
}

@Composable
private fun PanelHeader(t: String) =
    Text(t, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 6.dp))

@Composable
private fun StickerCell(s: TdApi.Sticker, onSticker: (TdApi.Sticker) -> Unit, onLong: (() -> Unit)? = null) {
    // static .webp stickers decode directly; animated/video ones show their webp thumbnail
    val file = if (s.format is TdApi.StickerFormatWebp) s.sticker else s.thumbnail?.file
    val bmp = rememberFileBitmap(file, 192)
    MorphBox(onClick = { onSticker(s) }, onLongClick = onLong, modifier = Modifier.aspectRatio(1f).padding(3.dp), rest = 16.dp, pressed = 28.dp, color = Color.Transparent, squish = 0.88f) {
        if (bmp != null) Image(bmp, s.emoji, Modifier.fillMaxSize().padding(2.dp), contentScale = ContentScale.Fit)
        else Box(contentAlignment = Alignment.Center) {
            Text(s.emoji, fontSize = 26.sp, modifier = Modifier.graphicsLayer { alpha = 0.3f })
            LoadingIndicator(Modifier.size(30.dp))
        }
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
                else LoadingIndicator(Modifier.size(36.dp))
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
    QSheet(onDismiss = onDismiss, wrap = true) {
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
            title = { WindowBlurBehind();  Text("Время отправки") },
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
    Surface(color = if (app.qwgram.next.Prefs.floatingBars.value) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh) {
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
fun ChatSearchBar(s: ChatSearch, origin: androidx.compose.ui.geometry.Rect? = null, grow: () -> Float = { 1f }, onClose: () -> Unit, onSubmit: () -> Unit, onStep: (Int) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(180); runCatching { focus.requestFocus() } }
    val cs = MaterialTheme.colorScheme
    var hostPos by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var placed by remember { mutableStateOf(origin == null) }
    Surface(color = Color.Transparent) {
        Row(Modifier.statusBarsPadding().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.graphicsLayer { alpha = ((grow() - 0.4f) / 0.6f).coerceIn(0f, 1f) }) { MorphIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", onClose) }
            // the field grows out of the search button's bounds (and shrinks back on close)
            Box(
                Modifier.weight(1f).padding(horizontal = 4.dp).height(48.dp)
                    .onGloballyPositioned { hostPos = it.positionInRoot(); placed = true }
                    .graphicsLayer { alpha = if (placed) 1f else 0f }   // hidden for the one frame before its position is known
                    .layout { m, c ->
                        val fw = c.maxWidth; val fh = c.maxHeight
                        val o = origin
                        if (o == null) { val pl = m.measure(c); return@layout layout(pl.width, pl.height) { pl.place(0, 0) } }
                        val p = grow()
                        fun lerp(a: Float, b: Float) = a + (b - a) * p
                        val w = lerp(o.width, fw.toFloat()).toInt().coerceAtLeast(1)
                        val h = lerp(o.height, fh.toFloat()).toInt().coerceAtLeast(1)
                        val pl = m.measure(androidx.compose.ui.unit.Constraints.fixed(w, h))
                        layout(fw, fh) { pl.place(lerp(o.left - hostPos.x, 0f).toInt(), lerp(o.top - hostPos.y, 0f).toInt()) }
                    }
                    // the very same container as the search button (colour, glass): it is the button, grown
                    // solid (even with the blur on): a crisp container reads clearly as the button growing
                    .clip(CircleShape).background(cs.surfaceContainerHigh)
                    .clipToBounds(),
                contentAlignment = Alignment.CenterStart,
            ) {
                // the button's magnifier stays: from the centre of the round button to the field's leading edge
                Icon(
                    Icons.Outlined.Search, null, tint = cs.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer { translationX = (11.dp.toPx()) + (14.dp.toPx() - 11.dp.toPx()) * grow() }.size(24.dp),
                )
                // the field's content is laid out at its final width and only revealed as the pill grows
                // (no re-wrapping of "Поиск в чате" into a tiny box), and fades in during the second half
                Box(Modifier.fillMaxWidth().padding(start = 50.dp, end = 16.dp).graphicsLayer { val g = grow(); alpha = ((g - 0.3f) / 0.5f).coerceIn(0f, 1f); translationX = (1f - g.coerceIn(0f, 1f)) * 24.dp.toPx() }) {
                BasicTextField(
                    s.query, { s.query = it }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface), cursorBrush = SolidColor(cs.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    decorationBox = { inner -> if (s.query.isEmpty()) Text("Поиск в чате", color = cs.onSurfaceVariant, maxLines = 1, softWrap = false); inner() },
                )
                }
            }
            if (s.results.isNotEmpty() && grow() > 0.9f) {
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
