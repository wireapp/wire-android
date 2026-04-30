#!/usr/bin/env python3
"""Validate and summarize one QA Android UI deflake input artifact."""

from __future__ import annotations

import json
import os
from pathlib import Path


def env(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


def write_output(name: str, value: str) -> None:
    output_path = env("GITHUB_OUTPUT")
    if not output_path:
        return
    with open(output_path, "a", encoding="utf-8") as output_file:
        output_file.write(f"{name}={value}\n")


def append_summary(lines: list[str]) -> None:
    summary_path = env("GITHUB_STEP_SUMMARY")
    if not summary_path:
        return
    with open(summary_path, "a", encoding="utf-8") as summary_file:
        summary_file.write("\n".join(lines) + "\n")


def resolve_bundle_dir(root: Path) -> Path:
    # The download action may create one extra wrapper directory, so locate the
    # bundle by its required metadata file instead of assuming one fixed layout.
    matches = sorted(root.rglob("metadata.json"))
    if not matches:
        raise SystemExit(f"ERROR: No metadata.json found under {root}")
    if len(matches) > 1:
        raise SystemExit(f"ERROR: Multiple metadata.json files found under {root}")
    return matches[0].parent


bundle_root = Path(os.environ["DEFLAKE_BUNDLE_ROOT"])
bundle_dir = resolve_bundle_dir(bundle_root)
metadata_path = bundle_dir / "metadata.json"
failed_tests_path = bundle_dir / "failed-tests.txt"
first_attempt_failed_tests_path = bundle_dir / "failed-tests-first-attempt.txt"

if not failed_tests_path.is_file():
    raise SystemExit(f"ERROR: Missing failed-tests.txt in {bundle_dir}")

metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
if metadata.get("schema_version") != 1:
    raise SystemExit("ERROR: Unsupported deflake metadata schema version.")

# Fail early if the selected run did not export the minimum context needed to
# rerun its leftover failed tests deterministically.
required_fields = [
    "source_workflow_name",
    "source_workflow_file",
    "source_run_id",
    "source_ref_name",
    "flavor",
]
for field in required_fields:
    value = str(metadata.get(field, "")).strip()
    if not value:
        raise SystemExit(f"ERROR: Missing required metadata field '{field}'.")

failed_tests = [
    line.strip()
    for line in failed_tests_path.read_text(encoding="utf-8").splitlines()
    if line.strip()
]
if not failed_tests:
    raise SystemExit("ERROR: No leftover failed tests found in failed-tests.txt. Nothing to deflake.")

# Let the manual workflow override the inherited Testiny run name, but keep the
# selected-run value when the override input is left empty.
effective_testiny_run_name = env("TESTINY_RUN_NAME_OVERRIDE") or str(
    metadata.get("testiny_run_name", "")
).strip()

print(f"Deflake bundle directory: {bundle_dir}")
print(f"Selected workflow name: {metadata['source_workflow_name']}")
print(f"Selected workflow file: {metadata['source_workflow_file']}")
print(f"Selected run id: {metadata['source_run_id']}")
print(f"Selected ref: {metadata['source_ref_name']}")
print(f"Flavor: {metadata['flavor']}")
print(f"Selector: {metadata.get('selector_type', '')}={metadata.get('selector_value', '')}")
print(f"Resolved build number: {metadata.get('resolved_build_number', '')}")
print(f"Leftover failed tests: {len(failed_tests)}")

if effective_testiny_run_name:
    print(f"Effective TESTINY_RUN_NAME: {effective_testiny_run_name}")

# Expose the validated bundle fields as step outputs so later deflake steps can
# reuse them without reparsing metadata.json themselves.
write_output("bundleDir", str(bundle_dir))
write_output("sourceWorkflowName", str(metadata["source_workflow_name"]))
write_output("sourceWorkflowFile", str(metadata["source_workflow_file"]))
write_output("sourceRunId", str(metadata["source_run_id"]))
write_output("sourceRefName", str(metadata["source_ref_name"]))
write_output("flavor", str(metadata["flavor"]))
write_output("tags", str(metadata.get("tags", "")))
write_output("selectorType", str(metadata.get("selector_type", "")))
write_output("selectorValue", str(metadata.get("selector_value", "")))
write_output("appBuildNumberInput", str(metadata.get("app_build_number_input", "")))
write_output("resolvedBuildNumber", str(metadata.get("resolved_build_number", "")))
write_output("newApkName", str(metadata.get("new_apk_name", "")))
write_output("isUpgrade", str(metadata.get("is_upgrade", False)).lower())
write_output("oldBuildNumber", str(metadata.get("old_build_number", "")))
write_output("enforceAppInstall", str(metadata.get("enforce_app_install", False)).lower())
write_output("androidDeviceId", str(metadata.get("android_device_id", "")))
write_output("rerunFailedEnabled", str(metadata.get("rerun_failed_enabled", False)).lower())
write_output("rerunFailedCount", str(metadata.get("rerun_failed_count", "")))
write_output("failedTestCount", str(len(failed_tests)))
write_output("failedTestsFile", str(failed_tests_path))
write_output("firstAttemptFailedTestsFile", str(first_attempt_failed_tests_path))
write_output("effectiveTestinyRunName", effective_testiny_run_name)

# Mirror the key resolved values into the workflow summary so the deflake run
# shows its selected-run context without opening the artifact contents manually.
summary_lines = [
    "### Manual Deflake Input",
    f"- selected workflow name: {metadata['source_workflow_name']}",
    f"- selected workflow file: `{metadata['source_workflow_file']}`",
    f"- selected run id: {metadata['source_run_id']}",
    f"- selected ref: {metadata['source_ref_name']}",
    f"- flavor: {metadata['flavor']}",
    f"- selector: {metadata.get('selector_type', '')} = {metadata.get('selector_value', '')}",
    f"- resolved build number: {metadata.get('resolved_build_number', '')}",
    f"- leftover failed tests: {len(failed_tests)}",
]
if effective_testiny_run_name:
    summary_lines.append(f"- effective TESTINY_RUN_NAME: {effective_testiny_run_name}")
append_summary(summary_lines)
