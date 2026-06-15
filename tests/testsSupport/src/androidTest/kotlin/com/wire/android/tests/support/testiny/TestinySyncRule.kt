/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.tests.support.testiny

import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import logger.WireTestLogger
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.PrintWriter
import java.io.StringWriter

/**
 * JUnit rule added by BaseUiTest.
 * After each test, it reads the method annotations and sends the final JUnit outcome to Testiny
 * when the CI run provided Testiny config.
 */
class TestinySyncRule(
    private val client: TestinyRestClient = TestinyRestClient(),
) : TestRule {
    private val logger = WireTestLogger.getLog(javaClass.name)
    private val statusReporter = TestinyStatusReporter

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val testCaseIds = description.annotations
                    .filterIsInstance<TestCaseId>()
                    .flatMap { it.value.asList() }
                    .distinct()
                val categories = description.annotations
                    .filterIsInstance<Category>()
                    .flatMap { it.value.asList() }
                    .distinct()
                // No Testiny args means this is a local or non-reporting run.
                val config = TestinyRuntimeConfigResolver.fromInstrumentationArgs()

                var testStatus = TestinyExecutionStatus.Passed
                var testFailure: Throwable? = null

                try {
                    base.evaluate()
                } catch (error: AssumptionViolatedException) {
                    testStatus = TestinyExecutionStatus.Skipped
                    testFailure = error
                    throw error
                } catch (error: Throwable) {
                    testStatus = TestinyExecutionStatus.Failed
                    testFailure = error
                    throw error
                } finally {
                    // JUnit outcome is known here, so this is the single publish path for pass/fail/skip.
                    publishIfConfigured(
                        reportContext = TestinyReportContext(
                            description = description,
                            testCaseIds = testCaseIds,
                            categories = categories,
                            config = config,
                        ),
                        status = testStatus,
                        failure = testFailure,
                    )
                }
            }
        }
    }

    private fun publishIfConfigured(
        reportContext: TestinyReportContext,
        status: TestinyExecutionStatus,
        failure: Throwable?,
    ) {
        val description = reportContext.description
        val testCaseIds = reportContext.testCaseIds
        val categories = reportContext.categories
        val config = reportContext.config
        val skipReason = when {
            config == null -> return
            !config.isConfigured -> {
                // Log presence flags only; never log the actual run name or API key.
                "incomplete config " +
                    "(project=${config.projectName.isNotBlank()}, " +
                    "run=${config.runName.isNotBlank()}, " +
                    "apiKey=${config.apiKey.isNotBlank()})"
            }
            testCaseIds.isEmpty() -> "no @TestCaseId"
            else -> null
        }

        if (skipReason != null) {
            if (skipReason == "no @TestCaseId") {
                statusReporter.info("Skipping ${description.displayName}: $skipReason")
            } else {
                statusReporter.warning("Skipping ${description.displayName}: $skipReason")
            }
            return
        }

        val result = TestinyTestResult(
            testName = description.displayName,
            testCaseIds = testCaseIds,
            categories = categories,
            status = status,
            comment = failure?.asComment(),
        )

        statusReporter.info(
            "Preparing ${description.displayName} ids=${result.reportableTestCaseIds} status=${result.status.name}"
        )

        runCatching {
            client.addOrUpdateTestResult(config, result)
            statusReporter.info("Synced ${description.displayName}")
        }.onFailure { syncError ->
            // Do not rethrow sync errors; the UI test result is already decided.
            logger.warning("Testiny sync failed for ${description.displayName}: ${syncError.message}")
            statusReporter.error("Sync failed for ${description.displayName}: ${syncError.message}", syncError)
        }
    }

    private fun Throwable.asComment(): String {
        // Put the full stack trace on the Testiny result so the run has useful failure context.
        return StringWriter().use { buffer ->
            PrintWriter(buffer).use { writer ->
                printStackTrace(writer)
            }
            buffer.toString()
        }
    }

    private data class TestinyReportContext(
        val description: Description,
        val testCaseIds: List<String>,
        val categories: List<String>,
        val config: TestinyRuntimeConfig?,
    )
}
