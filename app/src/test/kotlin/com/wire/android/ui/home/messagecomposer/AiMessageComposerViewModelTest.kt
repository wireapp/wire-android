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
package com.wire.android.ui.home.messagecomposer

import app.cash.turbine.test
import com.wire.android.R
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.feature.aiassistant.AiMessageComposerAgent
import com.wire.android.feature.aiassistant.AiMessageComposerResult
import com.wire.android.feature.aiassistant.AiMessageToneType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class AiMessageComposerViewModelTest {

    @Test
    fun `given proofread succeeds when proofread is requested then replace text effect is emitted and exact input is used`() = runTest {
        val inputText = "Helo"
        val updatedText = "Hello"
        val (arrangement, viewModel) = Arrangement()
            .withProofreadResult(inputText, AiMessageComposerResult.Success(updatedText))
            .arrange()

        viewModel.effect.test {
            viewModel.proofread(inputText)

            assertEquals(AiMessageComposerEffect.ReplaceText(updatedText), awaitItem())
        }

        coVerify(exactly = 1) {
            arrangement.aiMessageComposerAgent.proofread(inputText)
        }
        assertNull(viewModel.activeAction)
    }

    @Test
    fun `given formal tone succeeds when tone adjustment is requested then replace text effect is emitted and exact input is used`() = runTest {
        val inputText = "Send this today."
        val updatedText = "Please send this today."
        val (arrangement, viewModel) = Arrangement()
            .withAdjustToneResult(inputText, AiMessageToneType.Formal, AiMessageComposerResult.Success(updatedText))
            .arrange()

        viewModel.effect.test {
            viewModel.adjustTone(inputText, AiMessageToneType.Formal)

            assertEquals(AiMessageComposerEffect.ReplaceText(updatedText), awaitItem())
        }

        coVerify(exactly = 1) {
            arrangement.aiMessageComposerAgent.adjustTone(inputText, AiMessageToneType.Formal)
        }
        assertNull(viewModel.activeAction)
    }

    @Test
    fun `given informal tone succeeds when tone adjustment is requested then replace text effect is emitted and exact input is used`() = runTest {
        val inputText = "Please send this today."
        val updatedText = "Can you send this today?"
        val (arrangement, viewModel) = Arrangement()
            .withAdjustToneResult(inputText, AiMessageToneType.Informal, AiMessageComposerResult.Success(updatedText))
            .arrange()

        viewModel.effect.test {
            viewModel.adjustTone(inputText, AiMessageToneType.Informal)

            assertEquals(AiMessageComposerEffect.ReplaceText(updatedText), awaitItem())
        }

        coVerify(exactly = 1) {
            arrangement.aiMessageComposerAgent.adjustTone(inputText, AiMessageToneType.Informal)
        }
        assertNull(viewModel.activeAction)
    }

    @Test
    fun `given empty input when proofread is requested then empty input error effect is emitted`() = runTest {
        assertErrorEffect(
            result = AiMessageComposerResult.EmptyInput,
            expectedEffect = AiMessageComposerEffect.ShowError(R.string.error_proofread_message_empty_input)
        )
    }

    @Test
    fun `given missing model when proofread is requested then missing model error effect is emitted`() = runTest {
        assertErrorEffect(
            result = AiMessageComposerResult.MissingModel,
            expectedEffect = AiMessageComposerEffect.ShowError(R.string.error_proofread_message_missing_model)
        )
    }

    @Test
    fun `given unsupported model when proofread is requested then unsupported model error effect is emitted`() = runTest {
        assertErrorEffect(
            result = AiMessageComposerResult.UnsupportedModel,
            expectedEffect = AiMessageComposerEffect.ShowError(R.string.error_proofread_message_unsupported_model)
        )
    }

    @Test
    fun `given empty response when proofread is requested then generic error effect is emitted`() = runTest {
        assertErrorEffect(
            result = AiMessageComposerResult.EmptyResponse,
            expectedEffect = AiMessageComposerEffect.ShowError(R.string.error_proofread_message_generic)
        )
    }

    @Test
    fun `given inference failure when proofread is requested then generic error effect is emitted`() = runTest {
        assertErrorEffect(
            result = AiMessageComposerResult.InferenceFailed("Cannot run model"),
            expectedEffect = AiMessageComposerEffect.ShowError(R.string.error_proofread_message_generic)
        )
    }

    @Test
    fun `given empty input when tone adjustment is requested then empty input error effect is emitted`() = runTest {
        assertToneErrorEffect(
            result = AiMessageComposerResult.EmptyInput,
            expectedEffect = AiMessageComposerEffect.ShowError(R.string.error_adjust_tone_message_empty_input)
        )
    }

    @Test
    fun `given missing model when tone adjustment is requested then missing model error effect is emitted`() = runTest {
        assertToneErrorEffect(
            result = AiMessageComposerResult.MissingModel,
            expectedEffect = AiMessageComposerEffect.ShowError(R.string.error_adjust_tone_message_missing_model)
        )
    }

    @Test
    fun `given unsupported model when tone adjustment is requested then unsupported model error effect is emitted`() = runTest {
        assertToneErrorEffect(
            result = AiMessageComposerResult.UnsupportedModel,
            expectedEffect = AiMessageComposerEffect.ShowError(R.string.error_adjust_tone_message_unsupported_model)
        )
    }

    @Test
    fun `given empty response when tone adjustment is requested then generic error effect is emitted`() = runTest {
        assertToneErrorEffect(
            result = AiMessageComposerResult.EmptyResponse,
            expectedEffect = AiMessageComposerEffect.ShowError(R.string.error_adjust_tone_message_generic)
        )
    }

    @Test
    fun `given inference failure when tone adjustment is requested then generic error effect is emitted`() = runTest {
        assertToneErrorEffect(
            result = AiMessageComposerResult.InferenceFailed("Cannot run model"),
            expectedEffect = AiMessageComposerEffect.ShowError(R.string.error_adjust_tone_message_generic)
        )
    }

    @Test
    fun `given ai action is running when another action is requested then second request is ignored`() = runTest {
        val proofreadStarted = CompletableDeferred<Unit>()
        val completeProofread = CompletableDeferred<AiMessageComposerResult>()
        val (arrangement, viewModel) = Arrangement()
            .withSuspendedProofread(
                inputText = FIRST_INPUT,
                proofreadStarted = proofreadStarted,
                completeProofread = completeProofread
            )
            .arrange()

        viewModel.proofread(FIRST_INPUT)
        proofreadStarted.await()

        assertEquals(AiMessageComposerAction.Proofread, viewModel.activeAction)

        viewModel.adjustTone(SECOND_INPUT, AiMessageToneType.Formal)
        completeProofread.complete(AiMessageComposerResult.Success("Hello"))
        advanceUntilIdle()

        coVerify(exactly = 1) {
            arrangement.aiMessageComposerAgent.proofread(FIRST_INPUT)
        }
        coVerify(exactly = 0) {
            arrangement.aiMessageComposerAgent.adjustTone(SECOND_INPUT, AiMessageToneType.Formal)
        }
        assertNull(viewModel.activeAction)
    }

    private suspend fun assertErrorEffect(
        result: AiMessageComposerResult,
        expectedEffect: AiMessageComposerEffect.ShowError
    ) {
        val inputText = "Helo"
        val (_, viewModel) = Arrangement()
            .withProofreadResult(inputText, result)
            .arrange()

        viewModel.effect.test {
            viewModel.proofread(inputText)

            assertEquals(expectedEffect, awaitItem())
        }
    }

    private suspend fun assertToneErrorEffect(
        result: AiMessageComposerResult,
        expectedEffect: AiMessageComposerEffect.ShowError
    ) {
        val inputText = "Hey"
        val (_, viewModel) = Arrangement()
            .withAdjustToneResult(inputText, AiMessageToneType.Formal, result)
            .arrange()

        viewModel.effect.test {
            viewModel.adjustTone(inputText, AiMessageToneType.Formal)

            assertEquals(expectedEffect, awaitItem())
        }
    }

    private class Arrangement {

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        @MockK
        lateinit var aiMessageComposerAgent: AiMessageComposerAgent

        private val viewModel by lazy {
            AiMessageComposerViewModel(aiMessageComposerAgent)
        }

        fun withProofreadResult(inputText: String, result: AiMessageComposerResult) = apply {
            coEvery { aiMessageComposerAgent.proofread(inputText) } returns result
        }

        fun withAdjustToneResult(
            inputText: String,
            toneType: AiMessageToneType,
            result: AiMessageComposerResult
        ) = apply {
            coEvery { aiMessageComposerAgent.adjustTone(inputText, toneType) } returns result
        }

        fun withSuspendedProofread(
            inputText: String,
            proofreadStarted: CompletableDeferred<Unit>,
            completeProofread: CompletableDeferred<AiMessageComposerResult>
        ) = apply {
            coEvery { aiMessageComposerAgent.proofread(inputText) } coAnswers {
                proofreadStarted.complete(Unit)
                completeProofread.await()
            }
        }

        fun arrange() = this to viewModel
    }

    private companion object {
        const val FIRST_INPUT = "Helo"
        const val SECOND_INPUT = "Bonjour"
    }
}
