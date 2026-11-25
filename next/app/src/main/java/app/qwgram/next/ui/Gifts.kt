package app.qwgram.next.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.qwgram.next.td.Td
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.Texts
import org.drinkless.tdlib.TdApi
import kotlin.math.cos
import kotlin.math.sin

/*
 * Profile gifts (Telegram Stars gifts) and collectible NFT gifts.
 * A collectible is drawn the way Telegram draws it: its backdrop as a radial glow (centre → edge colour), a ring
 * of its pattern symbol in the backdrop's symbol colour, the animated model in the middle and its number.
 */

private fun argb(c: Int) = Color(c.toLong() or 0xFF000000L)

private fun rarityText(r: TdApi.UpgradedGiftAttributeRarity?): String = when (r) {
    is TdApi.UpgradedGiftAttributeRarityPerMille -> {
        val p = r.perMille / 10f
        if (p >= 1f) "${"%.1f".format(p).removeSuffix(",0").removeSuffix(".0")}%" else "${"%.1f".format(p)}%"
    }
    is TdApi.UpgradedGiftAttributeRarityUncommon -> "Необычный"
    is TdApi.UpgradedGiftAttributeRarityRare -> "Редкий"
    is TdApi.UpgradedGiftAttributeRarityEpic -> "Эпический"
    is TdApi.UpgradedGiftAttributeRarityLegendary -> "Легендарный"
    else -> ""
}

/**
 * The backdrop of a collectible, drawn in one Canvas (cheap enough for a grid of them): the radial glow from centre
 * to edge colour and two rings of the pattern symbol tinted with the backdrop's symbol colour.
 */
@Composable
private fun GiftBackdrop(center: Color, edge: Color, symbol: TdApi.Sticker?, symbolTint: Color, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val sym = rememberStickerStill(symbol, 96)
    Box(
        modifier.drawBehind {
            drawRect(edge)
            drawRect(Brush.radialGradient(listOf(center, edge), center = Offset(size.width / 2f, size.height * 0.45f), radius = size.maxDimension * 0.62f))
            val img = sym ?: return@drawBehind
            val unit = size.minDimension
            val tint = androidx.compose.ui.graphics.ColorFilter.tint(symbolTint)
            fun ring(n: Int, rx: Float, ry: Float, phase: Double, s: Float, a: Float) {
                val side = (unit * s).toInt().coerceAtLeast(2)
                for (i in 0 until n) {
                    val ang = Math.toRadians(i * 360.0 / n + phase)
                    val cx = size.width / 2f + (cos(ang) * rx * unit).toFloat()
                    val cy = size.height * 0.45f + (sin(ang) * ry * unit).toFloat()
                    drawImage(img, srcOffset = androidx.compose.ui.unit.IntOffset.Zero, srcSize = androidx.compose.ui.unit.IntSize(img.width, img.height),
                        dstOffset = androidx.compose.ui.unit.IntOffset((cx - side / 2f).toInt(), (cy - side / 2f).toInt()), dstSize = androidx.compose.ui.unit.IntSize(side, side),
                        alpha = a, colorFilter = tint)
                }
            }
            ring(6, 0.34f, 0.31f, 30.0, 0.11f, 0.30f)
            ring(10, 0.48f, 0.44f, 0.0, 0.09f, 0.16f)
        },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** the gift's model / sticker: a still in grids, animated where it's the star (details) */
@Composable
private fun GiftArt(s: TdApi.Sticker, size: Dp, animated: Boolean) {
    if (animated) { StickerContent(s, Modifier.size(size)); return }
    val px = with(androidx.compose.ui.platform.LocalDensity.current) { size.roundToPx() }
    val img = rememberStickerStill(s, px)
    if (img != null) androidx.compose.foundation.Image(img, null, Modifier.size(size))
    else Box(Modifier.size(size))
}

/** one gift tile (regular or collectible) */
@Composable
private fun GiftTile(g: TdApi.ReceivedGift, size: Dp, onClick: () -> Unit, modifier: Modifier = Modifier.size(size)) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp.rs())
    when (val sg = g.gift) {
        is TdApi.SentGiftUpgraded -> {
            val u = sg.gift
            val c = u.backdrop.colors
            Box(modifier.clip(shape).clickable(onClick = onClick)) {
                GiftBackdrop(argb(c.centerColor), argb(c.edgeColor), u.symbol.sticker, argb(c.symbolColor), Modifier.fillMaxSize()) {
                    Box(Modifier.padding(bottom = 10.dp)) { GiftArt(u.model.sticker, size * 0.6f, animated = false) }
                }
                // the number, Telegram-like ribbon
                Text(
                    "#${u.number}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.28f)).padding(horizontal = 8.dp, vertical = 2.dp),
                )
                if (g.isPinned) Icon(Icons.Outlined.PushPin, "Закреплён", tint = Color.White, modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(14.dp))
            }
        }
        is TdApi.SentGiftRegular -> {
            val gift = sg.gift
            val bg = gift.background
            Box(
                modifier.clip(shape)
                    .then(if (bg != null) Modifier.drawBehind {
                        drawRect(argb(bg.edgeColor))
                        drawRect(Brush.radialGradient(listOf(argb(bg.centerColor), argb(bg.edgeColor)), radius = this.size.maxDimension * 0.6f))
                    } else Modifier.background(cs.surfaceContainerHigh))
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.padding(bottom = 10.dp)) { GiftArt(gift.sticker, size * 0.58f, animated = false) }
                Text(
                    "★ ${gift.starCount}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFFFB300),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).clip(CircleShape).background(Color(0x33FFB300)).padding(horizontal = 8.dp, vertical = 2.dp),
                )
                if (g.isPinned) Icon(Icons.Outlined.PushPin, "Закреплён", tint = cs.onSurfaceVariant, modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(14.dp))
            }
        }
    }
}

