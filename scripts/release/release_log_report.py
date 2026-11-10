#!/usr/bin/env python3
"""Collect release log evidence from GitHub and send it to the Confluence automation webhook."""

from __future__ import annotations

import argparse
import enum
import io
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from datetime import datetime, timedelta

import release_log_contract as contract

API = "https://api.github.com"
PER_PAGE = 100
MAX_PAGES = 50
ARCHIVE_LIMIT = 5 * 1024 * 1024
RESPONSE_LIMIT = 10 * 1024 * 1024
API_TIMEOUT = 30

PROD_BUILD_PATH = ".github/workflows/build-production.yml"
REGRESSION_PATH = ".github/workflows/qa-android-prod-tests.yml"
WORKFLOW_KINDS = {
    ".github/workflows/build-beta.yml": "beta",
    PROD_BUILD_PATH: "production",
    REGRESSION_PATH: "regression",
}
BUILD_FLAVORS = {"beta": "Beta", "production": "Prod"}
ELIGIBLE_EVENTS = {"beta": ("push",), "production": ("release",)}
# The reusable build workflow that owns each kind's build and deployment jobs, and the flavor its
# deployment groups must name. A workflow that calls the build twice, like Production, also runs
# an F-Droid pipeline whose jobs have the same names.
PIPELINES = {"beta": ("build", None), "production": ("build-production", "Prod")}
ORIGIN_WINDOW_DAYS = 90
MAX_ANCESTRY = 20

_RELEASE_JOB = re.compile(r"Validate |validate-release$|Build [A-Za-z0-9]+-[A-Za-z0-9]+|[Dd]eploy|Publish ")
_AGGREGATE_JOB = "deployment-summary"
_UNSAFE_NAME = re.compile(r"[^A-Za-z0-9 ._()\[\]:,/+-]")
_BUILD_JOB = re.compile(r"Build [A-Za-z0-9]+-[A-Za-z0-9]+")
_QA_JOB = re.compile(r"Validate |Run ")
_QA_RUN_JOB = re.compile(r"Run ")
CONTEXT_JOB = "Export release log context"


class ApiError(Exception):
    """A GitHub request failed. The message never carries a URL or credential."""


class ApiNotFound(ApiError):
    pass


class CollectionError(Exception):
    """Evidence could not be collected. `partial` is a validated payload when one can be reported."""

    def __init__(self, reason, partial=None):
        super().__init__(reason)
        self.reason = reason
        self.partial = partial


class _NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *arguments, **keywords):
        return None


_OPENER = urllib.request.build_opener(_NoRedirect)


def _open(request, timeout):
    return _OPENER.open(request, timeout=timeout)


class GitHubApi:
    def __init__(self, token):
        self.token = token

    @classmethod
    def from_env(cls):
        token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
        if not token:
            raise CollectionError("GITHUB_TOKEN is not set")
        return cls(token)

    def _request(self, url, authorized=True):
        headers = {"Accept": "application/vnd.github+json", "X-GitHub-Api-Version": "2022-11-28"}
        if authorized:
            headers["Authorization"] = "Bearer " + self.token
        return urllib.request.Request(url, headers=headers, method="GET")

    def get(self, path, params=None):
        url = API + path + ("?" + urllib.parse.urlencode(params) if params else "")
        try:
            with _open(self._request(url), API_TIMEOUT) as response:
                body = response.read(RESPONSE_LIMIT + 1)
        except urllib.error.HTTPError as error:
            if error.code == 404:
                raise ApiNotFound("GitHub returned 404") from None
            raise ApiError("GitHub returned %d" % error.code) from None
        except (urllib.error.URLError, OSError):
            raise ApiError("GitHub is unreachable") from None
        if len(body) > RESPONSE_LIMIT:
            raise ApiError("GitHub response is too large")
        try:
            return json.loads(body.decode("utf-8"))
        except (ValueError, RecursionError):
            raise ApiError("GitHub returned malformed JSON") from None

    def download(self, url):
        if not url.startswith(API + "/"):
            raise ApiError("Unexpected download URL")
        try:
            try:
                with _open(self._request(url), API_TIMEOUT) as response:
                    return response.read(ARCHIVE_LIMIT + 1)
            except urllib.error.HTTPError as error:
                location = error.headers.get("Location") if error.code in (301, 302, 303, 307, 308) else None
                if not location or not location.startswith("https://"):
                    raise ApiError("GitHub returned %d" % error.code) from None
            # The signed storage URL must not receive the GitHub token.
            with _open(self._request(location, authorized=False), API_TIMEOUT) as response:
                return response.read(ARCHIVE_LIMIT + 1)
        except (urllib.error.URLError, OSError):
            raise ApiError("The download failed") from None


