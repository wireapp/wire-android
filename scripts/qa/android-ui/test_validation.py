#!/usr/bin/env python3
"""Check normal and deflake mode selection before execution."""

from __future__ import annotations

import os
import subprocess
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("validation.sh")


class ValidationTest(unittest.TestCase):
    def validate(self, **inputs: str) -> tuple[subprocess.CompletedProcess[str], str]:
        with tempfile.TemporaryDirectory() as directory:
            output_path = Path(directory) / "outputs"
            result = subprocess.run(
                ["bash", str(SCRIPT), "validate-run-mode"],
                env={**os.environ, "GITHUB_OUTPUT": str(output_path), **inputs},
                capture_output=True,
                text=True,
                check=False,
            )
            outputs = output_path.read_text() if output_path.exists() else ""
        return result, outputs

    def test_normal_and_scheduled_defaults(self) -> None:
        for mode in ("Normal tests", ""):
            with self.subTest(mode=mode):
                result, outputs = self.validate(RUN_MODE=mode, SOURCE_RUN_ID="")
                self.assertEqual(0, result.returncode, result.stdout)
                self.assertEqual("runMode=normal\n", outputs)

    def test_normal_rejects_source_id(self) -> None:
        result, outputs = self.validate(
            RUN_MODE="Normal tests", SOURCE_RUN_ID="12345"
        )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("only valid", result.stdout)
        self.assertEqual("", outputs)

    def test_deflake_accepts_source_id_with_surrounding_whitespace(self) -> None:
        result, outputs = self.validate(
            RUN_MODE="Deflake failed tests", SOURCE_RUN_ID=" 12345 "
        )
        self.assertEqual(0, result.returncode, result.stdout)
        self.assertEqual("runMode=deflake\n", outputs)

    def test_deflake_rejects_missing_or_invalid_source_id(self) -> None:
        for source_id in ("", "abc", "123\n456"):
            with self.subTest(source_id=source_id):
                result, outputs = self.validate(
                    RUN_MODE="Deflake failed tests", SOURCE_RUN_ID=source_id
                )
                self.assertNotEqual(0, result.returncode)
                self.assertIn("numeric sourceRunId", result.stdout)
                self.assertEqual("", outputs)

    def test_latest_source_is_opt_in_and_still_rejects_invalid_ids(self) -> None:
        for source_id in ("", "  ", "12345", "abc", "123\n456"):
            with self.subTest(source_id=source_id):
                result, outputs = self.validate(
                    RUN_MODE="Deflake failed tests", SOURCE_RUN_ID=source_id,
                    ALLOW_LATEST_SOURCE_RUN="true",
                )
                accepted = source_id.strip() in ("", "12345")
                self.assertEqual(accepted, result.returncode == 0)
                self.assertEqual("runMode=deflake\n" if accepted else "", outputs)

    def test_emulator_inputs_reject_upgrade_and_other_device_groups(self) -> None:
        for group, upgrade, category, accepted in (
            ("criticalFlow", "false", "criticalFlow", True),
            ("criticalFlow", "false", "", True),
            ("criticalFlow", "true", "criticalFlow", False),
            ("criticalFlow", "false", "upgrade", False),
            ("e2eTests", "false", "criticalFlow", False),
        ):
            with self.subTest(group=group, upgrade=upgrade, category=category):
                result = subprocess.run(
                    ["bash", str(SCRIPT), "validate-emulator-inputs"],
                    env={**os.environ, "IS_UPGRADE": upgrade,
                         "RESOLVED_CATEGORY": category, "TARGET_DEVICE_GROUP": group},
                    capture_output=True, text=True, check=False,
                )
                self.assertEqual(accepted, result.returncode == 0, result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
