#!/usr/bin/env bash
# install-launcher.sh — установить NA Clean Toour TV (com.gathin.tvhome) и сделать его Home.
#
# Что делает:
#   1. ставит apk/NA-Clean-Toour-TV-3.5.apk (собран из kryzhukov/natx5cleanyourtv);
#   2. запускает его и проверяет, что активити реально поднялась;
#   3. назначает главным экраном (cmd package set-home-activity) и проверяет;
#   4. кладёт конфиг (док/скрытые/подписи) в external files и ставит заставку-часы;
#   5. выключает автообновления прошивки и приложений.
#
# ВАЖНО: лаунчер Сбера (ru.sberdevices.starlauncher) удаляется ПОТОМ — scripts/debloat.sh
# или вручную, только после того как Home уже переключён на NA.
set -euo pipefail
: "${TV_SERIAL:?Задайте TV_SERIAL, например 192.168.1.50:5555}"
HERE="$(cd "$(dirname "$0")" && pwd)"
APK="${1:-$HERE/../apk/NA-Clean-Toour-TV-3.5.apk}"
LAUNCHER=com.gathin.tvhome
ACTIVITY=$LAUNCHER/.MainActivity

[ -f "$APK" ] || { echo "нет APK: $APK" >&2; exit 1; }
echo "[*] устройство: $TV_SERIAL"

echo "[*] установка $APK"
adb -s "$TV_SERIAL" install -r "$APK"

echo "[*] запуск лаунчера"
adb -s "$TV_SERIAL" shell "pm grant $LAUNCHER android.permission.ACCESS_FINE_LOCATION 2>/dev/null || true"
adb -s "$TV_SERIAL" shell "am force-stop $LAUNCHER"
START_OUT="$(adb -s "$TV_SERIAL" shell "am start -W -a android.intent.action.MAIN -c android.intent.category.HOME -n $ACTIVITY" | tr -d '\r')"
echo "$START_OUT" | grep -E 'Status|Error' || true
echo "$START_OUT" | grep -q 'Status: ok' || { echo "ОШИБКА: лаунчер не запустился; Home не меняю." >&2; exit 1; }

echo "[*] назначаю Home"
adb -s "$TV_SERIAL" shell "cmd package set-home-activity --user 0 $ACTIVITY"
HOME_NOW="$(adb -s "$TV_SERIAL" shell 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME' | tr -d '\r' | tail -1)"
echo "    Home = $HOME_NOW"
case "$HOME_NOW" in
  *"$LAUNCHER"*) ;;
  *) echo "ОШИБКА: Home не переключился на $LAUNCHER; лаунчер Сбера пока не удаляйте." >&2; exit 1 ;;
esac

echo "[*] конфиг (док, скрытые приложения, подписи)"
adb -s "$TV_SERIAL" shell "mkdir -p /sdcard/Android/data/$LAUNCHER/files"
adb -s "$TV_SERIAL" push "$HERE/launcher-config.json" "/sdcard/Android/data/$LAUNCHER/files/config.json" >/dev/null
adb -s "$TV_SERIAL" shell "am force-stop $LAUNCHER; am start -W -a android.intent.action.MAIN -c android.intent.category.HOME -n $ACTIVITY >/dev/null 2>&1 || true"

echo "[*] заставка и автообновления"
adb -s "$TV_SERIAL" shell "settings put secure screensaver_components $LAUNCHER/.ClockDream"
adb -s "$TV_SERIAL" shell "settings put global ota_disable_automatic_update 1"
adb -s "$TV_SERIAL" shell "settings put global auto_update_apps 0"

echo
echo "[*] Готово. Home → $LAUNCHER, заставка → $LAUNCHER/.ClockDream."
echo "    Теперь можно удалять лаунчер Сбера: scripts/debloat.sh (или вручную)."
