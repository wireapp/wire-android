#!/usr/bin/env python3
"""Unit tests for the release log contract."""

from __future__ import annotations

import copy
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import release_log_contract as contract

FIXTURES = Path(__file__).resolve().parent / "fixtures" / "release-log"


def load(name):
    return json.loads((FIXTURES / (name + ".json")).read_text())


class ReleaseVersionTest(unittest.TestCase):
    def test_beta_and_production_tags_share_one_base_version(self):
        self.assertEqual(contract.release_version("v4.38.0-beta.2"), "4.38.0")
        self.assertEqual(contract.release_version("v4.38.0"), "4.38.0")

    def test_hotfix_is_its_own_version(self):
        self.assertEqual(contract.release_version("v4.38.1"), "4.38.1")

    def test_invalid_tags_fail_without_a_guessed_version(self):
        for tag in ["", "4.38.0", "v4.38", "v4.38.0-rc.1", "v4.38.0-beta.0", "v04.38.0",
                    "v4.38.0-beta.1 ", "refs/tags/v4.38.0", "v4.38.0-beta", None, 438]:
                with self.subTest(tag=tag), self.assertRaises(ValueError):
                    contract.release_version(tag)

    def test_beta_number_comes_from_the_tag(self):
        self.assertEqual(contract.beta_number("v4.38.0-beta.12"), 12)
        self.assertIsNone(contract.beta_number("v4.38.0"))


