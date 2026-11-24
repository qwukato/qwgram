package app.qwgram.next.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.material.icons.outlined.Share
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.qwgram.next.Prefs
import app.qwgram.next.td.Images
import app.qwgram.next.td.Td
import app.qwgram.next.td.TdStore
import app.qwgram.next.td.Texts
import app.qwgram.next.td.rememberFileBitmap
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.border
import androidx.compose.animation.core.snap
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.filled.Close
import org.drinkless.tdlib.TdApi
import kotlin.math.roundToInt

private val emojiOnly = Regex("^(\\p{So}|\\p{Sk}|\\u200d|\\ufe0f|\\ud83c[\\udffb-\\udfff]|\\s|[\\ud800-\\udbff][\\udc00-\\udfff]){1,10}$")

@Composable
fun MessageItem(
    m: TdApi.Message,
    grp: Grp,
    chat: TdApi.Chat,
    group: Boolean,
    reply: TdApi.Message?,
    modifier: Modifier = Modifier,
    selection: Boolean? = null,
    highlighted: Boolean = false,
    onTap: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    onLong: () -> Unit,
    onDouble: () -> Unit,
    onSwipeReply: () -> Unit,
    onPhoto: (TdApi.Photo) -> Unit,
    onReplyClick: (Long) -> Unit = {},
) {
    val service = Texts.service(m)
    if (service != null) {
        Box(modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Text(service, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
            }
        }
        return
    }
    val out = m.isOutgoing && !m.isChannelPost
    val showAvatar = group && !out && Prefs.groupAvatars.value
    val lastInRun = grp == Grp.Single || grp == Grp.Bottom
    val firstInRun = grp == Grp.Single || grp == Grp.Top
    val swipeAnim = remember { Animatable(0f, visibilityThreshold = 0.5f) }
    // while the finger is down the offset is written synchronously (no coroutine hop → no jitter)
    var swipeDrag by remember { androidx.compose.runtime.mutableFloatStateOf(Float.NaN) }
    val swipe = object { val value: Float get() = if (!swipeDrag.isNaN()) swipeDrag else swipeAnim.value }
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()
    var armed by remember { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme
    var bubbleRect by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val tintBg by androidx.compose.animation.animateColorAsState(
        if (selection == true || highlighted) cs.primary.copy(alpha = 0.14f) else Color.Transparent, Springs.effects(), label = "sel",
    )

    Row(
        modifier.fillMaxWidth().background(tintBg).padding(start = 8.dp, end = 8.dp, top = if (firstInRun) 6.dp else 1.dp, bottom = 1.dp)
            // a tap beside the bubble (empty side of the row) also opens the menu, like Telegram
            .pointerInput(m.id) { detectTapGestures(onTap = { onTap(bubbleRect) }) }
            .pointerInput(m.id) {
                if (!Prefs.swipeReply.value) return@pointerInput
                // only leftward drags belong to reply-swipe; rightward ones stay free for swipe-back
                val slop = viewConfiguration.touchSlop
                val threshold = 64.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dx = 0f
                    var dy = 0f
                    var dragging = false
                    var raw = 0f
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        val d = ch.positionChange()
                        if (!dragging) {
                            if (ch.isConsumed) return@awaitEachGesture
                            dx += d.x; dy += d.y
                            if (kotlin.math.abs(dy) > slop && kotlin.math.abs(dy) > kotlin.math.abs(dx)) return@awaitEachGesture
                            if (dx > slop) return@awaitEachGesture
                            if (dx < -slop && -dx > kotlin.math.abs(dy) * 1.2f) dragging = true else continue
                        }
                        ch.consume()
                        raw = (raw + d.x).coerceAtMost(0f)
                        // one smooth curve from the first pixel: follows the finger, then gently saturates
                        val pull = -raw
                        val maxShown = threshold * 1.9f
                        val shown = maxShown * (1f - kotlin.math.exp(-pull / maxShown))
                        swipeDrag = -shown
                        val now = pull >= threshold * 1.15f
                        if (now && !armed) haptic(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        armed = now
                    }
                    if (dragging) {
                        if (armed) onSwipeReply()
                        armed = false
                        // soft return: low stiffness, gentle settle (no snapping back)
                        val from = swipeDrag
                        scope.launch {
                            if (!from.isNaN()) swipeAnim.snapTo(from)
                            swipeDrag = Float.NaN
                            swipeAnim.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 260f))
                        }
                    }
                }
            },
        horizontalArrangement = if (out) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        androidx.compose.animation.AnimatedVisibility(
            selection != null, Modifier.align(Alignment.CenterVertically),
            enter = androidx.compose.animation.expandHorizontally(Springs.spatial()) + androidx.compose.animation.fadeIn(Springs.effects()),
            exit = androidx.compose.animation.shrinkHorizontally(Springs.exit()) + androidx.compose.animation.fadeOut(Springs.exit()),
        ) {
            val on = selection == true
            val sc by androidx.compose.animation.core.animateFloatAsState(if (on) 1f else 0f, Springs.fastSpatial(), label = "chk")
            Box(
                Modifier.padding(start = 4.dp, end = 10.dp).size(24.dp).clip(CircleShape)
                    .border(2.dp, if (on) cs.primary else cs.outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(24.dp).graphicsLayer { scaleX = sc; scaleY = sc }.clip(CircleShape).background(cs.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = cs.onPrimary)
                }
            }
        }
        if (out) Spacer(Modifier.weight(1f))
        if (showAvatar) {
            if (lastInRun) SenderAvatar(m.senderId, 34.dp, Modifier.clickable { senderChatOrUser(m)?.let { Nav.push(Route.Profile(it)) } })
            else Spacer(Modifier.width(34.dp))
            Spacer(Modifier.width(6.dp))
        }
        Box(Modifier.offset { IntOffset(swipe.value.roundToInt(), 0) }.onGloballyPositioned { bubbleRect = it.boundsInRoot() }) {
            Bubble(m, grp, chat, group && !out, reply, { onTap(bubbleRect) }, onLong, onDouble, onPhoto, onReplyClick)
        }
        val swiping by remember { derivedStateOf { swipe.value < -4f } }
        if (swiping) {
            val thr = with(androidx.compose.ui.platform.LocalDensity.current) { 64.dp.toPx() }
            val pop by androidx.compose.animation.core.animateFloatAsState(if (armed) 1.15f else 1f, Springs.fastSpatial(), label = "pop")
            val bg by androidx.compose.animation.animateColorAsState(if (armed) cs.primary else cs.secondaryContainer, Springs.effects(), label = "rb")
            Box(
                Modifier.align(Alignment.CenterVertically).offset { IntOffset(swipe.value.roundToInt() + 8, 0) }
                    .graphicsLayer {
                        val p = (-swipe.value / thr).coerceIn(0f, 1f)
                        scaleX = p * pop; scaleY = p * pop; alpha = p; rotationZ = (1f - p) * -90f
                    }
                    .size(34.dp).clip(CircleShape).background(bg),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.Reply, null, Modifier.size(18.dp), tint = if (armed) cs.onPrimary else cs.onSecondaryContainer) }
        }
    }
}

