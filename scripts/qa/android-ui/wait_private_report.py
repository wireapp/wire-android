#!/usr/bin/env python3
"""Wait for the dispatched publisher, tolerating temporary GitHub API failures."""

import json
import subprocess
import sys
import time


def wait_for_report(repository, run_id, timeout=300):
    # The workflow step bounds total publication time; this deadline bounds API outages only.
    deadline = None
    while True:
        try:
            response = subprocess.run(
                ["gh", "api", f"repos/{repository}/actions/runs/{run_id}"],
                check=True, capture_output=True, text=True, timeout=45,
            )
            run = json.loads(response.stdout)
            if run.get("id") != int(run_id):
                raise ValueError("Publisher returned a different workflow run")
            deadline = None
            if run.get("status") == "completed":
                if run.get("conclusion") != "success":
                    raise RuntimeError(f"Private publisher finished: {run.get('conclusion')}")
                return
        except (subprocess.CalledProcessError, subprocess.TimeoutExpired, json.JSONDecodeError) as error:
            now = time.monotonic()
            if deadline is None:
                deadline = now + timeout
            if now >= deadline:
                raise TimeoutError("Timed out checking private publication status") from error
            print(getattr(error, "stderr", None) or str(error), file=sys.stderr)
            print("Publisher status temporarily unavailable; checking again.", file=sys.stderr)
        time.sleep(10 if deadline is None else min(10, max(0, deadline - time.monotonic())))


if __name__ == "__main__":
    wait_for_report(*sys.argv[1:])
