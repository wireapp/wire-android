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

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UrlConnectionWireLlmClientTest {
    @Test
    fun givenSuccessfulResponse_whenQuerying_thenExactEndpointAndEscapedPromptAreUsed() = runTest {
        val connection = FakeHttpURLConnection(
            responseCode = 200,
            responseBody = """{"result":"Remote result"}"""
        )
        var requestedUrl: URL? = null
        val client = UrlConnectionWireLlmClient { url ->
            requestedUrl = url
            connection
        }

        val result = client.query("192.168.1.20", "Line one\n\"quoted\"")

        assertEquals(WireLlmQueryResult.Success("Remote result"), result)
        assertEquals("http://192.168.1.20:8080/query", requestedUrl.toString())
        assertEquals("POST", connection.requestMethod)
        assertEquals("application/json", connection.requestProperties["Content-Type"]?.single())
        assertEquals("""{"prompt":"Line one\n\"quoted\""}""", connection.requestBody.toString(Charsets.UTF_8))
        assertTrue(connection.disconnected)
    }

    @Test
    fun givenMalformedResponse_whenQuerying_thenTypedFailureIsReturned() = runTest {
        val client = UrlConnectionWireLlmClient {
            FakeHttpURLConnection(responseCode = 200, responseBody = """{"unexpected":"value"}""")
        }

        assertEquals(
            WireLlmQueryResult.Failure("Wire LLM returned a malformed response"),
            client.query("10.0.0.4", "Hello")
        )
    }

    @Test
    fun givenNonSuccessfulResponse_whenQuerying_thenHttpFailureIsReturned() = runTest {
        val client = UrlConnectionWireLlmClient {
            FakeHttpURLConnection(responseCode = 502, responseBody = "")
        }

        assertEquals(
            WireLlmQueryResult.Failure("Wire LLM request failed with HTTP 502"),
            client.query("10.0.0.4", "Hello")
        )
    }
}

private class FakeHttpURLConnection(
    url: URL = URL("http://localhost"),
    private val responseCode: Int,
    responseBody: String
) : HttpURLConnection(url) {
    val requestBody = ByteArrayOutputStream()
    var disconnected: Boolean = false

    private val responseBytes = responseBody.toByteArray()

    override fun getOutputStream() = requestBody
    override fun getInputStream() = ByteArrayInputStream(responseBytes)
    override fun getResponseCode(): Int = responseCode
    override fun disconnect() {
        disconnected = true
    }
    override fun usingProxy(): Boolean = false
    override fun connect() = Unit
}
