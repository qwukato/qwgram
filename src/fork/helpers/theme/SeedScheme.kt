package desu.inugram.helpers.theme

import google_material.Hct

/**
 * Material 3 color scheme from any seed color, on any Android version.
 *
 * Port of the material-design-3 skill's generator (scripts/gen_theme.py): five palette variants,
 * the M3 tone map for light/dark roles and the three contrast levels. Tones are solved with
 * Google's HctSolver (google_material), so results match Material Color Utilities.
 *
 * Pure logic: no Android imports, unit-testable on the JVM.
 */
object SeedScheme {
    const val VARIANT_TONAL_SPOT = 0
    const val VARIANT_VIBRANT = 1
    const val VARIANT_EXPRESSIVE = 2
    const val VARIANT_NEUTRAL = 3
    const val VARIANT_MONOCHROME = 4

    const val CONTRAST_STANDARD = 0
    const val CONTRAST_MEDIUM = 1
    const val CONTRAST_HIGH = 2

    /** Default seed: the skill's baseline blue. */
    const val DEFAULT_SEED = 0xFF3D6CB5.toInt()

    /** Accent presets shown in settings (same set as the qwgram web client). */
    @JvmField
    val PRESETS = intArrayOf(
        0xFF3D6CB5.toInt(), 0xFF0B6B5E.toInt(), 0xFF3F6B2A.toInt(), 0xFF6B7A10.toInt(),
        0xFF8C5A00.toInt(), 0xFFB4502C.toInt(), 0xFFB3261E.toInt(), 0xFF984061.toInt(),
        0xFF6750A4.toInt(), 0xFF4A55A2.toInt(), 0xFF006874.toInt(), 0xFF5B6170.toInt(),
    )

    /** palette = (hue, chroma) */
    class Palette(@JvmField val hue: Double, @JvmField val chroma: Double) {
        private val cache = HashMap<Int, Int>()

        fun tone(tone: Double): Int {
            val key = (tone * 100).toInt()
            return cache.getOrPut(key) { Hct.from(hue, chroma, tone.coerceIn(0.0, 100.0)).toInt() }
        }
    }

    class Palettes(
        @JvmField val primary: Palette,
        @JvmField val secondary: Palette,
        @JvmField val tertiary: Palette,
        @JvmField val neutral: Palette,
        @JvmField val neutralVariant: Palette,
        @JvmField val error: Palette,
    ) {
        fun byName(name: String): Palette = when (name) {
            "primary", "a1" -> primary
            "secondary", "a2" -> secondary
            "tertiary", "a3" -> tertiary
            "neutral", "n1" -> neutral
            "neutral-variant", "n2" -> neutralVariant
            "error" -> error
            else -> throw IllegalArgumentException("unknown palette $name")
        }
    }

    private fun rotate(h: Double, thresholds: DoubleArray, turns: DoubleArray): Double {
        for (i in 0 until thresholds.size - 1) {
            if (thresholds[i] <= h && h < thresholds[i + 1]) return (h + turns[i]) % 360.0
        }
        return h
    }

    @JvmStatic
    fun palettes(seed: Int, variant: Int): Palettes {
        val h = Hct.fromInt(seed).hue
        val th = doubleArrayOf(0.0, 41.0, 61.0, 101.0, 131.0, 181.0, 251.0, 301.0, 360.0)
        val sh = doubleArrayOf(0.0, 21.0, 51.0, 121.0, 151.0, 191.0, 271.0, 321.0, 360.0)
        val tr = doubleArrayOf(18.0, 15.0, 10.0, 12.0, 15.0, 18.0, 15.0, 12.0, 12.0)
        val spec: Array<DoubleArray> = when (variant) {
            VARIANT_VIBRANT -> arrayOf(d(h, 200.0), d(rotate(h, sh, tr), 24.0), d(rotate(h, th, tr), 32.0), d(h, 10.0), d(h, 12.0))
            VARIANT_EXPRESSIVE -> arrayOf(d((h + 240) % 360, 40.0), d((h + 15) % 360, 24.0), d((h + 75) % 360, 32.0), d(h, 8.0), d(h, 12.0))
            VARIANT_NEUTRAL -> arrayOf(d(h, 12.0), d(h, 8.0), d((h + 60) % 360, 16.0), d(h, 2.0), d(h, 2.4))
            VARIANT_MONOCHROME -> arrayOf(d(h, 0.0), d(h, 0.0), d(h, 0.0), d(h, 0.0), d(h, 0.0))
            else -> arrayOf(d(h, 36.0), d(h, 16.0), d((h + 60) % 360, 24.0), d(h, 6.0), d(h, 8.0))
        }
        val p = spec.map { Palette(it[0], it[1]) }
        return Palettes(p[0], p[1], p[2], p[3], p[4], Palette(25.0, 84.0))
    }

