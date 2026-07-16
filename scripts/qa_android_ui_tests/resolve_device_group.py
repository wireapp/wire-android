#!/usr/bin/env python3
"""Resolve one configured Android device group against online ADB devices."""

from __future__ import annotations

import json
import os
import sys
from collections.abc import Mapping


class DeviceGroupError(ValueError):
    """Raised when the device-group configuration is invalid or unusable."""


def parse_groups(raw_json: str) -> dict[str, list[str]]:
    if not raw_json.strip():
        raise DeviceGroupError(
            "ANDROID_DEVICE_GROUPS_JSON is empty. Configure it as a GitHub repository variable."
        )

    try:
        raw_groups = json.loads(raw_json)
    except json.JSONDecodeError as error:
        raise DeviceGroupError(f"ANDROID_DEVICE_GROUPS_JSON is not valid JSON: {error.msg}") from error

    if not isinstance(raw_groups, Mapping) or not raw_groups:
        raise DeviceGroupError("ANDROID_DEVICE_GROUPS_JSON must be a non-empty JSON object.")

    groups: dict[str, list[str]] = {}
    device_owners: dict[str, str] = {}
    for raw_name, raw_devices in raw_groups.items():
        if not isinstance(raw_name, str) or not raw_name.strip():
            raise DeviceGroupError("Every device group must have a non-empty string name.")
        name = raw_name.strip()
        if name != raw_name:
            raise DeviceGroupError(f"Device group name must not have surrounding whitespace: {raw_name!r}")
        if not isinstance(raw_devices, list) or not raw_devices:
            raise DeviceGroupError(f"Device group '{name}' must contain at least one device serial.")

        devices: list[str] = []
        for raw_device in raw_devices:
            if not isinstance(raw_device, str) or not raw_device.strip():
                raise DeviceGroupError(f"Device group '{name}' contains an invalid device serial.")
            device = raw_device.strip()
            if device != raw_device or any(character.isspace() for character in device):
                raise DeviceGroupError(f"Device serial must not contain whitespace: {raw_device!r}")
            if device in devices:
                raise DeviceGroupError(f"Device serial '{device}' is duplicated in group '{name}'.")
            if device in device_owners:
                raise DeviceGroupError(
                    f"Device serial '{device}' belongs to both '{device_owners[device]}' and '{name}'. "
                    "Device groups must be disjoint so workflows cannot use the same phone concurrently."
                )
            devices.append(device)
            device_owners[device] = name
        groups[name] = devices

    return groups


def resolve_group(raw_json: str, group_name: str, online_devices: list[str]) -> tuple[list[str], list[str]]:
    groups = parse_groups(raw_json)
    requested_group = group_name.strip()
    if not requested_group:
        raise DeviceGroupError("DEVICE_GROUP is empty.")
    if requested_group not in groups:
        available_groups = ", ".join(sorted(groups))
        raise DeviceGroupError(
            f"Unknown device group '{requested_group}'. Configured groups: {available_groups}"
        )

    online = set(online_devices)
    configured = groups[requested_group]
    selected = [device for device in configured if device in online]
    unavailable = [device for device in configured if device not in online]
    if not selected:
        raise DeviceGroupError(
            f"Device group '{requested_group}' has no online devices. "
            f"Configured devices: {', '.join(configured)}"
        )
    return selected, unavailable


def main() -> None:
    raw_json = os.environ.get("DEVICE_GROUPS_JSON", "")
    group_name = os.environ.get("DEVICE_GROUP", "")
    online_devices = os.environ.get("ONLINE_DEVICE_IDS", "").split()
    try:
        selected, unavailable = resolve_group(raw_json, group_name, online_devices)
    except DeviceGroupError as error:
        raise SystemExit(f"ERROR: {error}") from error

    if unavailable:
        print(
            f"WARNING: Device group '{group_name}' has offline or unavailable devices: "
            f"{', '.join(unavailable)}",
            file=sys.stderr,
        )
    print(" ".join(selected))


if __name__ == "__main__":
    main()
