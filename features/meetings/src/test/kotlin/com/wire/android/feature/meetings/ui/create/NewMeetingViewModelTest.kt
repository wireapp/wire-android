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
package com.wire.android.feature.meetings.ui.create

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ramcosta.composedestinations.generated.meetings.navArgs
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.feature.meetings.mapper.toRepeatingInterval
import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.framework.TestUser
import com.wire.android.mapper.ContactMapper
import com.wire.android.model.Contact
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.CurrentTimeProvider
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.MeetingId
import com.wire.kalium.logic.data.meeting.Meeting
import com.wire.kalium.logic.data.meeting.MeetingOccurrence
import com.wire.kalium.logic.data.meeting.UpsertMeeting
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import com.wire.kalium.logic.feature.conversation.RenameConversationUseCase
import com.wire.kalium.logic.feature.conversation.RenamingResult
import com.wire.kalium.logic.feature.meeting.CreateNewMeetingUseCase
import com.wire.kalium.logic.feature.meeting.GetNextMeetingOccurrenceUseCase
import com.wire.kalium.logic.feature.meeting.UpdateMeetingUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, NavigationTestExtension::class, SnapshotExtension::class)
class NewMeetingViewModelTest {
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
    fun givenScheduleTypeAndCurrentTime_whenViewModelIsCreated_thenStateIsInitialized() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val (_, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Schedule)
                .withCurrentTimeProvider { currentTime }
        )

        assertEquals(NewMeetingType.Schedule, viewModel.type)
        assertEquals(currentTime + 1.hours, viewModel.state.startTime) // next full hour
        assertEquals(currentTime + 2.hours, viewModel.state.endTime) // start time + 1 hour
        assertFalse(viewModel.state.continueButtonEnabled)
        assertNull(viewModel.state.titleError)
        assertNull(viewModel.state.startTimeError)
        assertNull(viewModel.state.endTimeError)
    }

    @Test
    fun givenSelectedContacts_whenContactsAreConfirmedAndReset_thenStateKeepsConfirmedContacts() = runTest(dispatcher) {
        val contact = contact("contact-1")
        val otherContact = contact("contact-2")
        val (_, viewModel) = arrangeViewModel()

        viewModel.updateSelectedContact(selected = true, contact = contact)
        viewModel.updateSelectedContact(selected = true, contact = otherContact)
        assertEquals(setOf(contact, otherContact), viewModel.state.selectedContacts.toSet())
        assertEquals(emptySet<Contact>(), viewModel.state.confirmedContacts.toSet())

        viewModel.confirmSelectedContacts()
        assertEquals(setOf(contact, otherContact), viewModel.state.confirmedContacts.toSet())

        viewModel.updateSelectedContact(selected = false, contact = otherContact)
        assertEquals(setOf(contact), viewModel.state.selectedContacts.toSet())

        viewModel.resetSelectedContacts()
        assertEquals(setOf(contact, otherContact), viewModel.state.selectedContacts.toSet())
    }

    @Test
    fun givenRepeatingInterval_whenIntervalIsUpdated_thenStateIsUpdated() = runTest(dispatcher) {
        val (_, viewModel) = arrangeViewModel()

        viewModel.updateRepeatingInterval(MeetingItem.RepeatingInterval.Supported.first())

        assertEquals(MeetingItem.RepeatingInterval.Supported.first(), viewModel.state.repeatingInterval)
    }

    @Test
    fun givenInitialEmptyTitle_whenViewModelIsCreated_thenTitleErrorIsNotShownAndContinueIsDisabled() = runTest(dispatcher) {
        val (_, viewModel) = arrangeViewModel()

        assertNull(viewModel.state.titleError)
        assertFalse(viewModel.state.continueButtonEnabled)
    }

    @Test
    fun givenValidTitle_whenTitleChanges_thenContinueIsEnabled() = runTest(dispatcher) {
        val (_, viewModel) = arrangeViewModel()

        enterTitle(viewModel, "Weekly sync")

        assertNull(viewModel.state.titleError)
        assertEquals(true, viewModel.state.continueButtonEnabled)
    }

    @Test
    fun givenTitleIsClearedAfterInput_whenTitleChanges_thenEmptyTitleErrorIsShown() = runTest(dispatcher) {
        val (_, viewModel) = arrangeViewModel()

        enterTitle(viewModel, "Weekly sync")
        enterTitle(viewModel, "")

        assertEquals(NewMeetingState.TitleError.TitleEmptyError, viewModel.state.titleError)
        assertFalse(viewModel.state.continueButtonEnabled)
    }

    @Test
    fun givenTitleExceedsLimit_whenTitleChanges_thenTitleExceedsLimitErrorIsShown() = runTest(dispatcher) {
        val (_, viewModel) = arrangeViewModel()

        enterTitle(viewModel, "a".repeat(NewMeetingViewModel.MEETING_NAME_MAX_COUNT + 1))

        assertEquals(NewMeetingState.TitleError.TitleExceedsLimitError, viewModel.state.titleError)
        assertFalse(viewModel.state.continueButtonEnabled)
    }

    @Test
    fun givenStartTimeInPast_whenStartTimeChanges_thenStartTimeInPastErrorIsShown() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val (_, viewModel) = arrangeViewModel(Arrangement(dispatcher).withCurrentTimeProvider { currentTime })

        enterTitle(viewModel, "Weekly sync")
        viewModel.updateStartTime(currentTime - 1.hours)
        advanceUntilIdle()

        assertEquals(NewMeetingState.TimeError.StartTimeInPastError, viewModel.state.startTimeError)
        assertFalse(viewModel.state.continueButtonEnabled)
    }

    @Test
    fun givenEndTimeInPast_whenEndTimeChanges_thenEndTimeInPastErrorIsShown() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val (_, viewModel) = arrangeViewModel(Arrangement(dispatcher).withCurrentTimeProvider { currentTime })

        enterTitle(viewModel, "Weekly sync")
        viewModel.updateEndTime(currentTime - 1.hours)
        advanceUntilIdle()

        assertEquals(NewMeetingState.TimeError.EndTimeInPastError, viewModel.state.endTimeError)
        assertFalse(viewModel.state.continueButtonEnabled)
    }

    @Test
    fun givenEndTimeBeforeStartTime_whenEndTimeChanges_thenEndTimeBeforeStartTimeErrorIsShown() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val (_, viewModel) = arrangeViewModel(Arrangement(dispatcher).withCurrentTimeProvider { currentTime })

        enterTitle(viewModel, "Weekly sync")
        viewModel.updateStartTime(currentTime + 2.hours)
        viewModel.updateEndTime(currentTime + 1.hours)
        advanceUntilIdle()

        assertEquals(NewMeetingState.TimeError.EndTimeBeforeStartTimeError, viewModel.state.endTimeError)
        assertFalse(viewModel.state.continueButtonEnabled)
    }

    @Test
    fun givenInvalidTimesBecomeValid_whenTimesChange_thenErrorsAreClearedAndContinueIsEnabled() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val (_, viewModel) = arrangeViewModel(Arrangement(dispatcher).withCurrentTimeProvider { currentTime })

        enterTitle(viewModel, "Weekly sync")
        viewModel.updateStartTime(currentTime + 2.hours)
        viewModel.updateEndTime(currentTime + 1.hours)
        advanceUntilIdle()
        viewModel.updateEndTime(currentTime + 3.hours)
        advanceUntilIdle()

        assertNull(viewModel.state.startTimeError)
        assertNull(viewModel.state.endTimeError)
        assertEquals(true, viewModel.state.continueButtonEnabled)
    }

    @Test
    fun givenMeetNowTypeWithValidData_whenSubmitCreationIsCalled_thenMeetingIsCreatedAndSuccessActionIsSent() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withCurrentTimeProvider { currentTime }
                .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Success)
        )

        enterTitle(viewModel, "  Quick sync  ")

        viewModel.actions.test {
            viewModel.submitCreation()
            advanceUntilIdle()

            coVerify(exactly = 1) {
                arrangement.createNewMeeting(
                    UpsertMeeting(
                        title = "Quick sync",
                        startTime = currentTime,
                        endTime = currentTime + 1.hours,
                        recurrence = null,
                        otherParticipants = emptyList()
                    )
                )
            }
            assertEquals(currentTime, viewModel.state.startTime)
            assertEquals(currentTime + 1.hours, viewModel.state.endTime)
            assertFalse(viewModel.state.isSubmitting)
            assertNull(viewModel.state.submitError)
            assertEquals(NewMeetingViewActions.Success, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun givenScheduleTypeWithValidData_whenSubmitCreationIsCalled_thenMeetingIsCreatedAndSuccessActionIsSent() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val createMeeting = UPSERT_MEETING.copy(startTime = currentTime + 2.hours, endTime = currentTime + 3.hours)
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Schedule)
                .withCurrentTimeProvider { currentTime }
                .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Success)
        )
        enterTitle(viewModel, createMeeting.title)
        viewModel.updateStartTime(createMeeting.startTime)
        viewModel.updateEndTime(createMeeting.endTime)
        viewModel.updateSelectedContact(selected = true, contact = CONTACT)
        viewModel.confirmSelectedContacts()
        viewModel.updateRepeatingInterval(createMeeting.recurrence?.toRepeatingInterval())

        viewModel.actions.test {
            viewModel.submitCreation()
            advanceUntilIdle()
            coVerify(exactly = 1) { arrangement.createNewMeeting(createMeeting) }
            assertFalse(viewModel.state.isSubmitting)
            assertNull(viewModel.state.submitError)
            assertEquals(NewMeetingViewActions.Success, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun givenCreationFails_whenSubmitCreationIsCalled_thenErrorIsShownAndSuccessActionIsNotSent() = runTest(dispatcher) {
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.MeetNow)
                .withCreateMeetingResult(CreateNewMeetingUseCase.Result.Failure)
        )

        enterTitle(viewModel, "Weekly sync")

        viewModel.actions.test {
            viewModel.submitCreation()
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.createNewMeeting(any()) }
            expectNoEvents()
            assertFalse(viewModel.state.isSubmitting)
            assertEquals(true, viewModel.state.continueButtonEnabled)
            assertEquals(NewMeetingState.SubmitError.Other, viewModel.state.submitError)
        }
    }

    @Test
    fun givenEditTypeWithValidData_whenSubmitUpdateIsCalled_thenMeetingIsEditedAndSuccessActionIsSent() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val contact = contact("contact-1")
        val createMeeting = UPSERT_MEETING.copy(startTime = currentTime + 2.hours, endTime = currentTime + 3.hours)
        val editType = NewMeetingType.Edit(MeetingId("meeting-id", "domain"))
        val nextOccurrence = MEETING_OCCURRENCE.copy(
            meeting = MEETING_OCCURRENCE.meeting.copy(
                startTime = currentTime + 1.hours,
                endTime = currentTime + 2.hours,
                recurrence = Meeting.Recurrence(frequency = Meeting.Recurrence.Frequency.DAILY, interval = 1L, until = null),
            ),
            occurrenceStartTime = currentTime + 1.hours,
            occurrenceEndTime = currentTime + 2.hours,
        )
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(editType)
                .withNextMeetingOccurrence(nextOccurrence)
                .withUpdateMeetingResult(nextOccurrence.meeting.meetingId, UpdateMeetingUseCase.Result.Success)
                .withCurrentTimeProvider { currentTime }
        )

        enterTitle(viewModel, createMeeting.title)
        viewModel.updateStartTime(createMeeting.startTime)
        viewModel.updateEndTime(createMeeting.endTime)
        viewModel.updateSelectedContact(selected = true, contact = contact)
        viewModel.confirmSelectedContacts()
        viewModel.updateRepeatingInterval(createMeeting.recurrence?.toRepeatingInterval())

        viewModel.actions.test {
            viewModel.submitUpdate()
            advanceUntilIdle()
            coVerify(exactly = 1) { arrangement.updateMeeting(editType.id, createMeeting) }
            assertFalse(viewModel.state.isSubmitting)
            assertNull(viewModel.state.submitError)
            assertEquals(NewMeetingViewActions.Success, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun givenEditionFails_whenSubmitUpdateIsCalled_thenErrorIsShownAndSuccessActionIsNotSent() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val editType = NewMeetingType.Edit(MeetingId("meeting-id", "domain"))
        val nextOccurrence = MEETING_OCCURRENCE.copy(
            meeting = MEETING_OCCURRENCE.meeting.copy(
                startTime = currentTime + 1.hours,
                endTime = currentTime + 2.hours,
                recurrence = Meeting.Recurrence(frequency = Meeting.Recurrence.Frequency.DAILY, interval = 1L, until = null),
            ),
            occurrenceStartTime = currentTime + 1.hours,
            occurrenceEndTime = currentTime + 2.hours,
        )
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(editType)
                .withNextMeetingOccurrence(nextOccurrence)
                .withUpdateMeetingResult(nextOccurrence.meeting.meetingId, UpdateMeetingUseCase.Result.Failure.Other)
        )

        enterTitle(viewModel, "Weekly sync")

        viewModel.actions.test {
            viewModel.submitUpdate()
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.updateMeeting(editType.id, any()) }
            expectNoEvents()
            assertFalse(viewModel.state.isSubmitting)
            assertEquals(true, viewModel.state.continueButtonEnabled)
            assertEquals(NewMeetingState.SubmitError.Other, viewModel.state.submitError)
        }
    }

    @Test
    fun givenConversationNameEditionFails_whenSubmitUpdateIsCalled_thenErrorIsShownAndSuccessActionIsNotSent() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val editType = NewMeetingType.Edit(MeetingId("meeting-id", "domain"))
        val nextOccurrence = MEETING_OCCURRENCE.copy(
            meeting = MEETING_OCCURRENCE.meeting.copy(
                startTime = currentTime + 1.hours,
                endTime = currentTime + 2.hours,
                recurrence = Meeting.Recurrence(frequency = Meeting.Recurrence.Frequency.DAILY, interval = 1L, until = null),
            ),
            occurrenceStartTime = currentTime + 1.hours,
            occurrenceEndTime = currentTime + 2.hours,
        )
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(editType)
                .withNextMeetingOccurrence(nextOccurrence)
                .withUpdateMeetingResult(
                    nextOccurrence.meeting.meetingId,
                    UpdateMeetingUseCase.Result.Failure.UpdateConversationNameFailure(nextOccurrence.meeting.conversationId)
                )
        )

        enterTitle(viewModel, "Weekly sync")

        viewModel.actions.test {
            viewModel.submitUpdate()
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.updateMeeting(editType.id, any()) }
            expectNoEvents()
            assertFalse(viewModel.state.isSubmitting)
            assertEquals(true, viewModel.state.continueButtonEnabled)
            assertEquals(
                NewMeetingState.SubmitError.UpdateConversationNameFailure(nextOccurrence.meeting.conversationId),
                viewModel.state.submitError
            )
        }
    }

    @Test
    fun givenRetryUpdateConversationNameSucceeds_whenRetryUpdateConversationNameIsCalled_thenSuccessActionIsSent() = runTest(dispatcher) {
        val conversationId = ConversationId("conversation-id", "domain")
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withRenameConversationResult(conversationId, "Weekly sync", RenamingResult.Success)
        )

        enterTitle(viewModel, "  Weekly sync  ")

        viewModel.actions.test {
            viewModel.retryUpdateConversationName(conversationId)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                arrangement.renameConversationUseCase(
                    conversationId = conversationId,
                    conversationName = "Weekly sync"
                )
            }
            assertNull(viewModel.state.submitError)
            assertEquals(NewMeetingViewActions.Success, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun givenRetryUpdateConversationNameFails_whenRetryUpdateConversationNameIsCalled_thenErrorIsShownAndSuccessActionIsNotSent() =
        runTest(dispatcher) {
            val conversationId = ConversationId("conversation-id", "domain")
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withRenameConversationResult(
                        conversationId = conversationId,
                        conversationName = "Weekly sync",
                        result = RenamingResult.Failure(CoreFailure.Unknown(RuntimeException("Failed to rename conversation")))
                    )
            )

            enterTitle(viewModel, "  Weekly sync  ")

            viewModel.actions.test {
                viewModel.retryUpdateConversationName(conversationId)
                advanceUntilIdle()

                coVerify(exactly = 1) {
                    arrangement.renameConversationUseCase(
                        conversationId = conversationId,
                        conversationName = "Weekly sync"
                    )
                }
                expectNoEvents()
                assertEquals(NewMeetingState.SubmitError.UpdateConversationNameFailure(conversationId), viewModel.state.submitError)
            }
        }

    @Test
    fun givenInvalidTitle_whenSubmitCreationIsCalled_thenTitleErrorIsShownAndSuccessActionIsNotSent() = runTest(dispatcher) {
        val (arrangement, viewModel) = arrangeViewModel()

        viewModel.actions.test {
            viewModel.submitCreation()
            advanceUntilIdle()

            coVerify(exactly = 0) { arrangement.createNewMeeting(any()) }
            coVerify(exactly = 0) { arrangement.updateMeeting(any(), any()) }
            expectNoEvents()
            assertEquals(NewMeetingState.TitleError.TitleEmptyError, viewModel.state.titleError)
            assertFalse(viewModel.state.continueButtonEnabled)
        }
    }

    @Test
    fun givenScheduleTypeWithInvalidTimes_whenCreateMeetingIsCalled_thenTimeErrorIsShownAndMeetingIsNotCreated() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(NewMeetingType.Schedule)
                .withCurrentTimeProvider { currentTime }
        )

        enterTitle(viewModel, "Weekly sync")
        viewModel.updateStartTime(currentTime - 1.hours)

        viewModel.actions.test {
            viewModel.submitCreation()
            advanceUntilIdle()

            coVerify(exactly = 0) { arrangement.createNewMeeting(any()) }
            coVerify(exactly = 0) { arrangement.updateMeeting(any(), any()) }
            expectNoEvents()
            assertEquals(NewMeetingState.TimeError.StartTimeInPastError, viewModel.state.startTimeError)
            assertFalse(viewModel.state.continueButtonEnabled)
        }
    }

    @Test
    fun givenEditTypeAndNextOccurrenceExists_whenViewModelIsCreated_thenStateIsInitializedFromNextOccurrence() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val nextOccurrence = MEETING_OCCURRENCE.copy(
            meeting = MEETING_OCCURRENCE.meeting.copy(
                startTime = currentTime + 1.hours,
                endTime = currentTime + 2.hours,
                recurrence = Meeting.Recurrence(frequency = Meeting.Recurrence.Frequency.DAILY, interval = 1L, until = null),
            ),
            occurrenceStartTime = currentTime + 1.hours,
            occurrenceEndTime = currentTime + 2.hours,
        )
        val editType = NewMeetingType.Edit(nextOccurrence.meeting.meetingId)
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(editType)
                .withCurrentTimeProvider { currentTime }
                .withNextMeetingOccurrence(nextOccurrence)
        )

        coVerify(exactly = 1) { arrangement.getNextMeetingOccurrence(editType.id, currentTime) }
        assertEquals(editType, viewModel.type)
        assertEquals(nextOccurrence.meeting.title, viewModel.titleTextState.text.toString())
        assertEquals(nextOccurrence.occurrenceStartTime, viewModel.state.startTime)
        assertEquals(nextOccurrence.occurrenceEndTime, viewModel.state.endTime)
        assertEquals(nextOccurrence.meeting.recurrence?.frequency, viewModel.state.repeatingInterval?.frequency)
        assertEquals(nextOccurrence.meeting.recurrence?.interval?.toInt(), viewModel.state.repeatingInterval?.interval)
        assertEquals(NewMeetingState.InitialLoadingState.Loaded, viewModel.state.initialLoading)
    }

    @Suppress("UnusedFlow")
    @Test
    fun givenEditTypeAndNextOccurrenceExists_whenViewModelIsCreated_thenParticipantsAreInitializedFromOccurrenceConversation() =
        runTest(dispatcher) {
            val currentTime = Instant.parse("2026-01-01T12:00:00Z")
            val editType = NewMeetingType.Edit(MEETING_OCCURRENCE.meeting.meetingId)
            val firstUser = TestUser.OTHER_USER.copy(id = UserId("contact-1", "domain"))
            val secondUser = TestUser.OTHER_USER.copy(id = UserId("contact-2", "domain"))
            val firstContact = contact("contact-1")
            val secondContact = contact("contact-2")
            val conversationMembers = listOf(
                MemberDetails(firstUser, Conversation.Member.Role.Member),
                MemberDetails(secondUser, Conversation.Member.Role.Admin),
            )
            val (arrangement, viewModel) = arrangeViewModel(
                Arrangement(dispatcher)
                    .withNewMeetingType(editType)
                    .withCurrentTimeProvider { currentTime }
                    .withNextMeetingOccurrence(MEETING_OCCURRENCE)
                    .withConversationMembers(MEETING_OCCURRENCE.meeting.conversationId, conversationMembers)
                    .withMappedContact(firstUser, firstContact)
                    .withMappedContact(secondUser, secondContact)
            )

            coVerify(exactly = 1) { arrangement.observeConversationMembers(MEETING_OCCURRENCE.meeting.conversationId) }
            assertEquals(setOf(firstContact, secondContact), viewModel.state.selectedContacts.toSet())
            assertEquals(setOf(firstContact, secondContact), viewModel.state.confirmedContacts.toSet())
            assertEquals(NewMeetingState.InitialLoadingState.Loaded, viewModel.state.initialLoading)
        }

    @Test
    fun givenEditTypeAndNextOccurrenceDoesNotExist_whenViewModelIsCreated_thenStateStopsLoadingWithInitialTimes() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val editType = NewMeetingType.Edit(MeetingId("meeting-id", "domain"))
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(editType)
                .withCurrentTimeProvider { currentTime }
                .withNextMeetingOccurrence(null)
        )

        coVerify(exactly = 1) { arrangement.getNextMeetingOccurrence(editType.id, currentTime) }
        assertEquals(editType, viewModel.type)
        assertEquals("", viewModel.titleTextState.text.toString())
        assertEquals(currentTime + 1.hours, viewModel.state.startTime)
        assertEquals(currentTime + 2.hours, viewModel.state.endTime)
        assertEquals(NewMeetingState.InitialLoadingState.Loaded, viewModel.state.initialLoading)
    }

    @Test
    fun givenEditTypeAndNextOccurrenceLoadFails_whenViewModelIsCreated_thenInitialLoadingStateIsError() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val editType = NewMeetingType.Edit(MeetingId("meeting-id", "domain"))
        val (arrangement, viewModel) = arrangeViewModel(
            Arrangement(dispatcher)
                .withNewMeetingType(editType)
                .withCurrentTimeProvider { currentTime }
                .withNextMeetingOccurrenceFailure()
        )

        coVerify(exactly = 1) { arrangement.getNextMeetingOccurrence(editType.id, currentTime) }
        assertEquals(NewMeetingState.InitialLoadingState.Error, viewModel.state.initialLoading)
    }

    private fun TestScope.arrangeViewModel(
        arrangement: Arrangement = Arrangement(dispatcher)
    ): Pair<Arrangement, NewMeetingViewModelImpl> =
        arrangement.arrange().also { runCurrent() }

    private fun TestScope.enterTitle(viewModel: NewMeetingViewModel, title: String) {
        viewModel.titleTextState.setTextAndPlaceCursorAtEnd(title)
        advanceUntilIdle()
    }

    private fun contact(id: String) = Contact(
        id = id,
        domain = "wire.com",
        name = "Contact $id",
        handle = id,
        membership = Membership.Standard,
        connectionState = ConnectionState.ACCEPTED,
    )

    private class Arrangement(private val dispatcher: TestDispatcher) {
        var currentTimeProvider = CurrentTimeProvider {
            Instant.fromEpochMilliseconds(dispatcher.scheduler.currentTime)
        }

        @MockK
        private lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var createNewMeeting: CreateNewMeetingUseCase

        @MockK
        lateinit var updateMeeting: UpdateMeetingUseCase

        @MockK
        lateinit var getNextMeetingOccurrence: GetNextMeetingOccurrenceUseCase

        @MockK
        lateinit var observeConversationMembers: ObserveConversationMembersUseCase

        @MockK
        lateinit var renameConversationUseCase: RenameConversationUseCase

        @MockK
        lateinit var contactMapper: ContactMapper

        private var newMeetingType: NewMeetingType = NewMeetingType.MeetNow

        init {
            MockKAnnotations.init(this)
            every {
                savedStateHandle.navArgs<NewMeetingNavArgs>()
            } answers { NewMeetingNavArgs(type = newMeetingType) }
            coEvery { getNextMeetingOccurrence(any(), any()) } returns null
            coEvery { observeConversationMembers(any()) } returns flowOf(emptyList())
        }

        fun withNewMeetingType(type: NewMeetingType) = apply {
            newMeetingType = type
        }

        fun withCurrentTimeProvider(currentTime: () -> Instant) = apply {
            currentTimeProvider = CurrentTimeProvider(currentTime)
        }

        fun withCreateMeetingResult(result: CreateNewMeetingUseCase.Result) = apply {
            coEvery { createNewMeeting(any()) } returns result
        }

        fun withUpdateMeetingResult(meetingId: MeetingId, result: UpdateMeetingUseCase.Result) = apply {
            coEvery { updateMeeting(meetingId, any()) } returns result
        }

        fun withRenameConversationResult(conversationId: ConversationId, conversationName: String, result: RenamingResult) = apply {
            coEvery { renameConversationUseCase(conversationId, conversationName) } returns result
        }

        fun withNextMeetingOccurrence(nextMeetingOccurrence: MeetingOccurrence?) = apply {
            coEvery { getNextMeetingOccurrence(any(), any()) } returns nextMeetingOccurrence
        }

        fun withNextMeetingOccurrenceFailure() = apply {
            coEvery { getNextMeetingOccurrence(any(), any()) } throws IllegalStateException("Failed to load meeting occurrence")
        }

        fun withConversationMembers(conversationId: ConversationId, members: List<MemberDetails>) = apply {
            coEvery { observeConversationMembers(conversationId) } returns flowOf(members)
        }

        fun withMappedContact(otherUser: OtherUser, contact: Contact) = apply {
            every { contactMapper.fromOtherUser(otherUser) } returns contact
        }

        fun arrange() = this to NewMeetingViewModelImpl(
            savedStateHandle = savedStateHandle,
            currentTimeProvider = currentTimeProvider,
            createNewMeeting = createNewMeeting,
            updateMeeting = updateMeeting,
            getNextMeetingOccurrence = getNextMeetingOccurrence,
            observeConversationMembers = observeConversationMembers,
            renameConversationUseCase = renameConversationUseCase,
            contactMapper = contactMapper,
        )
    }

    private val MEETING_OCCURRENCE = MeetingOccurrence(
        meeting = Meeting(
            meetingId = MeetingId("meeting-id", "domain"),
            conversationId = ConversationId("conversation-id", "domain"),
            creatorId = UserId("creator-id", "domain"),
            title = "Daily",
            startTime = Instant.parse("2026-01-01T09:00:00Z"),
            endTime = Instant.parse("2026-01-01T10:00:00Z"),
            recurrence = Meeting.Recurrence(frequency = Meeting.Recurrence.Frequency.DAILY, interval = 1L, until = null),
        ),
        selfRole = MeetingOccurrence.SelfRole.Creator,
        conversationName = "Daily",
        conversationType = MeetingOccurrence.ConversationType.Group,
        occurrenceId = "occurrence-id",
        occurrenceStartTime = Instant.parse("2026-01-02T09:00:00Z"),
        occurrenceEndTime = Instant.parse("2026-01-02T10:00:00Z"),
    )
    private val CONTACT = contact("contact-1")
    private val UPSERT_MEETING = UpsertMeeting(
        title = "Weekly sync",
        startTime = Instant.parse("2026-01-01T09:00:00Z"),
        endTime = Instant.parse("2026-01-01T10:00:00Z"),
        recurrence = Meeting.Recurrence(
            frequency = MeetingItem.RepeatingInterval.Supported.first().frequency,
            interval = MeetingItem.RepeatingInterval.Supported.first().interval.toLong(),
            until = null
        ),
        otherParticipants = listOf(UserId(CONTACT.id, CONTACT.domain))
    )
}
