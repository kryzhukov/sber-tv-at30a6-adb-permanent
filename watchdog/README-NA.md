# «Сбер-Стоп NA» — сторож для Сбер-ТВ и универсальная очистка ЦТВ на других ТВ

Приложение-сторож для телевизора **SaluteTV AT30A6** (Amlogic T962D4, Android 13 / StarOS, прошивка
`TP1A.220905.004.A2`) с лаунчером **NA Clean Toour TV** (`com.gathin.tvhome`) из
[natx5cleanyourtv](https://github.com/kryzhukov/natx5cleanyourtv).

Основа — исходный проект «Сбер-Стоп» 1.4 из `/tmp/sberat30/sberstop` (Java, сборка без Gradle).
Задача этого билда — та же логика и **тот же проверенный список из 20 пакетов**, что у автора
[sber-tv-at30a6-free-my-tv](https://github.com/kryzhukov/sber-tv-at30a6-free-my-tv) (владелец ТВ:
«по очистке мусора опыт есть, только лаунчер не бери оттуда»), но с главным экраном и заставкой
NA Clean Toour TV.

**С 2.2-NA APK универсальный.** Его можно ставить и на ТВ, которые **не** являются Сбер-ТВ
(YAOS/Hartens и др.): там работает только очистка «Цифрового ТВ» (`limehd.ru.ctv`), а вся Сбер-логика
(пакеты Сбера, Home, заставка, автообновления, `appops`) **пропускается**, и на телевизоре ничего не
меняется. Подробности — в разделе «Универсальный режим: guard «это Сбер-ТВ?» (2.2-NA)».

**С 2.3-NA очистку ЦТВ на таком ТВ можно включить БЕЗ службы доступности** — фоновым сервисом
`io.sberstop.CleanerService`, который запускается с ПК по adb без UI
(`am start-foreground-service -n io.sberstop/.CleanerService`). Это ответ на YAOS/Hartens, где
прошивка не даёт включить сторонний AccessibilityService. Подробности — в разделе «Фоновая очистка
ЦТВ без службы доступности: CleanerService (2.3-NA)».

> **2026-11-25, версия 2.3-NA (`versionCode=9`).** Добавлен фоновый сервис `io.sberstop.CleanerService`
> — очистка ЦТВ **без службы доступности**. В манифесте он `android:exported="true"`, поэтому
> запускается с ПК по adb без UI. Раз в ~2 с опрашивает активную активити через ADB и, если
> `limehd.ru.ctv` ушло с экрана, делает `pm clear limehd.ru.ctv`; при `ACTION_SCREEN_OFF` — очистка,
> если ЦТВ было на экране; антидребезг 3 с; переключатель `clear_ctv_on_close` (default true);
> low-importance уведомление, канал `cleaner`; автозапуск из `BootReceiver`
> (`startForegroundService`, при исключении — `startService`). Новых разрешений нет, кнопка
> «Проверить сейчас» сервис не запускает. Полное описание и команды проверки — в разделе «Фоновая
> очистка ЦТВ без службы доступности: CleanerService (2.3-NA)».
>
> **2026-11-25, версия 2.2-NA (`versionCode=8`).** `Blacklist.apply()` перед любыми изменениями
> проверяет, что ТВ — Сбер-ТВ: установлены **и** сервисы Сбера (`ru.sberdevices.services` или
> `ru.sberdevices.starlauncher`), **и** лаунчер NA (`com.gathin.tvhome`). Иначе проход **сразу**
> возвращает «не Сбер-ТВ: Sber-очистка пропущена» и **ничего не меняет** (даже не открывает ADB).
> На не-Sber ТВ остаётся только служба доступности с очисткой ЦТВ. Дополнительно (2.2-NA) ADB-ключ
> можно подложить во внешний каталог приложения — `/sdcard/Android/data/io.sberstop/files/adbkey` и
> `adbkey.pub` — без root и без запроса авторизации на ТВ (см. раздел «ADB-ключ приложения…»).
>
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
| Размер | **78 473 байт** |
| **sha256** | **`b378b147207a85eba298d4ad966d3a3bf0c33c4c00649eaedee019d8bca5e751`** |
| md5 | `3b721cc618da86a3dd337223e03badec` |
| Пакет | **`io.sberstop`** (как у старого сторожа — см. «Подпись и пакет») |
| Версия | `versionCode=9`, `versionName=2.3-NA` |
| minSdk / targetSdk | **26** / **33** (compileSdk 34) |
| Подпись | `sberstop.keystore`, alias `sberstop`, пароль `sberstop`; SHA-256 сертификата `5F:38:FC:8A:75:4C:AE:32:B9:0E:0B:D7:C5:5A:56:E3:6E:01:2C:36:A9:5F:DB:00:46:B0:86:EA:47:E0:79:05` |
| Схемы подписи | v2 + v3 (v1 не нужна при minSdk 26; v4 отключена) |
| Разрешения | `INTERNET`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `QUERY_ALL_PACKAGES` — **новых разрешений 2.3-NA не добавляет** |
| Службы | `io.sberstop.CtvCloseWatcher` (служба доступности, включается вручную) и **`io.sberstop.CleanerService`** (2.3-NA: `exported=true`, запускается с ПК по adb; foreground, канал `cleaner`) |
| Тег в logcat | `SberStop` |

Проверено локально (без телевизора): `apksigner verify` — `Verifies` (exit 0); подпись v2+v3,
сертификат тот же, что у 2.1-NA/2.2-NA (SHA-256 `5f38fc8a…e07905`); `zipalign -c -v 4` — `Verification
succesful`; `aapt2 dump badging` показывает `package: name='io.sberstop' versionCode='9'
versionName='2.3-NA'`, `sdkVersion:'26'`, `targetSdkVersion:'33'`, те же 4 разрешения;
`aapt2 dump xmltree` — `versionCode=9`, служба **`.CleanerService` с `exported=true`** и
`foregroundServiceType=0x1` (dataSync), служба `.CtvCloseWatcher` с `BIND_ACCESSIBILITY_SERVICE`,
`exported=true`, intent-filter `android.accessibilityservice.AccessibilityService` и meta-data на
`@xml/accessibility_service_config`; файл `res/xml/accessibility_service_config.xml` есть в APK, его
флаги скомпилированы ровно в `typeWindowStateChanged|typeWindowsChanged` (`0x00400020`),
`feedbackGeneric` (`0x10`), `flagDefault` (`0x1`), `notificationTimeout=100`,
`canRetrieveWindowContent=false`; `dexdump` — DEX 038 (API 26), класс
`Lio/sberstop/CleanerService;` наследует `android.app.Service` и содержит методы `onCreate`,
`onStartCommand`, `onBind`, `onDestroy`, `start(Context)`, `isRunning()`, `pollOnce`, `clearCtv`,
`resumedActivity`, `ensureAdb`, `startForegroundSafely`, `buildNotification`, `record`,
`clearOnScreenOff`; в `BootReceiver` есть вызов `CleanerService.start`, в `MainActivity` —
`CleanerService.isRunning` (2 вызова), а кнопка «Проверить сейчас» запускает только `KeeperService`;
строки `dumpsys activity activities | grep -m1 mResumedActivity` (плюс запасные `topResumedActivity` и
`dumpsys window | grep -m1 mCurrentFocus`), `pm clear `, `limehd.ru.ctv`, `clear_ctv_on_close`,
`cleaner`, `CleanerService`, «Слежу за «Цифровым ТВ»», `android.intent.action.SCREEN_OFF`,
`startForegroundService`/`startService`; класс `Lio/sberstop/Adb;` с `externalKeyDir`; прежние строки
`ru.sberdevices.services`, `ru.sberdevices.starlauncher`, `com.gathin.tvhome`,
**«не Сбер-ТВ: Sber-очистка пропущена»** на месте; в `classes.dex` — все 20 пакетов списка;
«оставленные» пакеты (`starlivetv`, `startube`, `rcfupdater`, `gamepad.service`, `huaweicast`,
`maxhub`, `screenshare`, `dssshare`) в dex отсутствуют.

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
и элементы на экране (см. следующий раздел). 2.2-NA на Сбер-ТВ тоже ничего из этого не меняет:
список, Home и заставка те же; добавлены guard для не-Sber ТВ (раздел «Универсальный режим…»),
строка «Режим: …» на экране и возможность подложить внешний ADB-ключ. 2.3-NA не меняет ни список, ни
Сбер-логику: добавлен только фоновый `CleanerService` (очистка ЦТВ без службы доступности), строка его
статуса на экране и автозапуск из `BootReceiver`; остальные компоненты — те же.

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

## Универсальный режим: guard «это Сбер-ТВ?» (2.2-NA)

2.2-NA ставится на любые ТВ, но **Сбер-логика выполняется только на Сбер-ТВ**. В
`Blacklist.apply()` самым первым действием (до любых изменений и даже до подключения по ADB) стоит
проверка `isSberTv`:

```java
static boolean isSberTv(Context c) {
    boolean sber = isInstalled(c, "ru.sberdevices.services")
            || isInstalled(c, "ru.sberdevices.starlauncher");
    boolean na = isInstalled(c, "com.gathin.tvhome");
    return sber && na;
}
```

Условие — **оба** признака: на ТВ есть **и** сервисы Сбера (`ru.sberdevices.services` или
`ru.sberdevices.starlauncher`), **и** лаунчер NA (`com.gathin.tvhome`).

* **Условие выполнено (Сбер-ТВ):** всё работает как в 2.1-NA — главный экран и заставка NA, удаление
  пакетов из списка, `ota_disable_automatic_update`/`auto_update_apps`, `appops` на
  `ru.sberdevices.services`, очистка ЦТВ.
* **Условие НЕ выполнено (не-Sber ТВ, например YAOS/Hartens):** проход сразу возвращает статус
  `не Сбер-ТВ: Sber-очистка пропущена (…)` и **ничего не меняет**: не трогает Home, заставку,
  настройки и пакеты и **не открывает ADB** (значит, на чужом ТВ не появится даже запрос «Разрешить
  отладку?»). Цепочка заданий (2 минуты) и `BootReceiver` продолжают работать, но каждый проход — это
  только проверка пакетов через `PackageManager`. Активная функция на не-Sber ТВ — очистка ЦТВ: служба
  доступности `CtvCloseWatcher` (2.1-NA) и/или фоновый `CleanerService` (2.3-NA, работает без службы
  доступности — см. следующий раздел).
* В скобках после статуса пишется причина: `нет сервисов Сбера: …` и/или `нет лаунчера NA: …`.
* Пакеты проверяются через `PackageManager` (в манифесте объявлен `QUERY_ALL_PACKAGES`), а не через
  shell — так guard не зависит от ADB и не требует авторизации отладки.
* На экране появилась строка **«Режим: …»**: «Режим: Сбер-ТВ — …» либо «Режим: не Сбер-ТВ — работает
  только очистка «Цифрового ТВ»; приложения, главный экран и настройки ТВ не трогаются». Статус
  последнего прохода показывает тот же текст «не Сбер-ТВ: Sber-очистка пропущена …» (синим, не
  красным — это не ошибка).
* Уведомление кнопки «Проверить сейчас» на не-Sber ТВ: «Не Сбер-ТВ: Sber-очистка пропускается…».

**Важно:** если на Сбер-ТВ нет лаунчера NA (`com.gathin.tvhome`), guard тоже не пропустит Сбер-логику
— сторож ничего не удалит и не назначит Home. Это намеренно: удалять лаунчер Сбера, не имея куда
переключить Home, нельзя. Сначала надо поставить NA-лаунчер.

Проверка режима (команды для Lead, не для этой задачи):

```bash
adb shell pm list packages | grep -E 'ru\.sberdevices\.(services|starlauncher)$'  # есть хотя бы один — признак Сбера
adb shell pm list packages | grep com.gathin.tvhome                              # нужен на Сбер-ТВ
adb logcat -d -s SberStop | grep -E 'не Сбер-ТВ|Sber-очистка'                    # на не-Sber ТВ
```

## Фоновая очистка ЦТВ без службы доступности: CleanerService (2.3-NA)

На ТВ, где прошивка (**YAOS/Hartens** и др.) **не даёт включить сторонний AccessibilityService**,
очистка «Цифрового ТВ» работает через фоновый сервис `io.sberstop.CleanerService` — служба
доступности для этого не нужна. Сервис в манифесте объявлен `android:exported="true"`, поэтому его
можно запустить **с ПК по adb, без UI и без root**:

```bash
adb shell am start-foreground-service -n io.sberstop/.CleanerService
```

* Запускается как **foreground service** с уведомлением низкой важности
  (`IMPORTANCE_LOW`, канал **`cleaner`**, заголовок «Сбер-Стоп NA: очистка ЦТВ»). Уведомление
  обновляется после каждой попытки очистки («Очищено в чч:мм:сс · слежу за «Цифровым ТВ»»).
* Ничего не удаляет и не меняет в системе: только `pm clear limehd.ru.ctv` (кеш + данные), как
  `CtvCloseWatcher`. Сбер-логику (`Blacklist`) не вызывает, на не-Sber ТВ ничего не трогает.
* Живёт до остановки (`START_STICKY`: если система убьёт сервис, она попробует перезапустить его).

### Как устроен цикл

Всё — в отдельном фоновом потоке (`HandlerThread`, приоритет background), главный поток не
блокируется и ANR исключён:

1. Раз в **~2 с** (2000 мс) сервис через существующий ADB-клиент `Adb.java` (`127.0.0.1:5555`, тот же
   ключ, что у остального сторожа) спрашивает активную активити:
   `dumpsys activity activities | grep -m1 mResumedActivity` (запасные варианты:
   `topResumedActivity`, `dumpsys window | grep -m1 mCurrentFocus`).
2. Если на экране `limehd.ru.ctv` — запоминаем «ЦТВ было на экране» (взводим «курок») и время
   последнего показа.
3. Если ЦТВ **нет**, и **предыдущая проверка показывала его на экране** (или с последнего показа
   прошло **>5 с** — повтор после неудачной очистки), и переключатель `clear_ctv_on_close` включён,
   и с прошлой очистки прошло **>3 с** (антидребезг) — выполняется `pm clear limehd.ru.ctv`
   (кеш + данные; отдельной команды «только кеш» Android по ADB не даёт).
4. **`ACTION_SCREEN_OFF`** (приёмник внутри сервиса): если ЦТВ было на переднем плане — очистка
   сразу, не дожидаясь следующего опроса (при погашенном экране `dumpsys` может показывать ЦТВ
   «resumed», но это всё равно закрытие).
5. **Ошибка ADB** — пишется в лог (`adb logcat -s SberStop`), соединение закрывается, цикл
   продолжается; переподключение с паузой 10 с, чтобы не спамить запрос «Разрешить отладку?».
   После 3 неудачных `pm clear` подряд сервис ждёт следующего показа ЦТВ (не долбит вечно).
6. Любое неожиданное исключение внутри цикла перехватывается — сервис не падает.

**Защита данных (важно):** очистка происходит, только если сервис **сам видел ЦТВ на экране** во
время своей работы. Если сервис запустили, когда ЦТВ не открывали, он **не** будет чистить «на всякий
случай» (вход и настройки ЦТВ сохранятся). После успешной очистки «курок» сбрасывается — повторного
`pm clear` без нового показа ЦТВ не будет.

### Переключатель, ключ ADB, уведомление

* Переключатель тот же: SharedPreferences **`settings`**, ключ **`clear_ctv_on_close`**, по умолчанию
  **включён**. Выключен — сервис ничего не чистит (пишет в лог «очистка ЦТВ выключена»); после
  включения обратно нужно, чтобы ЦТВ снова появилось на экране.
* ADB-ключ — тот же, что у остальных проходов сторожа: внешний
  (`/sdcard/Android/data/io.sberstop/files/adbkey` + `adbkey.pub`, см. «Подложить готовый ключ без
  root (2.2-NA)») или внутренний. На не-Sber ТВ внешний ключ уже авторизованного ПК — самый простой
  способ: тогда запрос «Разрешить отладку?» на ТВ не появится вообще.
* Уведомление — low-importance; на Android 13 при targetSdk 33 без разрешения `POST_NOTIFICATIONS`
  оно может быть не видно в шторке (сервис при этом работает; его видно в «Активных приложениях»). На
  работу очистки это не влияет; новых разрешений APK не просит.

### Автозапуск и взаимодействие с 2.1-NA

* После **перезагрузки ТВ** и после **обновления сторожа** `BootReceiver` запускает сервис:
  `startForegroundService`, а если Android 12+ бросит исключение (старт foreground-службы из фона) —
  логирует и пробует `startService`. Явный запуск `am start-foreground-service` с ПК работает всегда.
* **Кнопка «Проверить сейчас»** сервис **не** запускает (как и раньше — только `KeeperService`).
* `CtvCloseWatcher` (служба доступности) и `CleanerService` **не конфликтуют**: оба пишут результат
  последней очистки в одни `state`-настройки. Если служба доступности на ТВ включается — можно
  оставить оба пути; на YAOS/Hartens работает только `CleanerService`.
* На экране «Сбер-Стоп NA» добавлена строка **«Фоновая очистка ЦТВ: работает/не запущен»** с
  командой запуска. Если служба доступности выключена, но `CleanerService` запущен, строка статуса
  очистки ЦТВ больше не пишет «служба доступности не включена».

### Как запустить и проверить (команды для Lead; в этой задаче я их НЕ выполнял)

```bash
# 1) запустить сервис (UI не нужен; exported=true):
adb shell am start-foreground-service -n io.sberstop/.CleanerService

# 2) сервис в списке и в logcat:
adb shell dumpsys activity services io.sberstop | grep -i -A3 CleanerService
adb logcat -d -s SberStop | grep CleanerService
# ожидается: «CleanerService создан: слежу за «Цифровым ТВ» (limehd.ru.ctv) каждые 2000 мс…»
#            «CleanerService запущен: опрос экрана каждые 2000 мс; канал "cleaner"»
#            «CleanerService: ADB подключён (127.0.0.1:5555)»

# 3) открыть ЦТВ, выйти из него (Домой/другое приложение) или выключить экран;
#    в logcat ожидается:
adb logcat -d -s SberStop | grep CleanerService
# «CleanerService: ЦТВ на экране (…limehd.ru.ctv…) — беру на заметку»
# «CleanerService: ЦТВ ушло с экрана (последний показ NNN мс назад) — pm clear limehd.ru.ctv: Success»
# (или «CleanerService: экран выключен — pm clear limehd.ru.ctv: Success»)

# 4) открыть ЦТВ снова — должно запуститься как впервые (предложить вход/настройку).

# остановить сервис (например, чтобы вернуться к очистке только через службу доступности):
adb shell am stopservice -n io.sberstop/.CleanerService
```

**Если сервис не чистит:** смотреть `adb logcat -d -s SberStop | grep CleanerService` — обычно там
«ADB недоступен (debugging is off on the TV (port 5555 closed)…)» (ADB на ТВ выключен) или
«debugging permission wasn't accepted on the TV» (ключ не подтверждён: подложить внешний ключ или
нажать «Всегда разрешать»). Порт/ключ проверяются так же, как у остального сторожа:
`adb shell getprop service.adb.tcp.port` и строка «Проверка ADB-ключа» на экране.

