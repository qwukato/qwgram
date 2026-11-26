package app.qwgram.next.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Where a tile sits in a connected group (M3 connected / segmented layouts): rows of tiles 2 dp apart, the group's
 * outer corners big, every corner that touches a neighbour small. Rows may hold different numbers of tiles
 * (one wide tile over three, 2×2, four in a row…). [groupTop]/[groupBottom] = false when the group continues
 * above / below with other pieces (a header card, a button).
 */
@Immutable
data class Seat(val row: Int, val rows: Int, val col: Int, val cols: Int, val groupTop: Boolean = true, val groupBottom: Boolean = true) {
    val topStart get() = row == 0 && groupTop && col == 0
    val topEnd get() = row == 0 && groupTop && col == cols - 1
    val bottomStart get() = row == rows - 1 && groupBottom && col == 0
    val bottomEnd get() = row == rows - 1 && groupBottom && col == cols - 1

    companion object {
        /** a single full-width piece of a vertical group: [i] of [n] */
        fun stacked(i: Int, n: Int) = Seat(i, n, 0, 1)
    }
}

/** the static shape of a seat (for things that aren't tiles: images, previews inside a group) */
@Composable
fun Seat.shape(big: Dp = 24.dp, small: Dp = 6.dp): RoundedCornerShape {
    val b = big.rs(); val s = small.rs()
    return RoundedCornerShape(if (topStart) b else s, if (topEnd) b else s, if (bottomEnd) b else s, if (bottomStart) b else s)
}

/**
 * One tile of a connected group. Pressed, all its corners ease to one middle radius and it squishes a little
 * (the M3 Expressive press); [selected] tints it. Works with the app's corner scale (Appearance → corners).
 */
@Composable
fun ConnectedTile(
    seat: Seat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    selected: Boolean = false,
    selectedColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    selectedContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    big: Dp = 24.dp,
    small: Dp = 6.dp,
    onLongClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val b = big.rs(); val s = small.rs(); val mid = ((big + small) / 2).rs()
    val spec = Springs.fastSpatial<Dp>()
    // selected tiles round a bit more (the M3 "on" state), pressed ones all meet in the middle
    fun target(outer: Boolean) = when { pressed -> mid; selected -> if (outer) b else (s + 6.dp); outer -> b; else -> s }
    val ts by animateDpAsState(target(seat.topStart), spec, label = "cts")
    val te by animateDpAsState(target(seat.topEnd), spec, label = "cte")
    val be by animateDpAsState(target(seat.bottomEnd), spec, label = "cbe")
    val bs by animateDpAsState(target(seat.bottomStart), spec, label = "cbs")
    val sq by animateFloatAsState(if (pressed) 0.965f else 1f, Springs.fastSpatial(), label = "csq")
    val bg by animateColorAsState(if (selected) selectedColor else color, Springs.effects(), label = "cbg")
    val fg by animateColorAsState(if (selected) selectedContentColor else contentColor, Springs.effects(), label = "cfg")
    val shape = RoundedCornerShape(ts, te, be, bs)
    Box(
        modifier
            .graphicsLayer { scaleX = sq; scaleY = sq }
            .clip(shape)
            .background(bg)
            .combinedClickable(interactionSource = src, indication = ripple(), onLongClick = onLongClick, onClick = onClick),
        contentAlignment = contentAlignment,
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) { content() }
    }
}
