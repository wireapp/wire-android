#!/usr/bin/env python3
"""URL validation and deflake artifact round-trip regression tests."""

import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parent))
from resolve_test_service_url import resolve_url, validate_url

SCRIPTS = Path(__file__).resolve().parent


class TestServiceUrlTest(unittest.TestCase):
    def test_manual_override_and_scheduled_default(self):
        self.assertEqual(resolve_url("https://version.example/", "http://default"), "https://version.example")
        self.assertEqual(resolve_url("", "http://default:8080/"), "http://default:8080")
        self.assertEqual(resolve_url("   ", "http://default"), "http://default")

    def test_accepts_device_reachable_origins(self):
        for url in ("http://10.0.2.2:8080", "http://test-service", "https://qa.example", "http://[::1]:8080"):
            with self.subTest(url=url):
                self.assertEqual(validate_url(url), url)

    def test_rejects_invalid_and_injected_urls(self):
        for url in (
            "", "ftp://qa.example", "https://user:password@qa.example", "https://qa.example/path",
            "https://qa.example?token=value", "https://qa.example#fragment", "http://qa.example:0",
            "http://qa.example:65536", "http://qa.example:", "http://[invalid]", "http://-bad.example",
            "http://qa.example\nINJECTED=value", "http://qa.example\r", "http://qa.example\t",
            "http://$(touch /tmp/injected)", "http://`whoami`", "http://qa.example\\evil", None, 42,
        ):
            with self.subTest(url=url), self.assertRaises(ValueError):
                validate_url(url)

    def test_rejects_control_only_override_instead_of_using_default(self):
        with self.assertRaises(ValueError):
            resolve_url("\n", "http://default")

    def test_requires_explicit_url_for_legacy_deflake(self):
        with self.assertRaisesRegex(ValueError, "legacy artifact"):
            resolve_url("", "", deflake=True)

    def test_deflake_artifact_round_trip_and_override(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            failed = root / "failed.txt"
            failed.write_text("tests.Status#busy\n")
            env = {**os.environ,
                "DEFLAKE_BUNDLE_DIR": str(root / "bundle"),
                "SOURCE_WORKFLOW_NAME": "QA Android Critical Flow Tests",
                "SOURCE_WORKFLOW_FILE": ".github/workflows/qa-android-critical-flow-tests.yml",
                "SOURCE_REPOSITORY": "wireapp/wire-android",
                "SOURCE_RUN_ID": "123", "SOURCE_REF_NAME": "develop", "SOURCE_SHA": "a" * 40,
                "FLAVOR_INPUT": "experimental", "TEST_SERVICE_URL": "https://version.example",
                "FINAL_FAILED_TESTS_FILE": str(failed), "GITHUB_STEP_SUMMARY": str(root / "summary"),
                "GITHUB_OUTPUT": str(root / "output"), "TEST_SERVICE_URL_OVERRIDE": "",
                "DEFLAKE_BUNDLE_ROOT": str(root / "bundle"),
            }
            for field in ("SOURCE_REPOSITORY", "SOURCE_RUN_ID", "SOURCE_SHA", "SOURCE_WORKFLOW_FILE", "SOURCE_REF_NAME"):
                env[f"EXPECTED_{field}"] = env[field]

            def run(script):
                return subprocess.run([sys.executable, str(SCRIPTS / script)], env=env, capture_output=True, text=True)

            result = run("prepare_deflake_bundle.py")
            self.assertEqual(result.returncode, 0, result.stderr)
            metadata_path = root / "bundle/metadata.json"
            metadata = json.loads(metadata_path.read_text())
            self.assertEqual(metadata["test_service_url"], "https://version.example")
            # Changing the current default must not redirect a deflake run.
            env["TEST_SERVICE_URL_DEFAULT"] = "http://different-default"
            result = run("inspect_deflake_bundle.py")
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn("\nhttps://version.example\n", (root / "output").read_text())
            env["TEST_SERVICE_URL_OVERRIDE"] = "https://explicit.example/"
            result = run("inspect_deflake_bundle.py")
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn("\nhttps://explicit.example\n", (root / "output").read_text())
            # Subsequent deflake artifacts record the effective override.
            env["TEST_SERVICE_URL"] = "https://explicit.example"
            self.assertEqual(run("prepare_deflake_bundle.py").returncode, 0)
            self.assertEqual(json.loads(metadata_path.read_text())["test_service_url"], "https://explicit.example")
            del metadata["test_service_url"]
            metadata_path.write_text(json.dumps(metadata))
            self.assertEqual(run("inspect_deflake_bundle.py").returncode, 0)
            env["TEST_SERVICE_URL_OVERRIDE"] = ""
            result = run("inspect_deflake_bundle.py")
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("legacy artifact", result.stderr)
            metadata["test_service_url"] = "https://qa.example\nINJECTED=value"
            metadata_path.write_text(json.dumps(metadata))
            self.assertNotEqual(run("inspect_deflake_bundle.py").returncode, 0)


if __name__ == "__main__":
    unittest.main()
