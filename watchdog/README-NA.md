# «Сбер-Стоп NA» — сторож для SaluteTV AT30A6 с лаунчером NA Clean Toour TV

Приложение-сторож для телевизора **SaluteTV AT30A6** (Amlogic T962D4, Android 13 / StarOS, прошивка
`TP1A.220905.004.A2`) с лаунчером **NA Clean Toour TV** (`com.gathin.tvhome`) из
[natx5cleanyourtv](https://github.com/kryzhukov/natx5cleanyourtv).

Основа — исходный проект «Сбер-Стоп» 1.4 из `/tmp/sberat30/sberstop` (Java, сборка без Gradle).
Задача этого билда — та же логика и **тот же проверенный список из 20 пакетов**, что у автора
[sber-tv-at30a6-free-my-tv](https://github.com/kryzhukov/sber-tv-at30a6-free-my-tv) (владелец ТВ:
«по очистке мусора опыт есть, только лаунчер не бери оттуда»), но с главным экраном и заставкой
NA Clean Toour TV.

> **2026-11-25, версия 2.1-NA (`versionCode=7`).** Добавлена служба доступности
> `io.sberstop.CtvCloseWatcher`: после ухода «Цифрового ТВ» (`limehd.ru.ctv`) с переднего плана или
> при выключении экрана она очищает кеш и данные ЦТВ через ADB (`pm clear limehd.ru.ctv`).
> Подробности и риски — в разделе «Очистка ЦТВ после закрытия (2.1-NA)». Логика 2.0-NA (список
> пакетов, главный экран/заставка, задания, кнопки на экране) сохранена без изменений.
>
> Пересборка от 2026-11-23 (по уточнению владельца): список пакетов возвращён к 20 пакетам
> оригинального sberstop 1.4; sha256 в таблице ниже — для актуального файла.

## Готовый APK

| | |
|---|---|
| Файл | `work/watchdog/SberStop-NA.apk` |
| Размер | **74 378 байт** |
| **sha256** | **`1868b18139a92cf026dc46d5f4af7a0ccbeebe6e3a1364fc1dd08c6aafcda435`** |
| md5 | `1e42fff1d46a36793b91b4ec7ce79700` |
| Пакет | **`io.sberstop`** (как у старого сторожа — см. «Подпись и пакет») |
| Версия | `versionCode=7`, `versionName=2.1-NA` |
| minSdk / targetSdk | **26** / **33** (compileSdk 34) |
| Подпись | `sberstop.keystore`, alias `sberstop`, пароль `sberstop`; SHA-256 сертификата `5F:38:FC:8A:75:4C:AE:32:B9:0E:0B:D7:C5:5A:56:E3:6E:01:2C:36:A9:5F:DB:00:46:B0:86:EA:47:E0:79:05` |
| Схемы подписи | v2 + v3 (v1 не нужна при minSdk 26; v4 отключена) |
| Разрешения | `INTERNET`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `QUERY_ALL_PACKAGES` — **новых разрешений 2.1-NA не добавляет** |
| Служба доступности | `io.sberstop.CtvCloseWatcher` (включается вручную, см. ниже) |
| Тег в logcat | `SberStop` |

Проверено локально (без телевизора): `apksigner verify` — `Verifies`; подпись v2+v3, сертификат тот
же, что у 2.0-NA (SHA-256 `5f38fc8a…e07905`); `zipalign -c -v 4` — успешно;
`aapt2 dump badging` показывает `package: name='io.sberstop' versionCode='7' versionName='2.1-NA'`,
`sdkVersion:'26'`, `targetSdkVersion:'33'`, прежние 4 разрешения;
`aapt2 dump xmltree` — служба `.CtvCloseWatcher` с `BIND_ACCESSIBILITY_SERVICE`, `exported=true`,
intent-filter `android.accessibilityservice.AccessibilityService` и meta-data на
`@xml/accessibility_service_config` лежит в манифесте APK; файл
`res/xml/accessibility_service_config.xml` есть в APK, и его флаги скомпилированы ровно в
`typeWindowStateChanged|typeWindowsChanged` (`0x00400020`), `feedbackGeneric` (`0x10`),
`flagDefault` (`0x1`), `notificationTimeout=100`, `canRetrieveWindowContent=false`;
`dexdump` — DEX 038 (API 26), в `classes.dex` есть класс `Lio/sberstop/CtvCloseWatcher;`,
строки `limehd.ru.ctv`, `pm clear`, `dumpsys activity activities`, `mResumedActivity`,
`clear_ctv_on_close`; строки в `classes.dex` содержат `com.gathin.tvhome`, `MainActivity`,
`ClockDream` и **все 20 пакетов списка**; «оставленные» пакеты (`starlivetv`, `startube`,
`rcfupdater`, `gamepad.service`, `huaweicast`, `maxhub`, `screenshare`, `dssshare`) в dex отсутствуют.

## Что изменено против исходного io.sberstop 1.4 (Free My TV)

| № | Было (1.4 / Free My TV) | Стало (2.0-NA) |
|---|---|---|
| 1 | Главный экран `io.freemytv/.HomeActivity` | **`com.gathin.tvhome/com.gathin.tvhome.MainActivity`** |
| 2 | Список 20 пакетов (sberstop 1.4) | **Тот же проверенный список владельца — 20 пакетов, без изменений** (список natx5 из 26 пакетов не используем) |
| 3 | Заставка не трогалась (её возвращал Free My TV) | Если в заставке Сбер или пусто — ставится **`com.gathin.tvhome/.ClockDream`** |
| 4 | Сначала удаление, потом Home | Сначала **Home**, проверка, что он реально переключился, и только потом удаление лаунчера Сбера |
| 5 | — | `pm enable com.gathin.tvhome` на случай, если StarOS выключил лаунчер |
| 6 | — | `QUERY_ALL_PACKAGES` в манифесте |
| 7 | Ожидание разрешения отладки 20 с | 30 с из фонового задания / **120 с** по кнопке «Проверить сейчас» + подсказка на экране |
| 8 | Экран без ключа | На экране показываются **MD5/SHA-256 отпечатки ADB-ключа** и сам ключ; они же пишутся в logcat |
| 9 | — | Имя ADB-ключа `sberstop-na@tv` (было `free-my-tv@tv`) |

2.1-NA **ничего из этой таблицы не меняет** — добавлены только служба доступности `CtvCloseWatcher`
и элементы на экране (см. следующий раздел).

Что сознательно сохранено без изменений:

* `BootReceiver`: `BOOT_COMPLETED`/`MY_PACKAGE_REPLACED` → задание через **45 с**, плюс цепочка заданий
  каждые **2 минуты** и страховочное периодическое **раз в 15 минут** (`JobScheduler`, `setPersisted`);
* события `PACKAGE_ADDED/REPLACED/CHANGED` → проход через 3 с (с антидребезгом 60 с);
* удаление `pm uninstall --user 0`, при отказе — `pm disable-user`;
* `settings put global ota_disable_automatic_update 1`;
* `settings put global auto_update_apps 0`;
* `appops set ru.sberdevices.services SYSTEM_ALERT_WINDOW deny` (рекламный оверлей);
* собственный ADB-клиент к `127.0.0.1:5555` (`Adb.java`), задания через `KeeperJob`, кнопка через
  `KeeperService`.

### Откуда взяты главный экран и заставка

* Пакет: `namespace 'com.gathin.tvhome'` и `applicationId ... 'com.gathin.tvhome'` —
  `launcher/app/build.gradle`.
* Home-активити: `launcher/app/src/main/AndroidManifest.xml` — `.MainActivity` с фильтром
  `MAIN` + `HOME` + `COMMON_HOME` + `DEFAULT` (именно этот фильтр ловит и кнопку «Домой», и загрузку на
  CVTE-прошивке). Полный компонент для `cmd package set-home-activity`:
  **`com.gathin.tvhome/com.gathin.tvhome.MainActivity`**.
* Заставка: в том же манифесте сервис `.ClockDream` (`DreamService`) с комментарием
  `settings put secure screensaver_components com.gathin.tvhome/.ClockDream`.
  Причина: по README natx5 для AT30A6, пока `screensaver_default_component` указывает на заставку
  Сбера, StarOS при загрузке снова включает `ru.sberdevices.starscreensaver`. С ClockDream она
  остаётся выключенной. **Чужую (пользовательскую) заставку сторож не перебивает** — меняет только
  сберовскую или пустую.

### Список пакетов

`Blacklist.PACKAGES` — **проверенный список владельца ТВ** (автор репозитория
kryzhukov/sber-tv-at30a6-free-my-tv): ровно **20 пакетов** из `/tmp/sberat30/README.md` и
оригинального `sberstop` 1.4. Это тот же список, которым владелец чистил свою AT30A6; список
`examples/sber-salute-tv-at30a6/debloat.txt` из natx5 (26 пакетов) сознательно **не** используется —
владелец подтвердил свой, а лаунчер и очистку просил брать не из natx5.

```
ru.sberdevices.starlauncher       ru.sberdevices.starscreensaver   ru.sberdevices.starmarket
ru.sberdevices.app.setup          ru.sberdevices.openbeta          ru.sberdevices.starcleaner
ru.sberdevices.smarthome.promo    ru.sberdevices.starbrowser       ru.sberdevices.videos
ru.sberdevices.music              ru.sberdevices.sberplay          ru.sberdevices.starprofile
ru.sberdevices.subscriptions      ru.sberdevices.parentcontrol     ru.sberdevices.starkidsroom.prod
ru.sberdevices.bugreporter        ru.sberdevices.survey            com.salute.star
com.sberbank.settings             com.spocky.projengmenu
```

Намеренно **не трогаются** (владелец оставляет): `ru.sberdevices.starlivetv` (Телеканалы),
`ru.sberdevices.startube` (СберTube), `ru.sberdevices.rcfupdater` (прошивка пульта),
`ru.sberdevices.gamepad.service` (пульт с телефона Homuz), `ru.sberdevices.huaweicast`,
`com.maxhub.screenshare.server.tv`, `com.screenshare.platform`, `com.cvte.dssshare.dlna`,
`ru.sberdevices.tv`; а также `ru.sberdevices.assistant`, `.tvinputs`, `.app.starkeyboard`,
`.webview`, `.settings`, `.systemui`, `.SberDevicesServiceSystem`, `.location.provider`,
`com.droidlogic.*`.

`ru.sberdevices.starlauncher` удаляется **последним** и только если главный экран уже подтверждённо
переключился на `com.gathin.tvhome` — чтобы не остаться без Home. Если переключить не удалось, сторож
пишет об этом в статус и лаунчер Сбера не трогает.

## Очистка ЦТВ после закрытия (2.1-NA)

Новая служба доступности `io.sberstop.CtvCloseWatcher` очищает **кеш и данные** приложения
«Цифровое ТВ» (`limehd.ru.ctv`) после того, как ЦТВ ушло с переднего плана или выключился экран.
Очистка — команда `pm clear limehd.ru.ctv` через уже существующий ADB-клиент `Adb.java`
(`127.0.0.1:5555`, тот же ключ приложения, что у остальных проходов сторожа; отдельной авторизации
не нужно). `pm clear` в Android — это **и кеш, и данные**: после очистки ЦТВ запускается как
впервые (вход, настройки, «избранное» в приложении сбрасываются). Отдельной команды «почистить
только кеш» ADB не даёт.

### Как определяется «закрытие» (и почему оверлей не считается)

1. Пришло событие accessibility с пакетом `limehd.ru.ctv` — запоминаем, что **ЦТВ на переднем
   плане** (взводим «курок»).
2. Пришло событие от **любого другого пакета** — через **1.5 с** служба проверяет **фактически
   активную активити** через ADB:
   `dumpsys activity activities | grep -m1 mResumedActivity` (запасные варианты:
   `topResumedActivity`, `dumpsys window | grep -m1 mCurrentFocus`).
   * в выводе **есть** `limehd.ru.ctv` — это оверлей/чужое окно поверх ЦТВ (заставка, шторка
     уведомлений, системный диалог, запрос «Разрешить отладку?», карточки ассистента): **не чистим**,
     «курок» остаётся взведённым;
   * ЦТВ **нет** — приложение действительно ушло с экрана: выполняем `pm clear limehd.ru.ctv`.
   * dumpsys не дал ответа (ADB недоступен) — **не чистим** (безопаснее сохранить данные).
3. **Выключение экрана** (`ACTION_SCREEN_OFF`, приёмник внутри службы) при взведённом «курке» —
   очищаем сразу, не дожидаясь событий: при погашенном экране активность может остаться «resumed»,
   но это всё равно закрытие.

Антидребезг: проверка откладывается на 1.5 с и переставляется на каждое новое событие; очистка —
**не чаще одного раза в 3 с**. Всё выполняется на отдельном фоновом потоке (`HandlerThread`), главный
поток не блокируется — ANR исключён. Один раз при подключении службы сторож спрашивает через ADB
текущий экран: если ЦТВ уже открыто в момент включения службы, «курок» взводится сразу.

### Переключатель и экран

* SharedPreferences-файл **`settings`**, ключ **`clear_ctv_on_close`**, по умолчанию **`true`**
  (включено). Служба читает его перед каждой очисткой; при выключенном переключателе она не делает
  ничего.
* На экране «Сбер-Стоп NA» добавлены:
  * флажок **«Чистить «Цифровое ТВ» после закрытия»** (по умолчанию отмечен) с подсказкой
    «кеш и данные; вход и настройки приложения сбросятся»;
  * строка статуса **«Очистка ЦТВ: включена, последняя: чч:мм»** (обновляется каждые 1.5 с, как и
    остальной статус). Если служба доступности не включена, строка так и говорит:
    «… · служба доступности не включена», а после неудачного `pm clear` — «(ошибка)».
  * Существующие кнопки «Проверить сейчас», «Показать ключ ADB», строка «Последняя проверка» и вся
    логика Blacklist/заданий не менялись.

### Файл конфигурации службы

`res/xml/accessibility_service_config.xml` (компилируется в APK под тем же именем):

```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowsChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault"
    android:canRetrieveWindowContent="false"
    android:notificationTimeout="100"
    android:description="@string/ctv_accessibility_description" />
```

`packageNames` **не** задаётся намеренно: нужны события и от чужих пакетов — именно чужое окно после
ЦТВ запускает проверку перед очисткой. Доступ к содержимому окон службе не нужен
(`canRetrieveWindowContent="false"`), новых разрешений APK не получает.

### Как включить службу доступности (делает Lead/владелец ТВ)

Включить можно с пульта: **Настройки → Специальные возможности (Accessibility) → «Сбер-Стоп NA:
очистка ЦТВ» → включить** и согласиться с предупреждением системы. Либо разово командами (после
того, как ADB-ключ приложения уже авторизован на ТВ):

```bash
# включить службу (имя компонента — пакет/полный класс):
adb shell settings put secure enabled_accessibility_services io.sberstop/io.sberstop.CtvCloseWatcher
adb shell settings put secure accessibility_enabled 1

# проверить:
adb shell settings get secure enabled_accessibility_services   # ожидается io.sberstop/io.sberstop.CtvCloseWatcher
adb shell settings get secure accessibility_enabled            # 1
adb shell dumpsys accessibility | grep -i sberstop             # служба в списке установленных/включённых
```

**Важно:** `settings put secure enabled_accessibility_services …` **перезаписывает** список включённых
служб. Если на ТВ уже включены другие службы доступности, их надо перечислить через `:` вместе с
нашей, иначе они выключатся. На чистом AT30A6 других служб нет.

### Как выключить/приостановить

* Только очистку — снять флажок на экране сторожа (служба остаётся включённой, но ничего не чистит).
* Всю службу — выключить в Настройках либо:

```bash
adb shell settings put secure enabled_accessibility_services ""
adb shell settings put secure accessibility_enabled 0
```

(это выключит **все** службы доступности, если они есть; вручную — «Специальные возможности»).

### Как проверить очистку ЦТВ

1. Открыть ЦТВ, убедиться, что оно на экране (`dumpsys activity activities | grep -m1 mResumedActivity`
   показывает `limehd.ru.ctv`), затем выйти из ЦТВ (Домой/другое приложение) или выключить экран.
2. В logcat: `adb logcat -d -s SberStop | grep CtvCloseWatcher` — ожидаются строки
   `CtvCloseWatcher: уход с экрана — pm clear limehd.ru.ctv: Success` (или
   `… экран выключен — pm clear …: Success`).
3. На экране сторожа строка статуса покажет время последней очистки.
4. Открыть ЦТВ снова — оно должно запуститься как впервые (предложить вход/настройку).

### Обновление поверх 2.0-NA и перезагрузки

* APK ставится обновлением поверх 2.0-NA (тот же пакет/подпись). **Служба доступности сама не
  включается** — её нужно включить один раз после установки (или она останется включённой, если уже
  была включена при обновлении; после обновления пакета Android обычно перезапускает привязку, но
  гарантий нет — проверить `settings get secure enabled_accessibility_services`).
* Включённые службы доступности переживают перезагрузку ТВ (настройка `secure`), но не сброс ТВ.
  После `adb uninstall io.sberstop` / очистки данных сторожа — включить заново.
* Если служба выключена, остальной сторож (2.0-NA) работает как раньше: главный экран, заставка,
  удаление мусора, автообновления, задания.

## ADB-ключ приложения: какой используется и как авторизовать

Сторож управляет телевизором через собственный ADB-клиент, подключаясь к **`127.0.0.1:5555`**
(то есть ADB на самом ТВ, включённый в заводском меню CVTE; см. README natx5 для AT30A6).

**В APK ключ НЕ встроен.** Ключ (RSA-2048) генерируется на самом телевизоре при первом подключении и
лежит в приватном каталоге приложения:

```
/data/data/io.sberstop/files/adbkey       (приватный ключ, PKCS#8)
/data/data/io.sberstop/files/adbkey.pub   (публичный, X.509)
```

Публичная часть в формате ADB (`base64 sberstop-na@tv`) — это то, что попадает в
`/data/misc/adb/adb_keys` после подтверждения на ТВ. Посмотреть её можно:

1. На экране «Сбер-Стоп NA»: строка `ADB-ключ приложения...` с **MD5-отпечатком**, SHA-256 и
   кнопкой «Показать ключ ADB» (сам base64-ключ).
2. В logcat: `adb logcat -d -s SberStop | grep "ADB-ключ"` — при старте экрана пишется
   `ADB-ключ приложения: <base64> (MD5 ..., SHA-256 ...)`.

**Как авторизовать (это делает Lead/владелец ТВ, один раз):**

1. Поставить APK и **запустить приложение** (свежеустановленное приложение в состоянии «stopped» —
   до первого запуска оно не получит даже `BOOT_COMPLETED`). На экране появятся отпечаток и ключ.
2. Нажать **«Проверить сейчас»**.
3. На телевизоре система покажет **«Разрешить отладку по USB?»** с отпечатком ключа. Сверить отпечаток
   с экраном сторожа и выбрать **«Всегда разрешать (с этого компьютера)»**.
4. Статус на экране станет «Последняя проверка: HH:mm — …». Всё, ключ принят; после перезагрузок
   повторно спрашивать не должен (ключи в Android переживают reboot).

Если запрос не появился: истекли 120 с, порт 5555 закрыт или отладка выключена. Проверить
`adb shell getprop service.adb.tcp.port` (должно быть `5555`) и включить ADB (заводское меню CVTE,
раздел Debug → ADB Enable). По README natx5 TCP-порт **не сохраняется сам по себе** — чтобы он был
после перезагрузок: `adb shell setprop persist.adb.tcp.port 5555`. **Без открытого порта 5555 сторож
после reboot ничего сделать не сможет.**

Ключ меняется, если очистить данные приложения/переустановить его с нуля (`adb uninstall`), либо
после сброса ТВ — тогда запрос «Разрешить отладку?» появится снова.

## Установка и первый проход (команды для Lead; я их НЕ выполнял)

```bash
APK=/home/ulalex/Проекты/ТВ/work/watchdog/SberStop-NA.apk
adb connect 192.168.3.110:5555

# если на ТВ уже стоит io.sberstop, подписанный ДРУГИМ ключом, установка обновиться не сможет:
# adb uninstall io.sberstop
adb install -r "$APK"

# открыть приложение один раз (снимает состояние stopped и генерирует ключ):
adb shell am start -n io.sberstop/.MainActivity
```

Дальше — на экране ТВ: «Проверить сейчас» → «Всегда разрешать» в запросе отладки.
Если хочется без пульта (после первого запуска приложения, когда оно уже не stopped):

```bash
adb shell am start-foreground-service -n io.sberstop/.KeeperService
```

**После того как ADB-ключ авторизован, включить службу доступности** (один раз; подробно — раздел
«Очистка ЦТВ после закрытия (2.1-NA)», там же как включить с пульта):

```bash
adb shell settings put secure enabled_accessibility_services io.sberstop/io.sberstop.CtvCloseWatcher
adb shell settings put secure accessibility_enabled 1
adb shell settings get secure enabled_accessibility_services
```

**Порядок важен:** сначала авторизовать ключ (шаги 1–4 выше), потом включать службу — иначе при
включении службы ADB-подключение попросит разрешение отладки, а экрана с отпечатком ключа рядом может
не быть. Если ключ уже авторизован, запрос не появится.

## Как проверить, что работает

На экране приложения: «Последняя проверка: HH:mm — Всё на месте: приложений Сбера нет, главный
экран — NA Clean Toour TV».

Команды для проверки (выполняет Lead):

```bash
# Главный экран = NA
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
# ожидается: com.gathin.tvhome/.MainActivity

# Заставка
adb shell settings get secure screensaver_components
# ожидается: com.gathin.tvhome/.ClockDream

# Автообновления и рекламный оверлей
adb shell settings get global ota_disable_automatic_update   # 1
adb shell settings get global auto_update_apps               # 0
adb shell appops get ru.sberdevices.services SYSTEM_ALERT_WINDOW   # deny

# Мусора из 20-пакетного списка не должно остаться
adb shell pm list packages | grep -E 'ru\.sberdevices\.(starlauncher|starscreensaver|starmarket|app\.setup|openbeta|starcleaner|smarthome\.promo|starbrowser|videos|music|sberplay|starprofile|subscriptions|parentcontrol|starkidsroom\.prod|bugreporter|survey)$|com\.salute\.star$|com\.sberbank\.settings$|com\.spocky\.projengmenu$'
# ожидается: пусто (starlivetv, startube, rcfupdater, gamepad.service и др. «оставленные» — могут быть)

# Отладка/ключ и задания
adb shell getprop service.adb.tcp.port          # 5555
adb logcat -d -s SberStop | tail -30             # события ключа и проходов
adb shell dumpsys jobscheduler | grep -i -A2 sberstop

# 2.1-NA: служба доступности и очистка ЦТВ
adb shell settings get secure enabled_accessibility_services  # io.sberstop/io.sberstop.CtvCloseWatcher
adb shell settings get secure accessibility_enabled           # 1
adb shell dumpsys accessibility | grep -i sberstop            # служба видна и включена
adb shell pm list packages | grep limehd.ru.ctv               # приложение на месте (pm clear его не удаляет)
# после того как на ТВ открыли и закрыли ЦТВ (или выключили экран):
adb logcat -d -s SberStop | grep -i CtvCloseWatcher
# ожидается: "CtvCloseWatcher: уход с экрана — pm clear limehd.ru.ctv: Success"
```

Проверка после перезагрузки: `adb reboot`, дождаться загрузки, до ~2 минут проверить главный экран и
`pm list packages` — вернувшийся лаунчер/мусор должны снова исчезнуть. Дополнительно после
перезагрузки посмотреть `settings get secure enabled_accessibility_services`: служба доступности
должна остаться включённой (если владелец её включал).

## Риски и нерешённые места

1. **Удаление, а не выключение.** Список владельца применяется именно через `pm uninstall --user 0`
   (как в оригинальном «Сбер-Стоп» 1.4 и в README владельца), а не `pm disable-user`, как советует
   список natx5. Данные удалённых приложений теряются; системные можно вернуть
   `cmd package install-existing --user 0 <пакет>`, приложения из `/data` — только factory reset /
   переустановкой. Плюс освобождается место (кэш обновлений, заставка).
2. **Список отличается от natx5.** В нём есть `ru.sberdevices.app.setup` (установщик приложений
   Сбера) и `com.sberbank.settings`, которых нет в 26-списке natx5, и нет «оставленных» пакетов
   (`starlivetv`, `startube`, `rcfupdater`, `gamepad.service`, `huaweicast`, `tv`,
   `com.maxhub.screenshare.server.tv`, `com.screenshare.platform`, `com.cvte.dssshare.dlna`).
   Если Сбер вернёт что-то из «оставленных», сторож его не тронет — так решил владелец.
3. **`com.spocky.projengmenu`** (Projectivy Launcher) входит в список: если он когда-нибудь
   понадобится как второй лаунчер, строку надо убрать из `PACKAGES` и пересобрать.
4. **ADB-порт 5555 после перезагрузки** (см. выше) — если он не поднялся, сторож бесполезен, пока
   ADB не включат снова. Команда для настойчивости: `setprop persist.adb.tcp.port 5555`.
5. **Свежеустановленное приложение в состоянии stopped** — до первого запуска не получает
   `BOOT_COMPLETED`. Один раз открыть вручную (заодно авторизуется ключ).
6. **Старый Free My TV (`io.freemytv`)**, если он ещё установлен и его собственный сторож работает,
   будет драться за главный экран с этим сторожем. При переходе на NA — выключить/удалить
   (`adb shell pm disable-user --user 0 io.freemytv`).
7. **«Ротация» StarOS:** при нехватке места в `/data` (порог ~355 МБ) StarOS удаляет
   («ротирует») установленные пользователем приложения — под это могут попасть и NA-лаунчер, и сам
   сторож. В natx5 рекомендуют `adb shell settings put global sys_storage_threshold_percentage 1`
   (порог ~231 МБ). Сторож это **не** делает намеренно (глобальная настройка), решение за Lead.
8. **Рекламный оверлей**: `appops … SYSTEM_ALERT_WINDOW deny` может скрыть визуальные карточки
   голосового ассистента (голос работает). Вернуть: `appops set ru.sberdevices.services
   SYSTEM_ALERT_WINDOW allow`.
9. **Фоновая доставка заданий** — best effort: JobScheduler может откладывать задания в Doze/standby;
   поэтому цепочка 2 минуты, а не таймер.
10. **Сырой ADB-клиент** (`Adb.java`) реализует протокол adbd вручную; на будущих прошивках формат
    может измениться. Проверено только логикой/сборкой — на устройстве этот билд ещё не запускался.
11. **Пакет/подпись `io.sberstop`+старый keystore** выбраны намеренно: если на ТВ стоит прежний
    «Сбер-Стоп», APK встанет обновлением и старое приложение не будет мешать. Если подпись на ТВ
    другая — нужен `adb uninstall io.sberstop` перед установкой.
12. **APK не байт-репродуцируем** (zip-таймстампы): каждый пересбор даёт новый sha256. Хеш выше —
    ровно для лежащего здесь файла.
13. **Очистка ЦТВ = сброс входа (главный риск 2.1-NA).** `pm clear limehd.ru.ctv` удаляет и кеш, и
    данные: после каждого закрытия ЦТВ (в том числе после простого нажатия «Домой» или выключения
    экрана) приложение запускается как впервые — заново вход/привязка, заново настройки. Отменить
    нельзя, «только кеш» Android по ADB не умеет. Если такое поведение не нужно — снять флажок
    «Чистить «Цифровое ТВ» после закрытия» (очистка перестанет работать, остальной сторож — нет).
14. **Ложное «закрытие».** Любой уход ЦТВ с переднего плана считается закрытием: переход в другой
    пакет, нажатие «Домой», выключение экрана пультом. Оверлеи (заставка, шторка, системные диалоги,
    запрос отладки) закрытием **не** считаются — перед очисткой служба перепроверяет фактическую
    активность через `dumpsys`. Но краткий уход «на секунду» всё равно приведёт к очистке: антидребезг
    только не даёт чистить чаще раза в 3 с, а 1.5-секундная задержка — это «устоялось ли окно».
15. **Служба доступности может быть выключена** системой/после обновления/сброса ТВ — тогда очистка
    не работает (сторож об этом пишет в строке статуса: «… · служба доступности не включена»).
    Проверка: `adb shell settings get secure enabled_accessibility_services`.
16. **Очистка зависит от ADB-порта 5555** так же, как и остальной сторож: без него `pm clear` не
    выполнится (в статусе будет «(ошибка)», в logcat — ошибка ADB). Служба при этом не падает.
17. **Android 13 и «ограниченные настройки» (restricted settings).** APK ставится не из Play, поэтому
    на некоторых прошивках переключатель службы в Настройках может быть заблокирован («Restricted
    setting»). Тогда либо включить в «Сведения о приложении» → ⋮ → «Разрешить ограниченные настройки»
    (пункт может требовать подтверждения), либо включить службу командами `settings put secure …` из
    раздела 2.1-NA. После включения проверить `dumpsys accessibility | grep -i sberstop`.

## Состав каталога и пересборка

```
work/watchdog/
├── SberStop-NA.apk        готовый подписанный APK (sha256 выше)
├── build-linux.sh         сборка без Gradle: aapt2 → javac → d8 → jar → zipalign → apksigner
├── AndroidManifest.xml    манифест 2.1-NA
├── src/io/sberstop/       Adb.java, Blacklist.java, BootReceiver.java, CtvCloseWatcher.java,
│                          KeeperJob.java, KeeperService.java, MainActivity.java
├── res/drawable-nodpi/    icon.png, banner.png (от исходного сторожа)
├── res/xml/               accessibility_service_config.xml (служба доступности 2.1-NA)
├── res/values/            strings.xml (описание службы для «Специальных возможностей»)
├── sberstop.keystore      ключ подписи/обновления (alias sberstop, пароль sberstop)
└── out/                   промежуточные файлы сборки (можно удалять)
```

Пересборка:

```bash
cd /home/ulalex/Проекты/ТВ/work/watchdog
./build-linux.sh
```

Скрипт использует JDK 17 `/home/ulalex/Проекты/ТВ/rebuild/jdk` и Android SDK
`/home/ulalex/Проекты/ТВ/rebuild` (build-tools 34.0.0, platforms/android-34); пути можно
переопределить (`JDK=... SDK=... ./build-linux.sh`). В конце печатает sha256 и `aapt2 dump badging`.

## Откат

```bash
# выключить сторожа (не удалять), затем вернуть пакеты
adb shell pm disable-user --user 0 io.sberstop
adb shell cmd package install-existing --user 0 ru.sberdevices.starlauncher
adb shell cmd package set-home-activity ru.sberdevices.starlauncher/.ui.mainscreen.MainActivity
adb shell appops set ru.sberdevices.services SYSTEM_ALERT_WINDOW allow
adb shell settings put global ota_disable_automatic_update 0
adb shell settings put global auto_update_apps 1
# включить обратно: adb shell pm enable io.sberstop
```

Полный откат к прежнему состоянию (Free My TV) — сброс ТВ или переустановка старых APK; удалённые
через `--user 0` приложения из `/data` сами не вернутся.

---

Собрано 2026-11-25 (2.1-NA, `versionCode=7`) и 2026-11-23 (2.0-NA) в
`/home/ulalex/Проекты/ТВ/work/watchdog`. Телевизор (192.168.3.110) в рамках этой задачи не
затрагивался: **никаких adb-команд к нему не выполнялось**, только сборка и локальные проверки APK
(`apksigner verify`, `zipalign -c`, `aapt2 dump badging/xmltree`, `dexdump`).
