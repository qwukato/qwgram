# qwgram (Android)

Клиент Telegram для Android: форк официального приложения (DrKLO/Telegram 13.2.2) в виде патчсета
на основе [Inugram](https://github.com/teidesu/inugram) (MIT) с доработками ZhirGram/qwgram.
Цель: настоящий Material 3 / Material 3 Expressive. Работает на серверах Telegram как обычный
Telegram: это тот же клиент, поэтому доступны все его функции (чаты, каналы, звонки, истории,
секретные чаты, папки, стикеры, боты и т. д.) плюс функции Inugram из [FEATURES.md](FEATURES.md).

Пакет `com.qwenz.zhirgram`, имя в лаунчере **qwgram**. Статус и правила работы: [AGENTS.md](AGENTS.md).

## Что нового в qwgram

- **Тема Material 3 из любого цвета на любом Android 8+.** Раньше M3-тема (Monet) работала только
  на Android 12+ и брала цвета из обоев. Теперь схема строится генератором HCT из скилла
  material-design-3 (`src/fork/helpers/theme/SeedScheme.kt`, решатель Google `google_material`):
  12 акцентов, свой цвет в HEX, цвета обоев (Android 12+), 5 палитр (спокойная, яркая,
  выразительная, нейтральная, монохром) и 3 уровня контрастности.
  Настройки → настройки qwgram → Оформление → раздел «Цвет».
- **Material 3 по умолчанию:** M3-переключатели, слайдеры, секции, аватары, кнопки профиля,
  нижняя панель и анимация переходов включены сразу; при первом запуске (до входа в аккаунт)
  включается M3-тема, которая следует светлой/тёмной теме системы. Всё отключается в настройках.
- Своя иконка (adaptive, круглая, монохромная для тематических значков, значок уведомлений).
- Название qwgram во всех строках; автообновление из канала Inugram отключено (оно предлагало
  бы чужие APK); в «О приложении» указано, что клиент неофициальный и основан на Inugram.

## Как собрать APK (GitHub Actions)

Локальная сборка не нужна, всё делает GitHub.

1. Получите **api_id** и **api_hash**: https://my.telegram.org → API development tools → создайте приложение
   (платформа Android).
2. Создайте **приватный или публичный** репозиторий на GitHub и загрузите туда содержимое этой папки.
3. Settings → Secrets and variables → Actions → New repository secret: `TG_APP_ID` и `TG_APP_HASH`.
4. Actions → **Build qwgram** → Run workflow. Сборка 30–90 минут (Telegram большой: нативные
   библиотеки, Rust, Gradle).
5. APK в разделе Artifacts запуска (`zhirgram-apk`), или `gh run download -n zhirgram-apk`.
6. Установите APK на телефон (разрешите установку из неизвестных источников).

Подпись пока debug-ключом CI: обновлять поверх можно только сборкой, подписанной тем же ключом.
Для постоянной подписи добавьте свой keystore в секреты (см. AGENTS.md, задача на будущее).

Если сборка упала, пришлите вывод:
```
ID=$(gh run list --workflow=build.yml -L1 --json databaseId -q '.[0].databaseId')
gh run view $ID --log-failed > ~/log.txt
grep -nE "What went wrong|error:|FAILED|Error" -A 6 ~/log.txt | cut -c1-300 | head -60
```

## Как посмотреть настоящий код Telegram

Actions → **Export stock files**: в поле paths пути внутри worktree, на выходе `files.zip`.

## Лицензии и правила Telegram

GPL v2+ (Telegram), MIT (Inugram), см. [ATTRIBUTION.md](ATTRIBUTION.md). Исходники при публикации
APK должны быть открыты. Используйте только свои api_id/api_hash, не называйте приложение
«Telegram» и не используйте его логотип: https://core.telegram.org/api/terms
