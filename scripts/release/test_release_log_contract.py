#!/usr/bin/env python3
"""Unit tests for the release log contract."""

from __future__ import annotations

import ast
import copy
import itertools
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

    def test_the_rerun_count_cannot_exceed_the_passed_count(self):
        self.assertRejected("regression", lambda r: r["results"].update(passed_on_rerun=r["results"]["passed"] + 1))
        self.assertRejected("regression", lambda r: r["results"].update(passed_on_rerun=9999))
        record = load("regression")
        record["results"]["passed_on_rerun"] = record["results"]["passed"]
        contract.validate_record(record)
        record["results"]["passed"] = None
        record["results"]["total"] = None
        contract.validate_record(record)

    def test_unavailable_counts_are_allowed_and_stay_null(self):
        record = load("regression")
        record["results"] = {key: None for key in record["results"]}
        contract.validate_record(record)


class SkipMarkerTest(unittest.TestCase):
    def test_the_marker_names_the_run_and_attempt_and_carries_no_evidence(self):
        self.assertEqual(contract.skip_marker("wireapp/wire-android", 1520, 2),
                         {"schema_version": 1, "kind": "skip", "repository": "wireapp/wire-android",
                          "run_id": 1520, "run_attempt": 2})


PAYLOADS = ["payload_beta", "payload_failed_build", "payload_regression", "payload_deflake", "payload_unmatched"]
FIELD_NAMES = {"releaseVersion", "kind", "result", "runUrl", "commit", "apkName", "apkSha256", "buildRunUrl",
               "deploy", "tests", "gaps", "eventId"}
BETA_APK = "5c2091ab" * 8
PROD_APK = "d7330f18" * 8


CODES = ["Evidence missing", "Tests incomplete", "Not attached to a build", "Deflake not verified",
         "Earlier attempt"]
DOWNGRADING = ["Evidence missing", "Tests incomplete", "Deflake not verified"]
PASSING = ["Not attached to a build", "Earlier attempt", None]


def gap(code, detail="Some detail"):
    return {"code": code, "detail": detail}


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
        self.assertRejected("payload_beta", lambda p: p["evidence"].update(gaps=["Evidence missing"]))

    def test_a_gap_has_exactly_a_code_from_the_closed_set_or_null_and_a_detail(self):
        for code in CODES + [None]:
            with self.subTest(code=code):
                payload = load("payload_beta")
                payload["evidence"]["gaps"] = [gap(code)]
                contract.validate_payload(payload)
        for bad in [gap("Something else"), gap("evidence missing"), gap(5), gap("Evidence missing", ""),
                    gap("Evidence missing", "  "), gap("Evidence missing", None), {"code": None},
                    {"detail": "x"}, dict(gap(None), extra=1)]:
            with self.subTest(bad=bad):
                self.assertRejected("payload_beta", lambda p: p["evidence"].update(gaps=[bad]))

    def test_the_closed_set_is_the_five_page_items(self):
        self.assertEqual(list(contract.GAP_CODES), CODES)

    def test_a_gap_helper_refuses_a_code_outside_the_set(self):
        self.assertEqual(contract.gap("Earlier attempt", "d"), {"code": "Earlier attempt", "detail": "d"})
        self.assertEqual(contract.gap(None, "d"), {"code": None, "detail": "d"})
        with self.assertRaises(ValueError):
            contract.gap("Anything else", "d")

    def test_the_origin_carries_the_build_commit(self):
        self.assertRejected("payload_regression", lambda p: p["evidence"]["origin"].pop("sha"))
        self.assertRejected("payload_regression", lambda p: p["evidence"]["origin"].update(sha="abc123"))
        self.assertRejected("payload_regression", lambda p: p["evidence"]["origin"].update(sha=None))


