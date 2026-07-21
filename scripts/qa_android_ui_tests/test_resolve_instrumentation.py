#!/usr/bin/env python3
"""Unit tests for exact test-APK instrumentation resolution."""

from __future__ import annotations

import unittest

from scripts.qa_android_ui_tests.resolve_instrumentation import (
    InstrumentationResolutionError,
    resolve_instrumentation,
)


class ResolveInstrumentationTest(unittest.TestCase):
    def test_ignores_tagged_runner_from_another_test_app(self) -> None:
        raw_output = "\n".join(
            [
                "instrumentation:hostile.test/com.example.TaggedTestRunner (target=hostile.app)",
                "instrumentation:wire.test/com.wire.android.tests.support.suite.TaggedTestRunner (target=wire.test)",
            ]
        )

        resolved = resolve_instrumentation(raw_output, "wire.test")

        self.assertEqual(
            "wire.test/com.wire.android.tests.support.suite.TaggedTestRunner",
            resolved,
        )

    def test_falls_back_to_only_runner_owned_by_test_app(self) -> None:
        raw_output = (
            "instrumentation:wire.test/androidx.test.runner.AndroidJUnitRunner "
            "(target=wire.test)"
        )

        resolved = resolve_instrumentation(raw_output, "wire.test")

        self.assertEqual("wire.test/androidx.test.runner.AndroidJUnitRunner", resolved)

    def test_rejects_multiple_tagged_runners_for_test_app(self) -> None:
        raw_output = "\n".join(
            [
                "instrumentation:wire.test/first.TaggedTestRunner (target=wire.test)",
                "instrumentation:wire.test/second.TaggedTestRunner (target=wire.test)",
            ]
        )

        with self.assertRaisesRegex(InstrumentationResolutionError, "Multiple TaggedTestRunner"):
            resolve_instrumentation(raw_output, "wire.test")

    def test_rejects_runner_that_targets_but_does_not_belong_to_test_app(self) -> None:
        raw_output = (
            "instrumentation:hostile.test/hostile.TaggedTestRunner "
            "(target=wire.test)"
        )

        with self.assertRaisesRegex(InstrumentationResolutionError, "No instrumentation belongs"):
            resolve_instrumentation(raw_output, "wire.test")

    def test_rejects_empty_test_app_id(self) -> None:
        with self.assertRaisesRegex(InstrumentationResolutionError, "TEST_APP_ID is empty"):
            resolve_instrumentation("", " ")

    def test_rejects_missing_target(self) -> None:
        raw_output = "instrumentation:other.test/other.TaggedTestRunner (target=other.app)"

        with self.assertRaisesRegex(InstrumentationResolutionError, "No instrumentation belongs"):
            resolve_instrumentation(raw_output, "wire.test")


if __name__ == "__main__":
    unittest.main()
