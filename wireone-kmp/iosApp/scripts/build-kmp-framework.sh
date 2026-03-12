#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
while [[ ! -x "${ROOT_DIR}/gradlew" && "${ROOT_DIR}" != "/" ]]; do
  ROOT_DIR="$(dirname "${ROOT_DIR}")"
done

if [[ ! -x "${ROOT_DIR}/gradlew" ]]; then
  echo "gradlew not found; expected at repo root" >&2
  exit 1
fi

cd "${ROOT_DIR}"

CONFIGURATION="${CONFIGURATION:-Debug}"
SDK_NAME="${SDK_NAME:-iphonesimulator}"

if [[ -z "${JAVA_HOME:-}" ]]; then
  if JAVA_HOME_CANDIDATE="$(/usr/libexec/java_home -v 17 2>/dev/null)"; then
    export JAVA_HOME="${JAVA_HOME_CANDIDATE}"
    export PATH="${JAVA_HOME}/bin:${PATH}"
  fi
fi

if [[ "${SDK_NAME}" == *"iphoneos"* ]]; then
  ./gradlew ":wireone-kmp:link${CONFIGURATION}FrameworkIosArm64"
else
  ./gradlew ":wireone-kmp:link${CONFIGURATION}FrameworkIosSimulatorArm64"
fi