def _paginate(api, path, key, params=None):
    items = []
    for page in range(1, MAX_PAGES + 1):
        data = api.get(path, dict(params or {}, per_page=PER_PAGE, page=page))
        batch = data.get(key) if isinstance(data, dict) else None
        if not isinstance(batch, list):
            raise CollectionError("GitHub returned an unexpected response")
        items.extend(batch)
        total = data.get("total_count")
        if len(batch) < PER_PAGE or (isinstance(total, int) and len(items) >= total):
            return items
    raise CollectionError("GitHub results were not fully enumerated")


def _leaf(name):
    return name.rsplit(" / ", 1)[-1] if isinstance(name, str) else ""


def _release_leaf(kind, name):
    """Return the job's own name when it belongs to this kind's release, else None."""
    if not isinstance(name, str):
        return None
    parts = name.split(" / ")
    if len(parts) == 1:
        return name
    pipeline, flavor = PIPELINES[kind]
    if parts[0] != pipeline:
        return None
    if len(parts) == 3 and flavor and not parts[1].startswith("deploy (%s," % flavor):
        return None
    return parts[-1]


def _validate_run(run, repository, run_id, run_attempt):
    if not isinstance(run, dict):
        raise CollectionError("GitHub returned an unexpected run")
    full_name = run.get("repository", {}).get("full_name") if isinstance(run.get("repository"), dict) else None
    if run.get("id") != run_id or run.get("run_attempt") != run_attempt or full_name != repository:
        raise CollectionError("The run does not match the request")
    raw = run.get("path")
    path = raw.split("@", 1)[0] if isinstance(raw, str) else None
    if path not in WORKFLOW_KINDS:
        raise CollectionError("The workflow is not eligible for the release log")
    if run.get("status") != "completed":
        raise CollectionError("The run has not completed")
    if run.get("conclusion") not in contract.CONCLUSIONS:
        raise CollectionError("The run has an unknown conclusion")
    if not isinstance(run.get("head_sha"), str) or not contract.SHA.fullmatch(run["head_sha"]):
        raise CollectionError("The run has no valid commit")
    return path, WORKFLOW_KINDS[path]


def _is_release_run(run, kind, default_branch=None):
    if kind == "regression":
        branch = run.get("head_branch")
        return run.get("event") == "workflow_dispatch" and isinstance(branch, str) and (
            branch == default_branch or branch.startswith("release/"))
    if run.get("event") not in ELIGIBLE_EVENTS[kind]:
        return False
    try:
        contract.release_version(run.get("head_branch"))
    except ValueError:
        return False
    return True


def _release_jobs(jobs, kind, pattern=_RELEASE_JOB):
    entries = []
    for entry in jobs:
        if not isinstance(entry, dict) or not isinstance(entry.get("name"), str):
            continue
        leaf = _release_leaf(kind, entry["name"]) if kind in PIPELINES else _leaf(entry["name"])
        if leaf is None or leaf == _AGGREGATE_JOB:
            continue
        name = " ".join(_UNSAFE_NAME.sub("", leaf).split())
        if name and pattern.match(leaf):
            conclusion = entry.get("conclusion")
            entries.append({"name": name, "conclusion": conclusion if conclusion in contract.CONCLUSIONS else None})
    return entries


