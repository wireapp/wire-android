#!/usr/bin/env python3
"""Unit tests for publishable Allure data sanitization."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from allure_sanitization import REDACTED_FAILURE_MESSAGE, sanitize_allure_payload


class AllureSanitizationTest(unittest.TestCase):
    def test_removes_nested_attachments_parameters_and_failure_details(self) -> None:
        payload = {
            "name": "test name",
            "parameters": [{"name": "password", "value": "secret"}],
            "attachments": [{"name": "screen", "source": "screenshot.png"}],
            "statusDetails": {
                "message": "user@example.com failed with password secret",
                "trace": "sensitive stack and request data",
                "flaky": True,
            },
            "steps": [
                {
                    "name": "safe step",
                    "attachments": [{"source": "nested.png"}],
                    "parameters": [{"name": "token", "value": "secret"}],
                }
            ],
        }

        sanitized = sanitize_allure_payload(payload)

        self.assertEqual("test name", sanitized["name"])
        self.assertEqual([], sanitized["parameters"])
        self.assertEqual([], sanitized["attachments"])
        self.assertEqual([], sanitized["steps"][0]["attachments"])
        self.assertEqual([], sanitized["steps"][0]["parameters"])
        self.assertEqual(
            {"message": REDACTED_FAILURE_MESSAGE, "flaky": True},
            sanitized["statusDetails"],
        )

    def test_preserves_non_sensitive_structure(self) -> None:
        payload = {"status": "passed", "labels": [{"name": "tag", "value": "criticalFlow"}]}

        self.assertEqual(payload, sanitize_allure_payload(payload))


if __name__ == "__main__":
    unittest.main()
