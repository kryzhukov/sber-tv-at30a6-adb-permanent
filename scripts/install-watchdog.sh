#!/usr/bin/env bash
# install-watchdog.sh — установить «Сбер-Стоп NA» (io.sberstop) и заранее авторизовать его ADB-ключ.
#
# Сторож ходит по ADB на 127.0.0.1:5555, чтобы удалять вернувшиеся приложения Сбера и возвращать
# главный экран на NA. Чтобы он работал БЕЗ запроса «Разрешить отладку» (в т.ч. после каждой
# перезагрузки), ключ генерируется этим скриптом заранее:
#   * приватная часть кладётся в /data/data/io.sberstop/files/ (root, chown на uid приложения);
#   * публичная часть дописывается в /data/misc/adb/adb_keys.
# После установки нужна ОДНА перезагрузка ТВ: система перечитает adb_keys.
#
# Требует: adb, openssl, python3, root-инструмент на ТВ (scripts/install-root-tool.sh).
set -euo pipefail
: "${TV_SERIAL:?Задайте TV_SERIAL, например 192.168.1.50:5555}"
HERE="$(cd "$(dirname "$0")" && pwd)"
APK="${1:-$HERE/../apk/SberStop-NA-2.0.apk}"
PKG=io.sberstop
KEYDIR="$HERE/wd-key"

command -v openssl >/dev/null || { echo "нужен openssl" >&2; exit 1; }
command -v python3 >/dev/null || { echo "нужен python3" >&2; exit 1; }
[ -f "$APK" ] || { echo "нет APK: $APK" >&2; exit 1; }

echo "[*] устройство: $TV_SERIAL"
echo "[*] установка сторожа"
adb -s "$TV_SERIAL" install -r "$APK"

# Разрешаем задания сторожа в Doze: без этого JobScheduler может откладывать
# проверки, когда телевизор «спит», и вернувшиеся приложения Сбера живут дольше.
adb -s "$TV_SERIAL" shell 'cmd deviceidle whitelist +io.sberstop' >/dev/null
echo "[*] сторож добавлен в whitelist Doze"

APP_UID="$(adb -s "$TV_SERIAL" shell "dumpsys package $PKG | grep -m1 userId=" | tr -dc '0-9')"
[ -n "$APP_UID" ] || { echo "не смог определить uid пакета $PKG" >&2; exit 1; }
echo "[*] uid приложения: $APP_UID"

mkdir -p "$KEYDIR"
if [ ! -f "$KEYDIR/wdkey.pem" ]; then
  echo "[*] генерирую ключ сторожа (RSA-2048)"
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$KEYDIR/wdkey.pem" 2>/dev/null
fi
openssl pkcs8 -topk8 -nocrypt -in "$KEYDIR/wdkey.pem" -outform DER -out "$KEYDIR/adbkey"
openssl rsa -in "$KEYDIR/wdkey.pem" -pubout -outform DER -out "$KEYDIR/adbkey.pub" 2>/dev/null

MODULUS="$(openssl rsa -in "$KEYDIR/wdkey.pem" -noout -modulus | cut -d= -f2)"
ADB_LINE="$(python3 - "$MODULUS" <<'PY'
import base64
import sys

n = int(sys.argv[1], 16)
e = 65537
r32 = 1 << 32
n0inv = (r32 - pow(n % r32, -1, r32)) % r32
rr = pow(2, 4096, n)


def le(value, size):
    return value.to_bytes(size, "big")[::-1]


blob = (64).to_bytes(4, "little") + n0inv.to_bytes(4, "little") + le(n, 256) + le(rr, 256) + e.to_bytes(4, "little")
assert len(blob) == 524
print(base64.b64encode(blob).decode() + " sberstop-na@tv")
PY
)"
[ -n "$ADB_LINE" ] || { echo "не смог собрать ADB-ключ" >&2; exit 1; }

PRIV_B64="$(base64 -w0 "$KEYDIR/adbkey" 2>/dev/null || base64 "$KEYDIR/adbkey" | tr -d '\n')"
PUB_B64="$(base64 -w0 "$KEYDIR/adbkey.pub" 2>/dev/null || base64 "$KEYDIR/adbkey.pub" | tr -d '\n')"

read -r -d '' CMD <<EOF || true
mkdir -p /data/data/$PKG/files
echo '$PRIV_B64' | base64 -d > /data/data/$PKG/files/adbkey
echo '$PUB_B64' | base64 -d > /data/data/$PKG/files/adbkey.pub
chown $APP_UID:$APP_UID /data/data/$PKG/files /data/data/$PKG/files/adbkey /data/data/$PKG/files/adbkey.pub
chmod 700 /data/data/$PKG/files
chmod 600 /data/data/$PKG/files/adbkey /data/data/$PKG/files/adbkey.pub
grep -qF '$ADB_LINE' /data/misc/adb/adb_keys 2>/dev/null || printf '%s\n' '$ADB_LINE' >> /data/misc/adb/adb_keys
chown system:shell /data/misc/adb/adb_keys
chmod 640 /data/misc/adb/adb_keys
echo "--- ключей в adb_keys:"
wc -l < /data/misc/adb/adb_keys
EOF

echo "[*] кладу ключ сторожа и авторизую его в adb_keys"
adb -s "$TV_SERIAL" shell "CLASSPATH=/data/local/tmp/atsudo.dex app_process /system/bin AtSudo \"$CMD\""

echo
echo "[*] Готово. Осталось:"
echo "    1) перезагрузить ТВ:  adb -s $TV_SERIAL reboot"
echo "    2) один раз открыть сторож и нажать «Проверить сейчас»:"
echo "       adb -s $TV_SERIAL shell am start -W -n $PKG/.MainActivity"
echo "    Дальше сторож сам: после каждой загрузки (через ~45 с) и раз в ~2 минуты."
