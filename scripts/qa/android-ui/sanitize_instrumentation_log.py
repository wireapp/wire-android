#!/usr/bin/env python3
"""Trim noisy instrumentation output before it reaches public GitHub logs."""

from __future__ import annotations

import re
import sys


STACK_TRACE_LINE = re.compile(r"^\s*at\s+[\w.$]+\(.*\)$")
OMIT_PATTERNS = (
    re.compile(r"^-{2,}\s*(begin|end) exception\s*-{2,}$", re.IGNORECASE),
    re.compile(r"^\s*Caused by:"),
    re.compile(r"^\s*\.\.\. \d+ more$"),
    STACK_TRACE_LINE,
)
FAILURE_REASON_PREFIX = "TEST_FAILURE_REASON: "
FAILURE_PATTERNS = (
    (re.compile(r"Element not found with selector", re.IGNORECASE), "Element not found"),
    (re.compile(r"\bAssertionError\b", re.IGNORECASE), "Assertion failed"),
    (re.compile(r"\bComparisonFailure\b", re.IGNORECASE), "Comparison failed"),
    (re.compile(r"\binvalid_grant\b|\bInvalid user credentials\b", re.IGNORECASE), "Authentication failed"),
    (re.compile(r"\bHttpRequestException\b", re.IGNORECASE), "Backend request failed"),
    (re.compile(r"\bSocketException\b", re.IGNORECASE), "Network error"),
    (re.compile(r"\bWaiterError\b", re.IGNORECASE), "Wait condition failed"),
)


def summarize_failure(message: str) -> str:
    for pattern, summary in FAILURE_PATTERNS:
        if pattern.search(message):
            return summary

    cleaned = re.sub(r"selector:.*", "", message, flags=re.IGNORECASE)
    cleaned = re.sub(r"resourceId='[^']*'", "resourceId='…'", cleaned)
    cleaned = re.sub(r"text='[^']*'", "text='…'", cleaned)
    cleaned = re.sub(r"'[^']{2,}'", "'…'", cleaned)
    cleaned = re.sub(r'"[^"]{2,}"', '"…"', cleaned)
    cleaned = re.sub(r"\b[\w.]+\.kt:\d+\b", "", cleaned)
    cleaned = re.sub(r"\b[a-zA-Z_][\w$.]*\([^)]+\)", "", cleaned)
    cleaned = re.sub(r"\s+", " ", cleaned).strip(" :-")
    if not cleaned:
        return "Test failed"
    return cleaned[:137] + "..." if len(cleaned) > 140 else cleaned


def sanitize_line(serial: str, raw_line: str) -> str | None:
    line = raw_line.rstrip("\n")
    if not line:
        return f"[{serial}]"

    if any(pattern.match(line) for pattern in OMIT_PATTERNS):
        return None

    stack_marker = "INSTRUMENTATION_STATUS: stack="
    if stack_marker in line:
        message = line.split(stack_marker, 1)[1]
        return f"[{serial}] {FAILURE_REASON_PREFIX}{summarize_failure(message)}"

    if re.match(r"^[\w.$]+(?:Exception|Error):", line):
        return f"[{serial}] {FAILURE_REASON_PREFIX}{summarize_failure(line)}"

    if "Element not found with selector" in line:
        return f"[{serial}] {FAILURE_REASON_PREFIX}Element not found"

    return f"[{serial}] {line}"


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: sanitize_instrumentation_log.py <device-serial>")

    serial = sys.argv[1]
    for raw_line in sys.stdin:
        sanitized = sanitize_line(serial, raw_line)
        if sanitized is None:
            continue
        print(sanitized, flush=True)


if __name__ == "__main__":
    main()
