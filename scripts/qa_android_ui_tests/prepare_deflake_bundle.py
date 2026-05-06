#!/usr/bin/env python3
"""Write one standard manual-deflake artifact for Android UI workflow runs."""

from __future__ import annotations

import json
import os
from pathlib import Path


def env(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


def env_bool(name: str) -> bool:
    return env(name).lower() == "true"


def env_int(name: str) -> int:
    raw = env(name)
    if not raw:
        return 0
    try:
        return int(raw)
    except ValueError:
        return 0


def append_summary(lines: list[str]) -> None:
    # Write helper output into the GitHub Actions step summary when available.
    summary_path = env("GITHUB_STEP_SUMMARY")
    if not summary_path:
        return
    with open(summary_path, "a", encoding="utf-8") as summary_file:
        summary_file.write("\n".join(lines) + "\n")


def copy_test_list(src_value: str, dest: Path) -> None:
    src = Path(src_value) if src_value else None
    if src and src.is_file():
        text = src.read_text(encoding="utf-8")
        dest.write_text(text, encoding="utf-8")
        return
    dest.write_text("", encoding="utf-8")


bundle_dir = Path(os.environ["DEFLAKE_BUNDLE_DIR"])
bundle_dir.mkdir(parents=True, exist_ok=True)

selector_type = "all"
selector_value = ""
resolved_test_case_id = env("RESOLVED_TESTCASE_ID")
resolved_category = env("RESOLVED_CATEGORY")
if resolved_test_case_id:
    selector_type = "testCaseId"
    selector_value = resolved_test_case_id
elif resolved_category:
    selector_type = "category"
    selector_value = resolved_category

android_device_id = env("ANDROID_DEVICE_ID")
device_mode = "auto_pool"
if android_device_id:
    device_mode = "specific_device"
elif resolved_test_case_id:
    device_mode = "single_testcase_auto"

metadata = {
    "schema_version": 1,
    "manual_deflake_id": env("SOURCE_RUN_ID"),
    "source_workflow_name": env("SOURCE_WORKFLOW_NAME"),
    "source_workflow_file": env("SOURCE_WORKFLOW_FILE"),
    "source_repository": env("SOURCE_REPOSITORY"),
    "source_run_id": env("SOURCE_RUN_ID"),
    "source_run_number": env("SOURCE_RUN_NUMBER"),
    "source_run_attempt": env("SOURCE_RUN_ATTEMPT"),
    "source_ref": env("SOURCE_REF"),
    "source_ref_name": env("SOURCE_REF_NAME"),
    "source_sha": env("SOURCE_SHA"),
    "flavor": env("FLAVOR_INPUT"),
    "tags": env("TAGS_INPUT"),
    "selector_type": selector_type,
    "selector_value": selector_value,
    "resolved_test_case_id": resolved_test_case_id,
    "resolved_category": resolved_category,
    "app_build_number_input": env("APP_BUILD_NUMBER_INPUT"),
    "resolved_build_number": env("REAL_BUILD_NUMBER"),
    "new_apk_name": env("NEW_APK_NAME"),
    "is_upgrade": env_bool("IS_UPGRADE"),
    "old_build_number": env("OLD_BUILD_NUMBER"),
    "enforce_app_install": env_bool("ENFORCE_APP_INSTALL"),
    "testiny_run_name": env("TESTINY_RUN_NAME"),
    "android_device_id": android_device_id,
    "device_mode": device_mode,
    "device_count": env_int("DEVICE_COUNT"),
    "rerun_failed_enabled": env_bool("RERUN_FAILED_ENABLED"),
    "rerun_failed_count": env_int("RERUN_FAILED_COUNT"),
    "failed_first_attempt_count": env_int("FIRST_FAILED_TESTS_COUNT"),
    "failed_after_retries_count": env_int("FINAL_FAILED_TESTS_COUNT"),
    "passed_on_rerun_count": env_int("PASSED_ON_RERUN_COUNT"),
}

(bundle_dir / "metadata.json").write_text(
    json.dumps(metadata, indent=2, sort_keys=True) + "\n",
    encoding="utf-8",
)

copy_test_list(env("FINAL_FAILED_TESTS_FILE"), bundle_dir / "failed-tests.txt")
copy_test_list(env("FIRST_FAILED_TESTS_FILE"), bundle_dir / "failed-tests-first-attempt.txt")

# Show the GitHub Actions run id in the summary so it can be copied directly
# into a later manual deflake run.
append_summary(
    [
        "### Manual Deflake",
        f"- manual deflake id: {metadata['manual_deflake_id']}",
    ]
)
