#!/usr/bin/env python3
"""Register device identifiers before they reach public Actions logs."""
import json
import os
from pathlib import Path
import shutil
import subprocess


def mask(value: str) -> None:
    if value:
        # Escape workflow-command characters so the ID stays within one add-mask instruction.
        value = value.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
        print(f"::add-mask::{value}", flush=True)


def main() -> None:
    # Hide an explicitly selected device in later console output; the dispatch input itself is unchanged.
    event = json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text())
    mask(str((event.get("inputs") or {}).get("androidDeviceId") or ""))
    # Register each configured ID separately; masking the full JSON does not mask every ID on its own.
    groups = json.loads(os.environ.get("DEVICE_GROUPS_JSON") or "{}")
    if not isinstance(groups, dict):
        raise ValueError("Device group configuration must be an object")
    for devices in groups.values():
        if isinstance(devices, list):
            for device in devices:
                if isinstance(device, str):
                    mask(device)
    # Capture the connected-device list without printing it, then mask IDs not covered by configuration.
    if shutil.which("adb"):
        result = subprocess.run(["adb", "devices"], capture_output=True, text=True, timeout=15, check=False)
        for line in result.stdout.splitlines()[1:]:
            if line.split():
                mask(line.split()[0])


if __name__ == "__main__":
    main()
