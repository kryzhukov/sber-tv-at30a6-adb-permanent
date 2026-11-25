#!/usr/bin/env bash
# install-ctv-cleaner.sh — поставить «Цифровое ТВ» (наш mod) и сторож-очистку на ТВ БЕЗ Сбера
# (YAOS/Hartens, любой Android TV), где сторонние службы доступности прошивка не включает.
#
# Что делает:
#   1) ставит APK «Цифрового ТВ» (переданный аргументом, пакет limehd.ru.ctv);
#   2) ставит сторожа apk/SberStop-NA-2.3.apk (универсальный режим: Sber-логика пропускается);
#   3) конвертирует ADB-ключ ЭТОГО ПК (~/.android/adbkey) в DER и кладёт его в external-каталог
#      сторожа — ключ уже доверен на ТВ, поэтому сторож работает по ADB молча, без запроса;
#   4) добавляет сторожа в whitelist Doze (без этого автозапуск после перезагрузки не работает);
#   5) запускает CleanerService (без UI) и проверяет, что он живой.
#
# Использование:
#   TV_SERIAL=192.168.1.113:5555 ./install-ctv-cleaner.sh /path/to/Цифровое-ТВ.apk
set -euo pipefail
: "${TV_SERIAL:?Задайте TV_SERIAL, например 192.168.1.50:5555}"
: "${1:?Укажите путь к APK «Цифрового ТВ» (пакет limehd.ru.ctv)}"
APK_CTV="$1"
HERE="$(cd "$(dirname "$0")" && pwd)"
APK_WD="$HERE/../apk/SberStop-NA-2.3.apk"

[ -f "$APK_CTV" ] || { echo "нет APK: $APK_CTV" >&2; exit 1; }
[ -f "$APK_WD" ] || { echo "нет APK сторожа: $APK_WD" >&2; exit 1; }
[ -f "$HOME/.android/adbkey" ] || { echo "нет ~/.android/adbkey — сначала подключитесь к ТВ по adb" >&2; exit 1; }

TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
openssl pkcs8 -topk8 -nocrypt -in "$HOME/.android/adbkey" -outform DER -out "$TMP/adbkey"
openssl rsa -in "$HOME/.android/adbkey" -pubout -outform DER -out "$TMP/adbkey.pub" 2>/dev/null

echo "[*] устройство: $TV_SERIAL"
adb -s "$TV_SERIAL" shell 'getprop ro.product.model; getprop ro.build.version.release'

echo "[*] ставлю «Цифровое ТВ»"
if adb -s "$TV_SERIAL" shell 'pm list packages' 2>/dev/null | grep -q 'package:limehd.ru.ctv'; then
  adb -s "$TV_SERIAL" install -r "$APK_CTV" || {
    echo "[*] подпись не совпала — переустанавливаю ЦТВ (данные старой версии будут удалены)"
    adb -s "$TV_SERIAL" uninstall limehd.ru.ctv >/dev/null 2>&1
    adb -s "$TV_SERIAL" install "$APK_CTV"
  }
else
  adb -s "$TV_SERIAL" install "$APK_CTV"
fi

echo "[*] ставлю сторожа «Сбер-Стоп NA» 2.3-NA"
adb -s "$TV_SERIAL" install -r "$APK_WD"

echo "[*] кладу внешний ADB-ключ (ключ этого ПК, он уже доверен на ТВ)"
adb -s "$TV_SERIAL" shell 'mkdir -p /sdcard/Android/data/io.sberstop/files'
adb -s "$TV_SERIAL" push "$TMP/adbkey" /sdcard/Android/data/io.sberstop/files/adbkey >/dev/null
adb -s "$TV_SERIAL" push "$TMP/adbkey.pub" /sdcard/Android/data/io.sberstop/files/adbkey.pub >/dev/null

echo "[*] whitelist Doze (нужен для автозапуска после перезагрузки)"
adb -s "$TV_SERIAL" shell 'cmd deviceidle whitelist +io.sberstop' >/dev/null

echo "[*] запускаю фоновую очистку ЦТВ (CleanerService, без UI)"
for _ in $(seq 1 30); do
  adb -s "$TV_SERIAL" shell 'am start-foreground-service -n io.sberstop/.CleanerService' >/dev/null 2>&1 || true
  sleep 5
  if adb -s "$TV_SERIAL" shell 'pidof io.sberstop' 2>/dev/null | grep -q .; then
    echo "[*] CleanerService работает"
    break
  fi
done

echo
echo "[*] готово. Проверка:"
adb -s "$TV_SERIAL" shell 'dumpsys package limehd.ru.ctv 2>/dev/null | grep -m1 versionName; dumpsys package io.sberstop 2>/dev/null | grep -m1 versionName; dumpsys activity services io.sberstop 2>/dev/null | grep -c CleanerService'
echo "    Сторож очистит кеш и данные ЦТВ после закрытия/выключения экрана."
echo "    После перезагрузки он поднимется сам (BootReceiver + whitelist Doze)."
