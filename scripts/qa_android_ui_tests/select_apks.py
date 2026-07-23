#!/usr/bin/env python3
"""Resolve NEW/OLD APK S3 keys from workflow inputs and print env-style outputs."""

import json
import os
import re
import sys
from datetime import datetime, timezone

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


def parse_last_modified(value: str):
    if not value:
        return None
    try:
        normalized = value.replace("Z", "+00:00")
        return datetime.fromisoformat(normalized).astimezone(timezone.utc)
    except ValueError:
        return None


apk_entries = []
for item in data:
    if isinstance(item, str) and item.lower().endswith(".apk"):
        apk_entries.append({
            "key": item,
            "name": item.split("/")[-1],
            "last_modified": None,
        })
    elif isinstance(item, dict):
        key = item.get("Key")
        if isinstance(key, str) and key.lower().endswith(".apk"):
            apk_entries.append({
                "key": key,
                "name": key.split("/")[-1],
                "last_modified": parse_last_modified(str(item.get("LastModified") or "")),
            })

if not apk_entries:
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
    for entry in apk_entries:
        if substr in entry["name"]:
            return entry["key"]
    return None


def pick_by_filename(filename: str):
    if not filename:
        return None
    for entry in apk_entries:
        if entry["name"] == filename:
            return entry["key"]
    return None


parsed = []
for entry in apk_entries:
    parsed_version = parse_version(entry["name"])
    if parsed_version is not None:
        parsed.append((parsed_version, entry["key"]))
# Sort ascending; fallback latest is the last entry.
parsed.sort(key=lambda x: x[0])

by_recency = [entry for entry in apk_entries if entry["last_modified"] is not None]
# S3 upload time is the only stable "latest" signal after the 5-digit APK
# suffix wraps, so use recency whenever the metadata is available.
by_recency.sort(key=lambda entry: entry["last_modified"])

ordered_keys = (
    [entry["key"] for entry in by_recency]
    if by_recency
    else ([item[1] for item in parsed] if parsed else [entry["key"] for entry in apk_entries])
)

latest_key = ordered_keys[-1]


def previous_key(current_key: str):
    try:
        index = ordered_keys.index(current_key)
    except ValueError:
        return None
    return ordered_keys[index - 1] if index > 0 else None


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
            old_key = pick_by_substring(old_input) if old_input else previous_key(new_key)
elif app_build == "latest":
    new_key = latest_key
    if is_upgrade:
        old_key = pick_by_substring(old_input) if old_input else previous_key(new_key)
else:
    new_key = pick_by_substring(app_build)
    if is_upgrade:
        old_key = pick_by_substring(old_input) if old_input else previous_key(new_key)

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