/** width available to message bubbles (the preview in settings is narrower than the screen) */
val LocalBubbleArea = androidx.compose.runtime.compositionLocalOf<androidx.compose.ui.unit.Dp?> { null }

private fun senderChatOrUser(m: TdApi.Message): Long? = when (val s = m.senderId) {
    is TdApi.MessageSenderChat -> s.chatId
    is TdApi.MessageSenderUser -> TdStore.chats.values.firstOrNull { TdStore.privateUserId(it) == s.userId }?.id
    else -> null
}

@Composable
private fun Bubble(
    m: TdApi.Message, grp: Grp, chat: TdApi.Chat, showName: Boolean, reply: TdApi.Message?,
    onTap: () -> Unit, onLong: () -> Unit, onDouble: () -> Unit, onPhoto: (TdApi.Photo) -> Unit, onReplyClick: (Long) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val out = m.isOutgoing && !m.isChannelPost
    val style = Prefs.bubbleStyle.value
    val r = Prefs.bubbleRadius.value.dp
    val small = 6.dp
    val tail = Prefs.bubbleTail.value
    // corners: runs of messages are joined on the sender's side; the last one gets the tail
    val topSide = if (grp == Grp.Middle || grp == Grp.Bottom) small else r
    val bottomSide = if (grp == Grp.Middle || grp == Grp.Top || tail) small else r
    val shape = if (out) RoundedCornerShape(topStart = r, topEnd = topSide, bottomEnd = bottomSide, bottomStart = r)
    else RoundedCornerShape(topStart = topSide, topEnd = r, bottomEnd = r, bottomStart = bottomSide)
    val (bg, fg) = when {
        out && style == "filled" -> when (Prefs.outBubble.value) { "secondary" -> cs.secondary to cs.onSecondary; "tertiary" -> cs.tertiary to cs.onTertiary; else -> cs.primary to cs.onPrimary }
        out && style == "outlined" -> cs.surface to cs.onSurface
        out -> when (Prefs.outBubble.value) { "secondary" -> cs.secondaryContainer to cs.onSecondaryContainer; "tertiary" -> cs.tertiaryContainer to cs.onTertiaryContainer; else -> cs.primaryContainer to cs.onPrimaryContainer }
        style == "outlined" -> cs.surface to cs.onSurface
        else -> cs.surfaceContainerHigh to cs.onSurface
    }
    val c = m.content
    val textOnly = c is TdApi.MessageText || c is TdApi.MessageAnimatedEmoji
    val bigEmoji = Prefs.bigEmoji.value && ((c is TdApi.MessageText && c.text.entities.isEmpty() && emojiOnly.matches(c.text.text.trim())) || c is TdApi.MessageAnimatedEmoji)
    val bare = c is TdApi.MessageSticker || bigEmoji
    val media = c is TdApi.MessagePhoto || c is TdApi.MessageVideo || c is TdApi.MessageAnimation
    val maxW = ((LocalBubbleArea.current ?: androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp) * (Prefs.bubbleWidth.value / 100f))
    var pressed by remember { mutableStateOf(false) }
    val squish by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.965f else 1f, if (pressed) Springs.pressIn() else Springs.fastSpatial(), label = "bp")
    val gestures = Modifier
        .graphicsLayer { scaleX = squish; scaleY = squish; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (out) 1f else 0f, 0.5f) }
        .pointerInput(m.id) {
            detectTapGestures(
                onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                onTap = { onTap() }, onLongPress = { onLong() }, onDoubleTap = { onDouble() },
            )
        }

    if (bare) {
        Column(gestures.widthIn(max = maxW), horizontalAlignment = if (out) Alignment.End else Alignment.Start) {
            if (c is TdApi.MessageSticker) StickerView(c.sticker)
            else Text(
                (c as? TdApi.MessageText)?.text?.text ?: (c as TdApi.MessageAnimatedEmoji).emoji,
                fontSize = (Prefs.textSize.value * 3).sp, lineHeight = (Prefs.textSize.value * 3.4).sp,
            )
            val metaPill: @Composable () -> Unit = { Surface(shape = CircleShape, color = cs.surfaceContainerHigh) { Meta(m, chat, cs.onSurfaceVariant, Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) } }
            if (m.interactionInfo?.reactions?.reactions?.isNotEmpty() == true && !Prefs.hideReactions.value) ReactionsMeta(m, Modifier.padding(top = 4.dp), metaPill)
            else metaPill()
        }
        return
    }

    Column(horizontalAlignment = if (out) Alignment.End else Alignment.Start) {
    Surface(
        shape = shape, color = bg, contentColor = fg,
        border = if (style == "outlined") androidx.compose.foundation.BorderStroke(if (out) 1.5.dp else 1.dp, if (out) cs.primary else cs.outlineVariant) else null,
        modifier = gestures.widthIn(max = maxW),
    ) {
        val hasReactionsOuter = m.interactionInfo?.reactions?.reactions?.isNotEmpty() == true && !Prefs.hideReactions.value
        Column(Modifier.padding(if (media) 3.dp else 0.dp).then(if (hasReactionsOuter && !media) Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Max) else Modifier)) {
            val pad = if (media) Modifier.padding(horizontal = 8.dp) else Modifier.padding(horizontal = 12.dp)
            if (showName && (grp == Grp.Single || grp == Grp.Top)) {
                val id = TdStore.senderId(m.senderId)
                Text(TdStore.senderName(m.senderId), style = MaterialTheme.typography.labelLarge, color = nameColor(id), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = pad.padding(top = if (media) 4.dp else 8.dp))
            }
            m.forwardInfo?.let { fi ->
                Text("Переслано от " + originName(fi.origin), style = MaterialTheme.typography.labelMedium, color = fg.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = pad.padding(top = 6.dp))
            }
            if (reply != null || m.replyTo is TdApi.MessageReplyToMessage) ReplyQuote(reply, m, fg, pad.padding(top = 6.dp).clickable {
                (m.replyTo as? TdApi.MessageReplyToMessage)?.let { r -> if (r.chatId == m.chatId || r.chatId == 0L) onReplyClick(r.messageId) }
            })
            val cap = Texts.caption(c)
            val hasCap = cap != null && cap.text.isNotEmpty()
            val preview = (c as? TdApi.MessageText)?.linkPreview
            val hasReactions = m.interactionInfo?.reactions?.reactions?.isNotEmpty() == true && !Prefs.hideReactions.value
            Box {
            when (c) {
                is TdApi.MessagePhoto -> PhotoView(c.photo, shapeTop = r - 3.dp, onClick = { onPhoto(c.photo) })
                is TdApi.MessageVideo -> VideoThumb(c.video.thumbnail?.file, c.video.minithumbnail, c.video.width, c.video.height, Texts.duration(c.video.duration)) {
                    Opener.file(it, c.video.video, c.video.mimeType)
                }
                is TdApi.MessageAnimation -> VideoThumb(c.animation.thumbnail?.file, c.animation.minithumbnail, c.animation.width, c.animation.height, "GIF") {
                    Opener.file(it, c.animation.animation, c.animation.mimeType)
                }
                is TdApi.MessageVoiceNote -> VoiceView(c.voiceNote.voice, c.voiceNote.duration, c.voiceNote.waveform, fg, pad.padding(top = 8.dp))
                is TdApi.MessageAudio -> AudioView(c.audio, fg, pad.padding(top = 8.dp))
                is TdApi.MessageDocument -> DocumentView(c.document, fg, pad.padding(top = 8.dp))
                is TdApi.MessageVideoNote -> VideoThumb(c.videoNote.thumbnail?.file, c.videoNote.minithumbnail, 240, 240, Texts.duration(c.videoNote.duration), circle = true) {
                    Opener.file(it, c.videoNote.video, "video/mp4")
                }
                is TdApi.MessageLocation -> LocationView(c.location, fg, pad.padding(top = 8.dp))
                is TdApi.MessageContact -> ContactView(c.contact, fg, pad.padding(top = 8.dp))
                is TdApi.MessagePoll -> PollView(m, c.poll, fg, pad.padding(top = 8.dp))
                is TdApi.MessageCall -> Row(pad.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Call, null); Spacer(Modifier.width(8.dp))
                    Text((if (c.isVideo) "Видеозвонок" else "Звонок") + if (c.duration > 0) " · " + Texts.duration(c.duration) else "")
                }
                else -> Unit
            }
            // time over the picture when there is nothing below it (Telegram style)
            if (media && !hasCap && !hasReactions) {
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.45f), modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                    Meta(m, chat, Color.White, Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
                }
            }
            }
            val metaColor = fg.copy(alpha = 0.7f)
            if (hasCap) {
                Box(pad.padding(top = if (textOnly && !showName && m.forwardInfo == null && m.replyTo == null) 8.dp else 6.dp, bottom = if (hasReactions) 0.dp else 6.dp)) {
                    if (hasReactions || preview != null) RichText(cap!!, fg)
                    else TextWithMeta(cap!!, fg) { Meta(m, chat, metaColor, Modifier) }
                }
                if (preview != null) LinkPreviewCard(preview, fg, pad.padding(top = 6.dp, bottom = if (hasReactions) 0.dp else 2.dp))
            } else if (!textOnly && c !is TdApi.MessagePhoto && c !is TdApi.MessageVideo && c !is TdApi.MessageAnimation && c !is TdApi.MessageVideoNote &&
                c !is TdApi.MessageVoiceNote && c !is TdApi.MessageAudio && c !is TdApi.MessageDocument && c !is TdApi.MessageLocation &&
                c !is TdApi.MessageContact && c !is TdApi.MessagePoll && c !is TdApi.MessageCall) {
                Text(Texts.preview(m), style = MaterialTheme.typography.bodyMedium, modifier = pad.padding(top = 8.dp), color = fg.copy(alpha = 0.8f))
            }
            if (hasReactions) {
                ReactionsMeta(m, Modifier.fillMaxWidth().then(pad).padding(top = 6.dp, bottom = 6.dp)) {
                    Meta(m, chat, metaColor, Modifier.padding(bottom = 2.dp))
                }
            } else if ((!hasCap && !media) || (hasCap && preview != null)) {
                Meta(m, chat, metaColor, Modifier.align(Alignment.End).padding(start = 12.dp, end = 10.dp, bottom = 6.dp, top = 2.dp))
            }
        }
    }
    InlineKeyboard(m, maxW)
    }
}