class BuildFieldsTest(unittest.TestCase):
    def test_beta_fields_state_that_regression_is_not_required(self):
        self.assertEqual(contract.build_fields(load("payload_beta")), {
            "releaseVersion": "4.38.0", "kind": "Beta 2", "result": "passed", "runUrl": run_url(1001),
            "commit": "7be41d0", "apkName": "wire-beta-release.apk", "apkSha256": BETA_APK,
            "buildRunUrl": "",
            "deploy": "Validate beta tag: success; Build Beta-Release: success; Publish beta prerelease: success",
            "tests": "Regression: not required", "gaps": "none",
            "eventId": "beta:wireapp/wire-android:1001:1"})

    def test_a_failed_build_without_an_apk_reports_its_gap(self):
        fields = contract.build_fields(load("payload_failed_build"))
        self.assertEqual(fields["kind"], "Production")
        self.assertEqual(fields["result"], "failed")
        self.assertEqual((fields["apkName"], fields["apkSha256"], fields["tests"]), ("", "", ""))
        self.assertEqual(fields["deploy"], "Build Prod-Compatrelease: failure")
        self.assertEqual(fields["gaps"], "none")

    def test_regression_fields_link_the_matched_build_and_the_report(self):
        fields = contract.build_fields(load("payload_regression"))
        self.assertEqual(fields["kind"], "Regression")
        self.assertEqual(fields["result"], "failed")
        self.assertEqual(fields["runUrl"], run_url(1520))
        self.assertEqual(fields["commit"], "9f3c2a1")
        self.assertEqual((fields["apkName"], fields["apkSha256"]), ("wire-prod-compatrelease.apk", PROD_APK))
        self.assertEqual(fields["buildRunUrl"], run_url(1400))
        self.assertEqual(fields["deploy"], "")
        self.assertEqual(fields["tests"], "412 passed, 6 failed, 3 skipped, 0 missing, 2 passed on rerun; "
                                          "selector @RC; report https://example.com/allure/1520")
        self.assertEqual(fields["gaps"], "none")

    def test_deflake_never_claims_a_full_regression(self):
        fields = contract.build_fields(load("payload_deflake"))
        self.assertEqual(fields["kind"], "Deflake")
        self.assertEqual(fields["result"], "passed")
        self.assertEqual(fields["tests"], "6 passed, 0 failed, 0 skipped, 0 missing; selector @RC; "
                                          "report https://example.com/allure/1600; "
                                          "not a full regression; source run 1520")

    def test_an_unmatched_passing_regression_stays_passed_with_the_not_attached_code(self):
        payload = load("payload_unmatched")
        payload["evidence"]["qa"]["results"].update(passed=418, failed=0)
        fields = contract.build_fields(payload)
        self.assertEqual(fields["releaseVersion"], "unmatched")
        self.assertEqual((fields["result"], fields["buildRunUrl"]), ("passed", ""))
        self.assertEqual(fields["gaps"], "Not attached to a build")

    def test_the_commit_of_a_regression_is_the_matched_build_commit_and_empty_when_unmatched(self):
        self.assertEqual(contract.build_fields(load("payload_deflake"))["commit"], "9f3c2a1")
        self.assertEqual(contract.build_fields(load("payload_unmatched"))["commit"], "")
        payload = load("payload_regression")
        payload["evidence"]["qa"] = None
        payload["evidence"]["origin"] = None
        self.assertEqual(contract.build_fields(payload)["commit"], "")

    def test_a_result_downgrades_only_for_the_codes_that_ask_for_attention(self):
        for code in DOWNGRADING:
            with self.subTest(code=code):
                payload = load("payload_beta")
                payload["evidence"]["gaps"] = [gap(code)]
                fields = contract.build_fields(payload)
                self.assertEqual((fields["result"], fields["gaps"]), ("partial", code))
        for code in PASSING:
            with self.subTest(code=code):
                payload = load("payload_beta")
                payload["evidence"]["gaps"] = [gap(code)]
                fields = contract.build_fields(payload)
                self.assertEqual(fields["result"], "passed")
                self.assertEqual(fields["gaps"], code or "none")

    def test_a_deploy_only_rerun_reads_by_its_conclusion_with_the_earlier_attempt_code(self):
        for conclusion, result in [("success", "passed"), ("failure", "failed"), ("cancelled", "cancelled")]:
            with self.subTest(conclusion=conclusion):
                payload = load("payload_beta")
                payload["evidence"].update(conclusion=conclusion, gaps=[gap("Earlier attempt")])
                fields = contract.build_fields(payload)
                self.assertEqual((fields["result"], fields["gaps"]), (result, "Earlier attempt"))

    def test_the_page_lists_each_code_once_in_first_seen_order(self):
        payload = load("payload_beta")
        payload["evidence"]["gaps"] = [gap("Earlier attempt"), gap(None, "note"), gap("Evidence missing"),
                                       gap("Earlier attempt", "again"), gap("Evidence missing", "again")]
        self.assertEqual(contract.build_fields(payload)["gaps"], "Earlier attempt; Evidence missing")

    def test_a_failed_test_outranks_every_gap(self):
        payload = load("payload_regression")
        payload["evidence"]["gaps"] = [gap("Evidence missing")]
        self.assertEqual(contract.build_fields(payload)["result"], "failed")

    def test_cancelled_runs_report_cancelled(self):
        payload = load("payload_regression")
        payload["evidence"].update(conclusion="cancelled", qa=None, gaps=[gap("Evidence missing")])
        fields = contract.build_fields(payload)
        self.assertEqual((fields["result"], fields["tests"], fields["apkName"]), ("cancelled", "", ""))
        self.assertEqual(fields["gaps"], "Evidence missing")

    def test_timed_out_and_startup_failure_runs_report_failed(self):
        for conclusion in ["failure", "timed_out", "startup_failure"]:
            with self.subTest(conclusion=conclusion):
                payload = load("payload_beta")
                payload["evidence"]["conclusion"] = conclusion
                self.assertEqual(contract.build_fields(payload)["result"], "failed")

    def test_missing_or_empty_results_are_incomplete_never_a_pass(self):
        for change in [dict(passed=None, failed=None, skipped=None, missing=None, selected=None, total=None),
                       dict(passed=0, failed=0, skipped=0, missing=0, selected=0, total=0, passed_on_rerun=0)]:
            with self.subTest(change=change):
                payload = load("payload_regression")
                payload["evidence"]["qa"]["results"].update(change)
                fields = contract.build_fields(payload)
                self.assertEqual(fields["result"], "partial")
                self.assertEqual(fields["gaps"], "Tests incomplete")
                self.assertIn("results unavailable", fields["tests"])

    def test_missing_selected_tests_make_the_result_partial(self):
        payload = load("payload_regression")
        payload["evidence"]["qa"]["results"].update(passed=415, failed=0, skipped=3, missing=3,
                                                    selected=421, total=421)
        fields = contract.build_fields(payload)
        self.assertEqual(fields["result"], "partial")
        self.assertEqual(fields["gaps"], "Tests incomplete")

    def test_a_successful_build_with_gaps_is_partial(self):
        payload = load("payload_beta")
        payload["evidence"]["gaps"] = [gap("Evidence missing", "Google Play outcome unknown")]
        self.assertEqual(contract.build_fields(payload)["result"], "partial")

    def test_fields_are_always_the_same_strings(self):
        for name in PAYLOADS:
            with self.subTest(name=name):
                fields = contract.build_fields(load(name))
                self.assertEqual(set(fields), FIELD_NAMES)
                self.assertTrue(all(isinstance(value, str) for value in fields.values()))

    def test_evidence_text_cannot_become_commands_or_markup(self):
        payload = load("payload_failed_build")
        payload["evidence"]["gaps"] = [gap("Evidence missing", text) for text in (
            "one\ntwo\rthree", "::error::boom", "<script>x</script>", "{{webhook.body}}", "x" * 2000)]
        payload["evidence"]["jobs"][0]["name"] = "Build\n::set-output name=a::b"
        for name, value in contract.build_fields(payload).items():
            with self.subTest(field=name):
                self.assertNotRegex(value, r"[\r\n]")
                for banned in ["::", "<", ">", "{{"]:
                    self.assertNotIn(banned, value)
                self.assertLessEqual(len(value), 1000)
        self.assertEqual(contract.build_fields(payload)["gaps"], "Evidence missing")


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
                payload["evidence"]["gaps"] = [gap("Evidence missing", "Outcome unknown " + hostile)]
                self.assertEqual(contract.build_fields(payload)["gaps"], "Evidence missing")
                lines = "\n".join(contract.detail_lines(payload))
                for banned in "[]{}|<>&~":
                    self.assertNotIn(banned, lines)
                self.assertNotRegex(lines, r":[^ \n]")
                self.assertNotRegex(lines, r"[^\x20-\x7e\n]")

    def test_detail_lines_list_each_gap_with_its_code_or_a_note_label(self):
        payload = load("payload_beta")
        payload["evidence"]["gaps"] = [gap("Evidence missing", "Build evidence export is missing"),
                                       gap(None, "No build record: the build produced no APK")]
        self.assertEqual(contract.detail_lines(payload), [
            "- Evidence missing: Build evidence export is missing",
            "- note: No build record: the build produced no APK"])
        self.assertEqual(contract.detail_lines(load("payload_beta")), [])

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


