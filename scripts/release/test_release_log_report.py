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


PROD_TESTS_PATH = ".github/workflows/qa-android-prod-tests.yml"
PROD_BUILD_PATH = ".github/workflows/build-production.yml"
TEST_SHA = "0123456789abcdef0123456789abcdef01234567"
APK_NAME = "wire-prod-compatrelease.apk"
APK_SHA = "d7330f18" * 8
PAGES = "https://wireapp.github.io/wire-android/qa-prod-tests/"
FINGERPRINT = "11aa22bb" * 8
SECOND_FINGERPRINT = "33cc44dd" * 8
QA_NOW = "2026-11-09T10:00:00Z"
UNMATCHED_TEXT = "Tested APK matched no release build"
NOT_ATTACHED = "this regression is not attached to a build page"


def qa_record(run_id=1520, attempt=1, **changes):
    record = fixture("regression")
    record.update(run_id=run_id, run_attempt=attempt, report_url=PAGES + "run-%d/" % run_id)
    record.update(changes)
    return record


def deflake_record(run_id=1600, attempt=1, source=(1520, 1), **changes):
    record = fixture("regression_deflake")
    record.update(run_id=run_id, run_attempt=attempt, source_qa_run_id=source[0], source_qa_run_attempt=source[1],
                  report_url=PAGES + "run-%d/" % run_id, source_deflake_bundle_sha256=FINGERPRINT)
    record.update(changes)
    return record


def qa_run(run_id=1520, attempt=1, **changes):
    run = {"id": run_id, "run_attempt": attempt, "path": PROD_TESTS_PATH + "@refs/heads/develop", "head_sha": SHA,
           "head_branch": "develop", "event": "workflow_dispatch", "status": "completed", "conclusion": "success",
           "created_at": QA_NOW, "repository": {"full_name": REPO}}
    run.update(changes)
    return run


def prod_run(run_id=1400, attempt=1, created="2026-10-20T10:00:00Z", **changes):
    run = {"id": run_id, "run_attempt": attempt, "path": PROD_BUILD_PATH + "@refs/tags/v4.38.0", "head_sha": SHA,
           "head_branch": "v4.38.0", "event": "release", "status": "completed", "conclusion": "success",
           "created_at": created, "repository": {"full_name": REPO}}
    run.update(changes)
    return run


def prod_build(run_id=1400, attempt=1, **changes):
    record = fixture("build_prod")
    record.update(run_id=run_id, run_attempt=attempt)
    record.update(changes)
    return record


def qa_jobs(conclusion="success"):
    return [job("run-tests / Validate + resolve run inputs"), job("run-tests / Run QA Android Prod Regression RC",
                                                                  conclusion)]


class RegressionApi:
    """A GitHub stand-in for regression runs and the production runs they are matched against."""

    def __init__(self):
        self.runs, self.jobs, self.artifacts, self.zips = {}, {}, {}, {}
        self.production, self.calls, self.listing_params = [], [], []
        self.listing_error, self.jobs_error = None, None
        self.default_branch = "develop"

    def add_run(self, run, jobs=None):
        self.runs[(run["id"], run["run_attempt"])] = run
        self.jobs[(run["id"], run["run_attempt"])] = qa_jobs() if jobs is None else jobs

    def add_artifact(self, run_id, name, content, expired=False):
        listed = self.artifacts.setdefault(run_id, [])
        url = "https://api.github.com/repos/%s/actions/artifacts/%d-%d/zip" % (REPO, run_id, len(listed))
        listed.append({"id": len(listed), "name": name, "expired": expired, "archive_download_url": url})
        self.zips[url] = evidence_zip(content) if isinstance(content, dict) else content

    def add_qa(self, record, run=None, jobs=None):
        self.add_run(run or qa_run(record["run_id"], record["run_attempt"]), jobs)
        self.add_artifact(record["run_id"], "release-log-qa-%d-%d" % (record["run_id"], record["run_attempt"]),
                          record)

    def add_production(self, record=None, run=None, expired=False):
        run = run or prod_run()
        record = record or prod_build(run["id"], run["run_attempt"])
        self.production.append(run)
        self.add_run(run, [])
        self.add_artifact(run["id"], "release-log-build-Prod-%d-%d" % (run["id"], run["run_attempt"]), record,
                          expired=expired)

    def page(self, items, key, params):
        number, size = int(params["page"]), int(params["per_page"])
        return {"total_count": len(items), key: items[(number - 1) * size:number * size]}

    def get(self, path, params=None):
        self.calls.append(path)
        base = "/repos/%s/actions/" % REPO
        if path == "/repos/" + REPO:
            return {"default_branch": self.default_branch}
        if path == base + "workflows/build-production.yml/runs":
            self.listing_params.append(dict(params))
            if self.listing_error:
                raise self.listing_error
            return self.page(self.production, "workflow_runs", params)
        parts = path[len(base):].split("/") if path.startswith(base) else []
        if len(parts) >= 2 and parts[0] == "runs" and parts[1].isdigit():
            run_id = int(parts[1])
            if len(parts) == 3 and parts[2] == "artifacts":
                return self.page(self.artifacts.get(run_id, []), "artifacts", params)
            if len(parts) >= 4 and parts[2] == "attempts":
                key = (run_id, int(parts[3]))
                if key in self.runs:
                    if len(parts) == 5 and parts[4] == "jobs":
                        if self.jobs_error and run_id == 1400:
                            raise self.jobs_error
                        return self.page(self.jobs[key], "jobs", params)
                    if len(parts) == 4:
                        return self.runs[key]
        raise report.ApiNotFound(path)

    def download(self, url):
        return self.zips[url]


def reg_world(record=None, jobs=None, production=True, **run_changes):
    api = RegressionApi()
    record = record or qa_record()
    api.add_qa(record, qa_run(record["run_id"], record["run_attempt"], **run_changes), jobs)
    if production:
        api.add_production()
    return api


def collect_regression(api, run_id=1520, attempt=1):
    return report.collect_event(REPO, run_id, attempt, api=api)


