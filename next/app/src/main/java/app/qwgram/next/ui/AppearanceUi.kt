package app.qwgram.next.ui

import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Dock
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Interests
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import app.qwgram.next.KeepAliveService
import app.qwgram.next.Prefs
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures

// ------------------------------------------------------------------ preview frames

/** titled card that frames one focused preview (palette, list, bubbles, panels, motion, type) */
@Composable
fun PreviewCard(label: String, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)) {
        Row(Modifier.padding(start = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(cs.primary))
            Spacer(Modifier.width(8.dp))
            Text("Предпросмотр · $label", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
        }
        Surface(shape = RoundedCornerShape(28.dp), color = cs.surface, border = androidx.compose.foundation.BorderStroke(1.dp, cs.outlineVariant), modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.padding(12.dp)) { content() }
        }
    }
}

/** the scheme as tonal swatches + the real components drawn in it */
@Composable
fun ThemePreview() = PreviewCard("палитра и компоненты") {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                Triple(cs.primary, cs.onPrimary, "Осн."), Triple(cs.secondary, cs.onSecondary, "Втор."), Triple(cs.tertiary, cs.onTertiary, "Трет."),
                Triple(cs.primaryContainer, cs.onPrimaryContainer, "Конт."), Triple(cs.surfaceContainerHigh, cs.onSurface, "Пов."),
            ).forEachIndexed { i, (bg, fg, t) ->
                val shape = listOf(MaterialShapes.Cookie9Sided, MaterialShapes.Clover4Leaf, MaterialShapes.Sunny, MaterialShapes.Pentagon, MaterialShapes.Circle)[i]
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(44.dp).clip(shape.asShape()).background(bg), contentAlignment = Alignment.Center) {
                        Text("Aa", color = fg, style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(t, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                }
            }
        }
        var sel by remember { mutableStateOf(1) }
        ButtonGroupRow(
            listOf(
                GroupButton(Icons.Outlined.Edit, null, sel == 0) { sel = 0 },
                GroupButton(null, "Кнопка", sel == 1) { sel = 1 },
                GroupButton(Icons.Outlined.Notifications, null, sel == 2) { sel = 2 },
            ),
            height = 48.dp,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var on by remember { mutableStateOf(true) }
            QSwitch(on) { on = it }
            FilterChip(selected = on, onClick = { on = !on }, label = { Text("Чип") })
            Spacer(Modifier.weight(1f))
            ContainedLoadingIndicator(Modifier.size(40.dp))
        }
    }
}

@Composable
fun ListPreview() = PreviewCard("список чатов") { Preview(list = true, chat = false, framed = false) }

@Composable
fun BubblesPreview() = PreviewCard("сообщения") { Preview(list = false, chat = true, framed = false) }

/** a miniature of the bottom area: floating pill + separate FAB, or the docked bar */
@Composable
fun NavPreview() = PreviewCard("панели") {
    val cs = MaterialTheme.colorScheme
    val floating = Prefs.navStyle.value == "floating"
    Column(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(20.dp)).background(cs.surfaceContainerLow)) {
        // a few fake list lines
        Column(Modifier.padding(12.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(2) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).clip(avatarShape()).background(cs.secondaryContainer))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.height(8.dp).fillMaxWidth(if (it == 0) 0.6f else 0.45f).clip(CircleShape).background(cs.outlineVariant))
                }
            }
        }
        if (floating) Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = cs.surfaceContainerHigh, shadowElevation = 2.dp) {
                Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Surface(shape = CircleShape, color = cs.secondaryContainer) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Outlined.Chat, null, Modifier.size(16.dp))
                            if (Prefs.navLabels.value) { Spacer(Modifier.width(4.dp)); Text("Чаты", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                    Icon(Icons.Outlined.Person, null, Modifier.padding(6.dp).size(16.dp))
                    Icon(Icons.Outlined.Settings, null, Modifier.padding(6.dp).size(16.dp))
                }
            }
            if (Prefs.fabStyle.value != "none") {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(40.dp).clip(fabPolygon().asShape()).background(cs.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp), tint = cs.onPrimaryContainer)
                }
            }
        } else Row(Modifier.fillMaxWidth().background(cs.surfaceContainer).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(Icons.AutoMirrored.Outlined.Chat, Icons.Outlined.Person, Icons.Outlined.Settings).forEachIndexed { i, ic ->
                Box(Modifier.clip(CircleShape).background(if (i == 0) cs.secondaryContainer else Color.Transparent).padding(horizontal = 14.dp, vertical = 4.dp)) { Icon(ic, null, Modifier.size(18.dp)) }
            }
        }
    }
}

