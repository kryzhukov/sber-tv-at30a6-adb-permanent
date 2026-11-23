# Откат и восстановление

Все действия обратимы, кроме заводского сброса (он стирает данные). Ниже — от частного к полному.

## Вернуть отдельное приложение Сбера

```bash
adb -s 192.168.1.50:5555 shell cmd package install-existing --user 0 <pkg>
```

Например: `ru.sberdevices.starmarket`, `ru.sberdevices.videos`, `ru.sberdevices.starbrowser`,
`ru.sberdevices.starscreensaver`, `ru.sberdevices.starlauncher`, `com.spocky.projengmenu` и т.д.
(полный список — [`scripts/debloat.txt`](../scripts/debloat.txt)).

## Вернуть «Эфир ТВ+» вместо «Цифрового ТВ»

```bash
adb -s 192.168.1.50:5555 install -r backup/limehd.ru.ctvshka-4.4.0.apk
adb -s 192.168.1.50:5555 shell pm uninstall --user 0 limehd.ru.ctv
```

(бэкап старого APK снимается перед удалением, см. [apps.md](apps.md#замена-эфир-тв-на-цифровое-тв);
sha256 файла на проверенном ТВ — `e0f773c244ce87cae09483f6a52948bb07ee6bf87048c433ee4c8d92a238a383`).

## Вернуть лаунчер Сбера и стоковый главный экран

Сначала выключить сторожа (иначе он удалит вернувшееся):

```bash
adb -s 192.168.1.50:5555 shell pm disable-user --user 0 io.sberstop
```

Затем:

```bash
A="adb -s 192.168.1.50:5555 shell"
$A 'cmd package install-existing --user 0 ru.sberdevices.starlauncher'
$A 'cmd package set-home-activity --user 0 ru.sberdevices.starlauncher/.ui.mainscreen.MainActivity'
$A 'settings put secure screensaver_components ru.sberdevices.starscreensaver/ru.sberdevices.starscreensaver.StarScreenSaverService'
$A 'settings put secure screensaver_enabled 1'
$A 'settings put global ota_disable_automatic_update 0'
$A 'settings put global auto_update_apps 1'
adband shell pm uninstall com.gathin.tvhome    # убрать NA (после того как Home уже вернулся к Сберу)
```

Удалить конфиг лаунчера (если он останется на флешке):

```bash
adb -s 192.168.1.50:5555 shell rm -rf /sdcard/Android/data/com.gathin.tvhome
```

## Вернуть запросы ADB (убрать «постоянный» доступ)

```bash
cd scripts
./root.sh 'rm -f /data/misc/adb/adb_keys; setprop persist.adb.tcp.port ""'
adb -s 192.168.1.50:5555 reboot
```

После перезагрузки adbd снова будет спрашивать разрешение, а TCP-порт 5555 сам подниматься перестанет
(включение ADB останется в заводском меню). Заодно можно убрать root-инструмент с ТВ:

```bash
adb -s 192.168.1.50:5555 shell rm -f /data/local/tmp/atsudo.dex
```

Сам сервис `cvte.at_sudo` — вендорный, он никуда не девается; «откатывать» в прошивке нечего
(мы ничего не прошивали и не меняли разделы).

## Вернуть всё сразу (кроме заводского сброса)

```bash
cd scripts
for p in ru.sberdevices.starlauncher ru.sberdevices.starscreensaver ru.sberdevices.starmarket \
         ru.sberdevices.app.setup ru.sberdevices.openbeta ru.sberdevices.starcleaner \
         ru.sberdevices.smarthome.promo ru.sberdevices.starbrowser ru.sberdevices.videos \
         ru.sberdevices.music ru.sberdevices.sberplay ru.sberdevices.starprofile \
         ru.sberdevices.subscriptions ru.sberdevices.parentcontrol ru.sberdevices.starkidsroom.prod \
         ru.sberdevices.bugreporter ru.sberdevices.survey com.salute.star com.sberbank.settings; do
  adb -s 192.168.1.50:5555 shell "cmd package install-existing --user 0 $p"
done
adb -s 192.168.1.50:5555 shell 'pm disable-user --user 0 io.sberstop'
adb -s 192.168.1.50:5555 shell 'cmd package set-home-activity --user 0 ru.sberdevices.starlauncher/.ui.mainscreen.MainActivity'
adb -s 192.168.1.50:5555 shell 'settings put secure screensaver_components ru.sberdevices.starscreensaver/ru.sberdevices.starscreensaver.StarScreenSaverService'
adb -s 192.168.1.50:5555 shell 'settings put global ota_disable_automatic_update 0'
adb -s 192.168.1.50:5555 shell 'settings put global auto_update_apps 1'
```

## Ядерный вариант — заводской сброс

Настройки ТВ → Система → Сброс к заводским настройкам. Это стирает `/data` целиком: приложения,
`adb_keys`, `persist`-свойства, root-инструмент и сторож. ТВ вернётся к стоковому состоянию
(лаунчер Сбера, все приложения Сбера, запросы ADB). После сброса ADB включается заново через
заводское меню CVTE ([device-info.md](device-info.md#как-включить-adb-для-повторного-включения-после-сброса)).
