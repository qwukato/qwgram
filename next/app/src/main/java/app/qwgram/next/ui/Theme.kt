package app.qwgram.next.ui

import android.os.Build
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.qwgram.next.Prefs

val LocalDark = compositionLocalOf { false }

fun seedColorScheme(seed: Int, variant: Int, dark: Boolean, contrast: Int, amoled: Boolean): ColorScheme {
    val p = SeedScheme.palettes(seed, variant)
    fun r(role: String) = Color(SeedScheme.role(p, role, dark, contrast) ?: 0xFFFF00FF.toInt())
    val n = p.neutral
    val base = if (dark) darkColorScheme(
        primary = r("primary"), onPrimary = r("on-primary"), primaryContainer = r("primary-container"), onPrimaryContainer = r("on-primary-container"),
        inversePrimary = r("inverse-primary"), secondary = r("secondary"), onSecondary = r("on-secondary"),
        secondaryContainer = r("secondary-container"), onSecondaryContainer = r("on-secondary-container"),
        tertiary = r("tertiary"), onTertiary = r("on-tertiary"), tertiaryContainer = r("tertiary-container"), onTertiaryContainer = r("on-tertiary-container"),
        background = r("background"), onBackground = r("on-background"), surface = r("surface"), onSurface = r("on-surface"),
        surfaceVariant = r("surface-variant"), onSurfaceVariant = r("on-surface-variant"), surfaceTint = r("primary"),
        inverseSurface = r("inverse-surface"), inverseOnSurface = r("inverse-on-surface"), error = r("error"), onError = r("on-error"),
        errorContainer = r("error-container"), onErrorContainer = r("on-error-container"), outline = r("outline"), outlineVariant = r("outline-variant"),
        surfaceBright = r("surface-bright"), surfaceDim = r("surface-dim"), surfaceContainer = r("surface-container"),
        surfaceContainerHigh = r("surface-container-high"), surfaceContainerHighest = r("surface-container-highest"),
        surfaceContainerLow = r("surface-container-low"), surfaceContainerLowest = r("surface-container-lowest"),
    ) else lightColorScheme(
        primary = r("primary"), onPrimary = r("on-primary"), primaryContainer = r("primary-container"), onPrimaryContainer = r("on-primary-container"),
        inversePrimary = r("inverse-primary"), secondary = r("secondary"), onSecondary = r("on-secondary"),
        secondaryContainer = r("secondary-container"), onSecondaryContainer = r("on-secondary-container"),
        tertiary = r("tertiary"), onTertiary = r("on-tertiary"), tertiaryContainer = r("tertiary-container"), onTertiaryContainer = r("on-tertiary-container"),
        background = r("background"), onBackground = r("on-background"), surface = r("surface"), onSurface = r("on-surface"),
        surfaceVariant = r("surface-variant"), onSurfaceVariant = r("on-surface-variant"), surfaceTint = r("primary"),
        inverseSurface = r("inverse-surface"), inverseOnSurface = r("inverse-on-surface"), error = r("error"), onError = r("on-error"),
        errorContainer = r("error-container"), onErrorContainer = r("on-error-container"), outline = r("outline"), outlineVariant = r("outline-variant"),
        surfaceBright = r("surface-bright"), surfaceDim = r("surface-dim"), surfaceContainer = r("surface-container"),
        surfaceContainerHigh = r("surface-container-high"), surfaceContainerHighest = r("surface-container-highest"),
        surfaceContainerLow = r("surface-container-low"), surfaceContainerLowest = r("surface-container-lowest"),
    )
    return if (dark && amoled) base.amoled(n) else base
}

private fun ColorScheme.amoled(n: SeedScheme.Palette): ColorScheme = copy(
    background = Color.Black, surface = Color.Black, surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(n.tone(4.0)), surfaceContainer = Color(n.tone(6.0)),
    surfaceContainerHigh = Color(n.tone(10.0)), surfaceContainerHighest = Color(n.tone(15.0)),
)

/**
 * Material 3 Expressive motion with a speed multiplier. Spring values are the M3 tokens
 * (expressive: spatial 800/0.6, 380/0.8, 200/0.8; standard: 1400/0.9, 700/0.9, 300/0.9;
 * effects 3800/1, 1600/1, 800/1). Speed s scales stiffness by s² so durations scale by 1/s.
 */
