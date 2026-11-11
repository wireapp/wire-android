#!/usr/bin/env python3
"""Unit tests for the release log collector and publisher, with mocked GitHub and transport."""

from __future__ import annotations

import copy
import http.client
import io
import json
import os
import socket
import sys
import tempfile
import unittest
import urllib.error
import urllib.request
import zipfile
from contextlib import redirect_stderr, redirect_stdout
from email.message import Message
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))

import release_log_contract as contract
import release_log_report as report

FIXTURES = Path(__file__).resolve().parent / "fixtures" / "release-log"
REPO = "wireapp/wire-android"
SHA = "7be41d0c3f2a9c1e5d6b8a4f0123456789abcdef"
OTHER_SHA = "1111111111111111111111111111111111111111"
BETA_PATH = ".github/workflows/build-beta.yml"
TAG = "v4.38.0-beta.2"
TAG_REFS = {TAG: {"object": {"type": "commit", "sha": SHA}}}


def fixture(name):
    return json.loads((FIXTURES / (name + ".json")).read_text())


def run_metadata(**changes):
    run = {"id": 1001, "run_attempt": 1, "path": BETA_PATH + "@refs/tags/" + TAG, "head_sha": SHA,
           "head_branch": TAG, "event": "push", "status": "completed", "conclusion": "success",
           "html_url": "https://github.com/wireapp/wire-android/actions/runs/1001",
           "repository": {"full_name": REPO}}
    run.update(changes)
    return run


def job(name, conclusion="success"):
    return {"name": name, "conclusion": conclusion, "status": "completed"}


def release_jobs(build="success", publish="success"):
    return [job("Validate beta tag"), job("build / Lint"), job("build / Unit Tests"),
            job("build / Build Beta-Release", build), job("build / deploy / deploy (s3)", build),
            job("Export release log context"), job("Publish beta prerelease", publish)]


def zip_of(entries):
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w", zipfile.ZIP_DEFLATED) as archive:
        for name, content in entries.items():
            archive.writestr(name, content)
    return buffer.getvalue()


def evidence_zip(record):
    return zip_of({"evidence.json": json.dumps(record)})


def context_record(**changes):
    record = fixture("context")
    record.update(changes)
    return record


def build_record(**changes):
    record = fixture("build")
    record.update(changes)
    return record


class FakeApi:
    def __init__(self, run=None, jobs=None, artifacts=None, refs=None, tags=None):
        self.run = run or run_metadata()
        self.jobs = release_jobs() if jobs is None else jobs
        self.artifacts = []
        self.zips = {}
        self.refs = refs or {}
        self.tags = tags or {}
        self.calls = []
        for name, content, *rest in artifacts or []:
            self.add_artifact(name, content, expired=bool(rest and rest[0]))

    def add_artifact(self, name, content, expired=False):
        identifier = len(self.artifacts) + 1
        url = "https://api.github.com/repos/%s/actions/artifacts/%d/zip" % (REPO, identifier)
        self.artifacts.append({"id": identifier, "name": name, "expired": expired, "archive_download_url": url})
        self.zips[url] = content

    def page(self, items, key, params):
        number, size = int(params["page"]), int(params["per_page"])
        return {"total_count": len(items), key: items[(number - 1) * size:number * size]}

    def get(self, path, params=None):
        self.calls.append(path)
        base = "/repos/%s/actions/runs/" % REPO
        if path.endswith("/jobs"):
            return self.page(self.jobs, "jobs", params)
        if path.endswith("/artifacts"):
            return self.page(self.artifacts, "artifacts", params)
        if path.startswith(base):
            return self.run
        if path.startswith("/repos/%s/git/ref/tags/" % REPO):
            tag = path.rsplit("/", 1)[-1]
            if tag in self.refs:
                return self.refs[tag]
        if path.startswith("/repos/%s/git/tags/" % REPO):
            tag_sha = path.rsplit("/", 1)[-1]
            if tag_sha in self.tags:
                return self.tags[tag_sha]
        raise report.ApiNotFound(path)

    def download(self, url):
        return self.zips[url]


def good_artifacts(attempt=1):
    return [("release-log-context-1001-%d" % attempt, evidence_zip(context_record(run_attempt=attempt))),
            ("release-log-build-Beta-1001-%d" % attempt, evidence_zip(build_record(run_attempt=attempt)))]


def collect(api, attempt=1):
    return report.collect_event(REPO, 1001, attempt, api=api)


class CollectBetaTest(unittest.TestCase):
    def test_a_successful_beta_reports_its_build_and_release_jobs(self):
        payload = collect(FakeApi(artifacts=good_artifacts()))
        contract.validate_payload(payload)
        self.assertEqual((payload["kind"], payload["release_version"], payload["event_id"]),
                         ("beta", "4.38.0", "beta:wireapp/wire-android:1001:1"))
        evidence = payload["evidence"]
        self.assertEqual(evidence["build"], build_record())
        self.assertEqual(evidence["gaps"], [])
        self.assertEqual([entry["name"] for entry in evidence["jobs"]], [
            "Validate beta tag", "Build Beta-Release", "deploy (s3)", "Publish beta prerelease"])
        self.assertEqual(evidence["source"], {
            "repository": REPO, "run_id": 1001, "run_attempt": 1, "workflow_path": BETA_PATH,
            "url": "https://github.com/wireapp/wire-android/actions/runs/1001/attempts/1",
            "tag": TAG, "sha": SHA})
        fields = contract.build_fields(payload)
        self.assertEqual((fields["kind"], fields["result"]), ("Beta 2", "passed"))

    def test_the_api_path_ref_suffix_is_stripped_before_the_workflow_check(self):
        for path in [BETA_PATH, BETA_PATH + "@refs/tags/" + TAG]:
            with self.subTest(path=path):
                payload = collect(FakeApi(run=run_metadata(path=path), artifacts=good_artifacts()))
                self.assertEqual(payload["evidence"]["source"]["workflow_path"], BETA_PATH)

    def test_jobs_and_artifacts_are_paginated(self):
        api = FakeApi(artifacts=[("noise-%d" % index, b"") for index in range(5)] + good_artifacts())
        api.jobs = [job("noise %d" % index) for index in range(5)] + release_jobs(publish="failure")
        with patch.object(report, "PER_PAGE", 2):
            payload = collect(api)
        self.assertEqual(payload["evidence"]["build"], build_record())
        self.assertEqual(payload["evidence"]["jobs"][-1], {"name": "Publish beta prerelease", "conclusion": "failure"})

    def test_artifacts_of_other_attempts_are_never_taken(self):
        api = FakeApi(artifacts=good_artifacts(2) + good_artifacts(1))
        api.run = run_metadata(run_attempt=1)
        self.assertEqual(collect(api)["evidence"]["build"]["run_attempt"], 1)
        api = FakeApi(run=run_metadata(run_attempt=2), artifacts=good_artifacts(2) + good_artifacts(1))
        self.assertEqual(collect(api, attempt=2)["evidence"]["build"]["run_attempt"], 2)

    def test_a_record_that_claims_another_attempt_is_not_accepted(self):
        artifacts = [("release-log-context-1001-2", evidence_zip(context_record(run_attempt=1))),
                     ("release-log-build-Beta-1001-2", evidence_zip(build_record(run_attempt=1)))]
        with self.assertRaises(report.CollectionError) as caught:
            collect(FakeApi(run=run_metadata(run_attempt=2), artifacts=artifacts, refs=TAG_REFS), attempt=2)
        gaps = caught.exception.partial["evidence"]["gaps"]
        self.assertEqual(gaps, ["Release context export is invalid", "Build evidence export is invalid"])

    def test_records_with_the_wrong_identity_are_rejected(self):
        mismatches = [dict(run_id=7), dict(repository="other/repo"), dict(workflow_path=".github/workflows/other.yml"),
                      dict(sha=OTHER_SHA)]
        for change in mismatches:
            with self.subTest(change=change):
                artifacts = [("release-log-context-1001-1", evidence_zip(context_record())),
                             ("release-log-build-Beta-1001-1", evidence_zip(build_record(**change)))]
                with self.assertRaises(report.CollectionError) as caught:
                    collect(FakeApi(artifacts=artifacts))
                self.assertIsNone(caught.exception.partial["evidence"]["build"])

    def test_context_and_build_records_must_agree(self):
        artifacts = [("release-log-context-1001-1", evidence_zip(context_record(tag="v4.39.0-beta.1"))),
                     ("release-log-build-Beta-1001-1", evidence_zip(build_record()))]
        with self.assertRaises(report.CollectionError):
            collect(FakeApi(artifacts=artifacts))

    def test_records_that_disagree_on_any_shared_key_are_rejected(self):
        for key, value in [("tag", "v4.38.0-beta.3"), ("sha", OTHER_SHA),
                           ("workflow_path", ".github/workflows/other.yml")]:
            with self.subTest(key=key):
                context, build = context_record(), build_record()
                build[key] = value
                with patch.object(report, "_check_identity"), \
                        self.assertRaisesRegex(report.CollectionError, "records disagree"):
                    collect(FakeApi(artifacts=[("release-log-context-1001-1", evidence_zip(context)),
                                               ("release-log-build-Beta-1001-1", evidence_zip(build))]))

    def test_a_build_record_of_another_flavor_is_not_accepted(self):
        record = build_record(flavor="Prod")
        artifacts = [good_artifacts()[0], ("release-log-build-Beta-1001-1", evidence_zip(record))]
        with self.assertRaises(report.CollectionError):
            collect(FakeApi(artifacts=artifacts))


