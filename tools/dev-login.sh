#!/bin/sh
# Hands the SIGNdigital login to a DEBUG build on the connected phone, so a
# developer does not type it after every reinstall. Reads the same variables
# the local scripts use; nothing is stored on the laptop by this script.
#
#     export SIGNDIGITAL_EMAIL='…' SIGNDIGITAL_PASSWORD='…'
#     tools/dev-login.sh
#
# Release builds ignore the extras. The password travels once through adb on
# your own cable and lands in the app's private preferences on the phone.
set -eu
: "${SIGNDIGITAL_EMAIL:?set SIGNDIGITAL_EMAIL}"
: "${SIGNDIGITAL_PASSWORD:?set SIGNDIGITAL_PASSWORD}"
adb="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
[ -x "$adb" ] || adb=adb
"$adb" shell am start -n de.lautstark.zeigmal/.MainActivity \
  --es de.lautstark.zeigmal.dev.email "$SIGNDIGITAL_EMAIL" \
  --es de.lautstark.zeigmal.dev.password "$SIGNDIGITAL_PASSWORD" >/dev/null
echo "login handed to the phone"