class ValidateRecordTest(unittest.TestCase):
    def test_every_fixture_is_valid(self):
        for name in ["context", "build", "build_prod", "regression", "regression_deflake"]:
            with self.subTest(name=name):
                contract.validate_record(load(name))

    def assertRejected(self, name, mutate):
        record = copy.deepcopy(load(name))
        mutate(record)
        with self.assertRaises(ValueError):
            contract.validate_record(record)

    def test_non_dict_and_unknown_kind_are_rejected(self):
        for value in [None, [], "context", 1]:
            with self.subTest(value=value), self.assertRaises(ValueError):
                contract.validate_record(value)
        self.assertRejected("context", lambda r: r.update(kind="other"))

    def test_schema_version_must_be_the_integer_one(self):
        for value in [2, "1", True, 1.0, None]:
            with self.subTest(value=value):
                self.assertRejected("build", lambda r: r.update(schema_version=value))

    def test_missing_and_unknown_fields_are_rejected(self):
        for name in ["context", "build", "regression"]:
            with self.subTest(name=name):
                self.assertRejected(name, lambda r: r.pop("run_id"))
                self.assertRejected(name, lambda r: r.update(issue_key="WPB-1"))

    def test_run_identity_must_be_positive_integers_and_not_booleans(self):
        for field in ["run_id", "run_attempt"]:
            for value in [0, -1, True, "7", 1.5, None]:
                with self.subTest(field=field, value=value):
                    self.assertRejected("build", lambda r: r.update({field: value}))

    def test_shas_and_digests_must_be_lowercase_hex_of_the_right_length(self):
        for value in ["abc", "A" * 40, "g" * 40, "a" * 41, None]:
            with self.subTest(sha=value):
                self.assertRejected("build", lambda r: r.update(sha=value))
                self.assertRejected("regression", lambda r: r.update(test_source_sha=value))
        for value in ["a" * 63, "A" * 64, "z" * 64, None]:
            with self.subTest(digest=value):
                self.assertRejected("build", lambda r: r["apks"][0].update(sha256=value))
                self.assertRejected("regression", lambda r: r["apk"].update(sha256=value))

    def test_apk_file_names_have_no_path_segments(self):
        for value in ["../a.apk", "dir/a.apk", "dir\\a.apk", "", ".", ".."]:
            with self.subTest(filename=value):
                self.assertRejected("build", lambda r: r["apks"][0].update(filename=value))

    def test_urls_must_be_https(self):
        for value in ["http://example.com/a", "ftp://x", "example.com", ""]:
            with self.subTest(url=value):
                self.assertRejected("regression", lambda r: r.update(report_url=value))

    def test_build_flavor_and_apks_are_constrained(self):
        self.assertRejected("build", lambda r: r.update(flavor="Dev"))
        self.assertRejected("build", lambda r: r.update(apks=[]))
        self.assertRejected("build", lambda r: r.update(version_code=0))
        self.assertRejected("context", lambda r: r.update(tag="v4.38.0-rc.1"))
        self.assertRejected("context", lambda r: r.update(repository="not-a-repository"))

    def test_regression_is_limited_to_production_modes_and_retries(self):
        self.assertRejected("regression", lambda r: r.update(flavor="fdroid"))
        self.assertRejected("regression", lambda r: r.update(mode="partial"))
        for value in [-1, 4, True, "2", None]:
            with self.subTest(retry=value):
                self.assertRejected("regression", lambda r: r.update(retry_count=value))
        self.assertRejected("regression", lambda r: r.update(is_upgrade="yes"))

    def test_apk_metadata_may_be_null(self):
        record = load("regression")
        record["apk"] = None
        record["old_apk"] = None
        contract.validate_record(record)

    def test_full_runs_carry_no_source_execution_and_deflake_runs_must(self):
        self.assertRejected("regression", lambda r: r.update(source_qa_run_id=1))
        self.assertRejected("regression", lambda r: r.update(source_deflake_bundle_sha256="a" * 64))
        self.assertRejected("regression_deflake", lambda r: r.update(source_qa_run_id=None))
        self.assertRejected("regression_deflake", lambda r: r.update(source_qa_run_attempt=None))

    def test_result_counts_must_add_up(self):
        self.assertRejected("regression", lambda r: r["results"].update(total=420))
        self.assertRejected("regression", lambda r: r["results"].update(passed=-1))
        self.assertRejected("regression", lambda r: r["results"].update(passed=True))
        self.assertRejected("regression", lambda r: r["results"].pop("missing"))
        self.assertRejected("regression_deflake", lambda r: r["results"].update(selected=5))

    def test_a_total_that_contradicts_the_parts_is_rejected_in_both_directions(self):
        for name in ["regression", "regression_deflake"]:
            for delta in [-1, 1]:
                with self.subTest(name=name, delta=delta):
                    def mutate(record):
                        record["results"]["total"] += delta
                        record["results"]["selected"] = record["results"]["total"]
                    self.assertRejected(name, mutate)

    def test_a_total_is_accepted_when_it_matches_the_parts(self):
        record = load("regression")
        results = record["results"]
        self.assertEqual(results["total"], sum(results[key] for key in ("passed", "failed", "skipped", "missing")))
        contract.validate_record(record)

    def test_unavailable_counts_are_allowed_and_stay_null(self):
        record = load("regression")
        record["results"] = {key: None for key in record["results"]}
        contract.validate_record(record)


PAYLOADS = ["payload_beta", "payload_failed_build", "payload_regression", "payload_deflake", "payload_unmatched"]
FIELD_NAMES = {"releaseVersion", "kind", "result", "runUrl", "commit", "apkName", "apkSha256", "buildRunUrl",
               "deploy", "tests", "gaps"}
BETA_APK = "5c2091ab" * 8
PROD_APK = "d7330f18" * 8


def run_url(run):
    return "https://github.com/wireapp/wire-android/actions/runs/%d/attempts/1" % run


