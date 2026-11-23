#!/usr/bin/env bash
# install-root-tool.sh — положить на ТВ инструмент root (atsudo.dex).
#
# Ничего не меняет в системе: просто копирует classes.dex в /data/local/tmp/.
# Дальше: scripts/root.sh '<команда>' выполняет команду от uid 0.
set -euo pipefail
: "${TV_SERIAL:?Задайте TV_SERIAL, например 192.168.1.50:5555}"
HERE="$(cd "$(dirname "$0")" && pwd)"
DEX="$HERE/atsudo/classes.dex"

if [ ! -f "$DEX" ]; then
  echo "[*] classes.dex нет — собираю (нужны JDK 17 и Android SDK 34)..." >&2
  "$HERE/atsudo/build.sh"
fi

echo "[*] устройство: $TV_SERIAL"
adb -s "$TV_SERIAL" push "$DEX" /data/local/tmp/atsudo.dex >/dev/null
adb -s "$TV_SERIAL" shell 'chmod 666 /data/local/tmp/atsudo.dex; ls -la /data/local/tmp/atsudo.dex'

echo "[*] проверка root:"
adb -s "$TV_SERIAL" shell 'CLASSPATH=/data/local/tmp/atsudo.dex app_process /system/bin AtSudo "id"' | tail -1
