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

import com.wire.android.config.TestDispatcherProvider
import com.wire.android.feature.aiassistant.model.AiModelDescriptor
import com.wire.android.feature.aiassistant.model.AiModelDownloadState
import com.wire.android.feature.aiassistant.storage.FakeAiModelStorage
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.exists
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class HuggingFaceAiModelDownloaderTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun givenTokenExists_whenDownloading_thenBearerTokenIsSent() = runTest {
        val arrangement = Arrangement(tempDir)
            .withToken("hf_token")
            .withHttpResponse(FakeAiModelHttpResponse(body = MODEL_BYTES))
            .arrange()

        arrangement.downloader.download(arrangement.descriptor).toList()

        assertEquals("Bearer hf_token", arrangement.httpClient.lastHeaders["Authorization"])
    }

    @Test
    fun givenTokenIsMissing_whenDownloading_thenAuthRequiredIsEmittedAndRequestIsNotSent() = runTest {
        val arrangement = Arrangement(tempDir)
            .withToken(null)
            .arrange()

        val states = arrangement.downloader.download(arrangement.descriptor).toList()

        assertEquals(listOf(AiModelDownloadState.Starting, AiModelDownloadState.AuthRequired), states)
        assertEquals(0, arrangement.httpClient.requestCount)
    }

    @Test
    fun givenUnauthorizedResponse_whenDownloading_thenAuthRequiredIsEmitted() = runTest {
        val arrangement = Arrangement(tempDir)
            .withToken("hf_token")
            .withHttpResponse(FakeAiModelHttpResponse(code = 401))
            .arrange()

        val states = arrangement.downloader.download(arrangement.descriptor).toList()

        assertEquals(AiModelDownloadState.AuthRequired, states.last())
    }

    @Test
    fun givenForbiddenResponse_whenDownloading_thenAuthRequiredIsEmitted() = runTest {
        val arrangement = Arrangement(tempDir)
            .withToken("hf_token")
            .withHttpResponse(FakeAiModelHttpResponse(code = 403))
            .arrange()

        val states = arrangement.downloader.download(arrangement.descriptor).toList()

        assertEquals(AiModelDownloadState.AuthRequired, states.last())
    }

    @Test
    fun givenSuccessfulResponse_whenDownloading_thenTempFileIsPromotedToFinalFile() = runTest {
        val arrangement = Arrangement(tempDir)
            .withToken("hf_token")
            .withHttpResponse(FakeAiModelHttpResponse(body = MODEL_BYTES))
            .arrange()

        val states = arrangement.downloader.download(arrangement.descriptor).toList()

        assertEquals(AiModelDownloadState.Ready(arrangement.storage.modelFile.absolutePath), states.last())
        assertTrue(arrangement.storage.modelFile.exists())
        assertFalse(arrangement.storage.tempModelFile.exists())
        assertEquals(MODEL_TEXT, arrangement.storage.modelFile.readText())
    }

    @Test
    fun givenResponseWithContentLength_whenDownloading_thenProgressIsEmitted() = runTest {
        val arrangement = Arrangement(tempDir)
            .withToken("hf_token")
            .withHttpResponse(FakeAiModelHttpResponse(body = MODEL_BYTES, contentLength = MODEL_BYTES.size.toLong()))
            .arrange()

        val states = arrangement.downloader.download(arrangement.descriptor).toList()

        assertTrue(states.contains(AiModelDownloadState.Downloading(1F)))
    }

    @Test
    fun givenHttpClientFails_whenDownloading_thenTempFileIsRemoved() = runTest {
        val arrangement = Arrangement(tempDir)
            .withToken("hf_token")
            .withFailingHttpClient()
            .arrange()
        arrangement.storage.ensureModelDirectoryExists(arrangement.descriptor)
        arrangement.storage.tempModelFile.writeText("partial")

        val states = arrangement.downloader.download(arrangement.descriptor).toList()

        assertTrue(states.last() is AiModelDownloadState.Failed)
        assertFalse(arrangement.storage.tempModelFile.toPath().exists())
    }

    @Test
    fun givenDownloadIsCancelled_whenDownloading_thenTempFileIsRemoved() = runTest {
        val arrangement = Arrangement(tempDir)
            .withToken("hf_token")
            .withHttpResponse(FakeAiModelHttpResponse(inputStream = CancellingInputStream()))
            .arrange()

        try {
            arrangement.downloader.download(arrangement.descriptor).toList()
            fail("Expected download cancellation")
        } catch (exception: CancellationException) {
            assertFalse(arrangement.storage.tempModelFile.toPath().exists())
        }
    }

    private class Arrangement(tempDir: Path) {
        val descriptor = AiModelDescriptor(
            displayName = "Test model",
            repositoryId = "google/test-model",
            artifactPath = "model/test-model.litertlm",
            localDirectoryName = "test-model",
            localFileName = "model.litertlm"
        )
        val storage = FakeAiModelStorage(tempDir, descriptor)
        var httpClient = FakeAiModelHttpClient(FakeAiModelHttpResponse())
        private var tokenProvider = FakeHuggingFaceTokenProvider("hf_token")

        fun withToken(token: String?) = apply {
            tokenProvider = FakeHuggingFaceTokenProvider(token)
        }

        fun withHttpResponse(response: FakeAiModelHttpResponse) = apply {
            httpClient = FakeAiModelHttpClient(response)
        }

        fun withFailingHttpClient() = apply {
            httpClient = FakeAiModelHttpClient(response = null)
        }

        fun arrange() = Result(
            descriptor = descriptor,
            storage = storage,
            httpClient = httpClient,
            downloader = HuggingFaceAiModelDownloader(
                tokenProvider = tokenProvider,
                httpClient = httpClient,
                storage = storage,
                dispatchers = TestDispatcherProvider()
            )
        )
    }

    private data class Result(
        val descriptor: AiModelDescriptor,
        val storage: FakeAiModelStorage,
        val httpClient: FakeAiModelHttpClient,
        val downloader: HuggingFaceAiModelDownloader
    )

    private companion object {
        const val MODEL_TEXT = "model"
        val MODEL_BYTES = MODEL_TEXT.toByteArray()
    }
}

private class FakeHuggingFaceTokenProvider(private val token: String?) : HuggingFaceTokenProvider {
    override suspend fun getToken(): String? = token
}

private class FakeAiModelHttpClient(
    private val response: FakeAiModelHttpResponse?
) : AiModelHttpClient {
    var requestCount = 0
        private set
    var lastHeaders = emptyMap<String, String>()
        private set

    override suspend fun open(url: String, headers: Map<String, String>): AiModelHttpResponse {
        requestCount++
        lastHeaders = headers
        return response ?: throw java.io.IOException("Failed request")
    }
}

private class FakeAiModelHttpResponse(
    override val code: Int = 200,
    private val body: ByteArray = ByteArray(0),
    override val contentLength: Long? = null,
    private val inputStream: InputStream? = null
) : AiModelHttpResponse {
    override fun inputStream(): InputStream = inputStream ?: ByteArrayInputStream(body)

    override fun close() = Unit
}

private class CancellingInputStream : InputStream() {
    override fun read(): Int = throw CancellationException("Cancelled")
}
