#!/usr/bin/env bash
set -euo pipefail

# Set up runner, device, and app prerequisites for qa-android-ui-tests workflow.

usage() {
  echo "Usage: $0 {ensure-required-tools|resolve-flavor|download-apks|detect-target-devices|install-apks-on-devices|fetch-runtime-secrets|build-test-apk|resolve-test-apk-path|resolve-test-services-apks}" >&2
  exit 2
}

ensure_required_tools() {
  command -v adb >/dev/null 2>&1 || { echo "ERROR: adb not found"; exit 1; }
  command -v python3 >/dev/null 2>&1 || { echo "ERROR: python3 not found on this runner"; exit 1; }

  if command -v aws >/dev/null 2>&1; then
    aws --version
    return
  fi

  command -v curl >/dev/null 2>&1 || { echo "ERROR: curl not found"; exit 1; }
  command -v unzip >/dev/null 2>&1 || { echo "ERROR: unzip not found"; exit 1; }
  : "${RUNNER_TEMP:?RUNNER_TEMP not set}"

  echo "aws CLI not found. Installing AWS CLI v2 locally..."
  local aws_root="${RUNNER_TEMP}/awscli"
  local zip_path="${RUNNER_TEMP}/awscliv2.zip"

  rm -rf "${aws_root}" "${zip_path}" "${RUNNER_TEMP}/aws"
  mkdir -p "${aws_root}"

  curl -fsSL -o "${zip_path}" "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip"
  unzip -oq "${zip_path}" -d "${RUNNER_TEMP}"
  rm -f "${zip_path}"

  "${RUNNER_TEMP}/aws/install" -i "${aws_root}" -b "${aws_root}/bin"
  echo "${aws_root}/bin" >> "${GITHUB_PATH}"
  export PATH="${aws_root}/bin:${PATH}"

  aws --version
}

resolve_flavor() {
  python3 scripts/qa_android_ui_tests/resolve_flavor.py
  echo "Resolved flavor from runner config: '${FLAVOR_INPUT:-}'"
}

download_apks() {
  : "${S3_BUCKET:?ERROR: Missing secret AWS_S3_BUCKET}"
  : "${S3_FOLDER:?ERROR: S3_FOLDER missing}"
  : "${RUNNER_TEMP:?RUNNER_TEMP not set}"
  : "${GITHUB_ENV:?GITHUB_ENV not set}"
  : "${GITHUB_OUTPUT:?GITHUB_OUTPUT not set}"

  aws s3api list-objects-v2 \
    --bucket "${S3_BUCKET}" \
    --prefix "${S3_FOLDER}" \
    --query "Contents[?ends_with(Key, '.apk')].Key" \
    --output json > "${RUNNER_TEMP}/apk_keys.json"

  local apk_env_file="${RUNNER_TEMP}/apk_env.txt"
  python3 scripts/qa_android_ui_tests/select_apks.py > "${apk_env_file}"

  local new_s3_key=""
  local old_s3_key=""
  while IFS= read -r line || [[ -n "${line}" ]]; do
    [[ -z "${line}" ]] && continue
    if [[ "${line}" != *=* ]]; then
      echo "ERROR: Invalid output line from select_apks.py: ${line}"
      exit 1
    fi

    local key="${line%%=*}"
    local value="${line#*=}"
    case "${key}" in
      NEW_S3_KEY|OLD_S3_KEY|NEW_APK_NAME|OLD_APK_NAME|REAL_BUILD_NUMBER|OLD_BUILD_NUMBER)
        printf '%s=%s\n' "${key}" "${value}" >> "$GITHUB_ENV"
        printf '%s=%s\n' "${key}" "${value}" >> "$GITHUB_OUTPUT"
        ;;
      *)
        echo "ERROR: Unexpected key from select_apks.py: ${key}"
        exit 1
        ;;
    esac

    case "${key}" in
      NEW_S3_KEY)
        new_s3_key="${value}"
        ;;
      OLD_S3_KEY)
        old_s3_key="${value}"
        ;;
    esac
  done < "${apk_env_file}"

  if [[ -z "${new_s3_key}" ]]; then
    echo "ERROR: Missing NEW_S3_KEY from select_apks.py output"
    exit 1
  fi

  local new_apk_path="${RUNNER_TEMP}/Wire.apk"
  echo "NEW_APK_PATH=${new_apk_path}" >> "$GITHUB_ENV"
  aws s3 cp "s3://${S3_BUCKET}/${new_s3_key}" "${new_apk_path}" --only-show-errors
  test -s "${new_apk_path}"

  if [[ "${IS_UPGRADE:-}" == "true" ]]; then
    if [[ -z "${old_s3_key}" ]]; then
      echo "ERROR: Missing OLD_S3_KEY for upgrade flow"
      exit 1
    fi
    local old_apk_path="${RUNNER_TEMP}/Wire.old.apk"
    echo "OLD_APK_PATH=${old_apk_path}" >> "$GITHUB_ENV"
    aws s3 cp "s3://${S3_BUCKET}/${old_s3_key}" "${old_apk_path}" --only-show-errors
    test -s "${old_apk_path}"
  fi
}

