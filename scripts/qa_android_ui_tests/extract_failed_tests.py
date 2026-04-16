#!/usr/bin/env python3
"""Extract failed test IDs (Class#method) from one attempt of Allure results."""

from __future__ import annotations

import json
import os
import sys
from pathlib import Path

attempt_dir = Path(os.environ["ATTEMPT_RESULTS_DIR"])
failed_output = Path(os.environ["FAILED_TESTS_FILE"])

if not attempt_dir.is_dir():
    print(f"ERROR: ATTEMPT_RESULTS_DIR does not exist: {attempt_dir}", file=sys.stderr)
    sys.exit(1)

FAILED_STATUSES = {"failed", "broken", "unknown"}


def test_id_from_labels(data: dict) -> str:
    labels = data.get("labels", [])
    if not isinstance(labels, list):
        return ""
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
        return f"{class_name}#{method_name}"
    return ""


def test_id_from_full_name(data: dict) -> str:
    full_name = data.get("fullName")
    if not isinstance(full_name, str):
        return ""
    full_name = full_name.strip()
    if not full_name:
        return ""
    if "#" in full_name:
        return full_name
    if "." not in full_name:
        return ""
    class_name, method_name = full_name.rsplit(".", 1)
    class_name = class_name.strip()
    method_name = method_name.strip()
    if class_name and method_name:
        return f"{class_name}#{method_name}"
    return ""


def resolve_test_id(data: dict) -> str:
    return test_id_from_labels(data) or test_id_from_full_name(data)


def result_dirs(base: Path) -> list[Path]:
    out = []
    for device_dir in sorted(p for p in base.iterdir() if p.is_dir()):
        candidate = device_dir / "allure-results"
        out.append(candidate if candidate.is_dir() else device_dir)
    return out


failed = set()
executed = set()

for src_dir in result_dirs(attempt_dir):
    for result_file in sorted(src_dir.glob("*-result.json")):
        try:
            data = json.loads(result_file.read_text(encoding="utf-8"))
        except Exception:
            continue
        test_id = resolve_test_id(data)
        if not test_id:
            continue
        executed.add(test_id)
        status = data.get("status")
        if isinstance(status, str) and status in FAILED_STATUSES:
            failed.add(test_id)

failed_output.parent.mkdir(parents=True, exist_ok=True)
failed_output.write_text("\n".join(sorted(failed)) + ("\n" if failed else ""), encoding="utf-8")

print(f"executed={len(executed)}")
print(f"failed={len(failed)}")
