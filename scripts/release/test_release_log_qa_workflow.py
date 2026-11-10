#!/usr/bin/env python3
"""Structure tests for the QA export steps, the Make entrypoint, and the retry default."""

from __future__ import annotations

import os
import re
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from test_release_log_export import ROOT, UPLOAD, job_block, step_block, step_index, workflow
from test_release_log_workflow import run_bodies

QA = ".github/workflows/qa-android-regression-tests.yml"
EXPORT_STEP = "Export release log QA record"
UPLOAD_STEP = "Upload release log QA record"
SUMMARIZE = "Summarize merged Allure results"
CLEANUP = "Cleanup (remove secrets + build outputs)"
SUMMARY_OUTPUTS = ["passed", "failed", "skipped", "missing", "selected", "total"]
EXECUTION_JOBS = {"run-android-ui-tests": "full", "run-manual-deflake": "deflake"}


class QaExportStepsTest(unittest.TestCase):
    def setUp(self):
        self.text = workflow("qa-android-regression-tests.yml")

    def jobs(self):
        for name, mode in EXECUTION_JOBS.items():
            yield name, mode, job_block(self.text, name)

    def test_both_execution_jobs_export_after_the_summary_and_before_cleanup(self):
        for name, _, job in self.jobs():
            with self.subTest(job=name):
                order = [step_index(job, step) for step in (SUMMARIZE, EXPORT_STEP, UPLOAD_STEP, CLEANUP)]
                self.assertEqual(order, sorted(order))

    def test_the_steps_never_gate_the_run(self):
        for name, _, job in self.jobs():
            for step in (EXPORT_STEP, UPLOAD_STEP):
                with self.subTest(job=name, step=step):
                    block = step_block(job, step)
                    self.assertIn("continue-on-error: true", block)

    def test_they_run_after_failed_tests_but_not_after_cancellation(self):
        for name, _, job in self.jobs():
            for step in (EXPORT_STEP, UPLOAD_STEP):
                with self.subTest(job=name, step=step):
                    block = step_block(job, step)
                    self.assertIn("if: ${{ !cancelled() }}", block)
                    self.assertNotIn("success()", block)

    def test_the_export_reads_the_summarizer_outputs_even_when_it_failed(self):
        for name, _, job in self.jobs():
            with self.subTest(job=name):
                block = step_block(job, EXPORT_STEP)
                for output in SUMMARY_OUTPUTS:
                    self.assertIn("TESTS_%s: ${{ steps.summarize_allure_results.outputs.%s }}"
                                  % (output.upper(), output), block)
                self.assertIn("steps.publish_allure_report.outputs.allure_report_url", block)
                self.assertIn("id: summarize_allure_results", step_block(job, SUMMARIZE))

    def test_the_export_takes_the_run_mode_flavor_scope_and_retry_count_from_the_resolved_inputs(self):
        normal = step_block(job_block(self.text, "run-android-ui-tests"), EXPORT_STEP)
        self.assertIn("RELEASE_LOG_RUN_MODE: full", normal)
        for name in ("resolvedFlavor", "resolvedTags", "resolvedCategory", "resolvedTestCaseId", "resolvedIsUpgrade",
                     "resolvedRerunFailedCount"):
            self.assertIn("needs.validate-and-resolve-inputs.outputs." + name, normal)
        self.assertIn("DEFLAKE_BUNDLE_DIR: ${{ runner.temp }}/deflake-input\n", normal)
        deflake = step_block(job_block(self.text, "run-manual-deflake"), EXPORT_STEP)
        self.assertIn("RELEASE_LOG_RUN_MODE: deflake", deflake)
        self.assertIn("DEFLAKE_BUNDLE_ROOT: ${{ runner.temp }}/deflake-input\n", deflake)
        self.assertIn("DEFLAKE_BUNDLE_DIR: ${{ runner.temp }}/deflake-input-next\n", deflake)
        self.assertIn("needs.validate-and-resolve-inputs.outputs.resolvedRerunFailedCount", deflake)

    def test_the_export_runs_the_make_wrapper_and_writes_outside_the_workspace(self):
        for name, _, job in self.jobs():
            with self.subTest(job=name):
                block = step_block(job, EXPORT_STEP)
                self.assertIn("run: make qa-ui-report cmd=export-qa-release-log", block)
                self.assertIn("RELEASE_LOG_OUTPUT: ${{ runner.temp }}/release-log-qa/evidence.json", block)

    def test_the_upload_uses_the_run_identity_name_and_keeps_the_record_for_90_days(self):
        for name, _, job in self.jobs():
            with self.subTest(job=name):
                block = step_block(job, UPLOAD_STEP)
                self.assertIn(UPLOAD, block)
                self.assertIn("name: release-log-qa-${{ github.run_id }}-${{ github.run_attempt }}", block)
                self.assertIn("path: ${{ runner.temp }}/release-log-qa/evidence.json", block)
                self.assertIn("retention-days: 90", block)
                self.assertIn("if-no-files-found: ignore", block)

    def test_the_new_steps_use_no_secrets_and_no_expressions_in_shell(self):
        for name, _, job in self.jobs():
            for step in (EXPORT_STEP, UPLOAD_STEP):
                block = step_block(job, step)
                self.assertNotIn("secrets.", block)
                for body in run_bodies(block):
                    self.assertNotIn("${{", body)

    def test_test_execution_and_deflake_artifact_selection_are_untouched(self):
        deflake = job_block(self.text, "run-manual-deflake")
        download = step_block(deflake, "Download deflake input artifact")
        self.assertIn("run_id: ${{ needs.validate-and-resolve-inputs.outputs.sourceRunId }}", download)
        self.assertIn("name: android-ui-test-deflake-input", download)
        self.assertIn("path: ${{ runner.temp }}/deflake-input", download)
        run = step_block(deflake, "Run failed tests from deflake artifact")
        self.assertIn("make qa-ui-run", run)
        self.assertIn("INITIAL_FAILED_TESTS_FILE: ${{ steps.validate_deflake_input.outputs.failedTestsFile }}", run)
        for job_name in EXECUTION_JOBS:
            upload = step_block(job_block(self.text, job_name), "Upload deflake input artifact")
            self.assertIn("name: android-ui-test-deflake-input", upload)
        normal = step_block(job_block(self.text, "run-android-ui-tests"), "Run UI tests (auto-rerun failed tests)")
        self.assertIn("RERUN_FAILED_COUNT: ${{ needs.validate-and-resolve-inputs.outputs.resolvedRerunFailedCount }}",
                      normal)

    def test_the_prod_workflow_still_calls_the_regression_workflow_so_the_caller_owns_the_export(self):
        prod = workflow("qa-android-prod-tests.yml")
        self.assertIn("uses: ./.github/workflows/qa-android-regression-tests.yml", prod)
        self.assertIn("workflow_dispatch:", prod)
        self.assertNotIn("schedule:", prod)
        self.assertIn("sourceWorkflowFile: .github/workflows/qa-android-prod-tests.yml", prod)


