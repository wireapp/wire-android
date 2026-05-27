/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.tests.support.suite

import android.os.Bundle
import android.util.Log
import io.qameta.allure.android.runners.AllureAndroidJUnitRunner

/**
 * Custom test runner that delegates to Allure's Android runner,
 * and filters tests by @TestCaseId, @Category, and @Tag BEFORE
 * they are executed (and before Allure sees them).
 *
 * Retry attempts use the same runner entry point, but pass an explicit rerun
 * list through instrumentation args so only previously failed tests execute.
 */
class TaggedTestRunner : AllureAndroidJUnitRunner() {

    override fun onCreate(arguments: Bundle) {
        val filterId = arguments.getString("testCaseId")
        val category = arguments.getString("category")
        val excludeCategory = arguments.getString("excludeCategory")
        val requiredCategory = arguments.getString("requiredCategory")
        val tagKey = arguments.getString("tagKey")
        val tagValue = arguments.getString("tagValue")
        val rerunMode = arguments.getString(RetryContract.ARG_ENABLE_RERUN_MODE)
        val rerunAttempt = arguments.getString(RetryContract.ARG_RERUN_ATTEMPT)
        val rerunListPath = arguments.getString(RetryContract.ARG_RERUN_LIST_PATH)
        val rerunListInline = arguments.getString(RetryContract.ARG_RERUN_LIST_INLINE)
        val rerunListInlinePartCount = arguments.keySet()
            .count { key -> key.startsWith(RetryContract.ARG_RERUN_LIST_INLINE_PART_PREFIX) }

        // Log the retry contract once so CI failures can confirm the runner saw
        // the expected inputs without dumping every rerun test ID into logcat.
        Log.i(
            "TaggedTestRunner",
            "onCreate called. " +
                    "testCaseId=$filterId, category=$category, excludeCategory=$excludeCategory, " +
                    "requiredCategory=$requiredCategory, " +
                    "tagKey=$tagKey, tagValue=$tagValue, " +
                    "rerunMode=$rerunMode, rerunAttempt=$rerunAttempt, " +
                    "rerunListPath=$rerunListPath, rerunListInlineLength=${rerunListInline?.length ?: 0}, " +
                    "rerunListInlinePartCount=$rerunListInlinePartCount"
        )

        super.onCreate(arguments)
    }

    override fun onStart() {
        // Before running any tests, clear previous Allure results on the device.
        clearAllureResultsOnDevice()

        // Then let Allure/AndroidJUnitRunner do its normal startup.
        super.onStart()
    }

    private fun clearAllureResultsOnDevice() {
        try {
            // This is where Allure stores results on the device in our setup.
            // If the directory doesn't exist yet, rm -rf is still safe.
            val cmd = "rm -rf /sdcard/googletest/test_outputfiles/allure-results"

            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val exitCode = process.waitFor()

            Log.i(
                "TaggedTestRunner",
                "Cleared Allure results dir on device, exitCode=$exitCode"
            )
        } catch (t: Throwable) {
            // Never fail the test run just because cleanup failed.
            Log.w(
                "TaggedTestRunner",
                "Failed to clear Allure results directory before tests",
                t
            )
        }
    }
}
