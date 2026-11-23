#!/usr/bin/env bash
# block-ota.sh — жёстко выключить автообновления прошивки и приложений Сбера на AT30A6.
#
# Что делает:
#   1. выключает автообновление прошивки (settings global ota_disable_automatic_update=1);
#   2. выключает автообновление приложений  (settings global auto_update_apps=0);
#   3. отключает сам OTA-апдейтер Сбера (`ru.sberdevices.updater`, /system/priv-app/SberDevicesUpdater)
#      для пользователя 0 — на проверенном ТВ это не ломает пульт/ассистента/каналы.
#
# Проверено: настройки и отключённый апдейтер переживают перезагрузку ТВ.
#
# Вернуть автообновления:
#   adb -s "$TV_SERIAL" shell 'pm enable --user 0 ru.sberdevices.updater'
#   adb -s "$TV_SERIAL" shell 'settings put global ota_disable_automatic_update 0'
#   adb -s "$TV_SERIAL" shell 'settings put global auto_update_apps 1'
set -euo pipefail
: "${TV_SERIAL:?Задайте TV_SERIAL, например 192.168.1.50:5555}"

echo "[*] устройство: $TV_SERIAL"
adb -s "$TV_SERIAL" shell 'settings put global ota_disable_automatic_update 1'
adb -s "$TV_SERIAL" shell 'settings put global auto_update_apps 0'
adb -s "$TV_SERIAL" shell 'pm disable-user --user 0 ru.sberdevices.updater'

echo "[*] проверка:"
echo "    ota_disable_automatic_update = $(adb -s "$TV_SERIAL" shell 'settings get global ota_disable_automatic_update' | tr -d '\r')"
echo "    auto_update_apps             = $(adb -s "$TV_SERIAL" shell 'settings get global auto_update_apps' | tr -d '\r')"
adb -s "$TV_SERIAL" shell 'dumpsys package ru.sberdevices.updater 2>/dev/null | grep -m1 "User 0"' | sed 's/^/    /'

echo
echo "[*] Готово. Включить обратно: см. шапку этого скрипта (pm enable + settings put ... 0/1)."
