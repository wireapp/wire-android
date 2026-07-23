#!/usr/bin/env python3
"""Unit tests for Android device-group resolution."""

from __future__ import annotations

import json
import unittest

from scripts.qa_android_ui_tests.resolve_device_group import DeviceGroupError, parse_groups, resolve_group


class ResolveDeviceGroupTest(unittest.TestCase):
    def test_resolves_online_devices_in_configured_order(self) -> None:
        groups = json.dumps(
            {
                "criticalFlow": ["critical-2", "critical-1"],
                "e2eTests": ["e2e-1"],
            }
        )

        selected, unavailable = resolve_group(
            groups,
            "criticalFlow",
            ["unassigned", "critical-1", "critical-2"],
        )

        self.assertEqual(["critical-2", "critical-1"], selected)
        self.assertEqual([], unavailable)

    def test_keeps_running_when_part_of_group_is_offline(self) -> None:
        groups = json.dumps({"criticalFlow": ["online", "offline"]})

        selected, unavailable = resolve_group(groups, "criticalFlow", ["online"])

        self.assertEqual(["online"], selected)
        self.assertEqual(["offline"], unavailable)

    def test_fails_when_group_has_no_online_devices(self) -> None:
        groups = json.dumps({"criticalFlow": ["offline"]})

        with self.assertRaisesRegex(DeviceGroupError, "has no online devices"):
            resolve_group(groups, "criticalFlow", ["another-device"])

    def test_fails_for_unknown_group(self) -> None:
        groups = json.dumps({"criticalFlow": ["device-1"]})

        with self.assertRaisesRegex(DeviceGroupError, "Unknown device group 'e2eTests'"):
            resolve_group(groups, "e2eTests", ["device-1"])

    def test_rejects_padded_requested_group_that_would_use_a_different_lock(self) -> None:
        groups = json.dumps({"criticalFlow": ["device-1"]})

        with self.assertRaisesRegex(DeviceGroupError, "must use only"):
            resolve_group(groups, " criticalFlow ", ["device-1"])

    def test_rejects_unsafe_configured_group_name(self) -> None:
        groups = json.dumps({"critical Flow": ["device-1"]})

        with self.assertRaisesRegex(DeviceGroupError, "must use only"):
            parse_groups(groups)

    def test_rejects_device_assigned_to_multiple_groups(self) -> None:
        groups = json.dumps(
            {
                "criticalFlow": ["shared-device"],
                "e2eTests": ["shared-device"],
            }
        )

        with self.assertRaisesRegex(DeviceGroupError, "must be disjoint"):
            parse_groups(groups)


if __name__ == "__main__":
    unittest.main()
