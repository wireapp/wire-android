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
package com.wire.android.feature.meetings.ui.list

import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.feature.meetings.model.MeetingListItem
import com.wire.android.feature.meetings.ui.MeetingsTabItem
import com.wire.android.feature.meetings.ui.mock.Meeting
import com.wire.android.feature.meetings.ui.usecase.GetMeetingsPaginatedUseCase
import com.wire.android.util.CurrentTimeProvider
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.call.usecase.ObserveActiveCallsUseCase
import io.mockk.MockKAnnotations
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class MeetingListViewModelTest {
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
    fun givenCurrentTimeBetweenFullMinutes_whenMeetingsAreCollected_thenNextEmissionIsAlignedToNextFullMinute() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:30.500Z")
        val meetingStartTime = Instant.parse("2026-01-01T12:01:00Z")
        val (_, viewModel) = Arrangement(dispatcher)
            .withCurrentTimeProvider { currentTime + testScheduler.currentTime.milliseconds }
            .withGetMeetingsPaginated(showingAll = false, meetings = listOf(meeting(startTime = meetingStartTime)))
            .arrange()

        viewModel.meetings.test {
            awaitItem()

            advanceTimeBy(29_499)
            expectNoEvents()

            advanceTimeBy(1)
            runCurrent()
            awaitItem()

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun givenCurrentTimeBetweenFullMinutes_whenMeetingsAreCollected_thenNextEmissionUpdatesTheStatusesOfMeetings() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:30.500Z")
        val meetingStartTime = Instant.parse("2026-01-01T12:01:00Z")
        val (_, viewModel) = Arrangement(dispatcher)
            .withCurrentTimeProvider { currentTime + testScheduler.currentTime.milliseconds }
            .withGetMeetingsPaginated(showingAll = false, meetings = listOf(meeting(startTime = meetingStartTime)))
            .arrange()

        viewModel.meetings.test {
            val first = awaitItem()

            advanceTimeBy(29_500)
            runCurrent()
            val second = expectMostRecentItem()

            assertEquals(MeetingItem.Status.Scheduled::class, first.items().meetingItem().status::class)
            assertEquals(MeetingItem.Status.Ongoing::class, second.items().meetingItem().status::class)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Suppress("UnusedFlow")
    @Test
    fun givenIsShowingAllChanges_whenMeetingsAreCollected_thenMeetingsAreRefreshedWithProperParameters() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val initialMeeting = meeting(meetingId = "initial-meeting", startTime = currentTime + 1.minutes)
        val allMeeting = meeting(meetingId = "all-meeting", startTime = currentTime + 2.minutes)
        val (arrangement, viewModel) = Arrangement(dispatcher)
            .withCurrentTimeProvider { currentTime }
            .withGetMeetingsPaginated(showingAll = false, meetings = listOf(initialMeeting))
            .withGetMeetingsPaginated(showingAll = true, meetings = listOf(allMeeting))
            .arrange()

        viewModel.meetings.test {
            val first = awaitItem().items()
            assertEquals(false, viewModel.isShowingAll.value)
            assertEquals(initialMeeting.meetingId, first.meetingItem().meetingId)
            verify(exactly = 1) { arrangement.getMeetingsPaginated(showingAll = false, type = arrangement.type) }
            verify(exactly = 0) { arrangement.getMeetingsPaginated(showingAll = true, type = arrangement.type) }

            clearMocks(arrangement.getMeetingsPaginated, answers = false, recordedCalls = true)
            viewModel.showAll()
            runCurrent()
            val second = awaitItem().items()
            assertEquals(true, viewModel.isShowingAll.value)
            assertEquals(allMeeting.meetingId, second.meetingItem().meetingId)
            verify(exactly = 0) { arrangement.getMeetingsPaginated(showingAll = false, type = arrangement.type) }
            verify(exactly = 1) { arrangement.getMeetingsPaginated(showingAll = true, type = arrangement.type) }

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun givenSomeMeetingsHaveActiveCalls_whenMeetingsAreCollected_thenMatchingMeetingsContainOngoingCallStatus() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val meetingWithActiveCall = meeting(
            meetingId = "active-meeting",
            conversationId = ConversationId("conversation-with-call", "domain"),
            startTime = currentTime - 10.minutes
        )
        val meetingWithoutActiveCall = meeting(
            meetingId = "inactive-meeting",
            conversationId = ConversationId("conversation-without-call", "domain"),
            startTime = currentTime - 5.minutes
        )
        val activeCall = call(
            conversationId = meetingWithActiveCall.conversationId,
            establishedTime = "2026-01-01T11:55:00.000Z"
        )
        val (_, viewModel) = Arrangement(dispatcher)
            .withCurrentTimeProvider { currentTime }
            .withGetMeetingsPaginated(showingAll = false, meetings = listOf(meetingWithActiveCall, meetingWithoutActiveCall))
            .withObserveActiveCalls(listOf(activeCall))
            .arrange()

        val meetingItems = viewModel.meetings.first().items().meetingItems()

        assertEquals(
            MeetingItem.OngoingCallStatus(
                currentCallEstablishedTime = Instant.parse("2026-01-01T11:55:00Z"),
                isSelfUserAttending = true
            ),
            meetingItems.single { it.meetingId == meetingWithActiveCall.meetingId }.ongoingStatus().ongoingCallStatus
        )
        assertEquals(
            null,
            meetingItems.single { it.meetingId == meetingWithoutActiveCall.meetingId }.ongoingStatus().ongoingCallStatus
        )
    }

    @Test
    fun givenNoActiveCalls_whenMeetingsAreCollected_thenMeetingsDoNotContainOngoingCallStatus() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val meetings = listOf(
            meeting(meetingId = "first-meeting", startTime = currentTime - 10.minutes),
            meeting(meetingId = "second-meeting", startTime = currentTime - 5.minutes)
        )
        val (_, viewModel) = Arrangement(dispatcher)
            .withCurrentTimeProvider { currentTime }
            .withGetMeetingsPaginated(showingAll = false, meetings = meetings)
            .withObserveActiveCalls(emptyList())
            .arrange()

        val meetingItems = viewModel.meetings.first().items().meetingItems()

        assertEquals(
            listOf(null, null),
            meetingItems.map { it.ongoingStatus().ongoingCallStatus }
        )
    }

    private fun List<MeetingListItem>.meetingItem() = meetingItems().single()
    private fun List<MeetingListItem>.meetingItems() = filterIsInstance<MeetingItem>()
    private fun MeetingItem.ongoingStatus() = status as MeetingItem.Status.Ongoing
    private suspend fun PagingData<MeetingListItem>.items(): List<MeetingListItem> = flowOf(this).asSnapshot()
    private fun meeting(
        meetingId: String = "meeting-id",
        startTime: Instant,
        conversationId: ConversationId = ConversationId("conversation-id", "domain"),
    ) = Meeting(
        meetingId = meetingId,
        conversationId = conversationId,
        belongingType = MeetingItem.BelongingType.Group("group"),
        title = "Meeting",
        startTime = startTime,
        endTime = startTime + 30.minutes,
        repeatingInterval = MeetingItem.RepeatingInterval.Never,
        selfRole = MeetingItem.SelfRole.Admin,
    )
    private fun call(
        conversationId: ConversationId,
        status: CallStatus = CallStatus.ESTABLISHED,
        establishedTime: String? = null,
    ) = Call(
        conversationId = conversationId,
        status = status,
        isMuted = false,
        isCameraOn = false,
        isCbrEnabled = false,
        callerId = QualifiedID("caller-id", "domain"),
        conversationName = "Meeting",
        conversationType = Conversation.Type.Group.Regular,
        callerName = "Caller",
        callerTeamName = "Team",
        establishedTime = establishedTime,
    )

    private class Arrangement(private val dispatcher: TestDispatcher) {
        val type = MeetingsTabItem.NEXT
        var currentTimeProvider = CurrentTimeProvider {
            Instant.fromEpochMilliseconds(dispatcher.scheduler.currentTime)
        }

        @MockK
        lateinit var getMeetingsPaginated: GetMeetingsPaginatedUseCase

        @MockK
        lateinit var observeActiveCalls: ObserveActiveCallsUseCase

        init {
            MockKAnnotations.init(this)
            every { observeActiveCalls() } returns flowOf(emptyList())
        }
        fun withCurrentTimeProvider(currentTime: () -> Instant) = apply {
            currentTimeProvider = CurrentTimeProvider(currentTime)
        }
        fun withGetMeetingsPaginated(showingAll: Boolean, meetings: List<Meeting>) = apply {
            every { getMeetingsPaginated(showingAll = showingAll, type = type) } returns flowOf(
                PagingData.from(meetings)
            )
        }
        fun withObserveActiveCalls(activeCalls: List<Call>) = apply {
            every { observeActiveCalls() } returns flowOf(activeCalls)
        }
        fun arrange() = this to MeetingListViewModelImpl(
            type = type,
            dispatcher = TestDispatcherProvider(dispatcher),
            currentTimeProvider = currentTimeProvider,
            getMeetingsPaginated = getMeetingsPaginated,
            observeActiveCalls = observeActiveCalls,
        )
    }
}
