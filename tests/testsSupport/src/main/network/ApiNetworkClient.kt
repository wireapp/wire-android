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
package network

import AccessCredentials
import backendConnections.HttpRequestException
import logger.WireTestLogger
import org.json.JSONObject
import user.utils.AccessCookie
import user.utils.AccessToken
import util.NumberSequence
import java.io.ByteArrayOutputStream
import java.net.HttpCookie
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Base64

object BackendClient {

    /**
    This is the base api class for making post and get request
    @Param url the api url
    @Param method: the method GET or  POST
    @Param body: the body json string
    @Param header: the headers
     */
    fun makeRequest(
        url: URL,
        method: String,
        body: Any? = null,
        headers: Map<String, String>,
        expectedResponseCodes: NumberSequence = NumberSequence.Range(200..299),
        accessToken: AccessToken? = null,
        cookie: AccessCookie? = null
    ): HttpURLConnection {
        print(url.toURI().toString())

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            doInput = true
            if (method != "GET") {
                doOutput = true
            }
            for ((key, value) in headers) {
                setRequestProperty(key, value)
            }
            if (accessToken != null) {
                setRequestProperty("Authorization", String.format("%s %s", accessToken.type, accessToken.value))
            }
            if (cookie != null) {
                setRequestProperty("Cookie", cookie.name + "=" + cookie.value)
            }
            connectTimeout = 5000
            readTimeout = 5000
        }

        if (method != "GET" && body != null) {
            connection.outputStream.use { os ->
                when (body) {
                    is String -> os.write(body.toByteArray())
                    is ByteArray -> os.write(body)
                    else -> throw IllegalArgumentException("Unsupported body type: ${body.javaClass.simpleName}")
                }
                os.flush()
            }
        }

        val responseCode = connection.responseCode

        WireTestLogger.getLog("Null").info("Response is $responseCode")

        val hasError = when (expectedResponseCodes) {
            is NumberSequence.Range -> responseCode !in expectedResponseCodes.range
            is NumberSequence.Array -> responseCode !in expectedResponseCodes.array
        }

        if (hasError) {
            val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw HttpRequestException(error, responseCode)
        }

        return connection
    }

    /**
    This is the base api class for making post and get request
    @Param url the api url
    @Param method: the method GET or  POST
    @Param body: the body json string
    @Param header: the headers
     */
    @Throws(Exception::class)
    fun sendJsonRequest(
        url: URL,
        method: String,
        body: Any? = null,
        headers: Map<String, String>,
        expectedResponseCodes: NumberSequence = NumberSequence.Range(200..299),
        accessToken: AccessToken? = null,
        cookie: AccessCookie? = null
    ) = makeRequest(url, method, body, headers, expectedResponseCodes, accessToken, cookie).response()


    /**
     * Sends an HTTP GET or POST request with optional JSON body and headers.
     * Returns the response body and any cookies from the "Set-Cookie" header.
    @Param url the api url
    @Param method: the method GET or  POST
    @Param body: the body json string
    @Param header: the headers
     */
    @Throws(Exception::class)
    fun sendJsonRequestWithCookies(
        url: URL,
        method: String,
        body: String? = null,
        headers: Map<String, String>,
        expectedResponseCodes: NumberSequence = NumberSequence.Range(200..299),
        accessToken: AccessToken? = null,
        cookie: AccessCookie? = null
    ): HttpResponseWithCookies {
        val connection = makeRequest(url, method, body, headers, expectedResponseCodes, accessToken, cookie)

        return HttpResponseWithCookies(
            body = connection.response(),
            cookies = connection.getHeaderField("Set-Cookie")?.let {
                HttpCookie.parse(it)
            } ?: emptyList()
        )
    }


    @Throws(Exception::class)
    fun uploadAsset(url: URL, token: AccessToken?, isPublic: Boolean, retention: String, content: ByteArray): String {
        val boundary = "frontier"
        val base64Encoder: Base64.Encoder = Base64.getEncoder()

        val digest = try {
            MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalArgumentException("MD5 algorithm not found", e)
        }
        digest.update(content)
        val md5 = base64Encoder.encodeToString(digest.digest())

        val metadata = JSONObject().apply {
            put("public", isPublic)
            put("retention", retention)
        }

        val multipartBodyBuilder = ByteArrayOutputStream()

        val metadataPartHeader = StringBuilder().apply {
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/json; charset=utf-8\r\n")
            append("Content-length: ").append(metadata.toString().length).append("\r\n")
            append("\r\n") // Empty line separates header from body
        }.toString().toByteArray()
        multipartBodyBuilder.write(metadataPartHeader)
        multipartBodyBuilder.write(metadata.toString().toByteArray())
        multipartBodyBuilder.write("\r\n".toByteArray()) // CRLF after body content

        // Add the second part: Content (application/octet-stream)
        val contentPartHeader = StringBuilder().apply {
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/octet-stream\r\n")
            append("Content-length: ").append(content.size).append("\r\n")
            append("Content-MD5: ").append(md5).append("\r\n")
            append("\r\n") // Empty line separates header from body
        }.toString().toByteArray()
        multipartBodyBuilder.write(contentPartHeader)
        multipartBodyBuilder.write(content) // Write the actual binary content
        multipartBodyBuilder.write("\r\n".toByteArray()) // CRLF after body content

        val footer = "--$boundary--\r\n"
        multipartBodyBuilder.write(footer.toByteArray())

        val fullRequestBodyBytes = multipartBodyBuilder.toByteArray()

        val headers = mapOf(
            "Content-Type" to "multipart/mixed; boundary=$boundary",
        )

        try {
            val output = sendJsonRequest(
                url = url,
                method = "POST",
                body = fullRequestBodyBytes,
                headers = headers,
                accessToken = token
            )

            val jsonOutput = JSONObject(output)
            return jsonOutput.getString("key")
        } catch (e: Exception) {
            throw RuntimeException("Asset upload failed: ${e.message}", e)
        }
    }

    fun HttpURLConnection.accessCredentials(response: String): AccessCredentials {
        val json = JSONObject(response)
        val cookie = getHeaderField("Set-Cookie")?.let { HttpCookie.parse(it) }?.let { AccessCookie("zuid", it) }
        val token = AccessToken(
            json.getString("access_token"),
            json.getString("token_type"),
            json.getLong("expires_in")
        )
        return AccessCredentials(token, cookie)
    }

    fun HttpURLConnection.response() = inputStream.bufferedReader().use { it.readText() }
}

data class HttpResponseWithCookies(
    val body: String,
    val cookies: List<HttpCookie>
)

