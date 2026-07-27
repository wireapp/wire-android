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

import app.cash.turbine.test
import com.wire.android.feature.meetings.R
import com.wire.android.model.asSnackBarMessage
import com.wire.android.util.ui.UIText
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.MeetingId
import com.wire.kalium.logic.data.meeting.MeetingOccurrence
import com.wire.kalium.logic.feature.meeting.DeleteMeetingUseCase
import com.wire.kalium.logic.feature.meeting.ObserveMeetingOccurrenceUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class MeetingOptionsMenuViewModelTest {
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
    fun givenFutureMeeting_andSelfUserIsCreator_whenObserving_thenDeleteForEveryoneIsAvailable() = runTest(dispatcher) {
        val meeting = meeting(selfRole = MeetingOccurrence.SelfRole.Creator, occurrenceStartTime = Clock.System.now() + 1.hours)
        val (_, viewModel) = Arrangement()
            .withObservedMeeting(meeting)
            .arrange()

        viewModel.observeMeetingStateFlow(OCCURRENCE_ID).test {
            assertEquals(MeetingOptionsMenuState.Loading, awaitItem())
            runCurrent()

            assertInstanceOf<MeetingOptionsMenuState.Meeting>(awaitItem()).also {
                assertEquals(MeetingOptionsMenuState.Meeting.DeleteOption.ForEveryone, it.deleteOption)
            }
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun givenFutureMeeting_andSelfUserIsMember_whenObserving_thenDeleteForMeIsAvailable() = runTest(dispatcher) {
        val meeting = meeting(selfRole = MeetingOccurrence.SelfRole.Member, occurrenceStartTime = Clock.System.now() + 1.hours)
        val (_, viewModel) = Arrangement()
            .withObservedMeeting(meeting)
            .arrange()

        viewModel.observeMeetingStateFlow(OCCURRENCE_ID).test {
            assertEquals(MeetingOptionsMenuState.Loading, awaitItem())
            runCurrent()

            assertInstanceOf<MeetingOptionsMenuState.Meeting>(awaitItem()).also {
                assertEquals(MeetingOptionsMenuState.Meeting.DeleteOption.ForMe, it.deleteOption)
            }
            cancelAndConsumeRemainingEvents()
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun givenPastMeeting_whenObserving_thenDeleteIsNotAvailable() = runTest(dispatcher) {
        val meeting = meeting(selfRole = MeetingOccurrence.SelfRole.Creator, occurrenceStartTime = Clock.System.now() - 1.hours)
        val (_, viewModel) = Arrangement()
            .withObservedMeeting(meeting)
            .arrange()

        viewModel.observeMeetingStateFlow(OCCURRENCE_ID).test {
            assertEquals(MeetingOptionsMenuState.Loading, awaitItem())
            runCurrent()

            assertInstanceOf<MeetingOptionsMenuState.Meeting>(awaitItem()).also {
                assertEquals(MeetingOptionsMenuState.Meeting.DeleteOption.None, it.deleteOption)
            }
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun givenSuccess_whenDeletingMeeting_thenSuccessMessageIsSentAndDialogIsDismissed() = runTest(dispatcher) {
        val (arrangement, viewModel) = Arrangement()
            .withDeleteMeetingResult(DeleteMeetingUseCase.Result.Success)
            .arrange()
        viewModel.deleteMeetingForEveryoneDialogState.show(DeleteMeetingDialogState(true, MEETING_ID, MEETING_TITLE))

        viewModel.actions.test {
            viewModel.deleteMeeting(MEETING_ID, MEETING_TITLE)
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.deleteMeetingUseCase.invoke(MEETING_ID) }
            assertFalse(viewModel.deleteMeetingForEveryoneDialogState.isVisible)
            assertEquals(
                MeetingOptionsMenuViewAction.Message(
                    UIText.StringResource(R.string.meeting_deleted_success, MEETING_TITLE).asSnackBarMessage()
                ),
                awaitItem()
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun givenFailure_whenDeletingMeeting_thenFailureMessageIsSentAndDialogIsDismissed() = runTest(dispatcher) {
        val (arrangement, viewModel) = Arrangement()
            .withDeleteMeetingResult(DeleteMeetingUseCase.Result.Failure(CoreFailure.Unknown(RuntimeException("delete failed"))))
            .arrange()
        viewModel.deleteMeetingForEveryoneDialogState.show(DeleteMeetingDialogState(true, MEETING_ID, MEETING_TITLE))

        viewModel.actions.test {
            viewModel.deleteMeeting(MEETING_ID, MEETING_TITLE)
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.deleteMeetingUseCase.invoke(MEETING_ID) }
            assertFalse(viewModel.deleteMeetingForEveryoneDialogState.isVisible)
            assertEquals(
                MeetingOptionsMenuViewAction.Message(
                    UIText.StringResource(R.string.meeting_deleted_failure, MEETING_TITLE).asSnackBarMessage()
                ),
                awaitItem()
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    private fun meeting(
        selfRole: MeetingOccurrence.SelfRole,
        occurrenceStartTime: Instant,
    ) = MeetingOccurrence(
        occurrenceId = OCCURRENCE_ID,
        meetingId = MEETING_ID,
        conversationId = CONVERSATION_ID,
        conversationName = "Meeting conversation",
        conversationType = MeetingOccurrence.ConversationType.Group,
        title = MEETING_TITLE,
        startTime = occurrenceStartTime,
        endTime = occurrenceStartTime + 30.minutes,
        occurrenceStartTime = occurrenceStartTime,
        occurrenceEndTime = occurrenceStartTime + 30.minutes,
        recurrence = null,
        selfRole = selfRole,
    )

    private class Arrangement {
        @MockK
        lateinit var observeMeetingOccurrenceUseCase: ObserveMeetingOccurrenceUseCase

        @MockK
        lateinit var deleteMeetingUseCase: DeleteMeetingUseCase

        init {
            MockKAnnotations.init(this)
            coEvery { observeMeetingOccurrenceUseCase.invoke(OCCURRENCE_ID) } returns flowOf(null)
            coEvery { deleteMeetingUseCase.invoke(MEETING_ID) } returns DeleteMeetingUseCase.Result.Success
        }
        fun withObservedMeeting(meeting: MeetingOccurrence?) = apply {
            coEvery { observeMeetingOccurrenceUseCase.invoke(OCCURRENCE_ID) } returns flowOf(meeting)
        }
        fun withDeleteMeetingResult(result: DeleteMeetingUseCase.Result) = apply {
            coEvery { deleteMeetingUseCase.invoke(MEETING_ID) } returns result
        }
        fun arrange() = this to MeetingOptionsMenuViewModelImpl(
            observeMeetingOccurrenceUseCase = observeMeetingOccurrenceUseCase,
            deleteMeetingUseCase = deleteMeetingUseCase,
        )
    }

    private companion object {
        const val OCCURRENCE_ID = "occurrence-id"
        const val MEETING_TITLE = "Weekly sync"
        val MEETING_ID = MeetingId("meeting-id", "domain")
        val CONVERSATION_ID = ConversationId("conversation-id", "domain")
    }
}
