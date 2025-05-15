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
package com.wire.android.tests.core.services.backend

//import java.net.HttpCookie
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.wire.android.tests.core.config.Credentials
import com.wire.android.tests.core.email.InbucketMailbox
import com.wire.android.tests.core.exceptions.HttpRequestException
import com.wire.android.tests.core.messages.ActivationMessage
import com.wire.android.tests.core.messages.WireMessage.Companion.ZETA_CODE_HEADER_NAME
import com.wire.android.tests.core.models.AccessCookie
import com.wire.android.tests.core.models.AccessCredentials
import com.wire.android.tests.core.models.AccessToken
import com.wire.android.tests.core.models.BasicAuth
import com.wire.android.tests.core.models.ClientUser
import com.wire.android.tests.core.models.Message
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.Authenticator
import java.net.HttpCookie
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_OK
import java.net.InetSocketAddress
import java.net.MalformedURLException
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Arrays
import java.util.Objects
import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.ws.rs.core.MediaType


class Backend(
    private var name: String?,
    private var backendUrl: String?,
    private var webappUrl: String?,
    private var domain: String?,
    private var backendWebsocket: String?,
    private var deeplink: String?,
    var inbucketUrl: String?,
    private var keycloakUrl: String?,
    private var acmeDiscoveryUrl: String?,
    private var k8sNamespace: String?,
    private var basicAuth: BasicAuth?,
    private var inbucketAuth: BasicAuth?,
    insecure: Boolean,
    private var socksProxy: String?
) {
    val log: Logger = Logger.getLogger(Backend::class.simpleName)

    val PROFILE_PICTURE_JSON_ATTRIBUTE: String = "complete"

    val PROFILE_PREVIEW_PICTURE_JSON_ATTRIBUTE: String = "preview"

    private var proxy: Proxy? = null

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

    init {
        if (socksProxy != null && socksProxy!!.isNotEmpty()) {
            val authenticator: Authenticator = object : Authenticator() {
                public override fun getPasswordAuthentication(): PasswordAuthentication {
                    val credentials = Credentials()
                    return PasswordAuthentication(
                        "qa",
                        credentials.getCredentials("SOCKS_PROXY_PASSWORD").toCharArray()
                    )
                }
            }
            this.proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("socks.wire.link", 1080))
            Authenticator.setDefault(authenticator)
        } else {
            this.proxy = null
        }

        if (insecure) {
            // Install the all-trusting trust manager
            try {
                val sc = SSLContext.getInstance("SSL")
                sc.init(null, trustAllCerts, SecureRandom())
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            } catch (e: Exception) {
                log.severe("Could not install all-trusting trust manager: " + e.message)
            }
        }
    }

    fun getBackendName(): String? {
        return this.name
    }

    fun getBackendUrl(): String? {
        return backendUrl
    }

    fun getWebappUrl(): String? {
        return webappUrl
    }

    fun getDomain(): String? {
        return domain
    }

    fun getDeeplinkForAndroid(): String {
        if (deeplink == null) {
            throw RuntimeException("No known deeplink URL for " + getBackendName())
        }
        return String.format("wire://access/?config=%s", deeplink)
    }

    fun getDeeplinkOldFlowsForAndroid(): String {
        if (deeplink == null) {
            throw RuntimeException("No known deeplink URL for " + getBackendName())
        }
        return String.format("wire://access/?config=%s&login-type=old", deeplink)
    }

    fun getDeeplinkForiOS(protocolHandler: String): String {
        if (deeplink == null) {
            throw java.lang.RuntimeException("No known deeplink URL for " + getBackendName())
        }
        return String.format("$protocolHandler://access/?config=%s", deeplink)
    }

    fun getDeeplinkUrl(): String {
        if (deeplink == null || deeplink.isNullOrBlank() ) {
            throw java.lang.RuntimeException("No known deeplink URL for " + getBackendName())
        }
        return deeplink as String
    }

    fun getBackendWebsocket(): String {
        return backendWebsocket!!
    }

    private fun getBackendUrl(path: String): URL {
        try {
            return URL(backendUrl + path)
        } catch (e: MalformedURLException) {
            throw java.lang.RuntimeException(e)
        }
    }

    fun hasInbucketSetup(): Boolean {
        return inbucketUrl != null
    }

    fun getInbucketUrl(): String {
        return inbucketUrl!!
    }

    fun getKeycloakUrl(): String {
        return keycloakUrl!!
    }

    fun getAcmeDiscoveryUrl(): String {
        return acmeDiscoveryUrl!!
    }

    fun getK8sNamespace(): String {
        if (k8sNamespace == null) {
            throw java.lang.RuntimeException("Backend is missing its namespace. Use 'kubectl get namespaces' to find out!")
        }
        return k8sNamespace as String
    }

    fun getBasicAuthUser(): String? {
        return basicAuth!!.user
    }

    fun getBasicAuthPassword(): String? {
        return basicAuth!!.password
    }

    fun getInbucketUsername(): String? {
        return inbucketAuth!!.user
    }

    fun getInbucketPassword(): String? {
        return inbucketAuth!!.password
    }

    fun useProxy(): Boolean {
        return socksProxy != null
    }


    // region Backend features
    fun isFeatureSFTEnabled(): Boolean {
        return if (this.name == "qa-column-1") {
            false
        } else {
            true
        }
    }

    fun isFeatureEncryptionAtRestEnabled(): Boolean {
        return if (this.name == "qa-column-1" || this.name == "qa-column-3") {
            true
        } else {
            false
        }
    }


    // endregion
    // region HTTP connection logic
    private fun buildDefaultRequestOnBackdoor(path: String, mediaType: String): HttpURLConnection? {
        val url = getBackendUrl(path)
        var c: HttpURLConnection? = null
        try {
            c = (if (socksProxy != null) url.openConnection(proxy) else url.openConnection()) as HttpURLConnection
        } catch (e: IOException) {
            throw java.lang.RuntimeException(e)
        }
        c!!.setRequestProperty("Content-Type", mediaType)
        c!!.setRequestProperty("Accept", mediaType)
        c!!.setRequestProperty("Authorization", basicAuth!!.encoded)
        return c
    }


    private fun buildNewDefaultRequestOnBackdoor(path: String, mediaType: String): Request {
        val url = getBackendUrl(path)
        val authHeader = basicAuth?.encoded ?: throw IllegalStateException("Basic auth credentials not set")

        return Request.Builder()
            .url(url)
            .header("Content-Type", mediaType)
            .header("Accept", mediaType)
            .header("Authorization", authHeader)
            .build()
    }
