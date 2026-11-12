#!/usr/bin/env python3
"""Unit tests for the release log context and build manifest exporters."""

from __future__ import annotations

import copy
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
import unittest
from contextlib import redirect_stderr, redirect_stdout
from io import StringIO
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))

import release_log_contract as contract
import release_log_export as export

FIXTURES = Path(__file__).resolve().parent / "fixtures" / "release-log"
SHA = "7be41d0c3f2a9c1e5d6b8a4f0123456789abcdef"
VERSION_FILE = ("VersionCode: 71389\nVersionName: 4.38.0-beta.2\nRevision: 7be41d0\n"
                "Buildtime: 2026-11-09 10:00:00\nApplication-name: com.wire.android.internal")

BETA_RUN = {
    "repository": "wireapp/wire-android", "run_id": "1001", "run_attempt": "1",
    "workflow_ref": "wireapp/wire-android/.github/workflows/build-beta.yml@refs/tags/v4.38.0-beta.2",
    "sha": SHA, "event_name": "push", "ref_type": "tag", "ref_name": "v4.38.0-beta.2",
}
PROD_RUN = {
    "repository": "wireapp/wire-android", "run_id": "1400", "run_attempt": "1",
    "workflow_ref": "wireapp/wire-android/.github/workflows/build-production.yml@refs/heads/develop",
    "sha": SHA, "event_name": "release", "ref_type": "branch", "ref_name": "develop",
}
RELEASE_EVENT = {"release": {"tag_name": "v4.38.0"}}


def fixture(name):
    return json.loads((FIXTURES / (name + ".json")).read_text())


class BuildContextTest(unittest.TestCase):
    def test_a_beta_tag_push_records_the_tag_ref(self):
        record = export.build_context({}, BETA_RUN)
        self.assertEqual(record, fixture("context"))
        contract.validate_record(record)

    def test_a_production_release_records_the_release_event_tag(self):
        record = export.build_context(RELEASE_EVENT, PROD_RUN)
        self.assertEqual(
            (record["tag"], record["event_name"], record["workflow_path"], record["run_id"]),
            ("v4.38.0", "release", ".github/workflows/build-production.yml", 1400))
        contract.validate_record(record)

    def test_context_needs_no_build_output(self):
        # A failed build still exports its context: only the event and run values are used.
        self.assertEqual(export.build_context(RELEASE_EVENT, PROD_RUN)["kind"], "context")

    def test_the_release_event_tag_wins_over_the_branch_ref(self):
        run = dict(PROD_RUN, ref_type="tag", ref_name="v9.9.9")
        self.assertEqual(export.build_context(RELEASE_EVENT, run)["tag"], "v4.38.0")

    def test_runs_without_a_valid_release_tag_fail(self):
        branch = dict(BETA_RUN, ref_type="branch", ref_name="develop")
        with self.assertRaises(ValueError):
            export.build_context({}, branch)
        for tag in ["v4.38.0-rc.1", "4.38.0", "v4.38.0\nx", ""]:
            with self.subTest(tag=tag), self.assertRaises(ValueError):
                export.build_context({"release": {"tag_name": tag}}, dict(PROD_RUN))
        for event in [{"release": "v4.38.0"}, {"release": {}}, {"release": {"tag_name": 5}}]:
            with self.subTest(event=event), self.assertRaises(ValueError):
                export.build_context(event, dict(PROD_RUN))

    def test_the_workflow_must_belong_to_this_repository(self):
        for ref in ["other/repo/.github/workflows/build-beta.yml@refs/heads/x",
                    "wireapp/wire-android/scripts/x.yml@refs/heads/x",
                    "wireapp/wire-android/.github/workflows/build-beta.yml",
                    "", "wireapp/wire-android"]:
            with self.subTest(ref=ref), self.assertRaises(ValueError):
                export.build_context({}, dict(BETA_RUN, workflow_ref=ref))

    def test_run_values_must_be_positive_integers(self):
        for key, value in [("run_id", "0"), ("run_id", "abc"), ("run_attempt", "-1"), ("run_attempt", "")]:
            with self.subTest(key=key, value=value), self.assertRaises(ValueError):
                export.build_context({}, dict(BETA_RUN, **{key: value}))

    def test_missing_run_values_fail(self):
        run = dict(BETA_RUN)
        del run["sha"]
        with self.assertRaises(ValueError):
            export.build_context({}, run)

    def test_run_context_comes_from_the_github_environment(self):
        env = {"GITHUB_REPOSITORY": "wireapp/wire-android", "GITHUB_RUN_ID": "1001", "GITHUB_RUN_ATTEMPT": "1",
               "GITHUB_WORKFLOW_REF": BETA_RUN["workflow_ref"], "GITHUB_SHA": SHA, "GITHUB_EVENT_NAME": "push",
               "GITHUB_REF_TYPE": "tag", "GITHUB_REF_NAME": "v4.38.0-beta.2", "UNRELATED": "x"}
        self.assertEqual(export.run_context_from_env(env), BETA_RUN)
        del env["GITHUB_SHA"]
        with self.assertRaises(ValueError):
            export.run_context_from_env(env)


class BuildManifestTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.version_file = self.root / "version.txt"
        self.version_file.write_text(VERSION_FILE)
        self.context = export.build_context({}, BETA_RUN)

    def apk(self, name="wire-beta-release.apk", content=b"signed apk bytes"):
        path = self.root / name
        path.write_bytes(content)
        return path

    def test_the_manifest_hashes_the_final_apk_bytes(self):
        apk = self.apk(content=b"before lineage signing")
        before = export.build_manifest(self.context, self.version_file, [apk])["apks"][0]["sha256"]
        apk.write_bytes(b"after lineage signing")
        manifest = export.build_manifest(self.context, self.version_file, [apk])
        self.assertEqual(manifest["apks"], [
            {"filename": "wire-beta-release.apk", "sha256": hashlib.sha256(b"after lineage signing").hexdigest()}])
        self.assertNotEqual(before, manifest["apks"][0]["sha256"])

    def test_a_beta_manifest_matches_the_fixture_shape(self):
        apk = self.apk(content=b"x")
        manifest = export.build_manifest(self.context, self.version_file, [apk])
        expected = copy.deepcopy(fixture("build"))
        expected["apks"] = [{"filename": "wire-beta-release.apk", "sha256": hashlib.sha256(b"x").hexdigest()}]
        self.assertEqual(manifest, expected)
        contract.validate_record(manifest)

    def test_version_values_come_from_the_version_file_not_the_run(self):
        self.version_file.write_text("VersionCode: 5\nVersionName: 4.38.0-beta.2\n")
        context = export.build_context({}, dict(BETA_RUN, run_id="999999"))
        manifest = export.build_manifest(context, self.version_file, [self.apk()])
        self.assertEqual((manifest["version_code"], manifest["version_name"], manifest["run_id"]),
                         (5, "4.38.0-beta.2", 999999))

    def test_the_flavor_follows_the_tag(self):
        self.assertEqual(export.build_manifest(self.context, self.version_file, [self.apk()])["flavor"], "Beta")
        production = export.build_context(RELEASE_EVENT, PROD_RUN)
        self.assertEqual(export.build_manifest(production, self.version_file, [self.apk()])["flavor"], "Prod")

    def test_a_rerun_attempt_is_its_own_build_identity(self):
        rerun = export.build_context({}, dict(BETA_RUN, run_attempt="2"))
        manifest = export.build_manifest(rerun, self.version_file, [self.apk()])
        self.assertEqual((manifest["run_id"], manifest["run_attempt"]), (1001, 2))

    def test_apks_are_sorted_and_names_must_be_unique(self):
        a, b = self.apk("b.apk", b"b"), self.apk("a.apk", b"a")
        manifest = export.build_manifest(self.context, self.version_file, [a, b])
        self.assertEqual([apk["filename"] for apk in manifest["apks"]], ["a.apk", "b.apk"])
        other = self.root / "sub"
        other.mkdir()
        duplicate = other / "a.apk"
        duplicate.write_bytes(b"dup")
        with self.assertRaises(ValueError):
            export.build_manifest(self.context, self.version_file, [b, a, duplicate])

    def test_invalid_inputs_fail(self):
        apk = self.apk()
        with self.assertRaises(ValueError):
            export.build_manifest(self.context, self.version_file, [])
        with self.assertRaises(ValueError):
            export.build_manifest(self.context, self.root / "missing.txt", [apk])
        with self.assertRaises(ValueError):
            export.build_manifest(self.context, self.version_file, [self.root / "missing.apk"])
        with self.assertRaises(ValueError):
            export.build_manifest(self.context, self.version_file, [self.root])
        with self.assertRaises(ValueError):
            export.build_manifest(fixture("build"), self.version_file, [apk])
        link = self.root / "link.apk"
        link.symlink_to(apk)
        with self.assertRaises(ValueError):
            export.build_manifest(self.context, self.version_file, [link])

    def test_malformed_version_files_fail(self):
        apk = self.apk()
        for text in ["", "VersionName: 4.38.0\n", "VersionCode: x\nVersionName: 4.38.0\n",
                     "VersionCode: 0\nVersionName: 4.38.0\n", "VersionCode: 5\n", "VersionCode: 5\nVersionName:\n"]:
            with self.subTest(text=text):
                self.version_file.write_text(text)
                with self.assertRaises(ValueError):
                    export.build_manifest(self.context, self.version_file, [apk])


