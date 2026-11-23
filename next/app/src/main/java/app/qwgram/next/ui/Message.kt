package app.qwgram.next.ui

import androidx.compose.animation.core.Animatable
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
import androidx.compose.material3.toShape
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
    onLong: () -> Unit,
    onDouble: () -> Unit,
    onSwipeReply: () -> Unit,
    onPhoto: (TdApi.Photo) -> Unit,
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
    val swipe = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()
    var armed by remember { mutableStateOf(false) }

    Row(
        modifier.fillMaxWidth().padding(top = if (firstInRun) 6.dp else 1.dp, bottom = 1.dp)
            .pointerInput(m.id) {
                if (!Prefs.swipeReply.value) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (armed) onSwipeReply()
                        armed = false
                        scope.launch { swipe.animateTo(0f) }
                    },
                    onDragCancel = { armed = false; scope.launch { swipe.animateTo(0f) } },
                ) { change, drag ->
                    val v = (swipe.value + drag).coerceIn(-90.dp.toPx(), 0f)
                    if (v < 0f) change.consume()
                    scope.launch { swipe.snapTo(v) }
                    val now = v <= -56.dp.toPx()
                    if (now && !armed) haptic(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    armed = now
                }
            },
        horizontalArrangement = if (out) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (showAvatar) {
            if (lastInRun) SenderAvatar(m.senderId, 34.dp, Modifier.clickable { senderChatOrUser(m)?.let { Nav.push(Route.Profile(it)) } })
            else Spacer(Modifier.width(34.dp))
            Spacer(Modifier.width(6.dp))
        }
        Box(Modifier.offset { IntOffset(swipe.value.roundToInt(), 0) }) {
            Bubble(m, grp, chat, group && !out, reply, onLong, onDouble, onPhoto)
        }
        if (swipe.value < -8f) {
            val p = (-swipe.value / 56.dp.value / 3f).coerceIn(0f, 1f)
            Box(
                Modifier.align(Alignment.CenterVertically).offset { IntOffset(swipe.value.roundToInt() + 8, 0) }
                    .graphicsLayer { scaleX = p; scaleY = p; alpha = p }
                    .size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.Reply, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer) }
        }
    }
}

private fun senderChatOrUser(m: TdApi.Message): Long? = when (val s = m.senderId) {
    is TdApi.MessageSenderChat -> s.chatId
    is TdApi.MessageSenderUser -> TdStore.chats.values.firstOrNull { TdStore.privateUserId(it) == s.userId }?.id
    else -> null
}

@Composable
private fun Bubble(
    m: TdApi.Message, grp: Grp, chat: TdApi.Chat, showName: Boolean, reply: TdApi.Message?,
    onLong: () -> Unit, onDouble: () -> Unit, onPhoto: (TdApi.Photo) -> Unit,
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
        out && style == "filled" -> cs.primary to cs.onPrimary
        out && style == "outlined" -> cs.surface to cs.onSurface
        out -> cs.primaryContainer to cs.onPrimaryContainer
        style == "outlined" -> cs.surface to cs.onSurface
        else -> cs.surfaceContainerHigh to cs.onSurface
    }
    val c = m.content
    val textOnly = c is TdApi.MessageText || c is TdApi.MessageAnimatedEmoji
    val bigEmoji = Prefs.bigEmoji.value && ((c is TdApi.MessageText && c.text.entities.isEmpty() && emojiOnly.matches(c.text.text.trim())) || c is TdApi.MessageAnimatedEmoji)
    val bare = c is TdApi.MessageSticker || bigEmoji
    val media = c is TdApi.MessagePhoto || c is TdApi.MessageVideo || c is TdApi.MessageAnimation
    val maxW = (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp * Prefs.bubbleWidth.value / 100f).dp
    val gestures = Modifier.pointerInput(m.id) {
        detectTapGestures(onLongPress = { onLong() }, onDoubleTap = { onDouble() })
    }

    if (bare) {
        Column(gestures.widthIn(max = maxW), horizontalAlignment = if (out) Alignment.End else Alignment.Start) {
            if (c is TdApi.MessageSticker) StickerView(c.sticker)
            else Text(
                (c as? TdApi.MessageText)?.text?.text ?: (c as TdApi.MessageAnimatedEmoji).emoji,
                fontSize = (Prefs.textSize.value * 3).sp, lineHeight = (Prefs.textSize.value * 3.4).sp,
            )
            Reactions(m, out)
            Surface(shape = CircleShape, color = cs.surfaceContainerHigh) { Meta(m, chat, cs.onSurfaceVariant, Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) }
        }
        return
    }

    Surface(
        shape = shape, color = bg, contentColor = fg,
        border = if (style == "outlined") androidx.compose.foundation.BorderStroke(if (out) 1.5.dp else 1.dp, if (out) cs.primary else cs.outlineVariant) else null,
        modifier = gestures.widthIn(max = maxW),
    ) {
        Column(Modifier.padding(if (media) 3.dp else 0.dp)) {
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
            if (reply != null || m.replyTo is TdApi.MessageReplyToMessage) ReplyQuote(reply, m, fg, pad.padding(top = 6.dp))
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
            val cap = Texts.caption(c)
            if (cap != null && cap.text.isNotEmpty()) {
                Box(pad.padding(top = if (textOnly && !showName && m.forwardInfo == null && m.replyTo == null) 8.dp else 6.dp)) {
                    RichText(cap, fg)
                }
            } else if (!textOnly && c !is TdApi.MessagePhoto && c !is TdApi.MessageVideo && c !is TdApi.MessageAnimation && c !is TdApi.MessageVideoNote &&
                c !is TdApi.MessageVoiceNote && c !is TdApi.MessageAudio && c !is TdApi.MessageDocument && c !is TdApi.MessageLocation &&
                c !is TdApi.MessageContact && c !is TdApi.MessagePoll && c !is TdApi.MessageCall) {
                Text(Texts.preview(m), style = MaterialTheme.typography.bodyMedium, modifier = pad.padding(top = 8.dp), color = fg.copy(alpha = 0.8f))
            }
            Reactions(m, out, pad)
            Meta(m, chat, fg.copy(alpha = 0.72f), Modifier.align(Alignment.End).padding(start = 12.dp, end = if (media) 8.dp else 10.dp, bottom = 6.dp, top = 2.dp))
        }
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

@Composable
private fun Reactions(m: TdApi.Message, out: Boolean, modifier: Modifier = Modifier) {
    val list = m.interactionInfo?.reactions?.reactions ?: return
    if (list.isEmpty()) return
    val cs = MaterialTheme.colorScheme
    FlowRow(modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        list.forEach { r ->
            val emoji = (r.type as? TdApi.ReactionTypeEmoji)?.emoji ?: "⭐"
            Surface(
                onClick = { if (r.type is TdApi.ReactionTypeEmoji) react(m, emoji) },
                shape = CircleShape,
                color = if (r.isChosen) cs.primary else if (out) cs.surface.copy(alpha = 0.55f) else cs.secondaryContainer,
                contentColor = if (r.isChosen) cs.onPrimary else cs.onSecondaryContainer,
            ) {
                Text("$emoji ${r.totalCount}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp))
            }
        }
    }
}

@Composable
fun RichText(ft: TdApi.FormattedText, color: Color) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val linkColor = if (color == cs.onPrimary) cs.onPrimary else cs.primary
    val annotated = remember(ft, linkColor) { formatted(ft, linkColor, ctx) }
    Text(annotated, style = MaterialTheme.typography.bodyLarge.copy(fontSize = Prefs.textSize.value.sp, lineHeight = (Prefs.textSize.value * 1.35f).sp), color = color)
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
        val img = bmp ?: mini
        if (img != null) Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (bmp == null) DownloadProgress(size.photo, Modifier.align(Alignment.Center))
    }
}