    private fun d(a: Double, b: Double) = doubleArrayOf(a, b)

    /** Android system palette shade (0, 10, 50, 100 … 1000) → M3 tone. */
    @JvmStatic
    fun shadeToTone(shade: Int): Double = when (shade) {
        0 -> 100.0
        10 -> 99.0
        50 -> 95.0
        else -> 100.0 - shade / 10.0
    }

    // contrast tone tables: role -> [light, dark][standard, medium, high]
    private val TT = mapOf(
        "main" to arrayOf(intArrayOf(40, 30, 20), intArrayOf(80, 85, 95)),
        "on_main" to arrayOf(intArrayOf(100, 100, 100), intArrayOf(20, 15, 10)),
        "cont" to arrayOf(intArrayOf(90, 90, 92), intArrayOf(30, 25, 18)),
        "on_cont" to arrayOf(intArrayOf(10, 5, 2), intArrayOf(90, 95, 99)),
        "on_surface" to arrayOf(intArrayOf(10, 5, 0), intArrayOf(90, 95, 100)),
        "on_var" to arrayOf(intArrayOf(30, 20, 10), intArrayOf(80, 88, 95)),
        "outline" to arrayOf(intArrayOf(50, 35, 20), intArrayOf(60, 72, 85)),
        "outline_var" to arrayOf(intArrayOf(80, 60, 45), intArrayOf(30, 45, 60)),
    )

    /**
     * Tone of an M3 role (names as in the skill / Android: "primary", "on-primary-container",
     * "surface-container-high", …), or null for an unknown role.
     */
    @JvmStatic
    fun roleTone(role: String, dark: Boolean, contrast: Int): Pair<String, Double>? {
        val lv = contrast.coerceIn(0, 2)
        val i = if (dark) 1 else 0
        fun tn(k: String) = TT.getValue(k)[i][lv].toDouble()
        for (n in arrayOf("primary", "secondary", "tertiary", "error")) {
            when (role) {
                n -> return n to if (lv > 0) tn("main") else if (dark) 80.0 else 40.0
                "on-$n" -> return n to if (lv > 0) tn("on_main") else if (dark) 20.0 else 100.0
                "$n-container" -> return n to if (lv > 0) tn("cont") else if (dark) 30.0 else 90.0
                "on-$n-container" -> return n to if (lv > 0) tn("on_cont") else if (dark) 90.0 else 10.0
            }
            if (n != "error") when (role) {
                "$n-fixed" -> return n to 90.0
                "$n-fixed-dim" -> return n to 80.0
                "on-$n-fixed" -> return n to 10.0
                "on-$n-fixed-variant" -> return n to 30.0
            }
        }
        val nn = "neutral"
        val nv = "neutral-variant"
        return when (role) {
            "surface", "background" -> nn to if (dark) 6.0 else 98.0
            "surface-dim" -> nn to if (dark) 6.0 else 87.0
            "surface-bright" -> nn to if (dark) 24.0 else 98.0
            "surface-container-lowest" -> nn to if (dark) 4.0 else 100.0
            "surface-container-low" -> nn to if (dark) 10.0 else 96.0
            "surface-container" -> nn to if (dark) 12.0 else 94.0
            "surface-container-high" -> nn to if (dark) 17.0 else 92.0
            "surface-container-highest" -> nn to if (dark) 22.0 else 90.0
            "on-surface", "on-background" -> nn to if (lv > 0) tn("on_surface") else if (dark) 90.0 else 10.0
            "on-surface-variant" -> nv to if (lv > 0) tn("on_var") else if (dark) 80.0 else 30.0
            "outline" -> nv to if (lv > 0) tn("outline") else if (dark) 60.0 else 50.0
            "outline-variant" -> nv to if (lv > 0) tn("outline_var") else if (dark) 30.0 else 80.0
            "surface-variant" -> nv to if (dark) 30.0 else 90.0
            "inverse-surface" -> nn to if (dark) 90.0 else 20.0
            "inverse-on-surface" -> nn to if (dark) 20.0 else 95.0
            "inverse-primary" -> "primary" to if (dark) 40.0 else 80.0
            "surface-tint" -> roleTone("primary", dark, contrast)
            else -> null
        }
    }

    @JvmStatic
    fun role(p: Palettes, role: String, dark: Boolean, contrast: Int): Int? {
        val (pal, tone) = roleTone(role, dark, contrast) ?: return null
        return p.byName(pal).tone(tone)
    }
}
