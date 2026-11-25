#!/usr/bin/env bash
# Сборка «Сбер-Стоп NA» без Gradle: aapt2 -> javac -> d8 -> jar -> zipalign -> apksigner.
# Linux-версия sberstop/build-sberstop.ps1 (PowerShell).
#
# Требуется только то, что уже лежит в проекте:
#   JDK 17          /home/ulalex/Проекты/ТВ/rebuild/jdk
#   Android SDK     /home/ulalex/Проекты/ТВ/rebuild
#                   build-tools/34.0.0 (aapt2, d8, zipalign, apksigner), platforms/android-34/android.jar
#
# Можно переопределить путями: JDK=/path SDK=/path ./build-linux.sh
# Результат: ./SberStop-NA.apk (+ sha256 и aapt2 dump badging в конце).
set -euo pipefail

cd "$(dirname "$0")"

ROOT=${ROOT:-/home/ulalex/Проекты/ТВ}
JDK=${JDK:-$ROOT/rebuild/jdk}
SDK=${SDK:-$ROOT/rebuild}
BT=${BT:-$SDK/build-tools/34.0.0}
AJ=${AJ:-$SDK/platforms/android-34/android.jar}
KS=${KS:-sberstop.keystore}
KS_PASS=${KS_PASS:-sberstop}
KS_ALIAS=${KS_ALIAS:-sberstop}
OUT=${OUT:-out}
APK=${APK:-SberStop-NA.apk}

export JAVA_HOME="$JDK"
export PATH="$JDK/bin:$PATH"

need() { [ -e "$1" ] || { echo "НЕТ: $1" >&2; exit 1; }; }
need "$JDK/bin/javac"
need "$BT/aapt2"
need "$BT/d8"
need "$BT/zipalign"
need "$BT/apksigner"
need "$AJ"

rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes"

echo "== aapt2 compile/link =="
"$BT/aapt2" compile --dir res -o "$OUT/res.zip"
"$BT/aapt2" link -o "$OUT/base.apk" -I "$AJ" \
    --manifest AndroidManifest.xml \
    --java "$OUT/gen" "$OUT/res.zip" \
    --min-sdk-version 26 --target-sdk-version 33

echo "== javac =="
# -source/-target 8 + android.jar в bootclasspath — как в исходном build-sberstop.ps1.
# core-lambda-stubs.jar нужен для javac-лямбд; d8 с --min-api 26 оставляет invoke-custom.
mapfile -t SOURCES < <(find src "$OUT/gen" -name '*.java' | sort)
javac -nowarn -Xlint:-options -source 8 -target 8 \
    -bootclasspath "$AJ:$BT/core-lambda-stubs.jar" -classpath "$AJ" \
    -encoding UTF-8 -d "$OUT/classes" "${SOURCES[@]}"

echo "== d8 =="
mapfile -t CLASSES < <(find "$OUT/classes" -name '*.class' | sort)
"$BT/d8" --release --min-api 26 --lib "$AJ" --output "$OUT" "${CLASSES[@]}"

echo "== classes.dex -> APK =="
# -M: не создавать META-INF/MANIFEST.MF (как в PowerShell-сборке).
( cd "$OUT" && jar uMf base.apk classes.dex )

echo "== zipalign =="
"$BT/zipalign" -f -p 4 "$OUT/base.apk" "$OUT/aligned.apk"

echo "== keystore =="
if [ ! -f "$KS" ]; then
    # Отдельный ключ подписи, если исходный не положили рядом. Обновление поверх старой
    # установки возможно только с тем же ключом, поэтому по умолчанию используется
    # приложенный sberstop.keystore (пароль sberstop).
    keytool -genkeypair -keystore "$KS" -alias "$KS_ALIAS" -keyalg RSA -keysize 2048 \
        -validity 20000 -storepass "$KS_PASS" -keypass "$KS_PASS" \
        -dname "CN=Sber Stop NA" >/dev/null 2>&1
    echo "создан новый $KS"
fi

echo "== apksigner =="
rm -f "$APK" "$APK.idsig"
# v4 (.idsig) для adb-incremental не нужен: APK ставится обычным `adb install -r`.
"$BT/apksigner" sign --ks "$KS" --ks-key-alias "$KS_ALIAS" \
    --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
    --v4-signing-enabled false \
    --out "$APK" "$OUT/aligned.apk"
"$BT/apksigner" verify --print-certs "$APK" | sed -n '1,8p'

echo
echo "== результат =="
ls -l "$APK"
echo "sha256: $(sha256sum "$APK" | cut -d' ' -f1)"
echo
"$BT/aapt2" dump badging "$APK" | grep -E \
    "^(package:|sdkVersion:|targetSdkVersion:|uses-permission:|application-label:|launchable-activity:|leanback-launchable-activity:)"