@Composable
private fun DownloadProgress(file: TdApi.File, modifier: Modifier) {
    val f = TdStore.files[file.id] ?: file
    val total = maxOf(f.size, f.expectedSize)
    val p = if (total > 0) f.local.downloadedSize.toFloat() / total else 0f
    Box(modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator(progress = { p }, modifier = Modifier.size(36.dp), color = Color.White, trackColor = Color.White.copy(alpha = 0.25f))
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
        if (bmp != null) Image(bmp, s.emoji, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        else Text(s.emoji, fontSize = 64.sp)
    }
}

@Composable
private fun VoiceView(file: TdApi.File, duration: Int, waveform: ByteArray, fg: Color, modifier: Modifier) {
    val playing = Player.fileId == file.id && Player.playing
    val progress = if (Player.fileId == file.id) Player.progress else 0f
    val bars = remember(waveform) { decodeWaveform(waveform, 36) }
    Row(modifier.widthIn(min = 220.dp), verticalAlignment = Alignment.CenterVertically) {
        PlayButton(playing, fg) { Player.toggle(file) }
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

@Composable
private fun PlayButton(playing: Boolean, fg: Color, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = if (playing) RoundedCornerShape(14.dp) else CircleShape, color = fg, contentColor = if (fg == cs.onPrimary) cs.primary else cs.surface, modifier = Modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Пауза" else "Слушать") }
    }
}

@Composable
private fun AudioView(a: TdApi.Audio, fg: Color, modifier: Modifier) {
    val playing = Player.fileId == a.audio.id && Player.playing
    Row(modifier.widthIn(min = 220.dp), verticalAlignment = Alignment.CenterVertically) {
        PlayButton(playing, fg) { Player.toggle(a.audio) }
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
        Box(Modifier.size(46.dp).clip(androidx.compose.material3.MaterialShapes.Sunny.toShape()).background(MaterialTheme.colorScheme.tertiaryContainer), contentAlignment = Alignment.Center) {
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
fun PhotoViewer(p: TdApi.Photo, onDismiss: () -> Unit) {
    val size = p.sizes.maxByOrNull { it.width * it.height } ?: return
    val bmp = rememberFileBitmap(size.photo, maxSide = 2560)
    val miniImg = remember(p) { Images.mini(p.minithumbnail) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val state = rememberTransformableState { z, pan, _ ->
        scale = (scale * z).coerceIn(1f, 5f)
        offset = if (scale > 1f) offset + pan else androidx.compose.ui.geometry.Offset.Zero
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black).transformable(state)
                .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }, onDoubleTap = { scale = if (scale > 1f) 1f else 2.5f; offset = androidx.compose.ui.geometry.Offset.Zero }) },
            contentAlignment = Alignment.Center,
        ) {
            val img = bmp ?: miniImg
            if (img != null) Image(
                img, null,
                Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
                contentScale = ContentScale.Fit,
            )
            if (bmp == null) DownloadProgress(size.photo, Modifier.align(Alignment.Center))
        }
    }
}
