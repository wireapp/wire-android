#!/usr/bin/env python3
"""Check that deflake inherits the source run's Testiny setting."""

from __future__ import annotations

import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("inspect_deflake_bundle.py")


class InspectDeflakeBundleTest(unittest.TestCase):
    def inspect(self, testiny_name: str) -> str:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "metadata.json").write_text(json.dumps({
                "schema_version": 1,
                "source_workflow_name": "QA Android Critical Flow Tests",
                "source_workflow_file": ".github/workflows/qa-android-critical-flow-tests.yml",
                "source_repository": "wireapp/wire-android",
                "source_run_id": "123",
                "source_ref_name": "develop",
                "source_sha": "a" * 40,
                "test_service_url": "https://test-service.example",
                "flavor": "alpha release candidate",
                "testiny_run_name": testiny_name,
            }))
            (root / "failed-tests.txt").write_text("Class#fails\n")
            outputs_path = root / "outputs"
            result = subprocess.run(
                [sys.executable, str(SCRIPT)],
                env={
                    **os.environ,
                    "DEFLAKE_BUNDLE_ROOT": str(root),
                    "GITHUB_OUTPUT": str(outputs_path),
                    "GITHUB_STEP_SUMMARY": str(root / "summary"),
                    "EXPECTED_SOURCE_REPOSITORY": "wireapp/wire-android",
                    "EXPECTED_SOURCE_RUN_ID": "123",
                    "EXPECTED_SOURCE_SHA": "a" * 40,
                    "EXPECTED_SOURCE_WORKFLOW_FILE": ".github/workflows/qa-android-critical-flow-tests.yml",
                    "EXPECTED_SOURCE_REF_NAME": "develop",
                },
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(0, result.returncode, result.stderr)
            # Only the inherited Testiny output is relevant to this change.
            lines = outputs_path.read_text().splitlines()
            for index, line in enumerate(lines):
                if line.startswith("effectiveTestinyRunName<<"):
                    return lines[index + 1]
            self.fail("Missing effectiveTestinyRunName output")

    def test_inherits_original_testiny_name(self) -> None:
        self.assertEqual("Original Testiny run", self.inspect("Original Testiny run"))

    def test_keeps_testiny_name_empty_when_source_has_none(self) -> None:
        self.assertEqual("", self.inspect(""))


if __name__ == "__main__":
    unittest.main()
