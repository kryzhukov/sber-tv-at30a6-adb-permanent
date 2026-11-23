# Телевизор SaluteTV AT30A6 — сведения об устройстве

## Свойства

| Свойство | Значение |
|---|---|
| `ro.product.brand` / `ro.product.model` | `SaluteTV` / `SaluteTV` |
| `ro.product.device` / `ro.product.board` | `at30a6` / `at30a6` |
| `ro.hardware` | `amlogic` |
| `ro.cvte.model` | `MD_CVTE_OVERSEA_T962D4_13_0_D18D20_2GB_STARKOS` |
| `ro.cvte.customer` | `SBER` |
| `ro.cvte.panelname` | `LE50VC3_CV500U5_T02_V02_WDX_MS` (50″) |
| SoC | Amlogic T962D4 («t5w»), 4× Cortex-A55, GPU Mali-G31 |
| `ro.product.cpu.abi` | `armeabi-v7a` (32-бит) |
| ОЗУ | ~2 ГБ (`MemTotal` 2 035 556 КБ) |
| `ro.build.version.release` / `sdk` | `13` / `33` |
| `ro.vendor.build.staros_version` | `1.103.80-b32095` (StarOS) |
| `ro.build.fingerprint` | `SaluteTV/SaluteTV/at30a6:13/TP1A.220905.004.A2/eng.tuz_st.20260617.001208:user/release-keys` |
| `ro.build.date` | 2026-06-17 |
| `ro.build.version.security_patch` | `2022-09-05` |
| `ro.build.type` | `user`, `ro.debuggable=0` |
| `ro.adb.secure` | `1` |
| `ro.boot.verifiedbootstate` / `vbmeta.device_state` | `green` / `locked` |
| `ro.boot.veritymode` | `disabled` (dm-verity выключена) |
| SELinux | `Permissive` |
| Экран | 1920×1080 (override 1980×1080), плотность 320 dpi |
| `/data` | 3,0 ГБ (`/dev/block/userdata`) |

Разделы `/vendor`, `/product`, `/odm` смонтированы read-only через `dm-*` (dynamic partitions,
super). Разметка A/B, загрузчик залочен, `dm-verity` выключена — поэтому ТВ **не** проверяет
содержимое system-разделов, но и писать в них без перемонтирования нельзя.

## Как включить ADB (для повторного включения после сброса)

Параметры разработчика на этой прошивке **залочены** (7 тапов по номеру сборки ничего не делают).
Рабочий путь — заводское меню CVTE:

1. Установить ярлык `FactoryMenu` (`com.stb.factorymenu`, 1.2, ~16 КБ) — он лишь открывает
   штатное заводское меню `com.cvte.fac.menu` через `TvMenuWindowManagerService`. APK есть на 4PDA
   в теме CV9632 («Сервисное меню / меню разработчика / активация ADB»); на ТВ ставится с флешки
   через файловый менеджер или из своего кабинета apps.sber.ru/my.
2. Открыть ярлык → **Debug** → **ADB Enable**. Порт 5555 открывается за секунды.
3. Больше в заводском меню **ничего не трогать**: там панель, картинка и тестовые инструменты.
   Не открывать другие `com.cvte.*` приложения (factory/test) — на CVTE это рискованно.

Альтернативный (официальный) путь Сбера — привязка ТВ в Studio developers.sber.ru (Settings →
Service settings → SmartApp → My devices) и включение ADB там; на проверенном экземпляре он
порт 5555 не открыл.

После включения ADB: `service.adb.tcp.port=5555`, `persist.sys.usb.config=adb`. Постоянным его
делает [adb-permanent.md](adb-permanent.md).

## Пакеты

Полный список снять так:

```bash
adb shell pm list packages > packages.txt
adb shell pm list packages -3 > packages-third-party.txt
adb shell getprop > props.txt
```

После чистки остались важные для работы ТВ (выборка):

```
ru.sberdevices.services            ru.sberdevices.services.core
ru.sberdevices.assistant           ru.sberdevices.tvinputs
ru.sberdevices.starlivetv          ru.sberdevices.startube
ru.sberdevices.rcfupdater          ru.sberdevices.gamepad.service
ru.sberdevices.app.starkeyboard    ru.sberdevices.webview
ru.sberdevices.settings            com.sberbank.settings
com.cvte.sber.onekey               com.cvte.tv.setting, com.cvte.tv.media, com.cvte.tv.api.impl
com.droidlogic.tvinput             com.android.tv.settings
com.google.android.youtube.tv      ru.kinopoisk.tv, ru.more.play, com.vk.tv, com.ivi...
limehd.ru.ctv (Цифровое ТВ)        com.gathin.tvhome (NA Clean Toour TV)
io.sberstop (Сбер-Стоп NA)
```

## Особенности, о которых стоит знать

* Прошивка Сбера **возвращает свой лаунчер и часть приложений** после перезагрузки — поэтому нужен
  сторож ([apps.md](apps.md#сторож-сбер-стоп-na)).
* **Автообновления** прошивки и приложений выключены, OTA-апдейтер `ru.sberdevices.updater`
  отключён ([apps.md](apps.md#автообновления-прошивки-и-приложений)).
* `ru.sberdevices.services` считает часть приложений «обязательными» и переустанавливает их; его
  самого отключить нельзя (`services.core` включает обратно). Поэтому удаляем только контент.
* **StarOS «ротация»**: при нехватке свободного места в `/data` (порог ~350 МБ) система может
  удалять sideload-приложения. Следите за `df -h /data`.
* `screencap` не захватывает видео-слой (только интерфейс) — это нормально для Amlogic.
* Сброс к заводским настройкам возвращает всё стоковое: лаунчер Сбера, приложения, запросы ADB.
