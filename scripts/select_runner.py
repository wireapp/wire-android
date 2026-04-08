#!/usr/bin/env python3
"""
GitHub Actions runner selector.

Queries repository-level and (optionally) organization-level self-hosted runners,
then outputs a JSON array of runner labels based on idle availability.

Environment variables (all required unless noted):
  RUNNER_TOKEN          - PAT with repo + admin:org scopes (or fine-grained equivalents)
  GITHUB_REPOSITORY     - owner/repo (set automatically by Actions)
  REQUIRED_LABELS       - JSON array of labels the preferred runner must have
  MIN_IDLE_RUNNERS      - minimum idle runners needed to prefer self-hosted (default: 1)
  FALLBACK_LABEL        - runner label to use when no idle match found (default: ubuntu-latest)

Output:
  stdout  - JSON array of selected runner labels (consumed by the workflow)
  stderr  - informational logs (safe to print; token is never logged)

Exit codes:
  0  - always exits 0; falls back gracefully on any error
"""

import json
import os
import sys
import urllib.error
import urllib.request


def _api_get(url: str, token: str) -> dict:
    req = urllib.request.Request(url)
    req.add_header("Authorization", f"Bearer {token}")
    req.add_header("Accept", "application/vnd.github+json")
    req.add_header("X-GitHub-Api-Version", "2022-11-28")
    req.add_header("User-Agent", "wire-android-ci/select-runner")
    with urllib.request.urlopen(req, timeout=15) as response:
        return json.loads(response.read().decode())


def _fetch_runners(url: str, token: str, label: str) -> list:
    try:
        data = _api_get(url, token)
        runners = data.get("runners", [])
        print(f"INFO: {label}: {len(runners)} runner(s) found", file=sys.stderr)
        return runners
    except urllib.error.HTTPError as exc:
        print(f"WARNING: {label}: HTTP {exc.code} - {exc.reason} (skipping)", file=sys.stderr)
        return []
    except Exception as exc:
        print(f"WARNING: {label}: {type(exc).__name__} (skipping)", file=sys.stderr)
        return []


def main() -> None:
    token = os.environ.get("RUNNER_TOKEN", "")
    repository = os.environ.get("GITHUB_REPOSITORY", "/")
    owner, _, repo = repository.partition("/")
    required_labels: list = json.loads(os.environ.get("REQUIRED_LABELS", "[]"))
    min_idle: int = int(os.environ.get("MIN_IDLE_RUNNERS", "1"))
    fallback_label: str = os.environ.get("FALLBACK_LABEL", "ubuntu-latest")
    fallback = [fallback_label]

    if not token:
        print("WARNING: RUNNER_TOKEN is empty — falling back", file=sys.stderr)
        print(json.dumps(fallback))
        return

    base = "https://api.github.com"

    all_runners = _fetch_runners(
        f"{base}/repos/{owner}/{repo}/actions/runners",
        token,
        "repo-level runners",
    )
    all_runners += _fetch_runners(
        f"{base}/orgs/{owner}/actions/runners",
        token,
        "org-level runners",
    )

    print(f"INFO: Total runners available: {len(all_runners)}", file=sys.stderr)

    required_set = set(required_labels)
    idle_count = 0
    for runner in all_runners:
        name = runner.get("name", "?")
        labels = {lbl["name"] for lbl in runner.get("labels", [])}
        status = runner.get("status")
        busy = runner.get("busy", True)

        if status != "online" or busy:
            print(f"INFO: Skipping '{name}' — not idle online (status={status}, busy={busy})", file=sys.stderr)
            continue

        missing = required_set - labels
        if missing:
            print(f"INFO: Skipping '{name}' — missing labels: {sorted(missing)}", file=sys.stderr)
            continue

        print(f"INFO: '{name}' matches all required labels and is idle", file=sys.stderr)
        idle_count += 1

    print(f"INFO: Idle matching runners: {idle_count} / minimum required: {min_idle}", file=sys.stderr)

    selected = required_labels if idle_count >= min_idle else fallback
    print(f"INFO: Selected runner pool: {json.dumps(selected)}", file=sys.stderr)
    print(json.dumps(selected))


if __name__ == "__main__":
    main()
