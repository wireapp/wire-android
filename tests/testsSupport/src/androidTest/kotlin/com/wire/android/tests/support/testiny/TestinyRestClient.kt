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

import logger.WireTestLogger

/** Sends per-test results to Testiny and retries per case on 4xx bulk failures. */
class TestinyRestClient(
    private val api: TestinyApi = TestinyApi(),
) : TestinyClient {
    private val logger = WireTestLogger.getLog(javaClass.name)

    override fun addOrUpdateTestResult(config: TestinyRuntimeConfig, result: TestinyTestResult) {
        if (!config.isConfigured || !result.hasReportableTestCaseIds) {
            return
        }

        val run = getOrCreateRun(config)
        val resolvedTestCaseIds = resolveTestCaseIds(run.projectId, result.reportableTestCaseIds, config.apiKey)

        try {
            syncResult(config, run, resolvedTestCaseIds, result.status, result.comment)
        } catch (error: TestinyRequestException) {
            if (!error.isClientError()) {
                throw error
            }

            logger.warning(
                "Bulk Testiny sync failed for ids $resolvedTestCaseIds. " +
                    "Retrying each testcase individually. ${error.message}"
            )

            resolvedTestCaseIds.forEach { testCaseId ->
                runCatching {
                    syncResult(config, run, listOf(testCaseId), result.status, result.comment)
                }.onFailure { retryError ->
                    logger.warning(
                        "Testiny sync retry failed for testcase $testCaseId: ${retryError.message}"
                    )
                }
            }
        }
    }

    private fun resolveTestCaseIds(projectId: Long, testCaseIds: List<String>, apiKey: String): List<String> {
        val mappedIds = api.findIdsByOldIds(projectId, testCaseIds, apiKey)
        if (mappedIds.isEmpty()) {
            return testCaseIds
        }

        logger.info("Resolved Testiny old ids $testCaseIds to project ids $mappedIds")
        return mappedIds
    }

    private fun syncResult(
        config: TestinyRuntimeConfig,
        run: CachedRun,
        testCaseIds: List<String>,
        status: TestinyExecutionStatus,
        comment: String?,
    ) {
        api.addOrUpdateResults(
            testRunId = run.testRunId,
            testCaseIds = testCaseIds,
            status = status,
            apiKey = config.apiKey,
        )

        comment
            ?.takeIf { it.isNotBlank() }
            ?.let {
                val commentId = api.createTextComment(run.projectId, it, config.apiKey)
                api.addCommentToResults(run.testRunId, testCaseIds, commentId, config.apiKey)
            }
    }

    private fun getOrCreateRun(config: TestinyRuntimeConfig): CachedRun {
        val cacheKey = "${config.projectName}::${config.runName}"

        return synchronized(runCache) {
            runCache.getOrPut(cacheKey) {
                // Reuse the same open run for all tests in this workflow run.
                val projectId = api.findProjectId(config.projectName, config.apiKey)
                val testRunId = api.findOpenTestRunId(projectId, config.runName, config.apiKey)
                    ?: api.createTestRun(projectId, config.runName, config.apiKey)

                CachedRun(projectId = projectId, testRunId = testRunId)
            }
        }
    }

    private fun TestinyRequestException.isClientError(): Boolean = returnCode in 400..499

    private data class CachedRun(
        val projectId: Long,
        val testRunId: Long,
    )

    private companion object {
        // Cache project/run ids so every test does not repeat the lookup.
        val runCache = mutableMapOf<String, CachedRun>()
    }
}
