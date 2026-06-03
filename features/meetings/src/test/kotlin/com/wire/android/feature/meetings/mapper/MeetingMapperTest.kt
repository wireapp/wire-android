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
package com.wire.android.feature.meetings.mapper

import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.feature.meetings.model.MeetingItem.BelongingType
import com.wire.android.feature.meetings.model.MeetingItem.OngoingCallStatus
import com.wire.android.feature.meetings.model.MeetingItem.RepeatingInterval
import com.wire.android.feature.meetings.model.MeetingItem.SelfRole
import com.wire.android.feature.meetings.model.MeetingItem.Status.Ended
import com.wire.android.feature.meetings.model.MeetingItem.Status.Ongoing
import com.wire.android.feature.meetings.model.MeetingItem.Status.Scheduled
import com.wire.android.feature.meetings.ui.mock.Meeting
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class MeetingMapperTest {

    @Test
    fun givenMeetingStartsInFuture_whenMappingToMeetingItem_thenStatusIsScheduled() {
        val startTime = currentTime + 10.minutes
        val endTime = currentTime + 40.minutes
        val expected = meetingItem(Scheduled(startTime = startTime, endTime = endTime))
        val result = meeting(startTime = startTime, endTime = endTime).toMeetingItem(currentTime)
        assertEquals(expected, result)
    }

    @Test
    fun givenMeetingEndedWithinBuffer_whenMappingToMeetingItem_thenStatusIsOngoing() {
        val startTime = currentTime - 30.minutes
        val endTime = currentTime - 5.minutes
        val expected = meetingItem(Ongoing(startTime = startTime, scheduledEndTime = endTime, ongoingCallStatus = ongoingCall))
        val result = meeting(startTime = startTime, endTime = endTime, ongoingCallStatus = ongoingCall).toMeetingItem(currentTime)
        assertEquals(expected, result)
    }

    @Test
    fun givenMeetingHasNoEndTime_whenMappingToMeetingItem_thenStatusIsOngoing() {
        val startTime = currentTime - 30.minutes
        val expected = meetingItem(Ongoing(startTime = startTime, scheduledEndTime = null, ongoingCallStatus = ongoingCall))
        val result = meeting(startTime = startTime, endTime = null, ongoingCallStatus = ongoingCall).toMeetingItem(currentTime)
        assertEquals(expected, result)
    }

    @Test
    fun givenMeetingEndedAfterBuffer_whenMappingToMeetingItem_thenStatusIsEnded() {
        val startTime = currentTime - 60.minutes
        val endTime = currentTime - 10.minutes
        val expected = meetingItem(Ended(startTime = startTime, endTime = endTime))
        val result = meeting(startTime = startTime, endTime = endTime).toMeetingItem(currentTime)
        assertEquals(expected, result)
    }

    @Test
    fun givenScheduledMeetingItem_whenMappingToMeeting_thenMeetingHasScheduledEndTime() {
        val startTime = currentTime + 10.minutes
        val endTime = currentTime + 40.minutes
        val expected = meeting(startTime = startTime, endTime = endTime)
        val result = meetingItem(Scheduled(startTime = startTime, endTime = endTime)).toMeeting()
        assertEquals(expected, result)
    }

    @Test
    fun givenOngoingMeetingItem_whenMappingToMeeting_thenMeetingHasScheduledEndTimeAndCallStatus() {
        val startTime = currentTime - 30.minutes
        val endTime = currentTime + 30.minutes
        val expected = meeting(startTime = startTime, endTime = endTime, ongoingCallStatus = ongoingCall)
        val result = meetingItem(Ongoing(startTime = startTime, scheduledEndTime = endTime, ongoingCallStatus = ongoingCall)).toMeeting()
        assertEquals(expected, result)
    }

    @Test
    fun givenEndedMeetingItem_whenMappingToMeeting_thenMeetingHasEndedTime() {
        val startTime = currentTime - 60.minutes
        val endTime = currentTime - 10.minutes
        val expected = meeting(startTime = startTime, endTime = endTime)
        val result = meetingItem(Ended(startTime = startTime, endTime = endTime)).toMeeting()
        assertEquals(expected, result)
    }

    private fun meeting(startTime: Instant, endTime: Instant?, ongoingCallStatus: OngoingCallStatus? = null) = Meeting(
        meetingId = MEETING_ID,
        conversationId = CONVERSATION_ID,
        belongingType = BELONGING_TYPE,
        title = TITLE,
        startTime = startTime,
        endTime = endTime,
        repeatingInterval = RepeatingInterval.Weekly,
        ongoingCallStatus = ongoingCallStatus,
        selfRole = SelfRole.Admin,
    )

    private fun meetingItem(status: MeetingItem.Status) = MeetingItem(
        meetingId = MEETING_ID,
        conversationId = CONVERSATION_ID,
        belongingType = BELONGING_TYPE,
        repeatingInterval = RepeatingInterval.Weekly,
        title = TITLE,
        status = status,
        selfRole = SelfRole.Admin,
    )

    private companion object {
        const val MEETING_ID = "meeting-id"
        const val TITLE = "Engineering sync"
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val CONVERSATION_ID = ConversationId(value = "conversation-id", domain = "wire.com")
        val BELONGING_TYPE = BelongingType.Group(name = "Engineering")
        val ongoingCall = OngoingCallStatus(currentCallStartedTime = currentTime - 20.minutes, isSelfUserAttending = true)
    }
}