//    private fun buildNewDefaultRequestOnBackdoor(path: String, mediaType: String): HttpRequest.Builder {
//        val url = getBackendUrl(path)
//        try {
//            return HttpRequest.newBuilder()
//                .uri(url.toURI())
//                .header("Content-Type", mediaType)
//                .header("Accept", mediaType)
//                .header("Authorization", basicAuth!!.encoded)
//        } catch (e: URISyntaxException) {
//            throw java.lang.RuntimeException(e)
//        }
//    }

    private fun buildDefaultRequest(path: String, mediaType: String): HttpURLConnection? {
        val url = getBackendUrl(path)
        var c: HttpURLConnection? = null
        try {
            c = (if (socksProxy != null) url.openConnection(proxy) else url.openConnection()) as HttpURLConnection
        } catch (e: IOException) {
            throw java.lang.RuntimeException(e)
        }
        c!!.setRequestProperty("Content-Type", mediaType)
        c!!.setRequestProperty("Accept", mediaType)
        return c
    }

    private fun buildDefaultRequestWithAuth(path: String, token: AccessToken): HttpURLConnection {
        return buildDefaultRequestWithAuth(path, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON, token, null)
    }

    private fun buildDefaultRequestWithAuth(path: String, token: AccessToken, cookie: AccessCookie): HttpURLConnection {
        return buildDefaultRequestWithAuth(path, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON, token, cookie)
    }

    private fun buildDefaultRequestWithAuth(
        path: String, contentType: String, acceptType: String,
        token: AccessToken, cookie: AccessCookie?
    ): HttpURLConnection {
        val url = getBackendUrl(path)
        val c: HttpURLConnection
        try {
            c = (if (socksProxy != null) url.openConnection(proxy) else url.openConnection()) as HttpURLConnection
        } catch (e: IOException) {
            throw java.lang.RuntimeException(e)
        }
        c.setRequestProperty("Content-Type", contentType)
        c.setRequestProperty("Accept", Objects.requireNonNullElse(acceptType, "*/*"))
        val header = String.format("%s %s", token.getType(), token.getValue())
        log.fine("Authorization: $header")
        c.setRequestProperty("Authorization", header)
        if (cookie != null) {
            log.fine("Cookie set")
            c.setRequestProperty("Cookie", cookie.getName() + "=" + cookie.getValue())
        }
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

    private fun getResponseCodeForHttpGet(c: HttpURLConnection): Int {
        var response: String? = ""
        var status = -1
        try {
            log.info("GET " + c.url)
            c.requestMethod = "GET"
            logHttpRequestProperties(c)
            status = c.responseCode
            response = readStream(c.inputStream)
            logResponseAndStatusCode(response, status)
        } catch (e: IOException) {
            try {
                response = readStream(c.errorStream)
            } catch (ex: IOException) {
                log.fine("Could not read error stream: " + e.message)
            }
            logResponseAndStatusCode(response, status)
        } finally {
            c.disconnect()
        }
        return status
    }

    private fun httpGetImage(c: HttpURLConnection, acceptableResponseCodes: IntArray): Bitmap? {
        var status = -1
        try {
            log.info("GET ${c.url}")
            c.requestMethod = "GET"
            logHttpRequestProperties(c)
            status = c.responseCode

            // Replace ImageIO.read() with BitmapFactory.decodeStream()
            return BitmapFactory.decodeStream(c.inputStream)
        } catch (e: IOException) {
            try {
                val response: String = readStream(c.errorStream)
                val error = "${e.message} ($status): $response"
                log.severe(error)
                throw HttpRequestException(error, status)
            } catch (ex: IOException) {
                log.fine("Could not read error stream: ${e.message}")
                return null
            }
        } finally {
            c.disconnect()
        }
    }

    private fun getResponseCodeForHttpPost(c: HttpURLConnection, requestBody: String): Int {
        var response: String? = ""
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
            logResponseAndStatusCode(response, status)
        } catch (e: IOException) {
            try {
                response = readStream(c.errorStream)
            } catch (ex: IOException) {
                log.fine("Could not read error stream: " + e.message)
            }
            logResponseAndStatusCode(response, status)
        } finally {
            c.disconnect()
        }
        return status
    }

    private fun httpPost(c: HttpURLConnection, requestBody: String, acceptableResponseCodes: IntArray): String {
        var response = ""
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

    private fun httpPost(c: HttpURLConnection, requestBody: ByteArray, acceptableResponseCodes: IntArray): String {
        var response = ""
        var status = -1
        try {
            log.info("POST " + c.url)
            c.requestMethod = "POST"
            logHttpRequestProperties(c)
            log.fine(" >>> Request: byte[]")
            c.doOutput = true
            val out = DataOutputStream(c.outputStream)
            out.write(requestBody)
            out.flush()
            out.close()
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
            val error = String.format("%s (%s): %s", e.message, status, response)
            log.severe(error)
            throw HttpRequestException(error, status)
        } finally {
            c.disconnect()
        }
    }

    private fun getResponseCodeForHttpPut(c: HttpURLConnection, requestBody: String): Int {
        var response: String? = ""
        var status = -1
        try {
            log.info("POST " + c.url)
            c.requestMethod = "PUT"
            logHttpRequestProperties(c)
            logRequest(requestBody)
            c.doOutput = true
            writeStream(requestBody, c.outputStream)
            status = c.responseCode
            response = readStream(c.inputStream)
            logResponseAndStatusCode(response, status)
        } catch (e: IOException) {
            try {
                response = readStream(c.errorStream)
            } catch (ex: IOException) {
                log.fine("Could not read error stream: " + e.message)
            }
            logResponseAndStatusCode(response, status)
        } finally {
            c.disconnect()
        }
        return status
    }


    private fun httpPut(c: HttpURLConnection, requestBody: String, acceptableResponseCodes: IntArray): String {
        var response = ""
        var status = -1
        try {
            log.info("PUT " + c.url)
            c.requestMethod = "PUT"
            logHttpRequestProperties(c)
            logRequest(requestBody)
            c.doOutput = true
            writeStream(requestBody, c.outputStream)
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
            val error = String.format("%s (%s): %s", e.message, status, response)
            log.severe(error)
            throw HttpRequestException(error, status)
        } finally {
            c.disconnect()
        }
    }

    private fun httpPatch(path: String, mediaType: String, requestBody: String, acceptableResponseCodes: IntArray): String {
        val clientBuilder: OkHttpClient.Builder = OkHttpClient().newBuilder()

        if (socksProxy != null && !socksProxy!!.isEmpty()) {
            val authenticator: Authenticator = object : Authenticator() {
                public override fun getPasswordAuthentication(): PasswordAuthentication {
                    val credentials = Credentials()
                    return (PasswordAuthentication(
                        "qa",
                        credentials.getCredentials("SOCKS_PROXY_PASSWORD").toCharArray()
                    ))
                }
            }
            Authenticator.setDefault(authenticator)
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("socks.wire.link", 1080))
            clientBuilder.proxy(proxy)
        }

        val client: OkHttpClient = clientBuilder.build()
        val url = getBackendUrl(path)
        try {
            log.info("PATCH $url")
            val body: RequestBody = requestBody.toRequestBody("application/json".toMediaTypeOrNull())
            // Build the PATCH request
            val request: Request = Request.Builder()
                .url(url) // Replace with your URL
                .patch(body)
                .addHeader("Content-Type", mediaType)
                .addHeader("Accept", mediaType)
                .addHeader("Authorization", basicAuth!!.encoded)
                .build()
            logRequest(requestBody)
            val response: Response = client.newCall(request).execute()
            val status: Int = response.code
            val responseBody = response.body?.string() ?: return ""
            logResponseAndStatusCode(responseBody, status)
            assertResponseCode(status, acceptableResponseCodes)
            return responseBody
        } catch (e: IOException) {
            throw java.lang.RuntimeException(e)
        }
    }

    private fun httpDelete(c: HttpURLConnection, requestBody: String, acceptableResponseCodes: IntArray): String {
        var response = ""
        var status = -1
        try {
            log.info("DELETE " + c.url)
            c.requestMethod = "DELETE"
            logHttpRequestProperties(c)
            log.fine(String.format(" >>> Request: %s", requestBody))
            c.doOutput = true
            writeStream(requestBody, c.outputStream)
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
            val error = String.format("%s (%s): %s", e.message, status, response)
            log.severe(error)
            throw HttpRequestException(error, status)
        } finally {
            c.disconnect()
        }
    }

    private fun httpDelete(c: HttpURLConnection, acceptableResponseCodes: IntArray): String {
        var response = ""
        var status = -1
        try {
            log.info("DELETE " + c.url)
            c.requestMethod = "DELETE"
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
            val error = String.format("%s (%s): %s", e.message, status, response)
            log.severe(error)
            throw HttpRequestException(error, status)
        } finally {
            c.disconnect()
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
    private fun readStream(inputStream: InputStream?): String {
        if (inputStream == null) return ""

        return inputStream.bufferedReader().use { reader ->
            var content = ""
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                content += line
            }
            content
        }
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

    private fun logResponseAndStatusCode(response: String?, responseCode: Int) {
        if (response != null && response.isEmpty()) {
            log.info(String.format(" >>> Response (%s) with no response body", responseCode))
        } else {
            if (log.isLoggable(Level.FINE)) {
                log.info(String.format(" >>> Response (%s): %s", responseCode, response))
            } else {
                log.info(String.format(" >>> Response (%s): %s", responseCode, truncate(response)))
            }
        }
    }

    private fun truncate(text: String?): String {
        val MAX_LOG_ENTRY_LENGTH = 280
        if (text == null){
            return ""
        }
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

    private fun logHttpRequestProperties(c: HttpURLConnection) {
        if (log.isLoggable(Level.FINE)) {
            for (property in c.requestProperties.keys) {
                val values = listOf(c.getRequestProperty(property))
                log.fine(String.format("%s: %s", property, java.lang.String.join(", ", values)))
            }
        }
    }


    // region Id
    fun getUserId(user: ClientUser?): String {
        if (user == null){
            throw RuntimeException("client user is null")
        }
        val `object`: JSONObject = getUserInfo(receiveAuthToken(user))
        return `object`.getString("id")
    }

    // region User Profile info
    private fun getUserInfo(token: AccessToken): JSONObject {
        val c = buildDefaultRequestWithAuth("self", token)
        val output = httpGet(c, intArrayOf(HTTP_OK))
        return JSONObject(output)
    }

    // region Authentication and user token
    private fun receiveAuthToken(user: ClientUser): AccessToken {
        val token = getTokenIfExpired(user)?.getAccessToken() ?: throw RuntimeException("access token not found")
        return token
    }


    private fun getTokenIfExpired(user: ClientUser): AccessCredentials? {
        if (user.getAccessCredentialsWithoutRefresh() == null) {
            val newCredentials: AccessCredentials = login(user)
            user.accessCredentials = newCredentials
            return newCredentials
        }
        if (user.getAccessCredentialsWithoutRefresh()!!.getAccessToken() == null || user.getAccessCredentialsWithoutRefresh()!!
                .getAccessToken()!!
                .isInvalid()
            || user.getAccessCredentialsWithoutRefresh()!!.getAccessToken()!!.isExpired()
        ) {
            val newCredentials: AccessCredentials = access(user.getAccessCredentialsWithoutRefresh()!!)
            user.accessCredentials = newCredentials
            return newCredentials
        }
        return user.getAccessCredentialsWithoutRefresh()
    }


    fun login(user: ClientUser): AccessCredentials {
        val c = buildDefaultRequest("login", MediaType.APPLICATION_JSON)
        val requestBody = JSONObject()
        requestBody.put("email", user.email)
        requestBody.put("password", user.password)
        requestBody.put("label", "")
        var response: String? = httpPost(c!!, requestBody.toString(), intArrayOf(HTTP_OK, HTTP_FORBIDDEN))
        try {
            if (c!!.responseCode == 403) {
                if (this.getInbucketUrl() == null) {
                    throw IOException("Received 403 for 2FA but no inbucket url present - check your backend settings")
                }
                // This is probably a user with 2FA
                trigger2FA(user.email)
                val inbucket: InbucketMailbox = InbucketMailbox(this, user.email)
                val recentMessages: List<Message> = inbucket.getRecentMessages(user.email)

                val lastMessage: Message = recentMessages[recentMessages.size - 1]
                val verificationCode: String = lastMessage.header[ZETA_CODE_HEADER_NAME]?.get(0) ?: "unknown"

                val c2 = buildDefaultRequest("login", MediaType.APPLICATION_JSON)
                val requestBodyLogin = JSONObject()
                requestBodyLogin.put("email", user.email)
                requestBodyLogin.put("password", user.password)
                requestBodyLogin.put("verification_code", verificationCode)
                response = httpPost(c2!!, requestBodyLogin.toString(), intArrayOf(HTTP_OK))
                val `object` = JSONObject(response)
                val cookiesHeader = c2!!.getHeaderField("Set-Cookie")
                val cookies = HttpCookie.parse(cookiesHeader)
                val cookie = AccessCookie("zuid", cookies)
                val token = AccessToken(
                    `object`.getString("access_token"), `object`.getString("token_type"),
                    `object`.getLong("expires_in")
                )
                return AccessCredentials(token, cookie)
            } else {
                // get access credentials from response
                val `object` = JSONObject(response)
                val cookiesHeader = c!!.getHeaderField("Set-Cookie")
                val cookies = HttpCookie.parse(cookiesHeader)
                val cookie = AccessCookie("zuid", cookies)
                val token = AccessToken(
                    `object`.getString("access_token"), `object`.getString("token_type"),
                    `object`.getLong("expires_in")
                )
                return AccessCredentials(token, cookie)
            }
        } catch (e: IOException) {
            throw HttpRequestException("Login failed: " + e.message)
        }
    }


    fun access(accessCredentials: AccessCredentials): AccessCredentials {
        val c = if (accessCredentials.getAccessToken() != null) {
            buildDefaultRequestWithAuth("access", accessCredentials.getAccessToken()!!)
        } else {
            buildDefaultRequest("access", MediaType.APPLICATION_JSON)
        }
        c!!.setRequestProperty("Cookie", String.format("zuid=%s", accessCredentials.getAccessCookie()!!.getValue()))
        val requestBody = JSONObject()
        requestBody.put("withCredentials", true)
        val response = httpPost(c!!, requestBody.toString(), intArrayOf(HTTP_OK))
        val `object` = JSONObject(response)
        val cookiesHeader = c!!.getHeaderField("Set-Cookie")
        var cookie: AccessCookie? = null
        if (cookiesHeader != null) {
            val cookies = HttpCookie.parse(cookiesHeader)
            if (cookies.stream().anyMatch { x: HttpCookie -> x.name == "zuid" }) {
                cookie = AccessCookie("zuid", cookies)
            }
        }

        if (cookie == null) {
            cookie = accessCredentials.getAccessCookie()
        }
        val token = AccessToken(
            `object`.getString("access_token"), `object`.getString("token_type"),
            `object`.getLong("expires_in")
        )
        return AccessCredentials(token, cookie)
    }

    fun logout(user: ClientUser) {
        val c = buildDefaultRequestWithAuth(
            "access/logout", receiveAuthToken(user),
            user.getAccessCredentialsWithoutRefresh()!!.getAccessCookie()!!
        )
        val requestBody = JSONObject()
        httpPost(c, requestBody.toString(), intArrayOf(HTTP_OK))
    }

    private fun trigger2FA(email: String) {
        val c = buildDefaultRequest("v5/verification-code/send", MediaType.APPLICATION_JSON)
        val requestBody = JSONObject()
        requestBody.put("action", "login")
        requestBody.put("email", email)
        httpPost(c!!, requestBody.toString(), intArrayOf(HTTP_OK, 429))
    }
}