/** the profile card: a row of gifts, "all gifts" opens the sheet */
@Composable
fun ProfileGifts(owner: TdApi.MessageSender, modifier: Modifier = Modifier) {
    var gifts by remember(owner) { mutableStateOf<List<TdApi.ReceivedGift>?>(null) }
    var total by remember(owner) { mutableStateOf(0) }
    var open by remember { mutableStateOf<TdApi.ReceivedGift?>(null) }
    var all by remember { mutableStateOf(false) }
    LaunchedEffect(owner) {
        val r = Td.callOrNull(TdApi.GetReceivedGifts("", owner, 0, false, false, false, false, false, false, false, false, false, "", 60))
        gifts = r?.gifts?.toList().orEmpty()
        total = r?.totalCount ?: 0
    }
    val list = gifts ?: return
    if (list.isEmpty()) return
    val cs = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(24.dp.rs()), color = cs.surfaceContainerLow, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { all = true }.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Redeem, null, tint = cs.primary)
                Spacer(Modifier.width(10.dp))
                Text("Подарки", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("$total", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Все подарки", tint = cs.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(list.take(12), key = { _, g -> g.receivedGiftId.ifEmpty { g.date.toString() + g.hashCode() } }) { i, g ->
                    // tiles pop in one after another
                    val a = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { kotlinx.coroutines.delay(40L * i.coerceAtMost(8)); a.animateTo(1f, spring(0.6f, 380f)) }
                    Box(Modifier.graphicsLayer { val s = 0.7f + 0.3f * a.value; scaleX = s; scaleY = s; alpha = a.value.coerceIn(0f, 1f) }) {
                        GiftTile(g, 96.dp, { open = g })
                    }
                }
            }
        }
    }
    if (all) QSheet(onDismiss = { all = false }) {
        Text("Подарки · $total", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
        LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list, key = { it.receivedGiftId.ifEmpty { it.date.toString() + it.hashCode() } }) { g ->
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f)) {
                    GiftTile(g, maxWidth, { open = g }, Modifier.fillMaxSize())
                }
            }
        }
    }
    open?.let { g -> GiftDetails(g) { open = null } }
}

/** gift details: the big collectible / gift, its attributes with rarity, who sent it and when */
@Composable
private fun GiftDetails(g: TdApi.ReceivedGift, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    QSheet(onDismiss = onDismiss, wrap = true) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            when (val sg = g.gift) {
                is TdApi.SentGiftUpgraded -> {
                    val u = sg.gift
                    val c = u.backdrop.colors
                    GiftBackdrop(argb(c.centerColor), argb(c.edgeColor), u.symbol.sticker, argb(c.symbolColor), Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(28.dp.rs()))) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            GiftArt(u.model.sticker, 150.dp, animated = true)
                            Text(u.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = argb(c.textColor).takeIf { c.textColor != 0 } ?: Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Коллекционный · #${u.number} из ${u.maxUpgradedCount.takeIf { it > 0 } ?: u.totalUpgradedCount}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.82f))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    val rows = buildList {
                        add(Triple("Модель", u.model.name, rarityText(u.model.rarity)))
                        add(Triple("Фон", u.backdrop.name, rarityText(u.backdrop.rarity)))
                        add(Triple("Узор", u.symbol.name, rarityText(u.symbol.rarity)))
                        val ownerName = u.ownerName.ifBlank { u.ownerId?.let { TdStore.senderName(it) } ?: "" }
                        if (ownerName.isNotBlank()) add(Triple("Владелец", ownerName, ""))
                    }
                    GiftRows(rows)
                }
                is TdApi.SentGiftRegular -> {
                    val gift = sg.gift
                    Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(28.dp.rs())).background(cs.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                        GiftArt(gift.sticker, 150.dp, animated = true)
                    }
                    Spacer(Modifier.height(12.dp))
                    val rows = buildList {
                        g.senderId?.let { add(Triple("От", TdStore.senderName(it), "")) }
                        add(Triple("Дата", Texts.dayHeader(g.date) + ", " + Texts.time(g.date), ""))
                        add(Triple("Стоимость", "★ ${gift.starCount}", ""))
                        gift.overallLimits?.let { lim -> add(Triple("Ограниченный", "${lim.remainingCount} из ${lim.totalCount}", "")) }
                    }
                    GiftRows(rows)
                }
            }
            if (g.text != null && g.text.text.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(20.dp.rs()), color = cs.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                    Text(g.text.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GiftRows(rows: List<Triple<String, String, String>>) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEachIndexed { i, (k, v, rare) ->
            Row(Modifier.segment(i, rows.size, cs.surfaceContainerHigh).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(k, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.width(110.dp))
                Text(v, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (rare.isNotBlank()) Text(
                    rare, style = MaterialTheme.typography.labelMedium, color = cs.onPrimaryContainer,
                    modifier = Modifier.clip(CircleShape).background(cs.primaryContainer).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}
