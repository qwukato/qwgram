package app.qwgram.next.ui

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.qwgram.next.td.Td
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

/**
 * Your stickers, loaded once per session and shared by the chat panel and the editors: favourites, recent and the
 * installed sets (each set with its stickers). Changes (add/remove a set, star a sticker) reload what changed.
 */
object StickerLib {
    var favorites by mutableStateOf<List<TdApi.Sticker>>(emptyList())
    var recent by mutableStateOf<List<TdApi.Sticker>>(emptyList())
    var sets by mutableStateOf<List<TdApi.StickerSet>>(emptyList())
    var loaded by mutableStateOf(false)
    private var loading = false

    suspend fun load(force: Boolean = false) {
        if ((loaded && !force) || loading) return
        loading = true
        try {
            favorites = Td.callOrNull(TdApi.GetFavoriteStickers())?.stickers?.toList().orEmpty()
            recent = Td.callOrNull(TdApi.GetRecentStickers(false))?.stickers?.toList().orEmpty().take(24)
            val infos = Td.callOrNull(TdApi.GetInstalledStickerSets(TdApi.StickerTypeRegular()))?.sets?.toList().orEmpty()
            loaded = true
            val old = sets.associateBy { it.id }
            val out = ArrayList<TdApi.StickerSet>()
            for (i in infos.take(80)) {
                (old[i.id] ?: Td.callOrNull(TdApi.GetStickerSet(i.id)))?.let { out.add(it) }
                if (out.size % 6 == 0) sets = out.toList()
            }
            sets = out.toList()
        } finally { loading = false }
    }

    suspend fun reloadFavorites() { favorites = Td.callOrNull(TdApi.GetFavoriteStickers())?.stickers?.toList().orEmpty() }
    fun isFavorite(s: TdApi.Sticker) = favorites.any { it.sticker.id == s.sticker.id }
    fun isInstalled(setId: Long) = sets.any { it.id == setId }
}

/** "Add a sticker set" / "preview a set" requests, shown by [StickerSetHost] (above every screen) */
object StickerSetPreview {
    var setId by mutableStateOf(0L)
    var name by mutableStateOf<String?>(null)
    fun open(id: Long) { if (id != 0L) { name = null; setId = id } }
    fun openName(n: String) { setId = 0L; name = n }
}

/** turns "t.me/addstickers/Name", "tg://addstickers?set=Name" or a bare name into the set's short name */
fun stickerSetName(input: String): String? {
    val t = input.trim()
    if (t.isEmpty()) return null
    Regex("addstickers[/?](?:set=)?([A-Za-z0-9_]+)").find(t)?.let { return it.groupValues[1] }
    return t.takeIf { Regex("^[A-Za-z0-9_]{2,64}$").matches(it) }
}

/** the sticker set preview sheet: every sticker of the set, and add / remove the set */
@Composable
fun StickerSetHost() {
    val id = StickerSetPreview.setId
    val name = StickerSetPreview.name
    if (id == 0L && name == null) return
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var set by remember(id, name) { mutableStateOf<TdApi.StickerSet?>(null) }
    var failed by remember(id, name) { mutableStateOf(false) }
    var preview by remember { mutableStateOf<TdApi.Sticker?>(null) }
    LaunchedEffect(id, name) {
        set = if (id != 0L) Td.callOrNull(TdApi.GetStickerSet(id)) else Td.callOrNull(TdApi.SearchStickerSet(name ?: "", false))
        if (set == null) failed = true
    }
    fun dismiss() { StickerSetPreview.setId = 0L; StickerSetPreview.name = null }
    QSheet(onDismiss = { dismiss() }, overlay = { sh ->
        val s = set
        if (s != null) {
            val installed = StickerLib.isInstalled(s.id) || (s.isInstalled && !s.isArchived)
            Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).fillMaxWidth().graphicsLayer { alpha = sh.progress() }) {
                MorphBox(
                    onClick = {
                        scope.launch {
                            val ok = Td.callOrNull(TdApi.ChangeStickerSet(s.id, !installed, false)) != null
                            if (ok) {
                                Toast.makeText(ctx, if (installed) "Набор удалён" else "Набор «${s.title}» добавлен", Toast.LENGTH_SHORT).show()
                                StickerLib.load(force = true)
                                sh.close()
                            } else Toast.makeText(ctx, "Не получилось", Toast.LENGTH_SHORT).show()
                        }
                    },
                    rest = 28.dp, pressed = 14.dp, elevation = 6.dp,
                    color = if (installed) cs.errorContainer else cs.primary, contentColor = if (installed) cs.onErrorContainer else cs.onPrimary,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (installed) Icons.Outlined.Delete else Icons.Outlined.Add, null)
                        Spacer(Modifier.width(10.dp))
                        Text(if (installed) "Удалить набор" else "Добавить ${s.stickers.size} стикеров", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    }
                }
            }
        }
    }) {
        val s = set
        when {
            s != null -> {
                Text(s.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp))
                Text("${s.stickers.size} стикеров", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                LazyVerticalGrid(GridCells.Adaptive(84.dp), Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 96.dp)) {
                    items(s.stickers.toList(), key = { it.id }) { st ->
                        Box(Modifier.aspectRatio(1f).padding(4.dp).clip(RoundedCornerShape(16.dp)).clickable { preview = st }.padding(4.dp), contentAlignment = Alignment.Center) {
                            StickerContent(st, Modifier.fillMaxSize()) { Text(st.emoji, fontSize = 30.sp) }
                        }
                    }
                }
            }
            failed -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Text("Набор не найден", color = cs.onSurfaceVariant) }
            else -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
        }
    }
    preview?.let { st -> StickerPreview(st, onDismiss = { preview = null }, onSend = null) }
}

