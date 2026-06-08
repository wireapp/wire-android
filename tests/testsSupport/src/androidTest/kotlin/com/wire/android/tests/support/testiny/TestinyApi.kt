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

import network.HttpRequestException
import network.NetworkBackendClient.sendJsonRequest
import network.NumberSequence
import network.RequestOptions
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Small REST wrapper for the Testiny endpoints this sync needs. */
class TestinyApi {
    private val baseUrl = "https://app.testiny.io/api/v1"
    private val knownProjectIds = mapOf(
        "Wire WebApp" to 3L,
        "Wire iOS" to 7L,
        "Wire Android Reloaded" to 8L,
        "Wire Desktop" to 9L,
        "Wire Account Pages" to 10L,
        "Wire Team Management" to 11L,
        "Wire Android New" to 15L,
    )

    fun findProjectId(projectName: String, apiKey: String): Long {
        knownProjectIds[projectName]?.let { return it }

        val response = postObject(
            path = "project/find",
            apiKey = apiKey,
            body = JSONObject().put("filter", JSONObject().put("title", projectName)),
        )

        val projects = response.optJSONArray("data") ?: JSONArray()
        if (projects.length() == 0) {
            throw IllegalArgumentException("Testiny project '$projectName' cannot be found")
        }

        return projects.getJSONObject(0).getLong("id")
    }

    fun findOpenTestRunId(projectId: Long, runName: String, apiKey: String): Long? {
        val response = postObject(
            path = "testrun/find",
            apiKey = apiKey,
            body = JSONObject().put(
                "filter",
                JSONObject().apply {
                    put("project_id", projectId)
                    put("title", runName)
                    put("is_closed", false)
                }
            ),
        )

        val runs = response.optJSONArray("data") ?: JSONArray()
        if (runs.length() == 0) {
            return null
        }

        return runs.getJSONObject(0).getLong("id")
    }

    fun getTestRunDescription(testRunId: Long, apiKey: String): String {
        val response = getObject(
            path = "testrun/$testRunId",
            apiKey = apiKey,
        )

        return response.optString("description", "")
    }

    fun findIdsByOldIds(projectId: Long, oldIds: List<String>, apiKey: String): List<String> {
        if (oldIds.isEmpty()) {
            return emptyList()
        }

        val response = postObject(
            path = "testcase/find",
            apiKey = apiKey,
            body = JSONObject().apply {
                put(
                    "filter",
                    JSONObject().apply {
                        put("cf__old_id", JSONArray().apply { oldIds.forEach(::put) })
                        put("project_id", projectId)
                    }
                )
                put("idOnly", true)
            },
        )

        val cases = response.optJSONArray("data") ?: JSONArray()
        return buildList(cases.length()) {
            repeat(cases.length()) { index ->
                add(cases.getJSONObject(index).get("id").toString())
            }
        }
    }

    fun createTestRun(projectId: Long, runName: String, apiKey: String): Long {
        val response = postObject(
            path = "testrun",
            apiKey = apiKey,
            body = JSONObject().apply {
                // Keep the create-run payload aligned with Android Reloaded.
                put("id", 1)
                put("title", runName)
                put("is_deleted", false)
                put("project_id", projectId)
                put("testplan_id", 0)
                put("is_closed", false)
                put("description", "")
            },
        )

        return response.getLong("id")
    }

    fun updateTestRunDescription(testRunId: Long, description: String, apiKey: String) {
        putObject(
            path = "testrun/$testRunId?force=true",
            apiKey = apiKey,
            body = JSONObject().put("description", description),
        )
    }

    fun addOrUpdateResults(
        testRunId: Long,
        testCaseIds: List<String>,
        status: TestinyExecutionStatus,
        apiKey: String,
    ) {
        val body = JSONArray().apply {
            testCaseIds.map(String::toLong).forEach { testCaseId ->
                put(
                    JSONObject().apply {
                        put(
                            "ids",
                            JSONObject().apply {
                                put("testcase_id", testCaseId)
                                put("testrun_id", testRunId)
                            }
                        )
                        put(
                            "mapped",
                            JSONObject().apply {
                                put("result_status", status.apiValue)
                                put("assigned_to", "OWNER")
                            }
                        )
                    }
                )
            }
        }

        postArray(
            path = "testrun/mapping/bulk/testcase:testrun?op=add_or_update",
            apiKey = apiKey,
            body = body,
        )
    }

    fun createTextComment(projectId: Long, comment: String, apiKey: String): Long {
        val response = postObject(
            path = "comment",
            apiKey = apiKey,
            body = JSONObject().apply {
                put("project_id", projectId)
                put("type", "TEXT")
                put("text", comment)
            },
        )

        return response.getLong("id")
    }

    fun addCommentToResults(
        testRunId: Long,
        testCaseIds: List<String>,
        commentId: Long,
        apiKey: String,
    ) {
        val body = JSONArray().apply {
            testCaseIds.map(String::toLong).forEach { testCaseId ->
                put(
                    JSONObject().apply {
                        put(
                            "ids",
                            JSONObject().apply {
                                put("comment_id", commentId)
                                put("testcase_id", testCaseId)
                                put("testrun_id", testRunId)
                            }
                        )
                    }
                )
            }
        }

        postArray(
            path = "comment/mapping/bulk/testcase:testrun?op=add",
            apiKey = apiKey,
            body = body,
        )
    }

    private fun postObject(path: String, apiKey: String, body: JSONObject): JSONObject {
        return JSONObject(postRaw(path, apiKey, body.toString()))
    }

    private fun getObject(path: String, apiKey: String): JSONObject {
        return JSONObject(sendRaw(path, "GET", apiKey))
    }

    private fun postArray(path: String, apiKey: String, body: JSONArray): JSONArray {
        return JSONArray(postRaw(path, apiKey, body.toString()))
    }

    private fun putObject(path: String, apiKey: String, body: JSONObject) {
        sendRaw(path, "PUT", apiKey, body.toString())
    }

    private fun postRaw(path: String, apiKey: String, body: String): String {
        return sendRaw(path, "POST", apiKey, body)
    }

    private fun sendRaw(path: String, method: String, apiKey: String, body: String? = null): String {
        return try {
            sendJsonRequest(
                url = URL("$baseUrl/$path"),
                method = method,
                body = body,
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "Accept" to "application/json",
                    "X-Api-Key" to apiKey,
                ),
                options = RequestOptions(
                    expectedResponseCodes = NumberSequence.Array(
                        intArrayOf(HttpURLConnection.HTTP_OK)
                    )
                ),
            )
        } catch (error: HttpRequestException) {
            throw TestinyRequestException(error.message, error.returnCode, error)
        }
    }
}
