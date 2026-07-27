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
package keycloak

import CredentialsManager
import backendUtils.BackendClient
import backendUtils.BackendClient.Companion.applicationJson
import network.NetworkBackendClient
import network.NumberSequence
import network.RequestOptions
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Keycloak admin client used by acceptance tests to create SAML clients and test users,
 * then clean them up after the run.
 */
class KeycloakApiClient(
    private val backend: BackendClient
) {
    private val baseUrl = backend.keycloakUrl.removeSuffix("/")
    private val adminPassword: String by lazy {
        CredentialsManager.getSecretFieldValue("KEYCLOAK_QA_AUTOMATION", "PASSWORD")
            ?.takeIf { it.isNotBlank() }
            ?: error("Missing Keycloak admin password in generated test BuildConfig.")
    }

    private var clientId: String? = null
    private val userIds = LinkedHashSet<String>()

    fun getMetadata(): String = requestResponseBody(
        path = "/realms/$REALM/protocol/saml/descriptor",
        method = "GET",
        headers = mapOf(
            HEADER_CONTENT_TYPE to APPLICATION_XML,
            "Accept" to APPLICATION_XML
        ),
        expectedResponseCodes = intArrayOf(HttpURLConnection.HTTP_OK)
    )

    fun createSamlClient(teamId: String) {
        val finalizeUrl = "${backend.backendUrl}sso/finalize-login/$teamId"
        val requestBody = JSONObject().apply {
            put("clientId", finalizeUrl)
            put("enabled", true)
            put("adminUrl", "")
            put("baseUrl", "")
            put("rootUrl", "")
            put("name", "")
            put("description", "")
            put("redirectUris", JSONArray().put(finalizeUrl))
            put("webOrigins", JSONArray().put(backend.backendUrl.removeSuffix("/")))
            put("protocol", "saml")
            put(
                "attributes",
                JSONObject().apply {
                    put("display.on.consent.screen", "false")
                    put("saml.encrypt", "false")
                    put("saml_assertion_consumer_url_post", finalizeUrl)
                    put("saml.client.signature", "false")
                    put("saml.artifact.binding", "false")
                    put("saml.assertion.signature", "true")
                    put("saml.onetimeuse.condition", "false")
                    put("saml.server.signature.keyinfo.ext", "false")
                    put("saml.server.signature.keyinfo.xmlSigKeyInfoKeyNameTransformer", "NONE")
                }
            )
        }

        clientId = idFromLocation(
            requestLocation(
                path = "admin/realms/$REALM/clients",
                method = "POST",
                body = requestBody.toString(),
                headers = authorizedHeaders(),
                expectedResponseCodes = intArrayOf(HttpURLConnection.HTTP_CREATED)
            )
        )
    }

    fun createUser(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ) {
        val requestBody = JSONObject().apply {
            put("username", username)
            put("firstName", firstName)
            put("lastName", lastName)
            put("email", email)
            put("emailVerified", true)
            put("enabled", true)
            put(
                "credentials",
                JSONArray().put(
                    JSONObject().apply {
                        put("type", "password")
                        put("value", password)
                        put("temporary", false)
                    }
                )
            )
        }

        userIds.add(
            idFromLocation(
                requestLocation(
                    path = "admin/realms/$REALM/users",
                    method = "POST",
                    body = requestBody.toString(),
                    headers = authorizedHeaders(),
                    expectedResponseCodes = intArrayOf(HttpURLConnection.HTTP_CREATED)
                )
            )
        )
    }

    fun cleanUp() {
        clientId?.let(::deleteSamlClient)
        userIds.forEach(::deleteUser)
    }

    private fun deleteSamlClient(clientId: String) {
        request(
            path = "admin/realms/$REALM/clients/$clientId",
            method = "DELETE",
            headers = authorizedHeaders(),
            expectedResponseCodes = intArrayOf(HttpURLConnection.HTTP_NO_CONTENT)
        ).disconnect()
    }

    private fun deleteUser(userId: String) {
        request(
            path = "admin/realms/$REALM/users/$userId",
            method = "DELETE",
            headers = authorizedHeaders(),
            expectedResponseCodes = intArrayOf(HttpURLConnection.HTTP_NO_CONTENT)
        ).disconnect()
    }

    private fun authorize(): String {
        val requestBody = listOf(
            "client_id" to "admin-cli",
            "username" to ADMIN_USER,
            "password" to adminPassword,
            "grant_type" to "password"
        ).joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }

        val response = requestResponseBody(
            path = "/realms/$REALM/protocol/openid-connect/token",
            method = "POST",
            body = requestBody,
        headers = mapOf(
            HEADER_CONTENT_TYPE to FORM_URL_ENCODED,
            "Accept" to applicationJson
        ),
        expectedResponseCodes = intArrayOf(HttpURLConnection.HTTP_OK)
        )

        return JSONObject(response).getString("access_token")
    }

    private fun authorizedHeaders(): Map<String, String> = mapOf(
        HEADER_CONTENT_TYPE to applicationJson,
        "Accept" to applicationJson,
        "Authorization" to "Bearer ${authorize()}"
    )

    private fun requestResponseBody(
        path: String,
        method: String,
        headers: Map<String, String>,
        expectedResponseCodes: IntArray,
        body: String? = null
    ): String {
        val connection = request(path, method, headers, expectedResponseCodes, body)
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun requestLocation(
        path: String,
        method: String,
        headers: Map<String, String>,
        expectedResponseCodes: IntArray,
        body: String? = null
    ): String {
        val connection = request(path, method, headers, expectedResponseCodes, body)
        return try {
            connection.getHeaderField("Location")
                ?: error("Keycloak response is missing the Location header for ${connection.url}.")
        } finally {
            connection.disconnect()
        }
    }

    private fun request(
        path: String,
        method: String,
        headers: Map<String, String>,
        expectedResponseCodes: IntArray,
        body: String? = null
    ): HttpURLConnection {
        check(baseUrl.isNotBlank()) { "Backend '${backend.name}' is missing keycloakUrl." }

        return NetworkBackendClient.makeRequest(
            url = URI("$baseUrl/${path.removePrefix("/")}").toURL(),
            method = method,
            body = body,
            headers = headers,
            options = RequestOptions(
                expectedResponseCodes = NumberSequence.Array(expectedResponseCodes),
                proxy = backend.proxy
            )
        )
    }

    private fun idFromLocation(location: String): String = location.substringAfterLast("/")

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    private companion object {
        const val REALM = "master"
        const val ADMIN_USER = "admin"
        const val HEADER_CONTENT_TYPE = "Content-Type"
        const val APPLICATION_XML = "application/xml"
        const val FORM_URL_ENCODED = "application/x-www-form-urlencoded"
    }
}
