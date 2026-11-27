package app.qwgram.next.ui

import androidx.compose.animation.togetherWith

import androidx.compose.foundation.layout.offset

import androidx.compose.foundation.layout.imePadding

import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.LocationOn
import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Poll
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** one item of the device gallery */
class DeviceMedia(val uri: Uri, val isVideo: Boolean, val durationMs: Long)

private fun mediaPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun hasMediaAccess(ctx: Context): Boolean {
    val granted = { p: String -> ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED }
    return when {
        Build.VERSION.SDK_INT >= 34 -> granted(Manifest.permission.READ_MEDIA_IMAGES) || granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        Build.VERSION.SDK_INT >= 33 -> granted(Manifest.permission.READ_MEDIA_IMAGES)
        else -> granted(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private suspend fun loadRecent(ctx: Context, limit: Int = 150): List<DeviceMedia> = withContext(Dispatchers.IO) {
    val out = ArrayList<DeviceMedia>()
    runCatching {
        val uri = MediaStore.Files.getContentUri("external")
        val proj = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.MEDIA_TYPE, MediaStore.Video.VideoColumns.DURATION)
        val sel = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}"
        ctx.contentResolver.query(uri, proj, sel, null, "${MediaStore.Files.FileColumns.DATE_ADDED} DESC")?.use { c ->
            while (c.moveToNext() && out.size < limit) {
                val id = c.getLong(0)
                val video = c.getInt(1) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val base = if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                out.add(DeviceMedia(ContentUris.withAppendedId(base, id), video, if (video) c.getLong(2) else 0L))
            }
        }
    }
    out
}

@Composable
private fun rememberThumb(uri: Uri): ImageBitmap? {
    val ctx = LocalContext.current
    var bmp by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        bmp = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= 29 && uri.scheme != "file") ctx.contentResolver.loadThumbnail(uri, android.util.Size(320, 320), null).asImageBitmap()
                else ctx.contentResolver.openInputStream(uri)?.use { i ->
                    android.graphics.BitmapFactory.decodeStream(i, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = 8 })
                }?.asImageBitmap()
            }.getOrNull()
        }
    }
    return bmp
}

/**
 * Telegram-style "send" sheet in M3 Expressive: a camera tile and your recent photos/videos in a grid (tap to pick,
 * numbered in order), the full system picker, file, music and poll as tonal shape buttons, and an extended
 * "Send N" button that springs in when something is picked. 2–10 photos/videos go as one album.
 */