class CommandLineTest(unittest.TestCase):
    def run_main(self, argv, env=None):
        output, errors = StringIO(), StringIO()
        with patch.dict(os.environ, env or {}, clear=True), redirect_stdout(output), redirect_stderr(errors):
            code = export.main(argv)
        return code, output.getvalue(), errors.getvalue()

    def beta_env(self):
        return {"GITHUB_REPOSITORY": "wireapp/wire-android", "GITHUB_RUN_ID": "1001", "GITHUB_RUN_ATTEMPT": "1",
                "GITHUB_WORKFLOW_REF": BETA_RUN["workflow_ref"], "GITHUB_SHA": SHA, "GITHUB_EVENT_NAME": "push",
                "GITHUB_REF_TYPE": "tag", "GITHUB_REF_NAME": "v4.38.0-beta.2"}

    def test_the_context_command_writes_a_valid_record(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "event.json").write_text("{}")
            code, _, _ = self.run_main(
                ["context", "--event", str(root / "event.json"), "--output", str(root / "context.json")],
                self.beta_env())
            self.assertEqual(code, 0)
            self.assertEqual(json.loads((root / "context.json").read_text()), fixture("context"))

    def make_outputs(self, root, names=("b.apk", "a.apk")):
        outputs = root / "outputs"
        (outputs / "apk" / "beta" / "release").mkdir(parents=True)
        (outputs / "version.txt").write_text(VERSION_FILE)
        for name in names:
            (outputs / "apk" / "beta" / "release" / name).write_bytes(name.encode())
        return outputs

    def run_build(self, root, outputs, sha=SHA, extra=()):
        argv = ["build", "--event", str(root / "event.json"), "--outputs-dir", str(outputs), "--flavor", "Beta",
                "--variant", "Release", "--output", str(root / "build.json"), *extra]
        with patch.object(export, "checked_out_sha", return_value=sha):
            return self.run_main(argv, self.beta_env())

    def test_the_build_command_writes_one_manifest_for_the_checked_out_apks(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "event.json").write_text("{}")
            code, _, _ = self.run_build(root, self.make_outputs(root, ("b.apk", "a b.apk", "a.apk")))
            self.assertEqual(code, 0)
            manifest = json.loads((root / "build.json").read_text())
            self.assertEqual([apk["filename"] for apk in manifest["apks"]], ["a b.apk", "a.apk", "b.apk"])
            self.assertEqual((manifest["version_name"], manifest["flavor"]), ("4.38.0-beta.2", "Beta"))
            contract.validate_record(manifest)

    def test_the_build_command_exports_the_prod_apks_for_a_release_event(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "event.json").write_text(json.dumps(RELEASE_EVENT))
            outputs = root / "outputs"
            (outputs / "apk" / "prod" / "compatrelease").mkdir(parents=True)
            (outputs / "apk" / "prod" / "compatrelease" / "wire-prod-compatrelease.apk").write_bytes(b"signed")
            (outputs / "version.txt").write_text(VERSION_FILE.replace("4.38.0-beta.2", "4.38.0"))
            argv = ["build", "--event", str(root / "event.json"), "--outputs-dir", str(outputs),
                    "--flavor", "Prod", "--variant", "Compatrelease", "--output", str(root / "build.json")]
            env = {"GITHUB_REPOSITORY": PROD_RUN["repository"], "GITHUB_RUN_ID": PROD_RUN["run_id"],
                   "GITHUB_RUN_ATTEMPT": "2", "GITHUB_WORKFLOW_REF": PROD_RUN["workflow_ref"], "GITHUB_SHA": SHA,
                   "GITHUB_EVENT_NAME": "release", "GITHUB_REF_TYPE": "tag", "GITHUB_REF_NAME": "v4.38.0"}
            with patch.object(export, "checked_out_sha", return_value=SHA):
                code, _, _ = self.run_main(argv, env)
            self.assertEqual(code, 0)
            manifest = json.loads((root / "build.json").read_text())
            self.assertEqual((manifest["flavor"], manifest["tag"], manifest["run_attempt"], manifest["version_name"]),
                             ("Prod", "v4.38.0", 2, "4.38.0"))
            self.assertEqual(manifest["apks"][0]["sha256"], hashlib.sha256(b"signed").hexdigest())

    def test_the_build_command_ignores_files_that_are_not_apks(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "event.json").write_text("{}")
            outputs = self.make_outputs(root, ("a.apk", "mapping.txt", "a.apk.sha256"))
            code, _, _ = self.run_build(root, outputs)
            self.assertEqual(code, 0)
            manifest = json.loads((root / "build.json").read_text())
            self.assertEqual([apk["filename"] for apk in manifest["apks"]], ["a.apk"])

    def test_the_build_command_refuses_a_checkout_that_is_not_the_runs_commit(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "event.json").write_text("{}")
            code, _, errors = self.run_build(root, self.make_outputs(root), sha="0" * 40)
            self.assertEqual(code, 1)
            self.assertIn("checked-out commit", errors)
            self.assertFalse((root / "build.json").exists())

    def test_the_build_command_fails_without_apks(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "event.json").write_text("{}")
            code, _, errors = self.run_build(root, self.make_outputs(root, ()))
            self.assertEqual(code, 1)
            self.assertNotIn("Traceback", errors)
            self.assertFalse((root / "build.json").exists())

    def test_hidden_apk_files_are_ignored(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "event.json").write_text("{}")
            code, _, _ = self.run_build(root, self.make_outputs(root, ("a.apk", ".hidden.apk")))
            self.assertEqual(code, 0)
            manifest = json.loads((root / "build.json").read_text())
            self.assertEqual([apk["filename"] for apk in manifest["apks"]], ["a.apk"])

    def test_flavor_and_variant_must_name_a_directory_under_apk(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "event.json").write_text("{}")
            outputs = self.make_outputs(root)
            (outputs / "stray").mkdir()
            (outputs / "stray" / "x.apk").write_bytes(b"x")
            argv = ["build", "--event", str(root / "event.json"), "--outputs-dir", str(outputs), "--flavor", "..",
                    "--variant", "stray", "--output", str(root / "build.json")]
            with patch.object(export, "checked_out_sha", return_value=SHA):
                code, _, _ = self.run_main(argv, self.beta_env())
            self.assertEqual(code, 1)
            self.assertFalse((root / "build.json").exists())

    def test_the_variant_and_flavor_cannot_leave_the_outputs_directory(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "event.json").write_text("{}")
            outputs = self.make_outputs(root)
            argv = ["build", "--event", str(root / "event.json"), "--outputs-dir", str(outputs), "--flavor", "..",
                    "--variant", "..", "--output", str(root / "build.json")]
            with patch.object(export, "checked_out_sha", return_value=SHA):
                code, _, _ = self.run_main(argv, self.beta_env())
            self.assertEqual(code, 1)

    def test_the_event_file_follows_the_contract_json_policy(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "dup.json").write_text('{"release": {"tag_name": "v4.38.0"}, "release": {}}')
            (root / "big.json").write_text('{"a": "' + "x" * contract.JSON_LIMIT + '"}')
            for name in ["dup.json", "big.json"]:
                with self.subTest(name=name):
                    code, _, _ = self.run_main(
                        ["context", "--event", str(root / name), "--output", str(root / "c.json")], self.beta_env())
                    self.assertEqual(code, 1)
                    self.assertFalse((root / "c.json").exists())

    def test_failures_exit_nonzero_write_nothing_and_print_no_traceback(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "event.json").write_text("{}")
            code, _, errors = self.run_main(
                ["context", "--event", str(root / "event.json"), "--output", str(root / "context.json")], {})
            self.assertEqual(code, 1)
            self.assertNotIn("Traceback", errors)
            self.assertFalse((root / "context.json").exists())
            code, _, _ = self.run_main(
                ["context", "--event", str(root / "missing.json"), "--output", str(root / "context.json")], {})
            self.assertEqual(code, 1)
            (root / "deep.json").write_text("[" * 100000)
            code, _, errors = self.run_main(
                ["context", "--event", str(root / "deep.json"), "--output", str(root / "context.json")], {})
            self.assertEqual(code, 1)
            self.assertNotIn("Traceback", errors)


