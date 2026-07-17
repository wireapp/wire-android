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
import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.model.Contact
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.CurrentTimeProvider
import com.wire.kalium.logic.data.user.ConnectionState
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.AfterEach
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
    fun givenValidData_whenCreateMeetingIsCalled_thenSuccessActionIsSent() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val (_, viewModel) = arrangeViewModel(Arrangement(dispatcher).withCurrentTimeProvider { currentTime })

        enterTitle(viewModel, "Weekly sync")

        viewModel.actions.test {
            viewModel.createMeeting()
            advanceUntilIdle()

            assertEquals(NewMeetingViewActions.Success, awaitItem())
        }
    }

    @Test
    fun givenInvalidTitle_whenCreateMeetingIsCalled_thenTitleErrorIsShownAndSuccessActionIsNotSent() = runTest(dispatcher) {
        val (_, viewModel) = arrangeViewModel()

        viewModel.actions.test {
            viewModel.createMeeting()
            advanceUntilIdle()

            expectNoEvents()
            assertEquals(NewMeetingState.TitleError.TitleEmptyError, viewModel.state.titleError)
            assertFalse(viewModel.state.continueButtonEnabled)
        }
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

        private var newMeetingType: NewMeetingType = NewMeetingType.MeetNow

        init {
            MockKAnnotations.init(this)
            every {
                savedStateHandle.navArgs<NewMeetingNavArgs>()
            } answers { NewMeetingNavArgs(type = newMeetingType) }
        }

        fun withNewMeetingType(type: NewMeetingType) = apply {
            newMeetingType = type
        }
        fun withCurrentTimeProvider(currentTime: () -> Instant) = apply {
            currentTimeProvider = CurrentTimeProvider(currentTime)
        }

        fun arrange() = this to NewMeetingViewModelImpl(
            savedStateHandle = savedStateHandle,
            currentTimeProvider = currentTimeProvider,
        )
    }
}