class RetryDefaultTest(unittest.TestCase):
    def test_the_prod_workflow_defaults_to_two_retries_for_normal_runs_and_none_for_deflake(self):
        prod = workflow("qa-android-prod-tests.yml")
        self.assertIn("rerunFailedCount: ${{ inputs.rerunFailedCount || (inputs.runMode != 'Deflake failed tests' "
                      "&& '2' || '') }}", prod)

    def validate(self, run_mode, count):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "output"
            env = {**os.environ, "RUN_MODE": run_mode, "RERUN_FAILED_COUNT": count, "GITHUB_OUTPUT": str(output)}
            subprocess.run(["bash", "scripts/qa/android-ui/validation.sh", "validate-rerun-inputs"], cwd=ROOT,
                           env=env, check=True, capture_output=True)
            return dict(line.split("=", 1) for line in output.read_text().splitlines())

    def test_a_blank_deflake_count_resolves_to_zero(self):
        self.assertEqual(self.validate("Deflake failed tests", "")["rerunFailedCount"], "0")

    def test_the_value_the_prod_workflow_passes_for_a_normal_run_resolves_to_two(self):
        result = self.validate("Normal tests", "2")
        self.assertEqual((result["rerunFailedCount"], result["rerunFailedEnabled"]), ("2", "true"))

    def test_the_documented_default_matches(self):
        text = (ROOT / "docs" / "release-log.md").read_text()
        self.assertIn("two retries", text)
        self.assertIn("no retries", text)


class QaMakeEntrypointTest(unittest.TestCase):
    def test_the_target_is_a_thin_wrapper_and_listed_in_help(self):
        result = subprocess.run(["make", "-n", "release-log/qa-export", "output=evidence.json"], cwd=ROOT,
                                capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('python3 scripts/qa/android-ui/export_release_log.py --output "evidence.json"', result.stdout)
        help_text = subprocess.run(["make", "help"], cwd=ROOT, capture_output=True, text=True).stdout
        self.assertIn("release-log/qa-export", help_text)

    def test_the_check_target_runs_the_exporter_tests_too(self):
        result = subprocess.run(["make", "-n", "release-log/check"], cwd=ROOT, capture_output=True, text=True)
        self.assertIn("test_export_release_log.py", result.stdout)
        self.assertIn("test_release_log_*.py", result.stdout)


class ReportingScriptTest(unittest.TestCase):
    def run_reporting(self, **env):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "qa" / "evidence.json"
            full = {**os.environ, "RELEASE_LOG_OUTPUT": str(output), **env}
            result = subprocess.run(["bash", "scripts/qa/android-ui/reporting.sh", "export-qa-release-log"], cwd=ROOT,
                                    env=full, capture_output=True, text=True)
            return result, output.exists()

    def test_the_usage_lists_the_subcommand(self):
        result = subprocess.run(["bash", "scripts/qa/android-ui/reporting.sh"], cwd=ROOT, capture_output=True,
                                text=True)
        self.assertEqual(result.returncode, 2)
        self.assertIn("export-qa-release-log", result.stderr)

    def test_a_scheduled_run_writes_nothing_and_succeeds(self):
        env = {"GITHUB_REPOSITORY": "wireapp/wire-android", "GITHUB_RUN_ID": "1", "GITHUB_RUN_ATTEMPT": "1",
               "GITHUB_WORKFLOW_REF": "wireapp/wire-android/.github/workflows/qa-android-regression-tests.yml@refs/heads/develop",
               "GITHUB_EVENT_NAME": "schedule", "RELEASE_LOG_RUN_MODE": "full", "FLAVOR_INPUT": "production",
               "RERUN_FAILED_COUNT": "2", "TEST_SOURCE_SHA": "0" * 40}
        result, written = self.run_reporting(**env)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse(written)

    def test_the_output_path_is_required(self):
        env = {key: value for key, value in os.environ.items() if key != "RELEASE_LOG_OUTPUT"}
        result = subprocess.run(["bash", "scripts/qa/android-ui/reporting.sh", "export-qa-release-log"], cwd=ROOT,
                                env=env, capture_output=True, text=True)
        self.assertNotEqual(result.returncode, 0)


if __name__ == "__main__":
    unittest.main()