private fun originName(o: TdApi.MessageOrigin): String = when (o) {
    is TdApi.MessageOriginUser -> TdStore.userName(o.senderUserId)
    is TdApi.MessageOriginHiddenUser -> o.senderName
    is TdApi.MessageOriginChat -> TdStore.chats[o.senderChatId]?.title ?: ""
    is TdApi.MessageOriginChannel -> TdStore.chats[o.chatId]?.title ?: ""
    else -> ""
}

@Composable
private fun ReplyQuote(reply: TdApi.Message?, m: TdApi.Message, fg: Color, modifier: Modifier) {
    val r = m.replyTo as? TdApi.MessageReplyToMessage
    val name = when {
        reply != null -> if (reply.isOutgoing) "Вы" else TdStore.senderName(reply.senderId)
        r?.origin != null -> originName(r.origin!!)
        else -> ""
    }
    val text = when {
        r?.quote != null -> r.quote!!.text.text
        reply != null -> Texts.preview(reply)
        r?.content != null -> "Сообщение"
        else -> "Сообщение"
    }
    Row(modifier.clip(RoundedCornerShape(8.dp)).background(fg.copy(alpha = 0.10f)).padding(end = 10.dp)) {
        Box(Modifier.width(3.dp).height(40.dp).background(fg.copy(alpha = 0.85f)))
        Column(Modifier.padding(start = 8.dp, top = 3.dp, bottom = 3.dp)) {
            Text(name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = fg.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun Meta(m: TdApi.Message, chat: TdApi.Chat, color: Color, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        val views = m.interactionInfo?.viewCount ?: 0
        if (views > 0 && m.isChannelPost) {
            Icon(Icons.Outlined.Visibility, null, Modifier.size(13.dp), tint = color)
            Spacer(Modifier.width(2.dp))
            Text(compact(views) + "  ", style = MaterialTheme.typography.labelSmall, color = color)
        }
        if (DeletedMessages.has(m)) {
            Icon(Icons.Outlined.DeleteOutline, "Удалено", Modifier.size(13.dp), tint = MaterialTheme.colorScheme.error)
            Text(" удалено  ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
        if (m.editDate > 0) Text("изм. ", style = MaterialTheme.typography.labelSmall, color = color)
        Text(Texts.time(m.date), style = MaterialTheme.typography.labelSmall, color = color)
        if (m.isOutgoing && !m.isChannelPost) {
            Spacer(Modifier.width(3.dp))
            val st = m.sendingState
            val icon = when {
                st is TdApi.MessageSendingStateFailed -> Icons.Outlined.ErrorOutline
                st is TdApi.MessageSendingStatePending -> Icons.Outlined.Schedule
                m.id <= chat.lastReadOutboxMessageId -> Icons.Rounded.DoneAll
                else -> Icons.Rounded.Check
            }
            Icon(icon, null, Modifier.size(15.dp), tint = if (st is TdApi.MessageSendingStateFailed) MaterialTheme.colorScheme.error else color)
        }
    }
}

private fun compact(n: Int): String = when {
    Prefs.seconds.value && false -> n.toString()
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 1000 -> "%.1fK".format(n / 1000.0)
    else -> n.toString()
}

/**
 * Reactions flow in compact lines; the time/ticks sit at the end of the last line (or on their own line when they
 * don't fit), right-aligned to the bubble. Reports real intrinsic widths so the bubble hugs its content.
 */
@Composable
private fun ReactionsMeta(m: TdApi.Message, modifier: Modifier = Modifier, meta: @Composable () -> Unit) {
    val list = m.interactionInfo?.reactions?.reactions ?: return
    val cs = MaterialTheme.colorScheme
    val density = androidx.compose.ui.platform.LocalDensity.current
    val policy = remember(density) { FlowMetaPolicy(with(density) { 4.dp.roundToPx() }, with(density) { 8.dp.roundToPx() }) }
    androidx.compose.ui.layout.Layout(
        modifier = modifier,
        measurePolicy = policy,
        content = {
            list.forEach { r -> key(reactionKey(r.type)) { ReactionChip(m, r) } }
            meta()
        },
    )
}

private fun reactionKey(t: TdApi.ReactionType): String = when (t) {
    is TdApi.ReactionTypeEmoji -> "e" + t.emoji
    is TdApi.ReactionTypeCustomEmoji -> "c" + t.customEmojiId
    is TdApi.ReactionTypePaid -> "paid"
    else -> t.toString()
}

/** one reaction: springs in, pops + bursts when you set it, count rolls; paid (Telegram Stars) reactions are gold */
@Composable
private fun ReactionChip(m: TdApi.Message, r: TdApi.MessageReaction) {
    val cs = MaterialTheme.colorScheme
    val paid = r.type is TdApi.ReactionTypePaid
    val emoji = when (val t = r.type) { is TdApi.ReactionTypeEmoji -> t.emoji; is TdApi.ReactionTypePaid -> "⭐"; else -> "✨" }
    val chosen = r.isChosen
    val pop = remember { Animatable(0.6f) }
    val burst = remember { Animatable(0f) }
    var seen by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(chosen) {
        val prev = seen
        seen = chosen
        if (prev == null) { pop.animateTo(1f, androidx.compose.animation.core.spring(0.55f, 600f)); return@LaunchedEffect }
        if (chosen && !prev) {
            launch { burst.snapTo(0f); burst.animateTo(1f, androidx.compose.animation.core.tween(620, easing = androidx.compose.animation.core.FastOutSlowInEasing)); burst.snapTo(0f) }
            pop.snapTo(0.72f)
            pop.animateTo(1f, androidx.compose.animation.core.spring(0.38f, 520f))
        } else pop.animateTo(1f, androidx.compose.animation.core.spring(0.7f, 600f))
    }
    val gold = Color(0xFFFFC233)
    val bg by androidx.compose.animation.animateColorAsState(
        // solid containers: readable on any bubble colour (light, dark, tinted)
        when { paid -> if (chosen) gold else Color(0xFFFFE8A3); chosen -> cs.primary; else -> cs.surfaceContainerHighest },
        Springs.effects(), label = "rb",
    )
    val fg = when { paid -> Color(0xFF5C3B00); chosen -> cs.onPrimary; else -> cs.onSurface }
    Box {
        MorphBox(
            onClick = { if (r.type is TdApi.ReactionTypeEmoji) react(m, emoji) }, rest = 14.dp, pressed = 7.dp,
            color = bg, contentColor = fg,
            modifier = Modifier.height(28.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                .then(if (paid) Modifier.border(1.dp, gold.copy(alpha = 0.7f), RoundedCornerShape(14.dp)) else Modifier),
        ) {
            Row(Modifier.padding(start = 7.dp, end = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 14.sp)
                Spacer(Modifier.width(3.dp))
                androidx.compose.animation.AnimatedContent(
                    r.totalCount,
                    transitionSpec = {
                        val up = targetState > initialState
                        (androidx.compose.animation.slideInVertically(Springs.fastSpatial()) { if (up) it else -it } + androidx.compose.animation.fadeIn(Springs.effects()))
                            .togetherWith(androidx.compose.animation.slideOutVertically(Springs.exit()) { if (up) -it else it } + androidx.compose.animation.fadeOut(Springs.exit()))
                    },
                    label = "rc",
                ) { n ->
                    Text(compact(n), style = MaterialTheme.typography.labelMedium, fontWeight = if (chosen || paid) androidx.compose.ui.text.font.FontWeight.Bold else null)
                }
            }
        }
        // burst: the emoji flies up, grows and fades out
        if (burst.value > 0f) Text(
            emoji, fontSize = 20.sp,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 3.dp).graphicsLayer {
                val t = burst.value
                translationY = -46.dp.toPx() * t
                scaleX = 1f + 1.4f * t; scaleY = scaleX
                alpha = 1f - t * t
            },
        )
    }
}

private class FlowMetaPolicy(private val gap: Int, private val metaGap: Int) : androidx.compose.ui.layout.MeasurePolicy {
    override fun androidx.compose.ui.layout.MeasureScope.measure(
        measurables: List<androidx.compose.ui.layout.Measurable>, constraints: androidx.compose.ui.unit.Constraints,
    ): androidx.compose.ui.layout.MeasureResult {
        val maxW = constraints.maxWidth
        val chips = measurables.dropLast(1).map { it.measure(androidx.compose.ui.unit.Constraints(maxWidth = maxW)) }
        val meta = measurables.last().measure(androidx.compose.ui.unit.Constraints(maxWidth = maxW))
        val pos = ArrayList<androidx.compose.ui.unit.IntOffset>(chips.size)
        var x = 0
        var y = 0
        var lineH = 0
        var widest = 0
        chips.forEach { c ->
            if (x > 0 && x + c.width > maxW) { y += lineH + gap; x = 0; lineH = 0 }
            pos.add(androidx.compose.ui.unit.IntOffset(x, y))
            x += c.width + gap
            lineH = maxOf(lineH, c.height)
            widest = maxOf(widest, x - gap)
        }
        val lastEnd = (x - gap).coerceAtLeast(0)
        val inline = lastEnd + metaGap + meta.width <= maxW
        val contentW = if (inline) maxOf(widest, lastEnd + metaGap + meta.width) else maxOf(widest, meta.width)
        val w = maxOf(constraints.minWidth, contentW).coerceAtMost(maxW)
        val metaY = if (inline) y + lineH - meta.height else y + lineH + 2
        val h = if (inline) y + lineH else y + lineH + 2 + meta.height
        return layout(w, h) {
            chips.forEachIndexed { i, c -> c.place(pos[i]) }
            meta.place(w - meta.width, metaY.coerceAtLeast(0))
        }
    }

    override fun androidx.compose.ui.layout.IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<androidx.compose.ui.layout.IntrinsicMeasurable>, height: Int): Int =
        measurables.sumOf { it.maxIntrinsicWidth(height) } + gap * (measurables.size - 2).coerceAtLeast(0) + metaGap

    override fun androidx.compose.ui.layout.IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<androidx.compose.ui.layout.IntrinsicMeasurable>, height: Int): Int =
        measurables.maxOf { it.minIntrinsicWidth(height) }
}

@Composable
fun RichText(ft: TdApi.FormattedText, color: Color, onLayout: ((androidx.compose.ui.text.TextLayoutResult) -> Unit)? = null) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val linkColor = if (color == cs.onPrimary) cs.onPrimary else cs.primary
    val annotated = remember(ft, linkColor) { formatted(ft, linkColor, ctx) }
    Text(
        annotated, style = MaterialTheme.typography.bodyLarge.copy(fontSize = Prefs.textSize.value.sp, lineHeight = (Prefs.textSize.value * 1.35f).sp), color = color,
        onTextLayout = { r -> onLayout?.invoke(r) },
    )
}

/**
 * Text with the time/ticks tucked into the end of the last line when they fit (Telegram layout),
 * otherwise on their own line at the bottom-right.
 */
@Composable
fun TextWithMeta(ft: TdApi.FormattedText, color: Color, meta: @Composable () -> Unit) {
    val holder = remember { arrayOfNulls<androidx.compose.ui.text.TextLayoutResult>(1) }
    androidx.compose.ui.layout.Layout(
        content = {
            RichText(ft, color) { holder[0] = it }
            meta()
        },
    ) { ms, c ->
        val gap = 8.dp.roundToPx()
        val text = ms[0].measure(c.copy(minWidth = 0))
        val metaP = ms[1].measure(androidx.compose.ui.unit.Constraints())
        val lay = holder[0]
        val lastW = if (lay != null && lay.lineCount > 0) {
            val l = lay.lineCount - 1
            kotlin.math.ceil(lay.getLineRight(l) - lay.getLineLeft(l)).toInt()
        } else text.width
        val rtl = lay != null && lay.lineCount > 0 && lay.getParagraphDirection(lay.getLineStart(lay.lineCount - 1)) == androidx.compose.ui.text.style.ResolvedTextDirection.Rtl
        val fitsInline = !rtl && lastW + gap + metaP.width <= c.maxWidth
        val w: Int
        val h: Int
        if (fitsInline) {
            w = maxOf(text.width, lastW + gap + metaP.width).coerceAtMost(c.maxWidth)
            h = maxOf(text.height, metaP.height)
        } else {
            w = maxOf(text.width, metaP.width).coerceAtMost(c.maxWidth)
            h = text.height + metaP.height
        }
        layout(w, h) {
            text.placeRelative(0, 0)
            metaP.placeRelative(w - metaP.width, h - metaP.height + if (fitsInline) 2 else 0)
        }
    }
}

private fun formatted(ft: TdApi.FormattedText, link: Color, ctx: android.content.Context): AnnotatedString = buildAnnotatedString {
    val t = ft.text
    append(t)
    val linkStyle = TextLinkStyles(SpanStyle(color = link, textDecoration = TextDecoration.Underline))
    for (e in ft.entities) {
        val s = e.offset.coerceIn(0, t.length)
        val end = (e.offset + e.length).coerceIn(s, t.length)
        if (s >= end) continue
        val piece = t.substring(s, end)
        when (val ty = e.type) {
            is TdApi.TextEntityTypeBold -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), s, end)
            is TdApi.TextEntityTypeItalic -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), s, end)
            is TdApi.TextEntityTypeUnderline -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), s, end)
            is TdApi.TextEntityTypeStrikethrough -> addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), s, end)
            is TdApi.TextEntityTypeCode, is TdApi.TextEntityTypePre, is TdApi.TextEntityTypePreCode ->
                addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = link.copy(alpha = 0.12f)), s, end)
            is TdApi.TextEntityTypeSpoiler -> addStyle(SpanStyle(background = link.copy(alpha = 0.35f), color = Color.Transparent), s, end)
            is TdApi.TextEntityTypeUrl -> addLink(LinkAnnotation.Clickable(piece, linkStyle) { Opener.url(ctx, piece) }, s, end)
            is TdApi.TextEntityTypeTextUrl -> addLink(LinkAnnotation.Clickable(ty.url, linkStyle) { Opener.url(ctx, ty.url) }, s, end)
            is TdApi.TextEntityTypeEmailAddress -> addLink(LinkAnnotation.Url("mailto:$piece", linkStyle), s, end)
            is TdApi.TextEntityTypePhoneNumber -> addLink(LinkAnnotation.Url("tel:$piece", linkStyle), s, end)
            is TdApi.TextEntityTypeMention -> addLink(LinkAnnotation.Clickable(piece, TextLinkStyles(SpanStyle(color = link))) { Opener.username(ctx, piece) }, s, end)
            is TdApi.TextEntityTypeMentionName -> addLink(LinkAnnotation.Clickable(piece + ty.userId, TextLinkStyles(SpanStyle(color = link))) {
                Td.send(TdApi.CreatePrivateChat(ty.userId, false)) { r -> if (r is TdApi.Chat) Nav.push(Route.Profile(r.id)) }
            }, s, end)
            is TdApi.TextEntityTypeHashtag, is TdApi.TextEntityTypeCashtag, is TdApi.TextEntityTypeBotCommand -> addStyle(SpanStyle(color = link), s, end)
            is TdApi.TextEntityTypeBlockQuote, is TdApi.TextEntityTypeExpandableBlockQuote -> addStyle(SpanStyle(fontStyle = FontStyle.Italic, background = link.copy(alpha = 0.08f)), s, end)
            else -> Unit
        }
    }
}