class GapClassificationTest(unittest.TestCase):
    """Whether a gap fails the run depends on the reason, never on the wording of its message."""

    def test_context_gaps(self):
        E = report.Export
        cases = [
            (E.MISSING, 2, False, "Release context belongs to an earlier attempt", False),
            (E.MISSING, 2, True, "Release context export is missing", True),
            (E.MISSING, 1, False, "Release context export is missing", True),
            (E.EXPIRED, 2, False, "Release context export has expired", True),
            (E.INVALID, 2, False, "Release context export is invalid", True),
        ]
        for reason, attempt, job_ran, message, failure in cases:
            with self.subTest(reason=reason, attempt=attempt, job_ran=job_ran):
                self.assertEqual(report._context_gap(reason, attempt, job_ran), (message, failure))

    def test_build_gaps(self):
        E = report.Export
        failed, passed = [{"conclusion": "failure"}], [{"conclusion": "success"}]
        cases = [
            (E.MISSING, 2, False, [], "Build did not run in this attempt; its evidence belongs to an earlier attempt", False),
            (E.MISSING, 1, True, failed, "No build record: the build produced no APK", False),
            (E.MISSING, 1, True, passed, "Build evidence export is missing", True),
            (E.MISSING, 1, True, [], "Build evidence export is missing", True),
            (E.EXPIRED, 1, True, failed, "Build evidence export has expired", True),
            (E.INVALID, 2, False, [], "Build evidence export is invalid", True),
        ]
        for reason, attempt, ran, build_jobs, message, failure in cases:
            with self.subTest(reason=reason, attempt=attempt, ran=ran, jobs=build_jobs):
                self.assertEqual(report._build_gap(reason, attempt, ran, build_jobs), (message, failure))


class CollectFailuresTest(unittest.TestCase):
    def test_a_failed_build_without_an_apk_is_reported_normally(self):
        api = FakeApi(run=run_metadata(conclusion="failure"), jobs=release_jobs(build="failure", publish="skipped"),
                      artifacts=good_artifacts()[:1])
        payload = collect(api)
        contract.validate_payload(payload)
        self.assertIsNone(payload["evidence"]["build"])
        self.assertEqual(payload["evidence"]["gaps"], ["No build record: the build produced no APK"])
        self.assertEqual(contract.build_fields(payload)["result"], "failed")

    def test_a_successful_producer_without_exports_is_partial_and_fails_visibly(self):
        with self.assertRaises(report.CollectionError) as caught:
            collect(FakeApi(artifacts=good_artifacts()[:1]))
        partial = caught.exception.partial
        contract.validate_payload(partial)
        self.assertEqual(partial["evidence"]["gaps"], ["Build evidence export is missing"])
        self.assertEqual(contract.build_fields(partial)["result"], "partial")

    def test_an_expired_export_is_a_gap(self):
        artifacts = [good_artifacts()[0], ("release-log-build-Beta-1001-1", evidence_zip(build_record()), True)]
        with self.assertRaises(report.CollectionError) as caught:
            collect(FakeApi(artifacts=artifacts))
        self.assertIn("Build evidence export has expired", caught.exception.partial["evidence"]["gaps"])

    def test_duplicate_artifact_names_are_not_trusted(self):
        artifacts = good_artifacts() + [good_artifacts()[1]]
        with self.assertRaises(report.CollectionError):
            collect(FakeApi(artifacts=artifacts))

    def test_a_cancelled_run_reports_cancelled_with_its_gaps(self):
        payload = collect(FakeApi(run=run_metadata(conclusion="cancelled"), jobs=release_jobs(build="cancelled"),
                                  artifacts=[], refs=TAG_REFS))
        fields = contract.build_fields(payload)
        self.assertEqual(fields["result"], "cancelled")
        self.assertIn("Release context export is missing", fields["gaps"])

    def test_a_deployment_only_rerun_does_not_relabel_an_earlier_build(self):
        jobs = [job("build / deploy / deploy (google-play)"), job("Publish beta prerelease")]
        api = FakeApi(run=run_metadata(run_attempt=2), jobs=jobs, artifacts=good_artifacts(1), refs=TAG_REFS)
        payload = collect(api, attempt=2)
        self.assertEqual((payload["event_id"], payload["evidence"]["source"]["run_attempt"]),
                         ("beta:wireapp/wire-android:1001:2", 2))
        self.assertIsNone(payload["evidence"]["build"])
        self.assertIn("Build did not run in this attempt; its evidence belongs to an earlier attempt",
                      payload["evidence"]["gaps"])

    def test_release_jobs_are_reported_by_their_own_results(self):
        api = FakeApi(run=run_metadata(conclusion="failure"), jobs=release_jobs(publish="failure"),
                      artifacts=good_artifacts())
        jobs = collect(api)["evidence"]["jobs"]
        self.assertEqual({entry["name"]: entry["conclusion"] for entry in jobs}["Publish beta prerelease"], "failure")

    def test_unknown_job_names_produce_no_jobs_and_no_guess(self):
        api = FakeApi(jobs=[job("something else")], artifacts=good_artifacts())
        self.assertEqual(collect(api)["evidence"]["jobs"], [])


