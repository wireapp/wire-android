#!/usr/bin/env python3
"""Merge Allure results across devices and retry attempts."""

from __future__ import annotations

import hashlib
import json
import os
import shutil
import subprocess
from datetime import datetime, timezone
from pathlib import Path

out_dir = Path(os.environ["OUT_DIR"])
merged_dir = Path(os.environ["MERGED_DIR"])
merged_dir.mkdir(parents=True, exist_ok=True)

FAILED_STATUSES = {"failed", "broken", "unknown"}
PASSED_ON_RERUN_LABEL = "passed_on_rerun"


def get_prop(serial: str, prop: str) -> str:
    try:
        result = subprocess.run(
            ["adb", "-s", serial, "shell", "getprop", prop],
            check=False,
            capture_output=True,
            text=True,
            timeout=5,
        )
        return result.stdout.strip()
    except Exception:
        return ""


def list_attempt_device_dirs(base_dir: Path) -> list[tuple[int, Path]]:
    def contains_result_files(device_dir: Path) -> bool:
        src_candidate = device_dir / "allure-results"
        src_dir = src_candidate if src_candidate.is_dir() else device_dir
        return src_dir.is_dir() and any(src_dir.glob("*-result.json"))

    # Discover retry-aware layout first: OUT_DIR/attempt-N/<serial>/...
    attempt_dirs = []
    for candidate in sorted(base_dir.iterdir()):
        if not candidate.is_dir():
            continue
        if not candidate.name.startswith("attempt-"):
            continue
        suffix = candidate.name[len("attempt-") :]
        if not suffix.isdigit():
            continue
        attempt_dirs.append((int(suffix), candidate))

    pairs: list[tuple[int, Path]] = []
    if attempt_dirs:
        max_attempt = 0
        for attempt, attempt_dir in sorted(attempt_dirs, key=lambda item: item[0]):
            max_attempt = max(max_attempt, attempt)
            for device_dir in sorted(p for p in attempt_dir.iterdir() if p.is_dir()):
                pairs.append((attempt, device_dir))

        # reporting.sh fallback pull stores results under OUT_DIR/<serial>/...
        # Treat those as the latest synthetic attempt so fallback data is never ignored.
        synthetic_attempt = max_attempt + 1
        for device_dir in sorted(p for p in base_dir.iterdir() if p.is_dir() and not p.name.startswith("attempt-")):
            if contains_result_files(device_dir):
                pairs.append((synthetic_attempt, device_dir))
        return pairs

    for device_dir in sorted(p for p in base_dir.iterdir() if p.is_dir()):
        if contains_result_files(device_dir):
            pairs.append((0, device_dir))
    return pairs


def resolve_src_dir(device_dir: Path) -> Path:
    candidate = device_dir / "allure-results"
    return candidate if candidate.is_dir() else device_dir


def device_label(serial: str, cache: dict[str, dict[str, str]]) -> str:
    meta = cache.get(serial) or {}
    model = meta.get("model") or "unknown"
    sdk = meta.get("sdk") or "unknown"
    return f"{model} - {sdk} ({serial})"


def add_label(data: dict, name: str, value: str) -> dict:
    labels = [label for label in data.get("labels", []) if label.get("name") != name]
    labels.append({"name": name, "value": value})
    data["labels"] = labels
    return data


def add_parameter(data: dict, name: str, value: str) -> dict:
    params = [param for param in data.get("parameters", []) if param.get("name") != name]
    params.append({"name": name, "value": value})
    data["parameters"] = params
    return data


def result_identity(data: dict, fallback: str) -> str:
    full_name = data.get("fullName")
    if isinstance(full_name, str) and full_name.strip():
        return full_name.strip()

    labels = data.get("labels", [])
    if isinstance(labels, list):
        class_name = ""
        method_name = ""
        for label in labels:
            if not isinstance(label, dict):
                continue
            name = label.get("name")
            value = label.get("value")
            if not isinstance(value, str):
                continue
            if name == "testClass" and not class_name:
                class_name = value.strip()
            elif name == "testMethod" and not method_name:
                method_name = value.strip()
        if class_name and method_name:
            return f"{class_name}.{method_name}"
    return fallback


def safe_write_result(filename: str, payload: str) -> None:
    target = merged_dir / filename
    if not target.exists():
        target.write_text(payload, encoding="utf-8")
        return

    # Two different source files can share the same filename across attempts/devices.
    existing = target.read_text(encoding="utf-8")
    if existing == payload:
        return

    digest = hashlib.sha1(payload.encode("utf-8")).hexdigest()[:10]
    alt = merged_dir / f"{digest}-{filename}"
    alt.write_text(payload, encoding="utf-8")


