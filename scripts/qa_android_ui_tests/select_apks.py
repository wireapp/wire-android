#!/usr/bin/env python3
"""Resolve NEW/OLD APK S3 keys from workflow inputs and print env-style outputs."""

import json
import os
import re
import sys

runner_temp = os.environ.get("RUNNER_TEMP")
if not runner_temp:
    print("ERROR: RUNNER_TEMP not set", file=sys.stderr)
    sys.exit(1)

keys_path = os.path.join(runner_temp, "apk_keys.json")
try:
    with open(keys_path, "r", encoding="utf-8") as handle:
        data = json.load(handle)
except Exception:
    data = []

if not isinstance(data, list):
    data = []

apks = [k for k in data if isinstance(k, str) and k.lower().endswith(".apk")]
if not apks:
    print("ERROR: No .apk files found in this prefix.", file=sys.stderr)
    sys.exit(1)

app_build = (os.environ.get("APP_BUILD_NUMBER") or "").strip()
is_upgrade = (os.environ.get("IS_UPGRADE", "false").strip().lower() == "true")
old_input = (os.environ.get("OLD_BUILD_NUMBER") or "").strip()


def parse_version(fname: str):
    # Parse supported Wire naming schemes into sortable tuples.
    m = re.search(r"-v(\d+)\.(\d+)\.(\d+)-(\d+)", fname)
    if m:
        return (int(m.group(1)), int(m.group(2)), int(m.group(3)), int(m.group(4)))
    m = re.search(r"-v(\d+)\.(\d+)\.(\d+)-fdroid", fname)
    if m:
        return (int(m.group(1)), int(m.group(2)), int(m.group(3)), 0)
    m = re.search(r"-v(\d+)\.(\d+)\.(\d+)", fname)
    if m:
        return (int(m.group(1)), int(m.group(2)), int(m.group(3)), 0)
    return None


def build_label(fname: str):
    # Extract a report-friendly version label (for example, 4.21.0-73937).
    m = re.search(r"-v(\d+\.\d+\.\d+-\d+)", fname)
    if m:
        return m.group(1)
    m = re.search(r"-v(\d+\.\d+\.\d+)-fdroid", fname)
    if m:
        return m.group(1)
    m = re.search(r"-v(\d+\.\d+\.\d+)", fname)
    if m:
        return m.group(1)
    return ""


def pick_by_substring(substr: str):
    # Keep current behavior: return the first filename that contains the token.
    if not substr:
        return None
    for key in apks:
        if substr in key.split("/")[-1]:
            return key
    return None


def pick_by_filename(filename: str):
    if not filename:
        return None
    for key in apks:
        if key.split("/")[-1] == filename:
            return key
    return None


parsed = []
for key in apks:
    parsed_version = parse_version(key.split("/")[-1])
    if parsed_version is not None:
        parsed.append((parsed_version, key))
# Sort ascending; latest is the last entry.
parsed.sort(key=lambda x: x[0])

latest_key = parsed[-1][1] if parsed else apks[-1]
second_latest_key = parsed[-2][1] if len(parsed) >= 2 else None


def normalize_direct(value: str):
    value = value.strip()
    if value.startswith("s3://"):
        parts = value.split("/", 3)
        return parts[3] if len(parts) >= 4 else ""
    return value.lstrip("/")


new_key = None
old_key = None

# Selection modes:
# 1) direct APK filename/path
# 2) "latest"
# 3) build token (substring)
if app_build.lower().endswith(".apk"):
    direct = normalize_direct(app_build)
    if "/" in direct:
        new_key = direct
    else:
        new_key = pick_by_filename(direct) or pick_by_substring(direct)

    if is_upgrade:
        if old_input.lower().endswith(".apk"):
            normalized_old = normalize_direct(old_input)
            old_key = normalized_old if "/" in normalized_old else (
                pick_by_filename(normalized_old) or pick_by_substring(normalized_old)
            )
        else:
            old_key = pick_by_substring(old_input) if old_input else second_latest_key
elif app_build == "latest":
    new_key = latest_key
    if is_upgrade:
        old_key = pick_by_substring(old_input) if old_input else second_latest_key
else:
    new_key = pick_by_substring(app_build)
    if is_upgrade:
        if not old_input:
            print("ERROR: isUpgrade=true but oldBuildNumber is empty.", file=sys.stderr)
            sys.exit(1)
        old_key = pick_by_substring(old_input)

if not new_key:
    print(f"ERROR: Could not resolve NEW apk for appBuildNumber='{app_build}'", file=sys.stderr)
    sys.exit(1)
if is_upgrade and not old_key:
    print("ERROR: Upgrade requested but OLD apk could not be resolved.", file=sys.stderr)
    sys.exit(1)

new_name = new_key.split("/")[-1]
old_name = old_key.split("/")[-1] if old_key else ""

# Print key/value lines so caller can append them to GitHub env/output files.
print(f"NEW_S3_KEY={new_key}")
print(f"OLD_S3_KEY={old_key or ''}")
print(f"NEW_APK_NAME={new_name}")
print(f"OLD_APK_NAME={old_name}")
print(f"REAL_BUILD_NUMBER={build_label(new_name)}")
print(f"OLD_BUILD_NUMBER={build_label(old_name) if old_name else ''}")
