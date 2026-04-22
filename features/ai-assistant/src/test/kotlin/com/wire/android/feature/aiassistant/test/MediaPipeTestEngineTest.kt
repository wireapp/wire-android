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
package com.wire.android.feature.aiassistant.test

import java.io.File
import com.wire.android.feature.aiassistant.model.AiModelDescriptor
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class MediaPipeTestEngineTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `given model file does not exist, when health check runs, then missing model is returned`() = runTest {
        // given
        val engine = MediaPipeTestEngine(
            descriptor = supportedDescriptor(),
            inferenceFactory = FakeMediaPipeLlmInferenceFactory(response = "OK")
        )

        // when
        val result = engine.runHealthCheck(File(tempDir, "missing-model.litertlm").absolutePath)

        // then
        assertEquals(AiModelHealthCheckResult.MissingModel, result)
    }

    @Test
    fun `given inference returns text, when health check runs, then healthy is returned and inference is closed`() = runTest {
        // given
        val factory = FakeMediaPipeLlmInferenceFactory(response = "OK")
        val engine = MediaPipeTestEngine(
            descriptor = supportedDescriptor(),
            inferenceFactory = factory
        )

        // when
        val result = engine.runHealthCheck(modelFile().absolutePath)

        // then
        assertEquals(AiModelHealthCheckResult.Healthy, result)
        assertEquals(true, factory.inference.isClosed)
    }

    @Test
    fun `given inference returns blank text, when health check runs, then empty response is returned`() = runTest {
        // given
        val engine = MediaPipeTestEngine(
            descriptor = supportedDescriptor(),
            inferenceFactory = FakeMediaPipeLlmInferenceFactory(response = " ")
        )

        // when
        val result = engine.runHealthCheck(modelFile().absolutePath)

        // then
        assertEquals(AiModelHealthCheckResult.EmptyResponse, result)
    }

    @Test
    fun `given inference throws, when health check runs, then inference failed is returned`() = runTest {
        // given
        val engine = MediaPipeTestEngine(
            descriptor = supportedDescriptor(),
            inferenceFactory = FakeMediaPipeLlmInferenceFactory(throwable = IllegalStateException("Cannot run model"))
        )

        // when
        val result = engine.runHealthCheck(modelFile().absolutePath)

        // then
        assertTrue(result is AiModelHealthCheckResult.InferenceFailed)
        assertEquals("Cannot run model", (result as AiModelHealthCheckResult.InferenceFailed).message)
    }

    @Test
    fun `given gemma 3n model, when health check runs, then unsupported model is returned and inference is not created`() = runTest {
        // given
        val factory = FakeMediaPipeLlmInferenceFactory(response = "OK")
        val engine = MediaPipeTestEngine(
            descriptor = supportedDescriptor(
                repositoryId = "google/gemma-3n-E2B-it-litert-lm",
                artifactPath = "gemma-3n-E2B-it-int4.litertlm"
            ),
            inferenceFactory = factory
        )

        // when
        val result = engine.runHealthCheck(modelFile().absolutePath)

        // then
        assertEquals(AiModelHealthCheckResult.UnsupportedModel, result)
        assertEquals(0, factory.createCount)
    }

    private fun modelFile(): File =
        File(tempDir, "model.litertlm").also { it.writeText("model") }

    private fun supportedDescriptor(
        repositoryId: String = "google/gemma-3-1b-it-litert-lm",
        artifactPath: String = "gemma-3-1b-it-int4.litertlm"
    ): AiModelDescriptor =
        AiModelDescriptor(
            displayName = "Model",
            repositoryId = repositoryId,
            artifactPath = artifactPath,
            localDirectoryName = "model",
            localFileName = "model.litertlm"
        )
}

private class FakeMediaPipeLlmInferenceFactory(
    response: String = "",
    throwable: Throwable? = null
) : MediaPipeLlmInferenceFactory {
    val inference = FakeMediaPipeLlmInference(response, throwable)
    var createCount = 0
        private set

    override fun create(modelPath: String): MediaPipeLlmInference {
        createCount++
        return inference
    }
}

private class FakeMediaPipeLlmInference(
    private val response: String,
    private val throwable: Throwable?
) : MediaPipeLlmInference {
    var isClosed = false
        private set

    override fun generateResponse(prompt: String): String {
        throwable?.let { throw it }
        return response
    }

    override fun close() {
        isClosed = true
    }
}
