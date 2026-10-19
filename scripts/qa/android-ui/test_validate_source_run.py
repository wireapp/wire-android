#!/usr/bin/env python3
"""Unit tests for trusted manual-deflake source runs."""

from __future__ import annotations

import sys
import json
import os
import tempfile
import unittest
from io import StringIO
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))

from validate_source_run import SourceRunValidationError, main, validate_run_metadata


class ValidateSourceRunTest(unittest.TestCase):
    def request(self, run_id, payload, *, allow_latest="true", workflow=".github/workflows/qa-android-fdroid-tests.yml"):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "outputs"
            values = {
                "GITHUB_REPOSITORY": "wireapp/wire-android", "SOURCE_RUN_ID": run_id,
                "DEFAULT_BRANCH": "develop", "GITHUB_TOKEN": "test-only",
                "GITHUB_OUTPUT": str(output), "ALLOW_LATEST_SOURCE_RUN": allow_latest,
                "EXPECTED_SOURCE_WORKFLOW_FILE": workflow,
            }
            with patch.dict(os.environ, values, clear=True), patch("builtins.print"), patch(
                "validate_source_run.urllib.request.urlopen", return_value=StringIO(json.dumps(payload))
            ) as api:
                main()
            return api.call_args.args[0].full_url, output.read_text()

    def setUp(self) -> None:
        self.metadata = {
            "id": 12345,
            "repository": {"full_name": "wireapp/wire-android"},
            "path": ".github/workflows/qa-android-regression-tests.yml",
            "head_sha": "a" * 40,
            "head_branch": "develop",
            "status": "completed",
            "conclusion": "failure",
        }

    def test_accepts_allowed_completed_default_branch_run(self) -> None:
        outputs = validate_run_metadata(self.metadata, "wireapp/wire-android", "develop")

        self.assertEqual("12345", outputs["sourceRunId"])
        self.assertEqual("a" * 40, outputs["sourceHeadSha"])

    def test_blank_id_selects_latest_completed_run_of_requesting_workflow(self) -> None:
        for filename in ("qa-android-regression-tests.yml", "qa-android-fdroid-tests.yml"):
            with self.subTest(filename=filename):
                workflow = f".github/workflows/{filename}"
                self.metadata["path"] = workflow
                older = {**self.metadata, "id": 12344}
                url, outputs = self.request("  ", {"workflow_runs": [self.metadata, older]}, workflow=workflow)
                self.assertEqual(
                    f"https://api.github.com/repos/wireapp/wire-android/actions/workflows/"
                    f"{filename}/runs?branch=develop&status=completed&per_page=1", url,
                )
                self.assertIn("\n12345\n", outputs)
                self.assertNotIn("12344", outputs)

    def test_explicit_id_uses_that_run_without_latest_lookup(self) -> None:
        for filename in ("qa-android-regression-tests.yml", "qa-android-fdroid-tests.yml"):
            with self.subTest(filename=filename):
                workflow = f".github/workflows/{filename}"
                self.metadata["path"] = workflow
                url, outputs = self.request(" 12345 ", self.metadata, workflow=workflow)
                self.assertEqual("https://api.github.com/repos/wireapp/wire-android/actions/runs/12345", url)
                self.assertIn("\n12345\n", outputs)

    def test_latest_lookup_rejects_empty_or_invalid_results_without_fallback(self) -> None:
        older = {**self.metadata, "path": ".github/workflows/qa-android-fdroid-tests.yml", "id": 12344}
        for payload, message in (
            ({"workflow_runs": []}, "No completed source runs"),
            ({"workflow_runs": [self.metadata, older]}, "not an allowed"),
            ({"workflow_runs": [{**older, "conclusion": "cancelled"}, older]}, "completed success/failure"),
            ({"workflow_runs": [{**older, "head_branch": "unsafe"}, older]}, "default branch"),
        ):
            with self.subTest(payload=payload), self.assertRaisesRegex(SystemExit, message):
                self.request("", payload)

    def test_missing_id_still_rejected_without_latest_opt_in(self) -> None:
        with self.assertRaisesRegex(SystemExit, "must be numeric"):
            self.request("", {}, allow_latest="false", workflow=self.metadata["path"])

    def test_rejects_unapproved_workflow(self) -> None:
        self.metadata["path"] = ".github/workflows/untrusted.yml"

        with self.assertRaisesRegex(SourceRunValidationError, "not an allowed"):
            validate_run_metadata(self.metadata, "wireapp/wire-android", "develop")

    def test_each_workflow_accepts_only_its_own_source_runs(self) -> None:
        workflows = (
            ".github/workflows/qa-android-regression-tests.yml",
            ".github/workflows/qa-android-fdroid-tests.yml",
        )
        for expected in workflows:
            for source in workflows:
                with self.subTest(expected=expected, source=source):
                    self.metadata["path"] = source
                    if source == expected:
                        outputs = validate_run_metadata(self.metadata, "wireapp/wire-android", "develop", expected)
                        self.assertEqual(source, outputs["sourceWorkflowFile"])
                    else:
                        with self.assertRaisesRegex(SourceRunValidationError, "not an allowed"):
                            validate_run_metadata(self.metadata, "wireapp/wire-android", "develop", expected)

    def test_regression_default_rejects_fdroid_source(self) -> None:
        self.metadata["path"] = ".github/workflows/qa-android-fdroid-tests.yml"
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
