package desu.inugram.helpers.dialogs

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import androidx.core.graphics.ColorUtils
import desu.inugram.InuConfig
import desu.inugram.helpers.theme.NonIslandHelper
import org.telegram.messenger.AndroidUtilities.dp

/**
 * Material 3 Expressive folder bar: every folder is a segment of a connected button group
 * (2dp gaps, small inner corners, round outer ends); the selected folder is a fully rounded
 * tonal pill that slides between segments with the existing indicator animation.
 */
object QwFolderStyle {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()
    private val radii = FloatArray(8)

    @JvmStatic
    fun enabled(): Boolean = InuConfig.EXPRESSIVE_FOLDERS.value && !NonIslandHelper.foldersBar()

    @JvmStatic
    fun halfHeight(): Int = dp(16f)

    @JvmStatic
    fun drawSegment(canvas: Canvas, view: View, position: Int, count: Int, accent: Int) {
        val h = view.measuredHeight
        val w = view.measuredWidth
        if (w <= 0 || h <= 0) return
        val half = halfHeight().toFloat()
        rect.set(dp(1f).toFloat(), h / 2f - half, w - dp(1f).toFloat(), h / 2f + half)
        val outer = half
        val inner = dp(6f).toFloat()
        val l = if (position <= 0) outer else inner
        val r = if (position >= count - 1) outer else inner
        radii[0] = l; radii[1] = l
        radii[2] = r; radii[3] = r
        radii[4] = r; radii[5] = r
        radii[6] = l; radii[7] = l
        path.rewind()
        path.addRoundRect(rect, radii, Path.Direction.CW)
        paint.color = ColorUtils.setAlphaComponent(accent, 24)
        canvas.drawPath(path, paint)
    }
}
