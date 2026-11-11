#!/usr/bin/env python3
"""Export the production regression record for the Android release log."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "release"))

import release_log_contract as contract  # noqa: E402
import release_log_export as release_export  # noqa: E402

PROD_WORKFLOW = ".github/workflows/qa-android-prod-tests.yml"
PRODUCTION_FLAVOR = "production"
LIST_LIMIT = 1024 * 1024


def bundle_fingerprint(metadata, failed_test_ids):
    text = json.dumps({"metadata": metadata, "failed_test_ids": list(failed_test_ids)},
                      sort_keys=True, separators=(",", ":"), ensure_ascii=False)
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def read_bundle(directory):
    """Return the metadata and the failed test IDs, in file order, of the one bundle under `directory`."""
    root = Path(directory)
    matches = sorted(root.rglob("metadata.json")) if root.is_dir() else []
    if len(matches) != 1:
        raise ValueError("Expected exactly one deflake bundle")
    folder = matches[0].parent
    for path in (matches[0], folder / "failed-tests.txt"):
        if path.is_symlink() or not path.is_file() or path.stat().st_size > LIST_LIMIT:
            raise ValueError("The deflake bundle is not readable")
    try:
        metadata = contract.read_json(matches[0])
        lines = (folder / "failed-tests.txt").read_text(encoding="utf-8").splitlines()
    except (OSError, UnicodeDecodeError):
        raise ValueError("The deflake bundle is not readable") from None
    if not isinstance(metadata, dict):
        raise ValueError("The deflake metadata must be an object")
    return metadata, [line.strip() for line in lines if line.strip()]


def directory_fingerprint(directory):
    try:
        metadata, failed = read_bundle(directory)
    except ValueError:
        return None
    return bundle_fingerprint(metadata, failed)


def _digest(path):
    if path is None:
        return None
    path = Path(path)
    if path.is_symlink() or not path.is_file():
        return None
    sha = hashlib.sha256()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            sha.update(chunk)
    return sha.hexdigest()


def _apk(path, name, label, fallback):
    digest = _digest(path)
    if digest is None:
        return None
    filename = name if isinstance(name, str) and name not in ("", ".", "..") and not (
        "/" in name or "\\" in name) else fallback
    return {"filename": filename, "sha256": digest, "resolved_version_label": label or None}


def _eligible(context):
    try:
        path = release_export._workflow_path(context["repository"], context["workflow_ref"])
    except ValueError:
        return False
    return (path == PROD_WORKFLOW and context["event_name"] == "workflow_dispatch"
            and context["flavor"] == PRODUCTION_FLAVOR)


def _count_results(results):
    counts = {key: (results or {}).get(key) for key in contract.RESULT_KEYS}
    parts = [counts[key] for key in ("passed", "failed", "skipped", "missing")]
    if counts["total"] is None and None not in parts and all(isinstance(part, int) for part in parts):
        counts["total"] = sum(parts)
    return counts


def regression_record(context, apk_path, old_apk_path, results, report_url):
    """Return the regression record, or None when this run is not a production regression."""
    if not _eligible(context):
        return None
    mode = context["mode"]
    record = {
        "schema_version": contract.SCHEMA_VERSION,
        "kind": "regression",
        "repository": context["repository"],
        "run_id": contract.parse_positive(str(context["run_id"]), "run_id"),
        "run_attempt": contract.parse_positive(str(context["run_attempt"]), "run_attempt"),
        "workflow_path": PROD_WORKFLOW,
        "test_source_sha": context["test_source_sha"],
        "flavor": PRODUCTION_FLAVOR,
        "suite": context["suite"],
        "selector": context["selector"],
        "mode": mode,
        "source_qa_run_id": context["source_qa_run_id"],
        "source_qa_run_attempt": context["source_qa_run_attempt"],
        "apk": _apk(apk_path, context.get("new_apk_name"), context.get("new_version_label"), "Wire.apk"),
        "is_upgrade": context["is_upgrade"],
        "old_apk": (_apk(old_apk_path, context.get("old_apk_name"), context.get("old_version_label"),
                         "Wire.old.apk") if context["is_upgrade"] else None),
        "retry_count": context["retry_count"],
        "results": _count_results(results),
        "report_url": report_url or None,
        "next_deflake_bundle_sha256": context["next_deflake_bundle_sha256"],
        "source_deflake_bundle_sha256": context["source_deflake_bundle_sha256"],
    }
    try:
        contract.validate_record(record)
    except ValueError:
        # Counts that contradict each other are not evidence. Keep the rest of the record.
        record["results"] = {key: (None if key != "passed_on_rerun" else record["results"][key])
                             for key in contract.RESULT_KEYS}
        contract.validate_record(record)
    return record


def checked_out_sha():
    try:
        result = subprocess.run(["git", "rev-parse", "HEAD"], capture_output=True, text=True, check=True)
    except (OSError, subprocess.CalledProcessError):
        raise ValueError("Cannot read the checked-out commit") from None
    return result.stdout.strip()


def _env(environ, name):
    return (environ.get(name) or "").strip()


def _required(environ, name):
    value = _env(environ, name)
    if not value:
        raise ValueError(name + " is not set")
    return value


def _count(text):
    text = text.strip()
    return int(text) if text.isascii() and text.isdigit() else None


def _retry_count(text):
    count = _count(text)
    if count is None or not 0 <= count <= 3:
        raise ValueError("RERUN_FAILED_COUNT must be a whole number from 0 through 3")
    return count


def _scope(tags, category, test_case):
    return category or test_case or "all", tags.strip() or "all"


def _positive_text(value, name):
    return contract.parse_positive(str(value), name) if value not in (None, "") else None


def _deflake_fields(environ):
    metadata, failed = read_bundle(_required(environ, "DEFLAKE_BUNDLE_ROOT"))
    selector_type, selector_value = metadata.get("selector_type"), metadata.get("selector_value") or ""
    category = selector_value if selector_type == "category" else ""
    test_case = selector_value if selector_type == "testCaseId" else ""
    suite, selector = _scope(str(metadata.get("tags") or ""), category, test_case)
    sha = metadata.get("test_source_sha") or metadata.get("source_sha")
    return {
        "mode": "deflake",
        "flavor": str(metadata.get("flavor") or ""),
        "suite": suite, "selector": selector,
        "is_upgrade": metadata.get("is_upgrade") is True,
        "source_qa_run_id": _positive_text(metadata.get("source_run_id"), "source_run_id"),
        "source_qa_run_attempt": _positive_text(metadata.get("source_run_attempt"), "source_run_attempt"),
        "source_deflake_bundle_sha256": bundle_fingerprint(metadata, failed),
        "test_source_sha": sha,
    }


def context_from_env(environ):
    context = {name: _required(environ, variable) for name, variable in (
        ("repository", "GITHUB_REPOSITORY"), ("run_id", "GITHUB_RUN_ID"), ("run_attempt", "GITHUB_RUN_ATTEMPT"),
        ("workflow_ref", "GITHUB_WORKFLOW_REF"), ("event_name", "GITHUB_EVENT_NAME"))}
    mode = _required(environ, "RELEASE_LOG_RUN_MODE")
    if mode not in ("full", "deflake"):
        raise ValueError("RELEASE_LOG_RUN_MODE must be full or deflake")
    suite, selector = _scope(_env(environ, "TAGS_INPUT"), _env(environ, "RESOLVED_CATEGORY"),
                             _env(environ, "RESOLVED_TESTCASE_ID"))
    context.update({
        "mode": "full", "flavor": _env(environ, "FLAVOR_INPUT"), "suite": suite, "selector": selector,
        "is_upgrade": _env(environ, "IS_UPGRADE") == "true",
        "source_qa_run_id": None, "source_qa_run_attempt": None, "source_deflake_bundle_sha256": None,
    })
    if mode == "deflake":
        context.update(_deflake_fields(environ))
    else:
        context["test_source_sha"] = _env(environ, "TEST_SOURCE_SHA") or checked_out_sha()
    context.update({
        "retry_count": _retry_count(_env(environ, "RERUN_FAILED_COUNT")),
        "new_apk_name": _env(environ, "NEW_APK_NAME"), "new_version_label": _env(environ, "REAL_BUILD_NUMBER"),
        "old_apk_name": _env(environ, "OLD_APK_NAME"), "old_version_label": _env(environ, "OLD_BUILD_NUMBER"),
        "next_deflake_bundle_sha256": (directory_fingerprint(Path(_env(environ, "DEFLAKE_BUNDLE_DIR")))
                                       if _env(environ, "DEFLAKE_BUNDLE_DIR") else None),
    })
    return context


def _lines(path):
    try:
        text = Path(path).read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError):
        return None
    return len([line for line in text.splitlines() if line.strip()])


def _passed_on_rerun(environ):
    """Count tests that failed first and passed later, from the retry state files."""
    first_file = _env(environ, "FIRST_FAILED_TESTS_FILE")
    if first_file:
        first = _lines(first_file)
        final_file = _env(environ, "FINAL_FAILED_TESTS_FILE")
        final = _lines(final_file) if final_file else 0
        if first is not None and final is not None:
            return max(first - final, 0)
    return _count(_env(environ, "PASSED_ON_RERUN_COUNT"))


def results_from_env(environ):
    results = {key: _count(_env(environ, "TESTS_" + key.upper())) for key in
               ("passed", "failed", "skipped", "missing", "selected", "total")}
    results["passed_on_rerun"] = _passed_on_rerun(environ)
    return results


def _command(arguments):
    environ = os.environ
    output = Path(arguments.output)
    output.unlink(missing_ok=True)
    context = context_from_env(environ)
    new_path = _env(environ, "NEW_APK_PATH")
    old_path = _env(environ, "OLD_APK_PATH")
    record = regression_record(context, new_path or None, old_path or None, results_from_env(environ),
                               _env(environ, "ALLURE_REPORT_URL") or None)
    if record is None:
        print("Release log: not a production regression, no record written")
        skip_output = _env(environ, "RELEASE_LOG_SKIP_OUTPUT")
        if skip_output:
            marker = contract.skip_marker(context["repository"],
                                          contract.parse_positive(str(context["run_id"]), "run_id"),
                                          contract.parse_positive(str(context["run_attempt"]), "run_attempt"))
            Path(skip_output).parent.mkdir(parents=True, exist_ok=True)
            contract.write_json(skip_output, marker)
        return 0
    output.parent.mkdir(parents=True, exist_ok=True)
    contract.write_json(output, record)
    return 0


def main(argv=None):
    parser = argparse.ArgumentParser(description="Export the production regression record.")
    parser.add_argument("--output", required=True)
    arguments = parser.parse_args(argv)
    try:
        return _command(arguments)
    except (OSError, ValueError) as error:
        print("Release log QA export failed: " + str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
