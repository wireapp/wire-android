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
require_var "KEYSTORE_FILE_PATH_COMPAT_RELEASE"
require_var "KEYSTORE_KEY_NAME_COMPAT_RELEASE"
require_var "KEYSTOREPWD_COMPAT_RELEASE"
require_var "KEYPWD_COMPAT_RELEASE"
require_var "ENCODED_KEYSTORE_PUBLIC_RELEASE_OLD"
require_var "SIGNING_KEY_ALIAS_PUBLIC_RELEASE_OLD"
require_var "SIGNING_KEY_PASSWORD_PUBLIC_RELEASE_OLD"
require_var "SIGNING_STORE_PASSWORD_PUBLIC_RELEASE_OLD"
require_var "APK_SIGNING_LINEAGE_PUBLIC_RELEASE_B64"

if [ ! -d "$APK_DIR" ]; then
    echo "APK directory does not exist: $APK_DIR" >&2
    exit 1
fi

APKSIGNER_BIN="$(find_apksigner)"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

OLD_KEYSTORE_PATH="${TMP_DIR}/old-public-release.keystore"
LINEAGE_PATH="${TMP_DIR}/public-release.lineage"

printf '%s' "$ENCODED_KEYSTORE_PUBLIC_RELEASE_OLD" | base64 -d > "$OLD_KEYSTORE_PATH"
printf '%s' "$APK_SIGNING_LINEAGE_PUBLIC_RELEASE_B64" | base64 -d > "$LINEAGE_PATH"

chmod 600 "$OLD_KEYSTORE_PATH" "$LINEAGE_PATH"

find "$APK_DIR" -maxdepth 1 -name '*.apk' -type f | sort | while read -r apk_path; do
    signed_apk="${TMP_DIR}/$(basename "$apk_path")"

    "$APKSIGNER_BIN" sign \
        --out "$signed_apk" \
        --ks "$OLD_KEYSTORE_PATH" \
        --ks-key-alias "$SIGNING_KEY_ALIAS_PUBLIC_RELEASE_OLD" \
        --ks-pass env:SIGNING_STORE_PASSWORD_PUBLIC_RELEASE_OLD \
        --key-pass env:SIGNING_KEY_PASSWORD_PUBLIC_RELEASE_OLD \
        --next-signer \
        --ks "$KEYSTORE_FILE_PATH_COMPAT_RELEASE" \
        --ks-key-alias "$KEYSTORE_KEY_NAME_COMPAT_RELEASE" \
        --ks-pass env:KEYSTOREPWD_COMPAT_RELEASE \
        --key-pass env:KEYPWD_COMPAT_RELEASE \
        --lineage "$LINEAGE_PATH" \
        --rotation-min-sdk-version 28 \
        "$apk_path"

    "$APKSIGNER_BIN" verify --verbose --print-certs "$signed_apk"
    "$APKSIGNER_BIN" lineage --in "$signed_apk" --print-certs --verbose

    mv "$signed_apk" "$apk_path"
done
