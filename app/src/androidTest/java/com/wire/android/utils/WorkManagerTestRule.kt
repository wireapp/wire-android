package com.wire.android.utils

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Test rule to initialize in an instrumented test the workmanager used in the application
 * Adding this as a test rule ensure order, so we can achieve launching first the work manager and later the test scenario
 */
class WorkManagerTestRule : TestRule {
    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                WorkManagerTestInitHelper.initializeTestWorkManager(
                    context,
                    Configuration.Builder()
                        .setMinimumLoggingLevel(Log.DEBUG)
                        .setExecutor(SynchronousExecutor())
                        .build()
                )
                base?.evaluate()
            }
        }
    }
}
