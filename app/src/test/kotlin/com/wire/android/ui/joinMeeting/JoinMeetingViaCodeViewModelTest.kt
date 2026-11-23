/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.joinMeeting

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.meeting.JoinMeetingViaCodeUseCase
import com.wire.kalium.logic.feature.user.IsMeetingsEnabledUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class JoinMeetingViaCodeViewModelTest {
    private val account = UserId("self", "domain")
    private val link = MeetingJoinLink("code", "key", "domain", account)
    private val joinViaCode = mockk<JoinMeetingViaCodeUseCase>()
    private val isMeetingsEnabled = mockk<IsMeetingsEnabledUseCase>()

    private fun createViewModel(
        supported: Boolean = true,
        result: JoinMeetingViaCodeUseCase.Result = JoinMeetingViaCodeUseCase.Result.Success,
    ): JoinMeetingViaCodeViewModel {
        coEvery { isMeetingsEnabled.isGuestLinkSharingSupported() } returns supported
        coEvery { joinViaCode(any(), any(), any(), any()) } returns result
        return JoinMeetingViaCodeViewModel(joinViaCode, isMeetingsEnabled)
    }

    @Test
    fun givenSupportedLink_whenStarting_thenJoinAutomatically() = runTest {
        val viewModel = createViewModel()
        val response = CompletableDeferred<JoinMeetingViaCodeUseCase.Result>()
        coEvery { joinViaCode("domain", "key", "code", null) } coAnswers { response.await() }
        assertEquals(JoinMeetingDialogState.Idle, viewModel.state)
        val request = launch { viewModel.startJoining(link) }
        advanceUntilIdle()
        assertEquals(JoinMeetingDialogState.Loading, viewModel.state)
        response.complete(JoinMeetingViaCodeUseCase.Result.Success)
        request.join()
        advanceUntilIdle()

        assertEquals(JoinMeetingDialogState.Success, viewModel.state)
        coVerify(exactly = 1) { joinViaCode("domain", "key", "code", null) }
    }

    @Test
    fun givenUnsupportedGuestLinks_whenStarting_thenShowUnavailableWithoutJoining() = runTest {
        val viewModel = createViewModel(supported = false)
        viewModel.startJoining(link)
        advanceUntilIdle()

        assertEquals(JoinMeetingDialogState.Error.Unavailable, viewModel.state)
        coVerify(exactly = 0) { joinViaCode(any(), any(), any(), any()) }
    }

    @Test
    fun givenPasswordProtectedLink_whenJoining_thenPromptAndKeepInvalidPasswordUntilEdited() = runTest {
        val viewModel = createViewModel(result = JoinMeetingViaCodeUseCase.Result.InvalidPassword)
        viewModel.startJoining(link)
        advanceUntilIdle()
        assertEquals(JoinMeetingDialogState.PasswordRequired(), viewModel.state)

        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("wrong")
        advanceUntilIdle()
        val response = CompletableDeferred<JoinMeetingViaCodeUseCase.Result>()
        coEvery { joinViaCode("domain", "key", "code", "wrong") } coAnswers { response.await() }
        val request = launch { viewModel.submitPassword(link) }
        advanceUntilIdle()
        assertEquals(JoinMeetingDialogState.PasswordRequired(loading = true), viewModel.state)
        viewModel.submitPassword(link)
        response.complete(JoinMeetingViaCodeUseCase.Result.InvalidPassword)
        request.join()
        advanceUntilIdle()
        assertEquals(JoinMeetingDialogState.PasswordRequired(error = JoinMeetingDialogState.Error.InvalidPassword), viewModel.state)
        coVerify(exactly = 1) { joinViaCode("domain", "key", "code", "wrong") }

        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("correct")
        advanceUntilIdle()
        assertEquals(JoinMeetingDialogState.PasswordRequired(), viewModel.state)
        coEvery { joinViaCode("domain", "key", "code", "correct") } returns JoinMeetingViaCodeUseCase.Result.Success
        viewModel.submitPassword(link)
        advanceUntilIdle()
        assertEquals(JoinMeetingDialogState.Success, viewModel.state)
    }

    @Test
    fun givenEmptyPassword_whenSubmitting_thenDoNotSendAnotherRequest() = runTest {
        val viewModel = createViewModel(result = JoinMeetingViaCodeUseCase.Result.InvalidPassword)
        viewModel.startJoining(link)
        advanceUntilIdle()
        viewModel.submitPassword(link)
        advanceUntilIdle()

        coVerify(exactly = 1) { joinViaCode(any(), any(), any(), any()) }
    }

    @Test
    fun givenJoinFailure_whenStarting_thenShowSpecificError() = runTest {
        for ((result, status) in listOf(
            JoinMeetingViaCodeUseCase.Result.MeetingNotFound to JoinMeetingDialogState.Error.NotFound,
            JoinMeetingViaCodeUseCase.Result.LinkUnavailable to JoinMeetingDialogState.Error.Unavailable,
            JoinMeetingViaCodeUseCase.Result.Failure to JoinMeetingDialogState.Error.Generic,
        )) {
            val viewModel = createViewModel(result = result)
            viewModel.startJoining(link)
            advanceUntilIdle()
            assertEquals(status, viewModel.state)
        }
    }

    @Test
    fun givenPreviousPasswordPrompt_whenFlowClosedAndReopened_thenClearPasswordAndJoinAgain() = runTest {
        val viewModel = createViewModel(result = JoinMeetingViaCodeUseCase.Result.InvalidPassword)
        viewModel.startJoining(link)
        advanceUntilIdle()
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("previous password")
        advanceUntilIdle()

        viewModel.reset()
        assertEquals(JoinMeetingDialogState.Idle, viewModel.state)
        assertEquals("", viewModel.passwordTextState.text.toString())

        coEvery { joinViaCode("domain", "key", "code", null) } returns JoinMeetingViaCodeUseCase.Result.Success
        viewModel.startJoining(link)
        advanceUntilIdle()
        assertEquals(JoinMeetingDialogState.Success, viewModel.state)
        coVerify(exactly = 2) { joinViaCode("domain", "key", "code", null) }
    }

    @Test
    fun givenPendingRequest_whenDialogCoroutineCancelledAndSameLinkReopened_thenIgnorePreviousResponse() = runTest {
        val viewModel = createViewModel()
        val oldResponse = CompletableDeferred<JoinMeetingViaCodeUseCase.Result>()
        coEvery { joinViaCode("domain", "key", "code", null) } coAnswers { oldResponse.await() }
        val request = launch { viewModel.startJoining(link) }
        advanceUntilIdle()

        request.cancel()
        request.join()
        viewModel.reset()
        coEvery { joinViaCode("domain", "key", "code", null) } returns JoinMeetingViaCodeUseCase.Result.Success
        viewModel.startJoining(link)
        advanceUntilIdle()
        oldResponse.complete(JoinMeetingViaCodeUseCase.Result.InvalidPassword)
        advanceUntilIdle()

        assertEquals(JoinMeetingDialogState.Success, viewModel.state)
    }

    @Test
    fun givenPendingRequest_whenDialogCoroutineCancelledAndDifferentLinkOpened_thenJoinNewLink() = runTest {
        val viewModel = createViewModel()
        val oldResponse = CompletableDeferred<JoinMeetingViaCodeUseCase.Result>()
        coEvery { joinViaCode("domain", "key", "code", null) } coAnswers { oldResponse.await() }
        val request = launch { viewModel.startJoining(link) }
        advanceUntilIdle()

        request.cancel()
        request.join()
        viewModel.startJoining(link.copy(code = "new-code"))
        advanceUntilIdle()
        oldResponse.complete(JoinMeetingViaCodeUseCase.Result.InvalidPassword)
        advanceUntilIdle()

        assertEquals(JoinMeetingDialogState.Success, viewModel.state)
        coVerify(exactly = 1) { joinViaCode("domain", "key", "new-code", null) }
    }

    @Test
    fun givenInitialJoinError_whenRetrying_thenJoinWithoutPasswordAndReturnSuccess() = runTest {
        for (failure in listOf(
            JoinMeetingViaCodeUseCase.Result.Failure,
            JoinMeetingViaCodeUseCase.Result.MeetingNotFound,
            JoinMeetingViaCodeUseCase.Result.LinkUnavailable,
        )) {
            val viewModel = createViewModel(result = failure)
            viewModel.startJoining(link)
            coEvery { joinViaCode("domain", "key", "code", null) } returns JoinMeetingViaCodeUseCase.Result.Success

            viewModel.retryJoining(link)

            assertEquals(JoinMeetingDialogState.Success, viewModel.state)
            assertEquals("", viewModel.passwordTextState.text.toString())
        }
    }

    @Test
    fun givenFailedPasswordSubmission_whenRetrying_thenKeepPasswordDialogAndReusePassword() = runTest {
        val failures = listOf(
            JoinMeetingViaCodeUseCase.Result.Failure to JoinMeetingDialogState.Error.Generic,
            JoinMeetingViaCodeUseCase.Result.MeetingNotFound to JoinMeetingDialogState.Error.NotFound,
            JoinMeetingViaCodeUseCase.Result.LinkUnavailable to JoinMeetingDialogState.Error.Unavailable,
        )
        for ((failure, error) in failures) {
            val viewModel = createViewModel(result = JoinMeetingViaCodeUseCase.Result.InvalidPassword)
            viewModel.startJoining(link)
            viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("meeting password")
            advanceUntilIdle()
            coEvery { joinViaCode("domain", "key", "code", "meeting password") } returns failure

            viewModel.submitPassword(link)

            assertEquals(JoinMeetingDialogState.PasswordRequired(error = error), viewModel.state)
            assertEquals("meeting password", viewModel.passwordTextState.text.toString())
            coEvery { joinViaCode("domain", "key", "code", "meeting password") } returns JoinMeetingViaCodeUseCase.Result.Success
            viewModel.submitPassword(link)
            assertEquals(JoinMeetingDialogState.Success, viewModel.state)
        }
    }

    @Test
    fun givenPasswordSubmissionError_whenPasswordEdited_thenClearErrorAndAllowNewPassword() = runTest {
        val viewModel = createViewModel(result = JoinMeetingViaCodeUseCase.Result.InvalidPassword)
        viewModel.startJoining(link)
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("old password")
        advanceUntilIdle()
        coEvery { joinViaCode("domain", "key", "code", "old password") } returns JoinMeetingViaCodeUseCase.Result.Failure
        viewModel.submitPassword(link)

        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("new password")
        advanceUntilIdle()

        assertEquals(JoinMeetingDialogState.PasswordRequired(), viewModel.state)
        coEvery { joinViaCode("domain", "key", "code", "new password") } returns JoinMeetingViaCodeUseCase.Result.Success
        viewModel.submitPassword(link)
        assertEquals(JoinMeetingDialogState.Success, viewModel.state)
    }

    @Test
    fun givenInitialJoinError_whenRetryInProgress_thenIgnoreAnotherRetry() = runTest {
        val viewModel = createViewModel(result = JoinMeetingViaCodeUseCase.Result.Failure)
        viewModel.startJoining(link)
        val response = CompletableDeferred<JoinMeetingViaCodeUseCase.Result>()
        coEvery { joinViaCode("domain", "key", "code", null) } coAnswers { response.await() }
        val request = launch { viewModel.retryJoining(link) }
        advanceUntilIdle()

        viewModel.retryJoining(link)
        response.complete(JoinMeetingViaCodeUseCase.Result.Success)
        request.join()

        assertEquals(JoinMeetingDialogState.Success, viewModel.state)
        coVerify(exactly = 2) { joinViaCode("domain", "key", "code", null) }
    }
}
