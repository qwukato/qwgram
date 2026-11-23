package app.qwgram.next.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import app.qwgram.next.Prefs

/** Material 3 Expressive shapes offered for avatars (key, title, polygon). */
object AvatarShapes {
    val all: List<Triple<String, String, RoundedPolygon>> by lazy {
        listOf(
            Triple("circle", "Круг", MaterialShapes.Circle),
            Triple("square", "Квадрат", MaterialShapes.Square),
            Triple("cookie4", "Печенье 4", MaterialShapes.Cookie4Sided),
            Triple("cookie6", "Печенье 6", MaterialShapes.Cookie6Sided),
            Triple("cookie7", "Печенье 7", MaterialShapes.Cookie7Sided),
            Triple("cookie9", "Печенье 9", MaterialShapes.Cookie9Sided),
            Triple("cookie12", "Печенье 12", MaterialShapes.Cookie12Sided),
            Triple("clover4", "Клевер 4", MaterialShapes.Clover4Leaf),
            Triple("clover8", "Клевер 8", MaterialShapes.Clover8Leaf),
            Triple("flower", "Цветок", MaterialShapes.Flower),
            Triple("sunny", "Солнце", MaterialShapes.Sunny),
            Triple("verysunny", "Яркое солнце", MaterialShapes.VerySunny),
            Triple("softburst", "Звезда", MaterialShapes.SoftBurst),
            Triple("burst", "Вспышка", MaterialShapes.Burst),
            Triple("softboom", "Взрыв", MaterialShapes.SoftBoom),
            Triple("puffy", "Облако", MaterialShapes.Puffy),
            Triple("puffydiamond", "Пухлый ромб", MaterialShapes.PuffyDiamond),
            Triple("pentagon", "Пятиугольник", MaterialShapes.Pentagon),
            Triple("gem", "Кристалл", MaterialShapes.Gem),
            Triple("diamond", "Ромб", MaterialShapes.Diamond),
            Triple("arch", "Арка", MaterialShapes.Arch),
            Triple("ghostish", "Призрак", MaterialShapes.Ghostish),
            Triple("bun", "Булочка", MaterialShapes.Bun),
            Triple("pixelcircle", "Пиксели", MaterialShapes.PixelCircle),
            Triple("heart", "Сердце", MaterialShapes.Heart),
            Triple("slanted", "Наклонный", MaterialShapes.Slanted),
        )
    }

    fun polygon(key: String): RoundedPolygon = all.firstOrNull { it.first == key }?.third ?: MaterialShapes.Circle
    fun title(key: String): String = all.firstOrNull { it.first == key }?.second ?: "Круг"
}

private val shapeCache = HashMap<String, Shape>()

fun shapeFor(key: String): Shape = if (key == "circle") CircleShape else shapeCache.getOrPut(key) { AvatarShapes.polygon(key).asShape() }

@Composable
fun avatarShape(): Shape {
    val key = Prefs.avatarShape.state.value
    return remember(key) { shapeFor(key) }
}

/** a shape that is a morph between two polygons at [progress] (spring-driven, may overshoot) */
class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val p = morph.toPath(progress).asComposePath()
        val b = p.getBounds()
        val m = Matrix()
        val sx = if (b.width > 0f) size.width / b.width else 1f
        val sy = if (b.height > 0f) size.height / b.height else 1f
        m.scale(sx, sy)
        m.translate(-b.left, -b.top)
        p.transform(m)
        return Outline.Generic(p)
    }
}

/** non-composable RoundedPolygon → Shape, scaled to fill the layout bounds */
class PolygonShape(private val polygon: RoundedPolygon) : Shape {
    private val base = polygon.toPath().asComposePath()
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val p = androidx.compose.ui.graphics.Path().apply { addPath(base) }
        val b = p.getBounds()
        val m = Matrix()
        m.scale(if (b.width > 0f) size.width / b.width else 1f, if (b.height > 0f) size.height / b.height else 1f)
        m.translate(-b.left, -b.top)
        p.transform(m)
        return Outline.Generic(p)
    }
}

fun RoundedPolygon.asShape(): Shape = PolygonShape(this)