def _read_archive(data, member="evidence.json"):
    if len(data) > ARCHIVE_LIMIT:
        raise ValueError("The archive is too large")
    try:
        archive = zipfile.ZipFile(io.BytesIO(data))
    except zipfile.BadZipFile:
        raise ValueError("The artifact is not an archive") from None
    with archive:
        entries = archive.infolist()
        if len(entries) != 1 or entries[0].filename != member:
            raise ValueError("The archive must hold only " + member)
        entry = entries[0]
        if entry.is_dir() or (entry.external_attr >> 16) & 0o170000 == 0o120000:
            raise ValueError("The archive entry is not a regular file")
        if entry.file_size > contract.JSON_LIMIT:
            raise ValueError("The evidence is too large")
        with archive.open(entry) as handle:
            raw = handle.read(contract.JSON_LIMIT + 1)
    if len(raw) > contract.JSON_LIMIT:
        raise ValueError("The evidence is too large")
    try:
        return contract.load_json(raw.decode("utf-8"))
    except UnicodeDecodeError:
        raise ValueError("The evidence is not UTF-8") from None


def _check_identity(record, kind, run, path, flavor):
    contract.validate_record(record)
    expected = {"kind": kind, "repository": run["repository"]["full_name"], "run_id": run["id"],
                "run_attempt": run["run_attempt"], "workflow_path": path}
    if kind != "regression":
        # A QA record names the tests commit, which need not be the commit of the run.
        expected["sha"] = run["head_sha"]
    if flavor:
        expected["flavor"] = flavor
    for key, value in expected.items():
        if record.get(key) != value:
            raise ValueError("The record does not belong to this run")


class Export(enum.Enum):
    MISSING = "is missing"
    EXPIRED = "has expired"
    INVALID = "is invalid"


def _load_export(api, artifacts, name, kind, run, path, flavor=None):
    """Return the verified record, or the reason there is none."""
    matches = [artifact for artifact in artifacts if isinstance(artifact, dict) and artifact.get("name") == name]
    if not matches:
        return None, Export.MISSING
    if len(matches) > 1:
        return None, Export.INVALID
    if matches[0].get("expired"):
        return None, Export.EXPIRED
    url = matches[0].get("archive_download_url")
    if not isinstance(url, str):
        return None, Export.INVALID
    try:
        record = _read_archive(api.download(url))
        _check_identity(record, kind, run, path, flavor)
    except (ValueError, ApiError):
        return None, Export.INVALID
    return record, None


def _context_gap(reason, attempt, context_job_ran):
    """Return the gap text and whether it makes the evidence incomplete."""
    if reason is Export.MISSING and attempt > 1 and not context_job_ran:
        return "Release context belongs to an earlier attempt", False
    return "Release context export " + reason.value, True


def _build_gap(reason, attempt, build_ran, build_jobs):
    if reason is Export.MISSING and attempt > 1 and not build_ran:
        return "Build did not run in this attempt; its evidence belongs to an earlier attempt", False
    if reason is Export.MISSING and build_jobs and not any(job["conclusion"] == "success" for job in build_jobs):
        return "No build record: the build produced no APK", False
    return "Build evidence export " + reason.value, True


def _verified_tag(api, run, repository):
    tag = run.get("head_branch")
    try:
        reference = api.get("/repos/%s/git/ref/tags/%s" % (repository, urllib.parse.quote(tag, safe="")))
        target = reference["object"]
        if target["type"] == "tag":
            target = api.get("/repos/%s/git/tags/%s" % (repository, target["sha"]))["object"]
        return tag if target["type"] == "commit" and target["sha"] == run["head_sha"] else None
    except (ValueError, KeyError, TypeError, ApiError):
        return None


