#!/usr/bin/env python3
"""Unit tests for exact-target instrumentation resolution."""

from __future__ import annotations

import unittest

from scripts.qa_android_ui_tests.resolve_instrumentation import (
    InstrumentationResolutionError,
    resolve_instrumentation,
)


class ResolveInstrumentationTest(unittest.TestCase):
    def test_ignores_tagged_runner_for_another_app(self) -> None:
        raw_output = "\n".join(
            [
                "instrumentation:hostile.test/com.example.TaggedTestRunner (target=hostile.app)",
                "instrumentation:wire.test/com.wire.android.tests.support.suite.TaggedTestRunner (target=com.wire)",
            ]
        )

        resolved = resolve_instrumentation(raw_output, "com.wire")

        self.assertEqual(
            "wire.test/com.wire.android.tests.support.suite.TaggedTestRunner",
            resolved,
        )

    def test_falls_back_to_only_exact_target_runner(self) -> None:
        raw_output = "instrumentation:wire.test/androidx.test.runner.AndroidJUnitRunner (target=com.wire)"

        resolved = resolve_instrumentation(raw_output, "com.wire")

        self.assertEqual("wire.test/androidx.test.runner.AndroidJUnitRunner", resolved)

    def test_rejects_multiple_tagged_runners_for_target(self) -> None:
        raw_output = "\n".join(
            [
                "instrumentation:first.test/first.TaggedTestRunner (target=com.wire)",
                "instrumentation:second.test/second.TaggedTestRunner (target=com.wire)",
            ]
        )

        with self.assertRaisesRegex(InstrumentationResolutionError, "Multiple TaggedTestRunner"):
            resolve_instrumentation(raw_output, "com.wire")

    def test_rejects_missing_target(self) -> None:
        raw_output = "instrumentation:other.test/other.TaggedTestRunner (target=other.app)"

        with self.assertRaisesRegex(InstrumentationResolutionError, "No instrumentation targets"):
            resolve_instrumentation(raw_output, "com.wire")


if __name__ == "__main__":
    unittest.main()
