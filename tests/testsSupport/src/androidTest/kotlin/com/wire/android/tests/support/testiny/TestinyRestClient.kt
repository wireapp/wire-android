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
import org.json.JSONArray
import org.json.JSONObject

/** Sends per-test results to Testiny and retries per case on 4xx bulk failures. */
class TestinyRestClient(
    private val api: TestinyApi = TestinyApi(),
) : TestinyClient {
    private val logger = WireTestLogger.getLog(javaClass.name)
    private val statusReporter = TestinyStatusReporter

    override fun addOrUpdateTestResult(config: TestinyRuntimeConfig, result: TestinyTestResult) {
        if (!config.isConfigured || !result.hasReportableTestCaseIds) {
            return
        }

        val run = getOrCreateRun(config)
        val resolvedTestCaseIds = resolveTestCaseIds(run.projectId, result.reportableTestCaseIds, config.apiKey)
        if (resolvedTestCaseIds.isEmpty()) {
            statusReporter.warning(
                "No Testiny ids could be resolved for ${result.testName} from ${result.reportableTestCaseIds}"
            )
            return
        }

        try {
            syncResult(config, run, resolvedTestCaseIds, result.status, result.comment)
            statusReporter.info(
                "Updated run ${run.testRunId} with ids=$resolvedTestCaseIds status=${result.status.name}"
            )
        } catch (error: TestinyRequestException) {
            if (!error.isClientError()) {
                throw error
            }

            logger.warning(
                "Bulk Testiny sync failed for ids $resolvedTestCaseIds. " +
                    "Retrying each testcase individually. ${error.message}"
            )
            statusReporter.warning(
                "Bulk sync failed for ids $resolvedTestCaseIds. Retrying individually. ${error.message}"
            )

            resolvedTestCaseIds.forEach { testCaseId ->
                runCatching {
                    syncResult(config, run, listOf(testCaseId), result.status, result.comment)
                }.onFailure { retryError ->
                    logger.warning(
                        "Testiny sync retry failed for testcase $testCaseId: ${retryError.message}"
                    )
                    statusReporter.warning(
                        "Retry failed for testcase $testCaseId: ${retryError.message}"
                    )
                }
            }
        }
    }

    private fun resolveTestCaseIds(projectId: Long, testCaseIds: List<String>, apiKey: String): List<String> {
        return testCaseIds.mapNotNull { rawId ->
            resolveTestCaseId(projectId, rawId, apiKey)
        }.distinct()
    }

    private fun resolveTestCaseId(projectId: Long, rawId: String, apiKey: String): String? {
        val normalizedId = rawId.trim().removePrefix("@")
        if (normalizedId.isBlank()) {
            return null
        }

        val lookupCandidates = buildLookupCandidates(normalizedId)
        val mappedIds = api.findIdsByOldIds(projectId, lookupCandidates, apiKey)
        val resolvedId = if (mappedIds.isNotEmpty()) {
            if (mappedIds.size > 1) {
                statusReporter.warning("Multiple Testiny ids $mappedIds matched lookup candidates $lookupCandidates")
            }
            mappedIds.first().also { mappedId ->
                logger.info("Resolved Testiny id $normalizedId to project id $mappedId")
                statusReporter.info("Resolved Testiny id $normalizedId to project id $mappedId")
            }
        } else {
            extractDirectTestCaseId(normalizedId)?.also { directId ->
                logger.info("Using direct Testiny id $directId for annotation $normalizedId")
                statusReporter.info("Using direct Testiny id $directId for annotation $normalizedId")
            }
        }

        if (resolvedId == null) {
            statusReporter.warning("Could not resolve Testiny id for annotation $normalizedId")
        }
        return resolvedId
    }

    private fun buildLookupCandidates(testCaseId: String): List<String> {
        val candidates = linkedSetOf<String>()
        val normalizedId = testCaseId.trim()
        val directId = extractDirectTestCaseId(normalizedId)

        candidates += normalizedId
        directId?.let {
            candidates += it
            candidates += "TC-$it"
            candidates += "C$it"
        }

        return candidates.filter(String::isNotBlank)
    }

    private fun extractDirectTestCaseId(testCaseId: String): String? {
        return when {
            testCaseId.matches(Regex("^TC-[0-9]+$", RegexOption.IGNORE_CASE)) ->
                testCaseId.replaceFirst(Regex("^TC-", RegexOption.IGNORE_CASE), "")
            testCaseId.matches(Regex("^[0-9]+$")) -> testCaseId
            else -> null
        }
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

                config.sourceRunUrl
                    ?.takeIf(String::isNotBlank)
                    ?.let { sourceRunUrl ->
                        updateRunDescriptionIfNeeded(
                            testRunId = testRunId,
                            runName = config.runName,
                            sourceRunUrl = sourceRunUrl,
                            apiKey = config.apiKey,
                        )
                    }

                statusReporter.info(
                    "Using Testiny projectId=$projectId testRunId=$testRunId run='${config.runName}'"
                )

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

    private fun updateRunDescriptionIfNeeded(
        testRunId: Long,
        runName: String,
        sourceRunUrl: String,
        apiKey: String,
    ) {
        val currentDescription = api.getTestRunDescription(testRunId, apiKey)
        if (currentDescription.contains(sourceRunUrl)) {
            statusReporter.info("Testiny run $testRunId already contains source run url")
            return
        }

        val newDescription = buildUpdatedDescription(currentDescription, runName, sourceRunUrl)
        api.updateTestRunDescription(testRunId, newDescription, apiKey)
        statusReporter.info("Updated Testiny run $testRunId description")
    }

    private fun buildUpdatedDescription(currentDescription: String, runName: String, sourceRunUrl: String): String {
        val entries = mutableListOf<JSONObject>()
        if (currentDescription.isNotBlank()) {
            val descriptionJson = JSONObject(currentDescription)
            val existingEntries = descriptionJson.optJSONArray("c") ?: JSONArray()
            repeat(existingEntries.length()) { index ->
                entries += existingEntries.getJSONObject(index)
            }
        }

        val newEntry = JSONObject().apply {
            put("t", "p")
            put(
                "children",
                JSONArray().apply {
                    put(JSONObject().put("text", "$runName - "))
                    put(
                        JSONObject().apply {
                            put("t", "a")
                            put("children", JSONArray().put(JSONObject().put("text", "GitHub Actions")))
                            put("url", sourceRunUrl)
                        }
                    )
                    put(JSONObject().put("text", ""))
                }
            )
        }

        entries.add(0, newEntry)
        return JSONObject().apply {
            put("c", JSONArray(entries))
            put("v", 1)
            put("t", "slate")
        }.toString()
    }
}
