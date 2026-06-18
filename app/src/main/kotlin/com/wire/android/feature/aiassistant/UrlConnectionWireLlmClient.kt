/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.feature.aiassistant

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.coroutines.coroutineContext

class UrlConnectionWireLlmClient @Inject constructor() : WireLlmClient {
    private var connectionFactory: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as HttpURLConnection
    }

    internal constructor(connectionFactory: (URL) -> HttpURLConnection) : this() {
        this.connectionFactory = connectionFactory
    }

    override suspend fun query(serverIp: String, prompt: String): WireLlmQueryResult =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                coroutineContext.ensureActive()
                connection = connectionFactory(URL("http://$serverIp:$SERVER_PORT/query")).apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECTION_TIMEOUT_MILLIS
                    readTimeout = READ_TIMEOUT_MILLIS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                }
                val requestBody = json.encodeToString(WireLlmQueryRequest(prompt))
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(requestBody) }
                coroutineContext.ensureActive()

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    return@withContext WireLlmQueryResult.Failure("Wire LLM request failed with HTTP $responseCode")
                }
                val responseBody = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val result = json.decodeFromString<WireLlmQueryResponse>(responseBody).result.trim()
                if (result.isBlank()) {
                    WireLlmQueryResult.Failure("Wire LLM returned an empty response")
                } else {
                    WireLlmQueryResult.Success(result)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: SerializationException) {
                WireLlmQueryResult.Failure("Wire LLM returned a malformed response")
            } catch (exception: IOException) {
                WireLlmQueryResult.Failure(exception.message ?: "Unable to connect to Wire LLM")
            } finally {
                connection?.disconnect()
            }
        }

    private companion object {
        const val SERVER_PORT = 8080
        const val CONNECTION_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 60_000
        val json = Json { ignoreUnknownKeys = true }
    }
}

@Serializable
private data class WireLlmQueryRequest(val prompt: String)

@Serializable
private data class WireLlmQueryResponse(val result: String)
