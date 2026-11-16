#!/usr/bin/env python3
"""Approve Xiaomi's USB install prompt during CI setup.

MIUI optimization must stay enabled for file-sharing URI permissions to work.
With it enabled, fresh Wire and test APK installs require USB approval, which
otherwise times out and fails CI. Approve only the expected app's Install prompt
before instrumentation starts. Other phones use the original ADB install command.
"""
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

serial, package, *command = sys.argv[1:]
adb_command = ["adb", "-s", serial]
hierarchy = "/data/local/tmp/wire-install.xml"


def adb(*args):
    return subprocess.run(adb_command + list(args), capture_output=True, text=True, timeout=10)


if adb("shell", "getprop", "ro.product.manufacturer").stdout.strip().lower() != "xiaomi":
    sys.exit(subprocess.call(adb_command + command))
label = {"com.wire.internal": "Wire Alpha", "com.wire.android.internal": "Wire Beta", "com.wire": "Wire"}.get(package, package)
process = subprocess.Popen(adb_command + command)
try:
    for _ in range(12):
        if process.poll() is not None:
            break
        try:
            if "dumped to:" not in adb("shell", "uiautomator", "dump", hierarchy).stdout:
                continue
            nodes = list(ET.fromstring(adb("shell", "cat", hierarchy).stdout).iter("node"))
            if not {"Install via USB", label}.issubset({node.get("text") for node in nodes}):
                continue
            button = next((node for node in nodes if node.get("text") == "Install" and node.get("enabled") == "true"), None)
            if button is not None:
                left, top, right, bottom = map(int, re.findall(r"\d+", button.get("bounds")))
                adb("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))
        except (ET.ParseError, subprocess.TimeoutExpired):
            continue
finally:
    try:
        adb("shell", "rm", "-f", hierarchy)
    except subprocess.TimeoutExpired:
        pass
status = process.wait()
if status == 0 and package == "com.wire.android.tests.testsCore.test":
    # A fresh MIUI install also blocks the test runner from launching Wire in the background.
    permission = adb("shell", "cmd", "appops", "set", package, "10021", "allow")
    if permission.returncode:
        error = permission.stderr.strip() or permission.stdout.strip() or "Could not allow the test runner to launch Wire."
        print(error, file=sys.stderr)
        sys.exit(permission.returncode)
sys.exit(status)