ROOT = Path(__file__).resolve().parents[2]
WORKFLOWS = ROOT / ".github" / "workflows"
UPLOAD = "actions/upload-artifact@043fb46d1a93c77aae656e7c1c64a875d1fc6a0a"


def workflow(name):
    return (WORKFLOWS / name).read_text()


def job_block(text, job):
    lines = text.splitlines()
    start = lines.index("  " + job + ":")
    end = next((i for i in range(start + 1, len(lines)) if re.match(r"^  [A-Za-z0-9_-]+:\s*$", lines[i])), len(lines))
    return "\n".join(lines[start:end])


def step_block(job, name):
    lines = job.splitlines()
    start = next(i for i, line in enumerate(lines) if line.strip() == "- name: " + name)
    end = next((i for i in range(start + 1, len(lines)) if lines[i].startswith("      - name:")), len(lines))
    return "\n".join(lines[start:end])


def step_index(job, name):
    return next(i for i, line in enumerate(job.splitlines()) if line.strip() == "- name: " + name)


class BetaWorkflowStructureTest(unittest.TestCase):
    def setUp(self):
        self.beta = workflow("build-beta.yml")
        self.context_job = job_block(self.beta, "release-log-context")

    def test_the_context_job_is_independent_and_nonblocking(self):
        self.assertIn("continue-on-error: true", self.context_job.split("steps:")[0])
        self.assertNotIn("needs:", self.context_job)
        self.assertIn("if: github.ref_type == 'tag'", self.context_job)
        self.assertIn("contents: read", self.context_job)
        self.assertNotIn("secrets.", self.context_job)

    def test_no_job_waits_for_the_context_job(self):
        for name in ["validate", "build", "publish"]:
            with self.subTest(job=name):
                self.assertNotIn("release-log-context", job_block(self.beta, name))

    def test_the_context_job_exports_one_retained_evidence_file(self):
        self.assertIn("make release-log/context", self.context_job)
        upload = step_block(self.context_job, "Upload release context")
        self.assertIn(UPLOAD, upload)
        self.assertIn("name: release-log-context-${{ github.run_id }}-${{ github.run_attempt }}", upload)
        self.assertIn("path: release-log/evidence.json", upload)
        self.assertIn("retention-days: 90", upload)
        for name in ["Export release context", "Upload release context"]:
            self.assertIn("continue-on-error: true", step_block(self.context_job, name))

    def test_the_beta_build_asks_for_the_build_export(self):
        build = job_block(self.beta, "build")
        self.assertIn('"export-release-log": true', build)
        self.assertNotIn("export-release-log: true", build)

    def test_release_behavior_is_untouched(self):
        self.assertIn("needs: validate", job_block(self.beta, "build"))
        self.assertIn("needs: build", job_block(self.beta, "publish"))
        self.assertIn("enable-deployment: true", job_block(self.beta, "build"))


