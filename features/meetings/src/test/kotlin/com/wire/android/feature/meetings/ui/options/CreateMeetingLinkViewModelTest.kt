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
package com.wire.android.feature.meetings.ui.options

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.wire.android.feature.meetings.ui.link.CreateMeetingLinkViewAction
import kotlinx.coroutines.flow.first
import com.wire.android.feature.meetings.model.MeetingLink
import com.wire.android.feature.meetings.ui.link.MeetingLinkState
import com.wire.android.feature.meetings.ui.link.MeetingLinkViewModelImpl
import com.wire.android.feature.meetings.ui.link.CreateMeetingLinkState
import com.wire.android.feature.meetings.ui.link.CreateMeetingLinkViewModelImpl
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.id.MeetingId
import com.wire.kalium.logic.data.meeting.Meeting
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.ValidatePasswordResult
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.meeting.ObserveMeetingUseCase
import com.wire.kalium.logic.feature.meeting.RotateMeetingLinkUseCase
import com.wire.kalium.logic.util.RandomPassword
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateMeetingLinkViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun givenMeetingWithoutUrl_thenConfirmationStillHasReadyState() = runTest(dispatcher) {
        val (_, viewModel) = Arrangement().withObservedMeeting(meeting(null, false)).arrange()
        runCurrent()
        assertEquals(CreateMeetingLinkState.Ready(MeetingLink(null)), viewModel.state)
    }

    @Test
    fun givenMeetingUpdates_thenStateUsesLatestValues() = runTest(dispatcher) {
        val (arrangement, viewModel) = Arrangement()
            .withObservedMeeting(meeting("first", false))
            .arrange()
        assertEquals(CreateMeetingLinkState.Loading, viewModel.state)
        runCurrent()
        assertEquals(CreateMeetingLinkState.Ready(MeetingLink("first", false)), viewModel.state)
        arrangement.withObservedMeeting(meeting("updated", true))
        runCurrent()
        assertEquals(CreateMeetingLinkState.Ready(MeetingLink("updated", true)), viewModel.state)
    }

    @Test
    fun givenMeetingDeletedOrCreatorAccessLost_thenStateIsNotAvailable() = runTest(dispatcher) {
        val (arrangement, viewModel) = Arrangement()
            .withObservedMeeting(meeting("link", false))
            .arrange()
        runCurrent()
        arrangement.withObservedMeeting(null)
        runCurrent()
        assertEquals(CreateMeetingLinkState.NotAvailable, viewModel.state)
        arrangement.withObservedMeeting(meeting("link", false))
        runCurrent()
        assertEquals(CreateMeetingLinkState.Ready(MeetingLink("link", false)), viewModel.state)
        arrangement.withObservedMeeting(meeting("link", false, UserId("other", "wire.test")))
        runCurrent()
        assertEquals(CreateMeetingLinkState.NotAvailable, viewModel.state)
    }

    @Test
    fun givenProtectedLinkAndNoPassword_whenCreating_thenRequiresConfirmation() = runTest(dispatcher) {
        val (arrangement, viewModel) = Arrangement().withObservedMeeting(meeting("old", true)).arrange()
        runCurrent()
        viewModel.onCreate()
        runCurrent()
        assertTrue((viewModel.state as CreateMeetingLinkState.Ready).confirmWithoutPassword)
        coVerify(exactly = 0) { arrangement.rotateMeetingLink(any(), any()) }
        viewModel.onDismissConfirmation()
        assertFalse((viewModel.state as CreateMeetingLinkState.Ready).confirmWithoutPassword)
        viewModel.onCreate()
        viewModel.onCreate(confirmWithoutPassword = true)
        runCurrent()
        coVerify(exactly = 1) { arrangement.rotateMeetingLink(MEETING_ID, null) }
        assertEquals(CreateMeetingLinkViewAction.Success, viewModel.actions.first())
    }

    @Test
    fun givenPassword_whenCreating_thenValidatesAndSubmitsPassword() = runTest(dispatcher) {
        val (arrangement, viewModel) = Arrangement().withObservedMeeting(meeting("old", true)).arrange()
        runCurrent()
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("Valid123!")
        viewModel.onCreate()
        assertTrue((viewModel.state as CreateMeetingLinkState.Ready).passwordsDoNotMatch)
        coVerify(exactly = 0) { arrangement.rotateMeetingLink(any(), any()) }
        viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("Valid123!")
        viewModel.onCreate()
        runCurrent()
        coVerify(exactly = 1) { arrangement.rotateMeetingLink(MEETING_ID, "Valid123!") }
    }

    @Test
    fun givenInvalidPassword_whenCreating_thenDoesNotRefresh() = runTest(dispatcher) {
        val (arrangement, viewModel) = Arrangement().withObservedMeeting(meeting("old", false)).arrange()
        every { arrangement.validatePassword("weak") } returns ValidatePasswordResult.Invalid()
        runCurrent()
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("weak")
        viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("weak")
        viewModel.onCreate()
        assertTrue((viewModel.state as CreateMeetingLinkState.Ready).invalidPassword)
        coVerify(exactly = 0) { arrangement.rotateMeetingLink(any(), any()) }
    }

    @Test
    fun givenRequestInProgress_whenCreatingAgain_thenOnlyOneRequestAndFailureAllowsRetry() = runTest(dispatcher) {
        val (arrangement, viewModel) = Arrangement().withObservedMeeting(meeting("old", false)).arrange()
        val response = CompletableDeferred<RotateMeetingLinkUseCase.Result>()
        coEvery { arrangement.rotateMeetingLink(any(), any()) } coAnswers { response.await() }
        runCurrent()
        viewModel.onCreate()
        viewModel.onCreate()
        runCurrent()
        assertTrue((viewModel.state as CreateMeetingLinkState.Ready).isSubmitting)
        coVerify(exactly = 1) { arrangement.rotateMeetingLink(MEETING_ID, null) }
        response.complete(RotateMeetingLinkUseCase.Result.Failure(CoreFailure.Unknown(IllegalStateException("rotation failed"))))
        runCurrent()
        assertFalse((viewModel.state as CreateMeetingLinkState.Ready).isSubmitting)
        assertTrue((viewModel.state as CreateMeetingLinkState.Ready).submitError)
        viewModel.actions.test {
            expectNoEvents()
        }
        viewModel.onDismissError()
        assertFalse((viewModel.state as CreateMeetingLinkState.Ready).submitError)
        coEvery { arrangement.rotateMeetingLink(any(), any()) } returns
            RotateMeetingLinkUseCase.Result.Success(meeting("new", false))
        viewModel.onCreate()
        runCurrent()
        assertEquals(CreateMeetingLinkViewAction.Success, viewModel.actions.first())
        coVerify(exactly = 2) { arrangement.rotateMeetingLink(MEETING_ID, null) }
    }

    @Test
    fun givenCreating_whenMeetingUpdates_thenDraftRemainsAndUsesLatestPasswordStatus() = runTest(dispatcher) {
        val (arrangement, viewModel) = Arrangement().withObservedMeeting(meeting("old", false)).arrange()
        runCurrent()
        viewModel.onGenerateRandomPassword()
        assertEquals("Abc123!@", viewModel.passwordTextState.text.toString())
        assertEquals(viewModel.passwordTextState.text.toString(), viewModel.confirmPasswordTextState.text.toString())
        arrangement.withObservedMeeting(meeting("updated", true))
        runCurrent()
        assertEquals(CreateMeetingLinkState.Ready(MeetingLink("updated", true)), viewModel.state)
        assertEquals("Abc123!@", viewModel.passwordTextState.text.toString())
    }

    @Test
    fun givenRotationSucceeds_whenStoredMeetingUpdates_thenLinkScreenUpdatesAndCreatedActionIsSentOnce() = runTest(dispatcher) {
        val (arrangement, viewModel) = Arrangement().withObservedMeeting(meeting("old", false)).arrange()
        val linkViewModel = MeetingLinkViewModelImpl(MEETING_ID, arrangement.observeMeetingUseCase, SELF_ID)
        val updatedMeeting = meeting("new", true)
        coEvery { arrangement.rotateMeetingLink(MEETING_ID, null) } coAnswers {
            arrangement.withObservedMeeting(updatedMeeting)
            RotateMeetingLinkUseCase.Result.Success(updatedMeeting)
        }
        runCurrent()

        viewModel.onCreate()
        runCurrent()

        viewModel.actions.test {
            assertEquals(CreateMeetingLinkViewAction.Success, awaitItem())
            assertEquals(MeetingLinkState.Ready(MeetingLink("new", true)), linkViewModel.state)
            arrangement.withObservedMeeting(meeting("latest", false))
            runCurrent()
            assertEquals(MeetingLinkState.Ready(MeetingLink("latest", false)), linkViewModel.state)
            viewModel.onCreate()
            runCurrent()
            coVerify(exactly = 1) { arrangement.rotateMeetingLink(MEETING_ID, null) }
            expectNoEvents()
        }
    }

    private fun meeting(
        link: String?,
        hasPassword: Boolean,
        creator: UserId = SELF_ID,
    ): Meeting = mockk {
        every { creatorId } returns creator
        every { this@mockk.link } returns link
        every { this@mockk.hasPassword } returns hasPassword
    }

    private class Arrangement {
        @MockK
        lateinit var observeMeetingUseCase: ObserveMeetingUseCase

        val rotateMeetingLink = mockk<RotateMeetingLinkUseCase>()
        val validatePassword = mockk<ValidatePasswordUseCase>()
        val generatePassword = mockk<RandomPassword>()

        private val observedMeeting = MutableStateFlow<Meeting?>(null)

        init {
            MockKAnnotations.init(this)
            every { validatePassword(any()) } returns ValidatePasswordResult.Valid
            every { generatePassword(any(), any()) } returns "Abc123!@"
            coEvery { rotateMeetingLink(any(), any()) } returns RotateMeetingLinkUseCase.Result.Success(
                mockk {
                    every { link } returns "new"
                    every { hasPassword } returns false
                }
            )
            coEvery { observeMeetingUseCase.invoke(MEETING_ID) } returns observedMeeting
        }

        fun withObservedMeeting(meeting: Meeting?) = apply {
            observedMeeting.value = meeting
        }

        fun arrange() = this to CreateMeetingLinkViewModelImpl(
            meetingId = MEETING_ID,
            observeMeeting = observeMeetingUseCase,
            selfUserId = SELF_ID,
            rotateMeetingLink = rotateMeetingLink,
            validatePassword = validatePassword,
            generatePassword = generatePassword,
        )
    }

    private companion object {
        val MEETING_ID = MeetingId("meeting-id", "wire.test")
        val SELF_ID = UserId("self", "wire.test")
    }
}
