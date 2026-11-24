package app.qwgram.next.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.math.roundToInt

/** bounds of a layout on the physical screen (the same in the app window and in a full-screen dialog) */
fun LayoutCoordinates.screenRect(): Rect = Rect(positionOnScreen(), size.toSize())

/** the rect a picture of [aw]×[ah] takes when fitted (centred) into [w]×[h] */
fun fitRect(w: Float, h: Float, aw: Int, ah: Int): Rect {
    val a = aw.toFloat() / ah.coerceAtLeast(1)
    return if (w / h > a) { val fw = h * a; Rect((w - fw) / 2f, 0f, (w + fw) / 2f, h) }
    else { val fh = w / a; Rect(0f, (h - fh) / 2f, w, (h + fh) / 2f) }
}

/** [r] scaled by [s] around its centre and moved down by [dy] (the viewer's pull-to-close state) */
fun Rect.pulled(s: Float, dy: Float): Rect {
    val c = center
    val hw = width / 2f * s; val hh = height / 2f * s
    return Rect(c.x - hw, c.y - hh + dy, c.x + hw, c.y + hh + dy)
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/**
 * The picture that flies between its bubble and the viewer (Telegram's open/close): a real rect animation — the
 * frame moves and resizes from [from] to [to] while the picture inside is centre-cropped, so the bubble's crop
 * opens up into the whole photo; corners go from the bubble's rounding (or a circle) to square.
 */
@Composable
fun FlyingMedia(bmp: ImageBitmap, from: Rect, to: Rect, t: Float, circle: Boolean = false) {
    Box(
        Modifier
            .layout { m, c ->
                val l = lerp(from.left, to.left, t); val tp = lerp(from.top, to.top, t)
                val w = lerp(from.width, to.width, t).roundToInt().coerceAtLeast(1)
                val h = lerp(from.height, to.height, t).roundToInt().coerceAtLeast(1)
                val pl = m.measure(Constraints.fixed(w, h))
                layout(c.maxWidth, c.maxHeight) { pl.place(l.roundToInt(), tp.roundToInt()) }
            }
            .graphicsLayer {
                val k = (1f - t).coerceIn(0f, 1f)
                val r = if (circle) minOf(size.width, size.height) / 2f * k else 16.dp.toPx() * k
                shape = androidx.compose.foundation.shape.RoundedCornerShape(r)
                clip = true
            },
    ) { Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
}

@Suppress("unused") private val keep = Offset.Zero to Size.Zero