class ValidatePayloadTest(unittest.TestCase):
    def test_every_payload_fixture_is_valid(self):
        for name in PAYLOADS:
            with self.subTest(name=name):
                contract.validate_payload(load(name))

    def assertRejected(self, name, mutate):
        payload = copy.deepcopy(load(name))
        mutate(payload)
        with self.assertRaises(ValueError):
            contract.validate_payload(payload)

    def test_envelope_is_strict(self):
        self.assertRejected("payload_beta", lambda p: p.update(schema_version=2))
        self.assertRejected("payload_beta", lambda p: p.update(kind="other"))
        self.assertRejected("payload_beta", lambda p: p.update(issue_key="WPB-1"))
        self.assertRejected("payload_beta", lambda p: p.update(jql="project = WPB"))
        self.assertRejected("payload_beta", lambda p: p.pop("evidence"))
        self.assertRejected("payload_beta", lambda p: p["evidence"].update(extra=1))

    def test_event_id_must_match_the_source_run(self):
        self.assertRejected("payload_beta", lambda p: p.update(event_id="beta:wireapp/wire-android:1001:2"))
        self.assertRejected("payload_beta", lambda p: p.update(event_id="production:wireapp/wire-android:1001:1"))

    def test_release_version_must_be_a_bare_version_or_unmatched_for_regression(self):
        for value in ["4.38", "v4.38.0", "Android 4.38.0", "", None]:
            with self.subTest(value=value):
                self.assertRejected("payload_beta", lambda p: p.update(release_version=value))
        self.assertRejected("payload_beta", lambda p: p.update(release_version="unmatched"))

    def test_evidence_must_belong_to_the_kind(self):
        self.assertRejected("payload_regression", lambda p: p["evidence"].update(build=load("build")))
        self.assertRejected("payload_beta", lambda p: p["evidence"].update(qa=load("regression")))
        self.assertRejected("payload_beta", lambda p: p["evidence"].update(
            origin={"run_id": 1, "run_attempt": 1, "url": "https://example.com/r"}))

    def test_embedded_records_are_validated_and_match_the_source_run(self):
        self.assertRejected("payload_beta", lambda p: p["evidence"]["build"].update(sha="bad"))
        self.assertRejected("payload_beta", lambda p: p["evidence"]["build"].update(run_id=7))
        self.assertRejected("payload_regression", lambda p: p["evidence"]["qa"].update(run_attempt=2))

    def test_source_conclusion_jobs_and_gaps_are_constrained(self):
        self.assertRejected("payload_beta", lambda p: p["evidence"]["source"].update(url="http://example.com"))
        self.assertRejected("payload_beta", lambda p: p["evidence"].update(conclusion="weird"))
        self.assertRejected("payload_beta", lambda p: p["evidence"]["jobs"].append({"name": "x", "conclusion": 5}))
        self.assertRejected("payload_beta", lambda p: p["evidence"].update(gaps="none"))
        self.assertRejected("payload_beta", lambda p: p["evidence"].update(gaps=[""]))