def collect_event(repository, run_id, run_attempt, api=None):
    if not isinstance(repository, str) or not contract.REPOSITORY.fullmatch(repository):
        raise CollectionError("The repository name is invalid")
    if not contract.is_positive(run_id) or not contract.is_positive(run_attempt):
        raise CollectionError("The run ID and attempt must be positive integers")
    try:
        api = api or GitHubApi.from_env()
        base = "/repos/%s/actions/runs/%d" % (repository, run_id)
        run = api.get("%s/attempts/%d" % (base, run_attempt))
        path, kind = _validate_run(run, repository, run_id, run_attempt)
        default_branch = None
        if kind == "regression":
            default_branch = api.get("/repos/" + repository).get("default_branch")
        if not _is_release_run(run, kind, default_branch):
            return None
        all_jobs = _paginate(api, "%s/attempts/%d/jobs" % (base, run_attempt), "jobs")
        artifacts = _paginate(api, base + "/artifacts", "artifacts")
        if kind == "regression":
            return _assemble_regression(api, repository, run, path, all_jobs, artifacts, default_branch)
        return _assemble(api, repository, run, path, kind, all_jobs, artifacts)
    except ApiError as error:
        raise CollectionError("GitHub request failed: " + str(error)) from None


def _assemble(api, repository, run, path, kind, all_jobs, artifacts):
    run_id, attempt, conclusion = run["id"], run["run_attempt"], run["conclusion"]
    flavor = BUILD_FLAVORS[kind]
    jobs = _release_jobs(all_jobs, kind)
    leaves = [_release_leaf(kind, entry.get("name")) or "" for entry in all_jobs if isinstance(entry, dict)]
    gaps, incomplete = [], False

    context, reason = _load_export(api, artifacts, "release-log-context-%d-%d" % (run_id, attempt),
                                   "context", run, path)
    if reason:
        gap, failed = _context_gap(reason, attempt, CONTEXT_JOB in leaves)
        gaps.append(gap)
        incomplete |= failed

    build, reason = _load_export(api, artifacts, "release-log-build-%s-%d-%d" % (flavor, run_id, attempt),
                                 "build", run, path, flavor)
    if reason:
        build_jobs = [entry for entry in jobs if _BUILD_JOB.match(entry["name"])]
        gap, failed = _build_gap(reason, attempt, any(_BUILD_JOB.match(leaf) for leaf in leaves), build_jobs)
        gaps.append(gap)
        incomplete |= failed

    if context and build and any(context[key] != build[key] for key in ("tag", "sha", "workflow_path")):
        raise CollectionError("The release context and build records disagree")
    record_tag = (context or build or {}).get("tag")
    if record_tag is not None and record_tag != run.get("head_branch"):
        raise CollectionError("The release tag does not match the run")
    tag = record_tag or _verified_tag(api, run, repository)
    if not tag:
        raise CollectionError("The release version cannot be established")
    payload = {
        "schema_version": contract.SCHEMA_VERSION,
        "release_version": contract.release_version(tag),
        "event_id": "%s:%s:%d:%d" % (kind, repository, run_id, attempt),
        "kind": kind,
        "evidence": {
            "source": {"repository": repository, "run_id": run_id, "run_attempt": attempt, "workflow_path": path,
                       "url": "https://github.com/%s/actions/runs/%d/attempts/%d" % (repository, run_id, attempt),
                       "tag": tag, "sha": run["head_sha"]},
            "conclusion": conclusion, "jobs": jobs, "build": build, "qa": None, "origin": None, "gaps": gaps,
        },
    }
    try:
        contract.validate_payload(payload)
    except ValueError:
        raise CollectionError("The collected evidence is invalid") from None
    if incomplete and conclusion == "success":
        raise CollectionError("Evidence is incomplete", partial=payload)
    return payload


_LABEL = re.compile(r"((?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*))(?:-([1-9][0-9]*))?")
_UNVERIFIED_LISTING = "production builds could not be fully enumerated"
NOT_ATTACHED = "this regression is not attached to a build page"


