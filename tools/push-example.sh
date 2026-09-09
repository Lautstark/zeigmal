#!/bin/sh
# Copies the example Kartensatz onto the connected phone, into the directory
# the app reads: Android/data/de.lautstark.zeigmal/files/kartensatz/. No
# permission is needed for that directory, and a file manager over USB reaches
# the same place by hand. Run tools/make-example-media.sh first.
#
#     tools/push-example.sh
set -eu
cd "$(dirname "$0")/.."
adb="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
[ -x "$adb" ] || adb=adb
target=/sdcard/Android/data/de.lautstark.zeigmal/files/kartensatz
"$adb" shell mkdir -p "$target"
"$adb" push example/kartensatz/. "$target/"
"$adb" shell ls -R "$target"
