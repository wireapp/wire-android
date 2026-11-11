#!/usr/bin/env python3
"""Classify a pull request diff into a review tier.

Tier 0: skip the automatic review.
Tier 1: light review.
Tier 2: deep review.

Usage:
    classify.py --diff <unified.diff> [--author <login>] [--kalium-repo <path>]

The script prints one JSON object with `tier`, `score` and `reasons`.
The result depends only on the diff, the author and, for a Kalium bump, the Kalium
checkout that `--kalium-repo` names. The script does not use the network itself, but a
blobless checkout fetches file contents on demand.
"""

import argparse
import json
import re
import subprocess
import sys
from collections import OrderedDict

BOT_AUTHORS = {
    "renovate[bot]",
    "dependabot[bot]",
    "github-actions[bot]",
    "wire-crowdin-bot",
}

# Modules whose public declarations other modules depend on.
CI_PREFIXES = (".github/workflows/", ".github/review/")

SHARED_PREFIXES = ("core/", "features/", "kalium/")

TEST_SEGMENTS = (
    "/src/test/",
    "/src/androidTest/",
    "/src/commonTest/",
    "/src/jvmTest/",
    "/src/androidDeviceTest/",
    "/src/testFixtures/",
)

# Paths that always start the deep review.
HARD_TRIGGERS = (
    ("kalium-pointer", re.compile(r"^kalium$")),
    ("di", re.compile(r"(^|/)di/")),
    ("navigation", re.compile(r"(^|/)(navigation|navigation-kmp)/")),
    ("database", re.compile(r"\.sqm?$|(^|/)db_?migrations?/")),
    # Workflows and review prompts control secrets, permissions and what the reviewer sees.
    ("ci", re.compile(r"^\.github/(workflows|review)/")),
)

SKIP_PATTERNS = (
    re.compile(r"\.md$"),
    re.compile(r"^docs/"),
    re.compile(r"^\.scratch/"),
    re.compile(r"^kalium/changelog\.d/"),
    re.compile(r"(^|/)res/values[^/]*/strings\.xml$"),
    re.compile(r"^crowdin\.yml$"),
    # Generated files.
    re.compile(r"(^|/)(baseline|startup)-prof\.txt$"),
    re.compile(r"\.stability$"),
    # The version bump changes one constant and nothing else.
    re.compile(r"^build-logic/plugins/src/main/kotlin/AndroidCoordinates\.kt$"),
)

BUILD_PATTERNS = (
    re.compile(r"(^|/)build\.gradle\.kts$"),
    re.compile(r"\.gradle\.kts$"),
    re.compile(r"^gradle/libs\.versions\.toml$"),
    re.compile(r"^(buildSrc|build-logic)/"),
    re.compile(r"^docs/adr/"),
)

# A declaration line that other code can see. `override`, `private`,
# `internal` and `protected` members are not part of the outer surface.
# Only top-level lines (no indent) and member lines (one indent level of four spaces
# or one tab) count, so locals inside function bodies are ignored. A `val` or `var`
# at member depth also matches locals of top-level functions, so it counts only with
# an explicit type, which locals rarely have.
DECLARATION = re.compile(
    r"^[+-](?P<indent>| {1,4}|\t)"
    r"(?!\s)(?!.*\b(private|internal|protected|override)\b)"
    r"(@\w+(\([^)]*\))?\s+)*"
    r"(public\s+)?(abstract\s+|open\s+|sealed\s+|data\s+|enum\s+|value\s+|inline\s+|suspend\s+|fun\s+"
    r"|expect\s+|actual\s+|const\s+|operator\s+|infix\s+|annotation\s+|lateinit\s+|external\s+|tailrec\s+)*"
    r"(?P<kind>interface|class|object|fun|val|var|typealias)\b(?P<rest>.*)"
)
TYPED_PROPERTY = re.compile(r"^\s+[\w.<>?]+\s*:")


def is_declaration(line):
    match = DECLARATION.match(line)
    if not match:
        return False
    if match.group("kind") in ("val", "var") and match.group("indent"):
        return bool(TYPED_PROPERTY.match(match.group("rest")))
    return True