PROD_PATH = ".github/workflows/build-production.yml"
PROD_TAG = "v4.38.0"
PROD_REFS = {PROD_TAG: {"object": {"type": "commit", "sha": SHA}}}
PROD_GROUP = "deploy (Prod, Compatrelease, public, both, true, s3, google-play, com.wire, production, 0.1, inPr..."
FDROID_GROUP = "deploy (Fdroid, Compatrelease, public, apk, false, s3)"
GITHUB_RELEASE = "deploy (github-release, app/build/outputs/version.txt)"


def production_run(**changes):
    values = {"path": PROD_PATH, "head_branch": PROD_TAG, "event": "release"}
    values.update(changes)
    return run_metadata(**values)


def production_jobs(prod="success", store="success", fdroid="success", validate="success"):
    pipeline = "build-production / "
    return [
        job("validate-release", validate),
        job(pipeline + "Workflow security analysis"), job(pipeline + "Upload event file"),
        job("build-fdroid / Upload event file"), job("build-fdroid / Lint"),
        job(pipeline + "Lint"), job(pipeline + "Unit Tests / coverage"),
        job("build-fdroid / Build Fdroid-Compatrelease", fdroid),
        job(pipeline + "Build Prod-Compatrelease", prod),
        job("Export release log context"),
        job(pipeline + PROD_GROUP + " / deploy (s3)", prod),
        job(pipeline + PROD_GROUP + " / " + GITHUB_RELEASE, prod),
        job(pipeline + PROD_GROUP + " / deploy (google-play, com.wire, production, 0.1, inProgress)", store),
        job("build-fdroid / " + FDROID_GROUP + " / deploy (s3)", fdroid),
        job(pipeline + PROD_GROUP + " / deployment-summary", prod),
        job("build-fdroid / " + FDROID_GROUP + " / deployment-summary", fdroid),
    ]


def production_artifacts(attempt=1):
    return [("release-log-context-1001-%d" % attempt,
             evidence_zip(context_record(run_attempt=attempt, workflow_path=PROD_PATH, tag=PROD_TAG,
                                         event_name="release"))),
            ("release-log-build-Prod-1001-%d" % attempt,
             evidence_zip(dict(fixture("build_prod"), run_id=1001, run_attempt=attempt)))]


class CollectProductionTest(unittest.TestCase):
    def test_a_production_run_reports_its_own_jobs_and_build(self):
        payload = collect(FakeApi(run=production_run(), jobs=production_jobs(), artifacts=production_artifacts()))
        contract.validate_payload(payload)
        self.assertEqual((payload["kind"], payload["release_version"], payload["event_id"]),
                         ("production", "4.38.0", "production:wireapp/wire-android:1001:1"))
        evidence = payload["evidence"]
        self.assertEqual(evidence["gaps"], [])
        self.assertEqual(evidence["build"]["flavor"], "Prod")
        self.assertEqual(evidence["source"]["workflow_path"], PROD_PATH)
        self.assertEqual([entry["name"] for entry in evidence["jobs"]], [
            "validate-release", "Build Prod-Compatrelease", "deploy (s3)", "deploy (github-release, app/build/outputs/version.txt)",
            "deploy (google-play, com.wire, production, 0.1, inProgress)"])
        fields = contract.build_fields(payload)
        self.assertEqual((fields["kind"], fields["result"]), ("Production", "passed"))

    def test_fdroid_jobs_and_the_aggregate_summary_are_not_evidence(self):
        names = [entry["name"] for entry in collect(
            FakeApi(run=production_run(), jobs=production_jobs(), artifacts=production_artifacts()))["evidence"]["jobs"]]
        self.assertEqual(names.count("deploy (s3)"), 1)
        self.assertNotIn("deployment-summary", names)
        self.assertFalse([name for name in names if "Fdroid" in name])

    def test_a_failed_fdroid_job_does_not_change_the_production_deployment_results(self):
        payload = collect(FakeApi(run=production_run(conclusion="failure"), jobs=production_jobs(fdroid="failure"),
                                  artifacts=production_artifacts()))
        results = {entry["name"]: entry["conclusion"] for entry in payload["evidence"]["jobs"]}
        self.assertEqual(set(results.values()), {"success"})

    def test_each_deployment_target_keeps_its_own_result(self):
        payload = collect(FakeApi(run=production_run(conclusion="failure"), jobs=production_jobs(store="failure"),
                                  artifacts=production_artifacts()))
        results = {entry["name"]: entry["conclusion"] for entry in payload["evidence"]["jobs"]}
        self.assertEqual(results["deploy (google-play, com.wire, production, 0.1, inProgress)"], "failure")
        self.assertEqual(results["deploy (s3)"], "success")
        self.assertIn("google-play", contract.build_fields(payload)["deploy"])
        self.assertIn("deploy (s3): success", contract.build_fields(payload)["deploy"])

    def test_the_version_file_target_name_keeps_its_path(self):
        payload = collect(FakeApi(run=production_run(), jobs=production_jobs(), artifacts=production_artifacts()))
        deploy = contract.build_fields(payload)["deploy"]
        self.assertIn("deploy (github-release, app/build/outputs/version.txt): success", deploy)

    def test_a_failed_validation_is_reported_with_the_original_tag(self):
        jobs = [job("validate-release", "failure"), job("build-production / Build Prod-Compatrelease", "skipped"),
                job("Export release log context")]
        api = FakeApi(run=production_run(conclusion="failure"), jobs=jobs, artifacts=production_artifacts()[:1])
        payload = collect(api)
        self.assertEqual(payload["evidence"]["source"]["tag"], PROD_TAG)
        self.assertEqual(payload["evidence"]["jobs"][0], {"name": "validate-release", "conclusion": "failure"})
        self.assertEqual(contract.build_fields(payload)["result"], "failed")

    def test_a_failed_production_build_exports_context_and_reports_failed(self):
        jobs = production_jobs(prod="failure")
        api = FakeApi(run=production_run(conclusion="failure"), jobs=jobs, artifacts=production_artifacts()[:1])
        payload = collect(api)
        self.assertIsNone(payload["evidence"]["build"])
        self.assertEqual(payload["evidence"]["gaps"], ["No build record: the build produced no APK"])
        self.assertEqual(payload["evidence"]["source"]["tag"], PROD_TAG)

    def test_the_fdroid_build_alone_does_not_explain_a_missing_prod_record(self):
        jobs = [job("build-production / Build Prod-Compatrelease", "success"),
                job("build-fdroid / Build Fdroid-Compatrelease", "failure")]
        with self.assertRaises(report.CollectionError) as caught:
            collect(FakeApi(run=production_run(), jobs=jobs, artifacts=production_artifacts()[:1]))
        self.assertEqual(caught.exception.partial["evidence"]["gaps"], ["Build evidence export is missing"])

    def test_a_deployment_only_rerun_reports_its_attempt_without_relabeling_the_build(self):
        jobs = [job("build-production / " + PROD_GROUP + " / deploy (google-play, com.wire, production, 0.1, inProgress)"),
                job("build-fdroid / " + FDROID_GROUP + " / deploy (s3)")]
        api = FakeApi(run=production_run(run_attempt=2), jobs=jobs, artifacts=production_artifacts(1),
                      refs=PROD_REFS)
        payload = collect(api, attempt=2)
        self.assertEqual((payload["event_id"], payload["evidence"]["source"]["run_attempt"]),
                         ("production:wireapp/wire-android:1001:2", 2))
        self.assertIsNone(payload["evidence"]["build"])
        self.assertEqual([entry["name"] for entry in payload["evidence"]["jobs"]],
                         ["deploy (google-play, com.wire, production, 0.1, inProgress)"])
        self.assertIn("Build did not run in this attempt; its evidence belongs to an earlier attempt",
                      payload["evidence"]["gaps"])

    def test_a_release_run_without_exports_takes_its_tag_from_the_verified_release_tag(self):
        api = FakeApi(run=production_run(conclusion="cancelled"), jobs=[job("validate-release", "cancelled")],
                      artifacts=[], refs=PROD_REFS)
        self.assertEqual(collect(api)["evidence"]["source"]["tag"], PROD_TAG)

    def test_a_release_run_cannot_claim_an_unverified_tag(self):
        api = FakeApi(run=production_run(conclusion="cancelled"), jobs=[], artifacts=[],
                      refs={PROD_TAG: {"object": {"type": "commit", "sha": OTHER_SHA}}})
        with self.assertRaises(report.CollectionError):
            collect(api)

    def test_other_workflows_run_by_a_release_stay_ineligible(self):
        with self.assertRaises(report.CollectionError):
            collect(FakeApi(run=production_run(path=".github/workflows/build-fdroid.yml")))

    def test_a_beta_record_cannot_stand_in_for_the_production_build(self):
        artifacts = [production_artifacts()[0], ("release-log-build-Prod-1001-1", evidence_zip(build_record()))]
        with self.assertRaises(report.CollectionError) as caught:
            collect(FakeApi(run=production_run(), jobs=production_jobs(), artifacts=artifacts))
        self.assertIn("Build evidence export is invalid", caught.exception.partial["evidence"]["gaps"])


