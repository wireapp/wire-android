#!/usr/bin/env python3
"""Structure tests for the release log reporter workflow, Make entrypoints, and documentation."""

from __future__ import annotations

import re
import subprocess
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from test_release_log_export import ROOT, WORKFLOWS, job_block, step_block, step_index, workflow

CHECKOUT = "actions/checkout@d23441a48e516b6c34aea4fa41551a30e30af803"


def run_bodies(text):
    """Return the shell text of every `run:` block, without the surrounding YAML."""
    bodies, lines = [], text.splitlines()
    for index, line in enumerate(lines):
        match = re.match(r"^(\s*)(?:- )?run:\s*(.*)$", line)
        if not match:
            continue
        if match.group(2) not in ("|", ">", "|-", ">-"):
            bodies.append(match.group(2))
            continue
        indent, body = len(match.group(1)), []
        for follower in lines[index + 1:]:
            if follower.strip() and len(follower) - len(follower.lstrip()) <= indent:
                break
            body.append(follower)
        bodies.append("\n".join(body))
    return bodies


class ReporterWorkflowTest(unittest.TestCase):
    def setUp(self):
        self.text = workflow("release-log.yml")
        self.job = job_block(self.text, "report")

    def test_it_runs_when_a_release_workflow_completes_or_by_hand(self):
        self.assertRegex(self.text, r"(?m)^name: \"?Release log\"?$")
        self.assertRegex(self.text, r"(?m)^  workflow_run:\n    workflows: \[.*\"Beta release\".*\]\n    types: \[completed\]")
        self.assertRegex(self.text, r"(?m)^  workflow_dispatch:")
        for trigger in ["pull_request", "pull_request_target", "push:", "issue_comment", "schedule"]:
            with self.subTest(trigger=trigger):
                self.assertNotRegex(self.text.split("jobs:")[0], r"(?m)^  " + trigger)

    def test_it_also_runs_when_the_production_workflow_completes(self):
        listed = re.search(r"workflows: \[(.*)\]", self.text).group(1)
        self.assertEqual(re.findall(r'"([^"]+)"', listed), ["Beta release", "Production"])

    def test_every_listed_workflow_name_exists(self):
        listed = re.search(r"workflows: \[(.*)\]", self.text).group(1)
        names = {path.read_text().split("\n", 1)[0].replace("name:", "").strip().strip('"')
                 for path in WORKFLOWS.glob("*.yml") if path.name != "release-log.yml"}
        for name in re.findall(r'"([^"]+)"', listed):
            with self.subTest(name=name):
                self.assertIn(name, names)

    def test_permissions_are_read_only(self):
        header = self.text.split("jobs:")[0]
        self.assertRegex(header, r"(?m)^permissions:\n  contents: read\n  actions: read\n")
        self.assertNotIn("write", self.text)

    def test_the_job_uses_the_protected_environment_and_default_branch_code(self):
        self.assertIn("environment: release-log", self.job)
        self.assertIn("runs-on: ubuntu-latest", self.job)
        self.assertRegex(self.job, r"timeout-minutes: \d+")
        checkout = step_block(self.job, "Checkout")
        self.assertIn(CHECKOUT, checkout)
        self.assertIn("persist-credentials: false", checkout)
        self.assertNotRegex(checkout, r"\n\s+ref:")

    def test_failed_and_cancelled_source_runs_are_reported_too(self):
        self.assertNotRegex(self.job.split("steps:")[0], r"\n    if:.*conclusion")
        self.assertNotIn("continue-on-error", self.job.split("steps:")[0])

    def test_untrusted_values_reach_the_shell_only_through_the_environment(self):
        for body in run_bodies(self.text):
            with self.subTest(body=body[:40]):
                self.assertNotIn("${{", body)

    def test_secrets_are_read_only_by_the_publish_step(self):
        publish = step_block(self.job, "Publish to the release log")
        self.assertIn("secrets.RELEASE_LOG_WEBHOOK_URL", publish)
        self.assertIn("secrets.RELEASE_LOG_WEBHOOK_TOKEN", publish)
        self.assertIn("vars.RELEASE_LOG_PUBLICATION_ENABLED", publish)
        self.assertEqual(self.text.count("secrets."), 2)

    def test_steps_collect_then_publish_then_fail_on_incomplete_evidence(self):
        order = [step_index(self.job, name) for name in
                 ["Collect release evidence", "Publish to the release log", "Fail when the evidence is incomplete"]]
        self.assertEqual(order, sorted(order))
        collect = step_block(self.job, "Collect release evidence")
        self.assertIn("id: collect", collect)
        self.assertIn("continue-on-error: true", collect)
        self.assertIn("make release-log/collect", collect)
        publish = step_block(self.job, "Publish to the release log")
        self.assertIn("always()", publish)
        self.assertIn("make release-log/publish", publish)
        self.assertIn('summary="$GITHUB_STEP_SUMMARY"', publish)
        self.assertIn("steps.collect.outcome == 'failure'", step_block(self.job, "Fail when the evidence is incomplete"))

    def test_the_run_identity_comes_from_the_event_or_the_manual_inputs(self):
        collect = step_block(self.job, "Collect release evidence")
        self.assertIn("github.event.workflow_run.id || inputs.run_id", collect)
        self.assertIn("github.event.workflow_run.run_attempt || inputs.run_attempt", collect)
        self.assertRegex(self.text, r"(?m)^      run_id:\n(?:.*\n)*?        required: true")

    def test_reports_do_not_cancel_each_other(self):
        group = re.search(r"(?m)^concurrency:\n  group: (.*)$", self.text).group(1)
        self.assertTrue(group.startswith("release-log-"))
        self.assertIn("github.event.workflow_run.run_attempt || inputs.run_attempt", group)
        self.assertIn("cancel-in-progress: false", self.text)

    def test_only_default_branch_code_gets_the_secrets(self):
        header = self.job.split("steps:")[0]
        self.assertIn("if: github.ref == format('refs/heads/{0}', github.event.repository.default_branch)", header)