# Kalium keeps ABI dumps for `logic` only. Other modules use the text heuristic.
KALIUM_API_DUMPS = re.compile(r"^logic/api/.+\.api$")
KALIUM_PERSISTENCE = re.compile(r"\.sqm?$|(^|/)db_?migrations?/")
KALIUM_CHANGED_FRAGMENT = re.compile(r"^changelog\.d/.+\.changed\.md$")
KALIUM_POINTER = re.compile(r"^diff --git a/kalium b/kalium\nindex ([0-9a-f]+)\.\.([0-9a-f]+)", re.M)
SHA = re.compile(r"^[0-9a-f]{7,40}$")

TIER_2_SCORE = 3
DISPERSION_MODULES = 3
LARGE_PRODUCTION_DIFF = 150


# `--- a/x`, `+++ b/x` and the `/dev/null` forms. A removed line whose text starts
# with "-- " (a SQL comment, a Markdown rule) is content and must be counted.
FILE_HEADER = re.compile(r"^(--- (a/|/dev/null)|\+\+\+ (b/|/dev/null))")


def parse_diff(text):
    """Return an ordered map of path to {added, removed, lines}."""
    files = OrderedDict()
    current = None
    for line in text.splitlines():
        header = re.match(r"^diff --git a/(.+?) b/(.+)$", line)
        if header:
            current = files.setdefault(
                header.group(2), {"added": 0, "removed": 0, "lines": []}
            )
            continue
        if current is None:
            continue
        if FILE_HEADER.match(line):
            continue
        if line.startswith("+"):
            current["added"] += 1
            current["lines"].append(line)
        elif line.startswith("-"):
            current["removed"] += 1
            current["lines"].append(line)
    return files


def kalium_range(text):
    """Return (old, new) commit ids when the diff moves the kalium pointer."""
    match = KALIUM_POINTER.search(text)
    return (match.group(1), match.group(2)) if match else None


def inspect_kalium(repo, old, new):
    """Summarize the change between two Kalium commits.

    Returns {"unavailable": True} when git cannot produce the diff.
    """
    if not (SHA.match(old) and SHA.match(new)):
        return {"unavailable": True}
    result = subprocess.run(
        ["git", "-C", repo, "diff", "--no-color", f"{old}..{new}", "--"],
        capture_output=True, text=True, errors="replace",
    )
    if result.returncode != 0:
        return {"unavailable": True}
    files = parse_diff(result.stdout)
    api_added = api_removed = 0
    for path, info in files.items():
        if KALIUM_API_DUMPS.match(path):
            api_added += info["added"]
            api_removed += info["removed"]
    # The ABI dump covers `logic`. The text heuristic covers the rest.
    heuristic = OrderedDict(
        ("kalium/" + path, info)
        for path, info in files.items()
        if not path.startswith("logic/") and not is_test(path)
    )
    return {
        "unavailable": False,
        "api_added": api_added,
        "api_removed": api_removed,
        "persistence": [p for p in files if KALIUM_PERSISTENCE.search(p)],
        "changed_fragments": [p for p in files if KALIUM_CHANGED_FRAGMENT.match(p)],
        "heuristic_files": heuristic,
        "file_count": len(files),
    }


def surface_score(added, removed, where):
    """Return (score, reason) for public declarations added or removed."""
    if removed:
        return 3, f"{removed} public declaration(s) removed or changed {where}"
    if added >= 5:
        return 2, f"{added} public declarations added {where}"
    if added:
        return 1, f"{added} public declaration(s) added {where}"
    return 0, None


def kalium_findings(kalium):
    """Return (hard_reasons, score, reasons) for an inspected Kalium bump."""
    if kalium["unavailable"]:
        return ["kalium bump cannot be inspected"], 0, []
    hard, reasons, score = [], [], 0
    if kalium["api_removed"]:
        hard.append(f"kalium ABI dump: {kalium['api_removed']} line(s) removed or changed")
    if kalium["persistence"]:
        hard.append(f"kalium persistence change: {kalium['persistence'][0]}")
    if kalium["changed_fragments"]:
        hard.append(f"kalium changelog marks a behavior change: {kalium['changed_fragments'][0]}")
    if hard:
        return hard, 0, []
    if kalium["api_added"]:
        score += 1
        reasons.append(f"kalium ABI dump: {kalium['api_added']} line(s) added")
    surface, reason = surface_score(*public_surface_changes(kalium["heuristic_files"]), "outside logic")
    if reason:
        score += surface
        reasons.append("kalium: " + reason)
    return [], score, reasons