class CollectProductionEligibilityTest(unittest.TestCase):
    def test_a_manual_run_from_a_branch_named_like_a_tag_publishes_nothing(self):
        tag = "v4.99.0"
        run = production_run(event="workflow_dispatch", head_branch=tag, path=PROD_PATH + "@refs/heads/" + tag)
        forged = [("release-log-context-1001-1", evidence_zip(context_record(
                      workflow_path=PROD_PATH, tag=tag, event_name="release"))),
                  production_artifacts()[1]]
        api = FakeApi(run=run, jobs=production_jobs(), artifacts=forged,
                      refs={tag: {"object": {"type": "commit", "sha": SHA}}})
        self.assertIsNone(collect(api))
        self.assertEqual([call for call in api.calls if "/artifacts" in call or "/git/" in call], [])

    def test_a_release_of_a_pre_release_tag_is_not_a_release_run(self):
        tag = "v4.33.0-rc.2"
        api = FakeApi(run=production_run(head_branch=tag), jobs=production_jobs(), artifacts=production_artifacts(),
                      refs={tag: {"object": {"type": "commit", "sha": SHA}}})
        self.assertIsNone(collect(api))
        self.assertEqual([call for call in api.calls if "/artifacts" in call or "/git/" in call], [])

    def test_a_release_run_whose_tag_moved_is_a_visible_error(self):
        api = FakeApi(run=production_run(conclusion="cancelled"), jobs=[], artifacts=[],
                      refs={PROD_TAG: {"object": {"type": "commit", "sha": OTHER_SHA}}})
        with self.assertRaisesRegex(report.CollectionError, "version cannot be established"):
            collect(api)

    def test_a_manual_run_is_skipped_even_when_the_branch_is_a_valid_tag_name_and_all_jobs_ran(self):
        run = production_run(event="workflow_dispatch", head_branch=PROD_TAG)
        self.assertIsNone(collect(FakeApi(run=run, jobs=production_jobs(), artifacts=production_artifacts())))

    def test_the_fdroid_deploy_group_inside_the_production_pipeline_is_not_evidence(self):
        pipeline = "build-production / "
        jobs = [job(pipeline + "Build Prod-Compatrelease"),
                job(pipeline + PROD_GROUP + " / deploy (s3)"),
                job(pipeline + FDROID_GROUP + " / deploy (s3)", "failure"),
                job(pipeline + FDROID_GROUP + " / deployment-summary", "failure")]
        payload = collect(FakeApi(run=production_run(), jobs=jobs, artifacts=production_artifacts()))
        self.assertEqual([(entry["name"], entry["conclusion"]) for entry in payload["evidence"]["jobs"]],
                         [("Build Prod-Compatrelease", "success"), ("deploy (s3)", "success")])


class CollectEligibilityTest(unittest.TestCase):
    def test_other_workflows_are_not_eligible(self):
        for path in [".github/workflows/build-develop-push.yml", ".github/workflows/qa-android-beta-tests.yml",
                     "scripts/build-beta.yml", "x" + BETA_PATH]:
            with self.subTest(path=path), self.assertRaises(report.CollectionError):
                collect(FakeApi(run=run_metadata(path=path)))

    def test_incomplete_or_mismatched_runs_are_rejected(self):
        for change in [dict(status="in_progress", conclusion=None), dict(id=7), dict(run_attempt=3),
                       dict(repository={"full_name": "other/repo"}), dict(conclusion="weird"),
                       dict(head_sha="abc")]:
            with self.subTest(change=change), self.assertRaises(report.CollectionError):
                collect(FakeApi(run=run_metadata(**change), artifacts=good_artifacts()))

    def test_bad_arguments_are_rejected(self):
        for arguments in [("not a repo", 1, 1), (REPO, 0, 1), (REPO, 1, 0), (REPO, True, 1), (REPO, "1", 1)]:
            with self.subTest(arguments=arguments), self.assertRaises(report.CollectionError):
                report.collect_event(*arguments, api=FakeApi())