class BuildFieldsTest(unittest.TestCase):
    def test_beta_fields_state_that_regression_is_not_required(self):
        self.assertEqual(contract.build_fields(load("payload_beta")), {
            "releaseVersion": "4.38.0", "kind": "Beta 2", "result": "passed", "runUrl": run_url(1001),
            "commit": "7be41d0", "apkName": "wire-beta-release.apk", "apkSha256": BETA_APK,
            "buildRunUrl": "",
            "deploy": "Validate beta tag: success; Build Beta-Release: success; Publish beta prerelease: success",
            "tests": "Regression: not required", "gaps": "none"})

    def test_a_failed_build_without_an_apk_reports_its_gap(self):
        fields = contract.build_fields(load("payload_failed_build"))
        self.assertEqual(fields["kind"], "Production")
        self.assertEqual(fields["result"], "failed")
        self.assertEqual((fields["apkName"], fields["apkSha256"], fields["tests"]), ("", "", ""))
        self.assertEqual(fields["deploy"], "Build Prod-Compatrelease: failure")
        self.assertEqual(fields["gaps"], "No build record: the build produced no APK")

    def test_regression_fields_link_the_matched_build_and_the_report(self):
        fields = contract.build_fields(load("payload_regression"))
        self.assertEqual(fields["kind"], "Regression")
        self.assertEqual(fields["result"], "failed")
        self.assertEqual(fields["runUrl"], run_url(1520))
        self.assertEqual(fields["commit"], "test 0123456")
        self.assertEqual((fields["apkName"], fields["apkSha256"]), ("wire-prod-compatrelease.apk", PROD_APK))
        self.assertEqual(fields["buildRunUrl"], run_url(1400))
        self.assertEqual(fields["deploy"], "")
        self.assertEqual(fields["tests"], "412 passed, 6 failed, 3 skipped, 0 missing; selector @RC; "
                                          "report https://example.com/allure/1520")
        self.assertEqual(fields["gaps"], "none")

    def test_deflake_never_claims_a_full_regression(self):
        fields = contract.build_fields(load("payload_deflake"))
        self.assertEqual(fields["kind"], "Deflake")
        self.assertEqual(fields["result"], "passed")
        self.assertEqual(fields["tests"], "6 passed, 0 failed, 0 skipped, 0 missing; selector @RC; "
                                          "report https://example.com/allure/1600; "
                                          "not a full regression; source run 1520")

    def test_unmatched_regression_is_partial_with_its_gap(self):
        payload = load("payload_unmatched")
        payload["evidence"]["qa"]["results"].update(passed=418, failed=0)
        fields = contract.build_fields(payload)
        self.assertEqual(fields["releaseVersion"], "unmatched")
        self.assertEqual((fields["result"], fields["buildRunUrl"]), ("partial", ""))
        self.assertEqual(fields["gaps"], "Tested APK matched no release build")

    def test_cancelled_runs_report_cancelled(self):
        payload = load("payload_regression")
        payload["evidence"].update(conclusion="cancelled", qa=None, gaps=["Evidence export unavailable"])
        fields = contract.build_fields(payload)
        self.assertEqual((fields["result"], fields["tests"], fields["apkName"]), ("cancelled", "", ""))
        self.assertEqual(fields["gaps"], "Evidence export unavailable")

    def test_timed_out_and_startup_failure_runs_report_failed(self):
        for conclusion in ["failure", "timed_out", "startup_failure"]:
            with self.subTest(conclusion=conclusion):
                payload = load("payload_beta")
                payload["evidence"]["conclusion"] = conclusion
                self.assertEqual(contract.build_fields(payload)["result"], "failed")

    def test_missing_or_empty_results_are_incomplete_never_a_pass(self):
        for change in [dict(passed=None, failed=None, skipped=None, missing=None, selected=None, total=None),
                       dict(passed=0, failed=0, skipped=0, missing=0, selected=0, total=0)]:
            with self.subTest(change=change):
                payload = load("payload_regression")
                payload["evidence"]["qa"]["results"].update(change)
                fields = contract.build_fields(payload)
                self.assertEqual(fields["result"], "partial")
                self.assertIn("Test results are missing or empty", fields["gaps"])
                self.assertIn("results unavailable", fields["tests"])

    def test_missing_selected_tests_make_the_result_partial(self):
        payload = load("payload_regression")
        payload["evidence"]["qa"]["results"].update(passed=415, failed=0, skipped=3, missing=3,
                                                    selected=421, total=421)
        fields = contract.build_fields(payload)
        self.assertEqual(fields["result"], "partial")
        self.assertIn("Selected tests are missing results", fields["gaps"])

    def test_a_successful_build_with_gaps_is_partial(self):
        payload = load("payload_beta")
        payload["evidence"]["gaps"] = ["Google Play outcome unknown"]
        self.assertEqual(contract.build_fields(payload)["result"], "partial")

    def test_fields_are_always_the_same_strings(self):
        for name in PAYLOADS:
            with self.subTest(name=name):
                fields = contract.build_fields(load(name))
                self.assertEqual(set(fields), FIELD_NAMES)
                self.assertTrue(all(isinstance(value, str) for value in fields.values()))

    def test_evidence_text_cannot_become_commands_or_markup(self):
        payload = load("payload_failed_build")
        payload["evidence"]["gaps"] = ["one\ntwo\rthree", "::error::boom", "<script>x</script>",
                                       "{{webhook.body}}", "x" * 2000]
        payload["evidence"]["jobs"][0]["name"] = "Build\n::set-output name=a::b"
        for name, value in contract.build_fields(payload).items():
            with self.subTest(field=name):
                self.assertNotRegex(value, r"[\r\n]")
                for banned in ["::", "<", ">", "{{"]:
                    self.assertNotIn(banned, value)
                self.assertLessEqual(len(value), 1000)
        self.assertTrue(contract.build_fields(payload)["gaps"].endswith("\u2026"))