// ------------------------------------------------------------------ media

private fun bestSize(p: TdApi.Photo, maxSide: Int = 1280): TdApi.PhotoSize? =
    p.sizes.filter { maxOf(it.width, it.height) <= maxSide }.maxByOrNull { it.width * it.height } ?: p.sizes.minByOrNull { it.width * it.height }

@Composable
private fun PhotoView(p: TdApi.Photo, shapeTop: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val size = bestSize(p) ?: return
    val bmp = rememberFileBitmap(size.photo, maxSide = 1080)
    val mini = remember(p) { Images.mini(p.minithumbnail) }
    val ratio = (size.width.toFloat() / size.height.coerceAtLeast(1)).coerceIn(0.5f, 2.2f)
    Box(
        Modifier.fillMaxWidth().widthIn(min = 160.dp).aspectRatio(ratio).clip(RoundedCornerShape(topStart = shapeTop, topEnd = shapeTop, bottomStart = 6.dp, bottomEnd = 6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest).clickable(onClick = onClick),
    ) {
        if (bmp == null && mini != null) Image(mini, null, Modifier.fillMaxSize().blur(14.dp), contentScale = ContentScale.Crop)
        androidx.compose.animation.AnimatedVisibility(bmp != null, enter = androidx.compose.animation.fadeIn(Springs.effects()), exit = androidx.compose.animation.fadeOut(snap())) {
            bmp?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        }
        if (bmp == null) DownloadProgress(size.photo, Modifier.align(Alignment.Center))
    }
}

@Composable
private fun DownloadProgress(file: TdApi.File, modifier: Modifier) {
    val f = TdStore.files[file.id] ?: file
    val total = maxOf(f.size, f.expectedSize)
    val target = if (total > 0) f.local.downloadedSize.toFloat() / total else 0f
    val p by androidx.compose.animation.core.animateFloatAsState(target, Springs.smooth(), label = "dl")
    Box(modifier.size(52.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.38f)), contentAlignment = Alignment.Center) {
        if (target <= 0.01f) androidx.compose.material3.LoadingIndicator(Modifier.size(40.dp), color = Color.White)
        else androidx.compose.material3.CircularWavyProgressIndicator(progress = { p }, modifier = Modifier.size(40.dp), color = Color.White, trackColor = Color.White.copy(alpha = 0.25f))
    }
}