@Composable
fun AttachGallerySheet(
    canPoll: Boolean,
    onDismiss: () -> Unit,
    onSend: (List<Uri>, String) -> Unit,
    onSystemPicker: () -> Unit,
    onFile: () -> Unit,
    onMusic: () -> Unit,
    onPoll: () -> Unit,
    onLocation: () -> Unit = {},
    onContact: () -> Unit = {},
    onDice: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var access by remember { mutableStateOf(hasMediaAccess(ctx)) }
    var media by remember { mutableStateOf<List<DeviceMedia>>(emptyList()) }
    val picked = remember { mutableStateListOf<Uri>() }
    var caption by remember { mutableStateOf("") }
    val edited = remember { androidx.compose.runtime.mutableStateMapOf<Uri, Uri>() }   // original → edited copy
    val isVideoUri = remember { androidx.compose.runtime.mutableStateMapOf<Uri, Boolean>() }
    fun isVideo(u: Uri) = isVideoUri[u] ?: media.firstOrNull { it.uri == u || edited[it.uri] == u }?.isVideo ?: false
    val askAccess = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { access = hasMediaAccess(ctx) }
    LaunchedEffect(access) { if (access) media = loadRecent(ctx) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> cameraUri?.let { if (ok) onSend(listOf(it), "") } }
    // Telegram-like sheet: opens half-way; scrolling the photos up first lifts it to full height, then scrolls them
    QSheet(onDismiss = onDismiss, overlay = { sh ->
        // picking something lifts the sheet fully
        LaunchedEffect(picked.isNotEmpty()) { if (picked.isNotEmpty()) sh.expand() }
            // "Send N": springs in from the bottom when something is picked
            androidx.compose.animation.AnimatedVisibility(
                picked.isNotEmpty(),
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp).graphicsLayer { alpha = sh.progress() },
                enter = scaleIn(Springs.spatial(), initialScale = 0.6f) + fadeIn(Springs.effects()),
                exit = scaleOut(Springs.exit(), targetScale = 0.6f) + fadeOut(Springs.exit()),
            ) {
                // Telegram: a caption field and the send button with the count, the pencil edits the last pick
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).imePadding(), verticalAlignment = Alignment.Bottom) {
                    androidx.compose.material3.Surface(
                        shape = RoundedCornerShape(28.dp), color = cs.surfaceContainerHigh, shadowElevation = 6.dp,
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    ) {
                        Row(Modifier.padding(start = 6.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            MorphIconButton(
                                Icons.Outlined.Edit, "Редактировать", {
                                    picked.lastOrNull()?.let { u ->
                                        val orig = edited.entries.firstOrNull { it.value == u }?.key ?: u
                                        openMediaEditor(u, isVideo(u)) { out -> val i = picked.indexOf(u); edited[orig] = out; isVideoUri[out] = isVideo(u); if (i >= 0) picked[i] = out }
                                    }
                                },
                                size = 44.dp, tint = cs.onSurfaceVariant,
                            )
                            Box(Modifier.weight(1f).padding(vertical = 16.dp, horizontal = 4.dp)) {
                                if (caption.isEmpty()) Text(if (picked.size > 1) "Подпись к альбому…" else "Добавить подпись…", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                                androidx.compose.foundation.text.BasicTextField(
                                    caption, { caption = it }, maxLines = 5,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(cs.primary),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box {
                        MorphBox(
                            onClick = { onSend(picked.toList(), caption) }, rest = 28.dp, pressed = 16.dp, elevation = 6.dp,
                            color = cs.primary, contentColor = cs.onPrimary, modifier = Modifier.size(56.dp),
                        ) { Icon(Icons.AutoMirrored.Filled.Send, "Отправить") }
                        // the count badge on the button's corner
                        androidx.compose.animation.AnimatedContent(picked.size, Modifier.align(Alignment.TopStart).offset(x = (-4).dp, y = (-4).dp), label = "cnt",
                            transitionSpec = { (scaleIn(Springs.fastSpatial(), initialScale = 0.4f) + fadeIn()).togetherWith(scaleOut(targetScale = 0.4f) + fadeOut()) }) { n ->
                            Box(Modifier.size(24.dp).clip(CircleShape).background(cs.surface).padding(2.dp).clip(CircleShape).background(cs.primaryContainer), contentAlignment = Alignment.Center) {
                                Text("$n", style = MaterialTheme.typography.labelMedium, color = cs.onPrimaryContainer)
                            }
                        }
                    }
                }
            }
    }) {
            Column(Modifier.fillMaxWidth()) {
                // quick actions: one connected group (M3) — big outer corners, small touching ones, an icon in a tinted circle
                val acts = buildList {
                    add(AttachAct("Галерея", Icons.Outlined.PhotoLibrary, cs.primaryContainer, cs.onPrimaryContainer, onSystemPicker))
                    add(AttachAct("Файл", Icons.Outlined.Description, cs.secondaryContainer, cs.onSecondaryContainer, onFile))
                    add(AttachAct("Музыка", Icons.Outlined.AudioFile, cs.tertiaryContainer, cs.onTertiaryContainer, onMusic))
                    if (canPoll) add(AttachAct("Опрос", Icons.Outlined.Poll, cs.primaryContainer, cs.onPrimaryContainer, onPoll))
                    add(AttachAct("Место", Icons.Outlined.LocationOn, cs.secondaryContainer, cs.onSecondaryContainer, onLocation))
                    add(AttachAct("Контакт", Icons.Outlined.Person, cs.tertiaryContainer, cs.onTertiaryContainer, onContact))
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    acts.forEachIndexed { k, a -> AttachTile(a, Seat(0, 1, k, acts.size), Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    GridCells.Fixed(3),
                    // the grid fills the sheet: it opens half-way, scrolling up expands it, then the photos scroll (Telegram)
                    Modifier.fillMaxWidth().fillMaxHeight(),
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item(key = "camera") {
                        MorphBox(
                            onClick = {
                                runCatching {
                                    val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
                                    val f = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
                                    val u = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
                                    cameraUri = u
                                    camera.launch(u)
                                }
                            },
                            rest = 20.dp, pressed = 32.dp, squish = 0.94f, color = cs.primaryContainer, contentColor = cs.onPrimaryContainer,
                            modifier = Modifier.aspectRatio(1f),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.PhotoCamera, null, Modifier.size(32.dp))
                                Spacer(Modifier.height(4.dp))
                                FitText("Камера", MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    if (!access) item(key = "access", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        MorphBox(
                            onClick = { askAccess.launch(mediaPermissions()) }, rest = 20.dp, pressed = 12.dp,
                            color = cs.surfaceContainerHigh, modifier = Modifier.aspectRatio(2f),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                                Icon(Icons.Outlined.Image, null)
                                Spacer(Modifier.height(6.dp))
                                FitText("Показать фото с телефона", MaterialTheme.typography.titleSmall, maxLines = 2)
                            }
                        }
                    }
                    itemsIndexed(media, key = { _, m -> m.uri.toString() }) { _, m ->
                        val shown = edited[m.uri] ?: m.uri
                        val order = picked.indexOf(shown)
                        GalleryTile(DeviceMedia(shown, m.isVideo, m.durationMs), order,
                            onEdit = { openMediaEditor(shown, m.isVideo) { out -> val i = picked.indexOf(shown); edited[m.uri] = out; isVideoUri[out] = m.isVideo; if (i >= 0) picked[i] = out else if (picked.size < 10) picked.add(out) } },
                        ) { if (order >= 0) picked.remove(shown) else if (picked.size < 10) picked.add(shown) }
                    }
                }
            }
    }
}

private class AttachAct(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val bg: Color, val fg: Color, val onClick: () -> Unit)

@Composable
private fun AttachTile(a: AttachAct, seat: Seat, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    ConnectedTile(seat, onClick = a.onClick, modifier = modifier.height(84.dp), big = 26.dp, small = 8.dp, color = cs.surfaceContainerHigh) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(a.bg), contentAlignment = Alignment.Center) { Icon(a.icon, null, Modifier.size(22.dp), tint = a.fg) }
            Spacer(Modifier.height(6.dp))
            FitText(a.title, MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun AttachShape(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, shape: androidx.graphics.shapes.RoundedPolygon, bg: Color, fg: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val p by animateFloatAsState(if (pressed) 1f else 0f, if (pressed) spring(0.8f, 900f) else spring(0.45f, 380f), label = "ap")
    val morph = remember(shape) { androidx.graphics.shapes.Morph(shape, MaterialShapes.Circle) }
    Column(
        Modifier.clip(RoundedCornerShape(20.dp)).clickable(interaction, indication = null, onClick = onClick).padding(horizontal = 6.dp, vertical = 6.dp).width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.matchParentSize().graphicsLayer { rotationZ = 40f * p; val s = 1f - 0.08f * p; scaleX = s; scaleY = s }.clip(MorphShape(morph, p.coerceIn(0f, 1f))).background(bg))
            Icon(icon, null, tint = fg)
        }
        Spacer(Modifier.height(6.dp))
        FitText(title, MaterialTheme.typography.labelLarge)
    }
}

/** a gallery thumbnail: picked ones shrink into a rounded frame with their number, like Telegram */
@Composable
private fun GalleryTile(m: DeviceMedia, order: Int, onEdit: (() -> Unit)? = null, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val sel = order >= 0
    val inset by animateDpAsState(if (sel) 10.dp else 0.dp, spring(0.6f, 520f), label = "in")
    val r by animateDpAsState(if (sel) 18.dp else 6.dp, spring(0.6f, 520f), label = "r")
    val bmp = rememberThumb(m.uri)
    Box(
        Modifier.aspectRatio(1f).clip(RoundedCornerShape(6.dp)).background(if (sel) cs.primaryContainer else cs.surfaceContainerHigh)
            .combinedClickable(onLongClick = onEdit, onClick = onClick),
    ) {
        Box(Modifier.fillMaxSize().padding(inset.coerceAtLeast(0.dp)).clip(RoundedCornerShape(r.coerceAtLeast(0.dp))).background(cs.surfaceContainerHighest)) {
            if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (m.isVideo) Text(
                "%d:%02d".format(m.durationMs / 60000, (m.durationMs / 1000) % 60),
                style = MaterialTheme.typography.labelSmall, color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(5.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
        // edit (photo editor) on picked photos
        if (sel && onEdit != null) Box(
            Modifier.align(Alignment.BottomEnd).padding(6.dp).size(30.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).clickable(onClick = onEdit),
            contentAlignment = Alignment.Center,
        ) { Icon(androidx.compose.material.icons.Icons.Outlined.Edit, "Редактировать", tint = Color.White, modifier = Modifier.size(18.dp)) }
        // the pick circle: empty ring → numbered primary dot
        Box(
            Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape)
                .background(if (sel) cs.primary else Color.Black.copy(alpha = 0.25f))
                .border(2.dp, if (sel) cs.onPrimary else Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedVisibility(sel, enter = scaleIn(spring(0.5f, 600f)), exit = scaleOut()) {
                Text("${order + 1}", style = MaterialTheme.typography.labelMedium, color = cs.onPrimary)
            }
        }
    }
}
