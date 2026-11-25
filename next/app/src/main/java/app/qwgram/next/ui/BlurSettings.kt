package app.qwgram.next.ui

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.qwgram.next.Prefs
import dev.chrisbanes.haze.HazeState

/** Appearance → Blur: every knob of the glass, with a live preview on top */
@Composable
fun BlurPage() = PageScaffold("Размытие фона") {
    item { BlurPreview() }
    val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    group(footer = if (supported) "Панели становятся «стеклом»: всё, что прокручивается под ними, размывается. По умолчанию выключено." else "Размытие работает на Android 12 и новее — на этом телефоне панели останутся как есть.") {
        switch("Размытие фона", Prefs.blurOn, sub = "Стеклянные панели, кнопки и окна")
    }
    if (!Prefs.blurOn.value) return@PageScaffold
    group("Общее") {
        pick("Оттенок стекла", Prefs.blurTone, listOf(
            "surface" to "Цвет панели", "container" to "Приподнятый", "accent" to "Акцентный",
            "dark" to "Тёмное стекло", "light" to "Светлое стекло", "none" to "Без оттенка",
        ))
        sliderFloat("Зерно в переходах", Prefs.blurNoise, 0f..0.3f, steps = 29) { "${(it * 100).toInt()}%" }
    }
    group("Кнопки и панели", footer = "Плавающие кнопки, нижняя панель, поле ввода, папки, мини-плеер.") {
        sliderInt("Сила размытия", Prefs.blurBtnRadius, 4..72, step = 2) { "$it dp" }
        sliderFloat("Плотность оттенка", Prefs.blurBtnTint, 0f..1f, steps = 19) { "${(it * 100).toInt()}%" }
        sliderFloat("Зерно", Prefs.blurBtnNoise, 0f..0.3f, steps = 29) { "${(it * 100).toInt()}%" }
        switch("Светлая кромка", Prefs.blurEdge, sub = "Тонкая обводка у стеклянных кнопок")
    }
    group("Переходы под заголовками", footer = "Заголовки и низ экрана: размытие сильнее у края и плавно растворяется к контенту. Без размытия переход остаётся просто цветным.") {
        switch("Размытие в переходах", Prefs.blurFadeBlur)
        if (Prefs.blurFadeBlur.value) {
            sliderInt("Сила размытия", Prefs.blurFadeRadius, 4..96, step = 2) { "$it dp" }
            sliderFloat("Плотность оттенка", Prefs.blurFadeTint, 0f..1f, steps = 19) { "${(it * 100).toInt()}%" }
        }
        switch("Плавный край (прогрессивно)", Prefs.blurFade)
        if (Prefs.blurFade.value) {
            sliderFloat("Длина перехода", Prefs.blurFadeLen, 0.3f..3f, steps = 26) { "${(it * 100).toInt()}%" }
            segmented("Кривая", Prefs.blurCurve, listOf("soft" to "Мягкая", "linear" to "Ровная", "sharp" to "Резкая"))
        }
        switch("Только при прокрутке", Prefs.fadeOnScroll, sub = "Переход появляется, когда под панелью есть контент")
    }
    group("Где размывать") {
        switch("Заголовки", Prefs.blurHeaders, sub = "Верхние панели всех экранов")
        switch("Папки", Prefs.blurChipsFolders, sub = "Кнопки папок над списком чатов")
        switch("Панели и кнопки", Prefs.blurBars, sub = "Нижняя панель, плавающие кнопки, мини-плеер")
        switch("Поле ввода", Prefs.blurComposer, sub = "Низ чата и само поле")
        switch("Даты и плашки на обоях", Prefs.blurChips)
        switch("Пузыри сообщений", Prefs.blurBubbles, sub = "Матовые пузыри поверх обоев — дороже для батареи")
        if (Prefs.blurBubbles.value) sliderFloat("Непрозрачность пузырей", Prefs.blurBubbleAlpha, 0.3f..1f, steps = 13) { "${(it * 100).toInt()}%" }
        switch("За меню", Prefs.blurMenus, sub = "Чат и экран за открытым меню")
        switch("За окнами и шторками", Prefs.blurWindows, sub = "Диалоги, шторки, просмотр стикеров")
        if (Prefs.blurWindows.value) sliderInt("Размытие за окнами", Prefs.blurWindowRadius, 4..64, step = 2) { "$it dp" }
    }
    group("Производительность", footer = "«Быстро» размывает уменьшенную копию — почти не заметно глазу и бережёт батарею. «Максимум» — полное разрешение.") {
        segmented("Качество", Prefs.blurQuality, listOf("speed" to "Быстро", "balanced" to "Баланс", "best" to "Максимум"))
    }
}