@Composable
private fun VideoThumb(thumb: TdApi.File?, mini: TdApi.Minithumbnail?, w: Int, h: Int, label: String, circle: Boolean = false, onOpen: (android.content.Context) -> Unit) {
    val ctx = LocalContext.current
    val bmp = rememberFileBitmap(thumb, maxSide = 720)
    val miniBmp = remember(mini) { Images.mini(mini) }
    val ratio = if (circle) 1f else (w.toFloat() / h.coerceAtLeast(1)).coerceIn(0.5f, 2.2f)
    Box(
        Modifier.then(if (circle) Modifier.size(220.dp) else Modifier.fillMaxWidth().widthIn(min = 180.dp))
            .aspectRatio(ratio).clip(if (circle) CircleShape else RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest).clickable { onOpen(ctx) },
        contentAlignment = Alignment.Center,
    ) {
        (bmp ?: miniBmp)?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Box(Modifier.size(52.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PlayArrow, "Смотреть", tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.45f), contentColor = Color.White, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
        }
    }
}

@Composable
private fun StickerView(s: TdApi.Sticker) {
    val isStatic = s.format is TdApi.StickerFormatWebp
    val bmp = rememberFileBitmap(if (isStatic) s.sticker else s.thumbnail?.file, maxSide = 512)
    Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
        androidx.compose.animation.AnimatedContent(bmp != null, transitionSpec = { (androidx.compose.animation.fadeIn(Springs.effects()) + androidx.compose.animation.scaleIn(Springs.spatial(), initialScale = 0.8f)).togetherWith(androidx.compose.animation.fadeOut(Springs.exit())) }, label = "st") { ready ->
            if (ready && bmp != null) Image(bmp, s.emoji, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            else Box(contentAlignment = Alignment.Center) {
                Text(s.emoji, fontSize = 56.sp, modifier = Modifier.graphicsLayer { alpha = 0.35f })
                androidx.compose.material3.LoadingIndicator(Modifier.size(48.dp))
            }
        }
    }
}

