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
package com.wire.android.feature.aiassistant

import com.wire.android.feature.aiassistant.model.AiModelDownloadState
import com.wire.android.feature.aiassistant.model.AiModelStatus
import com.wire.android.feature.aiassistant.test.LiteRtLmInference
import com.wire.android.feature.aiassistant.test.LiteRtLmInferenceFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class DefaultAiMessageComposerAgentTest {

    @Test
    fun givenBlankInput_whenProofreadIsCalled_thenEmptyInputIsReturnedAndInferenceIsNotCreated() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withInferenceResponse("Hello")
            .arrange()

        val result = arrangement.agent.proofread("  ")

        assertEquals(AiMessageComposerResult.EmptyInput, result)
        assertEquals(0, arrangement.inferenceFactory.createCount)
    }

    @Test
    fun givenModelIsNotDownloaded_whenProofreadIsCalled_thenMissingModelIsReturned() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.NotDownloaded)
            .withInferenceResponse("Hello")
            .arrange()

        val result = arrangement.agent.proofread("Helo")

        assertEquals(AiMessageComposerResult.MissingModel, result)
        assertEquals(0, arrangement.inferenceFactory.createCount)
    }

    @Test
    fun givenModelIsDownloading_whenProofreadIsCalled_thenMissingModelIsReturned() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Downloading(progress = 0.5F))
            .withInferenceResponse("Hello")
            .arrange()

        val result = arrangement.agent.proofread("Helo")

        assertEquals(AiMessageComposerResult.MissingModel, result)
        assertEquals(0, arrangement.inferenceFactory.createCount)
    }

    @Test
    fun givenReadyModelIsNotLiteRtLm_whenProofreadIsCalled_thenUnsupportedModelIsReturned() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready("/tmp/model.task"))
            .withInferenceResponse("Hello")
            .arrange()

        val result = arrangement.agent.proofread("Helo")

        assertEquals(AiMessageComposerResult.UnsupportedModel, result)
        assertEquals(0, arrangement.inferenceFactory.createCount)
    }

    @Test
    fun givenReadyModel_whenProofreadIsCalled_thenPromptIsSentAndUpdatedTextIsReturned() = runTest {
        val inputText = "Helo,\nthis is a mesage."
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withInferenceResponse("Hello,\nthis is a message.")
            .arrange()

        val result = arrangement.agent.proofread(inputText)

        assertEquals(AiMessageComposerResult.Success("Hello,\nthis is a message."), result)
        assertTrue(arrangement.inferenceFactory.inference.prompt.contains(inputText))
        assertTrue(arrangement.inferenceFactory.inference.prompt.contains("Return only the corrected message text."))
        assertEquals(MODEL_PATH, arrangement.inferenceFactory.modelPath)
        assertTrue(arrangement.inferenceFactory.inference.isClosed)
    }

    @Test
    fun givenInferenceReturnsBlankText_whenProofreadIsCalled_thenEmptyResponseIsReturned() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withInferenceResponse(" ")
            .arrange()

        val result = arrangement.agent.proofread("Helo")

        assertEquals(AiMessageComposerResult.EmptyResponse, result)
        assertTrue(arrangement.inferenceFactory.inference.isClosed)
    }

    @Test
    fun givenInferenceFactoryThrows_whenProofreadIsCalled_thenInferenceFailedIsReturned() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withFactoryThrowable(IllegalStateException("Engine init failed"))
            .arrange()

        val result = arrangement.agent.proofread("Helo")

        assertEquals(AiMessageComposerResult.InferenceFailed("Engine init failed"), result)
    }

    @Test
    fun givenInferenceThrows_whenProofreadIsCalled_thenInferenceFailedIsReturnedAndInferenceIsClosed() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withInferenceThrowable(IllegalStateException("Cannot run model"))
            .arrange()

        val result = arrangement.agent.proofread("Helo")

        assertEquals(AiMessageComposerResult.InferenceFailed("Cannot run model"), result)
        assertTrue(arrangement.inferenceFactory.inference.isClosed)
    }

    @Test
    fun givenInferenceIsCancelled_whenProofreadIsCalled_thenCancellationIsRethrownAndInferenceIsClosed() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withInferenceThrowable(CancellationException("Cancelled"))
            .arrange()

        try {
            arrangement.agent.proofread("Helo")
            fail("Expected proofread to rethrow cancellation")
        } catch (exception: CancellationException) {
            assertEquals("Cancelled", exception.message)
        }
        assertTrue(arrangement.inferenceFactory.inference.isClosed)
    }

    private class Arrangement {
        private var modelStatus: AiModelStatus = AiModelStatus.NotDownloaded
        private var response: String = ""
        private var inferenceThrowable: Throwable? = null
        private var factoryThrowable: Throwable? = null

        fun withModelStatus(status: AiModelStatus) = apply {
            modelStatus = status
        }

        fun withInferenceResponse(response: String) = apply {
            this.response = response
        }

        fun withInferenceThrowable(throwable: Throwable) = apply {
            inferenceThrowable = throwable
        }

        fun withFactoryThrowable(throwable: Throwable) = apply {
            factoryThrowable = throwable
        }

        fun arrange(): Result {
            val inferenceFactory = FakeLiteRtLmInferenceFactory(response, inferenceThrowable, factoryThrowable)
            return Result(
                agent = DefaultAiMessageComposerAgent(
                    aiModelManager = FakeAiModelManager(modelStatus),
                    inferenceFactory = inferenceFactory
                ),
                inferenceFactory = inferenceFactory
            )
        }
    }

    private data class Result(
        val agent: DefaultAiMessageComposerAgent,
        val inferenceFactory: FakeLiteRtLmInferenceFactory
    )

    private companion object {
        const val MODEL_PATH = "/tmp/model.litertlm"
    }
}

private class FakeAiModelManager(
    private val modelStatus: AiModelStatus
) : AiModelManager {
    override fun observeModelStatus(): Flow<AiModelStatus> = flowOf(modelStatus)

    override fun downloadModel(): Flow<AiModelDownloadState> = flowOf(AiModelDownloadState.AuthRequired)
}

private class FakeLiteRtLmInferenceFactory(
    response: String,
    inferenceThrowable: Throwable?,
    private val factoryThrowable: Throwable?
) : LiteRtLmInferenceFactory {
    val inference = FakeLiteRtLmInference(response, inferenceThrowable)
    var createCount = 0
        private set
    var modelPath: String? = null
        private set

    override fun create(modelPath: String): LiteRtLmInference {
        createCount++
        this.modelPath = modelPath
        factoryThrowable?.let { throw it }
        return inference
    }
}

private class FakeLiteRtLmInference(
    private val response: String,
    private val throwable: Throwable?
) : LiteRtLmInference {
    var prompt: String = ""
        private set
    var isClosed = false
        private set

    override fun generateResponse(prompt: String): String {
        assertFalse(isClosed)
        this.prompt = prompt
        throwable?.let { throw it }
        return response
    }

    override fun close() {
        isClosed = true
    }
}
