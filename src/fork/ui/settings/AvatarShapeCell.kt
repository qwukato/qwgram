package desu.inugram.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.TextPaint
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import desu.inugram.InuConfig
import desu.inugram.helpers.theme.AvatarShapes
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.Theme
import kotlin.math.ceil
import kotlin.math.max

/**
 * Avatar shape picker: a large preview avatar that morphs between shapes on an M3 spatial spring,
 * and a grid of all shapes. The selected tile gets a tonal rounded-square backdrop.
 */
@SuppressLint("ViewConstructor")
class AvatarShapeCell(
    context: Context,
    private val onPick: (key: String) -> Unit,
) : View(context) {

    private val keys = AvatarShapes.KEYS
    private val tile = AndroidUtilities.dp(44f).toFloat()
    private val gap = AndroidUtilities.dp(8f).toFloat()
    private val padH = AndroidUtilities.dp(21f).toFloat()
    private val padV = AndroidUtilities.dp(12f).toFloat()
    private val preview = AndroidUtilities.dp(72f).toFloat()
    private val previewBlock = preview + AndroidUtilities.dp(20f)

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = AndroidUtilities.dp(28f).toFloat()
        typeface = AndroidUtilities.bold()
        textAlign = Paint.Align.CENTER
    }
    private val path = Path()
    private val rect = RectF()
    private val rects = Array(keys.size) { RectF() }

    // morph state of the preview: from -> to, progress 0..1 (spring, may overshoot)
    private var fromPts: FloatArray = AvatarShapes.points(InuConfig.AVATAR_SHAPE.value) ?: AvatarShapes.points(AvatarShapes.CIRCLE)!!
    private var toPts: FloatArray = fromPts
    private val morphPts = FloatArray(360)
    private var progress = 1f
    private val holder = FloatValueHolder(1f)
    private var spring: SpringAnimation? = null
    private var downIndex = -1

    init {
        isClickable = true
        contentDescription = LocaleController.getString(R.string.QwAvatarShape)
    }

    private fun columns(width: Int): Int = max(1, ((width - 2 * padH + gap) / (tile + gap)).toInt())

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val cols = columns(w)
        val rows = ceil(keys.size / cols.toFloat()).toInt()
        setMeasuredDimension(w, (padV * 2 + previewBlock + rows * tile + (rows - 1) * gap).toInt())
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val cols = columns(width)
        val used = cols * tile + (cols - 1) * gap
        val startX = (width - used) / 2f
        val rtl = LocaleController.isRTL
        for (i in keys.indices) {
            val col = i % cols
            val row = i / cols
            val x = if (rtl) width - startX - tile - col * (tile + gap) else startX + col * (tile + gap)
            val y = padV + previewBlock + row * (tile + gap)
            rects[i].set(x, y, x + tile, y + tile)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val selected = InuConfig.AVATAR_SHAPE.value
        // preview avatar
        AvatarShapes.morph(fromPts, toPts, progress, morphPts)
        val cx = width / 2f
        rect.set(cx - preview / 2f, padV, cx + preview / 2f, padV + preview)
        AvatarShapes.buildPath(path, morphPts, rect.left, rect.top, preview, preview)
        fill.color = Theme.getColor(Theme.key_avatar_backgroundBlue)
        canvas.drawPath(path, fill)
        text.color = Theme.getColor(Theme.key_avatar_text)
        canvas.drawText("Q", cx, rect.centerY() - (text.descent() + text.ascent()) / 2f, text)

        val tonal = Theme.getColor(Theme.key_featuredStickers_addButton)
        val idle = Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon)
        for ((i, key) in keys.withIndex()) {
            val r = rects[i]
            val isSel = key == selected
            if (isSel) {
                fill.color = androidx.core.graphics.ColorUtils.setAlphaComponent(tonal, 46)
                val inset = AndroidUtilities.dp(2f).toFloat()
                rect.set(r.left - inset, r.top - inset, r.right + inset, r.bottom + inset)
                canvas.drawRoundRect(rect, AndroidUtilities.dp(14f).toFloat(), AndroidUtilities.dp(14f).toFloat(), fill)
            }
            val pts = AvatarShapes.points(key) ?: continue
            val pad = AndroidUtilities.dp(7f).toFloat()
            AvatarShapes.buildPath(path, pts, r.left + pad, r.top + pad, r.width() - pad * 2, r.height() - pad * 2)
            fill.color = if (isSel) tonal else androidx.core.graphics.ColorUtils.setAlphaComponent(idle, 140)
            canvas.drawPath(path, fill)
        }
    }

    private fun hit(x: Float, y: Float): Int {
        val slop = gap / 2f
        for ((i, r) in rects.withIndex()) {
            if (x >= r.left - slop && x <= r.right + slop && y >= r.top - slop && y <= r.bottom + slop) return i
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
                if (i >= 0 && i == downIndex) select(keys[i])
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

    private fun select(key: String) {
        if (key == InuConfig.AVATAR_SHAPE.value) return
        try {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {
        }
        // start the morph from whatever is on screen right now
        AvatarShapes.morph(fromPts, toPts, progress, morphPts)
        fromPts = morphPts.copyOf()
        toPts = AvatarShapes.points(key) ?: return
        progress = 0f
        holder.value = 0f
        val anim = spring ?: SpringAnimation(holder).also { a ->
            // M3 Expressive "default spatial" spring: 380 / 0.8, slight overshoot on the shape
            a.setSpring(SpringForce(1f).setStiffness(380f).setDampingRatio(0.8f))
            a.addUpdateListener { _, value, _ ->
                progress = value
                invalidate()
            }
            spring = a
        }
        anim.cancel()
        holder.value = 0f
        anim.setStartValue(0f)
        anim.animateToFinalPosition(1f)
        onPick(key)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        spring?.cancel()
    }
}
