#!/usr/bin/env python3
"""Unit tests for the QA regression record exporter."""

from __future__ import annotations

import copy
import hashlib
import json
import os
import subprocess
import sys
import tempfile
import unittest
from contextlib import redirect_stderr, redirect_stdout
from io import StringIO
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE.parents[1] / "release"))

import export_release_log as export
import release_log_contract as contract

REPOSITORY = "wireapp/wire-android"
PROD_REF = REPOSITORY + "/.github/workflows/qa-android-prod-tests.yml@refs/heads/develop"
REGRESSION_REF = REPOSITORY + "/.github/workflows/qa-android-regression-tests.yml@refs/heads/develop"
SHA = "0123456789abcdef0123456789abcdef01234567"
NEW_BYTES = b"new apk bytes"
OLD_BYTES = b"old apk bytes"
FIRST = "a" * 64
SECOND = "b" * 64


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def full_context(**changes):
    context = {
        "repository": REPOSITORY, "run_id": "1520", "run_attempt": "1", "workflow_ref": PROD_REF,
        "event_name": "workflow_dispatch", "test_source_sha": SHA, "flavor": "production", "mode": "full",
        "suite": "rc", "selector": "@RC", "is_upgrade": True, "retry_count": 2,
        "new_apk_name": "Wire-production-v4.38.0-71389.apk", "new_version_label": "4.38.0-71389",
        "old_apk_name": "Wire-production-v4.37.0-71000.apk", "old_version_label": "4.37.0-71000",
        "source_qa_run_id": None, "source_qa_run_attempt": None,
        "next_deflake_bundle_sha256": FIRST, "source_deflake_bundle_sha256": None,
    }
    context.update(changes)
    return context


RESULTS = {"passed": 412, "failed": 6, "skipped": 3, "missing": 0, "selected": 421, "total": 421,
           "passed_on_rerun": 2}