class QwMotion(private val expressive: Boolean, private val speed: Float, private val off: Boolean) : MotionScheme {
    private fun <T> sp(damping: Float, stiffness: Float): FiniteAnimationSpec<T> =
        if (off || speed <= 0.05f) snap() else spring(dampingRatio = damping, stiffness = stiffness * speed * speed)

    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = if (expressive) sp(0.8f, 380f) else sp(0.9f, 700f)
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = if (expressive) sp(0.6f, 800f) else sp(0.9f, 1400f)
    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = if (expressive) sp(0.8f, 200f) else sp(0.9f, 300f)
    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = sp(1f, 1600f)
    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = sp(1f, 3800f)
    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = sp(1f, 800f)
}

@Composable
fun isAppDark(): Boolean = when (Prefs.themeMode.state.value) {
    "dark" -> true
    "light" -> false
    else -> isSystemInDarkTheme()
}

@Composable
fun QwTheme(content: @Composable () -> Unit) {
    // energy saving can force a dark / pure-black look (less power on OLED screens)
    val dark = isAppDark() || (app.qwgram.next.Power.on && Prefs.psDark.value)
    val ctx = LocalContext.current
    val dynamic = Prefs.dynamicColor.state.value && Build.VERSION.SDK_INT >= 31
    val seed = Prefs.seed.state.value
    val variant = Prefs.paletteStyle.state.value
    val contrast = Prefs.contrast.state.value
    val amoled = Prefs.amoled.state.value || (app.qwgram.next.Power.on && Prefs.psAmoled.value)
    val scheme = remember(dark, dynamic, seed, variant, contrast, amoled) {
        if (dynamic) {
            val s = if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            if (dark && amoled) s.copy(background = Color.Black, surface = Color.Black, surfaceContainerLowest = Color.Black) else s
        } else seedColorScheme(seed, variant, dark, contrast, amoled)
    }
    val motionPref = Prefs.motion.state.value
    val speed = Prefs.motionSpeed.state.value
    val motion = remember(motionPref, speed) { QwMotion(motionPref == "expressive", speed, motionPref == "off") }
    val round = Prefs.cornerScale.state.value
    val shapes = remember(round) {
        val k = round
        Shapes(
            extraSmall = RoundedCornerShape((4 * k).dp), small = RoundedCornerShape((8 * k).dp),
            medium = RoundedCornerShape((12 * k).dp), large = RoundedCornerShape((16 * k).dp),
            extraLarge = RoundedCornerShape((28 * k).dp),
        )
    }
    val fontPref = Prefs.font.state.value
    val emojiPref = Prefs.emojiStyle.state.value
    val bodyW = Prefs.bodyWeight.state.value
    val headW = Prefs.headWeight.state.value
    val typography = remember(fontPref, emojiPref, bodyW, headW) {
        val family = when (fontPref) {
            "serif" -> FontFamily.Serif
            "mono" -> FontFamily.Monospace
            else -> Fonts.family(ctx, fontPref, emojiPref) ?: FontFamily.Default
        }
        val body = FontWeight(bodyW.coerceIn(100, 900))
        // headers / titles: heavy by default (M3 Expressive emphasised type); smaller titles a step lighter
        val head = FontWeight(headW.coerceIn(100, 900))
        val title = FontWeight((headW - 100).coerceIn(bodyW, 900))
        val label = FontWeight((headW - 200).coerceIn(bodyW, 900))
        val t = Typography()
        fun androidx.compose.ui.text.TextStyle.f(w: FontWeight?) = copy(fontFamily = family, fontWeight = w ?: fontWeight)
        Typography(
            displayLarge = t.displayLarge.f(head), displayMedium = t.displayMedium.f(head), displaySmall = t.displaySmall.f(head),
            headlineLarge = t.headlineLarge.f(head), headlineMedium = t.headlineMedium.f(head), headlineSmall = t.headlineSmall.f(head),
            titleLarge = t.titleLarge.f(head), titleMedium = t.titleMedium.f(title), titleSmall = t.titleSmall.f(title),
            bodyLarge = t.bodyLarge.f(body), bodyMedium = t.bodyMedium.f(body), bodySmall = t.bodySmall.f(body),
            labelLarge = t.labelLarge.f(label), labelMedium = t.labelMedium.f(label), labelSmall = t.labelSmall.f(null),
        )
    }
    val scale = Prefs.uiScale.state.value
    val density = LocalDensity.current
    val scaled = remember(density, scale) { Density(density.density * scale, density.fontScale) }
    CompositionLocalProvider(LocalDark provides dark, LocalDensity provides scaled) {
        MaterialExpressiveTheme(colorScheme = scheme, motionScheme = motion, shapes = shapes, typography = typography, content = content)
    }
}