class CollectReleaseRunTest(unittest.TestCase):
    def test_a_run_dispatched_from_develop_with_every_job_skipped_is_not_a_release_run(self):
        run = run_metadata(event="workflow_dispatch", head_branch="develop", conclusion="skipped")
        jobs = [job(name, "skipped") for name in ["Validate beta tag", "build / Build Beta-Release"]]
        self.assertIsNone(collect(FakeApi(run=run, jobs=jobs)))

    def test_events_outside_the_table_are_not_release_runs(self):
        for event in ["workflow_dispatch", "release", "pull_request", "schedule", None]:
            with self.subTest(event=event):
                self.assertIsNone(collect(FakeApi(run=run_metadata(event=event), artifacts=good_artifacts())))

    def test_head_branches_that_are_not_release_tags_are_not_release_runs(self):
        for branch in ["develop", "release/cycle-4.38", "v4.38", "v4.38.0-rc.1", None]:
            with self.subTest(branch=branch):
                self.assertIsNone(collect(FakeApi(run=run_metadata(head_branch=branch), artifacts=good_artifacts())))

    def test_a_tag_push_is_still_reported(self):
        self.assertEqual(collect(FakeApi(artifacts=good_artifacts()))["release_version"], "4.38.0")

    def test_a_moved_tag_is_a_visible_error(self):
        refs = {TAG: {"object": {"type": "commit", "sha": OTHER_SHA}}}
        with self.assertRaises(report.CollectionError):
            collect(FakeApi(jobs=release_jobs(build="failure"), run=run_metadata(conclusion="failure"), refs=refs))

    def test_a_deleted_tag_is_a_visible_error(self):
        with self.assertRaises(report.CollectionError):
            collect(FakeApi(jobs=release_jobs(build="failure"), run=run_metadata(conclusion="failure")))


class CollectForgeryTest(unittest.TestCase):
    def forged(self, tag):
        return [("release-log-context-1001-1", evidence_zip(context_record(tag=tag))),
                ("release-log-build-Beta-1001-1", evidence_zip(build_record(tag=tag)))]

    def test_a_tag_that_differs_from_the_run_tag_is_rejected(self):
        with self.assertRaises(report.CollectionError) as caught:
            collect(FakeApi(artifacts=self.forged("v9.9.9-beta.7"), refs=TAG_REFS))
        self.assertIsNone(caught.exception.partial)

    def test_a_branch_run_cannot_claim_a_release_tag(self):
        run = run_metadata(event="workflow_dispatch", head_branch="develop")
        self.assertIsNone(collect(FakeApi(run=run, artifacts=good_artifacts())))

    def test_a_dispatch_from_a_branch_named_like_a_tag_publishes_nothing(self):
        tag = "v4.99.0-beta.1"
        run = run_metadata(event="workflow_dispatch", head_branch=tag, path=BETA_PATH + "@refs/heads/" + tag)
        refs = {tag: {"object": {"type": "commit", "sha": SHA}}}
        api = FakeApi(run=run, artifacts=self.forged(tag), refs=refs)
        self.assertIsNone(collect(api))
        self.assertEqual([call for call in api.calls if "/artifacts" in call or "/git/" in call], [])

    def test_hostile_job_names_cannot_inject_markup_or_extra_segments(self):
        hostile = "Deploy x: success\n{{trigger.body}} <b>@here</b> [a](http://e) {code}; Deploy: success"
        api = FakeApi(artifacts=good_artifacts(), jobs=[job("build / " + hostile)])
        name = collect(api)["evidence"]["jobs"][0]["name"]
        for banned in ["\n", "{", "}", "<", ">", "@", ";"]:
            self.assertNotIn(banned, name)
        self.assertTrue(name.startswith("Deploy x: success"))

    def test_a_null_download_url_is_an_invalid_export_not_a_crash(self):
        api = FakeApi(artifacts=good_artifacts())
        api.artifacts[1]["archive_download_url"] = None
        with self.assertRaises(report.CollectionError) as caught:
            collect(api)
        self.assertIn("Build evidence export is invalid", caught.exception.partial["evidence"]["gaps"])


class CollectVersionTest(unittest.TestCase):
    def test_the_context_tag_decides_the_version(self):
        payload = collect(FakeApi(artifacts=good_artifacts()[:1], jobs=release_jobs(build="failure"),
                                  run=run_metadata(conclusion="failure")))
        self.assertEqual(payload["release_version"], "4.38.0")

    def test_without_context_a_verified_tag_ref_establishes_the_version(self):
        refs = {TAG: {"object": {"type": "commit", "sha": SHA}}}
        api = FakeApi(run=run_metadata(conclusion="failure"), jobs=release_jobs(build="failure"), refs=refs)
        payload = collect(api)
        self.assertEqual((payload["release_version"], payload["evidence"]["source"]["tag"]), ("4.38.0", TAG))
        self.assertIn("Release context export is missing", payload["evidence"]["gaps"])

    def test_an_annotated_tag_is_followed_to_its_commit(self):
        refs = {TAG: {"object": {"type": "tag", "sha": "a" * 40}}}
        tags = {"a" * 40: {"object": {"type": "commit", "sha": SHA}}}
        api = FakeApi(run=run_metadata(conclusion="failure"), jobs=release_jobs(build="failure"), refs=refs, tags=tags)
        self.assertEqual(collect(api)["release_version"], "4.38.0")

    def test_a_tag_that_does_not_point_at_the_run_commit_is_not_used(self):
        refs = {TAG: {"object": {"type": "commit", "sha": OTHER_SHA}}}
        api = FakeApi(run=run_metadata(conclusion="failure"), jobs=release_jobs(build="failure"), refs=refs)
        with self.assertRaises(report.CollectionError) as caught:
            collect(api)
        self.assertIsNone(caught.exception.partial)

    def test_a_shared_commit_never_selects_a_tag(self):
        refs = {"v4.38.0": {"object": {"type": "commit", "sha": SHA}}}
        api = FakeApi(run=run_metadata(head_branch="develop", event="workflow_dispatch", conclusion="failure"),
                      jobs=release_jobs(build="failure"), refs=refs)
        self.assertIsNone(collect(api))
        self.assertNotIn("v4.38.0", " ".join(api.calls))

    def test_a_missing_tag_ref_fails_without_a_guessed_version(self):
        api = FakeApi(run=run_metadata(conclusion="failure"), jobs=release_jobs(build="failure"))
        with self.assertRaises(report.CollectionError) as caught:
            collect(api)
        self.assertIsNone(caught.exception.partial)