@Composable
private fun VoiceView(file: TdApi.File, duration: Int, waveform: ByteArray, fg: Color, modifier: Modifier) {
    val playing = Player.fileId == file.id && Player.playing
    val progress = if (Player.fileId == file.id) Player.progress else 0f
    val bars = remember(waveform) { decodeWaveform(waveform, 36) }
    Row(modifier.widthIn(min = 220.dp), verticalAlignment = Alignment.CenterVertically) {
        PlayButton(playing, fg, file) { Player.toggle(file) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.height(26.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                bars.forEachIndexed { i, v ->
                    val on = i / bars.size.toFloat() <= progress && progress > 0f
                    Box(Modifier.weight(1f).height((4 + v * 22).dp).clip(CircleShape).background(fg.copy(alpha = if (on) 1f else 0.35f)))
                }
            }
            Text(Texts.duration(duration), style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.8f))
        }
    }
}

private fun decodeWaveform(w: ByteArray, n: Int): List<Float> {
    if (w.isEmpty()) return List(n) { i -> 0.25f + 0.5f * kotlin.math.abs(kotlin.math.sin(i * 1.3)).toFloat() }
    // Telegram packs 5-bit samples
    val count = w.size * 8 / 5
    val samples = IntArray(count) { i ->
        val bit = i * 5
        val byte = bit / 8
        val shift = bit % 8
        val v = ((w[byte].toInt() and 0xFF) or (if (byte + 1 < w.size) (w[byte + 1].toInt() and 0xFF) shl 8 else 0)) shr shift
        v and 31
    }
    return List(n) { i ->
        val a = i * count / n
        val b = ((i + 1) * count / n).coerceAtLeast(a + 1).coerceAtMost(count)
        (a until b).maxOfOrNull { samples[it] }?.div(31f) ?: 0f
    }
}

/**
 * Play/pause as an expressive toggle: the container morphs circle → rounded square while playing, tightens on
 * press; while the file downloads a wavy progress ring runs around it and the icon becomes a loading indicator.
 */
@Composable
private fun PlayButton(playing: Boolean, fg: Color, file: TdApi.File?, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val f = file?.let { TdStore.files[it.id] ?: it }
    val loading = f != null && !f.local.isDownloadingCompleted && f.local.isDownloadingActive
    val total = f?.let { maxOf(it.size, it.expectedSize) } ?: 0L
    val dl = if (f != null && total > 0) f.local.downloadedSize.toFloat() / total else 0f
    val dlA by androidx.compose.animation.core.animateFloatAsState(dl, Springs.smooth(), label = "vd")
    val morph = remember { androidx.graphics.shapes.Morph(androidx.compose.material3.MaterialShapes.Circle, androidx.compose.material3.MaterialShapes.Square) }
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val target = (if (playing) 1f else 0f) + if (pressed) 0.35f * pressFactor() * (if (playing) -1f else 1f) else 0f
    val p by androidx.compose.animation.core.animateFloatAsState(target.coerceIn(0f, 1f), if (pressed) Springs.pressIn() else Springs.morphRelease(), label = "pm")
    val content = if (fg == cs.onPrimary) cs.primary else cs.surface
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(44.dp).graphicsLayer { shape = MorphShape(morph, p); clip = true }.background(fg)
                .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedContent(
                when { loading -> 2; playing -> 1; else -> 0 },
                transitionSpec = { (androidx.compose.animation.scaleIn(Springs.fastSpatial(), initialScale = 0.5f) + androidx.compose.animation.fadeIn(Springs.effects())).togetherWith(androidx.compose.animation.scaleOut(Springs.exit(), targetScale = 0.5f) + androidx.compose.animation.fadeOut(Springs.exit())) },
                label = "pi",
            ) { st ->
                when (st) {
                    2 -> androidx.compose.material3.LoadingIndicator(Modifier.size(30.dp), color = content)
                    1 -> Icon(Icons.Filled.Pause, "Пауза", tint = content)
                    else -> Icon(Icons.Filled.PlayArrow, "Слушать", tint = content)
                }
            }
        }
        if (loading && dl > 0f) androidx.compose.material3.CircularWavyProgressIndicator(progress = { dlA }, modifier = Modifier.size(48.dp), color = fg, trackColor = fg.copy(alpha = 0.2f))
    }
}

