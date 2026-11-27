#!/usr/bin/env python3
"""Assign the Android fix version to Jira issues brought in by a Kalium ref update."""

import base64
from contextlib import closing
from http.client import HTTPException, HTTPSConnection
import json
import os
from pathlib import Path
import re
import subprocess
import sys


KALIUM_REPO = "wireapp/kalium"
JIRA_HOST = "wearezeta.atlassian.net"
ANDROID_COORDINATES = Path("build-logic/plugins/src/main/kotlin/AndroidCoordinates.kt")
ISSUE_KEY = re.compile(r"\b[A-Z][A-Z0-9]{0,9}-[0-9]+\b")
JIRA_LINK = re.compile(rf"https://{re.escape(JIRA_HOST)}/browse/({ISSUE_KEY.pattern})")
JIRA_DESCRIPTION = re.compile(
    r"<!--jira-description-action-hidden-marker-start-->(.*?)"
    r"<!--jira-description-action-hidden-marker-end-->",
    re.DOTALL,
)


class JiraRequestError(Exception):
    pass


def kalium_ref(android_ref):
    return subprocess.check_output(
        ["git", "rev-parse", "--verify", f"{android_ref}:kalium"], text=True
    ).strip()


def github_pages(endpoint):
    output = subprocess.check_output(
        ["gh", "api", "--paginate", "--slurp", f"repos/{KALIUM_REPO}/{endpoint}?per_page=100"],
        text=True,
    )
    return json.loads(output)


def pull_request_keys(commit_sha):
    for page in github_pages(f"commits/{commit_sha}/pulls"):
        for pull_request in page:
            if not pull_request["merged_at"] or pull_request["merge_commit_sha"] != commit_sha:
                continue

            # The Jira check writes the verified key here, including keys found only in the branch name.
            jira_description = JIRA_DESCRIPTION.search(pull_request["body"] or "")
            if jira_description:
                links = JIRA_LINK.findall(jira_description[1])
                if links:
                    yield from links
                    continue

            yield from ISSUE_KEY.findall(pull_request["title"]) or ISSUE_KEY.findall(
                pull_request["head"]["ref"]
            )


def introduced_issues(previous_ref, current_ref):
    issues = set()
    for page in github_pages(f"compare/{previous_ref}...{current_ref}"):
        for commit in page["commits"]:
            subject = commit["commit"]["message"].partition("\n")[0]
            issues.update(ISSUE_KEY.findall(subject) or pull_request_keys(commit["sha"]))
    return sorted(issues)


def jira_request(issue, authorization, update=None):
    if not ISSUE_KEY.fullmatch(issue):
        raise JiraRequestError("Invalid Jira issue key")

    path = f"/rest/api/3/issue/{issue}"
    headers = {"Authorization": authorization, "Accept": "application/json"}
    method = "GET"
    body = None
    if update is None:
        path += "?fields=fixVersions"
    else:
        method = "PUT"
        body = json.dumps(update).encode()
        headers["Content-Type"] = "application/json"

    with closing(HTTPSConnection(JIRA_HOST, timeout=30)) as connection:
        connection.request(method, path, body=body, headers=headers)
        with connection.getresponse() as response:
            if not 200 <= response.status < 300:
                error = response.read().decode(errors="replace")
                raise JiraRequestError(f"HTTP {response.status}: {error}")
            if update is None:
                return json.load(response)["fields"]["fixVersions"]


def add_fix_version(issue, version, authorization):
    versions = jira_request(issue, authorization)
    if any(existing["name"].strip().casefold() == "not applicable" for existing in versions):
        print(f"{issue} is marked Not applicable")
    elif any(existing["name"] == version for existing in versions):
        print(f"{issue} already has {version}")
    else:
        # Jira's add operation preserves fix versions for other clients and releases.
        jira_request(issue, authorization, {"update": {"fixVersions": [{"add": {"name": version}}]}})
        print(f"Added {version} to {issue}")


def main(previous_android_ref):
    previous_ref = kalium_ref(previous_android_ref)
    current_ref = kalium_ref("HEAD")
    if previous_ref == current_ref:
        print(f"Android still uses Kalium {current_ref}")
        return 0

    print(f"Checking Kalium changes from {previous_ref} to {current_ref}", flush=True)
    issues = introduced_issues(previous_ref, current_ref)
    if not issues:
        print("No Jira tickets in the newly included Kalium commits")
        return 0

    version_name = re.search(
        r'^\s*const val versionName\s*=\s*"([^"]+)"',
        ANDROID_COORDINATES.read_text(),
        re.MULTILINE,
    )
    if not version_name:
        sys.exit(f"Could not read the Android version from {ANDROID_COORDINATES}")
    version = f"Android {version_name[1]}"

    token = os.environ.get("JIRA_TOKEN")
    if not token:
        sys.exit("JIRA_TOKEN is required to assign Android fix versions")
    authorization = "Basic " + base64.b64encode(token.encode()).decode()

    failed = False
    for issue in issues:
        try:
            add_fix_version(issue, version, authorization)
        except (JiraRequestError, HTTPException, OSError) as error:
            print(f"Could not assign {version} to {issue}: {error}", file=sys.stderr)
            failed = True
    return int(failed)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
