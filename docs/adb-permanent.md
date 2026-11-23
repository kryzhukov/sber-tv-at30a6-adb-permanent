# Постоянный ADB без запроса «Разрешить отладку»

## Проблема

На AT30A6 `ro.adb.secure=1` (сборка `user`, `ro.debuggable=0`), поэтому adbd требует
авторизации каждого нового ключа. Штатная галочка «Всегда разрешать отладку с этого компьютера»
на этой прошивке **не сохраняется**: `/data/misc/adb/adb_keys` не создавался, в логах
`AdbDebuggingManager` ключ оставался `alwaysAllow = false`, и после любого перезапуска adbd
(а тем более после перезагрузки ТВ) запрос появлялся снова.

Вторая проблема: порт 5555 не постоянный. `service.adb.tcp.port=5555` выставляется заводским
меню/приложением только в текущей загрузке; `persist.adb.tcp.port` пуст, поэтому после
перезагрузки TCP-ADB не поднимался вовсе.

## Решение

Через root (см. [atsudo-root.md](atsudo-root.md)):

| Что | Значение | Зачем |
|---|---|---|
| `/data/misc/adb/adb_keys` | строки `<base64-pubkey> <имя>`; owner `system:shell`, режим `640` | список доверенных ключей — adbd/`AdbDebuggingManager` читают его при загрузке |
| `persist.adb.tcp.port` | `5555` | adbd читает это свойство при старте и слушает TCP |
| `persist.vendor.staros.adb` | `1` | init-триггер из `/vendor/etc/init/*.rc`: `on property:persist.vendor.staros.adb=1 → start adbd` |
| `settings global adb_enabled` | `1` | штатный флаг «отладка по ADB» |

Формат `adb_keys`: каждая строка — публичный ключ в формате ADB (base64) и имя через пробел.
Формат самого блоба (524 байта для RSA-2048, little-endian):

```
uint32 len = 64
uint32 n0inv
uint8  n[256]     (модуль, little-endian)
uint8  rr[256]    (2^4096 mod n, little-endian)
uint32 e          (экспонента, обычно 65537)
```

Публичный ключ компьютера уже лежит в `~/.android/adbkey.pub` — именно в этом формате.
Скрипт [`scripts/install-adb-keys.sh`](../scripts/install-adb-keys.sh) кладёт его (и любые
дополнительные `.pub`) в `adb_keys` и выставляет свойства.

```bash
scripts/install-root-tool.sh     # atsudo.dex на ТВ
scripts/install-adb-keys.sh      # ключ этого ПК + persist.adb.tcp.port
adb reboot                       # система перечитает adb_keys
adb connect 192.168.1.50:5555   # → device, без запроса
```

## Проверка

```bash
adb kill-server && adb start-server       # как после перезапуска компьютера
adb connect 192.168.1.50:5555
adb devices -l                            # 192.168.1.50:5555  device
adb shell getprop persist.adb.tcp.port    # 5555
adb shell wc -l /data/misc/adb/adb_keys   # число доверенных ключей
```

Проверено на живом ТВ: две перезагрузки ТВ подряд + перезапуск adb-сервера на компьютере —
подключение каждый раз сразу `device`, диалог не появляется.

## Добавить ещё один компьютер

```bash
# на новом ПК:  adb keygen ~/.android/adbkey   (или просто первый adb connect)
scripts/install-adb-keys.sh ~/.android/adbkey.pub /path/to/other-pc/adbkey.pub
adb reboot
```

Скрипт **перезаписывает** `adb_keys` переданным набором — так в файле не копятся забытые ключи.

## Ограничения и откат

* Диалог останется для **нового, неизвестного** ключа: `ro.adb.secure=1` — read-only свойство,
  снимается только правкой прошивки (пересборка `build.prop`/`default.prop`), что мы не делаем.
* Убрать доверенные ключи (вернуть запросы):

  ```bash
  scripts/root.sh 'rm -f /data/misc/adb/adb_keys; setprop persist.adb.tcp.port ""'
  adb reboot
  ```

* Заводской сброс стирает `/data` — ключи и `persist`-свойства пропадут, запросы вернутся.
* `adb_keys` даёт **полный доступ** к ТВ: ключ `~/.android/adbkey` на компьютере нужно беречь.