detect_target_devices() {
  : "${GITHUB_ENV:?GITHUB_ENV not set}"

  local device_lines
  device_lines="$(adb devices | awk 'NR>1 && $2=="device"{print $1}')"
  if [[ -z "${device_lines}" ]]; then
    echo "ERROR: No online Android devices found."
    exit 1
  fi

  local target="${TARGET_DEVICE_ID:-}"
  local device_list
  if [[ -n "${target}" ]]; then
    if ! printf '%s\n' "$device_lines" | grep -qx "$target"; then
      echo "ERROR: androidDeviceId '$target' not found in adb devices."
      exit 1
    fi
    device_list="$target"
  elif [[ -n "${RESOLVED_TESTCASE_ID:-}" ]]; then
    device_list="$(printf '%s\n' "$device_lines" | head -n 1)"
    echo "Single-testcase mode (${RESOLVED_TESTCASE_ID}): selected device ${device_list}"
  else
    device_list="$(printf '%s\n' "$device_lines" | xargs)"
  fi

  local device_count
  device_count="$(wc -w <<<"${device_list}" | tr -d ' ')"

  echo "DEVICE_LIST=${device_list}" >> "$GITHUB_ENV"
  echo "DEVICE_COUNT=${device_count}" >> "$GITHUB_ENV"
  echo "Using ${device_count} device(s)"
}

install_apks_on_devices() {
  : "${DEVICE_LIST:?DEVICE_LIST missing}"
  : "${APP_ID:?APP_ID missing}"
  : "${NEW_APK_PATH:?NEW_APK_PATH missing}"
  : "${GITHUB_ENV:?GITHUB_ENV not set}"

  local new_apk_device_path="/data/local/tmp/Wire.new.apk"
  local old_apk_device_path="/data/local/tmp/Wire.old.apk"
  echo "NEW_APK_DEVICE_PATH=${new_apk_device_path}" >> "$GITHUB_ENV"
  echo "OLD_APK_DEVICE_PATH=${old_apk_device_path}" >> "$GITHUB_ENV"

  local install_flags="-r"
  if [[ "${ENFORCE_APP_INSTALL:-}" == "true" ]]; then
    install_flags="-r -d"
  fi

  local packages_to_uninstall="${PACKAGES_TO_UNINSTALL:-}"
  read -ra PACKAGES <<< "${packages_to_uninstall}"

  read -ra DEVICES <<< "${DEVICE_LIST}"
  for serial in "${DEVICES[@]}"; do
    local adb_cmd="adb -s ${serial}"
    ${adb_cmd} wait-for-device

    local installed
    installed="$(${adb_cmd} shell pm list packages || true)"
    for pkg in "${PACKAGES[@]}"; do
      if [[ -n "${pkg}" ]] && echo "${installed}" | grep -qx "package:${pkg}"; then
        ${adb_cmd} uninstall "${pkg}" || true
      fi
    done

    if [[ "${IS_UPGRADE:-}" == "true" ]]; then
      : "${OLD_APK_PATH:?OLD_APK_PATH missing for upgrade}"
      ${adb_cmd} shell rm -f "${new_apk_device_path}" "${old_apk_device_path}" || true
      ${adb_cmd} push "${OLD_APK_PATH}" "${old_apk_device_path}" >/dev/null
      ${adb_cmd} push "${NEW_APK_PATH}" "${new_apk_device_path}" >/dev/null
      ${adb_cmd} install ${install_flags} "${OLD_APK_PATH}"
    else
      ${adb_cmd} install ${install_flags} "${NEW_APK_PATH}"
    fi

    if ! ${adb_cmd} shell pm list packages | grep -qx "package:${APP_ID}"; then
      echo "ERROR: '${APP_ID}' not installed on ${serial}."
      exit 1
    fi
  done
}

