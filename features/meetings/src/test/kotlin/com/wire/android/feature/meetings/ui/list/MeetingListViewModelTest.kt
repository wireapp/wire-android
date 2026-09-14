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
@file:Suppress("UnusedFlow")

package com.wire.android.feature.meetings.ui.list

import androidx.lifecycle.viewmodel.testing.viewModelScenario
import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.feature.meetings.model.MeetingListItem
import com.wire.android.feature.meetings.ui.MeetingsTabItem
import com.wire.android.feature.meetings.ui.usecase.GetPaginatedFlowOfMeetingsUseCase
import com.wire.android.feature.meetings.ui.util.SystemTimeObserver
import com.wire.android.util.CurrentTimeProvider
import com.wire.android.util.time.CurrentTimeZoneProvider
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.MeetingId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.meeting.Meeting
import com.wire.kalium.logic.data.meeting.MeetingOccurrence
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.ObserveActiveCallsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun givenNoTimeBroadcast_whenMeetingsAreCollected_thenPagingLoadsImmediately() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:30Z")
        val initialMeeting = meeting(startTime = currentTime + 30.minutes)
        val (arrangement, viewModelScenario) = Arrangement(dispatcher)
            .withCurrentTimeProvider { currentTime }
            .withGetMeetingsPaginated(listOf(initialMeeting))
            .arrange()

        viewModelScenario.use { scenario ->
            scenario.viewModel.meetings.test {
                val pagingData = awaitItem()
                assertEquals(0L, testScheduler.currentTime)
                assertEquals(initialMeeting.meeting.meetingId, pagingData.items().meetingItem().meetingId)
                coVerify(exactly = 1) { arrangement.getMeetingsPaginated(arrangement.type) }
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun givenMeetingsAreCollected_whenMinuteTickArrives_thenCurrentTimeIsUpdated() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:30.500Z")
        val meetingStartTime = Instant.parse("2026-01-01T12:01:00Z")
        val (arrangement, viewModelScenario) = Arrangement(dispatcher)
            .withCurrentTimeProvider { currentTime + testScheduler.currentTime.milliseconds }
            .withGetMeetingsPaginated(meetings = listOf(meeting(startTime = meetingStartTime)))
            .arrange()

        viewModelScenario.use { scenario ->
            scenario.viewModel.meetings.test {
                awaitItem()

                advanceTimeBy(29_499)
                expectNoEvents()

                advanceTimeBy(1)
                arrangement.minuteTicks.emit(Unit)
                runCurrent()
                awaitItem()

                cancelAndConsumeRemainingEvents()
            }
        }
    }

    @Test
    fun givenScheduledMeeting_whenMinuteTickArrives_thenMeetingBecomesOngoing() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:30.500Z")
        val meetingStartTime = Instant.parse("2026-01-01T12:01:00Z")
        val (arrangement, viewModelScenario) = Arrangement(dispatcher)
            .withCurrentTimeProvider { currentTime + testScheduler.currentTime.milliseconds }
            .withGetMeetingsPaginated(meetings = listOf(meeting(startTime = meetingStartTime)))
            .arrange()

        viewModelScenario.use { scenario ->
            scenario.viewModel.meetings.test {
                val first = awaitItem()

                advanceTimeBy(29_500)
                arrangement.minuteTicks.emit(Unit)
                runCurrent()
                val second = expectMostRecentItem()

                assertEquals(MeetingItem.Status.Scheduled::class, first.items().meetingItem().status::class)
                assertEquals(MeetingItem.Status.Ongoing::class, second.items().meetingItem().status::class)

                cancelAndConsumeRemainingEvents()
            }
        }
    }

    @Test
    fun givenSomeMeetingsHaveActiveCalls_whenMeetingsAreCollected_thenMatchingMeetingsContainOngoingCallStatus() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val meetingWithActiveCall = meeting(
            meetingId = MeetingId("active-meeting", "domain"),
            conversationId = ConversationId("conversation-with-call", "domain"),
            startTime = currentTime - 10.minutes
        )
        val meetingWithoutActiveCall = meeting(
            meetingId = MeetingId("inactive-meeting", "domain"),
            conversationId = ConversationId("conversation-without-call", "domain"),
            startTime = currentTime - 5.minutes
        )
        val activeCall = call(
            conversationId = meetingWithActiveCall.meeting.conversationId,
            establishedTime = Instant.parse("2026-01-01T11:55:00Z")
        )
        val (_, viewModelScenario) = Arrangement(dispatcher)
            .withCurrentTimeProvider { currentTime }
            .withGetMeetingsPaginated(meetings = listOf(meetingWithActiveCall, meetingWithoutActiveCall))
            .withObserveActiveCalls(listOf(activeCall))
            .arrange()

        viewModelScenario.use { scenario ->
            val meetingItems = scenario.viewModel.meetings.first().items().meetingItems()

            assertEquals(
                MeetingItem.OngoingCallStatus(
                    currentCallEstablishedTime = Instant.parse("2026-01-01T11:55:00Z"),
                    isSelfUserAttending = true
                ),
                meetingItems.single { it.meetingId == meetingWithActiveCall.meeting.meetingId }.ongoingStatus().ongoingCallStatus
            )
            assertEquals(
                null,
                meetingItems.single { it.meetingId == meetingWithoutActiveCall.meeting.meetingId }.ongoingStatus().ongoingCallStatus
            )
        }
    }

    @Test
    fun givenNoActiveCalls_whenMeetingsAreCollected_thenMeetingsDoNotContainOngoingCallStatus() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val meetings = listOf(
            meeting(meetingId = MeetingId("first-meeting", "domain"), startTime = currentTime - 10.minutes),
            meeting(meetingId = MeetingId("second-meeting", "domain"), startTime = currentTime - 5.minutes)
        )
        val (_, viewModelScenario) = Arrangement(dispatcher)
            .withCurrentTimeProvider { currentTime }
            .withGetMeetingsPaginated(meetings = meetings)
            .withObserveActiveCalls(emptyList())
            .arrange()

        viewModelScenario.use { scenario ->
            val meetingItems = scenario.viewModel.meetings.first().items().meetingItems()

            assertEquals(
                listOf(null, null),
                meetingItems.map { it.ongoingStatus().ongoingCallStatus }
            )
        }
    }

    @Test
    fun givenSameDayAndTimeZone_whenMinuteChanges_thenPagingIsNotReloaded() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:30Z")
        val (arrangement, viewModelScenario) = Arrangement(dispatcher)
            .withCurrentTimeProvider { currentTime + testScheduler.currentTime.milliseconds }
            .withGetMeetingsPaginated(emptyList())
            .arrange()

        viewModelScenario.use { scenario ->
            scenario.viewModel.meetings.test {
                awaitItem()
                advanceTimeBy(90_000)
                arrangement.minuteTicks.emit(Unit)
                runCurrent()
                coVerify(exactly = 1) { arrangement.getMeetingsPaginated(arrangement.type) }
                cancelAndConsumeRemainingEvents()
            }
        }
    }

    @Test
    fun givenLocalMidnight_whenDayChanges_thenPagingIsReloadedAndPreviousDayIsRemoved() = runTest(dispatcher) {
        // Midnight in Berlin occurs before the UTC date changes.
        val currentTime = Instant.parse("2026-01-01T22:59:30Z")
        val previousDayMeeting = meeting(startTime = currentTime - 30.minutes)
        val nextDayMeeting = meeting(meetingId = MeetingId("next-day", "domain"), startTime = currentTime + 30.minutes)
        val (arrangement, viewModelScenario) = Arrangement(dispatcher)
            .withTimeZoneProvider { TimeZone.of("Europe/Berlin") }
            .withCurrentTimeProvider { currentTime + testScheduler.currentTime.milliseconds }
            .withGetMeetingsPaginated(listOf(previousDayMeeting, nextDayMeeting))
            .arrange()

        viewModelScenario.use { scenario ->
            scenario.viewModel.meetings.test {
                assertEquals(2, awaitItem().items().meetingItems().size)
                arrangement.withGetMeetingsPaginated(listOf(nextDayMeeting))

                advanceTimeBy(30_000)
                arrangement.minuteTicks.emit(Unit)
                runCurrent()

                assertEquals(nextDayMeeting.meeting.meetingId, expectMostRecentItem().items().meetingItem().meetingId)
                coVerify(exactly = 2) { arrangement.getMeetingsPaginated(arrangement.type) }
                cancelAndConsumeRemainingEvents()
            }
        }
    }

    @Test
    fun givenSameLocalDate_whenTimeZoneChanges_thenPagingIsReloadedImmediately() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T12:00:30Z")
        var timeZone: TimeZone = TimeZone.UTC
        val (arrangement, viewModelScenario) = Arrangement(dispatcher)
            .withTimeZoneProvider { timeZone }
            .withCurrentTimeProvider { currentTime + testScheduler.currentTime.milliseconds }
            .withGetMeetingsPaginated(emptyList())
            .arrange()

        viewModelScenario.use { scenario ->
            scenario.viewModel.meetings.test {
                awaitItem()
                timeZone = TimeZone.of("Europe/Berlin")
                arrangement.systemTimeChanges.emit(Unit)
                runCurrent()
                coVerify(exactly = 2) { arrangement.getMeetingsPaginated(arrangement.type) }
                assertEquals(0L, testScheduler.currentTime)

                arrangement.systemTimeChanges.emit(Unit)
                advanceTimeBy(60_000)
                arrangement.minuteTicks.emit(Unit)
                runCurrent()
                coVerify(exactly = 2) { arrangement.getMeetingsPaginated(arrangement.type) }
                cancelAndConsumeRemainingEvents()
            }
        }
    }

    @Test
    fun givenCachedPaging_whenUiUnsubscribes_thenTimeObservationStopsAndCacheIsReusedOnReturn() = runTest(dispatcher) {
        val (arrangement, viewModelScenario) = Arrangement(dispatcher)
            .withCurrentTimeProvider { Instant.parse("2026-01-01T12:00:00Z") }
            .withGetMeetingsPaginated(emptyList())
            .arrange()

        viewModelScenario.use { scenario ->
            scenario.viewModel.meetings.test {
                awaitItem()
                runCurrent()
                assertEquals(1, arrangement.minuteTicks.subscriptionCount.value)
                assertEquals(1, arrangement.systemTimeChanges.subscriptionCount.value)
                cancelAndIgnoreRemainingEvents()
            }
            runCurrent()
            assertEquals(0, arrangement.minuteTicks.subscriptionCount.value)
            assertEquals(0, arrangement.systemTimeChanges.subscriptionCount.value)

            scenario.viewModel.meetings.test {
                awaitItem()
                runCurrent()
                assertEquals(1, arrangement.minuteTicks.subscriptionCount.value)
                assertEquals(1, arrangement.systemTimeChanges.subscriptionCount.value)
                coVerify(exactly = 1) { arrangement.getMeetingsPaginated(arrangement.type) }
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun givenUiUnsubscribed_whenMidnightPasses_thenPagingRefreshesOnlyWhenUiReturns() = runTest(dispatcher) {
        val currentTime = Instant.parse("2026-01-01T23:59:30Z")
        val (arrangement, viewModelScenario) = Arrangement(dispatcher)
            .withCurrentTimeProvider { currentTime + testScheduler.currentTime.milliseconds }
            .withGetMeetingsPaginated(emptyList())
            .arrange()

        viewModelScenario.use { scenario ->
            scenario.viewModel.meetings.test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            runCurrent()
            advanceTimeBy(30_000)
            arrangement.minuteTicks.emit(Unit)
            runCurrent()
            coVerify(exactly = 1) { arrangement.getMeetingsPaginated(arrangement.type) }

            scenario.viewModel.meetings.test {
                awaitItem()
                runCurrent()
                coVerify(exactly = 2) { arrangement.getMeetingsPaginated(arrangement.type) }
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    private fun List<MeetingListItem>.meetingItem() = meetingItems().single()
    private fun List<MeetingListItem>.meetingItems() = filterIsInstance<MeetingItem>()
    private fun MeetingItem.ongoingStatus() = status as MeetingItem.Status.Ongoing
    private suspend fun PagingData<MeetingListItem>.items(): List<MeetingListItem> = flowOf(this).asSnapshot()
    private fun meeting(
        meetingId: MeetingId = MeetingId("meeting-id", "domain"),
        startTime: Instant,
        conversationId: ConversationId = ConversationId("conversation-id", "domain"),
    ) = MeetingOccurrence(
        meeting = Meeting(
            meetingId = meetingId,
            conversationId = conversationId,
            creatorId = UserId("creator_id", "domain"),
            title = "Meeting",
            startTime = startTime,
            endTime = startTime + 30.minutes,
            tzid = "Europe/Berlin",
            recurrence = null,
        ),
        occurrenceId = "$meetingId-occurrence",
        conversationName = "Meeting",
        conversationType = MeetingOccurrence.ConversationType.Group,
        occurrenceStartTime = startTime,
        occurrenceEndTime = startTime + 30.minutes,
        selfRole = MeetingOccurrence.SelfRole.Creator,
    )
    private fun call(
        conversationId: ConversationId,
        status: CallStatus = CallStatus.ESTABLISHED,
        establishedTime: Instant? = null,
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

    private class Arrangement(
        private val dispatcher: TestDispatcher,
    ) {
        val type = MeetingsTabItem.NEXT
        val systemTimeChanges = MutableSharedFlow<Unit>()
        val minuteTicks = MutableSharedFlow<Unit>()

        @MockK
        lateinit var systemTimeObserver: SystemTimeObserver
        var currentTimeZoneProvider = CurrentTimeZoneProvider { TimeZone.UTC }
        var currentTimeProvider = CurrentTimeProvider {
            Instant.fromEpochMilliseconds(dispatcher.scheduler.currentTime)
        }

        @MockK
        lateinit var getMeetingsPaginated: GetPaginatedFlowOfMeetingsUseCase

        @MockK
        lateinit var observeActiveCalls: ObserveActiveCallsUseCase

        init {
            MockKAnnotations.init(this)
            every { observeActiveCalls() } returns flowOf(emptyList())
            every { systemTimeObserver() } returns merge(systemTimeChanges, minuteTicks)
        }
        fun withTimeZoneProvider(timeZone: () -> TimeZone) = apply {
            currentTimeZoneProvider = CurrentTimeZoneProvider(timeZone)
        }
        fun withCurrentTimeProvider(currentTime: () -> Instant) = apply {
            currentTimeProvider = CurrentTimeProvider(currentTime)
        }
        fun withGetMeetingsPaginated(meetings: List<MeetingOccurrence>) = apply {
            coEvery { getMeetingsPaginated(type = type) } returns flowOf(
                PagingData.from(meetings)
            )
        }
        fun withObserveActiveCalls(activeCalls: List<Call>) = apply {
            every { observeActiveCalls() } returns flowOf(activeCalls)
        }
        fun arrange() = this to viewModelScenario {
            MeetingListViewModelImpl(
                type = type,
                dispatcher = TestDispatcherProvider(dispatcher),
                currentTimeProvider = currentTimeProvider,
                currentTimeZoneProvider = currentTimeZoneProvider,
                systemTimeObserver = systemTimeObserver,
                getMeetingsPaginated = getMeetingsPaginated,
                observeActiveCalls = observeActiveCalls,
            )
        }
    }
}
