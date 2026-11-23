#!/usr/bin/env bash
# root.sh — выполнить команду от root (uid 0) на SaluteTV AT30A6.
#
# Работает через вендорный сервис cvte.at_sudo и подделку имени процесса
# (см. docs/atsudo-root.md). Требуется, чтобы на ТВ уже лежал /data/local/tmp/atsudo.dex
# (см. scripts/install-root-tool.sh).
#
# Использование:  ./root.sh 'id'
#                 ./root.sh 'setprop persist.adb.tcp.port 5555'
#
# ВАЖНО: команду передавайте в ОДИНАРНЫХ кавычках и не используйте внутри одинарных кавычек.
set -euo pipefail
: "${TV_SERIAL:?Задайте TV_SERIAL, например 192.168.1.50:5555}"

if [ $# -lt 1 ]; then
  echo "usage: TV_SERIAL=<ip:port> $0 '<shell-command>'" >&2
  exit 2
fi

exec adb -s "$TV_SERIAL" shell "CLASSPATH=/data/local/tmp/atsudo.dex app_process /system/bin AtSudo \"$1\""
