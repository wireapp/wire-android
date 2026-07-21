#!/usr/bin/env python3
"""Unit tests for strict Allure failed-test extraction."""

from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from scripts.qa_android_ui_tests.extract_failed_tests import (
    ResultExtractionError,
    extract_attempt,
)


class ExtractFailedTestsTest(unittest.TestCase):
    def test_extracts_executed_and_failed_test_ids(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            attempt_dir = Path(directory)
            device_dir = attempt_dir / "device" / "allure-results"
            device_dir.mkdir(parents=True)
            self.write_result(device_dir / "passed-result.json", "ClassA", "passes", "passed")
            self.write_result(device_dir / "failed-result.json", "ClassB", "fails", "failed")

            executed, failed = extract_attempt(attempt_dir)

            self.assertEqual({"ClassA#passes", "ClassB#fails"}, executed)
            self.assertEqual({"ClassB#fails"}, failed)

    def test_rejects_malformed_result_json(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            attempt_dir = Path(directory)
            device_dir = attempt_dir / "device"
            device_dir.mkdir()
            (device_dir / "broken-result.json").write_text("{", encoding="utf-8")

            with self.assertRaisesRegex(ResultExtractionError, "invalid result files: 1"):
                extract_attempt(attempt_dir)

    def test_rejects_result_without_test_identity(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            attempt_dir = Path(directory)
            device_dir = attempt_dir / "device"
            device_dir.mkdir()
            (device_dir / "unknown-result.json").write_text(
                json.dumps({"status": "passed"}),
                encoding="utf-8",
            )

            with self.assertRaisesRegex(ResultExtractionError, "without test identity: 1"):
                extract_attempt(attempt_dir)

    @staticmethod
    def write_result(path: Path, class_name: str, method_name: str, status: str) -> None:
        path.write_text(
            json.dumps(
                {
                    "status": status,
                    "labels": [
                        {"name": "testClass", "value": class_name},
                        {"name": "testMethod", "value": method_name},
                    ],
                }
            ),
            encoding="utf-8",
        )


if __name__ == "__main__":
    unittest.main()