## Очистка ЦТВ после закрытия (2.1-NA)

Новая служба доступности `io.sberstop.CtvCloseWatcher` очищает **кеш и данные** приложения
«Цифровое ТВ» (`limehd.ru.ctv`) после того, как ЦТВ ушло с переднего плана или выключился экран.
Очистка — команда `pm clear limehd.ru.ctv` через уже существующий ADB-клиент `Adb.java`
(`127.0.0.1:5555`, тот же ключ приложения, что у остальных проходов сторожа; отдельной авторизации
не нужно). `pm clear` в Android — это **и кеш, и данные**: после очистки ЦТВ запускается как
впервые (вход, настройки, «избранное» в приложении сбрасываются). Отдельной команды «почистить
только кеш» ADB не даёт.

> **2.3-NA:** если прошивка не даёт включить службу доступности (YAOS/Hartens), тот же результат
> даёт фоновый `CleanerService` — см. раздел «Фоновая очистка ЦТВ без службы доступности:
> CleanerService (2.3-NA)». Оба механизма используют один и тот же ADB-ключ и один переключатель
> `clear_ctv_on_close`.

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

### Обновление поверх 2.0-NA/2.1-NA/2.2-NA и перезагрузки

* APK 2.3-NA ставится обновлением поверх 2.0-NA/2.1-NA/2.2-NA (тот же пакет/подпись). **Служба
  доступности сама не включается** — её нужно включить один раз после установки (или она останется
  включённой, если уже была включена при обновлении; после обновления пакета Android обычно
  перезапускает привязку, но гарантий нет — проверить `settings get secure
  enabled_accessibility_services`).
