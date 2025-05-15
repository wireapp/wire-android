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
package com.wire.android.tests.core.config


import com.wire.android.tests.core.exceptions.HttpRequestException
import com.wire.android.tests.core.services.backend.BackendConnections
import com.wire.android.tests.core.utils.DomUtils
import com.wire.android.tests.core.utils.ZetaLogger
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.client.ClientProperties
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.HttpURLConnection.*
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Duration
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.ws.rs.core.MediaType


class KeycloakAPIClient {

    val log: Logger = ZetaLogger.getLog(KeycloakAPIClient::class.simpleName)
    val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(3)
    val READ_TIMEOUT: Duration = Duration.ofSeconds(240)
    val REALM: String = "master"
    val ADMIN: String = "admin"
    private var backendName: String? = null

    private var clientId: String? = null
    private var userIds = mutableSetOf<String>()

    // Create a trust manager that does not validate certificate chains
    val trustAllCerts: Array<TrustManager> = arrayOf(object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return emptyArray()  // Return empty array instead of null
        }

        override fun checkClientTrusted(
            certs: Array<X509Certificate>,
            authType: String
        ) {
            // Trust all client certificates
        }

        override fun checkServerTrusted(
            certs: Array<X509Certificate>,
            authType: String
        ) {
            // Trust all server certificates
        }
    })

    fun KeycloakAPIClient(backendName: String?) {
        this.backendName = backendName
        val configuration = ClientConfig()
        configuration.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT.toMillis().toInt())
        configuration.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT.toMillis().toInt())
        // Install the all-trusting trust manager
        try {
            val sc = SSLContext.getInstance("SSL")
            sc.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
        } catch (e: Exception) {
            log.severe("Could not install all-trusting trust manager: " + e.message)
        }
    }

    private fun getBaseURI(): String {
        val connections = BackendConnections()
        return connections.get(backendName).getKeycloakUrl()
    }

    fun getMetadata(): String {
        val c: HttpURLConnection? = buildDefaultRequest(
            String.format("/realms/%s/protocol/saml/descriptor", REALM),
            MediaType.APPLICATION_XML
        )
        if (c == null){
            throw HttpRequestException("url connection is null")
        }
        val output: String = httpGet(c, intArrayOf(HTTP_OK))
        val domUtils =DomUtils()
        domUtils.toDocument(output)
        return output
    }

    fun createSAMLClient(teamId: String?, backendURL: String) {
        val token: String = authorize()
        log.info("Token: $token")
        val c: HttpURLConnection? = buildAuthorizedRequest(
            String.format("admin/realms/%s/clients", REALM),
            MediaType.APPLICATION_JSON, token
        )
        val finalizeUrl = String.format("%ssso/finalize-login/%s", backendURL, teamId)
        val requestBody = JSONObject()
        requestBody.put("clientId", finalizeUrl)
        //requestBody.put("surrogateAuthRequired", false);
        requestBody.put("enabled", true)
        requestBody.put("adminUrl", "")
        requestBody.put("baseUrl", "")
        requestBody.put("rootUrl", "")
        requestBody.put("name", "")
        requestBody.put("description", "")
        //requestBody.put("clientAuthenticatorType", "client-secret");
        //requestBody.put("secret", "iJg6Ysa0qWdmGCnNzLkqb6rTjjhRgPF6");
        val redirectUris = JSONArray()
        redirectUris.put(finalizeUrl)
        requestBody.put("redirectUris", redirectUris)
        val webOrigins = JSONArray()
        webOrigins.put(backendURL.substring(0, backendURL.length - 1))
        requestBody.put("webOrigins", webOrigins)
        requestBody.put("protocol", "saml")
        val attributes = JSONObject()
        attributes.put("display.on.consent.screen", "false")
        attributes.put("saml.encrypt", "false")
        attributes.put("saml_assertion_consumer_url_post", finalizeUrl)
        attributes.put("saml.client.signature", "false")
        attributes.put("saml.artifact.binding", "false")
        attributes.put("saml.assertion.signature", "true")
        attributes.put("saml.onetimeuse.condition", "false")
        attributes.put("saml.server.signature.keyinfo.ext", "false")
        attributes.put("saml.server.signature.keyinfo.xmlSigKeyInfoKeyNameTransformer", "NONE")
        requestBody.put("attributes", attributes)
        if (c == null){
            throw HttpRequestException("url connection is null")
        }
        val location: String = httpPost(c, requestBody.toString(), intArrayOf(HTTP_CREATED))
        this.clientId = getIdFromLocation(location)
    }

    fun createSAMLClientQARealm(teamId: String?, backendURL: String) {
        val REALM = "QA"
        val token: String = authorize()
        log.info("Token: $token")
        val c: HttpURLConnection? = buildAuthorizedRequest(
            String.format("admin/realms/%s/clients", REALM),
            MediaType.APPLICATION_JSON, token
        )
        val finalizeUrl = String.format("%ssso/finalize-login/%s", backendURL, teamId)
        val requestBody = JSONObject()
        requestBody.put("clientId", finalizeUrl)
        //requestBody.put("surrogateAuthRequired", false);
        requestBody.put("enabled", true)
        requestBody.put("adminUrl", "")
        requestBody.put("baseUrl", "")
        requestBody.put("rootUrl", "")
        requestBody.put("name", "")
        requestBody.put("description", "")
        //requestBody.put("clientAuthenticatorType", "client-secret");
        //requestBody.put("secret", "iJg6Ysa0qWdmGCnNzLkqb6rTjjhRgPF6");
        val redirectUris = JSONArray()
        redirectUris.put(finalizeUrl)
        requestBody.put("redirectUris", redirectUris)
        val webOrigins = JSONArray()
        webOrigins.put(backendURL.substring(0, backendURL.length - 1))
        requestBody.put("webOrigins", webOrigins)
        requestBody.put("protocol", "saml")
        val attributes = JSONObject()
        attributes.put("display.on.consent.screen", "false")
        attributes.put("saml.encrypt", "false")
        attributes.put("saml_assertion_consumer_url_post", finalizeUrl)
        attributes.put("saml.client.signature", "false")
        attributes.put("saml.artifact.binding", "false")
        attributes.put("saml.assertion.signature", "true")
        attributes.put("saml.onetimeuse.condition", "false")
        attributes.put("saml.server.signature.keyinfo.ext", "false")
        attributes.put("saml.server.signature.keyinfo.xmlSigKeyInfoKeyNameTransformer", "NONE")
        requestBody.put("attributes", attributes)
        if (c == null){
            throw HttpRequestException("url connection is null")
        }
        val location: String = httpPost(c, requestBody.toString(), intArrayOf(HTTP_CREATED))
        this.clientId = getIdFromLocation(location)
    }

    fun createUser(username: String?, firstname: String?, lastname: String?, email: String?, password: String?) {
        val c: HttpURLConnection? = buildAuthorizedRequest(
            String.format("admin/realms/%s/users", REALM),
            MediaType.APPLICATION_JSON, authorize()
        )
        val user = JSONObject()
        user.put("username", username)
        user.put("firstName", firstname)
        user.put("lastName", lastname)
        user.put("email", email)
        user.put("emailVerified", true)
        user.put("enabled", true)
        val credentials = JSONArray()
        val passwordCredential = JSONObject()
        passwordCredential.put("type", "password")
        passwordCredential.put("value", password)
        passwordCredential.put("temporary", false)
        credentials.put(passwordCredential)
        user.put("credentials", credentials)
        if (c == null){
            throw HttpRequestException("url connection is null")
        }
        val location: String = httpPost(c, user.toString(), intArrayOf(HTTP_CREATED))
        userIds.add(getIdFromLocation(location))
    }

    fun createUserQARealm(username: String?, firstname: String?, lastname: String?, email: String?, password: String?) {
        val REALM = "QA"
        val c: HttpURLConnection? = buildAuthorizedRequest(
            String.format("admin/realms/%s/users", REALM),
            MediaType.APPLICATION_JSON, authorize()
        )
        val user = JSONObject()
        user.put("username", username)
        user.put("firstName", firstname)
        user.put("lastName", lastname)
        user.put("email", email)
        user.put("emailVerified", true)
        user.put("enabled", true)
        val credentials = JSONArray()
        val passwordCredential = JSONObject()
        passwordCredential.put("type", "password")
        passwordCredential.put("value", password)
        passwordCredential.put("temporary", false)
        credentials.put(passwordCredential)
        user.put("credentials", credentials)
        if (c == null){
            throw HttpRequestException("url connection is null")
        }
        val location: String = httpPost(c, user.toString(), intArrayOf(HTTP_CREATED))
        userIds.add(getIdFromLocation(location))
    }

    fun cleanUp() {
        clientId?.let { deleteSAMLClient(it) }
        for (userId in userIds) {
            deleteUser(userId)
        }
    }

    fun cleanUpQARealm() {
        val REALM = "QA"
        val token: String = authorize()
        //        JSONObject requestBody = new JSONObject();
//        JSONObject user = new JSONObject();
        val client: HttpURLConnection? =
            buildAuthorizedRequest(String.format("admin/realms/%s/clients", REALM), MediaType.APPLICATION_JSON, token)
        val userConnection: HttpURLConnection? =
            buildAuthorizedRequest(String.format("admin/realms/%s/users", REALM), MediaType.APPLICATION_JSON, token)
        if (client == null){
            throw HttpRequestException("url connection is null")
        }
        val location: String = httpGet(client, intArrayOf(HTTP_CREATED))
        clientId = getIdFromLocation(location)
        if (userConnection == null){
            throw HttpRequestException("url connection is null")
        }
        val locationUser: String = httpGet(userConnection, intArrayOf(HTTP_CREATED))
        userIds = mutableSetOf(getIdFromLocation(locationUser))
        clientId?.let { deleteSAMLClientQA(it) }
        for (userId in userIds) {
            deleteUserQA(userId)
        }
    }


    private fun deleteSAMLClient(clientId: String) {
        val c = buildAuthorizedRequest(
            String.format("admin/realms/%s/clients/%s", REALM, clientId),
            MediaType.APPLICATION_JSON, authorize()
        )
        httpDelete(c, intArrayOf(HTTP_NO_CONTENT))
    }

    private fun deleteSAMLClientQA(clientId: String) {
        val REALM = "QA"
        val c = buildAuthorizedRequest(
            String.format("admin/realms/%s/clients/%s", REALM, clientId),
            MediaType.APPLICATION_JSON, authorize()
        )
        httpDelete(c, intArrayOf(HTTP_NO_CONTENT))
    }

    private fun deleteUser(userId: String) {
        val c = buildAuthorizedRequest(
            String.format("admin/realms/%s/users/%s", REALM, userId),
            MediaType.APPLICATION_JSON, authorize()
        )
        httpDelete(c, intArrayOf(HTTP_NO_CONTENT))
    }

    private fun deleteUserQA(userId: String) {
        val REALM = "QA"
        val c = buildAuthorizedRequest(
            String.format("admin/realms/%s/users/%s", REALM, userId),
            MediaType.APPLICATION_JSON, authorize()
        )
        httpDelete(c, intArrayOf(HTTP_NO_CONTENT))
    }

    private fun getIdFromLocation(location: String): String {
        log.info("Location: $location")
        return location.substring(location.lastIndexOf("/") + 1)
    }

    private fun authorize(): String {
        val credentials = Credentials()
        val PASSWORD: String = credentials.getCredentials("KEYCLOAK_PASSWORD")
        val client = OkHttpClient()

        val requestBody = FormBody.Builder()
            .add("client_id", "admin-cli")
            .add("username", ADMIN)
            .add("password", PASSWORD)
            .add("grant_type", "password")
            .build()

        val request = Request.Builder()
            .url("${getBaseURI()}/realms/master/protocol/openid-connect/token")
            .post(requestBody)
            .addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED)
            .addHeader("Accept", MediaType.APPLICATION_JSON)
            .build()

        log.info("POST ${request.url}")
        logHttpRequestHeaders(request)

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            val status = response.code
            logResponseAndStatusCode(responseBody, status)

            if (!response.isSuccessful) {
                val error = "${response.message} ($status): $responseBody"
                log.severe(error)
                throw HttpRequestException(error, status)
            }

            return JSONObject(responseBody).getString("access_token")
        }
    }

    // Helper function to log request headers (similar to logHttpRequestProperties)
    private fun logHttpRequestHeaders(request: Request) {
        val headers = request.headers
        for (i in 0 until headers.size) {
            log.info(">>> ${headers.name(i)}: ${headers.value(i)}")
        }
    }

    private fun buildAuthorizedRequest(path: String, mediaType: String, token: String): HttpURLConnection? {
        var c: HttpURLConnection? = null
        try {
            val url = URL(String.format("%s/%s", getBaseURI(), path))
            c = url.openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        c!!.setRequestProperty("Content-Type", mediaType)
        c!!.setRequestProperty("Accept", mediaType)
        c!!.setRequestProperty("Authorization", "Bearer $token")
        return c
    }

    private fun buildDefaultRequest(path: String, mediaType: String): HttpURLConnection? {
        var c: HttpURLConnection? = null
        try {
            val url = URL(String.format("%s%s", getBaseURI(), path))
            c = url.openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        c!!.setRequestProperty("Content-Type", mediaType)
        c!!.setRequestProperty("Accept", mediaType)
        return c
    }

    private fun httpGet(c: HttpURLConnection, acceptableResponseCodes: IntArray): String {
        var response = ""
        var status = -1
        try {
            log.info("GET " + c.url)
            c.requestMethod = "GET"
            logHttpRequestProperties(c)
            status = c.responseCode
            response = readStream(c.inputStream)
            logResponseAndStatusCode(response, status)
            assertResponseCode(status, acceptableResponseCodes)
            return response
        } catch (e: IOException) {
            try {
                response = readStream(c.errorStream)
            } catch (ex: IOException) {
                log.fine("Could not read error stream: " + e.message)
            }
            if (Arrays.stream(acceptableResponseCodes).anyMatch { acceptable: Int -> acceptable > 400 }) {
                assertResponseCode(status, acceptableResponseCodes)
                log.info(String.format(">>> Response (%s): %s", status, response))
                return response
            } else {
                val error = String.format("%s (%s): %s", e.message, status, response)
                log.severe(error)
                throw HttpRequestException(error, status)
            }
        } finally {
            c.disconnect()
        }
    }

    private fun httpPost(c: HttpURLConnection, requestBody: String, acceptableResponseCodes: IntArray): String {
        var response = ""
        var location = ""
        var status = -1
        try {
            log.info("POST " + c.url)
            c.requestMethod = "POST"
            logHttpRequestProperties(c)
            logRequest(requestBody)
            c.doOutput = true
            writeStream(requestBody, c.outputStream)
            status = c.responseCode
            response = readStream(c.inputStream)
            location = c.getHeaderField("Location")
            logResponseAndStatusCode(response, status)
            assertResponseCode(status, acceptableResponseCodes)
            return location
        } catch (e: IOException) {
            try {
                response = readStream(c.errorStream)
            } catch (ex: IOException) {
                log.fine("Could not read error stream: " + e.message)
            }
            if (Arrays.stream(acceptableResponseCodes).anyMatch { acceptable: Int -> acceptable > 400 }) {
                assertResponseCode(status, acceptableResponseCodes)
                log.info(String.format(">>> Response (%s): %s", status, response))
                return response
            } else {
                val error = String.format("%s (%s): %s", e.message, status, response)
                log.severe(error)
                throw HttpRequestException(error, status)
            }
        } finally {
            c.disconnect()
        }
    }

    private fun httpDelete(c: HttpURLConnection?, acceptableResponseCodes: IntArray): String {
        var response = ""
        var status = -1
        try {
            log.info("DELETE " + c!!.url)
            c!!.requestMethod = "DELETE"
            logHttpRequestProperties(c)
            c!!.doOutput = true
            writeStream("", c!!.outputStream)
            status = c!!.responseCode
            response = readStream(c!!.inputStream)
            logResponseAndStatusCode(response, status)
            assertResponseCode(status, acceptableResponseCodes)
            return response
        } catch (e: IOException) {
            try {
                response = readStream(c!!.errorStream)
            } catch (ex: IOException) {
                log.fine("Could not read error stream: " + e.message)
            }
            val error = String.format("%s (%s): %s", e.message, status, response)
            log.severe(error)
            throw HttpRequestException(error, status)
        } finally {
            c!!.disconnect()
        }
    }

    @Throws(IOException::class)
    private fun writeStream(data: String, os: OutputStream) {
        val wr = DataOutputStream(os)
        val writer = BufferedWriter(OutputStreamWriter(wr, StandardCharsets.UTF_8))
        try {
            writer.write(data)
        } finally {
            writer.close()
            wr.close()
        }
    }

    @Throws(IOException::class)
    private fun readStream(`is`: InputStream?): String {
        if (`is` != null) {
            BufferedReader(InputStreamReader(`is`)).use { `in` ->
                var inputLine: String?
                var content = ""
                while ((`in`.readLine().also { inputLine = it }) != null) {
                    content += inputLine
                }
                return content
            }
        }
        return ""
    }

    private fun logRequest(request: String) {
        if (request.isEmpty()) {
            log.info(" >>> Request with no request body")
        } else {
            if (log.isLoggable(Level.FINE)) {
                log.info(String.format(" >>> Request: %s", request))
            } else {
                log.info(String.format(" >>> Request: %s", truncate(request)))
            }
        }
    }

    private fun logResponseAndStatusCode(response: String, responseCode: Int) {
        if (response.isEmpty()) {
            log.info(String.format(" >>> Response (%s) with no response body", responseCode))
        } else {
            if (log.isLoggable(Level.FINE)) {
                log.info(String.format(" >>> Response (%s): %s", responseCode, response))
            } else {
                log.info(String.format(" >>> Response (%s): %s", responseCode, truncate(response)))
            }
        }
    }

    private fun truncate(text: String): String {
        val MAX_LOG_ENTRY_LENGTH = 280
        if (text.length > MAX_LOG_ENTRY_LENGTH) {
            return text.substring(0, MAX_LOG_ENTRY_LENGTH) + "..."
        }
        return text
    }

    private fun assertResponseCode(responseCode: Int, acceptableResponseCodes: IntArray) {
        if (Arrays.stream(acceptableResponseCodes).noneMatch { a: Int -> a == responseCode }) {
            throw HttpRequestException(
                String.format(
                    "Backend request failed. Request return code is: %d. Expected codes are: %s.",
                    responseCode,
                    acceptableResponseCodes.contentToString()
                ),
                responseCode
            )
        }
    }

    private fun logHttpRequestProperties(c: HttpURLConnection?) {
        if (log.isLoggable(Level.FINE)) {
            for (property in c!!.requestProperties.keys) {
                val values = listOf(c!!.getRequestProperty(property))
                log.fine(String.format("%s: %s", property, java.lang.String.join(", ", values)))
            }
        }
    }
}
