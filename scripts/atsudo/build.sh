#!/usr/bin/env bash
# build.sh — собрать classes.dex из AtSudo.java (без Gradle).
#
# Нужны:
#   JAVA_HOME      — JDK 17 (javac);
#   ANDROID_HOME   — Android SDK с build-tools;34.0.0 и platforms;android-34.
#
# Результат: scripts/atsudo/classes.dex (его и кладём на ТВ в /data/local/tmp/atsudo.dex).
set -euo pipefail
cd "$(dirname "$0")"

: "${JAVA_HOME:?Задайте JAVA_HOME (JDK 17)}"
: "${ANDROID_HOME:?Задайте ANDROID_HOME (Android SDK с build-tools;34.0.0)}"
JAVAC="$JAVA_HOME/bin/javac"
D8="$ANDROID_HOME/build-tools/34.0.0/d8"
ANDROID_JAR="$ANDROID_HOME/platforms/android-34/android.jar"
[ -x "$JAVAC" ] || { echo "нет $JAVAC" >&2; exit 1; }
[ -x "$D8" ] || { echo "нет $D8 (нужен build-tools;34.0.0)" >&2; exit 1; }
[ -f "$ANDROID_JAR" ] || { echo "нет $ANDROID_JAR (нужен platforms;android-34)" >&2; exit 1; }

"$JAVAC" -source 8 -target 8 -classpath "$ANDROID_JAR" -d . AtSudo.java
"$D8" --lib "$ANDROID_JAR" --output . AtSudo.class
rm -f AtSudo.class
ls -la classes.dex