* **2.3-NA: `CleanerService` поднимается автоматически** после обновления пакета (`BootReceiver`
  получает `MY_PACKAGE_REPLACED`) и после перезагрузки; вручную его запускать не нужно, но можно —
  `adb shell am start-foreground-service -n io.sberstop/.CleanerService`.
* Включённые службы доступности переживают перезагрузку ТВ (настройка `secure`), но не сброс ТВ.
  После `adb uninstall io.sberstop` / очистки данных сторожа — включить заново.
* Если служба выключена, остальной сторож работает как раньше: на Сбер-ТВ — главный экран, заставка,
  удаление мусора, автообновления, задания; на не-Sber ТВ — только проходы-проверки режима (без
  изменений). Очистку ЦТВ при этом может делать `CleanerService` (2.3-NA), если он запущен.
* При обновлении 2.1-NA → 2.2-NA → 2.3-NA формат и расположение ключа не меняются: если ADB-ключ уже
  был авторизован, сторож продолжит работать с ним молча. Внешний ключ (если подложен) имеет
  приоритет; его же использует `CleanerService`.

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

### Подложить готовый ключ без root (2.2-NA)

Начиная с 2.2-NA приложение сначала ищет ключ **во внешнем каталоге приложения** (2.2-NA,
`Adb.externalKeyDir`), и только если его там нет — во внутреннем:

