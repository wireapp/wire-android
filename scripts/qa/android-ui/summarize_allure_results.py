#!/usr/bin/env python3
"""Summarize merged Allure result files for workflow notifications."""

from __future__ import annotations

import json
import os
from pathlib import Path

FAILED_STATUSES = {"failed", "broken", "unknown"}
SKIPPED_STATUSES = {"skipped"}
PASSED_STATUSES = {"passed"}


def env(name: str) -> str:
    return os.environ.get(name, "").strip()


def main() -> None:
    merged_dir = Path(env("MERGED_DIR"))
    if not merged_dir:
        raise RuntimeError("MERGED_DIR is not set")

    output_path = env("GITHUB_OUTPUT")
    if not output_path:
        raise RuntimeError("GITHUB_OUTPUT is not set")

    passed = 0
    failed = 0
    skipped = 0
    total = 0

    for result_file in sorted(merged_dir.glob("*-result.json")):
        try:
            data = json.loads(result_file.read_text(encoding="utf-8"))
        except Exception:
            continue

        status = str(data.get("status") or "").strip().lower()
        if not status:
            continue

        total += 1
        if status in PASSED_STATUSES:
            passed += 1
        elif status in SKIPPED_STATUSES:
            skipped += 1
        elif status in FAILED_STATUSES:
            failed += 1
        else:
            failed += 1

    success_percent = (passed * 100 // total) if total else 0

    with open(output_path, "a", encoding="utf-8") as output_file:
        output_file.write(f"passed={passed}\n")
        output_file.write(f"failed={failed}\n")
        output_file.write(f"skipped={skipped}\n")
        output_file.write(f"total={total}\n")
        output_file.write(f"success_percent={success_percent}\n")
        output_file.write(f"failed_after_retries={failed}\n")


if __name__ == "__main__":
    main()
