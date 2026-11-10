#!/usr/bin/env python3
"""Validate a manual-deflake source run against GitHub Actions metadata."""

from __future__ import annotations

import json
import os
import re
import urllib.parse
import urllib.request
import uuid
from typing import Any


ALLOWED_WORKFLOW_FILES = {
    ".github/workflows/qa-android-prod-tests.yml",
    ".github/workflows/qa-android-beta-tests.yml",
    ".github/workflows/qa-android-emulator-critical-flow-tests.yml",
    ".github/workflows/qa-android-regression-tests.yml",
    ".github/workflows/qa-android-fdroid-tests.yml",
}
ALLOWED_CONCLUSIONS = {"success", "failure"}


class SourceRunValidationError(ValueError):
    """Raised when the selected run is not a trusted deflake source."""


def validate_run_metadata(
    data: dict[str, Any],
    expected_repository: str,
    expected_default_branch: str,
    expected_workflow_file: str = ".github/workflows/qa-android-regression-tests.yml",
) -> dict[str, str]:
    repository = str((data.get("repository") or {}).get("full_name") or "")
    workflow_file = str(data.get("path") or "").split("@", 1)[0]
    head_sha = str(data.get("head_sha") or "")
    head_branch = str(data.get("head_branch") or "")
    status = str(data.get("status") or "")
    conclusion = str(data.get("conclusion") or "")
    run_id = str(data.get("id") or "")

    if repository != expected_repository:
        raise SourceRunValidationError("Selected run belongs to another repository")
    if expected_workflow_file not in ALLOWED_WORKFLOW_FILES or workflow_file != expected_workflow_file:
        raise SourceRunValidationError(f"Workflow is not an allowed deflake source: {workflow_file}")
    if head_branch != expected_default_branch:
        raise SourceRunValidationError("Selected run did not execute from the default branch")
    if status != "completed" or conclusion not in ALLOWED_CONCLUSIONS:
        raise SourceRunValidationError("Selected run is not a completed success/failure run")
    if not re.fullmatch(r"[0-9a-f]{40}", head_sha):
        raise SourceRunValidationError("Selected run has an invalid head SHA")
    if not run_id.isdigit():
        raise SourceRunValidationError("Selected run has an invalid run id")

    return {
        "sourceRunId": run_id,
        "sourceRepository": repository,
        "sourceWorkflowFile": workflow_file,
        "sourceHeadSha": head_sha,
        "sourceHeadBranch": head_branch,
    }


def write_output(name: str, value: str) -> None:
    output_path = os.environ.get("GITHUB_OUTPUT", "").strip()
    if not output_path:
        return
    delimiter = f"EOF_{uuid.uuid4().hex}"
    with open(output_path, "a", encoding="utf-8") as output_file:
        output_file.write(f"{name}<<{delimiter}\n{value}\n{delimiter}\n")


def main() -> None:
    repository = os.environ["GITHUB_REPOSITORY"]
    run_id = os.environ["SOURCE_RUN_ID"].strip()
    default_branch = os.environ["DEFAULT_BRANCH"].strip()
    token = os.environ["GITHUB_TOKEN"]
    api_url = os.environ.get("GITHUB_API_URL", "https://api.github.com").rstrip("/")
    expected_workflow_file = os.environ.get(
        "EXPECTED_SOURCE_WORKFLOW_FILE", ".github/workflows/qa-android-regression-tests.yml"
    )
    allow_latest = os.environ.get("ALLOW_LATEST_SOURCE_RUN", "false") == "true"
    if not run_id.isdigit() and (run_id or not allow_latest):
        raise SystemExit("ERROR: sourceRunId must be numeric.")

    request_url = f"{api_url}/repos/{repository}/actions/runs/{run_id}"
    if not run_id:
        if expected_workflow_file not in ALLOWED_WORKFLOW_FILES:
            raise SystemExit("ERROR: Workflow is not an allowed deflake source.")
        workflow = urllib.parse.quote(expected_workflow_file.rsplit("/", 1)[-1], safe="")
        query = urllib.parse.urlencode({"branch": default_branch, "status": "completed", "per_page": 1})
        request_url = f"{api_url}/repos/{repository}/actions/workflows/{workflow}/runs?{query}"

    request = urllib.request.Request(
        request_url,
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            data = json.load(response)
        if not run_id:
            runs = data.get("workflow_runs") or []
            if not runs:
                raise SourceRunValidationError("No completed source runs found on the default branch")
            data = runs[0]
        outputs = validate_run_metadata(data, repository, default_branch, expected_workflow_file)
    except (OSError, json.JSONDecodeError, SourceRunValidationError) as error:
        raise SystemExit(f"ERROR: Could not validate source run: {error}") from error

    for name, value in outputs.items():
        write_output(name, value)
    print(f"Deflake source run: {outputs['sourceRunId']}")


if __name__ == "__main__":
    main()
