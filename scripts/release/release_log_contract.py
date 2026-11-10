"""Record contract and webhook fields for the Android release log."""

from __future__ import annotations

import json
import re

_TAG = re.compile(r"v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(?:-beta\.([1-9][0-9]*))?")


def _parse_tag(tag):
    if not isinstance(tag, str) or not _TAG.fullmatch(tag):
        raise ValueError("Expected vX.Y.Z or vX.Y.Z-beta.N")
    return _TAG.fullmatch(tag)


def release_version(tag):
    match = _parse_tag(tag)
    return ".".join(match.group(1, 2, 3))


def beta_number(tag):
    number = _parse_tag(tag).group(4)
    return int(number) if number else None


SCHEMA_VERSION = 1
JSON_LIMIT = 1024 * 1024
BUILD_FLAVORS = ("Beta", "Prod")
RESULT_KEYS = ("passed", "failed", "skipped", "missing", "selected", "total", "passed_on_rerun")

_CONTROL = re.compile("[\x00-\x1f\x7f-\x9f\u2028\u2029]")
SHA = re.compile(r"[0-9a-f]{40}")
_SHA256 = re.compile(r"[0-9a-f]{64}")
REPOSITORY = re.compile(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")

_COMMON = ("schema_version", "kind", "repository", "run_id", "run_attempt", "workflow_path")
_CONTEXT = _COMMON + ("tag", "sha", "event_name")
_BUILD = _COMMON + ("tag", "sha", "flavor", "version_name", "version_code", "apks")
_REGRESSION = _COMMON + (
    "test_source_sha", "flavor", "suite", "selector", "mode", "source_qa_run_id",
    "source_qa_run_attempt", "apk", "is_upgrade", "old_apk", "retry_count", "results", "report_url",
    "next_deflake_bundle_sha256", "source_deflake_bundle_sha256",
)
_APK = ("filename", "sha256", "resolved_version_label")


def _is_int(value):
    return isinstance(value, int) and not isinstance(value, bool)


def is_positive(value):
    return _is_int(value) and value >= 1


def parse_positive(text, name):
    if not isinstance(text, str) or not re.fullmatch(r"[1-9][0-9]*", text):
        raise ValueError(name + " must be a positive integer")
    return int(text)


def _positive_int(value, name):
    if not is_positive(value):
        raise ValueError(name + " must be a positive integer")


def _nonnegative_int(value, name):
    if not _is_int(value) or value < 0:
        raise ValueError(name + " must be a nonnegative integer")


def _text(value, name):
    if not isinstance(value, str) or not value.strip() or _CONTROL.search(value):
        raise ValueError(name + " must be nonempty text without control characters")


def _pattern(value, regex, name):
    if not isinstance(value, str) or not regex.fullmatch(value):
        raise ValueError(name + " is malformed")


def _https_url(value, name):
    if (not isinstance(value, str) or not value.startswith("https://") or len(value) <= len("https://")
            or _CONTROL.search(value) or re.search(r"\s", value)):
        raise ValueError(name + " must be an HTTPS URL")


def _filename(value, name):
    if (not isinstance(value, str) or value in ("", ".", "..") or "/" in value or "\\" in value
            or _CONTROL.search(value)):
        raise ValueError(name + " must be a file name without path segments")


def _exact_keys(record, keys, name):
    if set(record) != set(keys):
        raise ValueError(name + " fields do not match the schema")


def _apk(value, name):
    if value is None:
        return
    if not isinstance(value, dict):
        raise ValueError(name + " must be an object or null")
    _exact_keys(value, _APK, name)
    _filename(value["filename"], name + ".filename")
    _pattern(value["sha256"], _SHA256, name + ".sha256")
    if value["resolved_version_label"] is not None:
        _text(value["resolved_version_label"], name + ".resolved_version_label")


def _identity(record):
    if record["schema_version"] != SCHEMA_VERSION or not _is_int(record["schema_version"]):
        raise ValueError("Unknown schema_version")
    _pattern(record["repository"], REPOSITORY, "repository")
    _positive_int(record["run_id"], "run_id")
    _positive_int(record["run_attempt"], "run_attempt")
    _text(record["workflow_path"], "workflow_path")


def _context(record):
    _exact_keys(record, _CONTEXT, "context")
    _identity(record)
    release_version(record["tag"])
    _pattern(record["sha"], SHA, "sha")
    _text(record["event_name"], "event_name")


def _build(record):
    _exact_keys(record, _BUILD, "build")
    _identity(record)
    release_version(record["tag"])
    _pattern(record["sha"], SHA, "sha")
    if record["flavor"] not in BUILD_FLAVORS:
        raise ValueError("flavor must be Beta or Prod")
    _text(record["version_name"], "version_name")
    _positive_int(record["version_code"], "version_code")
    apks = record["apks"]
    if not isinstance(apks, list) or not apks:
        raise ValueError("apks must be a nonempty list")
    for apk in apks:
        if not isinstance(apk, dict):
            raise ValueError("apk entries must be objects")
        _exact_keys(apk, _APK[:2], "apk")
        _filename(apk["filename"], "apk.filename")
        _pattern(apk["sha256"], _SHA256, "apk.sha256")


def _results(results, mode):
    if not isinstance(results, dict):
        raise ValueError("results must be an object")
    _exact_keys(results, RESULT_KEYS, "results")
    for key in RESULT_KEYS:
        if results[key] is not None:
            _nonnegative_int(results[key], "results." + key)
    parts = [results[key] for key in ("passed", "failed", "skipped", "missing")]
    if results["total"] is not None and None not in parts and results["total"] != sum(parts):
        raise ValueError("results.total must equal passed + failed + skipped + missing")
    if None not in (results["passed"], results["passed_on_rerun"]) and results["passed_on_rerun"] > results["passed"]:
        raise ValueError("results.passed_on_rerun must not exceed results.passed")
    if None not in (results["selected"], results["total"]):
        if results["selected"] > results["total"]:
            raise ValueError("results.selected must not exceed results.total")
        if mode == "deflake" and results["selected"] != results["total"]:
            raise ValueError("deflake requires selected to equal total")


def _regression(record):
    _exact_keys(record, _REGRESSION, "regression")
    _identity(record)
    _pattern(record["test_source_sha"], SHA, "test_source_sha")
    if record["flavor"] != "production":
        raise ValueError("regression flavor must be production")
    _text(record["suite"], "suite")
    _text(record["selector"], "selector")
    mode = record["mode"]
    if mode not in ("full", "deflake"):
        raise ValueError("mode must be full or deflake")
    source = (record["source_qa_run_id"], record["source_qa_run_attempt"],
              record["source_deflake_bundle_sha256"])
    if mode == "full":
        if any(value is not None for value in source):
            raise ValueError("full runs carry no source execution")
    else:
        _positive_int(record["source_qa_run_id"], "source_qa_run_id")
        _positive_int(record["source_qa_run_attempt"], "source_qa_run_attempt")
    if record["source_deflake_bundle_sha256"] is not None:
        _pattern(record["source_deflake_bundle_sha256"], _SHA256, "source_deflake_bundle_sha256")
    if record["next_deflake_bundle_sha256"] is not None:
        _pattern(record["next_deflake_bundle_sha256"], _SHA256, "next_deflake_bundle_sha256")
    _apk(record["apk"], "apk")
    _apk(record["old_apk"], "old_apk")
    if not isinstance(record["is_upgrade"], bool):
        raise ValueError("is_upgrade must be a boolean")
    if not _is_int(record["retry_count"]) or not 0 <= record["retry_count"] <= 3:
        raise ValueError("retry_count must be an integer from 0 through 3")
    _results(record["results"], mode)
    if record["report_url"] is not None:
        _https_url(record["report_url"], "report_url")


_RECORD_KINDS = {"context": _context, "build": _build, "regression": _regression}


def validate_record(record):
    if not isinstance(record, dict):
        raise ValueError("Record must be an object")
    kind = record.get("kind")
    if not isinstance(kind, str) or kind not in _RECORD_KINDS:
        raise ValueError("Unknown record kind")
    _RECORD_KINDS[kind](record)


KINDS = ("beta", "production", "regression")
FIELD_NAMES = ("releaseVersion", "kind", "result", "runUrl", "commit", "apkName", "apkSha256", "buildRunUrl",
               "deploy", "tests", "gaps", "eventId")
CONCLUSIONS = ("success", "failure", "cancelled", "timed_out", "skipped", "neutral", "action_required",
               "startup_failure", "stale")
UNMATCHED = "unmatched"
FIELD_LIMIT = 1000

_VERSION = re.compile(r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)")
_PAYLOAD = ("schema_version", "release_version", "event_id", "kind", "evidence")
_EVIDENCE = ("source", "conclusion", "jobs", "build", "qa", "origin", "gaps")
_SOURCE = ("repository", "run_id", "run_attempt", "workflow_path", "url", "tag", "sha")
_ORIGIN = ("run_id", "run_attempt", "url")


def run_url(repository, run_id, run_attempt):
    return "https://github.com/%s/actions/runs/%d/attempts/%d" % (repository, run_id, run_attempt)


def _source(source):
    if not isinstance(source, dict):
        raise ValueError("source must be an object")
    _exact_keys(source, _SOURCE, "source")
    _pattern(source["repository"], REPOSITORY, "source.repository")
    _positive_int(source["run_id"], "source.run_id")
    _positive_int(source["run_attempt"], "source.run_attempt")
    _text(source["workflow_path"], "source.workflow_path")
    if source["url"] != run_url(source["repository"], source["run_id"], source["run_attempt"]):
        raise ValueError("source.url must be the GitHub URL of the source run")
    if source["tag"] is not None:
        release_version(source["tag"])
    if source["sha"] is not None:
        _pattern(source["sha"], SHA, "source.sha")


def _jobs(jobs):
    if not isinstance(jobs, list):
        raise ValueError("jobs must be a list")
    for job in jobs:
        if not isinstance(job, dict):
            raise ValueError("job entries must be objects")
        _exact_keys(job, ("name", "conclusion"), "job")
        if not isinstance(job["name"], str) or not job["name"].strip():
            raise ValueError("job.name must be nonempty text")
        if job["conclusion"] is not None and job["conclusion"] not in CONCLUSIONS:
            raise ValueError("job.conclusion is unknown")


def _same_run(record, source, kind):
    validate_record(record)
    if record["kind"] != kind:
        raise ValueError("Embedded record must be a " + kind + " record")
    for key in ("repository", "run_id", "run_attempt", "workflow_path"):
        if record[key] != source[key]:
            raise ValueError("Embedded record does not belong to the source run")


def validate_payload(payload):
    if not isinstance(payload, dict):
        raise ValueError("Payload must be an object")
    _exact_keys(payload, _PAYLOAD, "payload")
    if not _is_int(payload["schema_version"]) or payload["schema_version"] != SCHEMA_VERSION:
        raise ValueError("Unknown schema_version")
    kind = payload["kind"]
    if kind not in KINDS:
        raise ValueError("Unknown payload kind")
    evidence = payload["evidence"]
    if not isinstance(evidence, dict):
        raise ValueError("evidence must be an object")
    _exact_keys(evidence, _EVIDENCE, "evidence")
    source = evidence["source"]
    _source(source)
    expected = "%s:%s:%d:%d" % (kind, source["repository"], source["run_id"], source["run_attempt"])
    if payload["event_id"] != expected:
        raise ValueError("event_id does not match the source run")
    version = payload["release_version"]
    if not (isinstance(version, str) and (_VERSION.fullmatch(version) or
                                          (version == UNMATCHED and kind == "regression"))):
        raise ValueError("release_version must be a bare version")
    if evidence["conclusion"] not in CONCLUSIONS:
        raise ValueError("conclusion is unknown")
    _jobs(evidence["jobs"])
    gaps = evidence["gaps"]
    if not isinstance(gaps, list) or not all(isinstance(gap, str) and gap.strip() for gap in gaps):
        raise ValueError("gaps must be a list of nonempty text")
    build, qa, origin = evidence["build"], evidence["qa"], evidence["origin"]
    tags = []
    if kind == "regression":
        if build is not None:
            raise ValueError("A regression carries no build record")
        if qa is not None:
            _same_run(qa, source, "regression")
        if origin is not None:
            if not isinstance(origin, dict):
                raise ValueError("origin must be an object or null")
            _exact_keys(origin, _ORIGIN, "origin")
            _positive_int(origin["run_id"], "origin.run_id")
            _positive_int(origin["run_attempt"], "origin.run_attempt")
            if origin["url"] != run_url(source["repository"], origin["run_id"], origin["run_attempt"]):
                raise ValueError("origin.url must be the GitHub URL of the origin run")
    else:
        if qa is not None or origin is not None:
            raise ValueError("A build event carries no regression evidence")
        if build is not None:
            _same_run(build, source, "build")
            tags.append(build["tag"])
        tags.append(source["tag"])
        if any(tag is not None and release_version(tag) != version for tag in tags):
            raise ValueError("release_version does not match the release tag")


def _clean(value):
    text = re.sub("[\x00-\x1f\x7f-\x9f\u2028\u2029]+", " ", str(value))
    text = re.sub(r":(?=:)", ": ", text)
    text = re.sub(r"\{(?=\{)", "{ ", text)
    text = text.replace("<", "\u2039").replace(">", "\u203a").replace("`", "'")
    text = re.sub(" {2,}", " ", text).strip()
    if len(text) > FIELD_LIMIT:
        text = text[:FIELD_LIMIT - 1] + "\u2026"
    return text


def _plain(value):
    return re.sub(r"[^A-Za-z0-9 ._()+,@/-]", "_", str(value))


def _sentence(value):
    text = re.sub(r"[^A-Za-z0-9 ._()+,@/;:-]", "_", str(value))
    return re.sub(r":(?! |$)", "_", text)


def _link(value):
    return re.sub(r"[^A-Za-z0-9._~:/?#&=%+-]", "_", str(value))


def _label(kind, source, qa):
    if kind == "beta":
        number = beta_number(source["tag"]) if source["tag"] else None
        return "Beta %d" % number if number else "Beta"
    if kind == "production":
        return "Production"
    return "Deflake" if qa is not None and qa["mode"] == "deflake" else "Regression"


def _tests(kind, qa, conclusion):
    """Return the tests text and the gaps this kind of evidence adds."""
    if kind == "beta":
        return "Regression: not required", []
    if kind == "production":
        return "", []
    if qa is None:
        if conclusion != "success":
            return "", []
        return "results unavailable", ["Regression evidence is missing"]
    gaps = []
    results = qa["results"]
    counts = [results[key] for key in ("passed", "failed", "skipped", "missing")]
    if None in counts or not results["total"]:
        parts = ["results unavailable"]
        gaps.append("Test results are missing or empty")
    else:
        text = "%d passed, %d failed, %d skipped, %d missing" % tuple(counts)
        if results["passed_on_rerun"]:
            text += ", %d passed on rerun" % results["passed_on_rerun"]
        parts = [text]
        if results["missing"]:
            gaps.append("Selected tests are missing results")
        if not results["passed"] + results["failed"]:
            gaps.append("No tests were executed")
    parts.append("selector " + _plain(qa["selector"]))
    if qa["report_url"]:
        parts.append("report " + _link(qa["report_url"]))
    if qa["mode"] == "deflake":
        parts.append("not a full regression")
        parts.append("source run %d" % qa["source_qa_run_id"])
    return "; ".join(parts), gaps


def _result(conclusion, qa, gaps):
    if conclusion == "cancelled":
        return "cancelled"
    if conclusion in ("failure", "timed_out", "startup_failure"):
        return "failed"
    if qa is not None and qa["results"]["failed"]:
        return "failed"
    if conclusion != "success" or gaps:
        return "partial"
    return "passed"


def build_fields(payload):
    validate_payload(payload)
    evidence = payload["evidence"]
    source, build, qa, origin = evidence["source"], evidence["build"], evidence["qa"], evidence["origin"]
    kind = payload["kind"]
    tests, added_gaps = _tests(kind, qa, evidence["conclusion"])
    gaps = list(evidence["gaps"]) + added_gaps
    if kind == "regression":
        apks = [qa["apk"]] if qa is not None and qa["apk"] else []
        commit = "test " + qa["test_source_sha"][:7] if qa is not None else ""
        deploy = ""
    else:
        apks = build["apks"] if build is not None else []
        commit = source["sha"][:7] if source["sha"] else ""
        deploy = "; ".join("%s: %s" % (_plain(job["name"]), job["conclusion"] or "unknown")
                           for job in evidence["jobs"])
    fields = {
        "releaseVersion": payload["release_version"],
        "kind": _label(kind, source, qa),
        "result": _result(evidence["conclusion"], qa, gaps),
        "runUrl": _link(source["url"]),
        "commit": commit,
        "apkName": ", ".join(_plain(apk["filename"]).replace(",", "_") for apk in apks),
        "apkSha256": ", ".join(apk["sha256"] for apk in apks),
        "buildRunUrl": _link(origin["url"]) if origin else "",
        "deploy": deploy,
        "tests": tests,
        "gaps": "; ".join(_sentence(gap) for gap in gaps) if gaps else "none",
        "eventId": payload["event_id"],
    }
    return {name: _clean(value) for name, value in fields.items()}


def _no_duplicates(pairs):
    keys = [key for key, _ in pairs]
    if len(keys) != len(set(keys)):
        raise ValueError("Duplicate JSON key")
    return dict(pairs)


def load_json(text):
    if len(text.encode("utf-8")) > JSON_LIMIT:
        raise ValueError("JSON document is too large")
    try:
        return json.loads(text, object_pairs_hook=_no_duplicates)
    except json.JSONDecodeError:
        raise ValueError("Malformed JSON document") from None
    except RecursionError:
        raise ValueError("JSON document is nested too deeply") from None


def read_json(path):
    with open(path, encoding="utf-8") as handle:
        return load_json(handle.read())


def dump_json(value):
    return json.dumps(value, indent=2, sort_keys=True) + "\n"


def write_json(path, value):
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(dump_json(value))
