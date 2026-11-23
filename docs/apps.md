# Лаунчер, «Цифровое ТВ», чистка и сторож

## Лаунчер NA Clean Toour TV

Исходники — [kryzhukov/natx5cleanyourtv](https://github.com/kryzhukov/natx5cleanyourtv) (русская
сборка NA TV Лаунчер, `com.gathin.tvhome`, `minSdk` 26). В репозитории лежит уже собранный APK:
[`apk/NA-Clean-Toour-TV-3.5.apk`](../apk/NA-Clean-Toour-TV-3.5.apk)
(sha256 `e170ce4fcc6026b7b7fff52207d49d6256656c4ae3d5ca6baa870581ae83f34e`).

Установка и назначение главным экраном — [`scripts/install-launcher.sh`](../scripts/install-launcher.sh):

```bash
scripts/install-launcher.sh
```

Скрипт: ставит APK → запускает и **проверяет, что активити поднялась** → `cmd package
set-home-activity --user 0 com.gathin.tvhome/.MainActivity` → проверяет, что Home действительно
переключился → кладёт конфиг и ставит заставку. **Лаунчер Сбера удаляется отдельно и только
после этого** — иначе можно остаться без главного экрана.

### Как это выглядит

| Главный экран | Настройки главного экрана | Библиотека приложений |
|---|---|---|
| ![Главный экран](../screenshots/01-home.png) | ![Настройки](../screenshots/04-launcher-settings.png) | ![Библиотека](../screenshots/05-launcher-library.png) |

### Конфиг без пересборки

Лаунчер читает `config.json` из своего external-каталога и накладывает его поверх встроенного
`config.xml`:

```
/sdcard/Android/data/com.gathin.tvhome/files/config.json
```

Файл [`scripts/launcher-config.json`](../scripts/launcher-config.json):

* `dock_apps` — **Цифровое ТВ, YouTube, Okko, Кинопоиск** (первым — новый ТВ-клиент);
* `hidden_apps` — заводские меню CVTE, настройки ТВ, webview, остатки Сбера — не показывать в сетке;
* `app_labels` — «Центр управления», «Телеканалы», «СберTube» вместо невнятных имён Сбера.

Заставка: `settings put secure screensaver_components com.gathin.tvhome/.ClockDream`.
Автообновления прошивки/приложений выключаются: `ota_disable_automatic_update=1`, `auto_update_apps=0`.

### Сборка из исходников (если нужен свой APK)

```bash
git clone https://github.com/kryzhukov/natx5cleanyourtv
cd natx5cleanyourtv/launcher
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk    # build-tools;34.0.0, platforms;android-34
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

## Замена «Эфир ТВ+» на «Цифровое ТВ»

На стоковом ТВ стоит **«Эфир ТВ+»** — `limehd.ru.ctvshka` 4.4.0 (приложение Lime HD). Его место
занимает **«Цифровое ТВ»** — `limehd.ru.ctv` 5.1.1 (тоже Lime HD, с телеканалами).

APK «Цифрового ТВ» — **сторонний**, в репозиторий не входит: возьмите свой файл (у автора это
`Цифровое-ТВ-5.1.1.apk`, sha256 `9569a6c6c354b96203aa1234643db7baaae29516f08de60c19427a7e24e44eaa`).

```bash
# 1) бэкап старого приложения (чтобы можно было вернуть)
adb -s "$TV_SERIAL" shell pm path limehd.ru.ctvshka      # → /data/app/.../base.apk
adb -s "$TV_SERIAL" pull /data/app/.../base.apk backup/limehd.ru.ctvshka-4.4.0.apk

# 2) удалить «Эфир ТВ+»
adb -s "$TV_SERIAL" shell pm uninstall --user 0 limehd.ru.ctvshka

