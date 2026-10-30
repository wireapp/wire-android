#!/usr/bin/env python3
"""Prepare Prod's Testiny run before parallel instrumentation starts."""

from __future__ import annotations

import json
import os
import re
import urllib.error
import urllib.request
import uuid
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo


API_URL = "https://app.testiny.io/api/v1"
PROJECT_ID = 8  # Wire Android; matches TestinyApi.kt.


def resolve_name(override: str, build: str, run_number: str, now: datetime | None = None) -> str:
    name = override.strip()
    if not name:
        if not re.fullmatch(r"\d+\.\d+\.\d+-\d+", build) or not run_number.isdigit():
            raise ValueError("Cannot name the Testiny run: missing APK version or CI run number.")
        date = (now or datetime.now(ZoneInfo("Europe/Berlin"))).astimezone(ZoneInfo("Europe/Berlin"))
        name = f"v{build}-prod-regression-{date:%d/%m/%Y}-run-{run_number}"
    if any(ord(character) < 32 or ord(character) == 127 for character in name):
        raise ValueError("Testiny run names must not contain control characters.")
    return name


def post(path: str, payload: dict, api_key: str) -> dict:
    request = urllib.request.Request(
        f"{API_URL}/{path}",
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json", "Accept": "application/json", "X-Api-Key": api_key},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            result = json.load(response)
            if not isinstance(result, dict):
                raise ValueError("Invalid response")
            return result
    except urllib.error.HTTPError as error:
        raise ValueError(f"Testiny preparation failed (HTTP {error.code}).") from None
    except (OSError, ValueError):
        raise ValueError("Testiny preparation failed: unavailable service or invalid response.") from None


def prepare_run(name: str, api_key: str) -> int:
    # Reuse an open run with this title; create one only when none exists.
    response = post("testrun/find", {"filter": {"project_id": PROJECT_ID, "title": name, "is_closed": False}}, api_key)
    runs = response.get("data")
    if not isinstance(runs, list):
        raise ValueError("Testiny lookup returned an invalid run list.")
    if runs:
        if not isinstance(runs[0], dict):
            raise ValueError("Testiny lookup returned an invalid run.")
        run_id = runs[0].get("id")
    else:
        run_id = post("testrun", {
            "id": 1, "title": name, "is_deleted": False, "project_id": PROJECT_ID,
            "testplan_id": 0, "is_closed": False, "description": "",
        }, api_key).get("id")
    if isinstance(run_id, bool) or not isinstance(run_id, int) or run_id <= 0:
        raise ValueError("Testiny preparation did not return a valid run ID.")
    return run_id


def main() -> None:
    try:
        name = resolve_name(
            os.environ.get("TESTINY_RUN_NAME_INPUT", ""),
            os.environ.get("REAL_BUILD_NUMBER", ""),
            os.environ.get("GITHUB_RUN_NUMBER", ""),
        )
        # Read the host-only API key from runtime secrets
        secrets = json.loads(Path(os.environ["SECRETS_JSON_PATH"]).read_text(encoding="utf-8"))
        api_key = secrets["TESTINY_API_KEY_ANDROID"]["fields"]["password"]["value"]
        if not isinstance(api_key, str) or not api_key.strip() or any(ord(c) < 32 for c in api_key):
            raise ValueError("Testiny API key is missing or invalid in runtime secrets.")
        # Match existing runtime masking; never persist the key in step outputs.
        print(f"::add-mask::{api_key}")
        # Prepare one shared run before parallel devices try to report results.
        run_id = prepare_run(name, api_key)
        delimiter = f"EOF_{uuid.uuid4().hex}"
        # Pass the resolved title to instrumentation so every device uses the same run.
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
            output.write(f"testinyRunName<<{delimiter}\n{name}\n{delimiter}\n")
        print(f"Testiny run {run_id} is ready.")
    except (KeyError, TypeError, OSError, json.JSONDecodeError):
        raise SystemExit("ERROR: Testiny preparation failed: check runtime secrets and CI configuration.") from None
    except ValueError as error:
        raise SystemExit(f"ERROR: {error}") from None


if __name__ == "__main__":
    main()