```
/sdcard/Android/data/io.sberstop/files/adbkey       PKCS#8 DER (ровно как внутренний adbkey)
/sdcard/Android/data/io.sberstop/files/adbkey.pub   X.509 DER / SubjectPublicKeyInfo (как внутренний adbkey.pub)
```

Если оба файла есть и читаются (KeyFactory RSA, `PKCS8EncodedKeySpec`/`X509EncodedKeySpec`) —
используется этот ключ (приоритет над внутренним), в logcat пишется
`ADB: использую ключ из внешнего каталога …`. Если файлов нет или они битые — всё как раньше:
внутренний ключ, иначе новый сгенерированный. Внешний каталог заполняется `adb push` **без root** —
так можно заранее положить ключ **уже авторизованного ПК** и не показывать на ТВ запрос «Разрешить
отладку?».

Ключ ПК на Linux — это PEM PKCS#8 (`~/.android/adbkey`), а приложению нужны DER-файлы, поэтому
сначала конвертация (openssl есть в системе):

```bash
openssl pkcs8 -topk8 -inform PEM -outform DER -in ~/.android/adbkey -nocrypt -out adbkey
openssl rsa -in ~/.android/adbkey -pubout -outform DER -out adbkey.pub
# adbkey.pub здесь — X.509 SubjectPublicKeyInfo; это НЕ «base64 adb-ключ» из ~/.android/adbkey.pub

adb shell mkdir -p /sdcard/Android/data/io.sberstop/files   # если приложение ещё ни разу не запускалось
adb push adbkey     /sdcard/Android/data/io.sberstop/files/adbkey
adb push adbkey.pub /sdcard/Android/data/io.sberstop/files/adbkey.pub
```