class UnifiedWorkflowStructureTest(unittest.TestCase):
    def setUp(self):
        self.unified = workflow("build-unified.yml")
        self.build = job_block(self.unified, "build")
        self.export = step_block(self.build, "Export release log build evidence")

    def test_the_export_is_opt_in_per_matrix_entry(self):
        self.assertIn("if: matrix.export-release-log == true", self.export)
        self.assertNotIn("inputs.export-release-log", self.unified)

    def test_the_export_is_nonblocking(self):
        self.assertIn("continue-on-error: true", self.export)
        upload = step_block(self.build, "Upload release log build evidence")
        self.assertIn("continue-on-error: true", upload)
        self.assertIn("steps.release_log_export.outcome == 'success'", upload)

    def test_digests_are_taken_after_signing_and_before_the_binary_upload(self):
        order = [step_index(self.build, name) for name in [
            "Apply lineage signing to public APK artifacts", "Build AAB",
            "Move version file for consistent artifact structure",
            "Export release log build evidence", "Upload build artifacts"]]
        self.assertEqual(order, sorted(order))

    def test_the_build_evidence_is_one_retained_file_per_attempt(self):
        upload = step_block(self.build, "Upload release log build evidence")
        self.assertIn(UPLOAD, upload)
        self.assertIn("name: release-log-build-${{ matrix.flavor }}-${{ github.run_id }}-${{ github.run_attempt }}", upload)
        self.assertIn("path: release-log/evidence.json", upload)
        self.assertIn("retention-days: 90", upload)

    def test_the_export_is_one_make_call_with_no_shell_logic(self):
        run = self.export.split("run:", 1)[1]
        commands = [line.strip() for line in run.splitlines() if line.strip() and not line.strip().startswith("#")]
        self.assertEqual(len([c for c in commands if c.startswith("make release-log/build")]), 1)
        self.assertLessEqual(len(commands), 3)
        for banned in ["git rev-parse", "shopt", "apks=", "for ", "if "]:
            self.assertNotIn(banned, run)
        self.assertNotIn("${{", run)

    def test_deployment_and_binary_retention_are_unchanged(self):
        self.assertNotIn("release-log", job_block(self.unified, "deploy"))
        self.assertIn("needs: build", job_block(self.unified, "deploy"))
        upload = step_block(self.build, "Upload build artifacts")
        self.assertIn("retention-days: 7", upload)
        self.assertNotIn("release-log", upload)

    def test_only_the_release_callers_ask_for_the_export(self):
        callers = {path.name for path in WORKFLOWS.glob("*.yml") if "export-release-log" in path.read_text()}
        self.assertLessEqual(callers, {"build-unified.yml", "build-beta.yml", "build-production.yml"})