class CollectRegressionTest(unittest.TestCase):
    def test_a_matched_regression_reports_its_record_origin_and_version(self):
        payload = collect_regression(reg_world())
        contract.validate_payload(payload)
        self.assertEqual((payload["kind"], payload["release_version"], payload["event_id"]),
                         ("regression", "4.38.0", "regression:wireapp/wire-android:1520:1"))
        evidence = payload["evidence"]
        self.assertEqual(evidence["qa"], qa_record())
        self.assertEqual(evidence["origin"], {"run_id": 1400, "run_attempt": 1,
                                              "url": "https://github.com/wireapp/wire-android/actions/runs/1400/attempts/1"})
        self.assertEqual((evidence["gaps"], evidence["build"]), ([], None))
        self.assertEqual(evidence["source"]["workflow_path"], PROD_TESTS_PATH)
        self.assertEqual((evidence["source"]["tag"], evidence["source"]["sha"]), (None, SHA))
        self.assertEqual([entry["name"] for entry in evidence["jobs"]],
                         ["Validate + resolve run inputs", "Run QA Android Prod Regression RC"])
        fields = contract.build_fields(payload)
        self.assertEqual((fields["kind"], fields["result"], fields["releaseVersion"]), ("Regression", "failed", "4.38.0"))
        self.assertEqual(fields["buildRunUrl"], "https://github.com/wireapp/wire-android/actions/runs/1400/attempts/1")
        self.assertEqual(fields["eventId"], "regression:wireapp/wire-android:1520:1")

    def test_the_tests_commit_may_differ_from_the_run_commit(self):
        record = qa_record(test_source_sha=OTHER_SHA)
        payload = collect_regression(reg_world(record))
        self.assertEqual(payload["evidence"]["qa"]["test_source_sha"], OTHER_SHA)
        self.assertEqual(payload["evidence"]["source"]["sha"], SHA)
        self.assertEqual(payload["release_version"], "4.38.0")

    def test_the_tests_commit_never_identifies_the_apk(self):
        api = reg_world(qa_record(test_source_sha=SHA), production=False)
        api.add_production(prod_build(apks=[{"filename": "other.apk", "sha256": "ab" * 32}]))
        payload = collect_regression(api)
        self.assertIsNone(payload["evidence"]["origin"])
        self.assertIn(UNMATCHED_TEXT, " ".join(payload["evidence"]["gaps"]))

    def test_only_manual_production_regression_runs_are_eligible(self):
        for change in [dict(path=".github/workflows/qa-android-regression-tests.yml@refs/heads/develop"),
                       dict(path=".github/workflows/qa-android-beta-tests.yml@refs/heads/develop")]:
            with self.subTest(change=change), self.assertRaises(report.CollectionError):
                collect_regression(reg_world(**change))

    def test_a_scheduled_or_non_dispatch_run_is_a_clean_skip(self):
        for event in ("schedule", "workflow_call", "push", "release"):
            with self.subTest(event=event):
                api = reg_world(event=event)
                self.assertIsNone(collect_regression(api))
                self.assertEqual([c for c in api.calls if "/artifacts" in c or "/git/" in c], [])

    def test_a_dispatch_from_a_feature_branch_cannot_forge_evidence(self):
        for branch in ("attacker/forge", "release", "releases/4.38", "feature/release/x", "develop-2", None):
            with self.subTest(branch=branch):
                api = reg_world(head_branch=branch)
                self.assertIsNone(collect_regression(api))
                self.assertEqual([c for c in api.calls if "/artifacts" in c or "/git/" in c or "/jobs" in c], [])
                self.assertEqual(api.listing_params, [])

    def test_a_dispatch_from_the_default_branch_or_a_release_branch_is_reported(self):
        for branch, default in [("develop", "develop"), ("release/cycle-4.38.0", "develop"),
                                ("main", "main")]:
            with self.subTest(branch=branch, default=default):
                api = reg_world(head_branch=branch)
                api.default_branch = default
                self.assertEqual(collect_regression(api)["kind"], "regression")

    def test_the_default_branch_is_read_once_and_only_for_regressions(self):
        api = reg_world()
        collect_regression(api)
        self.assertEqual(api.calls.count("/repos/" + REPO), 1)
        beta = FakeApi(artifacts=good_artifacts(), refs=TAG_REFS)
        collect(beta)
        self.assertNotIn("/repos/" + REPO, beta.calls)

    def test_a_regression_never_matches_a_build_of_an_ineligible_run(self):
        for change in [dict(event="workflow_dispatch"), dict(event="push"), dict(head_branch="attacker/forge"),
                       dict(head_branch="v4.38.0-beta.2")]:
            with self.subTest(change=change):
                api = reg_world(production=False)
                api.add_production(run=prod_run(**change))
                payload = collect_regression(api)
                self.assertIsNone(payload["evidence"]["origin"])
                self.assertIn(UNMATCHED_TEXT, " ".join(payload["evidence"]["gaps"]))

    def test_an_unfinished_or_cancelled_run_follows_the_run_rules(self):
        with self.assertRaises(report.CollectionError):
            collect_regression(reg_world(status="in_progress", conclusion=None))

    def test_records_are_checked_against_the_run_metadata(self):
        mismatches = [dict(run_id=7), dict(run_attempt=2), dict(repository="other/repo"),
                      dict(workflow_path=".github/workflows/qa-android-regression-tests.yml"),
                      dict(flavor="fdroid")]
        for change in mismatches:
            with self.subTest(change=change):
                api = reg_world(production=False)
                api.artifacts[1520].clear()
                api.add_artifact(1520, "release-log-qa-1520-1", dict(qa_record(), **change))
                api.add_production()
                with self.assertRaises(report.CollectionError) as caught:
                    collect_regression(api)
                partial = caught.exception.partial
                self.assertIsNone(partial["evidence"]["qa"])
                self.assertIn("Regression evidence export is invalid", partial["evidence"]["gaps"])
                self.assertEqual(partial["release_version"], "unmatched")

    def test_a_record_of_another_run_with_the_same_apk_names_is_not_accepted(self):
        api = reg_world(production=False)
        api.artifacts[1520].clear()
        api.add_artifact(1520, "release-log-qa-1520-1", qa_record(run_id=1999))
        api.add_production()
        with self.assertRaises(report.CollectionError) as caught:
            collect_regression(api)
        self.assertIsNone(caught.exception.partial["evidence"]["origin"])
        self.assertNotIn("1400", contract.build_fields(caught.exception.partial)["buildRunUrl"])

    def test_artifacts_of_earlier_attempts_are_never_taken_for_this_attempt(self):
        api = RegressionApi()
        api.add_qa(qa_record(attempt=1), qa_run(attempt=1))
        api.add_qa(qa_record(attempt=2, results=dict(qa_record()["results"], failed=0, passed=418, passed_on_rerun=0)),
                   qa_run(attempt=2))
        api.add_production()
        first = collect_regression(api, attempt=1)
        second = collect_regression(api, attempt=2)
        self.assertEqual((first["evidence"]["qa"]["run_attempt"], second["evidence"]["qa"]["run_attempt"]), (1, 2))
        self.assertEqual(second["evidence"]["qa"]["results"]["failed"], 0)
        self.assertEqual(second["event_id"], "regression:wireapp/wire-android:1520:2")

    def test_a_record_that_claims_another_attempt_is_not_accepted(self):
        api = RegressionApi()
        api.add_run(qa_run(attempt=2))
        api.add_artifact(1520, "release-log-qa-1520-2", qa_record(attempt=1))
        api.add_production()
        with self.assertRaises(report.CollectionError) as caught:
            collect_regression(api, attempt=2)
        self.assertIn("Regression evidence export is invalid", caught.exception.partial["evidence"]["gaps"])

    def test_a_rerun_that_skipped_the_qa_job_points_to_the_earlier_attempt(self):
        api = RegressionApi()
        api.add_qa(qa_record(attempt=1), qa_run(attempt=1))
        api.add_run(qa_run(attempt=2), jobs=[job("run-tests / Validate + resolve run inputs")])
        api.add_production()
        payload = collect_regression(api, attempt=2)
        self.assertIn("Regression evidence belongs to an earlier attempt", payload["evidence"]["gaps"])
        self.assertIsNone(payload["evidence"]["qa"])

    def test_duplicate_artifacts_are_not_trusted(self):
        api = reg_world()
        api.add_artifact(1520, "release-log-qa-1520-1", qa_record())
        with self.assertRaises(report.CollectionError):
            collect_regression(api)

    def test_a_missing_export_on_a_successful_run_is_partial_and_fails_visibly(self):
        api = reg_world()
        api.artifacts[1520].clear()
        with self.assertRaises(report.CollectionError) as caught:
            collect_regression(api)
        partial = caught.exception.partial
        contract.validate_payload(partial)
        self.assertEqual(partial["release_version"], "unmatched")
        self.assertIn("Regression evidence export is missing", partial["evidence"]["gaps"])
        self.assertEqual(contract.build_fields(partial)["result"], "partial")

    def test_a_failed_run_without_an_export_is_reported_as_failed(self):
        api = reg_world(conclusion="failure", jobs=qa_jobs("failure"))
        api.artifacts[1520].clear()
        payload = collect_regression(api)
        fields = contract.build_fields(payload)
        self.assertEqual((fields["result"], fields["releaseVersion"]), ("failed", "unmatched"))

    def test_a_cancelled_run_is_unavailable_evidence(self):
        api = reg_world(conclusion="cancelled", jobs=qa_jobs("cancelled"))
        api.artifacts[1520].clear()
        payload = collect_regression(api)
        fields = contract.build_fields(payload)
        self.assertEqual(fields["result"], "cancelled")
        self.assertIn("Regression evidence export is missing", fields["gaps"])
        self.assertEqual((fields["tests"], fields["apkName"]), ("", ""))

    def test_an_expired_export_is_a_gap(self):
        api = reg_world()
        api.artifacts[1520][0]["expired"] = True
        with self.assertRaises(report.CollectionError) as caught:
            collect_regression(api)
        self.assertIn("Regression evidence export has expired", caught.exception.partial["evidence"]["gaps"])

    def test_report_links_outside_the_repository_pages_are_dropped(self):
        for url in ["https://example.com/allure/1520", "https://evil.github.io/wire-android/x/",
                    "https://wireapp.github.io/other-repo/x/", "https://wireapp.github.io.evil.com/wire-android/x/",
                    "https://wireapp.github.io/wire-android"]:
            with self.subTest(url=url):
                payload = collect_regression(reg_world(qa_record(report_url=url)))
                self.assertIsNone(payload["evidence"]["qa"]["report_url"])
                self.assertIn("Report link is not on the repository pages site", payload["evidence"]["gaps"])
                self.assertNotIn("report", contract.build_fields(payload)["tests"])
        payload = collect_regression(reg_world(qa_record(report_url=None)))
        self.assertEqual(payload["evidence"]["gaps"], [])

    def test_a_run_with_no_executed_tests_is_partial_not_a_pass(self):
        record = qa_record(results={"passed": 0, "failed": 0, "skipped": 421, "missing": 0, "selected": 421,
                                    "total": 421, "passed_on_rerun": 0})
        fields = contract.build_fields(collect_regression(reg_world(record)))
        self.assertEqual(fields["result"], "partial")
        self.assertIn("No tests were executed", fields["gaps"])

    def test_hostile_qa_text_cannot_forge_page_text(self):
        record = qa_record(selector="@RC; result passed\n".replace("\n", ""), suite="x; gaps none")
        record["apk"] = dict(record["apk"])
        payload = collect_regression(reg_world(record))
        fields = contract.build_fields(payload)
        self.assertEqual(fields["tests"].count(";"), 2)
        self.assertNotIn("; result passed", fields["tests"])
        self.assertEqual(fields["result"], "failed")
        self.assertEqual(fields["gaps"], "none")