fetch_runtime_secrets() {
  if [[ -z "${OP_SERVICE_ACCOUNT_TOKEN:-}" ]]; then
    echo "ERROR: Missing OP_SERVICE_ACCOUNT_TOKEN secret"
    exit 1
  fi

  : "${RUNNER_TEMP:?RUNNER_TEMP not set}"
  : "${GITHUB_ENV:?GITHUB_ENV not set}"

  echo "::add-mask::${OP_SERVICE_ACCOUNT_TOKEN}"

  chmod +x ./gradlew

  local secrets_json_path="${RUNNER_TEMP}/secrets.json"
  export SECRETS_JSON_PATH="${secrets_json_path}"
  echo "SECRETS_JSON_PATH=${secrets_json_path}" >> "$GITHUB_ENV"

  python3 scripts/qa_android_ui_tests/fetch_secrets_json.py

  test -s "${secrets_json_path}"
  chmod 600 "${secrets_json_path}"

  rm -f "secrets.json" || true
  ln -s "${secrets_json_path}" "secrets.json"
  chmod 600 "secrets.json" || true

  mkdir -p .git/info
  grep -qxF "secrets.json" .git/info/exclude 2>/dev/null || echo "secrets.json" >> .git/info/exclude
}

build_test_apk() {
  ./gradlew :tests:testsCore:assembleDebugAndroidTest --no-daemon --no-configuration-cache
}

resolve_test_apk_path() {
  : "${GITHUB_ENV:?GITHUB_ENV not set}"

  local test_apk_path
  test_apk_path="$(ls -1 tests/testsCore/build/outputs/apk/androidTest/debug/*.apk | head -n 1 || true)"
  if [[ -z "${test_apk_path}" || ! -f "${test_apk_path}" ]]; then
    echo "ERROR: Could not find built androidTest APK under tests/testsCore/build/outputs/apk/androidTest/debug/"
    exit 1
  fi
  echo "TEST_APK_PATH=${test_apk_path}" >> "$GITHUB_ENV"
}

# Resolve newest cached artifacts so Test Services/Orchestrator can be installed without rebuilding.
resolve_test_services_apks() {
  : "${GITHUB_ENV:?GITHUB_ENV not set}"

  roots=()
  [[ -n "${GRADLE_USER_HOME:-}" && -d "${GRADLE_USER_HOME}" ]] && roots+=("${GRADLE_USER_HOME}")
  [[ -d "${HOME}/.gradle" ]] && roots+=("${HOME}/.gradle")

  if [[ ${#roots[@]} -eq 0 ]]; then
    echo "ERROR: Could not find any Gradle cache directory (no GRADLE_USER_HOME and no ~/.gradle)."
    exit 1
  fi

  find_newest() {
    local pattern="$1"
    shift
    local newest=""
    local candidates=()

    for r in "$@"; do
      while IFS= read -r -d '' f; do
        candidates+=("$f")
      done < <(find "$r" -type f -name "$pattern" -print0 2>/dev/null || true)
    done

    if [[ ${#candidates[@]} -gt 0 ]]; then
      newest="$(ls -t "${candidates[@]}" 2>/dev/null | head -n 1 || true)"
    fi

    echo "$newest"
  }

  local test_services_apk
  local orchestrator_apk
  test_services_apk="$(find_newest "*test-services*.apk" "${roots[@]}")"
  orchestrator_apk="$(find_newest "*orchestrator*.apk" "${roots[@]}")"

  if [[ -z "${test_services_apk}" || ! -f "${test_services_apk}" ]]; then
    echo "ERROR: Could not locate AndroidX Test Services APK in Gradle cache."
    echo "This APK is required for Allure TestStorage (content://androidx.test.services.storage...)."
    exit 1
  fi

  echo "TEST_SERVICES_APK_PATH=${test_services_apk}" >> "$GITHUB_ENV"
  if [[ -n "${orchestrator_apk}" && -f "${orchestrator_apk}" ]]; then
    echo "ORCHESTRATOR_APK_PATH=${orchestrator_apk}" >> "$GITHUB_ENV"
  fi
}

case "${1:-}" in
  ensure-required-tools)
    ensure_required_tools
    ;;
  resolve-flavor)
    resolve_flavor
    ;;
  download-apks)
    download_apks
    ;;
  detect-target-devices)
    detect_target_devices
    ;;
  install-apks-on-devices)
    install_apks_on_devices
    ;;
  fetch-runtime-secrets)
    fetch_runtime_secrets
    ;;
  build-test-apk)
    build_test_apk
    ;;
  resolve-test-apk-path)
    resolve_test_apk_path
    ;;
  resolve-test-services-apks)
    resolve_test_services_apks
    ;;
  *)
    usage
    ;;
esac