class TemporaryFiles(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.root = Path(directory.name)
        self.new_apk = self.root / "Wire.apk"
        self.old_apk = self.root / "Wire.old.apk"
        self.new_apk.write_bytes(NEW_BYTES)
        self.old_apk.write_bytes(OLD_BYTES)

    def record(self, context=None, results=RESULTS, url="https://example.github.io/wire-android/qa/1/",
               new_apk=True, old_apk=True):
        return export.regression_record(
            context or full_context(), self.new_apk if new_apk else None, self.old_apk if old_apk else None,
            results, url)


class RegressionRecordTest(TemporaryFiles):
    def test_a_manual_production_run_produces_a_valid_record(self):
        record = self.record()
        contract.validate_record(record)
        self.assertEqual((record["kind"], record["run_id"], record["run_attempt"]), ("regression", 1520, 1))
        self.assertEqual(record["workflow_path"], ".github/workflows/qa-android-prod-tests.yml")
        self.assertEqual((record["flavor"], record["suite"], record["selector"], record["mode"]),
                         ("production", "rc", "@RC", "full"))
        self.assertEqual((record["retry_count"], record["is_upgrade"], record["test_source_sha"]),
                         (2, True, SHA))
        self.assertEqual(record["results"], RESULTS)
        self.assertEqual(record["report_url"], "https://example.github.io/wire-android/qa/1/")
        self.assertEqual((record["source_qa_run_id"], record["source_qa_run_attempt"]), (None, None))
        self.assertEqual((record["next_deflake_bundle_sha256"], record["source_deflake_bundle_sha256"]),
                         (FIRST, None))

    def test_apks_are_hashed_from_the_downloaded_files_under_their_release_names(self):
        record = self.record()
        self.assertEqual(record["apk"], {"filename": "Wire-production-v4.38.0-71389.apk",
                                          "sha256": sha256(NEW_BYTES), "resolved_version_label": "4.38.0-71389"})
        self.assertEqual(record["old_apk"], {"filename": "Wire-production-v4.37.0-71000.apk",
                                              "sha256": sha256(OLD_BYTES), "resolved_version_label": "4.37.0-71000"})

    def test_the_digest_follows_the_file_content_and_never_the_name(self):
        self.new_apk.write_bytes(b"different")
        self.assertEqual(self.record()["apk"]["sha256"], sha256(b"different"))

    def test_a_missing_or_unreadable_apk_is_null_never_invented(self):
        record = self.record(new_apk=False, old_apk=False)
        self.assertEqual((record["apk"], record["old_apk"]), (None, None))
        self.new_apk.unlink()
        self.assertIsNone(self.record()["apk"])
        link = self.root / "link.apk"
        link.symlink_to(self.old_apk)
        self.assertIsNone(export.regression_record(full_context(), link, None, RESULTS, None)["apk"])

    def test_an_apk_name_with_path_segments_is_not_recorded(self):
        for name in ["../evil.apk", "a/b.apk", "a\\b.apk", "", ".."]:
            with self.subTest(name=name):
                record = self.record(full_context(new_apk_name=name))
                self.assertEqual(record["apk"]["filename"], "Wire.apk")

    def test_a_blank_version_label_is_null(self):
        self.assertIsNone(self.record(full_context(new_version_label=""))["apk"]["resolved_version_label"])

    def test_a_run_without_an_upgrade_has_no_old_apk_even_when_a_file_is_left_behind(self):
        self.assertIsNone(self.record(full_context(is_upgrade=False))["old_apk"])

    def test_results_are_optional_and_never_become_a_passing_zero(self):
        record = self.record(results=None, url=None)
        self.assertEqual(record["results"], {key: None for key in contract.RESULT_KEYS})
        self.assertIsNone(record["report_url"])

    def test_missing_selected_tests_stay_in_the_record(self):
        results = dict(RESULTS, passed=415, failed=0, missing=3)
        self.assertEqual(self.record(results=results)["results"], results)

    def test_an_empty_full_run_stays_empty(self):
        results = {key: 0 for key in contract.RESULT_KEYS}
        self.assertEqual(self.record(results=results)["results"], results)

    def test_the_total_is_the_sum_of_the_counts_when_it_is_absent(self):
        results = dict(RESULTS, total=None, selected=None)
        record = self.record(results=results)
        self.assertEqual((record["results"]["total"], record["results"]["selected"]), (421, None))

    def test_counts_that_contradict_each_other_are_dropped_but_the_rerun_count_is_kept(self):
        results = dict(RESULTS, total=1)
        record = self.record(results=results)
        self.assertEqual(record["results"], dict({key: None for key in contract.RESULT_KEYS}, passed_on_rerun=2))
        results = dict(RESULTS, selected=1000)
        self.assertIsNone(self.record(results=results)["results"]["passed"])

    def test_the_retry_count_is_the_current_setting_from_zero_through_three(self):
        for count in [0, 1, 3]:
            self.assertEqual(self.record(full_context(retry_count=count))["retry_count"], count)
        for count in [-1, 4, "2", None, True]:
            with self.subTest(count=count), self.assertRaises(ValueError):
                self.record(full_context(retry_count=count))


class EligibilityTest(TemporaryFiles):
    def test_scheduled_and_unrelated_runs_produce_no_record(self):
        cases = [dict(event_name="schedule"), dict(event_name="workflow_call"), dict(event_name="push"),
                 dict(workflow_ref=REGRESSION_REF),
                 dict(workflow_ref=REPOSITORY + "/.github/workflows/qa-android-prod-tests-extra.yml@refs/heads/x"),
                 dict(workflow_ref="other/repo/.github/workflows/qa-android-prod-tests.yml@refs/heads/x"),
                 dict(workflow_ref=PROD_REF.replace("@refs/heads/develop", "")), dict(workflow_ref="")]
        for change in cases:
            with self.subTest(change=change):
                self.assertIsNone(self.record(full_context(**change)))

    def test_only_the_production_flavor_produces_a_record(self):
        for flavor in ["alpha release candidate", "alpha beta", "experimental", "debug", "fdroid", "", "Production"]:
            with self.subTest(flavor=flavor):
                self.assertIsNone(self.record(full_context(flavor=flavor)))
        self.assertIsNotNone(self.record(full_context(flavor="production")))

    def test_the_tags_value_does_not_identify_a_production_regression(self):
        self.assertIsNone(self.record(full_context(workflow_ref=REGRESSION_REF, suite="rc", selector="@RC")))

    def test_a_deflake_of_a_production_run_produces_a_record(self):
        record = self.record(deflake_context())
        self.assertEqual((record["mode"], record["source_qa_run_id"], record["source_qa_run_attempt"]),
                         ("deflake", 1520, 1))
        self.assertEqual((record["source_deflake_bundle_sha256"], record["next_deflake_bundle_sha256"]),
                         (FIRST, SECOND))

    def test_a_deflake_of_another_flavor_produces_no_record(self):
        self.assertIsNone(self.record(deflake_context(flavor="alpha release candidate")))


def deflake_context(**changes):
    values = dict(mode="deflake", source_qa_run_id=1520, source_qa_run_attempt=1, is_upgrade=False,
                  retry_count=0, next_deflake_bundle_sha256=SECOND, source_deflake_bundle_sha256=FIRST)
    values.update(changes)
    return full_context(**values)


class DeflakeRecordTest(TemporaryFiles):
    def test_the_deflake_keeps_its_own_results_and_retry_count(self):
        results = {"passed": 6, "failed": 0, "skipped": 0, "missing": 0, "selected": 6, "total": 6,
                   "passed_on_rerun": 0}
        record = self.record(deflake_context(retry_count=1), results=results)
        self.assertEqual((record["results"], record["retry_count"]), (results, 1))
        contract.validate_record(record)

    def test_a_deflake_needs_its_source_identity_and_a_matching_selected_count(self):
        with self.assertRaises(ValueError):
            self.record(deflake_context(source_qa_run_id=None))
        record = self.record(deflake_context(), results=dict(RESULTS, selected=5, total=6, passed=410))
        self.assertIsNone(record["results"]["total"])

    def test_the_source_bundle_proof_is_optional_for_the_record(self):
        record = self.record(deflake_context(source_deflake_bundle_sha256=None))
        self.assertIsNone(record["source_deflake_bundle_sha256"])

    def test_a_full_run_cannot_carry_a_source(self):
        with self.assertRaises(ValueError):
            self.record(full_context(source_qa_run_id=5, source_qa_run_attempt=1))


class BundleFingerprintTest(unittest.TestCase):
    def test_it_hashes_the_canonical_json_of_the_metadata_and_the_ordered_ids(self):
        expected = hashlib.sha256(
            b'{"failed_test_ids":["a#x","b#y"],"metadata":{"a":1,"b":{"c":"\xc3\xa9"}}}').hexdigest()
        self.assertEqual(export.bundle_fingerprint({"b": {"c": "\u00e9"}, "a": 1}, ["a#x", "b#y"]), expected)

    def test_key_order_does_not_matter_but_the_id_order_does(self):
        one = export.bundle_fingerprint({"a": 1, "b": 2}, ["x", "y"])
        self.assertEqual(one, export.bundle_fingerprint({"b": 2, "a": 1}, ["x", "y"]))
        self.assertNotEqual(one, export.bundle_fingerprint({"a": 1, "b": 2}, ["y", "x"]))

    def test_any_change_to_the_metadata_or_the_list_changes_it(self):
        base = export.bundle_fingerprint({"a": 1}, ["x"])
        self.assertNotEqual(base, export.bundle_fingerprint({"a": 2}, ["x"]))
        self.assertNotEqual(base, export.bundle_fingerprint({"a": 1}, ["x", "y"]))
        self.assertNotEqual(base, export.bundle_fingerprint({"a": 1}, []))
        self.assertRegex(base, r"^[0-9a-f]{64}$")


class ReadBundleTest(unittest.TestCase):
    def write_bundle(self, root, metadata, failed="", wrapper=None):
        directory = Path(root) / wrapper if wrapper else Path(root)
        directory.mkdir(parents=True, exist_ok=True)
        (directory / "metadata.json").write_text(json.dumps(metadata))
        (directory / "failed-tests.txt").write_text(failed)
        return directory

    def test_the_fingerprint_comes_from_the_files_in_file_order(self):
        with tempfile.TemporaryDirectory() as root:
            metadata = {"schema_version": 1, "source_run_id": "7"}
            self.write_bundle(root, metadata, "B#two\n\nA#one  \n")
            self.assertEqual(export.read_bundle(Path(root)),
                             (metadata, ["B#two", "A#one"]))
            self.assertEqual(export.directory_fingerprint(Path(root)),
                             export.bundle_fingerprint(metadata, ["B#two", "A#one"]))

    def test_a_wrapper_directory_is_found_like_the_inspector_finds_it(self):
        with tempfile.TemporaryDirectory() as root:
            metadata = {"schema_version": 1}
            self.write_bundle(root, metadata, "A#one\n", wrapper="android-ui-test-deflake-input")
            self.assertEqual(export.read_bundle(Path(root))[1], ["A#one"])

    def test_a_missing_or_ambiguous_bundle_has_no_fingerprint(self):
        with tempfile.TemporaryDirectory() as root:
            self.assertIsNone(export.directory_fingerprint(Path(root)))
            self.assertIsNone(export.directory_fingerprint(Path(root) / "absent"))
            self.write_bundle(root, {"a": 1}, "x\n", wrapper="one")
            self.write_bundle(root, {"a": 1}, "x\n", wrapper="two")
            self.assertIsNone(export.directory_fingerprint(Path(root)))

    def test_a_bundle_with_unreadable_metadata_has_no_fingerprint(self):
        with tempfile.TemporaryDirectory() as root:
            (Path(root) / "metadata.json").write_text("{not json")
            (Path(root) / "failed-tests.txt").write_text("x\n")
            self.assertIsNone(export.directory_fingerprint(Path(root)))


def environment(root, **changes):
    values = {
        "GITHUB_REPOSITORY": REPOSITORY, "GITHUB_RUN_ID": "1520", "GITHUB_RUN_ATTEMPT": "1",
        "GITHUB_WORKFLOW_REF": PROD_REF, "GITHUB_EVENT_NAME": "workflow_dispatch",
        "RELEASE_LOG_RUN_MODE": "full", "FLAVOR_INPUT": "production", "TAGS_INPUT": "@RC",
        "RESOLVED_CATEGORY": "rc", "RESOLVED_TESTCASE_ID": "", "IS_UPGRADE": "true", "RERUN_FAILED_COUNT": "2",
        "TEST_SOURCE_SHA": SHA, "NEW_APK_PATH": str(Path(root) / "Wire.apk"),
        "NEW_APK_NAME": "Wire-production-v4.38.0-71389.apk", "REAL_BUILD_NUMBER": "4.38.0-71389",
        "OLD_APK_PATH": str(Path(root) / "Wire.old.apk"), "OLD_APK_NAME": "Wire-production-v4.37.0-71000.apk",
        "OLD_BUILD_NUMBER": "4.37.0-71000", "TESTS_PASSED": "412", "TESTS_FAILED": "6", "TESTS_SKIPPED": "3",
        "TESTS_MISSING": "0", "TESTS_SELECTED": "421", "TESTS_TOTAL": "421", "PASSED_ON_RERUN_COUNT": "2",
        "ALLURE_REPORT_URL": "https://example.github.io/wire-android/qa-prod-tests/run/",
    }
    values.update(changes)
    return values


class EnvironmentTest(TemporaryFiles):
    def env(self, **changes):
        return environment(self.root, **changes)

    def test_the_environment_becomes_the_same_record(self):
        context = export.context_from_env(self.env())
        results = export.results_from_env(self.env())
        record = export.regression_record(context, self.new_apk, self.old_apk, results,
                                          self.env()["ALLURE_REPORT_URL"])
        self.assertEqual(record["results"], RESULTS)
        self.assertEqual((record["suite"], record["selector"], record["retry_count"]), ("rc", "@RC", 2))
        self.assertEqual(record["apk"]["filename"], "Wire-production-v4.38.0-71389.apk")
        self.assertIsNone(record["next_deflake_bundle_sha256"])

    def test_the_selector_is_the_tags_text_and_the_suite_is_the_resolved_scope(self):
        cases = [
            (dict(TAGS_INPUT="@TC-8143", RESOLVED_CATEGORY="", RESOLVED_TESTCASE_ID="TC-8143"), "TC-8143", "@TC-8143"),
            (dict(TAGS_INPUT="", RESOLVED_CATEGORY="", RESOLVED_TESTCASE_ID=""), "all", "all"),
            (dict(TAGS_INPUT=" @RC ", RESOLVED_CATEGORY="rc"), "rc", "@RC"),
        ]
        for change, suite, selector in cases:
            with self.subTest(change=change):
                context = export.context_from_env(self.env(**change))
                self.assertEqual((context["suite"], context["selector"]), (suite, selector))

    def test_blank_or_invalid_counts_are_null_and_never_zero(self):
        results = export.results_from_env(self.env(TESTS_PASSED="", TESTS_FAILED="x", TESTS_SKIPPED="-1",
                                                   TESTS_MISSING="1.5", PASSED_ON_RERUN_COUNT=""))
        self.assertEqual(results, {"passed": None, "failed": None, "skipped": None, "missing": None,
                                   "selected": 421, "total": 421, "passed_on_rerun": None})

    def test_retry_recovery_comes_from_retry_state_not_the_summarizer(self):
        state = self.root / "retry"
        state.mkdir()
        (state / "first-attempt-failed.txt").write_text("A#one\nB#two\nC#three\n")
        (state / "attempt-1-failed.txt").write_text("C#three\n")
        env = self.env(PASSED_ON_RERUN_COUNT="", RETRY_STATE_DIR=str(state),
                       FIRST_FAILED_TESTS_FILE=str(state / "first-attempt-failed.txt"),
                       FINAL_FAILED_TESTS_FILE=str(state / "attempt-1-failed.txt"))
        self.assertEqual(export.results_from_env(env)["passed_on_rerun"], 2)
        env["PASSED_ON_RERUN_COUNT"] = "2"
        self.assertEqual(export.results_from_env(env)["passed_on_rerun"], 2)

    def test_without_retry_state_the_recovery_count_is_unknown(self):
        env = self.env(PASSED_ON_RERUN_COUNT="")
        self.assertIsNone(export.results_from_env(env)["passed_on_rerun"])

    def test_a_deflake_takes_its_scope_and_source_from_the_consumed_bundle(self):
        bundle = self.root / "consumed"
        bundle.mkdir()
        metadata = {"schema_version": 1, "source_run_id": "1520", "source_run_attempt": "2",
                    "flavor": "production", "tags": "@RC", "selector_type": "category",
                    "selector_value": "rc", "is_upgrade": False, "test_source_sha": SHA}
        (bundle / "metadata.json").write_text(json.dumps(metadata))
        (bundle / "failed-tests.txt").write_text("A#one\n")
        context = export.context_from_env(self.env(
            RELEASE_LOG_RUN_MODE="deflake", FLAVOR_INPUT="alpha beta", TAGS_INPUT="", RESOLVED_CATEGORY="",
            IS_UPGRADE="false", RERUN_FAILED_COUNT="0", DEFLAKE_BUNDLE_ROOT=str(bundle)))
        self.assertEqual((context["mode"], context["flavor"], context["suite"], context["selector"]),
                         ("deflake", "production", "rc", "@RC"))
        self.assertEqual((context["source_qa_run_id"], context["source_qa_run_attempt"]), (1520, 2))
        self.assertEqual(context["source_deflake_bundle_sha256"], export.bundle_fingerprint(metadata, ["A#one"]))
        self.assertEqual(context["test_source_sha"], SHA)

    def test_a_deflake_without_a_readable_bundle_is_an_error(self):
        with self.assertRaises(ValueError):
            export.context_from_env(self.env(RELEASE_LOG_RUN_MODE="deflake",
                                             DEFLAKE_BUNDLE_ROOT=str(self.root / "absent")))

    def test_the_produced_bundle_is_fingerprinted_from_its_files(self):
        produced = self.root / "produced"
        produced.mkdir()
        metadata = {"schema_version": 1, "source_run_id": "1520"}
        (produced / "metadata.json").write_text(json.dumps(metadata))
        (produced / "failed-tests.txt").write_text("A#one\n")
        context = export.context_from_env(self.env(DEFLAKE_BUNDLE_DIR=str(produced)))
        self.assertEqual(context["next_deflake_bundle_sha256"], export.bundle_fingerprint(metadata, ["A#one"]))

    def test_the_test_source_is_the_checked_out_commit_when_it_is_not_given(self):
        calls = []
        original = export.checked_out_sha
        export.checked_out_sha = lambda: calls.append(1) or SHA
        try:
            context = export.context_from_env(self.env(TEST_SOURCE_SHA=""))
        finally:
            export.checked_out_sha = original
        self.assertEqual((context["test_source_sha"], calls), (SHA, [1]))


class CommandLineTest(TemporaryFiles):
    def run_main(self, **changes):
        output = self.root / "out" / "evidence.json"
        env = environment(self.root, **changes)
        stdout, stderr = StringIO(), StringIO()
        old = os.environ.copy()
        os.environ.clear()
        os.environ.update(env)
        try:
            with redirect_stdout(stdout), redirect_stderr(stderr):
                status = export.main(["--output", str(output)])
        finally:
            os.environ.clear()
            os.environ.update(old)
        return status, output, stderr.getvalue()

    def test_a_production_run_writes_one_valid_record(self):
        status, output, _ = self.run_main()
        self.assertEqual(status, 0)
        record = contract.read_json(output)
        contract.validate_record(record)
        self.assertEqual(record["results"]["passed_on_rerun"], 2)

    def test_an_ineligible_run_writes_nothing_and_succeeds(self):
        for change in [dict(GITHUB_EVENT_NAME="schedule"), dict(GITHUB_WORKFLOW_REF=REGRESSION_REF),
                       dict(FLAVOR_INPUT="alpha beta")]:
            with self.subTest(change=change):
                status, output, _ = self.run_main(**change)
                self.assertEqual(status, 0)
                self.assertFalse(output.exists())

    def test_an_ineligible_run_writes_no_skip_marker(self):
        for change in [dict(GITHUB_EVENT_NAME="schedule"), dict(FLAVOR_INPUT="alpha beta"), dict(FLAVOR_INPUT="fdroid")]:
            with self.subTest(change=change):
                _, output, _ = self.run_main(**change)
                self.assertEqual(list(output.parent.glob("*")) if output.parent.exists() else [], [])

    def test_failed_tests_still_export_what_the_summarizer_wrote(self):
        status, output, _ = self.run_main(TESTS_PASSED="10", TESTS_FAILED="2", TESTS_SKIPPED="0",
                                          TESTS_MISSING="0", TESTS_SELECTED="12", TESTS_TOTAL="12")
        self.assertEqual(status, 0)
        self.assertEqual(contract.read_json(output)["results"]["failed"], 2)

    def test_an_error_names_the_reason_and_writes_nothing(self):
        status, output, message = self.run_main(RERUN_FAILED_COUNT="9")
        self.assertEqual(status, 1)
        self.assertFalse(output.exists())
        self.assertIn("Release log QA export failed", message)

    def test_a_missing_variable_is_an_error(self):
        status, output, message = self.run_main(GITHUB_RUN_ID="")
        self.assertEqual(status, 1)
        self.assertIn("GITHUB_RUN_ID", message)

    def test_an_old_record_is_not_left_behind_to_be_uploaded(self):
        output = self.root / "out" / "evidence.json"
        output.parent.mkdir()
        output.write_text("{}")
        status, _, _ = self.run_main(GITHUB_EVENT_NAME="schedule")
        self.assertEqual(status, 0)
        self.assertFalse(output.exists())


class SummarizerKeepsItsOutputTest(unittest.TestCase):
    def test_the_summarizer_writes_its_outputs_before_it_fails_on_missing_tests(self):
        with tempfile.TemporaryDirectory() as root:
            merged = Path(root) / "merged"
            merged.mkdir()
            (merged / "a-result.json").write_text(json.dumps(
                {"status": "passed", "fullName": "pkg.A#one", "testCaseId": "x"}))
            selected = Path(root) / "failed.txt"
            selected.write_text("pkg.A#one\npkg.B#two\n")
            outputs = Path(root) / "outputs"
            result = subprocess.run(
                [sys.executable, str(HERE / "summarize_allure_results.py")],
                env={**os.environ, "MERGED_DIR": str(merged), "GITHUB_OUTPUT": str(outputs),
                     "DEFLAKE_FAILED_TESTS_FILE": str(selected)},
                capture_output=True, text=True)
            self.assertNotEqual(result.returncode, 0)
            text = outputs.read_text()
            self.assertIn("missing=1", text)
            self.assertIn("total=2", text)
            self.assertIn("selected=2", text)


if __name__ == "__main__":
    unittest.main()