class RegressionCountsTest(unittest.TestCase):
    def fields(self, **results):
        payload = load("payload_regression")
        payload["evidence"]["qa"]["results"].update(results)
        return contract.build_fields(payload)

    def test_more_selected_than_counted_tests_is_a_validation_error(self):
        for name in ["regression", "regression_deflake"]:
            with self.subTest(name=name):
                record = load(name)
                record["results"].update(selected=1000, total=record["results"]["total"])
                with self.assertRaises(ValueError):
                    contract.validate_record(record)
        record = load("regression")
        record["results"].update(passed=1, failed=0, skipped=0, missing=0, selected=1000, total=1)
        with self.assertRaises(ValueError):
            contract.validate_record(record)

    def test_fewer_selected_than_counted_tests_stays_valid(self):
        record = load("regression")
        record["results"].update(selected=400)
        contract.validate_record(record)

    def test_a_run_that_executed_nothing_is_never_a_pass(self):
        fields = self.fields(passed=0, failed=0, skipped=421, missing=0, selected=421, total=421,
                           passed_on_rerun=0)
        self.assertEqual(fields["result"], "partial")
        self.assertEqual(fields["gaps"], "Tests incomplete")
        self.assertIn("0 passed, 0 failed, 421 skipped, 0 missing", fields["tests"])

    def test_a_run_with_only_missing_results_is_never_a_pass(self):
        fields = self.fields(passed=0, failed=0, skipped=0, missing=5, selected=5, total=5,
                           passed_on_rerun=0)
        self.assertEqual(fields["result"], "partial")
        self.assertEqual(fields["gaps"], "Tests incomplete")

    def test_a_flaky_pass_shows_the_rerun_count(self):
        self.assertIn("2 passed on rerun", self.fields()["tests"])
        self.assertNotIn("on rerun", self.fields(passed_on_rerun=0)["tests"])
        self.assertNotIn("on rerun", self.fields(passed_on_rerun=None)["tests"])

    def test_the_rerun_count_stays_inside_the_allowlist(self):
        self.assertLessEqual(set(self.fields(passed_on_rerun=7)["tests"]), ALLOWED_TEXT | {";", ":", "="})


