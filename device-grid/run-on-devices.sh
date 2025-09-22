#
# Wire
# Copyright (C) 2025 Wire Swiss GmbH
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program. If not, see http://www.gnu.org/licenses/.
#

#!/bin/bash

#!/usr/bin/env bash
set -euo pipefail

# TO RUN TESTS: "./run-on-devices.sh"  from device-grid directory

ADB="$HOME/Library/Android/sdk/platform-tools/adb"
REPO_ROOT="${REPO_ROOT:-/Users/antjerockstroh/Desktop/Android/wire-android}"
APK_PATH="${APK_PATH:-/Users/antjerockstroh/Desktop/com.wire.android-v4.15.1-32195-dev-debug.apk}"
PACKAGE_NAME="${PACKAGE_NAME:-com.waz.zclient.dev.debug}"

: "${ADB:="adb"}"
AAPT="$(command -v aapt || true)"

GRADLEW="${GRADLEW:-$REPO_ROOT/gradlew}"
GRADLE_TASK="${GRADLE_TASK:-:tests:testsCore:connectedDebugAndroidTest}"

die() { echo "ERROR: $*" >&2; exit 1; }
log() { echo "[$(date +%H:%M:%S)] $*"; }

[[ -f "$GRADLEW" ]] || die "gradlew not found at $GRADLEW (set REPO_ROOT correctly)"
chmod +x "$GRADLEW"
[[ -f "$APK_PATH" ]] || die "APK not found: $APK_PATH"

# Extract versionCode from the APK (best-effort)
get_apk_versionCode() {
  local apk="$1"
  if [[ -n "$AAPT" ]]; then
    # aapt dump badging outputs: versionCode='123' versionName='x.y.z'
    "$AAPT" dump badging "$apk" 2>/dev/null \
      | awk -F"'" '/versionCode=/{print $2; exit}'
  else
    # Fallback: unknown -> forces install path below to "always install"
    echo ""
  fi
}

# Check installed versionCode on device (empty string if not installed)
get_device_versionCode() {
  local serial="$1" pkg="$2"
  # dumpsys package <pkg> includes lines like: versionCode=123 minSdk=...
  $ADB -s "$serial" shell dumpsys package "$pkg" 2>/dev/null \
    | awk -F'=' '/versionCode=/{print $2; exit}' \
    | awk '{print $1}'
}

# Install APK if not installed or version differs
ensure_app_installed() {
  local serial="$1" apk="$2" pkg="$3"

  local apk_vc device_vc
  apk_vc="$(get_apk_versionCode "$apk")"
  device_vc="$(get_device_versionCode "$serial" "$pkg" || true)"

  if [[ -n "$device_vc" ]]; then
    if [[ -n "$apk_vc" && "$apk_vc" = "$device_vc" ]]; then
      log "[$serial] $pkg versionCode=$device_vc already installed — skipping install"
      return 0
    else
      log "[$serial] $pkg installed with versionCode=${device_vc:-UNKNOWN} (APK=$apk_vc) — reinstalling"
    fi
  else
    log "[$serial] $pkg not installed — installing"
  fi

  # Install with common flags:
  $ADB -s "$serial" install -r -g "$apk" >/dev/null
  log "[$serial] Install complete"
}

# Wake up and unlock device if the screen is off/locked
wake_device() {
  local serial="$1"

  # Wake up (224 = KEYCODE_WAKEUP)
  "$ADB" -s "$serial" shell input keyevent 224

  # Dismiss lock screen if present (82 = KEYCODE_MENU)
  "$ADB" -s "$serial" shell input keyevent 82
}

# Run Gradle tests for a single device
run_tests_for_device() {
  local serial="$1"

  log "[$serial] Waking up device"
  wake_device "$serial"

  log "[$serial] Starting tests: $GRADLE_TASK"
  ( cd "$REPO_ROOT" && ANDROID_SERIAL="$serial" "$GRADLEW" "$GRADLE_TASK" ) \
    && log "[$serial] Tests FINISHED OK" \
    || { log "[$serial] Tests FAILED"; return 1; }
}

# Collect connected devices (status == device)
get_connected_devices() {
  $ADB devices -l | awk '$2=="device"{print $1}'
}

########################################
# MAIN
########################################

log "Discovering connected devices…"

DEVICES=()
while IFS= read -r d; do
  [[ -n "$d" ]] && DEVICES+=("$d")
done <<< "$("$ADB" devices -l | awk '$2=="device"{print $1}')"

if [[ ${#DEVICES[@]} -eq 0 ]]; then
  die "No devices found. Plug in devices or start emulators."
fi

log "Found ${#DEVICES[@]} device(s): ${DEVICES[*]}"

# Verify adb can talk to each device
for d in "${DEVICES[@]}"; do
  $ADB -s "$d" get-state >/dev/null
done

# Install (or skip) per device
for d in "${DEVICES[@]}"; do
  ensure_app_installed "$d" "$APK_PATH" "$PACKAGE_NAME"
done

# Run tests in parallel on all devices
log "Running tests in parallel on ${#DEVICES[@]} device(s)…"
pids=()
rcs=()

for d in "${DEVICES[@]}"; do
  run_tests_for_device "$d" &
  pids+=($!)
done

# Wait for all and collate exit codes
i=0
for pid in "${pids[@]}"; do
  if wait "$pid"; then
    rcs[i]=0
  else
    rcs[i]=1
  fi
  ((i++))
done

# Exit non-zero if any test failed
sum=0
for r in "${rcs[@]}"; do ((sum+=r)); done

if [[ $sum -eq 0 ]]; then
  log "All device test runs passed ✅"
else
  log "One or more device test runs failed ❌"
  exit 1
fi