def is_test(path):
    return any(segment in "/" + path for segment in TEST_SEGMENTS)


def is_skippable(path):
    if path.startswith(CI_PREFIXES):
        return False
    return any(pattern.search(path) for pattern in SKIP_PATTERNS)


def module_of(path):
    parts = path.split("/")
    if parts[0] in ("core", "features") and len(parts) > 1:
        return "/".join(parts[:2])
    return parts[0]


def public_surface_changes(files):
    """Count added and removed public declarations in shared modules."""
    added = removed = 0
    for path, info in files.items():
        if not path.endswith(".kt") or is_test(path):
            continue
        if not path.startswith(SHARED_PREFIXES):
            continue
        for line in info["lines"]:
            if is_declaration(line):
                if line.startswith("+"):
                    added += 1
                else:
                    removed += 1
    return added, removed


def classify(files, author="", kalium=None):
    reasons = []
    paths = list(files)

    if author in BOT_AUTHORS:
        return {"tier": 0, "score": 0, "reasons": [f"bot author {author}"]}
    if not paths:
        return {"tier": 0, "score": 0, "reasons": ["empty diff"]}
    if all(is_skippable(p) for p in paths):
        return {"tier": 0, "score": 0, "reasons": ["docs, strings, changelog or generated files only"]}
    if all(is_test(p) for p in paths):
        return {"tier": 0, "score": 0, "reasons": ["tests only"]}

    candidates = [p for p in paths if not is_test(p) and not is_skippable(p)]
    for name, pattern in HARD_TRIGGERS:
        if name == "kalium-pointer" and kalium is not None:
            continue
        hits = [p for p in candidates if pattern.search(p)]
        if hits:
            reasons.append(f"hard trigger {name}: {hits[0]}")
    score = 0
    if kalium is not None and "kalium" in paths:
        hard, kalium_score, kalium_reasons = kalium_findings(kalium)
        reasons.extend(f"hard trigger {r}" for r in hard)
        score += kalium_score
        soft_reasons = kalium_reasons
    else:
        soft_reasons = []
    if reasons:
        return {"tier": 2, "score": score, "reasons": reasons}
    reasons = soft_reasons

    production = [p for p in paths if p.endswith((".kt", ".java")) and not is_test(p)]
    tests = [p for p in paths if is_test(p)]

    surface, reason = surface_score(*public_surface_changes(files), "in a shared module")
    if reason:
        score += surface
        reasons.append(reason)

    if production and not tests:
        score += 1
        reasons.append("production code changed without a test change")
        changed = sum(files[p]["added"] + files[p]["removed"] for p in production)
        if changed > LARGE_PRODUCTION_DIFF:
            score += 1
            reasons.append(f"{changed} production lines changed")

    build_hits = [p for p in paths if any(pat.search(p) for pat in BUILD_PATTERNS)]
    if build_hits:
        score += 2
        reasons.append(f"build, dependency or ADR file: {build_hits[0]}")

    modules = {module_of(p) for p in production}
    if len(modules) >= DISPERSION_MODULES:
        score += 2
        reasons.append(f"{len(modules)} modules touched")

    tier = 2 if score >= TIER_2_SCORE else 1
    if not reasons:
        reasons.append("no risk signal")
    return {"tier": tier, "score": score, "reasons": reasons}


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--diff", required=True, help="path to a unified diff")
    parser.add_argument("--author", default="", help="PR author login")
    parser.add_argument("--kalium-repo", help="path to the Kalium checkout, to inspect pointer bumps")
    args = parser.parse_args(argv)
    with open(args.diff, encoding="utf-8", errors="replace") as handle:
        text = handle.read()
    kalium = None
    moved = kalium_range(text)
    if moved and args.kalium_repo:
        kalium = inspect_kalium(args.kalium_repo, *moved)
    json.dump(classify(parse_diff(text), args.author, kalium), sys.stdout)
    sys.stdout.write("\n")


if __name__ == "__main__":
    main(sys.argv[1:])
