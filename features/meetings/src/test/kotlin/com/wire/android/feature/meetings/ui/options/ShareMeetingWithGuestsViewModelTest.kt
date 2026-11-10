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

import com.wire.kalium.logic.data.meeting.MeetingOccurrence
import com.wire.kalium.logic.feature.meeting.ObserveMeetingOccurrenceUseCase
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
class ShareMeetingWithGuestsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun givenMeetingUpdates_thenStateUsesLatestValues() = runTest(dispatcher) {
        val (arrangement, viewModel) = Arrangement()
            .withObservedMeeting(meeting("first", false))
            .arrange()
        assertEquals(ShareMeetingWithGuestsState.Loading, viewModel.state)
        runCurrent()
        assertEquals(ShareMeetingWithGuestsState.Ready("first", false), viewModel.state)
        arrangement.withObservedMeeting(meeting("updated", true))
        runCurrent()
        assertEquals(ShareMeetingWithGuestsState.Ready("updated", true), viewModel.state)
    }

    @Test
    fun givenMeetingDeletedOrCreatorAccessLost_thenStateIsNotAvailable() = runTest(dispatcher) {
        val (arrangement, viewModel) = Arrangement()
            .withObservedMeeting(meeting("link", false))
            .arrange()
        runCurrent()
        arrangement.withObservedMeeting(null)
        runCurrent()
        assertEquals(ShareMeetingWithGuestsState.NotAvailable, viewModel.state)
        arrangement.withObservedMeeting(meeting("link", false))
        runCurrent()
        assertEquals(ShareMeetingWithGuestsState.Ready("link", false), viewModel.state)
        arrangement.withObservedMeeting(meeting("link", false, MeetingOccurrence.SelfRole.Member))
        runCurrent()
        assertEquals(ShareMeetingWithGuestsState.NotAvailable, viewModel.state)
    }

    private fun meeting(
        link: String?,
        hasPassword: Boolean,
        role: MeetingOccurrence.SelfRole = MeetingOccurrence.SelfRole.Creator,
    ): MeetingOccurrence = mockk {
        every { selfRole } returns role
        every { meeting.link } returns link
        every { meeting.hasPassword } returns hasPassword
    }

    private class Arrangement {
        @MockK
        lateinit var observeMeetingOccurrenceUseCase: ObserveMeetingOccurrenceUseCase

        private val observedMeeting = MutableStateFlow<MeetingOccurrence?>(null)

        init {
            MockKAnnotations.init(this)
            coEvery { observeMeetingOccurrenceUseCase.invoke(OCCURRENCE_ID) } returns observedMeeting
        }

        fun withObservedMeeting(meeting: MeetingOccurrence?) = apply {
            observedMeeting.value = meeting
        }

        fun arrange() = this to ShareMeetingWithGuestsViewModelImpl(
            occurrenceId = OCCURRENCE_ID,
            observeMeetingOccurrence = observeMeetingOccurrenceUseCase,
        )
    }

    private companion object {
        const val OCCURRENCE_ID = "occurrence-id"
    }
}