/** tap the shape: it morphs on the current springs; press the buttons to feel the press morph */
@Composable
fun MotionPreview() = PreviewCard("анимации — нажмите") {
    val cs = MaterialTheme.colorScheme
    val shapes = remember { listOf(MaterialShapes.Cookie9Sided, MaterialShapes.Sunny, MaterialShapes.Clover4Leaf, MaterialShapes.Pill, MaterialShapes.Flower) }
    var idx by remember { mutableStateOf(0) }
    val p = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val morph = remember(idx) { Morph(shapes[(idx + shapes.size - 1) % shapes.size], shapes[idx]) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(84.dp).graphicsLayer { shape = MorphShape(morph, p.value); clip = true }.background(cs.tertiaryContainer)
                    .pointerInput(Unit) { detectTapGestures { idx = (idx + 1) % shapes.size; scope.launch { p.snapTo(0f); p.animateTo(1f, Springs.spatial()) } } },
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Морфинг формы", style = MaterialTheme.typography.titleSmall)
                Text("Пружина и скорость — из настроек ниже", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
        }
        var sel by remember { mutableStateOf(0) }
        ButtonGroupRow(listOf(
            GroupButton(Icons.Outlined.Palette, null, sel == 0) { sel = 0 },
            GroupButton(null, "Нажмите", sel == 1) { sel = 1 },
            GroupButton(Icons.Outlined.Animation, null, sel == 2) { sel = 2 },
            GroupButton(Icons.Outlined.Interests, null, sel == 3) { sel = 3 },
        ), height = 52.dp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LoadingIndicator(Modifier.size(40.dp))
            LinearWavyProgressIndicator(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun TypePreview() = PreviewCard("шрифт") {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Aa Бб 123", style = MaterialTheme.typography.displaySmall)
        Text("Заголовок раздела", style = MaterialTheme.typography.titleLarge)
        Text("Обычный текст сообщения: так будут выглядеть ваши переписки.", style = MaterialTheme.typography.bodyLarge.copy(fontSize = Prefs.textSize.value.sp))
        Text("Подпись и время · 12:34", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
    }
}

// ------------------------------------------------------------------ settings: tiles & pages

/**
 * Two big tiles: Appearance and qwgram features. Pressing one: the tile settles in a little, its corners soften,
 * and the icon's shape blooms into a circle while turning — then springs back. Same height, one-line titles.
 */
@Composable
fun QuickTiles2() {
    val cs = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
        BloomTile("Оформление", Icons.Outlined.Palette, MaterialShapes.Cookie9Sided, cs.primaryContainer, cs.onPrimaryContainer, cs.primary, cs.onPrimary, Modifier.weight(1f)) { Nav.push(Route.Page("appearance")) }
        BloomTile("Функции", Icons.Outlined.AutoAwesome, MaterialShapes.SoftBurst, cs.tertiaryContainer, cs.onTertiaryContainer, cs.tertiary, cs.onTertiary, Modifier.weight(1f)) { Nav.push(Route.Page("features")) }
    }
}

@Composable
private fun BloomTile(
    title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, shape: androidx.graphics.shapes.RoundedPolygon,
    bg: Color, fg: Color, iconBg: Color, iconFg: Color, modifier: Modifier, onClick: () -> Unit,
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val f = pressFactor()
    val k = Prefs.motionSpeed.value * Prefs.motionSpeed.value
    val bouncy = androidx.compose.animation.core.spring<Float>(dampingRatio = 0.5f, stiffness = 380f * k)
    val p by androidx.compose.animation.core.animateFloatAsState(if (pressed) 1f else 0f, if (pressed) androidx.compose.animation.core.spring(0.8f, 900f * k) else bouncy, label = "bloom")
    val morph = remember(shape) { androidx.graphics.shapes.Morph(shape, MaterialShapes.Circle) }
    val radius = (32.dp + 12.dp * p * f).rs()
    Box(
        modifier.fillMaxHeight().heightIn(min = 120.dp)
            .graphicsLayer {
                val sc = 1f - 0.04f * p * f
                scaleX = sc; scaleY = sc
                this.shape = RoundedCornerShape(radius.coerceAtLeast(0.dp)); clip = true
            }
            .background(bg)
            .drawWithContent { drawContent(); if (p > 0.001f) drawRect(fg.copy(alpha = 0.08f * p)) }   // state layer, inside the tile
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.matchParentSize().graphicsLayer {
                    rotationZ = 45f * p * f
                    val sc = 1f + 0.08f * p * f
                    scaleX = sc; scaleY = sc
                }.clip(MorphShape(morph, (p * f).coerceIn(0f, 1f))).background(iconBg))
                Icon(icon, null, tint = iconFg, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            FitText(title, MaterialTheme.typography.titleMedium, color = fg)
        }
    }
}

@Composable
fun AppearanceHub2() = PageScaffold("Внешний вид") {
    item {
        Text(
            "Всё оформление собрано по разделам. В каждом — свой предпросмотр, изменения видны сразу.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
        )
    }
    group("Цвет и форма") {
        nav("Тема и цвета", "Светлая или тёмная, акцент, палитра", Icons.Outlined.Palette, 0) { Nav.push(Route.Page("ap-theme")) }
        nav("Формы и аватарки", "26 форм, размеры, скругления", Icons.Outlined.Interests, 2) { Nav.push(Route.Page("ap-shapes")) }
        nav("Шрифт и масштаб", "Начертание, жирность, размер", Icons.Outlined.TextFields) { Nav.push(Route.Page("ap-text")) }
        nav("Эмодзи", "Twemoji, Noto или системные", Icons.Outlined.EmojiEmotions) { Nav.push(Route.Page("ap-emoji")) }
        nav("Размытие фона", if (Prefs.blurOn.value) "Включено · стекло, переходы, качество" else "Стеклянные панели и прогрессивное размытие", Icons.Outlined.BlurOn) { Nav.push(Route.Page("ap-blur")) }
    }
    group("Экраны") {
        nav("Список чатов", "Строки, счётчики, свайпы, архив", Icons.Outlined.ViewAgenda) { Nav.push(Route.Page("ap-list")) }
        nav("Папки", "Кнопки, вкладки или чипы", Icons.Outlined.FolderOpen) { Nav.push(Route.Page("ap-folders")) }
        nav("Чат и сообщения", "Пузыри, цвета, фон, жесты", Icons.Outlined.ChatBubbleOutline, 0) { Nav.push(Route.Page("ap-chat")) }
        nav("Панели и кнопки", "Нижняя панель, «Написать», меню", Icons.Outlined.Dock) { Nav.push(Route.Page("ap-nav")) }
    }
    group("Движение") {
        nav("Анимации и жесты", "Пружины, морфинг, переходы", Icons.Outlined.Animation, 2) { Nav.push(Route.Page("ap-motion")) }
    }
    group(footer = "Тема, шрифт, формы, панели и анимации вернутся к стандартным. Функции qwgram и данные не затрагиваются.") {
        action("Сбросить оформление", "Вернуть всё как было", Icons.Outlined.RestartAlt, danger = true) { askReset = true }
    }
    item {
        if (askReset) androidx.compose.material3.AlertDialog(
            onDismissRequest = { askReset = false },
            icon = { Icon(Icons.Outlined.RestartAlt, null) },
            title = { WindowBlurBehind();  Text("Сбросить оформление?") },
            text = { Text("Все настройки внешнего вида вернутся к стандартным.") },
            confirmButton = { QTextButton(onClick = { askReset = false; Prefs.resetAppearance() }) { Text("Сбросить", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { QTextButton(onClick = { askReset = false }) { Text("Отмена") } },
        )
    }
}

private var askReset by androidx.compose.runtime.mutableStateOf(false)

@Composable
fun ListPage2() = PageScaffold("Список чатов") {
    item { ListPreview() }
    group("Строки", footer = "Плотность меняет высоту строк; превью — сколько строк текста видно под именем.") {
        segmented("Стиль", Prefs.listStyle, listOf("flat" to "Сплошной", "cards" to "Карточки"))
        segmented("Плотность", Prefs.density, listOf(0 to "Плотно", 1 to "Обычно", 2 to "Свободно"))
        segmented("Строк превью", Prefs.previewLines, listOf(1 to "Одна", 2 to "Две"))
        sliderInt("Размер аватарок", Prefs.avatarSize, 40..64, 2) { "$it dp" }
        switch("Разделители", Prefs.separators, "Тонкие линии между чатами")
    }
    group("Счётчики") {
        segmented("Непрочитанные", Prefs.badgeStyle, listOf("filled" to "Заливка", "tonal" to "Тон", "dot" to "Точка"))
        switch("Значок «в сети»", Prefs.showOnlineDot)
    }
    group("Верх экрана") {
        segmented("Вид", Prefs.topBar, listOf("search" to "Поиск", "title" to "Заголовок"))
        pick("Текст заголовка", Prefs.titleText, listOf("qwgram" to "qwgram", "name" to "Ваше имя", "chats" to "Чаты", "folder" to "Название папки"))
    }
    group("Жесты", footer = "С действиями: свайп влево — в архив, вправо — прочитано. Папки тогда переключаются нажатием.") {
        segmented("Свайп по чату", Prefs.listSwipe, listOf("folders" to "Папки", "actions" to "Действия"))
    }
    group("Архив", footer = "Скрытый архив открывается, если потянуть список вниз.") {
        switch("Показывать архив", Prefs.showArchiveRow)
        switch("Скрыть над списком", Prefs.archiveHidden)
    }
}

@Composable
fun ChatStylePage2() = PageScaffold("Чат и сообщения") {
    item { BubblesPreview() }
    group("Пузыри") {
        segmented("Стиль", Prefs.bubbleStyle, listOf("tonal" to "Тон", "filled" to "Заливка", "outlined" to "Контур"))
        segmented("Цвет исходящих", Prefs.outBubble, listOf("primary" to "Основной", "secondary" to "Второй", "tertiary" to "Третий"))
        sliderInt("Скругление", Prefs.bubbleRadius, 4..28, 2) { "$it dp" }
        sliderInt("Ширина", Prefs.bubbleWidth, 60..95, 5) { "$it%" }
        switch("Хвостик у пузыря", Prefs.bubbleTail)
        switch("Аватарки в группах", Prefs.groupAvatars)
    }
    group("Текст") {
        sliderInt("Размер текста", Prefs.textSize, 12..22, 1) { "$it sp" }
        switch("Крупные эмодзи", Prefs.bigEmoji, "Сообщение из одних эмодзи — без пузыря")
        switch("Секунды во времени", Prefs.seconds)
    }
    item { GroupHeader("Обои") }
    item { WallpaperCard() }
    group(footer = "Свои обои для одного чата: меню ⋮ в чате → «Обои чата».") {
        pick("Фон чатов", Prefs.wallpaper, listOf("plain" to "Обычный", "tonal" to "Тональный", "shapes" to "Фигуры", "dots" to "Точки", "image" to "Своё фото"))
        switch("Обои из Telegram", Prefs.tgWallpapers, "Если собеседник поставил обои для чата")
        if (Prefs.wallpaperImage.value.isNotEmpty()) {
            switch("Фото на весь интерфейс", Prefs.wallpaperEverywhere, "Список чатов, настройки и все экраны")
            sliderFloat("Затемнение фото", Prefs.wallpaperDim, 0f..0.9f, 8) { "${(it * 100).toInt()}%" }
        }
    }
    group("Жесты") {
        pick("Двойной тап", Prefs.doubleTap, listOf("react" to "Реакция", "reply" to "Ответить", "none" to "Ничего"))
        pick("Быстрая реакция", Prefs.doubleTapEmoji, listOf("❤️" to "❤️", "👍" to "👍", "🔥" to "🔥", "😂" to "😂", "🎉" to "🎉"))
        switch("Ответ свайпом влево", Prefs.swipeReply)
        switch("Отправка по Enter", Prefs.sendByEnter)
    }
}

@Composable
fun NavPage2() = PageScaffold("Панели и кнопки") {
    item { NavPreview() }
    group("Кнопка на панели", footer = "«Поиск» вырастает в строку поиска прямо из кнопки.") {
        segmented("Действие", Prefs.fabAction, listOf("compose" to "Написать", "search" to "Поиск"))
    }
    group("Нижняя панель") {
        segmented("Вид", Prefs.navStyle, listOf("floating" to "Плавающая", "standard" to "Обычная"))
        switch("Подписи", Prefs.navLabels)
        switch("Вкладка «Контакты»", Prefs.showContactsTab)
    }
    group("Кнопка «Написать»", footer = "Нажатие открывает меню: сообщение, группа, канал, контакт.") {
        segmented("Показывать", Prefs.fabStyle, listOf("extended" to "Да", "none" to "Нет"))
        segmented("Форма", Prefs.fabShape, listOf("cookie" to "Печенье", "sunny" to "Солнце", "circle" to "Круг", "square" to "Квадрат"))
    }
    group("Скругление", footer = "Одно значение для кнопок, панелей, шапок и плиток во всём приложении.") {
        sliderFloat("Скругление интерфейса", Prefs.cornerScale, 0f..2f, 19) { if (it < 0.05f) "Квадратное" else "${(it * 100).toInt()}%" }
    }
    group("Шапки и панели", footer = "Плавающие: заголовок лежит на растворении, кнопки и папки — отдельные круглые кнопки с морфингом. Выключите, чтобы вернуть обычные прямоугольные панели везде.") {
        switch("Плавающие шапки и панели", Prefs.floatingBars)
        switch("Заголовок по центру", Prefs.centerTitle, "Во всех шапках")
    }
    group("Меню") {
        switch("Боковое меню", Prefs.drawer, "Открывается свайпом вправо по списку")
    }
}

@Composable
fun MotionPage2() = PageScaffold("Анимации и жесты") {
    item { MotionPreview() }
    group("Движение", footer = "Выразительное — пружины с лёгким отскоком (Material 3 Expressive). Спокойное — без отскока.") {
        segmented("Стиль", Prefs.motion, listOf("expressive" to "Живое", "standard" to "Спокойное", "off" to "Выкл"))
        sliderFloat("Скорость", Prefs.motionSpeed, 0.5f..2f, 5) { "%.2f×".format(it).replace(",00", "").replace(".00", "") }
    }
    group("Кнопки", footer = "Насколько меняется форма кнопки, пока вы её держите.") {
        segmented("Морфинг при нажатии", Prefs.pressMorph, listOf("off" to "Нет", "soft" to "Мягко", "normal" to "Обычно", "strong" to "Сильно"))
    }
    group("Переходы") {
        segmented("Открытие экранов", Prefs.screenAnim, listOf("slide" to "Сдвиг", "scale" to "Масштаб", "fade" to "Плавно"))
    }
    group("Жесты") {
        switch("Предиктивный «назад»", Prefs.predictiveBack, "Экран сдвигается, пока вы тянете")
        switch("Тактильный отклик", Prefs.haptics)
    }
}

@Composable
fun TextPage2() = PageScaffold("Шрифт и масштаб") {
    item { TypePreview() }
    item { GroupHeader("Шрифт") }
    item { FontCards() }
    group("Жирность", footer = "Работает для любого шрифта. Google Sans меняет толщину плавно, остальные — ступенями.") {
        sliderInt("Обычный текст", Prefs.bodyWeight, 300..700, 50) { weightName(it) }
        sliderInt("Заголовки", Prefs.headWeight, 400..900, 50) { weightName(it) }
    }
    group("Масштаб") {
        sliderFloat("Размер интерфейса", Prefs.uiScale, 0.85f..1.15f, 5) { "${(it * 100).toInt()}%" }
        sliderInt("Текст сообщений", Prefs.textSize, 12..22, 1) { "$it sp" }
    }
}

private fun weightName(w: Int) = when {
    w < 350 -> "Тонкий"; w < 450 -> "Обычный"; w < 550 -> "Средний"; w < 650 -> "Полужирный"; w < 750 -> "Жирный"; w < 850 -> "Очень жирный"; else -> "Чёрный"
} + " · $w"

/** font picker: each card shows "Аа" in its own font, the selected one morphs into a pill-ish primary container */
@Composable
private fun FontCards() {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val opts = listOf("google" to "Google Sans", "system" to "Системный", "serif" to "С засечками", "mono" to "Моноширинный")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        opts.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (k, title) ->
                    val sel = Prefs.font.value == k
                    val fam = remember(k) {
                        when (k) { "serif" -> androidx.compose.ui.text.font.FontFamily.Serif; "mono" -> androidx.compose.ui.text.font.FontFamily.Monospace
                            else -> Fonts.family(ctx, k, "system") ?: androidx.compose.ui.text.font.FontFamily.Default }
                    }
                    val bg by animateColorAsState(if (sel) cs.primaryContainer else cs.surfaceContainerHigh, Springs.effects(), label = "fb")
                    MorphBox(
                        onClick = { Prefs.font.value = k }, rest = if (sel) 32.dp else 18.dp, pressed = 10.dp,
                        color = bg, contentColor = if (sel) cs.onPrimaryContainer else cs.onSurface, contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f).heightIn(min = 96.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(10.dp)) {
                            Text("Аа", fontFamily = fam, fontSize = 30.sp, fontWeight = androidx.compose.ui.text.font.FontWeight(Prefs.headWeight.value))
                            Text(title, style = MaterialTheme.typography.labelLarge, fontFamily = fam, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

/** the own photo: preview tile with pick / remove */
@Composable
private fun WallpaperCard() {
    val cs = MaterialTheme.colorScheme
    val pick = rememberWallpaperPicker("global") { path -> Prefs.wallpaperImage.value = path; Prefs.wallpaper.value = "image" }
    val bmp = rememberPathBitmap(Prefs.wallpaperImage.value, 800)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MorphBox(
            onClick = pick, rest = 28.dp, pressed = 16.dp, color = cs.surfaceContainerHigh, contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f).height(150.dp),
        ) {
            if (bmp != null) Box(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp.rs()))) { PhotoWallpaper(bmp, Prefs.wallpaperDim.value * 0.6f) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                Icon(Icons.Outlined.AddPhotoAlternate, null)
                Spacer(Modifier.height(6.dp))
                Text(if (bmp == null) "Выбрать фото" else "Заменить фото", style = MaterialTheme.typography.titleSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        if (bmp != null) MorphBox(
            onClick = { Prefs.wallpaperImage.value = ""; Prefs.wallpaperEverywhere.value = false; if (Prefs.wallpaper.value == "image") Prefs.wallpaper.value = "shapes" },
            rest = 28.dp, pressed = 16.dp, color = cs.errorContainer, contentColor = cs.onErrorContainer, contentAlignment = Alignment.Center,
            modifier = Modifier.width(96.dp).height(150.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                Icon(Icons.Outlined.Delete, null)
                Spacer(Modifier.height(6.dp))
                Text("Убрать", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
fun EmojiPage() = PageScaffold("Эмодзи") {
    item { EmojiCards() }
    item {
        val ctx = LocalContext.current
        val needRestart = (Prefs.emojiStyle.value == "noto") != (app.qwgram.next.startEmoji == "noto")
        androidx.compose.animation.AnimatedVisibility(
            needRestart,
            enter = androidx.compose.animation.expandVertically(Springs.smooth()) + androidx.compose.animation.fadeIn(Springs.effects()),
            exit = androidx.compose.animation.shrinkVertically(Springs.smooth()) + androidx.compose.animation.fadeOut(Springs.exit()),
        ) {
            MorphBox(
                onClick = { restartApp(ctx) }, rest = 28.dp, pressed = 14.dp,
                color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Icon(Icons.Outlined.RestartAlt, null)
                    Spacer(Modifier.width(10.dp))
                    Text("Перезапустить qwgram, чтобы применить", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
    item {
        Text(
            "Twemoji — набор Twitter/X, цветной на Android 13+. Noto — набор Google, одинаковый на всех устройствах. Эмодзи Apple встроить нельзя: они принадлежат Apple.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun EmojiCards() {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val opts = listOf("twemoji" to "Twemoji", "noto" to "Noto", "system" to "Системные")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        opts.forEach { (k, title) ->
            val sel = Prefs.emojiStyle.value == k
            // Twemoji is previewed through its own font; Noto/system show the current system rendering
            val fam = remember(k) { if (k == "twemoji") Fonts.family(ctx, "system", "twemoji") else null }
            val bg by animateColorAsState(if (sel) cs.primaryContainer else cs.surfaceContainerHigh, Springs.effects(), label = "eb")
            MorphBox(
                onClick = { Prefs.emojiStyle.value = k }, rest = if (sel) 32.dp else 18.dp, pressed = 10.dp,
                color = bg, contentColor = if (sel) cs.onPrimaryContainer else cs.onSurface, contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).heightIn(min = 104.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                    Text("😀🔥", fontSize = 28.sp, fontFamily = fam)
                    Spacer(Modifier.height(6.dp))
                    Text(title, style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

fun restartApp(ctx: android.content.Context) {
    val i = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK) ?: return
    ctx.startActivity(i)
    Runtime.getRuntime().exit(0)
}

@Composable
fun FeaturesPage2() {
    val ctx = LocalContext.current
    PageScaffold("Функции qwgram") {
        item {
            Text(
                "Возможности, которых нет в официальном приложении. Всё работает на вашем устройстве.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
            )
        }
        group("Призрачный режим", footer = "Можно читать и писать, оставаясь незаметным.") {
            switch("Не показываться в сети", Prefs.ghostOffline, "Статус «в сети» не отправляется") { applyOnline(true) }
            switch("Не отправлять «печатает…»", Prefs.ghostTyping)
            switchState("Не отмечать прочитанным", !Prefs.markReadOnOpen.value, "Собеседник не увидит двух галочек") { Prefs.markReadOnOpen.value = !it }
        }
        group("Сообщения") {
            switch("Сохранять удалённые", Prefs.keepDeleted, "Удалённые собеседником остаются с пометкой (до перезапуска)")
            switch("Скрывать реакции", Prefs.hideReactions)
            switch("Счётчик символов", Prefs.charCounter, "Над полем ввода, лимит 4096")
            switch("Тихая отправка по умолчанию", Prefs.silentByDefault, "Получатель не услышит звук")
            switch("Подтверждать стикеры и GIF", Prefs.confirmSticker)
            switch("Пересылать без автора", Prefs.forwardNoAuthor, "Как копию, без «Переслано от» (можно менять при пересылке)")
            switch("Прятать клавиатуру при прокрутке", Prefs.hideKbOnScroll)
            switch("Время с секундами", Prefs.seconds, "У сообщений")
        }
        group("Приватность") {
            switch("Скрывать свой номер", Prefs.hidePhone, "В настройках, профиле и меню")
            switch("Показывать ID", Prefs.showIds, "В профилях людей и чатов")
            switch("Удалять трекинг из ссылок", Prefs.stripTracking, "utm_*, fbclid, si и т. п.")
        }
        group("Диалоги") {
            switch("Подтверждать удаление", Prefs.confirmDelete)
            action("Прочитать все чаты", "Снять все непрочитанные и упоминания", Icons.Outlined.DoneAll) { readAllChats(ctx) }
        }
        group("Фоновая работа", footer = "Без сервисов Google сообщения приходят, пока qwgram работает в фоне.") {
            switch("Получать сообщения в фоне", Prefs.keepAlive) { KeepAliveService.sync(ctx) }
            switch("Текст в уведомлениях", Prefs.notifyPreview)
        }
    }
}
