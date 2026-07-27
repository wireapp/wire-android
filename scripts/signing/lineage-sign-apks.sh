#!/usr/bin/env bash

set -euo pipefail

require_var() {
    local name="$1"
    if [ -z "${!name:-}" ]; then
        echo "Missing required environment variable: $name" >&2
        exit 1
    fi
}

find_apksigner() {
    if command -v apksigner >/dev/null 2>&1; then
        command -v apksigner
        return
    fi

    local candidate
    for candidate in \
        "${ANDROID_HOME:-}/build-tools" \
        "${ANDROID_SDK_ROOT:-}/build-tools" \
        "${ANDROID_SDK:-}/build-tools" \
        "${HOME}/Library/Android/sdk/build-tools"; do
        if [ -d "$candidate" ]; then
            find "$candidate" -name apksigner -type f | sort -V | tail -n1
            return
        fi
    done

    echo "Could not locate apksigner" >&2
    exit 1
}

require_var "APK_DIR"
require_var "CURRENT_KEYSTORE_PATH"
require_var "CURRENT_KEY_ALIAS"
require_var "CURRENT_STORE_PASSWORD"
require_var "CURRENT_KEY_PASSWORD"
require_var "OLD_KEYSTORE_B64"
require_var "OLD_KEY_ALIAS"
require_var "OLD_KEY_PASSWORD"
require_var "OLD_STORE_PASSWORD"
require_var "APK_SIGNING_LINEAGE_B64"

if [ ! -d "$APK_DIR" ]; then
    echo "APK directory does not exist: $APK_DIR" >&2
    exit 1
fi

APKSIGNER_BIN="$(find_apksigner)"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

OLD_KEYSTORE_PATH="${TMP_DIR}/old-signer.keystore"
LINEAGE_PATH="${TMP_DIR}/signing.lineage"

printf '%s' "$OLD_KEYSTORE_B64" | base64 -d > "$OLD_KEYSTORE_PATH"
printf '%s' "$APK_SIGNING_LINEAGE_B64" | base64 -d > "$LINEAGE_PATH"

chmod 600 "$OLD_KEYSTORE_PATH" "$LINEAGE_PATH"

find "$APK_DIR" -maxdepth 1 -name '*.apk' -type f | sort | while read -r apk_path; do
    signed_apk="${TMP_DIR}/$(basename "$apk_path")"

    echo "Preparing lineage signing for: $apk_path"
    echo "Old signer alias: $OLD_KEY_ALIAS"
    echo "Current signer alias: $CURRENT_KEY_ALIAS"
    echo "Current keystore path: $CURRENT_KEYSTORE_PATH"

    "$APKSIGNER_BIN" sign \
        --out "$signed_apk" \
        --ks "$OLD_KEYSTORE_PATH" \
        --ks-key-alias "$OLD_KEY_ALIAS" \
        --ks-pass env:OLD_STORE_PASSWORD \
        --key-pass env:OLD_KEY_PASSWORD \
        --next-signer \
        --ks "$CURRENT_KEYSTORE_PATH" \
        --ks-key-alias "$CURRENT_KEY_ALIAS" \
        --ks-pass env:CURRENT_STORE_PASSWORD \
        --key-pass env:CURRENT_KEY_PASSWORD \
        --lineage "$LINEAGE_PATH" \
        --rotation-min-sdk-version 28 \
        "$apk_path"

    "$APKSIGNER_BIN" verify --verbose --print-certs "$signed_apk"
    "$APKSIGNER_BIN" lineage --in "$signed_apk" --print-certs --verbose

    mv "$signed_apk" "$apk_path"
done