def _parse_time(value):
    try:
        return datetime.strptime(value, "%Y-%m-%dT%H:%M:%SZ")
    except (TypeError, ValueError):
        return None


def _label_parts(label):
    match = _LABEL.fullmatch(label) if isinstance(label, str) else None
    return match.groups() if match else None


def match_production_build(qa, manifests, unverified=()):
    """Find the one production build that produced the tested APK.

    `manifests` holds verified build records. `unverified` lists the reasons the enumeration of production
    builds is incomplete. The result has the matched `origin` and `release_version`, or a `gap` that says why
    there is no match. The digest and file name identify the APK. The version label is display only and never
    vetoes a match. The requested `latest` and the tests commit never identify the APK.
    """
    result = {"origin": None, "release_version": None, "gap": None}
    apk = qa.get("apk") if isinstance(qa, dict) else None
    if apk is None:
        result["gap"] = "the tested APK was not recorded"
        return result
    if unverified:
        result["gap"] = "production build enumeration is incomplete (%s)" % ", ".join(unverified)
        return result
    same = [build for build in manifests if isinstance(build, dict) and build.get("kind") == "build"
            and build.get("flavor") == "Prod"
            and any(item.get("filename") == apk["filename"] and item.get("sha256") == apk["sha256"]
                    for item in build.get("apks", []))]
    matches = same
    if len(matches) > 1:
        result["gap"] = "more than one production build has this APK digest and file name"
    elif not matches:
        result["gap"] = ("no production build from the preceding %d days has this APK digest and file name"
                         % ORIGIN_WINDOW_DAYS)
    else:
        build = matches[0]
        result["origin"] = {"run_id": build["run_id"], "run_attempt": build["run_attempt"],
                            "url": contract.run_url(build["repository"], build["run_id"], build["run_attempt"])}
        result["release_version"] = contract.release_version(build["tag"])
    return result


def _check_origin_run(run, repository, run_id, attempt):
    full_name = run.get("repository", {}).get("full_name") if isinstance(run, dict) and isinstance(
        run.get("repository"), dict) else None
    raw = run.get("path") if isinstance(run, dict) else None
    if (not isinstance(run, dict) or not _is_release_run(run, "production") or run.get("id") != run_id
            or run.get("run_attempt") != attempt
            or full_name != repository or not isinstance(raw, str) or raw.split("@", 1)[0] != PROD_BUILD_PATH
            or not isinstance(run.get("head_sha"), str) or not contract.SHA.fullmatch(run["head_sha"])):
        raise CollectionError("The production run does not match the request")


def _production_manifests(api, repository, qa_run):
    """Return the verified production build records of the preceding days and why the list may be incomplete."""
    created = _parse_time(qa_run.get("created_at"))
    if created is None:
        return [], ["the regression run time is unknown"]
    since = created - timedelta(days=ORIGIN_WINDOW_DAYS)
    listing = "/repos/%s/actions/workflows/%s/runs" % (repository, PROD_BUILD_PATH.rsplit("/", 1)[-1])
    try:
        runs = _paginate(api, listing, "workflow_runs", {"created": ">=" + since.date().isoformat()})
    except (ApiError, CollectionError):
        return [], [_UNVERIFIED_LISTING]
    manifests, problems = [], []

    def problem(text):
        if text not in problems:
            problems.append(text)

    seen = set()
    for listed in runs:
        if not isinstance(listed, dict) or not contract.is_positive(listed.get("id")) or listed["id"] in seen:
            continue
        repo = listed.get("repository")
        raw, started = listed.get("path"), _parse_time(listed.get("created_at"))
        if (not isinstance(repo, dict) or repo.get("full_name") != repository or not isinstance(raw, str)
                or raw.split("@", 1)[0] != PROD_BUILD_PATH or started is None or not since <= started <= created
                or listed.get("status") != "completed" or not contract.is_positive(listed.get("run_attempt"))):
            continue
        seen.add(listed["id"])
        base = "/repos/%s/actions/runs/%d" % (repository, listed["id"])
        try:
            artifacts = _paginate(api, base + "/artifacts", "artifacts")
        except (ApiError, CollectionError):
            problem(_UNVERIFIED_LISTING)
            continue
        names = {artifact.get("name") for artifact in artifacts if isinstance(artifact, dict)}
        for attempt in range(1, listed["run_attempt"] + 1):
            name = "release-log-build-Prod-%d-%d" % (listed["id"], attempt)
            if name not in names:
                continue
            try:
                run = api.get("%s/attempts/%d" % (base, attempt))
                _check_origin_run(run, repository, listed["id"], attempt)
            except (ApiError, CollectionError):
                problem(_UNVERIFIED_LISTING)
                continue
            record, reason = _load_export(api, artifacts, name, "build", run, PROD_BUILD_PATH, "Prod")
            if reason:
                problem("a production build record " + reason.value)
            elif record["tag"] != run.get("head_branch"):
                problem("a production build record is invalid")
            else:
                manifests.append(record)
    return manifests, problems


