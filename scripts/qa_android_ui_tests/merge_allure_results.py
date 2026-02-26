#!/usr/bin/env python3
"""Merge per-device Allure results into one reportable dataset."""

import json
import os
import shutil
import subprocess
from datetime import datetime, timezone
from pathlib import Path

out_dir = Path(os.environ["OUT_DIR"])
merged_dir = Path(os.environ["MERGED_DIR"])
merged_dir.mkdir(parents=True, exist_ok=True)


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


device_dirs = [p for p in out_dir.iterdir() if p.is_dir()]
device_info = {}
for device_dir in device_dirs:
    serial = device_dir.name
    model = get_prop(serial, "ro.product.model") or "unknown"
    sdk = get_prop(serial, "ro.build.version.release") or get_prop(serial, "ro.build.version.sdk") or "unknown"
    device_info[serial] = {"model": model, "sdk": sdk}


def device_label(serial: str) -> str:
    meta = device_info.get(serial, {})
    model = meta.get("model") or "unknown"
    sdk = meta.get("sdk") or "unknown"
    return f"{model} - {sdk} ({serial})"


def add_label(data: dict, name: str, value: str) -> dict:
    labels = [l for l in data.get("labels", []) if l.get("name") != name]
    labels.append({"name": name, "value": value})
    data["labels"] = labels
    return data


def add_parameter(data: dict, name: str, value: str) -> dict:
    params = [p for p in data.get("parameters", []) if p.get("name") != name]
    params.append({"name": name, "value": value})
    data["parameters"] = params
    return data


for device_dir in device_dirs:
    serial = device_dir.name
    # Support both pull layouts: <serial>/allure-results/* and <serial>/*.
    src_dir = device_dir / "allure-results"
    if not src_dir.is_dir():
        src_dir = device_dir
    if not src_dir.is_dir():
        continue

    label = device_label(serial)
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
            # Attach a stable per-device label for filtering and debugging in Allure.
            data = add_label(data, "device", label)
            data = add_label(data, "host", label)
            data = add_parameter(data, "device", label)
            (merged_dir / item.name).write_text(
                json.dumps(data, ensure_ascii=True),
                encoding="utf-8",
            )
        else:
            shutil.copy2(item, merged_dir / item.name)

env_lines = []
if device_info:
    devices = ", ".join(device_label(serial) for serial in sorted(device_info.keys()))
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

if env_lines:
    # Write Environment tab metadata for Allure.
    (merged_dir / "environment.properties").write_text(
        "\n".join(env_lines) + "\n", encoding="utf-8"
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

executor = {
    "name": "GitHub Actions",
    "type": "github",
    "url": run_url,
    "buildName": build_name,
    "buildUrl": run_url,
    "reportName": report_name,
}
# Write Executor widget metadata for Allure.
(merged_dir / "executor.json").write_text(
    json.dumps(executor, ensure_ascii=True),
    encoding="utf-8",
)