class EventIdFieldTest(unittest.TestCase):
    def test_every_entry_carries_the_event_identity(self):
        for name in PAYLOADS:
            with self.subTest(name=name):
                payload = load(name)
                self.assertEqual(contract.build_fields(payload)["eventId"], payload["event_id"])

    def test_the_event_id_cannot_carry_markup(self):
        payload = load("payload_regression")
        value = contract.build_fields(payload)["eventId"]
        for banned in ["::", "<", ">", "{{", "`", "\n"]:
            self.assertNotIn(banned, value)


class HostileRegressionTextTest(unittest.TestCase):
    def test_apk_names_and_selectors_from_the_qa_record_cannot_forge_text(self):
        for hostile in HOSTILE_TEXT + ["x, y", "a::b", "`code`", "{{webhook.body}}"]:
            with self.subTest(hostile=hostile):
                payload = load("payload_regression")
                qa = payload["evidence"]["qa"]
                qa["apk"]["filename"] = hostile.replace("/", "") + ".apk"
                qa["old_apk"]["filename"] = hostile.replace("/", "") + ".apk"
                qa["selector"] = hostile
                qa["suite"] = hostile
                fields = contract.build_fields(payload)
                self.assertEqual(fields["apkName"].count(","), 0)
                self.assertLessEqual(set(fields["apkName"]), ALLOWED_TEXT)
                self.assertEqual(fields["tests"].count("selector "), 1)
                self.assertEqual(fields["tests"].count(";"), 2)
                self.assertEqual(fields["gaps"], "none")
                for value in fields.values():
                    for banned in ["::", "<", ">", "{{", "`"]:
                        self.assertNotIn(banned, value)


