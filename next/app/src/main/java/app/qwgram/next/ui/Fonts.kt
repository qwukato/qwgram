package app.qwgram.next.ui

import android.content.Context
import android.os.Build
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import java.nio.ByteBuffer

/**
 * Bundled fonts (downloaded into assets at build time):
 *  - fonts/gsflex.ttf  — Google Sans Flex (variable, OFL), the Google UI font
 *  - fonts/twemoji.ttf — Twemoji Mozilla (COLR, CC-BY 4.0) used as the emoji fallback of that font
 * Anything missing simply falls back to the system font / system emoji.
 */
object Fonts {
    private const val SANS = "fonts/gsflex.ttf"
    private const val EMOJI = "fonts/twemoji.ttf"
    private const val BLOB = "fonts/blobmoji.ttf"
    private const val CYR = "fonts/robotoflex.ttf"
    private val WEIGHTS = intArrayOf(300, 400, 500, 600, 700, 800, 900)
    private val cache = HashMap<String, FontFamily?>()

    private fun has(ctx: Context, path: String) = runCatching { ctx.assets.open(path).close(); true }.getOrDefault(false)

    private fun read(ctx: Context, path: String): ByteBuffer? = runCatching {
        val bytes = ctx.assets.open(path).use { it.readBytes() }
        ByteBuffer.allocateDirect(bytes.size).put(bytes).also { it.flip() }
    }.getOrNull()

    /** twemoji colour glyphs render on Android 13+ (COLR support in the system renderer) */
    fun twemojiAvailable(ctx: Context) = Build.VERSION.SDK_INT >= 33 && has(ctx, EMOJI)
    fun googleAvailable(ctx: Context) = has(ctx, SANS)
    /** Blobmoji is CBDT (colour bitmaps): any Android with custom font fallbacks (10+) */
    fun blobAvailable(ctx: Context) = Build.VERSION.SDK_INT >= 29 && has(ctx, BLOB)

    /** the bundled emoji font for a style, as a plain typeface (previews draw with it directly) */
    fun emojiTypeface(ctx: Context, style: String): android.graphics.Typeface? = runCatching {
        when (style) {
            "twemoji" -> if (twemojiAvailable(ctx)) android.graphics.Typeface.createFromAsset(ctx.assets, EMOJI) else null
            "blob" -> if (has(ctx, BLOB)) android.graphics.Typeface.createFromAsset(ctx.assets, BLOB) else null
            else -> null
        }
    }.getOrNull()

    /** family for the chosen font + emoji style; null = keep the platform default */
    @OptIn(ExperimentalTextApi::class)
    fun family(ctx: Context, font: String, emoji: String): FontFamily? {
        val key = "$font/$emoji"
        if (cache.containsKey(key)) return cache[key]
        val f = runCatching { build(ctx, font, emoji) }.getOrNull()
        cache[key] = f
        return f
    }

    @OptIn(ExperimentalTextApi::class)
    private fun build(ctx: Context, font: String, emoji: String): FontFamily? {
        val wantTwemoji = emoji == "twemoji" && twemojiAvailable(ctx)
        val wantBlob = emoji == "blob" && blobAvailable(ctx)
        val google = font == "google" && googleAvailable(ctx)
        if (Build.VERSION.SDK_INT >= 29 && (google || ((wantTwemoji || wantBlob) && font == "system"))) {
            val mainBuf: java.nio.ByteBuffer? = if (google) read(ctx, SANS) else null
            val sysFile = java.io.File("/system/fonts/Roboto-Regular.ttf")
            if (!google && !sysFile.exists()) return null
            val cyrBuf = if (google) read(ctx, CYR) else null
            val emojiBuf = when { wantTwemoji -> read(ctx, EMOJI); wantBlob -> read(ctx, BLOB); else -> null }
            fun at(w: Int, buf: java.nio.ByteBuffer?, file: java.io.File?): android.graphics.fonts.Font =
                (if (buf != null) android.graphics.fonts.Font.Builder(buf.duplicate()) else android.graphics.fonts.Font.Builder(file!!))
                    .setWeight(w).setFontVariationSettings("'wght' $w").build()
            // one platform typeface PER WEIGHT (Compose picks the font by weight; a single typeface would ignore it):
            // the main font at that weight → Roboto Flex at the same weight for Cyrillic & co (Google Sans Flex is
            // Latin-only) → Twemoji → the system fallback
            val fonts = WEIGHTS.map { w ->
                val main = android.graphics.fonts.FontFamily.Builder(at(w, mainBuf, if (google) null else sysFile)).build()
                val b = android.graphics.Typeface.CustomFallbackBuilder(main).setSystemFallback("sans-serif")
                    .setStyle(android.graphics.fonts.FontStyle(w, android.graphics.fonts.FontStyle.FONT_SLANT_UPRIGHT))
                if (cyrBuf != null) b.addCustomFallback(android.graphics.fonts.FontFamily.Builder(at(w, cyrBuf, null)).build())
                if (emojiBuf != null) b.addCustomFallback(android.graphics.fonts.FontFamily.Builder(android.graphics.fonts.Font.Builder(emojiBuf.duplicate()).build()).build())
                TypefaceFont(b.build(), FontWeight(w))
            }
            return FontFamily(fonts)
        }
        if (google) {
            // Android 8–9: the variable font through Compose, without the custom emoji fallback
            return FontFamily(WEIGHTS.map { w -> Font(SANS, ctx.assets, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w))) })
        }
        return null
    }
}

/** a ready platform typeface presented to Compose as a font of a given weight */
@OptIn(ExperimentalTextApi::class)
private class TypefaceFont(val tf: android.graphics.Typeface, override val weight: FontWeight) :
    androidx.compose.ui.text.font.AndroidFont(androidx.compose.ui.text.font.FontLoadingStrategy.Blocking, Loader, FontVariation.Settings()) {
    override val style: androidx.compose.ui.text.font.FontStyle = androidx.compose.ui.text.font.FontStyle.Normal
    private object Loader : androidx.compose.ui.text.font.AndroidFont.TypefaceLoader {
        override fun loadBlocking(context: Context, font: androidx.compose.ui.text.font.AndroidFont): android.graphics.Typeface? = (font as? TypefaceFont)?.tf
        override suspend fun awaitLoad(context: Context, font: androidx.compose.ui.text.font.AndroidFont): android.graphics.Typeface? = (font as? TypefaceFont)?.tf
    }
}