PRODUCTION = ".github/workflows/build-production.yml"


class ProductionWorkflowStructureTest(unittest.TestCase):
    def setUp(self):
        self.text = workflow("build-production.yml")
        self.context_job = job_block(self.text, "release-log-context")

    def test_the_context_job_is_independent_and_nonblocking(self):
        header = self.context_job.split("steps:")[0]
        self.assertIn("continue-on-error: true", header)
        self.assertNotIn("needs:", self.context_job)
        self.assertIn("github.event_name == 'release'", header)
        self.assertIn("!contains(github.event.release.tag_name, '-')", header)
        self.assertIn("contents: read", self.context_job)
        self.assertNotIn("secrets.", self.context_job)

    def test_the_context_job_keeps_the_name_the_reporter_looks_for(self):
        self.assertIn("name: Export release log context", self.context_job.split("steps:")[0])

    def test_no_job_waits_for_the_context_job(self):
        for name in ["validate-release", "build-production", "build-fdroid"]:
            with self.subTest(job=name):
                self.assertNotIn("release-log-context", job_block(self.text, name))

    def test_the_context_job_exports_one_retained_evidence_file(self):
        self.assertIn("make release-log/context", self.context_job)
        self.assertIn('event="$GITHUB_EVENT_PATH"', self.context_job)
        checkout = step_block(self.context_job, "Checkout tagged commit")
        self.assertIn("persist-credentials: false", checkout)
        upload = step_block(self.context_job, "Upload release context")
        self.assertIn(UPLOAD, upload)
        self.assertIn("name: release-log-context-${{ github.run_id }}-${{ github.run_attempt }}", upload)
        self.assertIn("path: release-log/evidence.json", upload)
        self.assertIn("retention-days: 90", upload)
        for name in ["Export release context", "Upload release context"]:
            self.assertIn("continue-on-error: true", step_block(self.context_job, name))

    def test_only_the_prod_entry_asks_for_the_build_export(self):
        production = job_block(self.text, "build-production")
        self.assertIn('"export-release-log": true', production)
        self.assertNotIn("export-release-log: true", production)
        self.assertNotIn("export-release-log", job_block(self.text, "build-fdroid"))
        self.assertEqual(self.text.count("export-release-log"), 1)

    def test_the_export_input_is_a_matrix_key_not_a_workflow_input(self):
        self.assertNotIn("inputs.export-release-log", workflow("build-unified.yml"))

    def test_release_behavior_is_untouched(self):
        self.assertRegex(self.text, r"(?m)^on:\n  release:\n    types: \[ published \]\n")
        self.assertIn("if: ${{ !contains(github.event.release.tag_name, '-') }}", job_block(self.text, "validate-release"))
        for name in ["build-production", "build-fdroid"]:
            block = job_block(self.text, name)
            self.assertIn("needs: validate-release", block)
            self.assertIn("enable-deployment: true", block)
            self.assertIn("secrets: inherit", block)
        order = ["s3", "google-play", "github-release"]
        positions = [job_block(self.text, "build-production").index('"type": "%s"' % kind) for kind in order]
        self.assertEqual(positions, sorted(positions))
        self.assertIn('"additional-files": "app/build/outputs/version.txt"', self.text)