class MatchProductionBuildTest(unittest.TestCase):
    def match(self, qa=None, manifests=None, **keywords):
        return report.match_production_build(qa or qa_record(), [prod_build()] if manifests is None else manifests,
                                             **keywords)

    def test_one_matching_origin_attributes_the_build_and_the_version(self):
        result = self.match()
        self.assertEqual(result["origin"], {"run_id": 1400, "run_attempt": 1,
                                            "url": "https://github.com/wireapp/wire-android/actions/runs/1400/attempts/1"})
        self.assertEqual((result["release_version"], result["gap"]), ("4.38.0", None))

    def test_zero_matches_is_a_gap(self):
        for manifests in [[], [prod_build(apks=[{"filename": APK_NAME, "sha256": "ab" * 32}])],
                          [prod_build(apks=[{"filename": "other.apk", "sha256": APK_SHA}])]]:
            with self.subTest(manifests=manifests):
                result = self.match(manifests=manifests)
                self.assertIsNone(result["origin"])
                self.assertIsNone(result["release_version"])
                self.assertTrue(result["gap"])

    def test_multiple_origins_are_a_gap_never_a_guess(self):
        result = self.match(manifests=[prod_build(1400, 1), prod_build(1400, 2), prod_build(1450, 1)])
        self.assertIsNone(result["origin"])
        self.assertIn("more than one", result["gap"])

    def test_incomplete_or_expired_enumeration_is_a_gap_even_with_one_match(self):
        result = self.match(unverified=["a production build record has expired"])
        self.assertIsNone(result["origin"])
        self.assertIn("expired", result["gap"])

    def test_the_digest_and_the_file_name_must_both_match(self):
        other_name = qa_record(apk=dict(qa_record()["apk"], filename="renamed.apk"))
        self.assertIsNone(self.match(qa=other_name)["origin"])
        other_digest = qa_record(apk=dict(qa_record()["apk"], sha256="ee" * 32))
        self.assertIsNone(self.match(qa=other_digest)["origin"])

    def test_only_production_build_records_are_origins(self):
        beta = prod_build(flavor="Beta", tag="v4.38.0-beta.2")
        self.assertIsNone(self.match(manifests=[beta])["origin"])
        context = fixture("context")
        self.assertIsNone(self.match(manifests=[context])["origin"])

    def real(self, name, label, tag, version_name, version_code, sha=APK_SHA):
        apk = {"filename": name, "sha256": sha, "resolved_version_label": label}
        build = prod_build(tag=tag, version_name=version_name, version_code=version_code,
                           apks=[{"filename": name, "sha256": sha}])
        return apk, build

    def test_a_real_label_and_version_code_do_not_veto_a_digest_match(self):
        name = "com.wire.android-v4.38.0-50841-prod-compatrelease.apk"
        apk, build = self.real(name, "4.38.0-50841", "v4.38.0", "4.38.0", 100250841)
        result = self.match(qa=qa_record(apk=apk), manifests=[build])
        self.assertEqual((result["release_version"], result["gap"]), ("4.38.0", None))

    def test_a_label_that_names_another_version_than_the_tag_still_matches(self):
        name = "com.wire.android-v4.36.0-43922-prod-compatrelease.apk"
        apk, build = self.real(name, "4.36.0-43922", "v4.36.2", "4.36.0", 100243922)
        result = self.match(qa=qa_record(apk=apk), manifests=[build])
        self.assertEqual((result["release_version"], result["gap"]), ("4.36.2", None))

    def test_any_label_cannot_refuse_a_digest_and_file_name_match(self):
        for label in [None, "latest", "", "4.37.0", "4.38.0-beta.2", "4.38.0-71999"]:
            with self.subTest(label=label):
                qa = qa_record(apk=dict(qa_record()["apk"], resolved_version_label=label))
                self.assertEqual(self.match(qa=qa)["release_version"], "4.38.0")

    def test_the_same_name_with_another_digest_is_a_gap(self):
        name = "com.wire.android-v4.38.0-50841-prod-compatrelease.apk"
        apk, build = self.real(name, "4.38.0-50841", "v4.38.0", "4.38.0", 100250841, sha="ee" * 32)
        build["apks"] = [{"filename": name, "sha256": APK_SHA}]
        result = self.match(qa=qa_record(apk=apk), manifests=[build])
        self.assertIsNone(result["origin"])
        self.assertIn("no production build", result["gap"])

    def test_the_same_digest_with_another_name_is_a_gap(self):
        apk, build = self.real("com.wire.android-v4.38.0-50841-prod-compatrelease.apk", "4.38.0-50841",
                               "v4.38.0", "4.38.0", 100250841)
        build["apks"] = [{"filename": "com.wire.android-v4.38.0-50842-prod-compatrelease.apk", "sha256": APK_SHA}]
        self.assertIsNone(self.match(qa=qa_record(apk=apk), manifests=[build])["origin"])

    def test_an_unmatched_digest_gets_the_gap_text_and_is_not_a_skip(self):
        other = qa_record(apk=dict(qa_record()["apk"], sha256="ee" * 32))
        result = self.match(qa=other)
        self.assertIsNone(result["origin"])
        self.assertIn("no production build from the preceding", result["gap"])

    def test_a_missing_record_or_apk_cannot_match(self):
        self.assertIsNone(self.match(qa=qa_record(apk=None))["origin"])
        self.assertIn("not recorded", self.match(qa=qa_record(apk=None))["gap"])
        self.assertIsNone(report.match_production_build(None, [prod_build()])["origin"])


