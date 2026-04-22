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
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.feature.aiassistant.download

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class UrlConnectionAiModelHttpClient @Inject constructor() : AiModelHttpClient {
    override suspend fun open(url: String, headers: Map<String, String>): AiModelHttpResponse {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECTION_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            instanceFollowRedirects = true
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }
        return UrlConnectionAiModelHttpResponse(connection)
    }

    private companion object {
        const val CONNECTION_TIMEOUT_MILLIS = 30_000
        const val READ_TIMEOUT_MILLIS = 30_000
    }
}

private class UrlConnectionAiModelHttpResponse(
    private val connection: HttpURLConnection
) : AiModelHttpResponse {
    override val code: Int
        get() = connection.responseCode

    override val contentLength: Long?
        get() = connection.contentLengthLong.takeIf { it > 0L }

    override fun inputStream(): InputStream = connection.inputStream

    override fun close() {
        connection.disconnect()
    }
}
