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

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import io.qameta.allure.android.allureScreenshot
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit rule that takes an Allure screenshot **only when a test really fails**.
 *
 * - If the test is skipped via AssumptionViolatedException (our filter rules),
 *   this rule does nothing.
 * - For other Throwables (real failures), it captures a screenshot and
 *   attaches it to Allure, then rethrows the error.
 */
class AllureFailureScreenshotRule(
    private val screenshotName: String = "failure-screenshot"
) : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    // Run the actual test + other rules
                    base.evaluate()
                } catch (e: AssumptionViolatedException) {
                    // Test was skipped by a filter rule -> NO screenshot
                    throw e
                } catch (t: Throwable) {
                    // Real failure -> take screenshot for Allure
                    try {
                        Log.i(
                            "AllureFailureScreenshotRule",
                            "Taking failure screenshot for ${description.displayName}"
                        )

                        // This helper both captures the device screen
                        // and attaches it directly to the current Allure test.
                        allureScreenshot(
                            name = screenshotName,
                            quality = 80,
                            scale = 1.0f
                        )
                    } catch (s: Throwable) {
                        // Never break the test because screenshot failed
                        Log.e(
                            "AllureFailureScreenshotRule",
                            "Failed to capture screenshot", s
                        )
                    }

                    // Re-throw the original failure so JUnit/Allure can mark it as failed
                    throw t
                }
            }
        }
    }
}
