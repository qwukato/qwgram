package desu.inugram.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import desu.inugram.InuConfig
import desu.inugram.helpers.theme.MonetHelper
import desu.inugram.helpers.theme.SeedScheme
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.Theme
import kotlin.math.ceil
import kotlin.math.max

/**
 * Accent color picker row: a wrapping grid of swatches.
 * Order: [system wallpaper palette (Android 12+)] + presets + custom.
 * The selected swatch morphs from a circle to a rounded square on an M3 spatial spring
 * and gets an outline ring; color changes never bounce.
 */
@SuppressLint("ViewConstructor")
class SeedSwatchesCell(
    context: Context,
    private val onPick: (seed: Int) -> Unit,
    private val onCustom: () -> Unit,
) : View(context) {

    private enum class Kind { SYSTEM, PRESET, CUSTOM }

    private class Swatch(val kind: Kind, val color: Int) {
        val holder = FloatValueHolder(0f)
        var spring: SpringAnimation? = null
        var progress = 0f
        val rect = RectF()
    }

    private val swatches = ArrayList<Swatch>().apply {
        if (MonetHelper.isSystemPaletteAvailable()) add(Swatch(Kind.SYSTEM, 0))
        for (c in SeedScheme.PRESETS) add(Swatch(Kind.PRESET, c))
        add(Swatch(Kind.CUSTOM, 0))
    }

    private val size = AndroidUtilities.dp(44f).toFloat()
    private val gap = AndroidUtilities.dp(12f).toFloat()
    private val padH = AndroidUtilities.dp(21f).toFloat()
    private val padV = AndroidUtilities.dp(14f).toFloat()
    private val selectedRadius = AndroidUtilities.dp(14f).toFloat()

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = AndroidUtilities.dp(2.5f).toFloat()
    }
    private val plus = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = AndroidUtilities.dp(2f).toFloat()
    }
    private val clip = Path()
    private val tmp = RectF()
    private var downIndex = -1

    init {
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = LocaleController.getString(R.string.QwAccentColor)
        val sel = selectedIndex()
        for ((i, s) in swatches.withIndex()) {
            s.progress = if (i == sel) 1f else 0f
            s.holder.value = s.progress
        }
    }

    private fun selectedIndex(): Int {
        val seed = InuConfig.M3_SEED_COLOR.value
        if (seed == 0) {
            val sys = swatches.indexOfFirst { it.kind == Kind.SYSTEM }
            if (sys >= 0) return sys
        }
        val effective = MonetHelper.effectiveSeed()
        val preset = swatches.indexOfFirst { it.kind == Kind.PRESET && it.color == effective }
        if (preset >= 0) return preset
        return swatches.size - 1
    }

    /** call after the seed changed elsewhere (e.g. the custom color dialog) */
    fun sync() {
        val sel = selectedIndex()
        for ((i, s) in swatches.withIndex()) animateTo(s, if (i == sel) 1f else 0f)
        invalidate()
    }

    private fun animateTo(s: Swatch, target: Float) {
        if (s.progress == target && s.spring?.isRunning != true) return
        val spring = s.spring ?: SpringAnimation(s.holder).also { anim ->
            // M3 Expressive "fast spatial" spring: quick, with a small overshoot (shape only)
            anim.setSpring(SpringForce(target).setStiffness(800f).setDampingRatio(0.6f))
            anim.addUpdateListener { _, value, _ ->
                s.progress = value
                invalidate()
            }
            s.spring = anim
        }
        spring.animateToFinalPosition(target)
    }

    private fun columns(width: Int): Int =
        max(1, ((width - 2 * padH + gap) / (size + gap)).toInt())

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val cols = columns(w)
        val rows = ceil(swatches.size / cols.toFloat()).toInt()
        val h = (2 * padV + rows * size + (rows - 1) * gap).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val cols = columns(width)
        val rtl = LocaleController.isRTL
        for ((i, s) in swatches.withIndex()) {
            val col = i % cols
            val row = i / cols
            val x = if (rtl) width - padH - size - col * (size + gap) else padH + col * (size + gap)
            val y = padV + row * (size + gap)
            s.rect.set(x, y, x + size, y + size)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val ringColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)
        val iconColor = Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon)
        val emptyColor = Theme.getColor(Theme.key_windowBackgroundGray)
        val effectiveSeed = MonetHelper.effectiveSeed()
        for (s in swatches) {
            val p = s.progress
            val r = size / 2f + (selectedRadius - size / 2f) * p.coerceIn(-0.2f, 1.2f)
            val radius = r.coerceIn(AndroidUtilities.dp(6f).toFloat(), size / 2f)
            when (s.kind) {
                Kind.PRESET -> {
                    fill.color = s.color
                    canvas.drawRoundRect(s.rect, radius, radius, fill)
                }
                Kind.SYSTEM -> drawSystem(canvas, s.rect, radius)
                Kind.CUSTOM -> {
                    val chosen = p > 0.01f && SeedScheme.PRESETS.none { it == effectiveSeed } && InuConfig.M3_SEED_COLOR.value != 0
                    fill.color = if (chosen) effectiveSeed else emptyColor
                    canvas.drawRoundRect(s.rect, radius, radius, fill)
                    plus.color = if (chosen) contrastOn(effectiveSeed) else iconColor
                    val cx = s.rect.centerX()
                    val cy = s.rect.centerY()
                    val arm = AndroidUtilities.dp(7f).toFloat()
                    canvas.drawLine(cx - arm, cy, cx + arm, cy, plus)
                    canvas.drawLine(cx, cy - arm, cx, cy + arm, plus)
                }
            }
            if (p > 0.01f) {
                val inset = AndroidUtilities.dp(4.5f).toFloat()
                tmp.set(s.rect.left - inset, s.rect.top - inset, s.rect.right + inset, s.rect.bottom + inset)
                ring.color = ColorUtils.setAlphaComponent(ringColor, (255 * p.coerceIn(0f, 1f)).toInt())
                canvas.drawRoundRect(tmp, radius + inset, radius + inset, ring)
            }
        }
    }

    /** wallpaper palette preview: primary / tertiary halves, like the system color picker */
    private fun drawSystem(canvas: Canvas, rect: RectF, radius: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val primary = context.getColor(android.R.color.system_accent1_500)
        val tertiary = context.getColor(android.R.color.system_accent3_300)
        clip.reset()
        clip.addRoundRect(rect, radius, radius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clip)
        fill.color = primary
        canvas.drawRect(rect.left, rect.top, rect.right, rect.centerY(), fill)
        fill.color = tertiary
        canvas.drawRect(rect.left, rect.centerY(), rect.right, rect.bottom, fill)
        canvas.restore()
    }

    private fun contrastOn(color: Int): Int =
        if (ColorUtils.calculateLuminance(color) > 0.45) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()

    private fun hit(x: Float, y: Float): Int {
        val slop = gap / 2f
        for ((i, s) in swatches.withIndex()) {
            if (x >= s.rect.left - slop && x <= s.rect.right + slop && y >= s.rect.top - slop && y <= s.rect.bottom + slop) return i
        }
        return -1
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downIndex = hit(event.x, event.y)
                return downIndex >= 0
            }
            MotionEvent.ACTION_UP -> {
                val i = hit(event.x, event.y)
                if (i >= 0 && i == downIndex) select(i)
                downIndex = -1
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                downIndex = -1
                return true
            }
        }
        return downIndex >= 0
    }

    private fun select(i: Int) {
        val s = swatches[i]
        try {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {
        }
        if (s.kind == Kind.CUSTOM) {
            onCustom()
            return
        }
        for ((j, o) in swatches.withIndex()) animateTo(o, if (j == i) 1f else 0f)
        onPick(if (s.kind == Kind.SYSTEM) 0 else s.color)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        for (s in swatches) s.spring?.cancel()
    }
}