`adbkey` и `adbkey.pub` должны быть **от одной пары** (иначе ТВ отклоняет подпись). Точный путь внешнего
каталога — тот, что приложение возвращает из `getExternalFilesDir(null)` (обычно
`/sdcard/Android/data/io.sberstop/files`). Файлы можно перезаписать в любой момент — ключ читается при
следующем подключении. Проверить, что подхватился:

```bash
adb logcat -d -s SberStop | grep -E 'ADB: использую ключ|ADB: внешний ключ'
```

Ограничение/риск: внешний ключ — обычный файл в общей памяти; кто имеет доступ к `adb shell` (или к
общей памяти на старых Android), может его прочитать. Это осознанный компромисс ради установки без
root; ключ ПК даёт полный ADB-доступ к ТВ, поэтому после настройки файлы можно удалить
(`adb shell rm /sdcard/Android/data/io.sberstop/files/adbkey*`) — приложение вернётся к внутреннему
ключу (и, если он не авторизован, снова спросит разрешение).

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
# проход по списку (кнопка «Проверить сейчас»):
adb shell am start-foreground-service -n io.sberstop/.KeeperService

# 2.3-NA: очистка ЦТВ без службы доступности (видно в logcat и в уведомлении «Очистка ЦТВ»):
adb shell am start-foreground-service -n io.sberstop/.CleanerService
```

`CleanerService` (2.3-NA) запускается и без UI вообще: `exported=true`, foreground-служба с
low-importance уведомлением, автозапуск после перезагрузки/обновления. Как проверить — раздел
«Фоновая очистка ЦТВ без службы доступности: CleanerService (2.3-NA)».

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

**Не-Sber ТВ (2.2-NA/2.3-NA):** ставить так же (`adb install -r`), но Сбер-часть не выполнится — на
экране будет «Режим: не Сбер-ТВ …». Нужно только:
1. один раз открыть приложение (снять stopped);
2. очистка ЦТВ: **если прошивка даёт включить службу доступности** — включить её (команды выше или с
   пульта) **ИЛИ**, если не даёт (YAOS/Hartens), — запустить фоновый сервис 2.3-NA:
   `adb shell am start-foreground-service -n io.sberstop/.CleanerService` (можно и то, и другое);
3. чтобы очистка ЦТВ работала, сторожу всё равно нужен ADB на `127.0.0.1:5555` — либо авторизовать
   его ключ (запрос «Разрешить отладку?»), либо заранее подложить ключ уже авторизованного ПК во
   внешний каталог (раздел «Подложить готовый ключ без root (2.2-NA)»). С внешним ключом
   `CleanerService` подключается молча (запроса на ТВ не будет).
Проверка: `adb logcat -d -s SberStop | grep -E 'не Сбер-ТВ|CtvCloseWatcher|CleanerService'` — проходы
пишут «не Сбер-ТВ: Sber-очистка пропущена», служба доступности или `CleanerService` чистит ЦТВ после
закрытия.

## Как проверить, что работает

На экране приложения строка **«Режим: …»** показывает, как сторож определил ТВ: «Режим: Сбер-ТВ —
…» или «Режим: не Сбер-ТВ — работает только очистка «Цифрового ТВ»…». На Сбер-ТВ статус:
«Последняя проверка: HH:mm — Всё на месте: приложений Сбера нет, главный экран — NA Clean Toour TV»;
на не-Sber ТВ: «… — не Сбер-ТВ: Sber-очистка пропущена (нет сервисов Сбера: …)».

Команды для проверки (выполняет Lead):

```bash
# 2.2-NA: определить режим (то же, что делает guard)
adb shell pm list packages | grep -E 'ru\.sberdevices\.(services|starlauncher)$' # на Сбер-ТВ — есть
adb shell pm list packages | grep -E '^package:com\.gathin\.tvhome$'             # на Сбер-ТВ — есть
# не-Sber ТВ: первая команда пустая → в logcat/на экране «не Сбер-ТВ: Sber-очистка пропущена»
adb logcat -d -s SberStop | grep -E 'не Сбер-ТВ|Sber-очистка'