def _load_source(api, repository, run_id, attempt, default_branch):
    """Return the exact source QA record of a deflake, or the reason there is none."""
    try:
        run = api.get("/repos/%s/actions/runs/%d/attempts/%d" % (repository, run_id, attempt))
    except ApiError:
        return None, "source run could not be read"
    try:
        path, kind = _validate_run(run, repository, run_id, attempt)
    except CollectionError:
        return None, "source run is not an eligible regression"
    if kind != "regression" or not _is_release_run(run, kind, default_branch):
        return None, "source run is not an eligible regression"
    try:
        artifacts = _paginate(api, "/repos/%s/actions/runs/%d/artifacts" % (repository, run_id), "artifacts")
    except (ApiError, CollectionError):
        return None, "source run could not be read"
    record, reason = _load_export(api, artifacts, "release-log-qa-%d-%d" % (run_id, attempt), "regression", run, path)
    if reason:
        return None, {Export.MISSING: "source record is missing", Export.EXPIRED: "source record has expired",
                      Export.INVALID: "source record is invalid"}[reason]
    return record, None


def _verify_ancestry(api, repository, qa, default_branch):
    """Return None when every source of a deflake is verified, else the reason it is not."""
    seen = {(qa["run_id"], qa["run_attempt"])}
    current = qa
    for _ in range(MAX_ANCESTRY):
        if current["mode"] == "full":
            return None
        key = (current["source_qa_run_id"], current["source_qa_run_attempt"])
        if key in seen:
            return "source chain repeats"
        seen.add(key)
        source, reason = _load_source(api, repository, *key, default_branch)
        if reason:
            return reason
        if current["source_deflake_bundle_sha256"] is None or source["next_deflake_bundle_sha256"] is None:
            return "bundle is unverified"
        if current["source_deflake_bundle_sha256"] != source["next_deflake_bundle_sha256"]:
            return "bundle fingerprint differs"
        if source["test_source_sha"] != current["test_source_sha"]:
            return "test revision differs"
        if any(source[field] != current[field] for field in ("suite", "selector", "flavor")):
            return "scope differs"
        current = source
    return None if current["mode"] == "full" else "source chain is too long"


def _qa_gap(reason, attempt, qa_ran):
    if reason is Export.MISSING and attempt > 1 and not qa_ran:
        return "Regression evidence belongs to an earlier attempt", False
    return "Regression evidence export " + reason.value, True


def _pages_prefix(repository):
    owner, name = repository.split("/")
    return "https://%s.github.io/%s/" % (owner.lower(), name)


def _regression_version(origin_version, qa):
    if origin_version:
        return origin_version
    parts = _label_parts(qa["apk"]["resolved_version_label"]) if qa is not None and qa["apk"] else None
    return parts[0] if parts else contract.UNMATCHED


