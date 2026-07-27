#!/usr/bin/env python3
"""Remove sensitive data from Allure payloads before Pages publication."""

from __future__ import annotations

from typing import Any


REDACTED_FAILURE_MESSAGE = "Failure details omitted from the publishable report."
SAFE_STATUS_DETAIL_FLAGS = {"known", "muted", "flaky"}


def sanitize_allure_payload(value: Any) -> Any:
    if isinstance(value, list):
        return [sanitize_allure_payload(item) for item in value]
    if not isinstance(value, dict):
        return value

    sanitized = {}
    for key, child in value.items():
        if key in {"attachments", "parameters"}:
            sanitized[key] = []
        elif key == "statusDetails":
            safe_status_details = {"message": REDACTED_FAILURE_MESSAGE}
            if isinstance(child, dict):
                for flag in SAFE_STATUS_DETAIL_FLAGS:
                    if isinstance(child.get(flag), bool):
                        safe_status_details[flag] = child[flag]
            sanitized[key] = safe_status_details
        else:
            sanitized[key] = sanitize_allure_payload(child)
    return sanitized