attempt_device_dirs = list_attempt_device_dirs(out_dir)
device_info: dict[str, dict[str, str]] = {}
# Resolve device metadata once so the same label is reused across all attempts.
for _, device_dir in attempt_device_dirs:
    serial = device_dir.name
    if serial in device_info:
        continue
    model = get_prop(serial, "ro.product.model") or "unknown"
    sdk = get_prop(serial, "ro.build.version.release") or get_prop(serial, "ro.build.version.sdk") or "unknown"
    device_info[serial] = {"model": model, "sdk": sdk}

first_status: dict[str, str] = {}
latest_by_test: dict[str, dict] = {}

# Walk every device/attempt result and keep the latest outcome for each logical test.
for attempt, device_dir in attempt_device_dirs:
    serial = device_dir.name
    src_dir = resolve_src_dir(device_dir)
    if not src_dir.is_dir():
        continue

    label = device_label(serial, device_info)
    for item in src_dir.iterdir():
        if item.is_dir():
            continue
        if item.name in ("executor.json", "environment.properties"):
            continue
        if item.name.endswith("-result.json"):
            try:
                data = json.loads(item.read_text(encoding="utf-8"))
            except Exception:
                continue

            data = add_label(data, "device", label)
            data = add_label(data, "host", label)
            data = add_parameter(data, "device", label)
            data = add_parameter(data, "attempt", str(attempt))

            # Use one stable identity across attempts so the final report keeps the latest outcome per test.
            identity = result_identity(data, fallback=item.stem)
            status = str(data.get("status") or "").strip().lower()
            if identity not in first_status:
                first_status[identity] = status

            prev = latest_by_test.get(identity)
            if prev is None or attempt >= prev["attempt"]:
                latest_by_test[identity] = {
                    "attempt": attempt,
                    "status": status,
                    "name": item.name,
                    "data": data,
                }
        else:
            shutil.copy2(item, merged_dir / item.name)

# Write the final reportable result set after all attempts have been compared.
for identity, info in latest_by_test.items():
    data = info["data"]
    initial_status = first_status.get(identity, "")
    final_status = str(info.get("status", "")).lower()
    # Merge logic is the single owner of passed_on_rerun because it can see first and final status together.
    if initial_status in FAILED_STATUSES and final_status == "passed":
        data = add_label(data, PASSED_ON_RERUN_LABEL, "true")

    payload = json.dumps(data, ensure_ascii=True)
    safe_write_result(info["name"], payload)

failed_first_attempt = sum(1 for status in first_status.values() if status in FAILED_STATUSES)
failed_after_retries = sum(
    1
    for info in latest_by_test.values()
    if str(info.get("status", "")).lower() in FAILED_STATUSES
)
passed_on_rerun_count = 0
for identity, info in latest_by_test.items():
    initial_status = first_status.get(identity, "")
    final_status = str(info.get("status", "")).lower()
    if initial_status in FAILED_STATUSES and final_status == "passed":
        passed_on_rerun_count += 1

# Write Environment tab metadata for the merged Allure report.
env_lines = []
if device_info:
    devices = ", ".join(device_label(serial, device_info) for serial in sorted(device_info.keys()))
    env_lines.append(f"devices={devices}")

apk_version = os.environ.get("REAL_BUILD_NUMBER", "").strip()
apk_name = os.environ.get("NEW_APK_NAME", "").strip()
if apk_version:
    env_lines.append(f"apk={apk_version}")
elif apk_name:
    env_lines.append(f"apk={apk_name}")

run_number = os.environ.get("GITHUB_RUN_NUMBER", "").strip()
if run_number:
    env_lines.append(f"run={run_number}")

run_date = datetime.now(timezone.utc).strftime("%Y-%m-%d")
env_lines.append(f"date={run_date}")

tags_input = os.environ.get("INPUT_TAGS", "").strip()
if tags_input:
    env_lines.append(f"input_tags={tags_input}")

env_lines.append(f"failed_first_attempt={failed_first_attempt}")
env_lines.append(f"passed_on_rerun={passed_on_rerun_count}")
env_lines.append(f"failed_after_retries={failed_after_retries}")

if env_lines:
    (merged_dir / "environment.properties").write_text(
        "\n".join(env_lines) + "\n",
        encoding="utf-8",
    )

run_id = os.environ.get("GITHUB_RUN_ID", "")
repo = os.environ.get("GITHUB_REPOSITORY", "")
server = os.environ.get("GITHUB_SERVER_URL", "https://github.com")
run_url = f"{server}/{repo}/actions/runs/{run_id}" if repo and run_id else ""
build_name = run_number
if run_number and apk_version:
    build_name = f"{run_number} / {apk_version}"
report_name = "Android UI Tests"
if apk_version:
    report_name = f"Android UI Tests ({apk_version})"

# Write Executor widget metadata so Allure links back to this GitHub Actions run.
executor = {
    "name": "GitHub Actions",
    "type": "github",
    "url": run_url,
    "buildName": build_name,
    "buildUrl": run_url,
    "reportName": report_name,
}
(merged_dir / "executor.json").write_text(
    json.dumps(executor, ensure_ascii=True),
    encoding="utf-8",
)