def _attribute(api, repository, run, qa, gaps, default_branch):
    """Return the production origin and the release version of a regression, and add the gaps."""
    reason, origin, version = None, None, None
    if qa is None:
        reason = "there is no regression record"
    else:
        failure = _verify_ancestry(api, repository, qa, default_branch) if qa["mode"] == "deflake" else None
        if failure:
            gaps.append("Deflake source is not verified (%s)" % failure)
            reason = "the deflake source is not verified"
        elif qa["apk"] is not None:
            manifests, problems = _production_manifests(api, repository, run)
            match = match_production_build(qa, manifests, problems)
            origin, version, reason = match["origin"], match["release_version"], match["gap"]
        else:
            reason = match_production_build(qa, [])["gap"]
    if reason:
        gaps.append("Tested APK matched no release build (%s): %s" % (reason, NOT_ATTACHED))
    return origin, _regression_version(version, qa)


def _is_skipped(api, artifacts, repository, run_id, attempt):
    """True when the exporter marked this exact run and attempt as not a production regression."""
    name = "release-log-qa-skip-%d-%d" % (run_id, attempt)
    matches = [artifact for artifact in artifacts if isinstance(artifact, dict) and artifact.get("name") == name]
    if len(matches) != 1 or matches[0].get("expired") or not isinstance(matches[0].get("archive_download_url"), str):
        return False
    try:
        marker = _read_archive(api.download(matches[0]["archive_download_url"]), "skip.json")
    except (ValueError, ApiError):
        return False
    return marker == contract.skip_marker(repository, run_id, attempt)


def _assemble_regression(api, repository, run, path, all_jobs, artifacts, default_branch):
    run_id, attempt, conclusion = run["id"], run["run_attempt"], run["conclusion"]
    if run.get("event") != "workflow_dispatch":
        raise CollectionError("The run is not a manual production regression")
    jobs = _release_jobs(all_jobs, "regression", _QA_JOB)
    qa_ran = any(_QA_RUN_JOB.match(_leaf(entry.get("name"))) for entry in all_jobs if isinstance(entry, dict))
    gaps, incomplete = [], False
    qa, reason = _load_export(api, artifacts, "release-log-qa-%d-%d" % (run_id, attempt), "regression", run, path)
    if reason is Export.MISSING and _is_skipped(api, artifacts, repository, run_id, attempt):
        return None
    if reason:
        gap, failed = _qa_gap(reason, attempt, qa_ran)
        gaps.append(gap)
        incomplete |= failed
    if qa is not None and qa["report_url"] and not qa["report_url"].startswith(_pages_prefix(repository)):
        qa = dict(qa, report_url=None)
        gaps.append("Report link is not on the repository pages site")
    origin, version = _attribute(api, repository, run, qa, gaps, default_branch)
    payload = {
        "schema_version": contract.SCHEMA_VERSION,
        "release_version": version,
        "event_id": "regression:%s:%d:%d" % (repository, run_id, attempt),
        "kind": "regression",
        "evidence": {
            "source": {"repository": repository, "run_id": run_id, "run_attempt": attempt, "workflow_path": path,
                       "url": contract.run_url(repository, run_id, attempt), "tag": None, "sha": run["head_sha"]},
            "conclusion": conclusion, "jobs": jobs, "build": None, "qa": qa, "origin": origin, "gaps": gaps,
        },
    }
    try:
        contract.validate_payload(payload)
    except ValueError:
        raise CollectionError("The collected evidence is invalid") from None
    if incomplete and conclusion == "success":
        raise CollectionError("Evidence is incomplete", partial=payload)
    return payload


PUBLISH_TIMEOUT = 20
_HOST = re.compile(r"[A-Za-z0-9.-]+")
_TOKEN = re.compile(r"[\x21-\x7e]+")


class PublishError(Exception):
    """Delivery failed. The message never carries the webhook URL, token, or response body."""


def _check_fields(fields):
    if (not isinstance(fields, dict) or set(fields) != set(contract.FIELD_NAMES)
            or not all(isinstance(value, str) for value in fields.values())):
        raise ValueError("The webhook fields do not match the contract")


