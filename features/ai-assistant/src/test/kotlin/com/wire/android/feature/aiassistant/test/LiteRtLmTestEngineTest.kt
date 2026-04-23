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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class LiteRtLmTestEngineTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `given model file does not exist, when health check runs, then missing model is returned`() = runTest {
        val engine = LiteRtLmTestEngine(
            inferenceFactory = FakeLiteRtLmInferenceFactory(response = "OK")
        )

        val result = engine.runHealthCheck(File(tempDir, "missing-model.litertlm").absolutePath)

        assertEquals(AiModelHealthCheckResult.MissingModel, result)
    }

    @Test
    fun `given model is not a litertlm file, when health check runs, then unsupported model is returned and inference is not created`() =
        runTest {
            val factory = FakeLiteRtLmInferenceFactory(response = "OK")
            val engine = LiteRtLmTestEngine(inferenceFactory = factory)

            val result = engine.runHealthCheck(modelFile("model.task").absolutePath)

            assertEquals(AiModelHealthCheckResult.UnsupportedModel, result)
            assertEquals(0, factory.createCount)
        }

    @Test
    fun `given inference returns text, when health check runs, then healthy is returned and inference is closed`() = runTest {
        val factory = FakeLiteRtLmInferenceFactory(response = "OK")
        val engine = LiteRtLmTestEngine(inferenceFactory = factory)

        val result = engine.runHealthCheck(modelFile().absolutePath)

        assertEquals(AiModelHealthCheckResult.Healthy, result)
        assertEquals(true, factory.inference.isClosed)
    }

    @Test
    fun `given inference returns blank text, when health check runs, then empty response is returned`() = runTest {
        val engine = LiteRtLmTestEngine(
            inferenceFactory = FakeLiteRtLmInferenceFactory(response = " ")
        )

        val result = engine.runHealthCheck(modelFile().absolutePath)

        assertEquals(AiModelHealthCheckResult.EmptyResponse, result)
    }

    @Test
    fun `given inference throws, when health check runs, then inference failed is returned`() = runTest {
        val engine = LiteRtLmTestEngine(
            inferenceFactory = FakeLiteRtLmInferenceFactory(throwable = IllegalStateException("Engine init failed"))
        )

        val result = engine.runHealthCheck(modelFile().absolutePath)

        assertTrue(result is AiModelHealthCheckResult.InferenceFailed)
        assertEquals("Engine init failed", (result as AiModelHealthCheckResult.InferenceFailed).message)
    }

    private fun modelFile(name: String = "model.litertlm"): File =
        File(tempDir, name).also { it.writeText("model") }
}

private class FakeLiteRtLmInferenceFactory(
    response: String = "",
    throwable: Throwable? = null
) : LiteRtLmInferenceFactory {
    val inference = FakeLiteRtLmInference(response, throwable)
    var createCount = 0
        private set

    override fun create(modelPath: String, initialExchanges: List<Pair<String, String>>): LiteRtLmInference {
        createCount++
        return inference
    }
}

private class FakeLiteRtLmInference(
    private val response: String,
    private val throwable: Throwable?
) : LiteRtLmInference {
    var isClosed = false
        private set

    override fun generateResponse(userMessage: String): String {
        throwable?.let { throw it }
        return response
    }

    override fun close() {
        isClosed = true
    }
}