class ProductionEnumerationTest(unittest.TestCase):
    def test_only_the_preceding_90_days_are_enumerated(self):
        api = reg_world(production=False)
        api.add_production(run=prod_run(created="2026-07-01T10:00:00Z"))
        payload = collect_regression(api)
        self.assertIsNone(payload["evidence"]["origin"])
        self.assertEqual(api.listing_params[0]["created"], ">=2026-08-11")
        self.assertIn(UNMATCHED_TEXT, " ".join(payload["evidence"]["gaps"]))

    def test_a_run_just_inside_the_window_matches(self):
        api = reg_world(production=False)
        api.add_production(run=prod_run(created="2026-08-12T00:00:00Z"))
        self.assertEqual(collect_regression(api)["evidence"]["origin"]["run_id"], 1400)

    def test_a_production_run_created_after_the_regression_is_not_its_origin(self):
        api = reg_world(production=False)
        api.add_production(run=prod_run(created="2026-11-10T10:00:00Z"))
        self.assertIsNone(collect_regression(api)["evidence"]["origin"])

    def test_two_origins_with_the_same_digest_are_a_gap(self):
        api = reg_world()
        api.add_production(run=prod_run(1450, created="2026-10-25T10:00:00Z"))
        payload = collect_regression(api)
        self.assertIsNone(payload["evidence"]["origin"])
        self.assertIn("more than one", " ".join(payload["evidence"]["gaps"]))

    def test_every_attempt_of_a_production_run_is_its_own_build_record(self):
        api = reg_world(production=False)
        run = prod_run(attempt=2)
        api.runs[(1400, 1)] = prod_run(attempt=1)
        api.add_production(run=run, record=prod_build(1400, 2))
        api.add_artifact(1400, "release-log-build-Prod-1400-1", prod_build(1400, 1, apks=[
            {"filename": "other.apk", "sha256": "ab" * 32}]))
        payload = collect_regression(api)
        self.assertEqual(payload["evidence"]["origin"], {
            "run_id": 1400, "run_attempt": 2,
            "url": "https://github.com/wireapp/wire-android/actions/runs/1400/attempts/2"})

    def test_a_rerun_production_run_matches_one_origin_when_only_attempt_one_has_the_record(self):
        api = reg_world(production=False)
        api.production.append(prod_run(attempt=2))
        api.add_run(prod_run(attempt=1), [])
        api.add_run(prod_run(attempt=2), [])
        api.add_artifact(1400, "release-log-build-Prod-1400-1", prod_build(1400, 1))
        payload = collect_regression(api)
        self.assertEqual(payload["evidence"]["gaps"], [])
        self.assertEqual((payload["evidence"]["origin"]["run_id"], payload["evidence"]["origin"]["run_attempt"]),
                         (1400, 1))

    def test_a_production_run_listed_twice_is_one_origin(self):
        api = reg_world()
        filler = [prod_run(2000 + index, created="2026-10-21T10:00:00Z") for index in range(3)]
        api.production[:0] = filler[:1]
        api.production.extend(filler[1:] + [api.production[-1]])
        for run in filler:
            api.add_run(run, [])
            api.add_artifact(run["id"], "release-log-build-Prod-%d-1" % run["id"],
                             prod_build(run["id"], apks=[{"filename": "x.apk", "sha256": "ab" * 32}]))
        with patch.object(report, "PER_PAGE", 2):
            payload = collect_regression(api)
        self.assertEqual(payload["evidence"]["gaps"], [])
        self.assertEqual(payload["evidence"]["origin"]["run_id"], 1400)

    def test_a_production_run_that_is_not_completed_is_never_an_origin(self):
        api = reg_world(production=False)
        api.add_production(run=prod_run(status="in_progress", conclusion=None))
        payload = collect_regression(api)
        self.assertIsNone(payload["evidence"]["origin"])
        self.assertIn(UNMATCHED_TEXT, " ".join(payload["evidence"]["gaps"]))

    def test_a_production_run_with_an_unknown_conclusion_leaves_the_attribution_unverified(self):
        api = reg_world(production=False)
        api.add_production(run=prod_run(conclusion="mystery"))
        payload = collect_regression(api)
        self.assertIsNone(payload["evidence"]["origin"])
        self.assertIn("production builds could not be fully enumerated", " ".join(payload["evidence"]["gaps"]))

    def test_a_regression_without_a_start_time_gets_the_unverified_window_gap(self):
        for created in (None, "", "yesterday"):
            with self.subTest(created=created):
                payload = collect_regression(reg_world(created_at=created))
                self.assertIsNone(payload["evidence"]["origin"])
                self.assertIn("the regression run time is unknown", " ".join(payload["evidence"]["gaps"]))

    def test_an_expired_build_record_makes_the_attribution_a_gap(self):
        api = reg_world(production=False)
        api.add_production(expired=True)
        payload = collect_regression(api)
        self.assertIsNone(payload["evidence"]["origin"])
        self.assertIn("expired", " ".join(payload["evidence"]["gaps"]))

    def test_an_invalid_or_forged_build_record_makes_the_attribution_a_gap(self):
        forged = [dict(prod_build(), run_id=99), dict(prod_build(), sha=OTHER_SHA), dict(prod_build(), tag="v9.9.9"),
                  dict(prod_build(), flavor="Beta"), dict(prod_build(), workflow_path=BETA_PATH), {"kind": "build"}]
        for record in forged:
            with self.subTest(record=record.get("tag")):
                api = reg_world(production=False)
                api.add_production(record=record)
                payload = collect_regression(api)
                self.assertIsNone(payload["evidence"]["origin"])
                self.assertIn(UNMATCHED_TEXT, " ".join(payload["evidence"]["gaps"]))

    def test_a_listing_failure_is_an_incomplete_enumeration_not_a_crash(self):
        api = reg_world()
        api.listing_error = report.ApiError("GitHub is unreachable")
        payload = collect_regression(api)
        self.assertIsNone(payload["evidence"]["origin"])
        self.assertIn("could not be fully enumerated", " ".join(payload["evidence"]["gaps"]))

    def test_a_listing_that_exceeds_the_page_limit_is_incomplete(self):
        api = reg_world(production=False)
        for index in range(6):
            api.add_production(run=prod_run(2000 + index, created="2026-10-21T10:00:00Z"),
                               record=prod_build(2000 + index, apks=[{"filename": "x.apk", "sha256": "ab" * 32}]))
        with patch.object(report, "PER_PAGE", 1), patch.object(report, "MAX_PAGES", 3):
            payload = collect_regression(api)
        self.assertIsNone(payload["evidence"]["origin"])
        self.assertIn("could not be fully enumerated", " ".join(payload["evidence"]["gaps"]))

    def test_the_listing_is_paginated(self):
        api = reg_world(production=False)
        for index in range(4):
            api.add_production(run=prod_run(2000 + index, created="2026-10-21T10:00:00Z"),
                               record=prod_build(2000 + index, apks=[{"filename": "x.apk", "sha256": "ab" * 32}]))
        api.add_production()
        with patch.object(report, "PER_PAGE", 2):
            payload = collect_regression(api)
        self.assertEqual(payload["evidence"]["origin"]["run_id"], 1400)

    def attempt_two_world(self, attempt_two_jobs):
        api = reg_world(production=False)
        api.production.append(prod_run(attempt=2))
        api.add_run(prod_run(attempt=1), [])
        api.add_run(prod_run(attempt=2), attempt_two_jobs)
        api.add_artifact(1400, "release-log-build-Prod-1400-1", prod_build(1400, 1))
        return api

    def test_a_successful_build_without_a_record_leaves_the_attribution_unverified(self):
        api = self.attempt_two_world([job("build-production / Build Prod-Compatrelease")])
        payload = collect_regression(api)
        self.assertIsNone(payload["evidence"]["origin"])
        self.assertIn("a production build record is missing", " ".join(payload["evidence"]["gaps"]))
        self.assertIn(UNMATCHED_TEXT, " ".join(payload["evidence"]["gaps"]))

    def test_a_missing_record_of_a_build_that_did_not_succeed_or_did_not_run_is_allowed(self):
        for jobs in ([job("build-production / Build Prod-Compatrelease", "failure")],
                     [job("build-production / Build Prod-Compatrelease", "cancelled")],
                     [job("build-production / Build Prod-Compatrelease", "skipped")],
                     [job("build-production / deploy (s3)")],
                     [job("other-pipeline / Build Prod-Compatrelease")], []):
            with self.subTest(jobs=jobs):
                payload = collect_regression(self.attempt_two_world(jobs))
                self.assertEqual(payload["evidence"]["gaps"], [])
                self.assertEqual(payload["evidence"]["origin"]["run_attempt"], 1)

    def test_an_unreadable_jobs_listing_leaves_the_attribution_unverified(self):
        api = self.attempt_two_world([])
        api.jobs_error = report.ApiError("GitHub is unreachable")
        payload = collect_regression(api)
        self.assertIsNone(payload["evidence"]["origin"])
        self.assertIn("could not be fully enumerated", " ".join(payload["evidence"]["gaps"]))

    def test_jobs_are_listed_only_for_attempts_without_a_record(self):
        api = reg_world()
        collect_regression(api)
        self.assertEqual([call for call in api.calls if call.startswith("/repos/%s/actions/runs/1400" % REPO)
                          and call.endswith("/jobs")], [])
        api = self.attempt_two_world([])
        collect_regression(api)
        self.assertEqual([call for call in api.calls if call.startswith("/repos/%s/actions/runs/1400" % REPO)
                          and call.endswith("/jobs")], ["/repos/%s/actions/runs/1400/attempts/2/jobs" % REPO])

    def test_production_runs_of_other_workflows_or_repositories_are_ignored(self):
        api = reg_world(production=False)
        api.add_production(run=prod_run(path=".github/workflows/build-beta.yml@refs/tags/v4.38.0"))
        api.add_production(run=prod_run(1450, repository={"full_name": "other/repo"}))
        self.assertIsNone(collect_regression(api)["evidence"]["origin"])


