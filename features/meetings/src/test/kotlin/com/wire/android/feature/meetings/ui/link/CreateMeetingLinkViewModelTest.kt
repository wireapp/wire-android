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
package com.wire.android.feature.meetings.ui.link

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.wire.android.config.SnapshotExtension
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.MeetingId
import com.wire.kalium.logic.data.meeting.Meeting
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.ValidatePasswordResult
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.meeting.ObserveMeetingUseCase
import com.wire.kalium.logic.feature.meeting.RotateMeetingLinkUseCase
import com.wire.kalium.logic.util.RandomPassword
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(SnapshotExtension::class)
class CreateMeetingLinkViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun givenRotationFails_whenResubmitting_thenPasswordIsPreservedAndUserCanRetry() = runTest(dispatcher) {
        val result = CompletableDeferred<RotateMeetingLinkUseCase.Result>()
        val (arrangement, viewModel) = Arrangement()
            .withObservedMeeting(MEETING)
            .withPendingRotation(result)
            .arrange()
        runCurrent()
        val password = "ValidPassword1!"
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd(password)
        viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd(password)
        runCurrent()

        viewModel.actions.test {
            viewModel.onCreate()
            assertEquals(true, (viewModel.state as CreateMeetingLinkState.Ready).isSubmitting)
            viewModel.onCreate()
            runCurrent()
            coVerify(exactly = 1) { arrangement.rotateMeetingLink(MEETING_ID, password) }
            result.complete(RotateMeetingLinkUseCase.Result.Failure(NetworkFailure.NoNetworkConnection(null)))
            runCurrent()
            val failedState = viewModel.state as CreateMeetingLinkState.Ready
            assertEquals(false, failedState.isSubmitting)
            assertEquals(true, failedState.submitError)
            assertEquals(MEETING.link, failedState.meetingLink.url)
            assertEquals(password, viewModel.passwordTextState.text.toString())
            assertEquals(password, viewModel.confirmPasswordTextState.text.toString())
            expectNoEvents()

            viewModel.onDismissError()
            assertEquals(false, (viewModel.state as CreateMeetingLinkState.Ready).submitError)
            arrangement.withRotationResult(RotateMeetingLinkUseCase.Result.Success(MEETING))
            viewModel.onCreate()
            runCurrent()
            assertEquals(CreateMeetingLinkViewAction.Success, awaitItem())
            coVerify(exactly = 2) { arrangement.rotateMeetingLink(MEETING_ID, password) }
        }
    }

    private class Arrangement {
        private val observeMeeting = mockk<ObserveMeetingUseCase>()
        val rotateMeetingLink = mockk<RotateMeetingLinkUseCase>()
        private val validatePassword = mockk<ValidatePasswordUseCase>()
        private val generatePassword = mockk<RandomPassword>()

        init {
            every { validatePassword(any()) } returns ValidatePasswordResult.Valid
        }

        fun withObservedMeeting(meeting: Meeting?) = apply {
            coEvery { observeMeeting(MEETING_ID) } returns flowOf(meeting)
        }

        fun withPendingRotation(result: Deferred<RotateMeetingLinkUseCase.Result>) = apply {
            coEvery { rotateMeetingLink(MEETING_ID, any()) } coAnswers { result.await() }
        }

        fun withRotationResult(result: RotateMeetingLinkUseCase.Result) = apply {
            coEvery { rotateMeetingLink(MEETING_ID, any()) } returns result
        }

        fun arrange() = this to CreateMeetingLinkViewModelImpl(
            meetingId = MEETING_ID,
            observeMeeting = observeMeeting,
            rotateMeetingLink = rotateMeetingLink,
            validatePassword = validatePassword,
            generatePassword = generatePassword,
            selfUserId = SELF_USER_ID,
        )
    }

    private companion object {
        val MEETING_ID = MeetingId("meeting-id", "domain")
        val SELF_USER_ID = UserId("self", "domain")
        val MEETING = Meeting(
            meetingId = MEETING_ID,
            conversationId = ConversationId("conversation-id", "domain"),
            creatorId = SELF_USER_ID,
            title = "Weekly sync",
            startTime = Instant.parse("2026-11-26T12:00:00Z"),
            endTime = Instant.parse("2026-11-26T13:00:00Z"),
            recurrence = null,
            tzid = "UTC",
            link = "https://wire.com/meeting/old",
            hasPassword = true,
        )
    }
}
