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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.home.messagecomposer.poll

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.android.R
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.framework.TestConversation
import com.wire.android.model.DefaultSnackBarMessage
import com.wire.android.util.ui.UIText
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.feature.message.MessageOperationResult
import com.wire.kalium.logic.feature.message.poll.SendPollMessageUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class, NavigationTestExtension::class, SnapshotExtension::class)
class CreatePollViewModelTest {

    @Test
    fun givenInitialState_thenSendShouldBeDisabled() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        assertFalse(viewModel.state.canSend)
        assertEquals(CreatePollViewModel.MIN_OPTIONS, viewModel.optionTextStates.size)
    }

    @Test
    fun givenQuestionAndTwoOptions_whenTextChanges_thenSendShouldBeEnabled() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()

        arrangement.updateQuestion("Where should we eat?")
        arrangement.updateOption(0, "Pizza")
        arrangement.updateOption(1, "Sushi")
        advanceUntilIdle()

        assertTrue(viewModel.state.canSend)
    }

    @Test
    fun givenOnlyOneNonBlankOption_whenTextChanges_thenSendShouldRemainDisabled() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()

        arrangement.updateQuestion("Where should we eat?")
        arrangement.updateOption(0, "Pizza")
        arrangement.updateOption(1, " ")
        advanceUntilIdle()

        assertFalse(viewModel.state.canSend)
    }

    @Test
    fun whenAddingAndRemovingOptions_thenOptionsShouldNotGoBelowMinimum() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        viewModel.addOption()
        assertEquals(3, viewModel.optionTextStates.size)

        viewModel.removeOption(2)
        viewModel.removeOption(1)
        assertEquals(CreatePollViewModel.MIN_OPTIONS, viewModel.optionTextStates.size)
    }

    @Test
    fun whenPollSendingSucceeds_thenNavigateBackActionShouldBeEmittedAndValuesShouldBeTrimmed() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withSendPollMessageResult(MessageOperationResult.Success)
            .arrange()
        arrangement.updateQuestion(" Lunch? ")
        arrangement.updateOption(0, " Pizza ")
        arrangement.updateOption(1, " Sushi ")
        viewModel.addOption()
        arrangement.updateOption(2, " ")
        viewModel.setAllowMultipleAnswers(true)
        viewModel.setHideVoterNames(true)
        advanceUntilIdle()

        viewModel.actions.test {
            viewModel.sendPoll()

            assertEquals(CreatePollAction.NavigateBack, awaitItem())
        }

        coVerify {
            arrangement.sendPollMessage(
                conversationId = TestConversation.ID,
                question = "Lunch?",
                options = listOf("Pizza", "Sushi"),
                allowMultipleAnswers = true,
                hideVoterNames = true
            )
        }
    }

    @Test
    fun whenPollSendingFails_thenSnackbarShouldBeEmittedAndSendShouldBeEnabledAgain() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withSendPollMessageResult(MessageOperationResult.Failure(CoreFailure.Unknown(null)))
            .arrange()
        arrangement.updateQuestion("Lunch?")
        arrangement.updateOption(0, "Pizza")
        arrangement.updateOption(1, "Sushi")
        advanceUntilIdle()

        viewModel.infoMessage.test {
            viewModel.sendPoll()

            assertEquals(
                DefaultSnackBarMessage(UIText.StringResource(R.string.create_poll_error_generic)),
                awaitItem()
            )
        }
        assertFalse(viewModel.state.isSending)
        assertTrue(viewModel.state.canSend)
    }

    private class Arrangement {

        @MockK
        lateinit var sendPollMessage: SendPollMessageUseCase

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        private lateinit var viewModel: CreatePollViewModel

        init {
            MockKAnnotations.init(this)
            every { savedStateHandle.navArgs<CreatePollNavArgs>() } returns CreatePollNavArgs(TestConversation.ID)
            coEvery { sendPollMessage(any(), any(), any(), any(), any()) } returns MessageOperationResult.Success
        }

        fun withSendPollMessageResult(result: MessageOperationResult) = apply {
            coEvery { sendPollMessage(any(), any(), any(), any(), any()) } returns result
        }

        fun updateQuestion(text: String) {
            viewModel.questionTextState.setTextAndPlaceCursorAtEnd(text)
        }

        fun updateOption(index: Int, text: String) {
            viewModel.optionTextStates[index].setTextAndPlaceCursorAtEnd(text)
        }

        fun arrange(): Pair<Arrangement, CreatePollViewModel> {
            viewModel = CreatePollViewModel(
                savedStateHandle = savedStateHandle,
                sendPollMessage = sendPollMessage,
            )
            return this to viewModel
        }
    }
}
