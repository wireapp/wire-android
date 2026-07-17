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

/**
 * Turns one finished UI test into Testiny updates.
 * This resolves `@TestCaseId` values, reuses or creates the run, writes the status,
 * and adds the failure stack trace when the test failed.
 */
class TestinyRestClient(
    private val api: TestinyApi = TestinyApi(),
) {
    private val logger = WireTestLogger.getLog(javaClass.name)
    private val statusReporter = TestinyStatusReporter

    fun addOrUpdateTestResult(config: TestinyRuntimeConfig, result: TestinyTestResult) {
        // Nothing to report for local runs or tests without Testiny ids.
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

        val synced = try {
            syncResult(config, run, resolvedTestCaseIds, result.status, result.comment)
            true
        } catch (error: TestinyRequestException) {
            retrySyncIndividually(config, run, resolvedTestCaseIds, result, error)
        }

        if (synced) {
            statusReporter.info(
                "Updated run ${run.testRunId} with ids=$resolvedTestCaseIds status=${result.status.name}"
            )
        } else {
            statusReporter.warning(
                "No Testiny result could be updated for ids=$resolvedTestCaseIds status=${result.status.name}"
            )
        }
    }

    private fun retrySyncIndividually(
        config: TestinyRuntimeConfig,
        run: CachedRun,
        resolvedTestCaseIds: List<String>,
        result: TestinyTestResult,
        error: TestinyRequestException,
    ): Boolean {
        if (!error.isClientError()) {
            throw error
        }

        // If one bad id breaks the bulk call, retry each id so the valid cases still update.
        logger.warning(
            "Bulk Testiny sync failed for ids $resolvedTestCaseIds. " +
                "Retrying each testcase individually. ${error.message}"
        )
        statusReporter.warning(
            "Bulk sync failed for ids $resolvedTestCaseIds. Retrying individually. ${error.message}"
        )

        var synced = false
        resolvedTestCaseIds.forEach { testCaseId ->
            runCatching {
                syncResult(config, run, listOf(testCaseId), result.status, result.comment)
            }.onSuccess {
                synced = true
            }.onFailure { retryError ->
                logger.warning(
                    "Testiny sync retry failed for testcase $testCaseId: ${retryError.message}"
                )
                statusReporter.warning(
                    "Retry failed for testcase $testCaseId: ${retryError.message}"
                )
            }
        }
        return synced
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

        // Prefer Testiny's old-id mapping, then fall back to direct ids for migrated annotations.
        val lookupCandidates = buildLookupCandidates(normalizedId)
        val mappedIds = api.findIdsByOldIds(projectId, lookupCandidates, apiKey)
        val resolvedId = if (mappedIds.isNotEmpty()) {
            if (mappedIds.size > 1) {
                statusReporter.warning("Multiple Testiny ids $mappedIds matched lookup candidates $lookupCandidates")
            }
            mappedIds.first().also { mappedId ->
                statusReporter.info("Resolved Testiny id $normalizedId to project id $mappedId")
            }
        } else {
            extractDirectTestCaseId(normalizedId)?.also { directId ->
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

        // Testiny data has used both "TC-123" and "123", so query both forms.
        candidates += normalizedId
        directId?.let {
            candidates += it
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

        // Add the stack trace only after the result row exists in Testiny.
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
                // One CI run name should map to one open Testiny run.
                val projectId = api.findProjectId(config.projectName)
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
        // Avoid the same project/run lookup for every test method in the process.
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
            // Description is Testiny rich-text JSON. Keep existing blocks and add this CI run at the top.
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