class ArchiveSafetyTest(unittest.TestCase):
    def assertExportRejected(self, content):
        artifacts = [good_artifacts()[0], ("release-log-build-Beta-1001-1", content)]
        with self.assertRaises(report.CollectionError) as caught:
            collect(FakeApi(artifacts=artifacts))
        self.assertIsNone(caught.exception.partial["evidence"]["build"])
        self.assertIn("Build evidence export is invalid", caught.exception.partial["evidence"]["gaps"])

    def test_archives_with_extra_nested_or_unsafe_entries_are_rejected(self):
        record = json.dumps(build_record())
        for entries in [{"evidence.json": record, "extra.txt": "x"}, {"dir/evidence.json": record},
                        {"../evidence.json": record}, {"/evidence.json": record}, {"other.json": record}, {}]:
            with self.subTest(entries=list(entries)):
                self.assertExportRejected(zip_of(entries))

    def test_symlink_entries_are_rejected(self):
        buffer = io.BytesIO()
        with zipfile.ZipFile(buffer, "w") as archive:
            info = zipfile.ZipInfo("evidence.json")
            info.external_attr = 0o120777 << 16
            archive.writestr(info, "/etc/passwd")
        self.assertExportRejected(buffer.getvalue())

    def test_a_symlink_entry_with_valid_evidence_is_rejected(self):
        buffer = io.BytesIO()
        with zipfile.ZipFile(buffer, "w") as archive:
            info = zipfile.ZipInfo("evidence.json")
            info.external_attr = 0o120777 << 16
            archive.writestr(info, json.dumps(build_record()))
        self.assertExportRejected(buffer.getvalue())
        with self.assertRaises(ValueError):
            report._read_archive(buffer.getvalue())
        report._read_archive(evidence_zip(build_record()))

    def test_oversized_documents_and_archives_are_rejected(self):
        self.assertExportRejected(zip_of({"evidence.json": " " * (contract.JSON_LIMIT + 1)}))
        self.assertExportRejected(b"x" * (report.ARCHIVE_LIMIT + 1))

    def test_invalid_json_duplicate_keys_and_non_archives_are_rejected(self):
        self.assertExportRejected(b"not a zip")
        self.assertExportRejected(zip_of({"evidence.json": "{"}))
        self.assertExportRejected(zip_of({"evidence.json": '{"kind": "build", "kind": "build"}'}))
        self.assertExportRejected(zip_of({"evidence.json": json.dumps(dict(build_record(), issue_key="WPB-1"))}))

    def test_downloaded_archives_are_never_extracted_to_disk(self):
        with patch.object(zipfile.ZipFile, "extract", side_effect=AssertionError("extract")), \
                patch.object(zipfile.ZipFile, "extractall", side_effect=AssertionError("extractall")):
            collect(FakeApi(artifacts=good_artifacts()))


HOOK_URL = "https://automation.example.com/pro/hook/abc123secretpath"
HOOK_TOKEN = "token-secret-value-42"


class Response:
    def __init__(self, status=200):
        self.status = status

    def __enter__(self):
        return self

    def __exit__(self, *arguments):
        return False

    def read(self, size=-1):
        return b"response body with " + HOOK_TOKEN.encode()


class Transport:
    """Stands in for report._open and records every request."""

    def __init__(self, outcome=None):
        self.calls = []
        self.outcome = outcome or Response()

    def __call__(self, request, timeout):
        self.calls.append((request, timeout))
        if isinstance(self.outcome, Exception):
            raise self.outcome
        return self.outcome


def strict_open(request, timeout):
    """Applies the checks that http.client makes before it sends a request."""
    http.client.HTTPSConnection(request.host)
    for value in request.headers.values():
        value.encode("latin-1")
    raise OSError("no network in tests")


def http_error(code, location=None):
    headers = Message()
    if location:
        headers["Location"] = location
    return urllib.error.HTTPError(HOOK_URL, code, "failure " + HOOK_URL, headers, None)


def sample_fields():
    return contract.build_fields(fixture("payload_beta"))


def send(transport, fields=None, enabled=True, dry_run=False, url=HOOK_URL, token=HOOK_TOKEN):
    output = io.StringIO()
    with patch.object(report, "_open", transport), redirect_stdout(output), redirect_stderr(output):
        report.publish(sample_fields() if fields is None else fields, enabled, url, token, dry_run=dry_run)
    return output.getvalue()


class PublishTest(unittest.TestCase):
    def test_disabled_publication_sends_nothing_and_needs_no_secrets(self):
        transport = Transport()
        printed = send(transport, enabled=False, url="", token="")
        self.assertEqual(transport.calls, [])
        self.assertIn("disabled", printed)

    def test_dry_run_sends_nothing_even_when_enabled(self):
        transport = Transport()
        printed = send(transport, dry_run=True)
        self.assertEqual(transport.calls, [])
        self.assertIn("dry run", printed)

    def test_a_live_request_is_one_post_with_the_token_header_and_a_20_second_timeout(self):
        transport = Transport()
        printed = send(transport)
        self.assertEqual(len(transport.calls), 1)
        request, timeout = transport.calls[0]
        self.assertEqual((request.get_method(), request.full_url, timeout), ("POST", HOOK_URL, 20))
        self.assertEqual(request.get_header("X-automation-webhook-token"), HOOK_TOKEN)
        self.assertEqual(request.get_header("Content-type"), "application/json")
        self.assertEqual(json.loads(request.data.decode("utf-8")), sample_fields())
        self.assertIn("accepted for processing", printed)

    def test_success_statuses_are_any_2xx(self):
        for status in [200, 202, 204]:
            with self.subTest(status=status):
                self.assertIn("accepted for processing", send(Transport(Response(status))))

    def test_failures_are_not_retried_and_never_claim_acceptance(self):
        outcomes = [http_error(500), http_error(401), http_error(404), http_error(302, "https://elsewhere.example/x"),
                    Response(199), Response(301)]
        for outcome in outcomes:
            with self.subTest(outcome=outcome):
                transport = Transport(outcome)
                output = io.StringIO()
                with self.assertRaises(report.PublishError) as caught, redirect_stdout(output):
                    with patch.object(report, "_open", transport):
                        report.publish(sample_fields(), True, HOOK_URL, HOOK_TOKEN)
                self.assertEqual(len(transport.calls), 1)
                self.assertNotIn("accepted", output.getvalue())
                self.assertRegex(str(caught.exception), r"HTTP \d+")

    def test_network_errors_are_not_retried_and_leak_nothing(self):
        leaky = "boom " + HOOK_URL + " " + HOOK_TOKEN
        for outcome in [urllib.error.URLError(leaky), socket.timeout(leaky), OSError(leaky), TimeoutError(leaky)]:
            with self.subTest(outcome=type(outcome).__name__):
                transport = Transport(outcome)
                with self.assertRaises(report.PublishError) as caught:
                    with patch.object(report, "_open", transport):
                        report.publish(sample_fields(), True, HOOK_URL, HOOK_TOKEN)
                self.assertEqual(len(transport.calls), 1)
                for secret in [HOOK_URL, HOOK_TOKEN, "boom", "abc123secretpath"]:
                    self.assertNotIn(secret, str(caught.exception))

    def test_output_and_errors_never_contain_the_url_token_or_response_body(self):
        printed = send(Transport())
        for secret in [HOOK_URL, HOOK_TOKEN, "response body"]:
            self.assertNotIn(secret, printed)
        transport = Transport(http_error(500))
        output = io.StringIO()
        with redirect_stdout(output), redirect_stderr(output), patch.object(report, "_open", transport):
            with self.assertRaises(report.PublishError) as caught:
                report.publish(sample_fields(), True, HOOK_URL, HOOK_TOKEN)
        for secret in [HOOK_URL, HOOK_TOKEN, "abc123secretpath"]:
            self.assertNotIn(secret, output.getvalue() + str(caught.exception))

    def test_invalid_urls_and_tokens_fail_before_any_request(self):
        for url in ["http://automation.example.com/hook", "ftp://x/y", "https://", "https://user:pw@example.com/h",
                    "", "automation.example.com/hook", "https://exa mple.com/h"]:
            with self.subTest(url=url):
                transport = Transport()
                with self.assertRaises(ValueError):
                    send(transport, url=url)
                self.assertEqual(transport.calls, [])
        with self.assertRaises(ValueError):
            send(Transport(), token="")

    def test_malformed_fields_are_rejected_even_when_disabled(self):
        for fields in [{}, dict(sample_fields(), extra="x"), dict(sample_fields(), kind=5),
                       {key: value for key, value in sample_fields().items() if key != "gaps"}, "fields", []]:
            with self.subTest(fields=fields):
                transport = Transport()
                with self.assertRaises(ValueError):
                    send(transport, fields=fields, enabled=False)
                self.assertEqual(transport.calls, [])

    def test_request_failures_leak_no_fragment_of_the_url_or_token(self):
        cases = [("port", dict(url="https://automation.example.com:SECRETPORT/hook"), strict_open),
                 ("token", dict(token="tok\u20acSECRETTOKEN"), strict_open),
                 ("status line", {}, Transport(http.client.BadStatusLine("SECRETLINE"))),
                 ("value error", {}, Transport(ValueError("SECRETVALUE")))]
        for name, changes, transport in cases:
            with self.subTest(name):
                output = io.StringIO()
                with self.assertRaises((report.PublishError, ValueError)) as caught:
                    with redirect_stdout(output), redirect_stderr(output):
                        send(transport, **changes)
                text = output.getvalue() + str(caught.exception)
                self.assertNotIn("SECRET", text)
                self.assertNotIn("u20ac", text)
                self.assertNotIn("codec", text)
        with self.assertRaises(report.PublishError) as caught:
            send(Transport(http.client.BadStatusLine("SECRETLINE")))
        self.assertEqual(str(caught.exception), "The webhook request failed")

    def test_redirects_are_never_followed(self):
        self.assertIsNone(report._NoRedirect().redirect_request(None, None, 302, "Found", {}, "https://x"))