def _check_webhook(url, token):
    try:
        parts = urllib.parse.urlsplit(url)
        host = parts.hostname
    except (ValueError, AttributeError):
        raise ValueError("The webhook URL is invalid") from None
    if (parts.scheme != "https" or not host or not _HOST.fullmatch(host) or parts.username or parts.password
            or re.search(r"\s", url)):
        raise ValueError("The webhook URL must be an HTTPS URL")
    if not isinstance(token, str) or not _TOKEN.fullmatch(token):
        raise ValueError("The webhook token is missing or invalid")


def publish(fields, publication_enabled, webhook_url, token, dry_run=False):
    _check_fields(fields)
    if dry_run:
        print("Release log: dry run, nothing sent")
        return
    if not publication_enabled:
        print("Release log: publication is disabled, nothing sent")
        return
    _check_webhook(webhook_url, token)
    try:
        request = urllib.request.Request(
            webhook_url, data=json.dumps(fields).encode("utf-8"), method="POST",
            headers={"Content-Type": "application/json", "X-Automation-Webhook-Token": token})
        with _open(request, PUBLISH_TIMEOUT) as response:
            status = response.status
    except urllib.error.HTTPError as error:
        raise PublishError("The webhook returned HTTP %d" % error.code) from None
    except Exception:
        raise PublishError("The webhook request failed") from None
    if not 200 <= status < 300:
        raise PublishError("The webhook returned HTTP %d" % status)
    print("Release log: accepted for processing")


def _collect_command(arguments):
    repository = arguments.repository or os.environ.get("GITHUB_REPOSITORY", "")
    try:
        payload = collect_event(repository, contract.parse_positive(arguments.run_id, "run-id"),
                                contract.parse_positive(arguments.run_attempt, "run-attempt"))
    except CollectionError as error:
        print("Release log collection failed: " + error.reason, file=sys.stderr)
        if error.partial:
            contract.write_json(arguments.output, error.partial)
        return 1
    if payload is None:
        print("Release log: run %d is not a release run, nothing to report" % int(arguments.run_id))
        return 0
    contract.write_json(arguments.output, payload)
    return 0


def _publish_command(arguments):
    fields = contract.build_fields(contract.read_json(arguments.payload))
    if arguments.summary:
        with open(arguments.summary, "a", encoding="utf-8") as handle:
            handle.write("### Release log entry\n\n````json\n" + contract.dump_json(fields) + "````\n")
    publish(fields, os.environ.get("RELEASE_LOG_PUBLICATION_ENABLED") == "true",
            os.environ.get("RELEASE_LOG_WEBHOOK_URL", ""), os.environ.get("RELEASE_LOG_WEBHOOK_TOKEN", ""),
            dry_run=arguments.dry_run)
    return 0


def main(argv=None):
    parser = argparse.ArgumentParser(description="Collect release log evidence and send it to Confluence.")
    commands = parser.add_subparsers(dest="command", required=True)
    collect = commands.add_parser("collect", help="Write the evidence payload for one workflow run")
    collect.add_argument("--repository")
    collect.add_argument("--run-id", required=True)
    collect.add_argument("--run-attempt", required=True)
    collect.add_argument("--output", required=True)
    collect.set_defaults(run=_collect_command)
    publish_parser = commands.add_parser("publish", help="Send the webhook fields for a payload")
    publish_parser.add_argument("--payload", required=True)
    publish_parser.add_argument("--summary")
    publish_parser.add_argument("--dry-run", action="store_true")
    publish_parser.set_defaults(run=_publish_command)
    arguments = parser.parse_args(argv)
    try:
        return arguments.run(arguments)
    except PublishError as error:
        print("Release log delivery failed: " + str(error), file=sys.stderr)
    except (OSError, ValueError, CollectionError) as error:
        print("Release log failed: " + str(error), file=sys.stderr)
    return 1


if __name__ == "__main__":
    sys.exit(main())
