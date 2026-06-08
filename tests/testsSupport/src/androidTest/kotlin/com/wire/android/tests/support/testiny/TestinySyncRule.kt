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

import com.wire.android.tests.support.suite.TestMetadataExtractor
import logger.WireTestLogger
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.PrintWriter
import java.io.StringWriter

/** JUnit rule that reports each finished test to Testiny. */
class TestinySyncRule(
    private val client: TestinyClient = TestinyRestClient(),
) : TestRule {
    private val logger = WireTestLogger.getLog(javaClass.name)

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val metadata = TestMetadataExtractor.from(description)
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
                    // Report after the test finishes so the final outcome is stable.
                    publishIfConfigured(
                        description = description,
                        metadata = metadata,
                        config = config,
                        status = testStatus,
                        failure = testFailure,
                    )
                }
            }
        }
    }

    private fun publishIfConfigured(
        description: Description,
        metadata: com.wire.android.tests.support.suite.TestMetadata,
        config: TestinyRuntimeConfig?,
        status: TestinyExecutionStatus,
        failure: Throwable?,
    ) {
        if (config == null || !config.isConfigured || metadata.testCaseIds.isEmpty()) {
            return
        }

        val result = TestinyTestResult(
            testName = description.displayName,
            metadata = metadata,
            status = status,
            comment = failure?.asComment(),
        )

        runCatching {
            client.addOrUpdateTestResult(config, result)
        }.onFailure { syncError ->
            // Reporting must never change the test result.
            logger.warning("Testiny sync failed for ${description.displayName}: ${syncError.message}")
        }
    }

    private fun Throwable.asComment(): String {
        return StringWriter().use { buffer ->
            PrintWriter(buffer).use { writer ->
                printStackTrace(writer)
            }
            buffer.toString()
        }
    }
}
