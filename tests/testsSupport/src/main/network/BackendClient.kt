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


import java.net.HttpURLConnection
import java.net.URL

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
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw Exception("HTTP $responseCode: $error")
            }

            return connection.inputStream.bufferedReader().use { it.readText() }
        }


    }
}