@Composable
private fun AudioView(a: TdApi.Audio, fg: Color, modifier: Modifier) {
    val playing = Player.fileId == a.audio.id && Player.playing
    Row(modifier.widthIn(min = 220.dp), verticalAlignment = Alignment.CenterVertically) {
        PlayButton(playing, fg, a.audio) { Player.toggle(a.audio) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(a.title.ifBlank { a.fileName }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOf(a.performer, Texts.duration(a.duration)).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.8f))
            if (Player.fileId == a.audio.id) LinearProgressIndicator(progress = { Player.progress }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
}

@Composable
private fun DocumentView(d: TdApi.Document, fg: Color, modifier: Modifier) {
    val ctx = LocalContext.current
    val f = TdStore.files[d.document.id] ?: d.document
    val done = f.local.isDownloadingCompleted
    val cs = MaterialTheme.colorScheme
    Row(modifier.widthIn(min = 220.dp).clip(RoundedCornerShape(12.dp)).clickable { Opener.file(ctx, d.document, d.mimeType) }, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(fg), contentAlignment = Alignment.Center) {
            if (f.local.isDownloadingActive) androidx.compose.material3.CircularProgressIndicator(
                progress = { if (f.size > 0) f.local.downloadedSize.toFloat() / f.size else 0f }, modifier = Modifier.size(30.dp),
                color = if (fg == cs.onPrimary) cs.primary else cs.surface,
            )
            else Icon(if (done) Icons.Outlined.Description else Icons.Outlined.Download, null, tint = if (fg == cs.onPrimary) cs.primary else cs.surface)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(d.fileName.ifBlank { "Файл" }, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(Texts.size(maxOf(f.size, f.expectedSize)), style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun LocationView(l: TdApi.Location, fg: Color, modifier: Modifier) {
    val ctx = LocalContext.current
    Row(modifier.widthIn(min = 220.dp).clip(RoundedCornerShape(12.dp)).clickable { Opener.location(ctx, l.latitude, l.longitude) }, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).clip(androidx.compose.material3.MaterialShapes.Sunny.asShape()).background(MaterialTheme.colorScheme.tertiaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Геопозиция", style = MaterialTheme.typography.titleSmall)
            Text("%.5f, %.5f".format(l.latitude, l.longitude), style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun ContactView(c: TdApi.Contact, fg: Color, modifier: Modifier) {
    Row(modifier.widthIn(min = 200.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(listOf(c.firstName, c.lastName).joinToString(" "), c.userId, null, null, 46.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(listOf(c.firstName, c.lastName).joinToString(" ").trim(), style = MaterialTheme.typography.titleSmall)
            Text(c.phoneNumber, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun PollView(m: TdApi.Message, p: TdApi.Poll, fg: Color, modifier: Modifier) {
    val voted = p.options.any { it.isChosen } || p.isClosed
    Column(modifier.widthIn(min = 240.dp)) {
        Text(p.question.text, style = MaterialTheme.typography.titleSmall)
        Text((if (p.isAnonymous) "Анонимный опрос" else "Опрос") + " · " + Texts.plural(p.totalVoterCount, "голос", "голоса", "голосов"),
            style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.75f))
        p.options.forEachIndexed { i, o ->
            Column(
                Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = !voted) { Td.send(TdApi.SetPollAnswer(m.chatId, m.id, intArrayOf(i))) }
                    .padding(vertical = 4.dp),
            ) {
                Row {
                    Text((if (o.isChosen) "✓ " else "") + o.text.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (voted) Text("${o.votePercentage}%", style = MaterialTheme.typography.labelLarge)
                }
                if (voted) LinearProgressIndicator(progress = { o.votePercentage / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = fg, trackColor = fg.copy(alpha = 0.15f))
            }
        }
    }
}

/** full-screen photo with pinch-to-zoom */
@Composable
fun PhotoViewer(p: TdApi.Photo, onDismiss: () -> Unit) = PhotoGallery(listOf(p), 0, onDismiss)

/**
 * Full-screen gallery: swipe between photos, pinch / double-tap to zoom (side swipes still page when not zoomed),
 * drag down to close with the background fading out.
 */
@Composable
fun PhotoGallery(photos: List<TdApi.Photo>, start: Int, onDismiss: () -> Unit) {
    if (photos.isEmpty()) { onDismiss(); return }
    val pager = androidx.compose.foundation.pager.rememberPagerState(start.coerceIn(0, photos.lastIndex)) { photos.size }
    val drag = remember { androidx.compose.animation.core.Animatable(0f) }
    val appear = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val closeAt = with(density) { 120.dp.toPx() }
    var chrome by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appear.animateTo(1f, Springs.smooth()) }
    fun close() { scope.launch { appear.animateTo(0f, androidx.compose.animation.core.tween(180)); onDismiss() } }
    Dialog(onDismissRequest = { close() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val pull = (kotlin.math.abs(drag.value) / (closeAt * 2.2f)).coerceIn(0f, 1f)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = (1f - pull) * appear.value))) {
            androidx.compose.foundation.pager.HorizontalPager(
                pager, userScrollEnabled = !zoomed, pageSpacing = 16.dp, beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    // swipe down/up to close: the photo follows the finger and shrinks a little (Telegram)
                    translationY = drag.value
                    val sc = (1f - pull * 0.25f) * (0.9f + 0.1f * appear.value)
                    scaleX = sc; scaleY = sc
                    alpha = appear.value
                },
            ) { page ->
                ZoomPhoto(
                    photos[page], active = page == pager.currentPage,
                    onTap = { chrome = !chrome },
                    onZoomed = { if (page == pager.currentPage) zoomed = it },
                    onVerticalDrag = { dy -> scope.launch { drag.snapTo(drag.value + dy) } },
                    onVerticalEnd = { v ->
                        if (kotlin.math.abs(drag.value) > closeAt || kotlin.math.abs(v) > 1800f) close()
                        else scope.launch { drag.animateTo(0f, Springs.morphRelease()) }
                    },
                )
            }
            val chromeA by androidx.compose.animation.core.animateFloatAsState(if (chrome && pull < 0.05f) 1f else 0f, Springs.effects(), label = "ch")
            // top: close + counter, floating M3 containers
            Row(
                Modifier.statusBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp).fillMaxWidth().graphicsLayer { alpha = chromeA * appear.value },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MorphIconButton(Icons.Filled.Close, "Закрыть", { close() }, color = Color.White.copy(alpha = 0.16f), tint = Color.White)
                Spacer(Modifier.weight(1f))
                if (photos.size > 1) Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.16f), contentColor = Color.White) {
                    androidx.compose.animation.AnimatedContent(pager.currentPage, transitionSpec = {
                        val fwd = targetState > initialState
                        (androidx.compose.animation.slideInVertically(Springs.fastSpatial()) { if (fwd) it else -it } + androidx.compose.animation.fadeIn(Springs.effects()))
                            .togetherWith(androidx.compose.animation.slideOutVertically(Springs.exit()) { if (fwd) -it else it } + androidx.compose.animation.fadeOut(Springs.exit()))
                    }, label = "pg") { i ->
                        Text("${i + 1} из ${photos.size}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
            }
            // bottom: a floating toolbar (M3 Expressive) — share, save
            Row(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp)
                    .graphicsLayer { alpha = chromeA * appear.value; translationY = (1f - chromeA) * 40.dp.toPx() }
                    .clip(CircleShape).background(Color(0xFF2A2A2E).copy(alpha = 0.92f)).padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val cur = photos.getOrNull(pager.currentPage)
                val file = cur?.sizes?.maxByOrNull { it.width * it.height }?.photo
                MorphIconButton(Icons.Outlined.Share, "Поделиться", { file?.let { shareImage(ctx, it) } }, tint = Color.White)
                MorphIconButton(Icons.Outlined.Download, "Сохранить в галерею", { file?.let { saveImage(ctx, it) } }, tint = Color.White)
            }
        }
    }
}

private fun shareImage(ctx: android.content.Context, f0: TdApi.File) {
    val f = TdStore.files[f0.id] ?: f0
    val path = f.local?.path?.takeIf { f.local.isDownloadingCompleted } ?: run {
        android.widget.Toast.makeText(ctx, "Фото ещё загружается", android.widget.Toast.LENGTH_SHORT).show(); return
    }
    runCatching {
        val uri = androidx.core.content.FileProvider.getUriForFile(ctx, ctx.packageName + ".files", java.io.File(path))
        val i = android.content.Intent(android.content.Intent.ACTION_SEND).setType("image/*").putExtra(android.content.Intent.EXTRA_STREAM, uri)
            .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(android.content.Intent.createChooser(i, null))
    }.onFailure { android.widget.Toast.makeText(ctx, "Не удалось поделиться", android.widget.Toast.LENGTH_SHORT).show() }
}

private fun saveImage(ctx: android.content.Context, f0: TdApi.File) {
    val f = TdStore.files[f0.id] ?: f0
    val path = f.local?.path?.takeIf { f.local.isDownloadingCompleted } ?: run {
        android.widget.Toast.makeText(ctx, "Фото ещё загружается", android.widget.Toast.LENGTH_SHORT).show(); return
    }
    val ok = runCatching {
        if (android.os.Build.VERSION.SDK_INT < 29) error("old")
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "qwgram_${System.currentTimeMillis()}.jpg")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/qwgram")
        }
        val uri = ctx.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        ctx.contentResolver.openOutputStream(uri)!!.use { out -> java.io.File(path).inputStream().use { it.copyTo(out) } }
    }.isSuccess
    android.widget.Toast.makeText(ctx, if (ok) "Сохранено в «Изображения/qwgram»" else "Не удалось сохранить", android.widget.Toast.LENGTH_SHORT).show()
}

/**
 * One photo: pinch to zoom around the fingers, pan within the photo's edges, double-tap zooms to the tapped point
 * (animated), single tap toggles the chrome; at 1× a vertical drag closes the viewer and a horizontal one flips pages.
 */
@Composable
private fun ZoomPhoto(p: TdApi.Photo, active: Boolean, onTap: () -> Unit, onZoomed: (Boolean) -> Unit, onVerticalDrag: (Float) -> Unit, onVerticalEnd: (Float) -> Unit) {
    val size = p.sizes.maxByOrNull { it.width * it.height } ?: return
    val bmp = rememberFileBitmap(size.photo, maxSide = 2560)
    val miniImg = remember(p) { Images.mini(p.minithumbnail) }
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    val ox = remember { androidx.compose.animation.core.Animatable(0f) }
    val oy = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()
    var box by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val aspect = if (size.height > 0) size.width.toFloat() / size.height else 1f
    LaunchedEffect(active) { if (!active) { scale.snapTo(1f); ox.snapTo(0f); oy.snapTo(0f); onZoomed(false) } }
    fun limits(sc: Float): Pair<Float, Float> {
        val w = box.width.toFloat(); val h = box.height.toFloat()
        if (w <= 0f || h <= 0f) return 0f to 0f
        // displayed (fit) size of the photo
        val fitW = if (w / h > aspect) h * aspect else w
        val fitH = if (w / h > aspect) h else w / aspect
        return ((fitW * sc - w) / 2f).coerceAtLeast(0f) to ((fitH * sc - h) / 2f).coerceAtLeast(0f)
    }
    fun settle() = scope.launch {
        val target = scale.value.coerceIn(1f, 6f)
        val (mx, my) = limits(target)
        launch { scale.animateTo(target, Springs.spatial()) }
        launch { ox.animateTo(ox.value.coerceIn(-mx, mx), Springs.spatial()) }
        launch { oy.animateTo(oy.value.coerceIn(-my, my), Springs.spatial()) }
        onZoomed(target > 1.01f)
    }
    Box(
        Modifier.fillMaxSize().onSizeChanged { box = it }
            .pointerInput(p) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { pos ->
                        scope.launch {
                            if (scale.value > 1.05f) {
                                launch { scale.animateTo(1f, Springs.spatial()) }; launch { ox.animateTo(0f, Springs.spatial()) }; launch { oy.animateTo(0f, Springs.spatial()) }
                                onZoomed(false)
                            } else {
                                val z = 2.6f
                                val cx = pos.x - box.width / 2f
                                val cy = pos.y - box.height / 2f
                                val (mx, my) = limits(z)
                                launch { scale.animateTo(z, Springs.spatial()) }
                                launch { ox.animateTo((-cx * (z - 1f)).coerceIn(-mx, mx), Springs.spatial()) }
                                launch { oy.animateTo((-cy * (z - 1f)).coerceIn(-my, my), Springs.spatial()) }
                                onZoomed(true)
                            }
                        }
                    },
                )
            }
            .pointerInput(p) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val tracker = androidx.compose.ui.input.pointer.util.VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    var mode = 0  // 0 undecided, 1 transform, 2 dismiss-drag, 3 let the pager have it
                    var acc = androidx.compose.ui.geometry.Offset.Zero
                    while (true) {
                        val ev = awaitPointerEvent()
                        if (ev.changes.none { it.pressed }) break
                        val multi = ev.changes.count { it.pressed } > 1
                        ev.changes.firstOrNull { it.id == down.id }?.let { tracker.addPosition(it.uptimeMillis, it.position) }
                        if (mode == 0) {
                            acc += ev.calculatePan()
                            mode = when {
                                multi || scale.value > 1.01f -> 1
                                kotlin.math.abs(acc.y) > slop && kotlin.math.abs(acc.y) > kotlin.math.abs(acc.x) -> 2
                                kotlin.math.abs(acc.x) > slop -> 3
                                else -> 0
                            }
                        }
                        when (mode) {
                            1 -> {
                                val z = ev.calculateZoom()
                                val pan = ev.calculatePan()
                                val c = ev.calculateCentroid(useCurrent = true) - androidx.compose.ui.geometry.Offset(box.width / 2f, box.height / 2f)
                                val ns = (scale.value * z).coerceIn(0.85f, 7f)
                                val k = ns / scale.value
                                val nx = c.x - (c.x - ox.value) * k + pan.x
                                val ny = c.y - (c.y - oy.value) * k + pan.y
                                val (mx, my) = limits(ns)
                                // at the zoomed photo's edge, a horizontal pull goes to the pager (next/previous photo)
                                if (!multi && ns <= 1.01f) { mode = 3 } else {
                                    scope.launch { scale.snapTo(ns); ox.snapTo(nx.coerceIn(-mx - 60f, mx + 60f)); oy.snapTo(ny.coerceIn(-my - 60f, my + 60f)) }
                                    ev.changes.forEach { it.consume() }
                                }
                            }
                            2 -> {
                                val ch = ev.changes.firstOrNull { it.id == down.id }
                                if (ch != null) { onVerticalDrag(ch.positionChange().y); ch.consume() }
                            }
                        }
                    }
                    when (mode) {
                        1 -> settle()
                        2 -> onVerticalEnd(tracker.calculateVelocity().y)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val img = bmp ?: miniImg
        if (img != null) Image(
            img, null,
            Modifier.fillMaxSize().graphicsLayer { scaleX = scale.value; scaleY = scale.value; translationX = ox.value; translationY = oy.value }
                .then(if (bmp == null) Modifier.blur(10.dp) else Modifier),
            contentScale = ContentScale.Fit,
        )
        if (bmp == null) DownloadProgress(size.photo, Modifier.align(Alignment.Center))
    }
}