class QaSkipCommandTest(unittest.TestCase):
    def run_main(self, **env):
        values = {"GITHUB_REPOSITORY": "wireapp/wire-android", "GITHUB_RUN_ID": "1520", "GITHUB_RUN_ATTEMPT": "2"}
        values.update(env)
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "qa" / "skip.json"
            with patch.dict(os.environ, values, clear=True), redirect_stderr(StringIO()):
                status = export.main(["qa-skip", "--output", str(output)])
            return status, (contract.read_json(output) if output.exists() else None)

    def test_it_writes_the_marker_of_this_run_and_attempt(self):
        status, marker = self.run_main()
        self.assertEqual(status, 0)
        self.assertEqual(marker, contract.skip_marker("wireapp/wire-android", 1520, 2))

    def test_it_fails_without_writing_when_the_run_identity_is_bad(self):
        for change in [dict(GITHUB_RUN_ID="x"), dict(GITHUB_RUN_ID="0"), dict(GITHUB_RUN_ATTEMPT=""),
                       dict(GITHUB_REPOSITORY="")]:
            with self.subTest(change=change):
                status, marker = self.run_main(**change)
                self.assertEqual((status, marker), (1, None))


class MakeTargetTest(unittest.TestCase):
    def dry_run(self, *arguments):
        return subprocess.run(["make", "-n", *arguments], cwd=ROOT, capture_output=True, text=True)

    def test_context_and_build_targets_are_thin_wrappers(self):
        result = self.dry_run("release-log/context", "event=e.json", "output=o.json")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("release_log_export.py context", result.stdout)
        self.assertIn('--event "e.json"', result.stdout)
        result = self.dry_run("release-log/build", "event=e.json", "flavor=Beta", "variant=Release", "output=o.json")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("release_log_export.py build", result.stdout)
        self.assertIn('--flavor "Beta" --variant "Release"', result.stdout)

    def test_the_manifest_target_is_gone(self):
        self.assertNotEqual(self.dry_run("release-log/manifest").returncode, 0)

    def test_help_lists_the_targets(self):
        help_text = subprocess.run(["make", "help"], cwd=ROOT, capture_output=True, text=True).stdout
        for target in ["release-log/context", "release-log/build"]:
            self.assertIn(target, help_text)
        self.assertNotIn("release-log/manifest", help_text)


if __name__ == "__main__":
    unittest.main()