class PrereleaseSkipTest(unittest.TestCase):
    def setUp(self):
        text = (WORKFLOWS / "release-log.yml").read_text()
        self.condition = re.search(r"(?m)^    if: (.*)$", job_block(text, "report")).group(1)

    def test_production_runs_of_prerelease_tags_are_skipped(self):
        self.assertRegex(
            self.condition,
            r"&& !\(github\.event\.workflow_run\.name == 'Production' "
            r"&& contains\(github\.event\.workflow_run\.head_branch, '-'\)\)")

    def test_the_default_branch_guard_stays(self):
        self.assertIn("github.ref == format('refs/heads/{0}', github.event.repository.default_branch)",
                      self.condition)


class MakeEntrypointTest(unittest.TestCase):
    def dry_run(self, *arguments):
        return subprocess.run(["make", "-n", *arguments], cwd=ROOT, capture_output=True, text=True)

    def test_collect_and_publish_are_thin_wrappers(self):
        result = self.dry_run("release-log/collect", "run_id=1", "run_attempt=2", "output=payload.json")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('release_log_report.py collect --run-id "1" --run-attempt "2" --output "payload.json"',
                      result.stdout)
        result = self.dry_run("release-log/publish", "payload=payload.json", "summary=s.md")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('release_log_report.py publish --payload "payload.json" --summary "s.md"', result.stdout)
        result = self.dry_run("release-log/publish", "payload=payload.json", "dry_run=1")
        self.assertIn("--dry-run", result.stdout)

    def test_help_lists_the_targets(self):
        help_text = subprocess.run(["make", "help"], cwd=ROOT, capture_output=True, text=True).stdout
        for target in ["release-log/collect", "release-log/publish"]:
            self.assertIn(target, help_text)


class DocumentationTest(unittest.TestCase):
    def test_the_guide_names_every_setting_and_the_recovery_path(self):
        text = (ROOT / "docs" / "release-log.md").read_text()
        for term in ["release-log", "RELEASE_LOG_WEBHOOK_URL", "RELEASE_LOG_WEBHOOK_TOKEN",
                     "RELEASE_LOG_PUBLICATION_ENABLED", "X-Automation-Webhook-Token",
                     "workflow_dispatch", "make release-log/publish"]:
            with self.subTest(term=term):
                self.assertIn(term, text)

    def test_the_public_docs_hold_no_confluence_internals(self):
        for path in [ROOT / "docs" / "release-log.md", ROOT / "docs" / "adr" / "0020-android-release-log.md"]:
            text = path.read_text()
            for term in ["3448602704", "space `AR`", "CQL", "cqlResults", "webhook.body", "Release log Android",
                         "Release logs"]:
                with self.subTest(path=path.name, term=term):
                    self.assertNotIn(term, text)

    def test_the_guide_contains_no_secret_values(self):
        text = (ROOT / "docs" / "release-log.md").read_text()
        self.assertNotRegex(text, r"https://api-private\.atlassian\.com")
        self.assertNotRegex(text, r"[0-9a-f]{32,}")


if __name__ == "__main__":
    unittest.main()