/**
 * Live preview: a little chat (colourful, slowly scrolling) under a glass header, a frosted pill bar and a
 * floating button — every setting above shows here at once.
 */
@Composable
private fun BlurPreview() {
    val cs = MaterialTheme.colorScheme
    val state = remember { HazeState() }
    val t = rememberInfiniteTransition(label = "bp")
    val shift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "sh")
    Box(
        Modifier.padding(top = 4.dp, bottom = 12.dp).fillMaxWidth().height(260.dp).clip(RoundedCornerShape(28.dp.rs()))
            .background(cs.surfaceContainerLowest),
    ) {
        // the "content": blobs and bubbles drifting under the chrome
        Box(Modifier.fillMaxSize().blurSource(state = state)) {
            Box(
                Modifier.fillMaxSize().graphicsLayer { translationY = -120.dp.toPx() * shift }
                    .background(Brush.verticalGradient(listOf(cs.primaryContainer, cs.tertiaryContainer, cs.secondaryContainer, cs.primaryContainer))),
            )
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp).graphicsLayer { translationY = 40.dp.toPx() - 140.dp.toPx() * shift },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Привет! Как тебе стекло?" to false, "Очень красиво ✨" to true, "Размытие под шапкой плавно уходит вниз" to false,
                    "А кнопки как матовые" to true, "И пузыри поверх обоев" to false, "Настраивается всё 👌" to true, "Ещё сообщение" to false, "И ещё одно" to true)
                    .forEach { (text, out) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = if (out) Arrangement.End else Arrangement.Start) {
                            Text(
                                text, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = if (out) cs.onPrimary else cs.onSurface,
                                modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(if (out) cs.primary else cs.surfaceContainerHigh).padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
            }
        }
        // glass header (progressive)
        Box(
            Modifier.fillMaxWidth().height(70.dp)
                .then(if (blurOn()) Modifier.glassFade(FadeEdge.Bottom, cs.surface, Prefs.blurOn, state = state) else Modifier.topFade(cs.surface)),
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(cs.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Person, null, tint = cs.onPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Предпросмотр", style = MaterialTheme.typography.titleSmall, color = cs.onSurface)
                    Text(if (blurOn()) "размытие включено" else "размытие выключено", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
            }
        }
        // glass pill bar + floating button
        Row(
            Modifier.align(Alignment.BottomCenter).padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val g = blurOn()
            Row(
                Modifier.weight(1f).height(52.dp)
                    .then(if (g) Modifier.glass(RoundedCornerShape(26.dp), cs.surfaceContainerHigh, Prefs.blurOn, state = state) else Modifier.clip(RoundedCornerShape(26.dp)).background(cs.surfaceContainerHigh))
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Icon(Icons.Outlined.ChatBubbleOutline, null, tint = cs.onSurface)
                Icon(Icons.Outlined.Person, null, tint = cs.onSurfaceVariant)
                Icon(Icons.Outlined.Settings, null, tint = cs.onSurfaceVariant)
            }
            Box(
                Modifier.size(52.dp)
                    .then(if (g) Modifier.glass(CircleShape, cs.primaryContainer, Prefs.blurOn, state = state) else Modifier.clip(CircleShape).background(cs.primaryContainer)),
                contentAlignment = Alignment.Center,
            ) { Text("✎", color = cs.onPrimaryContainer, style = MaterialTheme.typography.titleMedium) }
        }
    }
}


/** Settings → Energy saving: everything that trades a bit of motion for battery (off by default) */
@Composable
fun PowerSavePage() = PageScaffold("Энергосбережение") {
    group(footer = "Когда включено, приложение меньше перерисовывает экран без дела и бережёт батарею. По умолчанию выключено.") {
        switch("Энергосбережение", Prefs.powerSave, sub = "Экономить батарею за счёт части анимаций")
    }
    if (!Prefs.powerSave.value) return@PageScaffold
    group("Что экономить") {
        switch("Останавливать фоновые анимации", Prefs.psIdleAnims, sub = "Вращение обложки, волны плеера и декоративные фигуры — только когда нужны")
        segmented("Анимированные стикеры и эмодзи", Prefs.psStickers, listOf("loop" to "Всегда", "once" to "Один раз", "still" to "Без анимации"))
        switch("Пониженное качество размытия", Prefs.psBlurLow, sub = "Размывать уменьшенную копию")
        switch("Не проигрывать GIF сами", Prefs.psGifs, sub = "GIF и кружки в чатах стоят на паузе, пока на них не нажмёте")
    }
}
