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
import com.wire.kalium.logic.data.id.ConversationId
import io.mockk.MockKAnnotations
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private fun List<MeetingListItem>.meetingItem() = filterIsInstance<MeetingItem>().single()
    private suspend fun PagingData<MeetingListItem>.items(): List<MeetingListItem> = flowOf(this).asSnapshot()
    private fun meeting(meetingId: String = "meeting-id", startTime: Instant) = Meeting(
        meetingId = meetingId,
        conversationId = ConversationId("conversation-id", "domain"),
        belongingType = MeetingItem.BelongingType.Group("group"),
        title = "Meeting",
        startTime = startTime,
        endTime = startTime + 30.minutes,
        repeatingInterval = null,
        ongoingCallStatus = null,
        selfRole = MeetingItem.SelfRole.Admin,
    )

    private class Arrangement(private val dispatcher: TestDispatcher) {
        val type = MeetingsTabItem.NEXT
        var currentTimeProvider = CurrentTimeProvider {
            Instant.fromEpochMilliseconds(dispatcher.scheduler.currentTime)
        }
        @MockK
        lateinit var getMeetingsPaginated: GetMeetingsPaginatedUseCase

        init {
            MockKAnnotations.init(this)
        }
        fun withCurrentTimeProvider(currentTime: () -> Instant) = apply {
            currentTimeProvider = CurrentTimeProvider(currentTime)
        }
        fun withGetMeetingsPaginated(showingAll: Boolean, meetings: List<Meeting>) = apply {
            every { getMeetingsPaginated(showingAll = showingAll, type = type) } returns flowOf(
                PagingData.from(meetings)
            )
        }
        fun arrange() = this to MeetingListViewModelImpl(
            type = type,
            dispatcher = TestDispatcherProvider(dispatcher),
            currentTimeProvider = currentTimeProvider,
            getMeetingsPaginated = getMeetingsPaginated,
        )
    }
}