# 3) установить «Цифровое ТВ»
adb -s "$TV_SERIAL" install -r Цифровое-ТВ-5.1.1.apk
```

Проверка: `am start -W -n limehd.ru.ctv/limehd.ru.ctv.MainActivity` → `Status: ok`, видны каналы.
Сборка, которая проверялась, — «Modification by vadj»: при первом запуске показывает окно
поддержки с галочкой «не показывать».

Откат: `adb install -r backup/limehd.ru.ctvshka-4.4.0.apk && adb shell pm uninstall --user 0 limehd.ru.ctv`.

## Чистка приложений Сбера

Список — [`scripts/debloat.txt`](../scripts/debloat.txt) (19 пакетов Сбера + Projectivy),
удаление — [`scripts/debloat.sh`](../scripts/debloat.sh):

```bash
scripts/debloat.sh --dry-run   # посмотреть
scripts/debloat.sh             # удалить (pm uninstall --user 0)
```

Удаляется «для пользователя 0»: иконка и данные пропадают, APK остаётся в прошивке, вернуть можно
`cmd package install-existing --user 0 <pkg>`. После чистки освободилось ~600 МБ (было 284 МБ
свободных на 3,0 ГБ).

**Оставлено специально** (иначе отваливаются пульт/ассистент/каналы/настройки):
`ru.sberdevices.services(.core)`, `ru.sberdevices.assistant`, `ru.sberdevices.tvinputs`,
`ru.sberdevices.starlivetv` («Телеканалы»), `ru.sberdevices.startube`, `ru.sberdevices.rcfupdater`,
`ru.sberdevices.gamepad.service`, `ru.sberdevices.app.starkeyboard`, `ru.sberdevices.webview`,
`ru.sberdevices.settings`, `com.sberbank.settings`, все `com.cvte.*` и `com.droidlogic.*`.

## Сторож «Сбер-Стоп NA»

Прошивка Сбера при каждой загрузке возвращает свой лаунчер и часть приложений. Сторож
(`io.sberstop`, 2.0-NA, [`apk/SberStop-NA-2.0.apk`](../apk/SberStop-NA-2.0.apk), sha256
`a670dd1eb7cbdf9c4db4896cf3126b23a79ae02c59c15ab38c4f578fc161a11d`) собран из проекта
[sber-tv-at30a6-free-my-tv](https://github.com/kryzhukov/sber-tv-at30a6-free-my-tv) и переделан
под лаунчер NA:

* после `BOOT_COMPLETED` (через ~45 с) и далее каждые ~2 минуты (JobScheduler, `persisted`);
* удаляет вернувшиеся пакеты из списка (если удалить не дают — `disable-user`);
* возвращает Home на `com.gathin.tvhome` и заставку `ClockDream`, если Сбер их сбросил;
* выключает автообновления и рекламный оверлей (`appops SYSTEM_ALERT_WINDOW deny`).

Скрипт установки добавляет сторожа в whitelist Doze (`cmd deviceidle whitelist +io.sberstop`):
иначе JobScheduler откладывает проверки, пока телевизор «спит», и вернувшиеся приложения Сбера
живут дольше обычного.

Сторож ходит по ADB на `127.0.0.1:5555`. Чтобы он не вызывал запрос «Разрешить отладку»,
[`scripts/install-watchdog.sh`](../scripts/install-watchdog.sh) **заранее** генерирует его ключ,
кладёт приватную часть в `/data/data/io.sberstop/files/` и дописывает публичную в
`/data/misc/adb/adb_keys` (см. [adb-permanent.md](adb-permanent.md)). После установки — одна
перезагрузка, затем один запуск приложения и кнопка «Проверить сейчас».

Проверено: после перезагрузки сторож поднялся сам и отработал — «удалено: 1 прил., главный экран → NA».

### Он не будет висеть на экране

Обычные проходы идут через `JobScheduler` (`io.sberstop/.KeeperJob`, `PERSISTED`, каждые ~2 минуты):
это **фоновая служба без UI** — экран не открывается, поверх лаунчера ничего не появляется.
Проверено: после прохода в фоне на экране остаётся `com.gathin.tvhome/.MainActivity`, а время
последней проверки в `SharedPreferences` обновляется.

Видимое окно сторожа появляется только если **сами** его откроете (иконка в библиотеке приложений);
кнопка «Проверить сейчас» дополнительно показывает уведомление переднего плана. Если случайно
открыли — нажмите **Home**: проверки продолжат идти в фоне. Не делайте «Остановить»/force-stop —
это переводит приложение в stopped-состояние и задания не будут запускаться до следующего запуска.

## Итоговые проверки

| Проверка | Результат |
|---|---|
| Главный экран | `com.gathin.tvhome/.MainActivity` |
| Заставка | `com.gathin.tvhome/.ClockDream` |
| Лаунчер Сбера и 19 приложений | отсутствуют |
| «Цифровое ТВ» 5.1.1 | установлен, запускается |
| Сторож после перезагрузки | поднялся сам, вернул Home, удалил вернувшееся |
| Свободно на /data | ~0,6–0,9 ГБ |
