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

import logger.WireTestLogger
import java.net.HttpURLConnection
import java.net.URL
import java.net.HttpCookie

class BackendClient {
    companion object {

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
            body: String? = null,
            headers: Map<String, String>
        ): String {

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
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (method != "GET" && body != null) {
                connection.outputStream.use { os ->
                    os.write(body.toByteArray())
                    os.flush()
                }
            }

            val responseCode = connection.responseCode

            WireTestLogger.getLog("Null").info("Response is ${responseCode}")

            if (responseCode !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw Exception("HTTP $responseCode: $error")
            }

            return connection.inputStream.bufferedReader().use { it.readText() }
        }


        /**
         * Sends an HTTP GET or POST request with optional JSON body and headers.
         * Returns the response body and any cookies from the "Set-Cookie" header.
         */
        /**

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
            headers: Map<String, String>
        ): HttpResponseWithCookies {

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
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (method != "GET" && body != null) {
                connection.outputStream.use { os ->
                    os.write(body.toByteArray())
                    os.flush()
                }
            }

            val responseCode = connection.responseCode
            WireTestLogger.getLog("Test").info("Response code is: " + responseCode.toString())
            if (responseCode !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw Exception("HTTP $responseCode: $error")
            }
            val cookies = connection.getHeaderField("Set-Cookie")?.let {
                HttpCookie.parse(it)
            } ?: emptyList()

            return HttpResponseWithCookies(
                connection.inputStream.bufferedReader().use { it.readText() }, cookies
            )
        }


    }
}

data class HttpResponseWithCookies(
    val body: String,
    val cookies: List<HttpCookie>
)

