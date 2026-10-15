#!/usr/bin/env python3
"""Print the source commit only if it is a live release-cycle branch tip."""

import base64
import os
import subprocess
import sys


def git(*args, env=None):
    return subprocess.run(["git", *args], env=env, capture_output=True, text=True)


def main():
    branch = os.environ.get("SOURCE_BRANCH")
    if branch is not None and not branch.startswith("release/cycle-"):
        sys.exit("Select a release-cycle branch named release/cycle-*")

    source = os.environ.get("SOURCE_COMMIT", "HEAD")
    resolved = git("rev-parse", "--verify", "--end-of-options", source + "^{commit}")
    if resolved.returncode:
        sys.exit("Cannot resolve the beta source commit")
    commit = resolved.stdout.strip()

    # Checkout deliberately does not persist credentials. Supply an optional
    # GitHub token only to this read operation, without printing or saving it.
    env = dict(os.environ, GIT_TERMINAL_PROMPT="0")
    token = env.get("GH_TOKEN")
    if token:
        index = int(env.get("GIT_CONFIG_COUNT", "0"))
        env["GIT_CONFIG_COUNT"] = str(index + 1)
        env[f"GIT_CONFIG_KEY_{index}"] = "http.https://github.com/.extraheader"
        credentials = base64.b64encode(("x-access-token:" + token).encode()).decode()
        env[f"GIT_CONFIG_VALUE_{index}"] = "AUTHORIZATION: basic " + credentials
    heads = git("ls-remote", "--heads", "origin", "refs/heads/release/cycle-*", env=env)
    if heads.returncode:
        sys.exit("Cannot read current release-cycle branch tips from the remote")

    for line in heads.stdout.splitlines():
        sha, ref = line.split()
        if sha == commit and (branch is None or ref == "refs/heads/" + branch):
            print(commit)
            return
    target = branch if branch is not None else "a release/cycle-* branch"
    sys.exit(f"Beta source must equal the current remote tip of {target} (release-cycle branch)")


if __name__ == "__main__":
    main()
