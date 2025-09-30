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
package okta

import android.content.Context
import com.wire.android.testSupport.BuildConfig
import com.wire.android.testSupport.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class OktaApiClient {

    // Properties to hold the state
    private var applicationId: String? = null
    private val userIds = mutableSetOf<String>()

    companion object {

        private const val BASE_URI = "https://dev-500508-admin.oktapreview.com"

        private val apiKey: String by lazy {
           BuildConfig.OKTA_API_KEY_PASSWORD
        }

        fun getFinalizeUrlDependingOnBackend(backendUrl: String): String {
            val trimmedUrl = backendUrl.removeSuffix("/")
            return "$trimmedUrl/sso/finalize-login"
        }
    }

    @Suppress("TooGenericExceptionThrown", "MagicNumber")
    /**
     * Core suspend function to handle all HTTP requests asynchronously.
     * It uses HttpURLConnection and runs on the IO dispatcher.
     */
    private suspend fun makeRequest(
        path: String,
        method: String,
        body: String? = null,
        expectedStatusCodes: List<Int>,
        acceptHeader: String = "application/json"
    ): String = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URI/$path")
        val connection = (url.openConnection() as HttpsURLConnection).apply {
            requestMethod = method
            setRequestProperty("Authorization", "SSWS $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", acceptHeader)
            connectTimeout = 3000 // 3 seconds
            readTimeout = 240000 // 240 seconds

            if (method == "POST" || method == "PUT") {
                doOutput = true
                body?.let {
                    outputStream.bufferedWriter().use { writer -> writer.write(it) }
                }
            }
        }

        val responseCode = connection.responseCode
        val responseStream = if (responseCode in expectedStatusCodes) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val responseBody = responseStream.bufferedReader().use(BufferedReader::readText)

        if (responseCode !in expectedStatusCodes) {
            throw Exception("Request Failed: $responseCode ${connection.responseMessage}. Body: $responseBody")
        }

        connection.disconnect()
        responseBody
    }

    /**
     * Reads a resource file from the classpath.
     */
    private fun readRawResource(context: Context, resId: Int): String {
        context.resources.openRawResource(resId).bufferedReader().use { reader ->
            return reader.readText()
        }
    }

    suspend fun createApplication(label: String, finalizeUrl: String, context: Context): String {
        val template = readRawResource(context, R.raw.app_creation) // Note the leading slash
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

        val responseJson = makeRequest(
            path = "api/v1/apps",
            method = "POST",
            body = requestBody.toString(),
            expectedStatusCodes = listOf(HttpURLConnection.HTTP_OK)
        )

        this.applicationId = JSONObject(responseJson).getString("id")
        val groupId = fetchGroupId("Everyone")
        assignApplicationToGroup(this.applicationId!!, groupId)
        return this.applicationId!!
    }

    suspend fun createUser(name: String, email: String, password: String) {
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

        val responseJson = makeRequest(
            path = "api/v1/users?activate=true",
            method = "POST",
            body = requestBody.toString(),
            expectedStatusCodes = listOf(HttpURLConnection.HTTP_OK)
        )
        val userId = JSONObject(responseJson).getString("id")
        userIds.add(userId)
    }

    private suspend fun fetchGroupId(groupName: String): String {
        val responseJson = makeRequest(
            path = "api/v1/groups?limit=100",
            method = "GET",
            expectedStatusCodes = listOf(HttpURLConnection.HTTP_OK)
        )
        val groups = JSONArray(responseJson)
        for (i in 0 until groups.length()) {
            val group = groups.getJSONObject(i)
            if (group.getJSONObject("profile").getString("name") == groupName) {
                return group.getString("id")
            }
        }
        throw IllegalStateException("Cannot fetch id of a group with name '$groupName'")
    }

    suspend fun getApplicationMetadata(): String {
        val appId =
            applicationId ?: throw IllegalStateException("Application ID is not set. Create an application first.")
        return makeRequest(
            path = "api/v1/apps/$appId/sso/saml/metadata",
            method = "GET",
            expectedStatusCodes = listOf(HttpURLConnection.HTTP_OK),
            acceptHeader = "application/xml" // Special case for metadata
        )
    }

    private suspend fun assignApplicationToGroup(appId: String, groupId: String) {
        makeRequest(
            path = "api/v1/apps/$appId/groups/$groupId",
            method = "PUT",
            body = "{}", // Body can be empty but some APIs require it to be a valid JSON object
            expectedStatusCodes = listOf(HttpURLConnection.HTTP_OK)
        )
    }

    private suspend fun deleteUser(userId: String) {
        // 1. Deactivate the user
        makeRequest(
            path = "api/v1/users/$userId/lifecycle/deactivate",
            method = "POST",
            expectedStatusCodes = listOf(HttpURLConnection.HTTP_OK)
        )
        // 2. Delete the user
        makeRequest(
            path = "api/v1/users/$userId",
            method = "DELETE",
            expectedStatusCodes = listOf(HttpURLConnection.HTTP_NO_CONTENT)
        )
    }

    suspend fun cleanUp() {
        applicationId?.let { appId ->
            // Deactivate app
            makeRequest(
                path = "api/v1/apps/$appId/lifecycle/deactivate",
                method = "POST",
                expectedStatusCodes = listOf(HttpURLConnection.HTTP_OK)
            )
            // Delete app
            makeRequest(
                path = "api/v1/apps/$appId",
                method = "DELETE",
                expectedStatusCodes = listOf(HttpURLConnection.HTTP_NO_CONTENT)
            )
        }
        userIds.forEach { deleteUser(it) }
        userIds.clear()
        applicationId = null
    }
}
