#!/usr/bin/env python3
"""Typecheck sampled public SDK Swift calls against an already linked macOS framework."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
SDK = ROOT / "kalium"
if Path(subprocess.check_output(["git", "rev-parse", "--show-toplevel"], cwd=SDK, text=True).strip()) != SDK:
    raise SystemExit("Unexpected SDK Git top-level")
frameworks = SDK / "logic/build/bin/macosArm64/debugFramework"
if not (frameworks / "KaliumLogic.framework").exists():
    raise SystemExit("First run Kalium :logic:linkDebugFrameworkMacosArm64")
cache = SDK / "build/metro-verification/swift-sdk-api/module-cache"
cache.mkdir(parents=True, exist_ok=True)
apple_sdk = subprocess.check_output(["xcrun", "--sdk", "macosx", "--show-sdk-path"], text=True).strip()
print("SDK " + subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=SDK, text=True).strip(), flush=True)
raise SystemExit(subprocess.run([
    "xcrun", "swiftc", "-typecheck", "-swift-version", "5", "-target", "arm64-apple-macos15.0",
    "-sdk", apple_sdk, "-F", str(frameworks), "-module-cache-path", str(cache),
    str(Path(__file__).with_name("SDKAPICompatibility.swift")),
]).returncode)
