package app.qwgram.next.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

/** the tag Compose uses for inline content in an AnnotatedString (same value as appendInlineContent) */
const val INLINE_TAG = "androidx.compose.foundation.text.inlineContent"

fun customEmojiIds(ft: TdApi.FormattedText): List<Long> =
    ft.entities.mapNotNull { (it.type as? TdApi.TextEntityTypeCustomEmoji)?.customEmojiId }.distinct()

/** inline slots for a text's premium emoji (they stand in for their fallback emoji, animated, like Telegram) */
@Composable
fun rememberEmojiInline(ft: TdApi.FormattedText, tint: Color): Map<String, InlineTextContent> {
    val ids = remember(ft) { customEmojiIds(ft) }
    return remember(ids, tint) {
        ids.associate { id ->
            "ce:$id" to InlineTextContent(Placeholder(1.2.em, 1.2.em, PlaceholderVerticalAlign.TextCenter)) {
                PremiumEmoji(id, tint, Modifier.fillMaxSize())
            }
        }
    }
}

/**
 * One premium (custom) emoji: plays like in Telegram (TGS / video / static); emoji marked "needs repainting"
 * (monochrome packs) take the colour of the text around them.
 */
@Composable
fun PremiumEmoji(id: Long, tint: Color, modifier: Modifier = Modifier) {
    LaunchedEffect(id) { CustomEmoji.request(id) }
    val s = CustomEmoji.stickers[id] ?: run { Box(modifier); return }
    val repaint = (s.fullType as? TdApi.StickerFullTypeCustomEmoji)?.needsRepainting == true
    val m = if (repaint) modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent { drawContent(); drawRect(tint, blendMode = BlendMode.SrcIn) } else modifier
    StickerContent(s, m)
}

@Composable
fun PremiumEmoji(id: Long, tint: Color, size: Dp) = PremiumEmoji(id, tint, Modifier.size(size))

/** a message that is only 1–3 premium emoji: shown big, without a bubble (Telegram) */
fun bigCustomEmoji(c: TdApi.MessageContent): List<Long>? {
    val t = (c as? TdApi.MessageText)?.text ?: return null
    if (t.entities.isEmpty() || t.entities.size > 3) return null
    if (t.entities.any { it.type !is TdApi.TextEntityTypeCustomEmoji }) return null
    // nothing but the emoji themselves (and spaces)
    var covered = 0
    t.entities.forEach { covered += it.length }
    if (t.text.replace(" ", "").length != covered) return null
    return t.entities.map { (it.type as TdApi.TextEntityTypeCustomEmoji).customEmojiId }
}

/**
 * The premium emoji typed into the input: kept as entities next to the plain text (the field shows their
 * fallback emoji) and moved along as the text is edited; an emoji whose characters are touched is dropped.
 */
class ComposerEmoji {
    val list = mutableStateListOf<TdApi.TextEntity>()

    fun add(offset: Int, length: Int, id: Long) { list.add(TdApi.TextEntity(offset, length, TdApi.TextEntityTypeCustomEmoji(id))) }

    fun set(entities: List<TdApi.TextEntity>) { list.clear(); list.addAll(entities.filter { it.type is TdApi.TextEntityTypeCustomEmoji }) }

    fun update(old: String, new: String) {
        if (list.isEmpty() || old == new) return
        val m = minOf(old.length, new.length)
        var p = 0
        while (p < m && old[p] == new[p]) p++
        var sfx = 0
        while (sfx < m - p && old[old.length - 1 - sfx] == new[new.length - 1 - sfx]) sfx++
        val oldEnd = old.length - sfx
        val delta = new.length - old.length
        val out = list.mapNotNull { e ->
            val s = e.offset
            val en = e.offset + e.length
            when {
                en <= p -> e
                s >= oldEnd -> TdApi.TextEntity(s + delta, e.length, e.type)
                else -> null
            }
        }
        list.clear(); list.addAll(out)
    }

    /** the entities for [full] trimmed to [t] (leading spaces removed) */
    fun forTrimmed(full: String, t: String): Array<TdApi.TextEntity> {
        val lead = full.length - full.trimStart().length
        return list.mapNotNull { e ->
            val o = e.offset - lead
            if (o < 0 || o + e.length > t.length) null else TdApi.TextEntity(o, e.length, e.type)
        }.toTypedArray()
    }
}

/** formatted text → (markdown text, premium emoji entities) for putting a message / draft back into the input */
fun toInput(f: TdApi.FormattedText): Pair<String, List<TdApi.TextEntity>> {
    if (f.entities.isEmpty()) return f.text to emptyList()
    if (!app.qwgram.next.Prefs.markdown.value) return f.text to f.entities.filter { it.type is TdApi.TextEntityTypeCustomEmoji }
    // getMarkdownText keeps what markdown can't spell (premium emoji) as entities, with fixed offsets
    val md = runCatching { Client.execute(TdApi.GetMarkdownText(f)) as? TdApi.FormattedText }.getOrNull() ?: return f.text to f.entities.filter { it.type is TdApi.TextEntityTypeCustomEmoji }
    return md.text to md.entities.filter { it.type is TdApi.TextEntityTypeCustomEmoji }
}

/** recently used premium emoji (ids), newest first */
object RecentCustomEmoji {
    private val sp by lazy { app.qwgram.next.App.instance.getSharedPreferences("recent_emoji", 0) }
    fun get(): List<Long> = sp.getString("custom", "")!!.split(',').mapNotNull { it.toLongOrNull() }
    fun add(id: Long) {
        val l = (listOf(id) + get().filter { it != id }).take(24)
        sp.edit().putString("custom", l.joinToString(",")).apply()
    }
}
