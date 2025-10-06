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

@file:Suppress("TooGenericExceptionCaught", "VariableNaming", "MagicNumber", "PackageNaming", "TooGenericExceptionThrown")

package okta

import android.content.Context
import com.wire.android.testSupport.BuildConfig
import com.wire.android.testSupport.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.math.max

class OktaApiClient {

    private val CONNECT_TIMEOUT_MS = 3_000
    private val READ_TIMEOUT_MS = 240_000
    private val BASE_URI = "https://dev-500508-admin.oktapreview.com"
    private val apiKey: String by lazy { BuildConfig.OKTA_API_KEY_PASSWORD }

    private var applicationId: String? = null
    private val userIds = LinkedHashSet<String>()

    companion object {
        @JvmStatic
        fun getFinalizeUrlDependingOnBackend(backendUrl: String): String {
            val trimmed = backendUrl.removeSuffix("/")
            return "$trimmed/sso/finalize-login"
        }
    }

    // ───────────────────── Retry wrapper ─────────────────────
    private class RestHandlers(
        private val verify: (code: Int, acceptable: IntArray, message: String) -> Unit,
        private val retries: Int = 3
    ) {
        fun run(
            block: () -> Pair<Int, String>,
            acceptable: IntArray
        ): String {
            var err: Throwable? = null
            repeat(max(1, retries)) {
                try {
                    val (code, body) = block()
                    verify(code, acceptable, body)
                    return body
                } catch (t: Throwable) {
                    err = t
                }
            }
            throw err ?: IllegalStateException("Request failed after $retries attempts")
        }
    }

    private val restHandlers = RestHandlers(::verifyRequestResult, retries = 3)

    private fun verifyRequestResult(currentCode: Int, acceptable: IntArray, message: String) {
        if (acceptable.any { it == currentCode }) return
        throw Exception(
            "Request to Okta API failed. " +
                    "Request return code is: $currentCode. " +
                    "Expected codes are: ${acceptable.contentToString()}. " +
                    "Message from service is: $message"
        )
    }

    @Suppress("NestedBlockDepth")
    private fun httpRequest(
        path: String,
        method: String,
        acceptableCodes: IntArray,
        accept: String = "application/json",
        body: String? = null
    ): String = restHandlers.run(
        block = {
            val url = URL("$BASE_URI/$path")
            val conn = (url.openConnection() as HttpsURLConnection).apply {
                requestMethod = method
                setRequestProperty("Authorization", "SSWS $apiKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", accept)
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                if (method == "POST" || method == "PUT") {
                    doOutput = true
                    body?.let { outputStream.bufferedWriter().use { w -> w.write(it) } }
                }
            }
            val code = conn.responseCode
            val text = (if (code in acceptableCodes) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            conn.disconnect()
            code to text
        },
        acceptable = acceptableCodes
    )

    private fun readRawResource(context: Context, resId: Int): String =
        context.resources.openRawResource(resId).bufferedReader().use { it.readText() }

    private fun hardSleep(ms: Long) {
        try {
            Thread.sleep(ms)
        } catch (_: InterruptedException) {
        }
    }

    fun createApplication(label: String, finalizeUrl: String, context: Context): String {
        val template = readRawResource(context, R.raw.app_creation)
        val requestBody = JSONObject(template).apply {
            put("label", label)
            val settings = getJSONObject("settings")
            val signOn = settings.getJSONObject("signOn").apply {
                put("ssoAcsUrl", finalizeUrl)
                put("audience", finalizeUrl)
                put("recipient", finalizeUrl)
                put("destination", finalizeUrl)
            }
            settings.put("signOn", signOn)
            put("settings", settings)
        }

        val output = httpRequest(
            path = "api/v1/apps",
            method = "POST",
            body = requestBody.toString(),
            acceptableCodes = intArrayOf(HttpURLConnection.HTTP_OK)
        )

        val response = JSONObject(output)
        applicationId = response.getString("id")
        val groupId = fetchGroupId("Everyone")
        assignApplicationToGroup(requireNotNull(applicationId), groupId)

        // NEW: wait until the assignment is visible to GETs (propagation)
        waitForAppGroupLink(requireNotNull(applicationId), groupId)

        // Tiny grace period (Okta can still be warming up)
        hardSleep(1200)

        return requireNotNull(applicationId)
    }

    private fun fetchGroupId(groupName: String): String {
        val output = httpRequest(
            path = "api/v1/groups?limit=100",
            method = "GET",
            acceptableCodes = intArrayOf(HttpURLConnection.HTTP_OK)
        )
        val arr = JSONArray(output)
        for (i in 0 until arr.length()) {
            val group = arr.getJSONObject(i)
            if (group.getJSONObject("profile").getString("name") == groupName) {
                return group.getString("id")
            }
        }
        throw IllegalStateException("Cannot fetch id of a group with name '$groupName'")
    }

    private fun assignApplicationToGroup(appId: String, groupId: String) {
        httpRequest(
            path = "api/v1/apps/$appId/groups/$groupId",
            method = "PUT",
            body = "{}", // Okta often expects a JSON object even if empty
            acceptableCodes = intArrayOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_NO_CONTENT)
        )
    }

