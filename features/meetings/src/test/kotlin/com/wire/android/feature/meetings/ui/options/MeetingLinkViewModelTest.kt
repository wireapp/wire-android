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

import com.wire.android.feature.meetings.model.MeetingLink
import com.wire.android.feature.meetings.ui.link.MeetingLinkState
import com.wire.android.feature.meetings.ui.link.MeetingLinkViewModelImpl
import com.wire.kalium.logic.data.meeting.Meeting
import com.wire.kalium.logic.data.id.MeetingId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.meeting.ObserveMeetingUseCase
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeetingLinkViewModelTest {
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
        assertEquals(MeetingLinkState.Ready(MeetingLink(null)), viewModel.state)
    }

    @Test
    fun givenMeetingUpdates_thenStateUsesLatestValues() = runTest(dispatcher) {
        val (arrangement, viewModel) = Arrangement()
            .withObservedMeeting(meeting("first", false))
            .arrange()
        assertEquals(MeetingLinkState.Loading, viewModel.state)
        runCurrent()
        assertEquals(MeetingLinkState.Ready(MeetingLink("first", false)), viewModel.state)
        arrangement.withObservedMeeting(meeting("updated", true))
        runCurrent()
        assertEquals(MeetingLinkState.Ready(MeetingLink("updated", true)), viewModel.state)
    }

    @Test
    fun givenMeetingDeletedOrCreatorAccessLost_thenStateIsNotAvailable() = runTest(dispatcher) {
        val (arrangement, viewModel) = Arrangement()
            .withObservedMeeting(meeting("link", false))
            .arrange()
        runCurrent()
        arrangement.withObservedMeeting(null)
        runCurrent()
        assertEquals(MeetingLinkState.NotAvailable, viewModel.state)
        arrangement.withObservedMeeting(meeting("link", false))
        runCurrent()
        assertEquals(MeetingLinkState.Ready(MeetingLink("link", false)), viewModel.state)
        arrangement.withObservedMeeting(meeting("link", false, UserId("other", "wire.test")))
        runCurrent()
        assertEquals(MeetingLinkState.NotAvailable, viewModel.state)
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

        private val observedMeeting = MutableStateFlow<Meeting?>(null)

        init {
            MockKAnnotations.init(this)
            coEvery { observeMeetingUseCase.invoke(MEETING_ID) } returns observedMeeting
        }

        fun withObservedMeeting(meeting: Meeting?) = apply {
            observedMeeting.value = meeting
        }

        fun arrange() = this to MeetingLinkViewModelImpl(
            meetingId = MEETING_ID,
            observeMeeting = observeMeetingUseCase,
            selfUserId = SELF_ID,
        )
    }

    private companion object {
        val MEETING_ID = MeetingId("meeting-id", "wire.test")
        val SELF_ID = UserId("self", "wire.test")
    }
}
