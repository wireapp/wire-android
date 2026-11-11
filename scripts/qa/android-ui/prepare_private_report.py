#!/usr/bin/env python3
"""Collect only test evidence and deflake inputs for the internal reporting repo."""
from __future__ import annotations

import gzip
import json
import os
from pathlib import Path
import shutil
import subprocess


def copy_evidence(source: Path, destination: Path, compress: bool = False) -> int:
    # Reject symbolic links at the root and below so copying cannot follow them to unrelated files.
    count = 0
    if source.is_symlink():
        raise ValueError("Evidence roots must not be symbolic links")
    if not source.is_dir():
        return count
    for item in sorted(source.rglob("*")):
        if item.is_symlink():
            raise ValueError("Test evidence must not contain symbolic links")
        if not item.is_file():
            continue
        target = destination / item.relative_to(source)
        target.parent.mkdir(parents=True, exist_ok=True)
        # Compress diagnostic text without changing its contents; keep Allure attachments unchanged.
        if compress and item.suffix in {".log", ".txt"}:
            target = target.with_name(target.name + ".gz")
            with item.open("rb") as src, gzip.open(target, "wb") as dst:
                shutil.copyfileobj(src, dst)
        else:
            shutil.copyfile(item, target)
        # Leave a margin below GitHub's per-file upload limit before the handoff pushes these files.
        if target.stat().st_size > 95 * 1024 * 1024:
            raise ValueError("An evidence file exceeds the GitHub file size limit")
        count += 1
    return count


def device_property(serial: str, name: str) -> str:
    # A disconnected device must not prevent collection of evidence already saved on the runner.
    try:
        result = subprocess.run(
            ["adb", "-s", serial, "shell", "getprop", name],
            capture_output=True, text=True, timeout=5, check=False,
        )
        return result.stdout.strip() if result.returncode == 0 else "unknown"
    except (OSError, subprocess.TimeoutExpired):
        return "unknown"


def main() -> None:
    destination = Path(os.environ["PRIVATE_REPORT_DIR"])
    # Require a new destination so evidence from separate runs cannot be mixed.
    destination.mkdir(parents=True, exist_ok=False)
    temporary = Path(os.environ["RUNNER_TEMP"])
    # Copy only these evidence directories, rather than the workspace, APKs, or runtime secrets file.
    roots = [
        (temporary / "allure-results", "results", False),
        (temporary / "instrumentation-logs", "logs/instrumentation", True),
        (Path(os.environ["DEFLAKE_BUNDLE_DIR"]), "deflake-input", False),
    ]
    if os.environ.get("EMULATOR_DIAGNOSTICS_DIR"):
        roots.append((Path(os.environ["EMULATOR_DIAGNOSTICS_DIR"]), "logs/emulator", True))
    count = sum(copy_evidence(src, destination / name, compress) for src, name, compress in roots)
    devices = {
        serial: {"model": device_property(serial, "ro.product.model"),
                 "sdk": device_property(serial, "ro.build.version.release")}
        for serial in os.environ.get("DEVICE_LIST", "").split()
    }
    # Associate the evidence with its Android run and retain real device details for the private report.
    metadata = {
        "source_repository": os.environ["GITHUB_REPOSITORY"],
        "run_id": os.environ["GITHUB_RUN_ID"],
        "run_attempt": os.environ["GITHUB_RUN_ATTEMPT"],
        "run_number": os.environ["GITHUB_RUN_NUMBER"],
        "source_sha": os.environ["GITHUB_SHA"],
        "title": os.environ.get("PRIVATE_REPORT_TITLE", "Android UI tests"),
        "suite": os.environ.get("PRIVATE_REPORT_SUITE", "qa-ui-tests"),
        "run_mode": os.environ.get("PRIVATE_REPORT_MODE", "normal"),
        "apk": os.environ.get("NEW_APK_NAME", ""),
        "tags": os.environ.get("PRIVATE_REPORT_TAGS", ""),
        "devices": devices,
    }
    (destination / "metadata.json").write_text(json.dumps(metadata, indent=2) + "\n")
    print(f"Collected {count} evidence files for private reporting.")


if __name__ == "__main__":
    main()