    // poll for the app-group link to be visible
    private fun waitForAppGroupLink(appId: String, groupId: String, timeoutMs: Long = 30_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastErr: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                httpRequest(
                    path = "api/v1/apps/$appId/groups/$groupId",
                    method = "GET",
                    acceptableCodes = intArrayOf(HttpURLConnection.HTTP_OK)
                )
                return // success
            } catch (t: Throwable) {
                lastErr = t
                hardSleep(700)
            }
        }
        throw lastErr ?: IllegalStateException("Timed out waiting for app $appId to link group $groupId")
    }

    fun getApplicationMetadata(): String {
        val appId = applicationId ?: error("Application ID is not set. Create an application first.")
        return httpRequest(
            path = "api/v1/apps/$appId/sso/saml/metadata",
            method = "GET",
            accept = "application/xml",
            acceptableCodes = intArrayOf(HttpURLConnection.HTTP_OK)
        )
    }

    fun createUser(name: String, email: String, password: String) {
        val requestBody = JSONObject().apply {
            put(
                "profile",
                JSONObject().apply {
                put(
                    "firstName",
                    name
                )
                put(
                    "lastName",
                    name
                )
                put(
                    "email",
                    email
                )
                put(
                    "login",
                    email
                )
            }
            )
            put(
                "credentials",
                JSONObject().apply {
                    put(
                        "password",
                        JSONObject().put(
                            "value",
                            password
                        )
                    )
                    put(
                        "recovery_question",
                        JSONObject().apply {
                            put(
                                "question",
                                "What is the answer to life, the universe and everything?"
                            )
                            put(
                                "answer",
                                "fortytwo"
                            )
                        }
                    )
                }
            )
        }

        val output = httpRequest(
            path = "api/v1/users?activate=true",
            method = "POST",
            body = requestBody.toString(),
            acceptableCodes = intArrayOf(HttpURLConnection.HTTP_OK)
        )
        val response = JSONObject(output)
        userIds.add(response.getString("id"))
        network.WireTestLogger.getLog("LogginUserId").info("User id is ${response.getString("id")}")
    }

    fun deleteUser(userId: String) {
        httpRequest(
            path = "api/v1/users/$userId/lifecycle/deactivate",
            method = "POST",
            acceptableCodes = intArrayOf(HttpURLConnection.HTTP_OK)
        )
        val response = httpRequest(
            path = "api/v1/users/$userId",
            method = "DELETE",
            acceptableCodes = intArrayOf(HttpURLConnection.HTTP_NO_CONTENT)
        )
        network.WireTestLogger.getLog("Delete user").info("Delete user response is $response")
    }

    fun cleanUp() {
        if (applicationId != null) {
            httpRequest(
                path = "api/v1/apps/$applicationId/lifecycle/deactivate",
                method = "POST",
                acceptableCodes = intArrayOf(HttpURLConnection.HTTP_OK)
            )
            httpRequest(
                path = "api/v1/apps/$applicationId",
                method = "DELETE",
                acceptableCodes = intArrayOf(HttpURLConnection.HTTP_NO_CONTENT)
            )
        }
        for (userId in userIds) {
            deleteUser(userId)
        }
    }
}