def skip_zip(attempt=1, **changes):
    marker = contract.skip_marker(REPO, 1520, attempt)
    marker.update(changes)
    return zip_of({"skip.json": json.dumps(marker)})


def skip_world(artifact_name="release-log-qa-skip-1520-1", content=None, **run_changes):
    api = RegressionApi()
    api.add_run(qa_run(**run_changes), qa_jobs())
    api.add_artifact(1520, artifact_name, skip_zip() if content is None else content)
    api.add_production()
    return api


class SkippedRegressionTest(unittest.TestCase):
    def test_a_marker_without_a_record_ends_in_a_clean_skip(self):
        for conclusion in ("success", "failure"):
            with self.subTest(conclusion=conclusion):
                self.assertIsNone(collect_regression(skip_world(conclusion=conclusion)))

    def test_a_missing_marker_and_a_missing_export_is_still_missing_evidence(self):
        api = RegressionApi()
        api.add_run(qa_run(), qa_jobs())
        with self.assertRaises(report.CollectionError) as caught:
            collect_regression(api)
        self.assertIn("Regression evidence export is missing", caught.exception.partial["evidence"]["gaps"])

    def test_a_marker_never_turns_a_regression_record_into_a_skip(self):
        api = reg_world()
        api.add_artifact(1520, "release-log-qa-skip-1520-1", skip_zip())
        self.assertEqual(collect_regression(api)["kind"], "regression")

    def test_a_marker_does_not_hide_an_invalid_or_expired_record(self):
        for expired, content in [(True, qa_record()), (False, b"not a zip")]:
            with self.subTest(expired=expired):
                api = skip_world()
                api.add_artifact(1520, "release-log-qa-1520-1", content, expired=expired)
                with self.assertRaises(report.CollectionError):
                    collect_regression(api)

    def test_a_marker_of_another_run_or_attempt_is_not_a_skip(self):
        for name, content in [("release-log-qa-skip-1520-2", None), ("release-log-qa-skip-1999-1", None),
                              ("release-log-qa-skip-1520-1", skip_zip(run_id=1999)),
                              ("release-log-qa-skip-1520-1", skip_zip(run_attempt=2)),
                              ("release-log-qa-skip-1520-1", skip_zip(repository="other/repo")),
                              ("release-log-qa-skip-1520-1", skip_zip(kind="regression")),
                              ("release-log-qa-skip-1520-1", skip_zip(extra="x")),
                              ("release-log-qa-skip-1520-1", zip_of({"evidence.json": "{}"})),
                              ("release-log-qa-skip-1520-1", b"not a zip")]:
            with self.subTest(name=name), self.assertRaises(report.CollectionError):
                collect_regression(skip_world(name, content))

    def rerun_world(self, marker_attempt=1, expired=False, content=None, record=False):
        api = RegressionApi()
        api.add_run(qa_run(attempt=2), qa_jobs())
        api.add_artifact(1520, "release-log-qa-skip-1520-%d" % marker_attempt,
                         skip_zip(marker_attempt) if content is None else content, expired=expired)
        if record:
            api.add_artifact(1520, "release-log-qa-1520-2", qa_record(attempt=2))
            api.add_production()
        return api

    def test_a_marker_of_an_earlier_attempt_skips_a_later_attempt_of_the_same_run(self):
        self.assertIsNone(collect_regression(self.rerun_world(), attempt=2))

    def test_a_marker_of_a_later_attempt_is_not_accepted(self):
        api = RegressionApi()
        api.add_run(qa_run(attempt=1), qa_jobs())
        api.add_artifact(1520, "release-log-qa-skip-1520-2", skip_zip(2))
        with self.assertRaises(report.CollectionError):
            collect_regression(api, attempt=1)

    def test_an_earlier_marker_must_be_valid_unexpired_and_for_its_own_attempt(self):
        for world in (self.rerun_world(expired=True), self.rerun_world(content=skip_zip(2)),
                      self.rerun_world(content=skip_zip(run_id=1999)), self.rerun_world(content=b"not a zip")):
            with self.assertRaises(report.CollectionError):
                collect_regression(world, attempt=2)

    def test_two_markers_of_one_earlier_attempt_are_not_accepted(self):
        api = self.rerun_world()
        api.add_artifact(1520, "release-log-qa-skip-1520-1", skip_zip())
        with self.assertRaises(report.CollectionError):
            collect_regression(api, attempt=2)

    def test_a_record_wins_over_a_marker_of_an_earlier_attempt(self):
        payload = collect_regression(self.rerun_world(record=True), attempt=2)
        self.assertEqual(payload["kind"], "regression")

    def test_two_markers_for_one_run_are_not_a_skip(self):
        api = skip_world()
        api.add_artifact(1520, "release-log-qa-skip-1520-1", skip_zip())
        with self.assertRaises(report.CollectionError):
            collect_regression(api)

    def test_the_collect_command_writes_no_payload_and_succeeds_on_a_skip(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / "payload.json"
            code, output, errors = CommandLineTest.run_main(
                self, ["collect", "--repository", REPO, "--run-id", "1520", "--run-attempt", "1",
                       "--output", str(target)], api=skip_world())
            self.assertEqual((code, errors), (0, ""))
            self.assertIn("is not a release run, nothing to report", output)
            self.assertFalse(target.exists())


class UnmatchedRegressionTest(unittest.TestCase):
    def unmatched(self, **record_changes):
        api = reg_world(qa_record(**record_changes), production=False)
        return collect_regression(api)

    def test_the_apk_version_label_names_the_page_and_the_gap_says_it_is_unattached(self):
        passing = {"passed": 421, "failed": 0, "skipped": 0, "missing": 0, "selected": 421, "total": 421,
                   "passed_on_rerun": 0}
        payload = self.unmatched(apk=dict(qa_record()["apk"], resolved_version_label="4.39.0-72000"),
                                 results=passing)
        self.assertEqual(payload["release_version"], "4.39.0")
        fields = contract.build_fields(payload)
        self.assertEqual((fields["releaseVersion"], fields["buildRunUrl"], fields["result"]), ("4.39.0", "", "partial"))
        self.assertIn(UNMATCHED_TEXT, fields["gaps"])
        self.assertIn(NOT_ATTACHED, fields["gaps"])

    def test_without_a_label_the_fixed_unmatched_version_is_used(self):
        for apk in [dict(qa_record()["apk"], resolved_version_label=None), None,
                    dict(qa_record()["apk"], resolved_version_label="latest"),
                    dict(qa_record()["apk"], resolved_version_label="Wire release 9.9.9 label")]:
            with self.subTest(apk=apk):
                payload = self.unmatched(apk=apk)
                self.assertEqual(payload["release_version"], "unmatched")
                fields = contract.build_fields(payload)
                self.assertEqual(fields["releaseVersion"], "unmatched")
                self.assertIn(NOT_ATTACHED, fields["gaps"])

    def test_the_gap_names_why_it_is_unmatched(self):
        payload = self.unmatched()
        self.assertIn("no production build", " ".join(payload["evidence"]["gaps"]))

    def test_a_regression_with_a_full_pass_still_shows_the_gap_as_partial(self):
        results = {"passed": 421, "failed": 0, "skipped": 0, "missing": 0, "selected": 421, "total": 421,
                   "passed_on_rerun": 0}
        fields = contract.build_fields(self.unmatched(results=results))
        self.assertEqual(fields["result"], "partial")


class DeflakeAncestryTest(unittest.TestCase):
    def chain(self, source_changes=None, deflake_changes=None, source_run=None):
        api = RegressionApi()
        api.add_qa(qa_record(**dict({"next_deflake_bundle_sha256": FINGERPRINT}, **(source_changes or {}))),
                   source_run)
        api.add_qa(deflake_record(**(deflake_changes or {})))
        api.add_production()
        return api

    def collect(self, api, run_id=1600):
        return collect_regression(api, run_id=run_id)

    def assertAttributed(self, payload):
        self.assertEqual(payload["evidence"]["gaps"], [])
        self.assertEqual(payload["evidence"]["origin"]["run_id"], 1400)

    def assertNotAttributed(self, payload, reason):
        self.assertIsNone(payload["evidence"]["origin"])
        gaps = " ".join(payload["evidence"]["gaps"])
        self.assertIn("Deflake source is not verified", gaps)
        self.assertIn(reason, gaps)
        self.assertIn(NOT_ATTACHED, gaps)
        self.assertEqual(contract.build_fields(payload)["result"], "partial")

    def test_a_deflake_with_a_matching_source_is_attributed_and_never_a_full_regression(self):
        payload = self.collect(self.chain())
        self.assertAttributed(payload)
        fields = contract.build_fields(payload)
        self.assertEqual((fields["kind"], fields["result"]), ("Deflake", "passed"))
        self.assertIn("not a full regression", fields["tests"])
        self.assertIn("source run 1520", fields["tests"])

    def test_the_current_run_keeps_its_own_results_and_retry_count(self):
        api = self.chain(deflake_changes=dict(retry_count=1, results={
            "passed": 6, "failed": 0, "skipped": 0, "missing": 0, "selected": 6, "total": 6, "passed_on_rerun": 1}))
        qa = self.collect(api)["evidence"]["qa"]
        self.assertEqual((qa["retry_count"], qa["results"]["total"], qa["results"]["passed_on_rerun"]), (1, 6, 1))
        self.assertEqual(qa["run_id"], 1600)

    def test_a_chain_of_deflakes_is_followed_to_the_full_run(self):
        api = RegressionApi()
        api.add_qa(qa_record(next_deflake_bundle_sha256=FINGERPRINT))
        api.add_qa(deflake_record(1600, next_deflake_bundle_sha256=SECOND_FINGERPRINT))
        api.add_qa(deflake_record(1700, source=(1600, 1), source_deflake_bundle_sha256=SECOND_FINGERPRINT))
        api.add_production()
        self.assertAttributed(self.collect(api, run_id=1700))

    def test_a_fingerprint_that_differs_from_the_source_record_fails_attribution(self):
        payload = self.collect(self.chain(deflake_changes=dict(source_deflake_bundle_sha256=SECOND_FINGERPRINT)))
        self.assertNotAttributed(payload, "bundle fingerprint differs")

    def test_a_legacy_bundle_without_proof_fails_attribution(self):
        payload = self.collect(self.chain(deflake_changes=dict(source_deflake_bundle_sha256=None)))
        self.assertNotAttributed(payload, "bundle is unverified")

    def test_a_source_record_without_a_produced_fingerprint_fails_attribution(self):
        payload = self.collect(self.chain(source_changes=dict(next_deflake_bundle_sha256=None)))
        self.assertNotAttributed(payload, "bundle is unverified")

    def test_a_missing_source_record_fails_attribution(self):
        api = self.chain()
        api.artifacts[1520].clear()
        self.assertNotAttributed(self.collect(api), "source record is missing")

    def test_a_source_run_that_cannot_be_found_fails_attribution(self):
        api = self.chain(deflake_changes=dict(source=(1999, 1)))
        self.assertNotAttributed(self.collect(api), "source run could not be read")

    def test_a_source_record_of_another_run_fails_attribution(self):
        api = self.chain()
        api.artifacts[1520].clear()
        api.add_artifact(1520, "release-log-qa-1520-1", qa_record(run_id=1999, next_deflake_bundle_sha256=FINGERPRINT))
        self.assertNotAttributed(self.collect(api), "source record is invalid")

    def test_a_source_attempt_record_is_not_taken_from_another_attempt(self):
        api = RegressionApi()
        api.add_qa(qa_record(attempt=1, next_deflake_bundle_sha256=FINGERPRINT))
        api.add_run(qa_run(attempt=2))
        api.add_qa(deflake_record(source=(1520, 2)))
        api.add_production()
        self.assertNotAttributed(self.collect(api), "source record is missing")

    def test_a_source_run_of_another_workflow_fails_attribution(self):
        api = self.chain(source_run=qa_run(path=".github/workflows/qa-android-regression-tests.yml@refs/heads/x"))
        self.assertNotAttributed(self.collect(api), "source run is not an eligible regression")

    def test_a_source_run_that_was_not_manual_fails_attribution(self):
        api = self.chain(source_run=qa_run(event="schedule"))
        self.assertNotAttributed(self.collect(api), "source run is not an eligible regression")

    def test_a_source_run_from_a_branch_fails_attribution(self):
        api = self.chain(source_run=qa_run(head_branch="attacker/forge"))
        self.assertNotAttributed(self.collect(api), "source run is not an eligible regression")

    def test_a_source_run_from_a_release_branch_is_accepted(self):
        api = self.chain(source_run=qa_run(head_branch="release/cycle-4.38.0"))
        self.assertAttributed(self.collect(api))

    def test_a_different_test_revision_fails_attribution(self):
        api = self.chain(source_changes=dict(test_source_sha=OTHER_SHA))
        self.assertNotAttributed(self.collect(api), "test revision differs")

    def test_a_different_suite_or_selector_fails_attribution(self):
        for change in [dict(suite="other"), dict(selector="@criticalFlow")]:
            with self.subTest(change=change):
                self.assertNotAttributed(self.collect(self.chain(source_changes=change)), "scope differs")

    def test_a_cycle_fails_attribution(self):
        api = RegressionApi()
        api.add_qa(deflake_record(1600, source=(1700, 1), next_deflake_bundle_sha256=FINGERPRINT,
                                  source_deflake_bundle_sha256=SECOND_FINGERPRINT))
        api.add_qa(deflake_record(1700, source=(1600, 1), next_deflake_bundle_sha256=SECOND_FINGERPRINT,
                                  source_deflake_bundle_sha256=FINGERPRINT))
        api.add_production()
        self.assertNotAttributed(self.collect(api), "source chain repeats")

    def test_a_deflake_that_names_itself_as_source_fails_attribution(self):
        api = RegressionApi()
        api.add_qa(deflake_record(1600, source=(1600, 1), next_deflake_bundle_sha256=FINGERPRINT))
        api.add_production()
        self.assertNotAttributed(self.collect(api), "source chain repeats")

    def test_a_chain_that_is_too_long_fails_attribution(self):
        api = RegressionApi()
        api.add_qa(qa_record(next_deflake_bundle_sha256=FINGERPRINT))
        for index in range(1, 5):
            api.add_qa(deflake_record(1520 + index, source=(1519 + index, 1),
                                      next_deflake_bundle_sha256=FINGERPRINT))
        api.add_production()
        with patch.object(report, "MAX_ANCESTRY", 2):
            self.assertNotAttributed(self.collect(api, run_id=1524), "source chain is too long")

    def chain_of(self, hops):
        """A deflake whose full run is `hops` source runs away. Run 1600 is the deflake that is collected."""
        api = RegressionApi()
        api.add_qa(qa_record(next_deflake_bundle_sha256=FINGERPRINT))
        previous = 1520
        for index in range(hops):
            run_id = 1600 + index
            api.add_qa(deflake_record(run_id, source=(previous, 1), next_deflake_bundle_sha256=FINGERPRINT))
            previous = run_id
        api.add_production()
        return api, previous

    def source_reads(self, api, collected):
        return [call for call in api.calls if "/attempts/" in call and not call.endswith("/jobs")
                and "/runs/%d/" % collected not in call and "/runs/1400/" not in call]

    def test_a_chain_of_exactly_the_limit_is_verified_and_one_more_is_not(self):
        with patch.object(report, "MAX_ANCESTRY", 3):
            api, last = self.chain_of(3)
            self.assertAttributed(self.collect(api, run_id=last))
            self.assertEqual(len(self.source_reads(api, last)), 3)
            api, last = self.chain_of(4)
            payload = self.collect(api, run_id=last)
            self.assertNotAttributed(payload, "source chain is too long")
            self.assertEqual(len(self.source_reads(api, last)), 3)

    def test_an_unverified_deflake_is_still_reported_under_its_apk_version(self):
        api = self.chain(deflake_changes=dict(source_deflake_bundle_sha256=None))
        fields = contract.build_fields(self.collect(api))
        self.assertEqual((fields["releaseVersion"], fields["kind"], fields["buildRunUrl"]), ("4.38.0", "Deflake", ""))

    def test_a_full_run_never_follows_ancestry(self):
        api = reg_world()
        collect_regression(api)
        self.assertEqual([call for call in api.calls if "/attempts/" in call and "1400" not in call
                          and "1520" not in call], [])


class PublishCarriesTheEventIdTest(unittest.TestCase):
    def test_the_twelve_fields_are_the_contract(self):
        self.assertIn("eventId", sample_fields())
        self.assertEqual(set(sample_fields()), set(contract.FIELD_NAMES))
        with self.assertRaises(ValueError):
            send(Transport(), fields={key: value for key, value in sample_fields().items() if key != "eventId"},
                 enabled=False)


if __name__ == "__main__":
    unittest.main()
