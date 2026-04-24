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

import com.wire.android.feature.aiassistant.model.AiModelDescriptor
import com.wire.android.feature.aiassistant.model.AiPromptCapability
import com.wire.android.feature.aiassistant.model.AiModelDownloadState
import com.wire.android.feature.aiassistant.model.AiModelStatus
import com.wire.android.feature.aiassistant.test.LiteRtLmInference
import com.wire.android.feature.aiassistant.test.LiteRtLmInferenceFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
            .withSelectedModelCapability(AiPromptCapability.Weak)
            .withInferenceResponse("Hello,\nthis is a message.")
            .arrange()

        val result = arrangement.agent.proofread(inputText)

        assertEquals(AiMessageComposerResult.Success("Hello,\nthis is a message."), result)
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains(inputText))
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains("grammar"))
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains("one result only"))
        assertTrue(arrangement.inferenceFactory.initialExchanges.isNotEmpty())
        assertEquals(MODEL_PATH, arrangement.inferenceFactory.modelPath)
        assertTrue(arrangement.inferenceFactory.inference.isClosed)
    }

    @Test
    fun givenCapableModel_whenProofreadIsCalled_thenDetailedPromptIsSentWithoutWeakExamples() = runTest {
        val inputText = "Helo,\nthis is a mesage."
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withSelectedModelCapability(AiPromptCapability.Capable)
            .withInferenceResponse("Hello,\nthis is a message.")
            .arrange()

        val result = arrangement.agent.proofread(inputText)

        assertEquals(AiMessageComposerResult.Success("Hello,\nthis is a message."), result)
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains("Proofread the message below"))
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains("Fix grammar, spelling, punctuation"))
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains("Return exactly one rewritten message and nothing else"))
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains(inputText))
        assertTrue(arrangement.inferenceFactory.initialExchanges.isEmpty())
    }

    @Test
    fun givenInferenceReturnsTextWrappedInDoubleQuotes_whenProofreadIsCalled_thenQuotesAreStripped() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withInferenceResponse("\"Hello, this is a message.\"")
            .arrange()

        val result = arrangement.agent.proofread("Helo, this is a mesage.")

        assertEquals(AiMessageComposerResult.Success("Hello, this is a message."), result)
    }

    @Test
    fun givenInferenceReturnsTextWrappedInCurlyQuotes_whenProofreadIsCalled_thenQuotesAreStripped() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withInferenceResponse("\u201CHello, this is a message.\u201D")
            .arrange()

        val result = arrangement.agent.proofread("Helo, this is a mesage.")

        assertEquals(AiMessageComposerResult.Success("Hello, this is a message."), result)
    }

    @Test
    fun givenInferenceReturnsTextWrappedInSingleQuotes_whenProofreadIsCalled_thenQuotesAreStripped() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withInferenceResponse("'Hello, this is a message.'")
            .arrange()

        val result = arrangement.agent.proofread("Helo, this is a mesage.")

        assertEquals(AiMessageComposerResult.Success("Hello, this is a message."), result)
    }

    @Test
    fun givenInferenceReturnsTextWithInternalQuotes_whenProofreadIsCalled_thenInternalQuotesArePreserved() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withInferenceResponse("She said \"hello\" to me.")
            .arrange()

        val result = arrangement.agent.proofread("She said hello to me")

        assertEquals(AiMessageComposerResult.Success("She said \"hello\" to me."), result)
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

    @Test
    fun givenBlankInput_whenAdjustToneIsCalled_thenEmptyInputIsReturnedAndInferenceIsNotCreated() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withInferenceResponse("Hello")
            .arrange()

        val result = arrangement.agent.adjustTone("  ", AiMessageToneType.Formal)

        assertEquals(AiMessageComposerResult.EmptyInput, result)
        assertEquals(0, arrangement.inferenceFactory.createCount)
    }

    @Test
    fun givenModelIsNotDownloaded_whenAdjustToneIsCalled_thenMissingModelIsReturned() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.NotDownloaded)
            .withInferenceResponse("Hello")
            .arrange()

        val result = arrangement.agent.adjustTone("Hey", AiMessageToneType.Informal)

        assertEquals(AiMessageComposerResult.MissingModel, result)
        assertEquals(0, arrangement.inferenceFactory.createCount)
    }

    @Test
    fun givenModelIsDownloading_whenAdjustToneIsCalled_thenMissingModelIsReturned() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Downloading(progress = 0.5F))
            .withInferenceResponse("Hello")
            .arrange()

        val result = arrangement.agent.adjustTone("Hey", AiMessageToneType.Formal)

        assertEquals(AiMessageComposerResult.MissingModel, result)
        assertEquals(0, arrangement.inferenceFactory.createCount)
    }

    @Test
    fun givenReadyModelIsNotLiteRtLm_whenAdjustToneIsCalled_thenUnsupportedModelIsReturned() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready("/tmp/model.task"))
            .withInferenceResponse("Hello")
            .arrange()

        val result = arrangement.agent.adjustTone("Hey", AiMessageToneType.Informal)

        assertEquals(AiMessageComposerResult.UnsupportedModel, result)
        assertEquals(0, arrangement.inferenceFactory.createCount)
    }

    @Test
    fun givenReadyModel_whenAdjustToneFormalIsCalled_thenPromptIsSentAndUpdatedTextIsReturned() = runTest {
        val inputText = "Can you send it today?"
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withSelectedModelCapability(AiPromptCapability.Weak)
            .withInferenceResponse("Could you please send it today?")
            .arrange()

        val result = arrangement.agent.adjustTone(inputText, AiMessageToneType.Formal)

        assertEquals(AiMessageComposerResult.Success("Could you please send it today?"), result)
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains(inputText))
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains("formally"))
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains("one result only"))
        assertTrue(arrangement.inferenceFactory.initialExchanges.isNotEmpty())
        assertEquals(MODEL_PATH, arrangement.inferenceFactory.modelPath)
        assertTrue(arrangement.inferenceFactory.inference.isClosed)
    }

    @Test
    fun givenReadyModel_whenAdjustToneInformalIsCalled_thenPromptIsSentAndUpdatedTextIsReturned() = runTest {
        val inputText = "Could you please send it today?"
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withSelectedModelCapability(AiPromptCapability.Weak)
            .withInferenceResponse("Can you send it today?")
            .arrange()

        val result = arrangement.agent.adjustTone(inputText, AiMessageToneType.Informal)

        assertEquals(AiMessageComposerResult.Success("Can you send it today?"), result)
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains(inputText))
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains("casually"))
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains("one result only"))
        assertTrue(arrangement.inferenceFactory.initialExchanges.isNotEmpty())
        assertTrue(arrangement.inferenceFactory.inference.isClosed)
    }

    @Test
    fun givenCapableModel_whenAdjustToneFormalIsCalled_thenDetailedPromptIsSentWithoutWeakExamples() = runTest {
        val inputText = "Can you send it today?"
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withSelectedModelCapability(AiPromptCapability.Capable)
            .withInferenceResponse("Could you please send it today?")
            .arrange()

        val result = arrangement.agent.adjustTone(inputText, AiMessageToneType.Formal)

        assertEquals(AiMessageComposerResult.Success("Could you please send it today?"), result)
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains("more formal tone"))
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains("Preserve the original meaning, key details, and language"))
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains("Return exactly one rewritten message and nothing else"))
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains(inputText))
        assertTrue(arrangement.inferenceFactory.initialExchanges.isEmpty())
    }

    @Test
    fun givenCapableModel_whenAdjustToneInformalIsCalled_thenDetailedPromptIsSentWithoutWeakExamples() = runTest {
        val inputText = "Could you please send it today?"
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withSelectedModelCapability(AiPromptCapability.Capable)
            .withInferenceResponse("Can you send it today?")
            .arrange()

        val result = arrangement.agent.adjustTone(inputText, AiMessageToneType.Informal)

        assertEquals(AiMessageComposerResult.Success("Can you send it today?"), result)
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains("more casual tone"))
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains("Return exactly one rewritten message and nothing else"))
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains(inputText))
        assertTrue(arrangement.inferenceFactory.initialExchanges.isEmpty())
    }

    @Test
    fun givenInferenceReturnsBlankText_whenAdjustToneIsCalled_thenEmptyResponseIsReturned() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withInferenceResponse(" ")
            .arrange()

        val result = arrangement.agent.adjustTone("Hey", AiMessageToneType.Formal)

        assertEquals(AiMessageComposerResult.EmptyResponse, result)
        assertTrue(arrangement.inferenceFactory.inference.isClosed)
    }

    @Test
    fun givenInferenceFactoryThrows_whenAdjustToneIsCalled_thenInferenceFailedIsReturned() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withFactoryThrowable(IllegalStateException("Engine init failed"))
            .arrange()

        val result = arrangement.agent.adjustTone("Hey", AiMessageToneType.Informal)

        assertEquals(AiMessageComposerResult.InferenceFailed("Engine init failed"), result)
    }

    @Test
    fun givenInferenceThrows_whenAdjustToneIsCalled_thenInferenceFailedIsReturnedAndInferenceIsClosed() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withInferenceThrowable(IllegalStateException("Cannot run model"))
            .arrange()

        val result = arrangement.agent.adjustTone("Hey", AiMessageToneType.Formal)

        assertEquals(AiMessageComposerResult.InferenceFailed("Cannot run model"), result)
        assertTrue(arrangement.inferenceFactory.inference.isClosed)
    }

    @Test
    fun givenInferenceIsCancelled_whenAdjustToneIsCalled_thenCancellationIsRethrownAndInferenceIsClosed() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withInferenceThrowable(CancellationException("Cancelled"))
            .arrange()

        try {
            arrangement.agent.adjustTone("Hey", AiMessageToneType.Informal)
            fail("Expected adjustTone to rethrow cancellation")
        } catch (exception: CancellationException) {
            assertEquals("Cancelled", exception.message)
        }
        assertTrue(arrangement.inferenceFactory.inference.isClosed)
    }

    @Test
    fun givenBlankInput_whenCustomPromptIsCalled_thenEmptyInputIsReturnedAndInferenceIsNotCreated() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withInferenceResponse("Result")
            .arrange()

        val result = arrangement.agent.customPrompt("  ", "Make this shorter")

        assertEquals(AiMessageComposerResult.EmptyInput, result)
        assertEquals(0, arrangement.inferenceFactory.createCount)
    }

    @Test
    fun givenModelIsNotDownloaded_whenCustomPromptIsCalled_thenMissingModelIsReturned() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.NotDownloaded)
            .withInferenceResponse("Result")
            .arrange()

        val result = arrangement.agent.customPrompt("Hello", "Make this shorter")

        assertEquals(AiMessageComposerResult.MissingModel, result)
        assertEquals(0, arrangement.inferenceFactory.createCount)
    }

    @Test
    fun givenModelIsDownloading_whenCustomPromptIsCalled_thenMissingModelIsReturned() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Downloading(progress = 0.5F))
            .withInferenceResponse("Result")
            .arrange()

        val result = arrangement.agent.customPrompt("Hello", "Make this shorter")

        assertEquals(AiMessageComposerResult.MissingModel, result)
        assertEquals(0, arrangement.inferenceFactory.createCount)
    }

    @Test
    fun givenReadyModelIsNotLiteRtLm_whenCustomPromptIsCalled_thenUnsupportedModelIsReturned() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready("/tmp/model.task"))
            .withInferenceResponse("Result")
            .arrange()

        val result = arrangement.agent.customPrompt("Hello", "Make this shorter")

        assertEquals(AiMessageComposerResult.UnsupportedModel, result)
        assertEquals(0, arrangement.inferenceFactory.createCount)
    }

    @Test
    fun givenReadyModel_whenCustomPromptIsCalled_thenPromptContainsInputAndInstructionAndUpdatedTextIsReturned() = runTest {
        val inputText = "Hello, this is a long message about something."
        val userPrompt = "Make this shorter"
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withSelectedModelCapability(AiPromptCapability.Weak)
            .withInferenceResponse("Hello, short message.")
            .arrange()

        val result = arrangement.agent.customPrompt(inputText, userPrompt)

        assertEquals(AiMessageComposerResult.Success("Hello, short message."), result)
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains(inputText))
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains(userPrompt))
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains("one result only"))
        assertTrue(arrangement.inferenceFactory.initialExchanges.isEmpty())
        assertEquals(MODEL_PATH, arrangement.inferenceFactory.modelPath)
        assertTrue(arrangement.inferenceFactory.inference.isClosed)
    }

    @Test
    fun givenCapableModel_whenCustomPromptIsCalled_thenPromptContainsGuardrailsAndUpdatedTextIsReturned() = runTest {
        val inputText = "Hello, this is a long message about something."
        val userPrompt = "Make this shorter"
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withSelectedModelCapability(AiPromptCapability.Capable)
            .withInferenceResponse("Hello, short message.")
            .arrange()

        val result = arrangement.agent.customPrompt(inputText, userPrompt)

        assertEquals(AiMessageComposerResult.Success("Hello, short message."), result)
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains("Apply the following instruction to the message below"))
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains(userPrompt))
        assertTrue(
            arrangement.inferenceFactory.inference.userMessage.contains(
                "Preserve the original language unless the instruction explicitly asks to change it"
            )
        )
        assertTrue(
            arrangement.inferenceFactory.inference.userMessage.contains(
                "Return exactly one final rewritten message and nothing else"
            )
        )
        assertTrue(arrangement.inferenceFactory.inference.userMessage.contains(inputText))
        assertTrue(arrangement.inferenceFactory.initialExchanges.isEmpty())
    }

    @Test
    fun givenInferenceReturnsBlankText_whenCustomPromptIsCalled_thenEmptyResponseIsReturned() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withInferenceResponse(" ")
            .arrange()

        val result = arrangement.agent.customPrompt("Hello", "Make this shorter")

        assertEquals(AiMessageComposerResult.EmptyResponse, result)
        assertTrue(arrangement.inferenceFactory.inference.isClosed)
    }

    @Test
    fun givenInferenceFactoryThrows_whenCustomPromptIsCalled_thenInferenceFailedIsReturned() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withFactoryThrowable(IllegalStateException("Engine init failed"))
            .arrange()

        val result = arrangement.agent.customPrompt("Hello", "Make this shorter")

        assertEquals(AiMessageComposerResult.InferenceFailed("Engine init failed"), result)
    }

    @Test
    fun givenInferenceThrows_whenCustomPromptIsCalled_thenInferenceFailedIsReturnedAndInferenceIsClosed() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withInferenceThrowable(IllegalStateException("Cannot run model"))
            .arrange()

        val result = arrangement.agent.customPrompt("Hello", "Make this shorter")

        assertEquals(AiMessageComposerResult.InferenceFailed("Cannot run model"), result)
        assertTrue(arrangement.inferenceFactory.inference.isClosed)
    }

    @Test
    fun givenInferenceIsCancelled_whenCustomPromptIsCalled_thenCancellationIsRethrownAndInferenceIsClosed() = runTest {
        val arrangement = Arrangement()
            .withModelStatus(AiModelStatus.Ready(MODEL_PATH))
            .withInferenceThrowable(CancellationException("Cancelled"))
            .arrange()

        try {
            arrangement.agent.customPrompt("Hello", "Make this shorter")
            fail("Expected customPrompt to rethrow cancellation")
        } catch (exception: CancellationException) {
            assertEquals("Cancelled", exception.message)
        }
        assertTrue(arrangement.inferenceFactory.inference.isClosed)
    }

    private class Arrangement {
        private var modelStatus: AiModelStatus = AiModelStatus.NotDownloaded
        private var descriptor: AiModelDescriptor = testDescriptor()
        private var response: String = ""
        private var inferenceThrowable: Throwable? = null
        private var factoryThrowable: Throwable? = null

        fun withModelStatus(status: AiModelStatus) = apply {
            modelStatus = status
        }

        fun withSelectedModelCapability(promptCapability: AiPromptCapability) = apply {
            descriptor = testDescriptor(promptCapability)
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
                    aiModelManager = FakeAiModelManager(modelStatus, descriptor),
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
    private val modelStatus: AiModelStatus,
    private val descriptor: AiModelDescriptor
) : AiModelManager {
    override val availableModels: List<AiModelDescriptor> = listOf(descriptor)
    override val selectedModel: StateFlow<AiModelDescriptor> = MutableStateFlow(descriptor)
    override fun selectModel(descriptor: AiModelDescriptor) = Unit
    override fun observeModelStatus(): Flow<AiModelStatus> = flowOf(modelStatus)
    override fun downloadModel(): Flow<AiModelDownloadState> = flowOf(AiModelDownloadState.AuthRequired())
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
    var initialExchanges: List<Pair<String, String>> = emptyList()
        private set

    override fun create(modelPath: String, initialExchanges: List<Pair<String, String>>): LiteRtLmInference {
        createCount++
        this.modelPath = modelPath
        this.initialExchanges = initialExchanges
        factoryThrowable?.let { throw it }
        return inference
    }
}

private class FakeLiteRtLmInference(
    private val response: String,
    private val throwable: Throwable?
) : LiteRtLmInference {
    var userMessage: String = ""
        private set
    var isClosed = false
        private set

    override fun generateResponse(userMessage: String): String {
        assertFalse(isClosed)
        this.userMessage = userMessage
        throwable?.let { throw it }
        return response
    }

    override fun close() {
        isClosed = true
    }
}

private fun testDescriptor(promptCapability: AiPromptCapability = AiPromptCapability.Weak): AiModelDescriptor =
    AiModelDescriptor(
        displayName = "Test model",
        repositoryId = "test/model",
        artifactPath = "model.litertlm",
        localDirectoryName = "test-model",
        localFileName = "model.litertlm",
        promptCapability = promptCapability
    )
