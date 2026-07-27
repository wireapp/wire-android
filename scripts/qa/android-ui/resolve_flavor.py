#!/usr/bin/env python3
"""Resolve flavor config from flavors.json and export workflow env vars."""

import json
import os
import sys

flavor = (os.environ.get("FLAVOR_INPUT") or "").strip()
cfg_path = os.environ.get("FLAVORS_CONFIG_PATH") or "/etc/android-qa/flavors.json"

if not cfg_path:
    print("ERROR: FLAVORS_CONFIG_PATH not set", file=sys.stderr)
    sys.exit(1)

if not os.path.isfile(cfg_path):
    print(f"ERROR: Missing flavors config on runner: {cfg_path}", file=sys.stderr)
    sys.exit(1)

try:
    with open(cfg_path, "r", encoding="utf-8") as handle:
        cfg = json.load(handle)
except Exception as exc:
    print(f"ERROR: Failed to read {cfg_path}: {exc}", file=sys.stderr)
    sys.exit(1)

flavors = cfg.get("flavors") or {}
packages = cfg.get("packagesToUninstall")

if flavor not in flavors:
    print(f"ERROR: Flavor '{flavor}' not found in {cfg_path}", file=sys.stderr)
    sys.exit(1)

entry = flavors.get(flavor) or {}
s3 = (entry.get("s3Folder") or "").strip()
app = (entry.get("appId") or "").strip()

if not s3 or not app:
    print(f"ERROR: Flavor '{flavor}' missing s3Folder/appId in {cfg_path}", file=sys.stderr)
    sys.exit(1)

if packages is None:
    pkgs = []
elif isinstance(packages, list) and all(isinstance(x, str) for x in packages):
    pkgs = [x.strip() for x in packages if x.strip()]
else:
    print(f"ERROR: 'packagesToUninstall' must be an array of strings in {cfg_path}", file=sys.stderr)
    sys.exit(1)

env_path = os.environ.get("GITHUB_ENV")
if not env_path:
    print("ERROR: GITHUB_ENV not set", file=sys.stderr)
    sys.exit(1)

with open(env_path, "a", encoding="utf-8") as handle:
    # Export variables used by downstream setup/install workflow steps.
    handle.write(f"S3_FOLDER={s3}\n")
    handle.write(f"APP_ID={app}\n")
    handle.write("PACKAGES_TO_UNINSTALL=" + " ".join(pkgs) + "\n")
