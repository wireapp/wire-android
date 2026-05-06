#!/usr/bin/env python3
"""Build the Wire notification body for critical flow workflow runs."""

from __future__ import annotations

import json
import os
from pathlib import Path


def env(name: str) -> str:
    return os.environ.get(name, "").strip()


def resolve_trigger_label(event_name: str) -> str:
    return {
        "merge_group": "merge queue",
        "schedule": "scheduled",
        "workflow_dispatch": "dispatch",
    }.get(event_name, event_name.replace("_", " "))


def resolve_pr_number() -> str:
    event_path = env("GITHUB_EVENT_PATH")
    if not event_path:
        return ""

    try:
        payload = json.loads(Path(event_path).read_text(encoding="utf-8"))
    except Exception:
        return ""

    pull_request = payload.get("pull_request") or {}
    if isinstance(pull_request, dict):
        pr_number = str(pull_request.get("number") or "").strip()
        if pr_number:
            return pr_number

    merge_group = payload.get("merge_group")
    if not isinstance(merge_group, dict):
        merge_group = {}

    prs = payload.get("pull_requests") or merge_group.get("pull_requests") or []
    if isinstance(prs, list) and prs:
        first = prs[0]
        if isinstance(first, dict):
            return str(first.get("number") or "").strip()

    return ""


def build_body() -> str:
    status = env("JOB_STATUS")
    icon = "❌" if status == "failure" else ("⚪" if status == "cancelled" else "✅")

    header = f"{icon} android critical flows #{env('RUN_NUMBER')}"
    pr_number = resolve_pr_number()
    if pr_number:
        header = f"{header} (PR #{pr_number})"

    lines = [header]

    tags = env("RESOLVED_TAGS")
    if tags:
        lines.append(tags)

    apk_name = env("APK_NAME")
    if apk_name:
        lines.append(apk_name)

    lines.append(f"triggered by: {resolve_trigger_label(env('EVENT_NAME'))}")

    allure_report_url = env("ALLURE_REPORT_URL")
    if allure_report_url:
        lines.append(f"See Allure Reports ({allure_report_url})")

    lines.append(
        "Tests passed: "
        f"{env('TESTS_PASSED') or '0'}, "
        f"Failed: {env('TESTS_FAILED') or '0'} / -{env('PASSED_ON_RERUN_COUNT') or '0'}, "
        f"Skipped: {env('TESTS_SKIPPED') or '0'}, "
        f"Total: {env('TESTS_TOTAL') or '0'} "
        f"({env('TESTS_SUCCESS_PERCENT') or '0'}%)"
    )

    return "\n".join(lines)


def main() -> None:
    output_path = env("GITHUB_OUTPUT")
    if not output_path:
        raise RuntimeError("GITHUB_OUTPUT is not set")

    body = build_body()
    with open(output_path, "a", encoding="utf-8") as output_file:
        output_file.write("body<<EOF\n")
        output_file.write(body)
        output_file.write("\nEOF\n")


if __name__ == "__main__":
    main()
