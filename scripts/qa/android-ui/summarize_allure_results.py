#!/usr/bin/env python3
"""Summarize merged Allure result files for workflow notifications."""

from __future__ import annotations

import json
import os
from pathlib import Path

from extract_failed_tests import resolve_test_id


def env(name: str) -> str:
    return os.environ.get(name, "").strip()


def selected_deflake_tests() -> set[str] | None:
    path = env("DEFLAKE_FAILED_TESTS_FILE")
    if not path:
        return None
    tests = {line.strip() for line in Path(path).read_text(encoding="utf-8").splitlines() if line.strip()}
    if not tests:
        raise ValueError("The selected deflake test list is empty")
    return tests


def summarize_results(results: list[dict], selected: set[str] | None = None) -> dict[str, int]:
    if selected is None:
        statuses = [str(data.get("status") or "").strip().lower() for data in results]
        statuses = [status for status in statuses if status]
        missing = 0
        total = len(statuses)
    else:
        # Only explicit results for the selected source failures prove recovery.
        by_test = {resolve_test_id(data): data for data in results}
        statuses = [
            str(by_test[test].get("status") or "unknown").strip().lower()
            for test in selected if test in by_test
        ]
        missing = len(selected - by_test.keys())
        total = len(selected)
    passed = statuses.count("passed")
    skipped = statuses.count("skipped")
    failed = len(statuses) - passed - skipped
    return {
        "passed": passed, "failed": failed, "skipped": skipped, "missing": missing,
        "total": total, "selected": total, "success_percent": passed * 100 // total if total else 0,
    }


def main() -> None:
    if not env("MERGED_DIR"):
        raise RuntimeError("MERGED_DIR is not set")
    merged_dir = Path(env("MERGED_DIR"))

    output_path = env("GITHUB_OUTPUT")
    if not output_path:
        raise RuntimeError("GITHUB_OUTPUT is not set")

    results = []
    for result_file in sorted(merged_dir.glob("*-result.json")):
        try:
            data = json.loads(result_file.read_text(encoding="utf-8"))
        except Exception:
            continue

        if isinstance(data, dict):
            results.append(data)
    selected = selected_deflake_tests()
    counts = summarize_results(results, selected)

    with open(output_path, "a", encoding="utf-8") as output_file:
        for name, value in counts.items():
            output_file.write(f"{name}={value}\n")
        output_file.write(f"failed_after_retries={counts['failed']}\n")

    if selected is not None and env("GITHUB_STEP_SUMMARY"):
        lines = [
            "### Manual Deflake Results", f"- Tests selected: {counts['selected']}",
            f"- Passed: {counts['passed']}", f"- Still unsuccessful: {counts['failed']}",
        ]
        for name in ("skipped", "missing"):
            if counts[name]:
                lines.append(f"- {name.capitalize()}: {counts[name]}")
        with open(env("GITHUB_STEP_SUMMARY"), "a", encoding="utf-8") as summary:
            summary.write("\n".join(lines) + "\n")

    if counts["missing"]:
        raise SystemExit(f"ERROR: Missing final results for {counts['missing']} selected deflake test(s).")


if __name__ == "__main__":
    main()