class GitHubApiTest(unittest.TestCase):
    TOKEN = "ghs_github_token_value"

    def test_downloads_do_not_send_the_token_to_the_redirect_target(self):
        calls = []

        def fake_open(request, timeout):
            calls.append(request)
            if len(calls) == 1:
                raise http_error(302, "https://blob.example.com/signed?sig=1")
            return Response()

        with patch.object(report, "_open", fake_open):
            data = report.GitHubApi(self.TOKEN).download(report.API + "/repos/o/r/actions/artifacts/1/zip")
        self.assertTrue(data.startswith(b"response body"))
        self.assertEqual(calls[0].get_header("Authorization"), "Bearer " + self.TOKEN)
        self.assertEqual(calls[1].full_url, "https://blob.example.com/signed?sig=1")
        self.assertIsNone(calls[1].get_header("Authorization"))

    def test_unsafe_or_missing_redirects_and_foreign_urls_are_refused(self):
        api = report.GitHubApi(self.TOKEN)
        for outcome in [http_error(302, "http://blob.example.com/x"), http_error(302), http_error(403)]:
            with self.subTest(outcome=outcome), patch.object(report, "_open", Transport(outcome)):
                with self.assertRaises(report.ApiError):
                    api.download(report.API + "/repos/o/r/actions/artifacts/1/zip")
        with patch.object(report, "_open", Transport()) as transport:
            with self.assertRaises(report.ApiError):
                api.download("https://evil.example.com/zip")
            self.assertEqual(transport.calls, [])

    def test_a_response_over_the_size_cap_is_refused_without_data(self):
        class Big(Response):
            def read(self, size=-1):
                return b" " * size

        with patch.object(report, "_open", Transport(Big())), self.assertRaisesRegex(report.ApiError, "too large"):
            report.GitHubApi(self.TOKEN).get("/repos/o/r")
        small = Response()
        small.read = lambda size=-1: b'{"ok": true}'
        with patch.object(report, "_open", Transport(small)):
            self.assertEqual(report.GitHubApi(self.TOKEN).get("/repos/o/r"), {"ok": True})

    def test_api_errors_are_classified_and_leak_no_credentials(self):
        api = report.GitHubApi(self.TOKEN)
        with patch.object(report, "_open", Transport(http_error(404))), self.assertRaises(report.ApiNotFound):
            api.get("/repos/o/r")
        for outcome in [http_error(500), urllib.error.URLError("fail " + self.TOKEN), OSError(self.TOKEN)]:
            with self.subTest(outcome=type(outcome).__name__), patch.object(report, "_open", Transport(outcome)):
                with self.assertRaises(report.ApiError) as caught:
                    api.get("/repos/o/r")
                self.assertNotIn(self.TOKEN, str(caught.exception))
                self.assertNotIn(HOOK_URL, str(caught.exception))

    def test_a_token_is_required(self):
        with patch.dict(os.environ, {}, clear=True), self.assertRaises(report.CollectionError):
            report.GitHubApi.from_env()


