#!/usr/bin/env bash
# verify.sh — проверить итоговое состояние SaluteTV AT30A6.
# Ничего не меняет, только читает и печатает отчёт.
set -uo pipefail
: "${TV_SERIAL:?Задайте TV_SERIAL, например 192.168.1.50:5555}"
HERE="$(cd "$(dirname "$0")" && pwd)"
A() { adb -s "$TV_SERIAL" shell "$1" 2>/dev/null | tr -d '\r'; }

echo "== устройство =="
adb -s "$TV_SERIAL" shell 'getprop ro.product.model; getprop ro.build.version.release; getprop ro.vendor.build.staros_version' | tr -d '\r'

echo
echo "== ADB =="
echo "persist.adb.tcp.port = $(A 'getprop persist.adb.tcp.port')"
echo "service.adb.tcp.port = $(A 'getprop service.adb.tcp.port')"
echo "adb_enabled          = $(A 'settings get global adb_enabled')"
echo "ключей в adb_keys    = $(A 'wc -l < /data/misc/adb/adb_keys' || echo 'нет файла')"

echo
echo "== root =="
if [ -f "$HERE/root.sh" ]; then
  bash "$HERE/root.sh" "id" | tail -1
else
  echo "(root.sh рядом не найден)"
fi

echo
echo "== главный экран и заставка =="
A 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME' | tail -1
echo "заставка: $(A 'settings get secure screensaver_components')"

echo
echo "== ключевые пакеты =="
for p in com.gathin.tvhome limehd.ru.ctv io.sberstop ru.sberdevices.starlivetv ru.sberdevices.startube ru.sberdevices.tvinputs; do
  if A "pm list packages | grep -qx 'package:$p'"; then st="есть"; else st="НЕТ"; fi
  printf '  %-32s %s\n' "$p" "$st"
done

echo
echo "== мусор Сбера (должно быть всё «нет») =="
for p in ru.sberdevices.starlauncher ru.sberdevices.starscreensaver ru.sberdevices.starmarket \
         ru.sberdevices.app.setup ru.sberdevices.openbeta ru.sberdevices.starcleaner \
         ru.sberdevices.smarthome.promo ru.sberdevices.starbrowser ru.sberdevices.videos \
         ru.sberdevices.music ru.sberdevices.sberplay ru.sberdevices.starprofile \
         ru.sberdevices.subscriptions ru.sberdevices.parentcontrol ru.sberdevices.starkidsroom.prod \
         ru.sberdevices.bugreporter ru.sberdevices.survey com.salute.star com.sberbank.settings; do
  if A "pm list packages | grep -qx 'package:$p'"; then st="ЕСТЬ (!)"; else st="нет"; fi
  printf '  %-40s %s\n' "$p" "$st"
done

echo
echo "== сторож =="
echo "задания JobScheduler: $(A 'dumpsys jobscheduler 2>/dev/null | grep -c io.sberstop')"
echo "whitelist Doze:       $(A 'cmd deviceidle whitelist | grep -c io.sberstop')"
echo "свободно на /data:     $(A 'df -h /data | tail -1')"
