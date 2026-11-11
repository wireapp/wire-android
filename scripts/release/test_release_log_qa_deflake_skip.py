#!/usr/bin/env python3
"""A deflake of a run of another flavor must end in a clean skip, from the exporter through the collector."""

from __future__ import annotations

import json
import os
import sys
import tempfile
import unittest
from contextlib import redirect_stderr, redirect_stdout
from io import StringIO
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE.parents[1] / "scripts" / "qa" / "android-ui"))

import export_release_log as export
import release_log_contract as contract
import release_log_report as report
import test_release_log_report as fakes

REPOSITORY = "wireapp/wire-android"
PROD_REF = REPOSITORY + "/.github/workflows/qa-android-prod-tests.yml@refs/heads/develop"
SHA = "0123456789abcdef0123456789abcdef01234567"


def run_exporter(root, flavor, skip_output=True):
    bundle = Path(root) / "bundle"
    bundle.mkdir()
    (bundle / "metadata.json").write_text(json.dumps({
        "schema_version": 1, "source_run_id": "1519", "source_run_attempt": "1", "flavor": flavor,
        "tags": "@RC", "selector_type": "category", "selector_value": "rc", "is_upgrade": False,
        "test_source_sha": SHA}))
    (bundle / "failed-tests.txt").write_text("A#one\n")
    env = {
        "GITHUB_REPOSITORY": REPOSITORY, "GITHUB_RUN_ID": "1520", "GITHUB_RUN_ATTEMPT": "1",
        "GITHUB_WORKFLOW_REF": PROD_REF, "GITHUB_EVENT_NAME": "workflow_dispatch",
        "RELEASE_LOG_RUN_MODE": "deflake", "DEFLAKE_BUNDLE_ROOT": str(bundle), "RERUN_FAILED_COUNT": "0",
        "TESTS_PASSED": "1", "TESTS_FAILED": "0", "TESTS_SKIPPED": "0", "TESTS_MISSING": "0",
        "TESTS_SELECTED": "1", "TESTS_TOTAL": "1",
    }
    record, marker = Path(root) / "out" / "evidence.json", Path(root) / "skip" / "skip.json"
    if skip_output:
        env["RELEASE_LOG_SKIP_OUTPUT"] = str(marker)
    old = os.environ.copy()
    os.environ.clear()
    os.environ.update(env)
    try:
        with redirect_stdout(StringIO()), redirect_stderr(StringIO()):
            status = export.main(["--output", str(record)])
    finally:
        os.environ.clear()
        os.environ.update(old)
    return status, record, marker


class DeflakeOfAnotherFlavorTest(unittest.TestCase):
    def test_the_exporter_writes_the_marker_and_no_record(self):
        with tempfile.TemporaryDirectory() as root:
            status, record, marker = run_exporter(root, "fdroid")
            self.assertEqual(status, 0)
            self.assertFalse(record.exists())
            self.assertEqual(contract.read_json(marker), contract.skip_marker(REPOSITORY, 1520, 1))

    def test_the_collector_ends_in_a_clean_skip_with_that_marker(self):
        with tempfile.TemporaryDirectory() as root:
            _, _, marker = run_exporter(root, "fdroid")
            api = fakes.RegressionApi()
            api.add_run(fakes.qa_run(), fakes.qa_jobs())
            api.add_artifact(1520, "release-log-qa-skip-1520-1", fakes.zip_of({"skip.json": marker.read_text()}))
            api.add_production()
            self.assertIsNone(fakes.collect_regression(api))

    def test_without_the_marker_the_collector_still_reports_missing_evidence(self):
        with tempfile.TemporaryDirectory() as root:
            _, record, marker = run_exporter(root, "fdroid", skip_output=False)
            self.assertFalse(record.exists() or marker.exists())
            api = fakes.RegressionApi()
            api.add_run(fakes.qa_run(), fakes.qa_jobs())
            with self.assertRaises(report.CollectionError):
                fakes.collect_regression(api)

    def test_a_deflake_of_a_production_run_writes_a_record_and_no_marker(self):
        with tempfile.TemporaryDirectory() as root:
            status, record, marker = run_exporter(root, "production")
            self.assertEqual(status, 0)
            self.assertEqual(contract.read_json(record)["mode"], "deflake")
            self.assertFalse(marker.exists())


if __name__ == "__main__":
    unittest.main()