class ReviewFindingsTest(unittest.TestCase):
    def test_unicode_line_separators_and_c1_controls_are_removed_from_fields(self):
        payload = load("payload_failed_build")
        payload["evidence"]["gaps"] = [gap(None, "a\u2028b\u2029c\x85d\x9fe")]
        gaps = "".join(contract.detail_lines(payload))
        for char in ["\u2028", "\u2029", "\x85", "\x9f"]:
            self.assertNotIn(char, gaps)

    def test_backticks_cannot_close_a_markdown_fence(self):
        payload = load("payload_failed_build")
        payload["evidence"]["gaps"] = [gap(None, "```\n# injected heading\n```")]
        for value in list(contract.build_fields(payload).values()) + contract.detail_lines(payload):
            self.assertNotIn("`", value)
            self.assertNotIn("\n", value)

    def test_a_regression_without_its_qa_record_is_never_a_pass(self):
        for name in ["payload_regression", "payload_deflake"]:
            with self.subTest(name=name):
                payload = load(name)
                payload["evidence"]["qa"] = None
                fields = contract.build_fields(payload)
                self.assertEqual(fields["result"], "partial")
                self.assertEqual(fields["gaps"], "Evidence missing")
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

class ClosedSetTest(unittest.TestCase):
    """The page can show only the five codes, whoever produces a gap."""

    CONSTANTS = {name: value for name, value in vars(contract).items()
                 if name.isupper() and isinstance(value, str) and value in contract.GAP_CODES}

    def test_every_producer_names_a_code_from_the_closed_set(self):
        sources = [Path(contract.__file__), Path(__file__).resolve().parent / "release_log_report.py"]
        calls = 0
        for path in sources:
            for node in ast.walk(ast.parse(path.read_text())):
                if not isinstance(node, ast.Call):
                    continue
                name = node.func.attr if isinstance(node.func, ast.Attribute) else getattr(node.func, "id", None)
                if name != "gap" or not node.args:
                    continue
                calls += 1
                code = node.args[0]
                label = "%s:%d" % (path.name, node.lineno)
                if isinstance(code, ast.Constant):
                    self.assertIsNone(code.value, label)
                elif isinstance(code, ast.Attribute) and isinstance(code.value, ast.Name) and code.value.id == "contract":
                    self.assertIn(code.attr, self.CONSTANTS, label)
                elif isinstance(code, ast.Name):
                    self.assertIn(code.id, self.CONSTANTS, label)
                else:
                    self.fail("gap code is not a constant of the closed set at " + label)
        self.assertGreater(calls, 10)

    def test_the_page_never_shows_anything_but_none_or_codes_of_the_set(self):
        allowed = set(contract.GAP_CODES)
        results = [dict(passed=None, failed=None, skipped=None, missing=None, selected=None, total=None, passed_on_rerun=None),
                   dict(passed=0, failed=0, skipped=5, missing=0, selected=5, total=5, passed_on_rerun=0),
                   dict(passed=4, failed=0, skipped=0, missing=1, selected=5, total=5, passed_on_rerun=0),
                   dict(passed=5, failed=0, skipped=0, missing=0, selected=5, total=5, passed_on_rerun=0)]
        for name, change, shown, conclusion, with_qa in itertools.product(
                PAYLOADS, results, [[]] + [[gap(code)] for code in CODES + [None]],
                ["success", "failure", "cancelled"], [True, False]):
            payload = copy.deepcopy(load(name))
            evidence = payload["evidence"]
            evidence["gaps"], evidence["conclusion"] = shown, conclusion
            if evidence["qa"] is not None:
                evidence["qa"]["results"].update(change)
                evidence["qa"] = evidence["qa"] if with_qa else None
            page = contract.build_fields(payload)["gaps"]
            self.assertTrue(page == "none" or set(page.split("; ")) <= allowed, page)


if __name__ == "__main__":
    unittest.main()