# Главный экран = NA (только Сбер-ТВ)
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
# ожидается: com.gathin.tvhome/.MainActivity (на не-Sber ТВ сторож Home НЕ менял)

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

# 2.3-NA: фоновый CleanerService (работает без службы доступности)
adb shell am start-foreground-service -n io.sberstop/.CleanerService   # запустить с ПК
adb shell dumpsys activity services io.sberstop | grep -i -A3 CleanerService   # сервис в списке
adb logcat -d -s SberStop | grep -i CleanerService
# ожидается: "CleanerService создан: слежу за «Цифровым ТВ» (limehd.ru.ctv) каждые 2000 мс…",
#            "CleanerService запущен: опрос экрана каждые 2000 мс; канал \"cleaner\"",
#            "CleanerService: ADB подключён (127.0.0.1:5555)"
# после того как на ТВ открыли и закрыли ЦТВ (или выключили экран):
# "CleanerService: … — pm clear limehd.ru.ctv: Success"
adb shell am stopservice -n io.sberstop/.CleanerService   # остановить (больше не нужен)
```

Проверка после перезагрузки: `adb reboot`, дождаться загрузки, до ~2 минут проверить главный экран и
`pm list packages` — вернувшийся лаунчер/мусор должны снова исчезнуть. Дополнительно после
перезагрузки посмотреть `settings get secure enabled_accessibility_services`: служба доступности
должна остаться включённой (если владелец её включал), а `CleanerService` — подняться сам
(`dumpsys activity services io.sberstop | grep -i CleanerService`, в logcat — «CleanerService создан…»).

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
    В 2.3-NA то же самое делает `CleanerService` (очистка без службы доступности) — флажок
    `clear_ctv_on_close` у них общий.
14. **Ложное «закрытие».** Любой уход ЦТВ с переднего плана считается закрытием: переход в другой
    пакет, нажатие «Домой», выключение экрана пультом. Оверлеи (заставка, шторка, системные диалоги,
    запрос отладки) закрытием **не** считаются — перед очисткой служба перепроверяет фактическую
    активность через `dumpsys`. Но краткий уход «на секунду» всё равно приведёт к очистке: антидребезг
    только не даёт чистить чаще раза в 3 с, а 1.5-секундная задержка — это «устоялось ли окно».
15. **Служба доступности может быть выключена** системой/после обновления/сброса ТВ — тогда очистка
    через неё не работает; если при этом не запущен `CleanerService` (2.3-NA), очистки нет вообще
    (сторож пишет в статусе «… · служба доступности не включена»). Проверка:
    `adb shell settings get secure enabled_accessibility_services` и
    `adb shell dumpsys activity services io.sberstop | grep -i CleanerService`.
16. **Очистка зависит от ADB-порта 5555** так же, как и остальной сторож: без него `pm clear` не
    выполнится (в статусе будет «(ошибка)», в logcat — ошибка ADB). Служба при этом не падает.
17. **Android 13 и «ограниченные настройки» (restricted settings).** APK ставится не из Play, поэтому
    на некоторых прошивках переключатель службы в Настройках может быть заблокирован («Restricted
    setting»). Тогда либо включить в «Сведения о приложении» → ⋮ → «Разрешить ограниченные настройки»
    (пункт может требовать подтверждения), либо включить службу командами `settings put secure …` из
    раздела 2.1-NA. После включения проверить `dumpsys accessibility | grep -i sberstop`.
18. **Guard 2.2-NA — определение только по установленным пакетам.** `isSberTv` смотрит через
    `PackageManager`, установлены ли `ru.sberdevices.services`/`ru.sberdevices.starlauncher` и
    `com.gathin.tvhome`. Если на Сбер-ТВ этих пакетов не видно (например, сервисы Сбера удалены
    «для пользователя» другим инструментом), сторож посчитает ТВ не-Сбер и ничего не тронет — это
    безопасный отказ (лучше не чистить, чем сломать чужой ТВ). Наоборот, если на не-Sber ТВ каким-то
    образом появятся оба признака, сторож начнёт применять Сбер-логику — поэтому признаки выбраны
    так, чтобы на YAOS/Hartens их не было.
19. **Guard требует лаунчер NA на Сбер-ТВ.** Если `com.gathin.tvhome` не установлен, Сбер-очистка
    тоже пропускается (нельзя удалить лаунчер Сбера, не имея куда переключить Home). Сначала ставится
    NA-лаунчер, затем сторож.
20. **На не-Sber ТВ цепочка заданий работает «вхолостую»**: каждые 2 минуты проход сразу
    возвращается после проверки пакетов (без ADB и без изменений). Это дешёво (несколько вызовов
    `PackageManager`), но задания можно считать лишними — они оставлены, чтобы APK был один и тот же
    для всех ТВ.
21. **Внешний ADB-ключ (2.2-NA).** `Adb.java` читает `adbkey`/`adbkey.pub` из
    `getExternalFilesDir(null)`, если оба файла есть и корректны (PKCS#8 DER / X.509 DER). Ключ ПК в
    общей памяти может прочитать `adb shell` (и приложения с доступом к общей памяти на старых
    Android); после настройки файлы лучше удалить. Битые файлы игнорируются — используется внутренний
    ключ. Дополнительных разрешений на внешнее хранилище это не требует (app-specific каталог).
    Формат важен: `~/.android/adbkey.pub` (base64 adb-формат) **не** подходит, нужен DER
    SubjectPublicKeyInfo — команды конвертации в разделе «ADB-ключ приложения…».
22. **CleanerService — постоянный опрос раз в 2 с (2.3-NA).** Это foreground-сервис: раз в 2 с по ADB
    выполняется `dumpsys activity activities | grep …`. Нагрузка на ТВ небольшая, но постоянная (CPU,
    сокет ADB, `dumpsys`), и сервис висит в уведомлениях. Если очистка ЦТВ не нужна — остановить
    `adb shell am stopservice -n io.sberstop/.CleanerService` (после перезагрузки/обновления он
    поднимется снова, т.к. автозапуск из `BootReceiver`). На не-Sber ТВ это единственный способ
    очистки, если службу доступности включить нельзя.
23. **CleanerService чистит только после того, как сам увидел ЦТВ.** Если сервис запустили, когда ЦТВ
    не открывали, он не очистит данные «на всякий случай» (это защита от сброса входа). Очистка
    срабатывает после первого показа ЦТВ и его ухода с экрана (или выключения экрана). После успешной
    очистки «курок» сбрасывается — повторно без нового показа ЦТВ `pm clear` не выполняется.
24. **Уведомление CleanerService на Android 13.** APK не просит `POST_NOTIFICATIONS` (новых разрешений
    2.3-NA не добавляет), поэтому при targetSdk 33 уведомление low-importance может быть не видно в
    шторке — сервис при этом работает (виден в «Активных приложениях»). На очистку это не влияет;
    если уведомление нужно видимым, разрешение надо будет добавлять отдельной версией.
25. **START_STICKY и автозапуск.** После убийства сервиса системой Android попробует перезапустить его
    (`START_STICKY`); после перезагрузки ТВ и после обновления APK его запускает `BootReceiver`. Явный
    `am start-foreground-service` работает всегда. Автозапуск из фона (Android 12+) может быть
    запрещён прошивкой — тогда в logcat будет ошибка `startForegroundService`, и `BootReceiver`
    пробует `startService` (задание 2.3-NA); в этом случае сервис придётся запускать по adb вручную.
26. **Два механизма очистки одновременно.** Если включены и `CtvCloseWatcher`, и `CleanerService`,
    оба пишут результат в одни `state`-настройки и оба могут выполнить `pm clear` (антидребезг у
    каждого свой). Вреда нет (второй `pm clear` уже очищенного приложения безвреден), но для чистоты
    можно оставить один путь: на YAOS/Hartens — `CleanerService`, на Сбер-ТВ — службу доступности.

## Состав каталога и пересборка

```
work/watchdog/
├── SberStop-NA.apk        готовый подписанный APK (sha256 выше)
├── build-linux.sh         сборка без Gradle: aapt2 → javac → d8 → jar → zipalign → apksigner
├── AndroidManifest.xml    манифест 2.3-NA (versionCode=9)
├── src/io/sberstop/       Adb.java, Blacklist.java, BootReceiver.java, CleanerService.java (2.3-NA),
│                          CtvCloseWatcher.java, KeeperJob.java, KeeperService.java, MainActivity.java
├── res/drawable-nodpi/    icon.png, banner.png (от исходного сторожа)
├── res/xml/               accessibility_service_config.xml (служба доступности 2.1-NA)
├── res/values/            strings.xml (описание службы для «Специальных возможностей»)
├── sberstop.keystore      ключ подписи/обновления (alias sberstop, пароль sberstop)
└── out/                   промежуточные файлы сборки (можно удалять)
```

`CleanerService.java` (2.3-NA) — фоновый сервис очистки ЦТВ: `onCreate`/`onStartCommand` поднимают
foreground-уведомление (канал `cleaner`), `HandlerThread` раз в 2 с вызывает `pollOnce()`, который
через `Adb` выполняет `dumpsys … mResumedActivity` и при уходе ЦТВ — `pm clear limehd.ru.ctv`
(антидребезг 3 с); `clearOnScreenOff` обрабатывает `ACTION_SCREEN_OFF`; `start(Context)` —
автозапуск (`startForegroundService` → fallback `startService`); `isRunning()` — для строки статуса
на экране.

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
# 2.3-NA: сначала остановить фоновую очистку ЦТВ (если запущена)
adb shell am stopservice -n io.sberstop/.CleanerService
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

Собрано 2026-11-25 (2.3-NA, `versionCode=9`; ранее 2.2-NA `versionCode=8`, 2.1-NA `versionCode=7` и
2.0-NA) в `/home/ulalex/Проекты/ТВ/work/watchdog`. Телевизоры в рамках этой задачи не затрагивались:
**никаких adb-команд к ним не выполнялось**, только сборка и локальные проверки APK
(`apksigner verify`, `zipalign -c`, `aapt2 dump badging/xmltree`, `dexdump`).
