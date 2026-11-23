#!/usr/bin/env bash
# debloat.sh — удалить приложения Сбера «для пользователя 0» (обратимо).
#
# Читает scripts/debloat.txt (один пакет в строке, # — комментарий).
# Восстановить любой пакет: adb shell cmd package install-existing --user 0 <pkg>
#
# Использование:
#   ./debloat.sh            # удалить
#   ./debloat.sh --dry-run  # только показать, что будет удалено
set -euo pipefail
: "${TV_SERIAL:?Задайте TV_SERIAL, например 192.168.1.50:5555}"
HERE="$(cd "$(dirname "$0")" && pwd)"
LIST="$HERE/debloat.txt"
DRY=0
[ "${1:-}" = "--dry-run" ] && DRY=1

echo "[*] устройство: $TV_SERIAL"
installed="$(adb -s "$TV_SERIAL" shell pm list packages | tr -d '\r' | sed 's/^package://')"

while IFS= read -r line || [ -n "$line" ]; do
  pkg="$(printf '%s' "$line" | sed 's/#.*//' | tr -d ' \t\r')"
  [ -z "$pkg" ] && continue
  case " $installed " in
    *" $pkg "*) ;;
    *) printf '%-40s уже отсутствует\n' "$pkg"; continue ;;
  esac
  if [ "$DRY" = 1 ]; then
    printf '%-40s БУДЕТ УДАЛЁН\n' "$pkg"
    continue
  fi
  out="$(adb -s "$TV_SERIAL" shell "pm uninstall --user 0 $pkg" 2>&1 | tr -d '\r')"
  printf '%-40s %s\n' "$pkg" "$out"
done < "$LIST"

[ "$DRY" = 1 ] || echo "[*] готово. Вернуть пакет: adb -s $TV_SERIAL shell cmd package install-existing --user 0 <pkg>"