/**
 * Long-press preview of a sticker (Telegram): it plays big in the middle; send it, star it (favourites) or open
 * its set. Springs in from small, the background dims.
 */
@Composable
fun StickerPreview(s: TdApi.Sticker, onDismiss: () -> Unit, onSend: (() -> Unit)?) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val appear = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(0.6f, 380f)) }
    fun close(then: () -> Unit = {}) {
        if (closing) return
        closing = true
        scope.launch { appear.animateTo(0f, spring(1f, 900f)); onDismiss(); then() }
    }
    var fav by remember(s) { mutableStateOf(StickerLib.isFavorite(s)) }
    Dialog(onDismissRequest = { close() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = androidx.compose.ui.platform.LocalView.current
        androidx.compose.runtime.SideEffect { (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.setDimAmount(0f) }
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = appear.value.coerceIn(0f, 1f) }.background(Color.Black.copy(alpha = 0.55f))
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { close() },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.emoji, fontSize = 30.sp, modifier = Modifier.graphicsLayer { alpha = appear.value.coerceIn(0f, 1f) })
                Spacer(Modifier.height(8.dp))
                Box(Modifier.size(260.dp).graphicsLayer { val k = 0.4f + 0.6f * appear.value; scaleX = k; scaleY = k }) {
                    StickerContent(s, Modifier.fillMaxSize()) { Text(s.emoji, fontSize = 96.sp) }
                }
                Spacer(Modifier.height(20.dp))
                Column(
                    Modifier.width(260.dp).graphicsLayer { alpha = appear.value.coerceIn(0f, 1f); translationY = (1f - appear.value) * 40.dp.toPx() },
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val rows = buildList<Triple<androidx.compose.ui.graphics.vector.ImageVector, String, () -> Unit>> {
                        if (onSend != null) add(Triple(Icons.AutoMirrored.Filled.Send, "Отправить") { close { onSend() } })
                        add(Triple(if (fav) Icons.Outlined.Star else Icons.Outlined.StarBorder, if (fav) "Убрать из избранного" else "В избранное") {
                            scope.launch {
                                val input = TdApi.InputFileId(s.sticker.id)
                                val ok = Td.callOrNull(if (fav) TdApi.RemoveFavoriteSticker(input) else TdApi.AddFavoriteSticker(input)) != null
                                if (ok) { fav = !fav; StickerLib.reloadFavorites() } else Toast.makeText(ctx, "Не получилось", Toast.LENGTH_SHORT).show()
                            }
                        })
                        if (s.setId != 0L) add(Triple(Icons.Outlined.Collections, "Набор стикеров") { close { StickerSetPreview.open(s.setId) } })
                    }
                    rows.forEachIndexed { i, (icon, label, act) ->
                        Row(
                            Modifier.segment(i, rows.size, cs.surfaceContainerHigh).clickable { act() }.padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(icon, null, tint = cs.onSurface)
                            Spacer(Modifier.width(14.dp))
                            Text(label, style = MaterialTheme.typography.titleSmall, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

/** a dialog to add any public sticker set by its link or short name */
@Composable
fun AddStickerSetDialog(onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val name = stickerSetName(q)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить набор") },
        text = {
            Column {
                Text("Вставьте ссылку t.me/addstickers/… или имя набора", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(q, { q = it }, singleLine = true, placeholder = { Text("t.me/addstickers/…", maxLines = 1) }, shape = RoundedCornerShape(16.dp))
            }
        },
        confirmButton = { QTextButton(onClick = { name?.let { StickerSetPreview.openName(it) }; onDismiss() }, enabled = name != null) { Text("Открыть") } },
        dismissButton = { QTextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
