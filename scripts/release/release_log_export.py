#!/usr/bin/env python3
"""Export the release context and the signed APK manifest for the Android release log."""

from __future__ import annotations

import argparse
import hashlib
import os
import re
import subprocess
import sys
from pathlib import Path

import release_log_contract as contract

VERSION_FILE_LIMIT = 64 * 1024
_ENVIRONMENT = {
    "repository": "GITHUB_REPOSITORY",
    "run_id": "GITHUB_RUN_ID",
    "run_attempt": "GITHUB_RUN_ATTEMPT",
    "workflow_ref": "GITHUB_WORKFLOW_REF",
    "sha": "GITHUB_SHA",
    "event_name": "GITHUB_EVENT_NAME",
    "ref_type": "GITHUB_REF_TYPE",
    "ref_name": "GITHUB_REF_NAME",
}


def run_context_from_env(environ):
    context = {}
    for key, variable in _ENVIRONMENT.items():
        value = environ.get(variable)
        if not value:
            raise ValueError(variable + " is not set")
        context[key] = value
    return context


def _workflow_path(repository, workflow_ref):
    prefix = repository + "/"
    if not isinstance(workflow_ref, str) or not workflow_ref.startswith(prefix) or "@" not in workflow_ref:
        raise ValueError("workflow_ref does not belong to this repository")
    path = workflow_ref[len(prefix):].split("@", 1)[0]
    if not path.startswith(".github/workflows/") or ".." in path:
        raise ValueError("workflow_ref is not a workflow file")
    return path


def _tag(event, run_context):
    if run_context["event_name"] == "release":
        release = event.get("release") if isinstance(event, dict) else None
        tag = release.get("tag_name") if isinstance(release, dict) else None
        if not isinstance(tag, str):
            raise ValueError("The release event has no tag")
        return tag
    if run_context.get("ref_type") == "tag":
        return run_context.get("ref_name")
    raise ValueError("The run has no release tag")


def build_context(event, run_context):
    for key in ("repository", "run_id", "run_attempt", "workflow_ref", "sha", "event_name"):
        if not run_context.get(key):
            raise ValueError(key + " is missing")
    record = {
        "schema_version": contract.SCHEMA_VERSION,
        "kind": "context",
        "repository": run_context["repository"],
        "run_id": contract.parse_positive(run_context["run_id"], "run_id"),
        "run_attempt": contract.parse_positive(run_context["run_attempt"], "run_attempt"),
        "workflow_path": _workflow_path(run_context["repository"], run_context["workflow_ref"]),
        "tag": _tag(event, run_context),
        "sha": run_context["sha"],
        "event_name": run_context["event_name"],
    }
    contract.validate_record(record)
    return record


def _version_values(version_file):
    path = Path(version_file)
    try:
        if path.stat().st_size > VERSION_FILE_LIMIT:
            raise ValueError("The version file is too large")
        text = path.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError):
        raise ValueError("Cannot read the version file") from None
    values = {}
    for line in text.splitlines():
        key, separator, value = line.partition(":")
        if separator and key in ("VersionCode", "VersionName") and key not in values:
            values[key] = value.strip()
    code, name = values.get("VersionCode"), values.get("VersionName")
    if not name:
        raise ValueError("The version file needs VersionCode and VersionName")
    return contract.parse_positive(code, "VersionCode"), name


def _digest(path):
    path = Path(path)
    if path.is_symlink() or not path.is_file():
        raise ValueError("APK path is not a regular file: " + path.name)
    sha = hashlib.sha256()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            sha.update(chunk)
    return {"filename": path.name, "sha256": sha.hexdigest()}


def build_manifest(context, version_file, apk_paths):
    contract.validate_record(context)
    if context["kind"] != "context":
        raise ValueError("A manifest is built from a context record")
    if not apk_paths:
        raise ValueError("At least one APK is required")
    code, name = _version_values(version_file)
    apks = sorted((_digest(path) for path in apk_paths), key=lambda apk: apk["filename"])
    if len({apk["filename"] for apk in apks}) != len(apks):
        raise ValueError("APK file names must be unique")
    record = {
        "schema_version": contract.SCHEMA_VERSION,
        "kind": "build",
        "repository": context["repository"],
        "run_id": context["run_id"],
        "run_attempt": context["run_attempt"],
        "workflow_path": context["workflow_path"],
        "tag": context["tag"],
        "sha": context["sha"],
        "flavor": "Beta" if contract.beta_number(context["tag"]) else "Prod",
        "version_name": name,
        "version_code": code,
        "apks": apks,
    }
    contract.validate_record(record)
    return record


def checked_out_sha():
    try:
        result = subprocess.run(["git", "rev-parse", "HEAD"], capture_output=True, text=True, check=True)
    except (OSError, subprocess.CalledProcessError):
        raise ValueError("Cannot read the checked-out commit") from None
    return result.stdout.strip()


def _context_command(arguments):
    return build_context(contract.read_json(arguments.event), run_context_from_env(os.environ))


def _build_command(arguments):
    context = _context_command(arguments)
    if checked_out_sha() != context["sha"]:
        raise ValueError("The checked-out commit is not the commit of this run")
    outputs = Path(arguments.outputs_dir)
    root = (outputs / "apk").resolve()
    apk_dir = (root / arguments.flavor.lower() / arguments.variant.lower()).resolve()
    if root not in apk_dir.parents:
        raise ValueError("The flavor and variant must name a directory under the APK outputs")
    apks = sorted(path for path in apk_dir.glob("*.apk") if not path.name.startswith("."))
    return build_manifest(context, outputs / "version.txt", apks)


def main(argv=None):
    parser = argparse.ArgumentParser(description="Export release log evidence records.")
    commands = parser.add_subparsers(dest="command", required=True)
    context = commands.add_parser("context", help="Write the release context record")
    context.add_argument("--event", required=True)
    context.add_argument("--output", required=True)
    context.set_defaults(run=_context_command)
    build = commands.add_parser("build", help="Write the signed APK record for the checked-out tag")
    build.add_argument("--event", required=True)
    build.add_argument("--flavor", required=True)
    build.add_argument("--variant", required=True)
    build.add_argument("--outputs-dir", default="app/build/outputs")
    build.add_argument("--output", required=True)
    build.set_defaults(run=_build_command)
    arguments = parser.parse_args(argv)
    try:
        record = arguments.run(arguments)
        contract.write_json(arguments.output, record)
    except (OSError, ValueError) as error:
        print("Release log export failed: " + str(error), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