class CommandLineTest(unittest.TestCase):
    def run_main(self, argv, env=None, api=None, transport=None):
        output, errors = io.StringIO(), io.StringIO()
        with patch.dict(os.environ, env or {}, clear=True), redirect_stdout(output), redirect_stderr(errors), \
                patch.object(report.GitHubApi, "from_env", return_value=api), \
                patch.object(report, "_open", transport or Transport()):
            code = report.main(argv)
        return code, output.getvalue(), errors.getvalue()

    def test_collect_writes_the_payload(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "payload.json"
            code, _, _ = self.run_main(["collect", "--repository", REPO, "--run-id", "1001", "--run-attempt", "1",
                                        "--output", str(target)], api=FakeApi(artifacts=good_artifacts()))
            self.assertEqual(code, 0)
            contract.validate_payload(json.loads(target.read_text()))

    def test_collect_takes_the_repository_from_the_environment(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "payload.json"
            code, _, _ = self.run_main(["collect", "--run-id", "1001", "--run-attempt", "1", "--output", str(target)],
                                       env={"GITHUB_REPOSITORY": REPO}, api=FakeApi(artifacts=good_artifacts()))
            self.assertEqual(code, 0)

    def test_collect_writes_a_partial_payload_and_fails(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "payload.json"
            code, _, errors = self.run_main(["collect", "--repository", REPO, "--run-id", "1001", "--run-attempt", "1",
                                             "--output", str(target)], api=FakeApi(artifacts=good_artifacts()[:1]))
            self.assertEqual(code, 1)
            self.assertIn("incomplete", errors)
            self.assertIn("Build evidence export is missing", json.loads(target.read_text())["evidence"]["gaps"])

    def test_collect_fails_cleanly_without_a_version_and_writes_nothing(self):
        api = FakeApi(run=run_metadata(conclusion="failure"), jobs=release_jobs(build="failure"))
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "payload.json"
            code, _, errors = self.run_main(["collect", "--repository", REPO, "--run-id", "1001", "--run-attempt", "1",
                                             "--output", str(target)], api=api)
            self.assertEqual(code, 1)
            self.assertNotIn("Traceback", errors)
            self.assertFalse(target.exists())

    def test_collect_skips_a_run_that_is_not_a_release_run_and_exits_zero(self):
        run = run_metadata(event="workflow_dispatch", head_branch="develop", conclusion="skipped")
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "payload.json"
            code, printed, errors = self.run_main(["collect", "--repository", REPO, "--run-id", "1001",
                                                   "--run-attempt", "1", "--output", str(target)],
                                                  api=FakeApi(run=run, jobs=[job("Validate beta tag", "skipped")]))
            self.assertEqual(code, 0)
            self.assertFalse(target.exists())
        self.assertIn("Release log: run 1001 is not a release run, nothing to report", printed)
        self.assertEqual(errors, "")

    def test_bad_run_numbers_fail_without_a_traceback(self):
        for argv in [["--run-id", "abc", "--run-attempt", "1"], ["--run-id", "1001", "--run-attempt", "0"]]:
            with self.subTest(argv=argv):
                code, _, errors = self.run_main(["collect", "--repository", REPO, "--output", "x"] + argv,
                                                api=FakeApi())
                self.assertEqual(code, 1)
                self.assertNotIn("Traceback", errors)

    def payload_file(self, directory, payload=None):
        path = Path(directory) / "payload.json"
        path.write_text(json.dumps(payload or fixture("payload_beta")))
        return path

    def test_publication_is_off_unless_the_variable_is_exactly_true(self):
        for value in [None, "", "false", "TRUE", "True", "1", "yes", "true "]:
            with self.subTest(value=value), tempfile.TemporaryDirectory() as directory:
                env = {"RELEASE_LOG_WEBHOOK_URL": HOOK_URL, "RELEASE_LOG_WEBHOOK_TOKEN": HOOK_TOKEN}
                if value is not None:
                    env["RELEASE_LOG_PUBLICATION_ENABLED"] = value
                transport = Transport()
                code, printed, _ = self.run_main(["publish", "--payload", str(self.payload_file(directory))],
                                                 env=env, transport=transport)
                self.assertEqual((code, transport.calls), (0, []))
                self.assertIn("disabled", printed)

    def test_publish_sends_the_fields_when_enabled(self):
        env = {"RELEASE_LOG_PUBLICATION_ENABLED": "true", "RELEASE_LOG_WEBHOOK_URL": HOOK_URL,
               "RELEASE_LOG_WEBHOOK_TOKEN": HOOK_TOKEN}
        with tempfile.TemporaryDirectory() as directory:
            transport = Transport()
            code, printed, _ = self.run_main(["publish", "--payload", str(self.payload_file(directory))],
                                             env=env, transport=transport)
            self.assertEqual(code, 0)
            self.assertEqual(len(transport.calls), 1)
            self.assertEqual(json.loads(transport.calls[0][0].data.decode()), sample_fields())
            self.assertNotIn(HOOK_TOKEN, printed)

    def test_dry_run_and_missing_secrets(self):
        env = {"RELEASE_LOG_PUBLICATION_ENABLED": "true"}
        with tempfile.TemporaryDirectory() as directory:
            path = str(self.payload_file(directory))
            transport = Transport()
            code, printed, _ = self.run_main(["publish", "--payload", path, "--dry-run"], env=env, transport=transport)
            self.assertEqual((code, transport.calls), (0, []))
            code, _, errors = self.run_main(["publish", "--payload", path], env=env, transport=transport)
            self.assertEqual((code, transport.calls), (1, []))
            self.assertNotIn("Traceback", errors)

    def test_failed_delivery_exits_nonzero_with_a_sanitized_message(self):
        env = {"RELEASE_LOG_PUBLICATION_ENABLED": "true", "RELEASE_LOG_WEBHOOK_URL": HOOK_URL,
               "RELEASE_LOG_WEBHOOK_TOKEN": HOOK_TOKEN}
        with tempfile.TemporaryDirectory() as directory:
            code, printed, errors = self.run_main(["publish", "--payload", str(self.payload_file(directory))],
                                                  env=env, transport=Transport(http_error(500)))
        self.assertEqual(code, 1)
        for secret in [HOOK_URL, HOOK_TOKEN, "abc123secretpath"]:
            self.assertNotIn(secret, printed + errors)

    def test_a_malformed_url_port_or_token_fails_without_a_traceback_or_fragment(self):
        for name, change in [("port", {"RELEASE_LOG_WEBHOOK_URL": "https://example.com:SECRETPORT/h"}),
                             ("token", {"RELEASE_LOG_WEBHOOK_TOKEN": "tok\u20acSECRETTOKEN"})]:
            env = dict({"RELEASE_LOG_PUBLICATION_ENABLED": "true", "RELEASE_LOG_WEBHOOK_URL": HOOK_URL,
                        "RELEASE_LOG_WEBHOOK_TOKEN": HOOK_TOKEN}, **change)
            with self.subTest(name), tempfile.TemporaryDirectory() as directory:
                code, printed, errors = self.run_main(
                    ["publish", "--payload", str(self.payload_file(directory))], env=env, transport=strict_open)
            self.assertEqual(code, 1)
            self.assertNotIn("Traceback", errors)
            for fragment in ["SECRET", "u20ac", "codec"]:
                self.assertNotIn(fragment, printed + errors)

    def test_invalid_payloads_fail_cleanly(self):
        with tempfile.TemporaryDirectory() as directory:
            payload = fixture("payload_beta")
            payload["issue_key"] = "WPB-1"
            code, _, errors = self.run_main(["publish", "--payload", str(self.payload_file(directory, payload))])
            self.assertEqual(code, 1)
            self.assertNotIn("Traceback", errors)
            code, _, _ = self.run_main(["publish", "--payload", str(Path(directory) / "missing.json")])
            self.assertEqual(code, 1)

    def test_the_summary_holds_the_fields_in_one_fence_without_active_markup(self):
        payload = fixture("payload_failed_build")
        payload["evidence"]["gaps"] = ["```\n::error::x\n<script>alert(1)</script>\n```"]
        with tempfile.TemporaryDirectory() as directory:
            summary = Path(directory) / "summary.md"
            code, _, _ = self.run_main(["publish", "--payload", str(self.payload_file(directory, payload)),
                                        "--summary", str(summary)])
            text = summary.read_text()
        self.assertEqual(code, 0)
        self.assertEqual(text.count("````"), 2)
        self.assertNotIn("```\n", text.replace("````", ""))
        for banned in ["::", "<script", "</script"]:
            self.assertNotIn(banned, text)


if __name__ == "__main__":
    unittest.main()
