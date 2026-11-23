#!/usr/bin/env bash
# install-adb-keys.sh — прописать доверенные ADB-ключи на ТВ и включить постоянный TCP ADB.
#
# По умолчанию берёт ключ ЭТОГО компьютера (~/.android/adbkey.pub) и ПЕРЕЗАПИСЫВАЕТ
# /data/misc/adb/adb_keys — в файле остаются ровно переданные ключи. Чтобы разрешить
# несколько компьютеров, передайте несколько .pub-файлов.
#
# Требует: root-инструмент на ТВ (scripts/install-root-tool.sh уже выполнен).
#
# Использование:
#   ./install-adb-keys.sh                                  # ключ этого ПК
#   ./install-adb-keys.sh ~/.android/adbkey.pub other.pub  # несколько ПК
set -euo pipefail
: "${TV_SERIAL:?Задайте TV_SERIAL, например 192.168.1.50:5555}"
HERE="$(cd "$(dirname "$0")" && pwd)"

if [ $# -eq 0 ]; then
  set -- "$HOME/.android/adbkey.pub"
fi

TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT
for key in "$@"; do
  [ -f "$key" ] || { echo "нет файла ключа: $key" >&2; exit 1; }
  cat "$key" >> "$TMP"
  echo >> "$TMP"
done

B64="$(base64 -w0 "$TMP" 2>/dev/null || base64 "$TMP" | tr -d '\n')"
read -r -d '' CMD <<EOF || true
mkdir -p /data/misc/adb
echo '$B64' | base64 -d > /data/misc/adb/adb_keys
chown system:shell /data/misc/adb/adb_keys
chmod 640 /data/misc/adb/adb_keys
setprop persist.adb.tcp.port 5555
setprop persist.vendor.staros.adb 1
settings put global adb_enabled 1
echo "--- ключей в adb_keys:"
wc -l < /data/misc/adb/adb_keys
EOF

echo "[*] устройство: $TV_SERIAL"
# shellcheck disable=SC2086
adb -s "$TV_SERIAL" shell "CLASSPATH=/data/local/tmp/atsudo.dex app_process /system/bin AtSudo \"$CMD\""

echo
echo "[*] Готово. Перезагрузите ТВ (adb -s $TV_SERIAL reboot), чтобы adbd и система перечитали ключи."
echo "    После перезагрузки: adb connect $TV_SERIAL → сразу 'device', без запроса."
