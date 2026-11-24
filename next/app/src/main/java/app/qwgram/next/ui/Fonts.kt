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
    private val WEIGHTS = intArrayOf(300, 400, 500, 600, 700, 800, 900)
    private var cache: Pair<String, FontFamily?>? = null

    private fun has(ctx: Context, path: String) = runCatching { ctx.assets.open(path).close(); true }.getOrDefault(false)

    private fun read(ctx: Context, path: String): ByteBuffer? = runCatching {
        val bytes = ctx.assets.open(path).use { it.readBytes() }
        ByteBuffer.allocateDirect(bytes.size).put(bytes).also { it.flip() }
    }.getOrNull()

    /** twemoji colour glyphs render on Android 13+ (COLR support in the system renderer) */
    fun twemojiAvailable(ctx: Context) = Build.VERSION.SDK_INT >= 33 && has(ctx, EMOJI)
    fun googleAvailable(ctx: Context) = has(ctx, SANS)

    /** family for the chosen font + emoji style; null = keep the platform default */
    @OptIn(ExperimentalTextApi::class)
    fun family(ctx: Context, font: String, emoji: String): FontFamily? {
        val key = "$font/$emoji"
        cache?.let { if (it.first == key) return it.second }
        val f = runCatching { build(ctx, font, emoji) }.getOrNull()
        cache = key to f
        return f
    }

    @OptIn(ExperimentalTextApi::class)
    private fun build(ctx: Context, font: String, emoji: String): FontFamily? {
        val wantTwemoji = emoji == "twemoji" && twemojiAvailable(ctx)
        val google = font == "google" && googleAvailable(ctx)
        if (Build.VERSION.SDK_INT >= 29 && (google || (wantTwemoji && font == "system"))) {
            // one platform typeface: every weight of the (variable) main font, then Twemoji, then the system fallback
            val fonts: List<android.graphics.fonts.Font> = if (google) {
                val buf = read(ctx, SANS) ?: return null
                WEIGHTS.map { w -> android.graphics.fonts.Font.Builder(buf.duplicate()).setWeight(w).setFontVariationSettings("'wght' $w").build() }
            } else {
                // system font + Twemoji: the platform Roboto (variable on Android 12+)
                val f = java.io.File("/system/fonts/Roboto-Regular.ttf")
                if (!f.exists()) return null
                WEIGHTS.map { w -> android.graphics.fonts.Font.Builder(f).setWeight(w).setFontVariationSettings("'wght' $w").build() }
            }
            val fam = android.graphics.fonts.FontFamily.Builder(fonts.first()).also { b -> fonts.drop(1).forEach { b.addFont(it) } }.build()
            val builder = android.graphics.Typeface.CustomFallbackBuilder(fam).setSystemFallback("sans-serif")
            if (wantTwemoji) read(ctx, EMOJI)?.let { e ->
                builder.addCustomFallback(android.graphics.fonts.FontFamily.Builder(android.graphics.fonts.Font.Builder(e).build()).build())
            }
            return FontFamily(androidx.compose.ui.text.font.Typeface(builder.build()))
        }
        if (google) {
            // Android 8–9: the variable font through Compose, without the custom emoji fallback
            return FontFamily(WEIGHTS.map { w -> Font(SANS, ctx.assets, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w))) })
        }
        return null
    }
}
