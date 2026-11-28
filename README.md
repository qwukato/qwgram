<div align="center">

<img src="docs/assets/icon.svg" width="112" alt="qwgram">

# qwgram Next

**Неофициальный клиент Telegram для Android в стиле Material 3 Expressive**

Kotlin · Jetpack Compose · официальная библиотека TDLib

[![Последний релиз](https://img.shields.io/github/v/release/qwukato/qwgram?label=%D0%B2%D0%B5%D1%80%D1%81%D0%B8%D1%8F&color=6750A4&style=for-the-badge)](https://github.com/qwukato/qwgram/releases/latest)
[![Скачивания](https://img.shields.io/github/downloads/qwukato/qwgram/total?label=%D1%81%D0%BA%D0%B0%D1%87%D0%B8%D0%B2%D0%B0%D0%BD%D0%B8%D1%8F&color=6750A4&style=for-the-badge)](https://github.com/qwukato/qwgram/releases)
[![Сборка](https://img.shields.io/github/actions/workflow/status/qwukato/qwgram/build-next.yml?branch=main&label=%D1%81%D0%B1%D0%BE%D1%80%D0%BA%D0%B0&style=for-the-badge)](https://github.com/qwukato/qwgram/actions/workflows/build-next.yml)

<br>

<a href="https://github.com/qwukato/qwgram/releases/latest/download/qwgram-next.apk">
  <img src="https://img.shields.io/badge/%D0%A1%D0%BA%D0%B0%D1%87%D0%B0%D1%82%D1%8C%20APK-6750A4?style=for-the-badge&logo=android&logoColor=white&labelColor=4F378B" height="56" alt="Скачать APK">
</a>

<sub>Android 8.0 и новее · ссылка всегда ведёт на самую свежую версию · <a href="https://github.com/qwukato/qwgram/releases">все версии</a></sub>

</div>

---

## Установка

1. Нажмите **«Скачать APK»** выше (или откройте [последний релиз](https://github.com/qwukato/qwgram/releases/latest) и скачайте `qwgram-next.apk` из раздела *Assets*).
2. Откройте файл на телефоне. Если Android спросит — разрешите установку из этого источника.
3. Войдите по номеру телефона, как в обычном Telegram.

**Обновление:** скачайте новый APK и установите поверх — чаты и настройки сохранятся. Новая версия появляется после каждого изменения в коде.

> [!NOTE]
> Google Play Protect может предупредить о «неизвестном приложении» — так бывает со всеми приложениями не из Play Маркета. Нажмите «Всё равно установить».

## Что умеет

**Чаты и сообщения**
- Папки, архив, поиск, контакты, закреп, черновики, отложенная и тихая отправка
- Ответы, пересылка, редактирование, реакции (панель как в Telegram), выбор нескольких сообщений
- Фото и видео альбомами с подписями, голосовые и **видеокружки** (нажатие — смена режима, удержание — запись, вверх — закрепить)
- Стикеры, GIF, эмодзи, опросы, файлы, геопозиция, форматирование текста
- Каналы с подписями авторов, комментарии, темы в форумах

**Музыкальный плеер**
- Мини-плеер на каждом экране и полноэкранный плеер с цветами из обложки
- Карусель обложек, волнистый прогресс, очередь с перетаскиванием, таймер сна
- **Синхронный текст песен** с подсветкой слов (LRCLIB)
- Музыка профиля, сохранение в «Музыку»

**Оформление — Material 3 Expressive**
- Тема из любого цвета, Material You, AMOLED, своя палитра
- Формы аватарок с морфингом, соединённые группы кнопок, пружинные анимации
- Каскадное появление экранов, моушен блюр при прокрутке, настройка скорости анимаций
- Шрифты Google Sans Flex и Roboto Flex, эмодзи Twemoji и Blobmoji

**Приватность и удобство**
- Скрытие номера, чтение без отметки «прочитано», очистка ссылок от трекинга
- Энергосбережение, которое само включается при низком заряде

## Чего пока нет

- **Звонков.** Для голоса нужна отдельная VoIP-библиотека; сейчас доступны история звонков и экран входящего звонка (отклонить можно, ответить — с другого устройства).
- Секретных чатов и входа по QR-коду.

Нашли ошибку? Если приложение упало, при следующем запуске оно покажет отчёт с кнопкой «Скопировать» — приложите его к [issue](https://github.com/qwukato/qwgram/issues).

## Для разработчиков

<details>
<summary>Собрать свою версию</summary>

Всё собирается в GitHub Actions, локально ничего ставить не нужно.

1. Сделайте форк репозитория.
2. Получите `api_id` и `api_hash` на [my.telegram.org](https://my.telegram.org) → API development tools.
3. Settings → Secrets and variables → Actions → добавьте секреты `TG_APP_ID` и `TG_APP_HASH`.
4. Actions → **Build qwgram Next** → Run workflow. Первая сборка дольше: собирается TDLib, потом она берётся из кэша.
5. Готовый APK появится в разделе Releases (и в артефакте `qwgram-next-apk`).

Код приложения — в папке [`next/`](next). Подпись APK выводится из секретов, поэтому новые сборки ставятся поверх старых.

</details>

<details>
<summary>Классический qwgram (форк официального Telegram)</summary>

В корне репозитория также лежит патчсет qwgram поверх DrKLO/Telegram на основе [Inugram](https://github.com/teidesu/inugram). Описание и инструкция по сборке: [docs/classic-fork.md](docs/classic-fork.md).

</details>

## Лицензия и благодарности

qwgram — неофициальный клиент, не связан с Telegram. Работает через [TDLib](https://github.com/tdlib/td).
Классическая часть основана на [Inugram](https://github.com/teidesu/inugram) (MIT) и [Telegram для Android](https://github.com/DrKLO/Telegram) (GPL-2.0).
Подробности — в [LICENSE](LICENSE) и [ATTRIBUTION.md](ATTRIBUTION.md).