HOSTILE_TEXT = [
    "[~accountId:5b10ac8d82e05b22cc7d4ef5]", "{panel}x{panel}", "{include:Page}", "[x|evil.example]",
    "[link](https://evil.example)", ":white_check_mark:", "&lt;b&gt;&#x41;", "x; Publish: success",
    "\uff3b~accountId\uff1ax\uff3d", "a\u202eb\u200bc",
]
ALLOWED_TEXT = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 ._()+,@/-")


class HostileTextTest(unittest.TestCase):
    def assertPlain(self, text):
        self.assertLessEqual(set(text), ALLOWED_TEXT, text)

    def test_file_names_cannot_carry_mentions_macros_or_links(self):
        for hostile in HOSTILE_TEXT:
            with self.subTest(hostile=hostile):
                payload = load("payload_beta")
                payload["evidence"]["build"]["apks"][0]["filename"] = hostile.replace("/", "") + ".apk"
                self.assertPlain(contract.build_fields(payload)["apkName"])

    def test_job_names_cannot_forge_a_deploy_segment(self):
        for hostile in HOSTILE_TEXT:
            with self.subTest(hostile=hostile):
                payload = load("payload_beta")
                payload["evidence"]["jobs"] = [{"name": hostile, "conclusion": "failure"}]
                deploy = contract.build_fields(payload)["deploy"]
                self.assertEqual(deploy.count(": failure"), 1)
                self.assertEqual(deploy.count(";"), 0)
                self.assertPlain(deploy.replace(": failure", ""))

    def test_selectors_and_report_links_stay_plain(self):
        for hostile in HOSTILE_TEXT:
            with self.subTest(hostile=hostile):
                payload = load("payload_regression")
                payload["evidence"]["qa"]["selector"] = hostile
                payload["evidence"]["qa"]["report_url"] = "https://example.com/" + hostile.replace(" ", "")
                tests = contract.build_fields(payload)["tests"]
                self.assertEqual(tests.count("; report "), 1)
                selector, report = tests.split("; selector ")[1].split("; report ")
                self.assertPlain(selector)
                for char in "[]{}|<>\\^\"@ ;":
                    self.assertNotIn(char, report)

    def test_gap_text_cannot_carry_mentions_macros_or_shortcodes(self):
        for hostile in HOSTILE_TEXT:
            with self.subTest(hostile=hostile):
                payload = load("payload_beta")
                payload["evidence"]["gaps"] = ["Outcome unknown " + hostile]
                gaps = contract.build_fields(payload)["gaps"]
                for banned in "[]{}|<>&~":
                    self.assertNotIn(banned, gaps)
                self.assertNotRegex(gaps, r":[^ ]")
                self.assertNotRegex(gaps, r"[^\x20-\x7e]")

    def test_the_collector_gap_texts_survive_unchanged(self):
        for text in ["No build record: the build produced no APK",
                     "Build did not run in this attempt; its evidence belongs to an earlier attempt",
                     "Release context belongs to an earlier attempt", "Tested APK matched no release build"]:
            with self.subTest(text=text):
                payload = load("payload_beta")
                payload["evidence"]["gaps"] = [text]
                self.assertEqual(contract.build_fields(payload)["gaps"], text)

    def test_a_file_name_cannot_pose_as_two_apks(self):
        payload = load("payload_beta")
        payload["evidence"]["build"]["apks"][0]["filename"] = "a.apk, forged.apk"
        fields = contract.build_fields(payload)
        self.assertEqual(fields["apkName"].count(", "), fields["apkSha256"].count(", "))

    def test_the_plain_forms_survive(self):
        payload = load("payload_regression")
        payload["evidence"]["qa"]["apk"]["filename"] = "wire-prod-compat_release-4.38.0+1.apk"
        payload["evidence"]["qa"]["selector"] = "@RC and @smoke"
        fields = contract.build_fields(payload)
        self.assertEqual(fields["apkName"], "wire-prod-compat_release-4.38.0+1.apk")
        self.assertIn("selector @RC and @smoke;", fields["tests"])
        self.assertIn("report https://example.com/allure/1520", fields["tests"])


