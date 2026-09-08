#!/usr/bin/env python3
"""Unit tests for Wire notification body generation."""

from __future__ import annotations

import os
import sys
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))

from build_wire_notification import build_body, resolve_status_icon


class BuildWireNotificationTest(unittest.TestCase):
    def test_uses_success_icon_when_failed_job_has_recovered_test_results(self) -> None:
        with patch.dict(
            os.environ,
            {
                "JOB_STATUS": "failure",
                "TESTS_FAILED": "0",
                "TESTS_TOTAL": "25",
                "RUN_NUMBER": "123",
                "EVENT_NAME": "workflow_dispatch",
                "PASSED_ON_RERUN_COUNT": "2",
            },
            clear=True,
        ):
            body = build_body()

        self.assertTrue(body.startswith("✅ android critical flows #123"))
        self.assertIn("Failed: 0 / -2", body)

    def test_uses_failure_icon_when_final_test_results_have_failures(self) -> None:
        with patch.dict(
            os.environ,
            {
                "JOB_STATUS": "success",
                "TESTS_FAILED": "1",
                "TESTS_TOTAL": "25",
            },
            clear=True,
        ):
            self.assertEqual("❌", resolve_status_icon())

    def test_uses_cancelled_icon_before_test_result_summary(self) -> None:
        with patch.dict(
            os.environ,
            {
                "JOB_STATUS": "cancelled",
                "TESTS_FAILED": "0",
                "TESTS_TOTAL": "25",
            },
            clear=True,
        ):
            self.assertEqual("⚪", resolve_status_icon())

    def test_falls_back_to_job_status_when_result_summary_is_missing(self) -> None:
        with patch.dict(os.environ, {"JOB_STATUS": "failure"}, clear=True):
            self.assertEqual("❌", resolve_status_icon())


if __name__ == "__main__":
    unittest.main()
