#!/usr/bin/env python3
"""Unit tests for trusted manual-deflake source runs."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from validate_source_run import SourceRunValidationError, validate_run_metadata


class ValidateSourceRunTest(unittest.TestCase):
    def setUp(self) -> None:
        self.metadata = {
            "id": 12345,
            "repository": {"full_name": "wireapp/wire-android"},
            "path": ".github/workflows/qa-android-critical-flow-tests.yml",
            "head_sha": "a" * 40,
            "head_branch": "develop",
            "status": "completed",
            "conclusion": "failure",
        }

    def test_accepts_allowed_completed_default_branch_run(self) -> None:
        outputs = validate_run_metadata(self.metadata, "wireapp/wire-android", "develop")

        self.assertEqual("12345", outputs["sourceRunId"])
        self.assertEqual("a" * 40, outputs["sourceHeadSha"])

    def test_rejects_unapproved_workflow(self) -> None:
        self.metadata["path"] = ".github/workflows/untrusted.yml"

        with self.assertRaisesRegex(SourceRunValidationError, "not an allowed"):
            validate_run_metadata(self.metadata, "wireapp/wire-android", "develop")

    def test_rejects_non_default_branch_run(self) -> None:
        self.metadata["head_branch"] = "feature/unsafe"

        with self.assertRaisesRegex(SourceRunValidationError, "default branch"):
            validate_run_metadata(self.metadata, "wireapp/wire-android", "develop")

    def test_rejects_run_from_another_repository(self) -> None:
        self.metadata["repository"] = {"full_name": "attacker/fork"}

        with self.assertRaisesRegex(SourceRunValidationError, "another repository"):
            validate_run_metadata(self.metadata, "wireapp/wire-android", "develop")


if __name__ == "__main__":
    unittest.main()