class RunLinkTest(unittest.TestCase):
    def test_run_links_must_point_at_the_runs_they_name(self):
        for url in ["https://evil.example/login", "https://github.com/wireapp/wire-android/actions/runs/1",
                    "https://github.com/other/repo/actions/runs/1001/attempts/1",
                    "https://github.com/wireapp/wire-android/actions/runs/1001/attempts/1?x=1"]:
            with self.subTest(url=url):
                payload = load("payload_beta")
                payload["evidence"]["source"]["url"] = url
                with self.assertRaises(ValueError):
                    contract.validate_payload(payload)

    def test_the_origin_link_must_point_at_the_origin_run(self):
        payload = load("payload_regression")
        payload["evidence"]["origin"]["url"] = "https://evil.example/login"
        with self.assertRaises(ValueError):
            contract.validate_payload(payload)
        payload = load("payload_regression")
        payload["evidence"]["origin"]["url"] = run_url(9999)
        with self.assertRaises(ValueError):
            contract.validate_payload(payload)


class SharedHelpersTest(unittest.TestCase):
    def test_is_positive_accepts_only_positive_integers(self):
        self.assertTrue(contract.is_positive(1))
        for value in [0, -1, True, 1.0, "1", None]:
            with self.subTest(value=value):
                self.assertFalse(contract.is_positive(value))

    def test_parse_positive_reads_decimal_text(self):
        self.assertEqual(contract.parse_positive("42", "run_id"), 42)
        for text in ["", "0", "01", "-1", "1.5", " 1", None, 7]:
            with self.subTest(text=text), self.assertRaisesRegex(ValueError, "run_id must be a positive integer"):
                contract.parse_positive(text, "run_id")

    def test_dump_json_is_stable_text_with_a_final_newline(self):
        self.assertEqual(contract.dump_json({"b": 1, "a": [2]}), '{\n  "a": [\n    2\n  ],\n  "b": 1\n}\n')

    def test_write_json_writes_dump_json_to_a_file(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "out.json"
            contract.write_json(target, {"a": 1})
            self.assertEqual(target.read_text(), contract.dump_json({"a": 1}))
            with self.assertRaises(OSError):
                contract.write_json(Path(directory) / "missing" / "out.json", {})

    def test_read_json_applies_the_loader_policy_to_a_file(self):
        with tempfile.TemporaryDirectory() as directory:
            good, dup = Path(directory) / "good.json", Path(directory) / "dup.json"
            good.write_text('{"a": 1}')
            dup.write_text('{"a": 1, "a": 2}')
            self.assertEqual(contract.read_json(good), {"a": 1})
            with self.assertRaises(ValueError):
                contract.read_json(dup)
            with self.assertRaises(OSError):
                contract.read_json(Path(directory) / "missing.json")

    def test_the_shared_patterns_are_public(self):
        self.assertTrue(contract.REPOSITORY.fullmatch("wireapp/wire-android"))
        self.assertTrue(contract.SHA.fullmatch("a" * 40))
        self.assertFalse(contract.SHA.fullmatch("A" * 40))


class LoadJsonTest(unittest.TestCase):
    def test_duplicate_keys_are_rejected(self):
        with self.assertRaises(ValueError):
            contract.load_json('{"a": 1, "a": 2}')

    def test_oversized_documents_are_rejected(self):
        with self.assertRaises(ValueError):
            contract.load_json('{"a": "' + "x" * contract.JSON_LIMIT + '"}')

    def test_malformed_documents_raise_value_error(self):
        for text in ["", "{", "[1,", "nope"]:
            with self.subTest(text=text), self.assertRaises(ValueError):
                contract.load_json(text)

    def test_valid_documents_load(self):
        self.assertEqual(contract.load_json('{"a": [1, null]}'), {"a": [1, None]})


class ReviewFindingsTest(unittest.TestCase):
    def test_unicode_line_separators_and_c1_controls_are_removed_from_fields(self):
        payload = load("payload_failed_build")
        payload["evidence"]["gaps"] = ["a\u2028b\u2029c\x85d\x9fe"]
        gaps = contract.build_fields(payload)["gaps"]
        for char in ["\u2028", "\u2029", "\x85", "\x9f"]:
            self.assertNotIn(char, gaps)

    def test_backticks_cannot_close_a_markdown_fence(self):
        payload = load("payload_failed_build")
        payload["evidence"]["gaps"] = ["```\n# injected heading\n```"]
        for value in contract.build_fields(payload).values():
            self.assertNotIn("`", value)

    def test_a_regression_without_its_qa_record_is_never_a_pass(self):
        for name in ["payload_regression", "payload_deflake"]:
            with self.subTest(name=name):
                payload = load(name)
                payload["evidence"]["qa"] = None
                fields = contract.build_fields(payload)
                self.assertEqual(fields["result"], "partial")
                self.assertIn("Regression evidence is missing", fields["gaps"])
                self.assertEqual(fields["tests"], "results unavailable")

    def test_a_cancelled_regression_without_its_record_stays_cancelled(self):
        payload = load("payload_regression")
        payload["evidence"].update(conclusion="cancelled", qa=None)
        self.assertEqual(contract.build_fields(payload)["result"], "cancelled")

    def test_unhashable_record_kinds_raise_value_error(self):
        for kind in [[], {}, ["context"]]:
            with self.subTest(kind=kind), self.assertRaises(ValueError):
                contract.validate_record({"kind": kind})

    def test_deeply_nested_json_raises_value_error(self):
        with self.assertRaises(ValueError):
            contract.load_json("[" * 100000)

    def test_text_with_control_characters_is_rejected(self):
        record = load("regression")
        for value in ["a\nb", "a\u2028b", "a\x85b"]:
            with self.subTest(value=value):
                changed = copy.deepcopy(record)
                changed["suite"] = value
                with self.assertRaises(ValueError):
                    contract.validate_record(changed)
        changed = copy.deepcopy(record)
        changed["report_url"] = "https://example.com/a b"
        with self.assertRaises(ValueError):
            contract.validate_record(changed)
        build = load("build")
        build["apks"][0]["filename"] = "a\nb.apk"
        with self.assertRaises(ValueError):
            contract.validate_record(build)

    def test_embedded_records_must_share_the_source_workflow(self):
        payload = load("payload_beta")
        payload["evidence"]["build"]["workflow_path"] = ".github/workflows/other.yml"
        with self.assertRaises(ValueError):
            contract.validate_payload(payload)

    def test_the_release_version_must_agree_with_the_tags(self):
        payload = load("payload_beta")
        payload["release_version"] = "4.39.0"
        with self.assertRaises(ValueError):
            contract.validate_payload(payload)
        payload = load("payload_failed_build")
        payload["release_version"] = "4.39.0"
        with self.assertRaises(ValueError):
            contract.validate_payload(payload)


if __name__ == "__main__":
    unittest.main()
