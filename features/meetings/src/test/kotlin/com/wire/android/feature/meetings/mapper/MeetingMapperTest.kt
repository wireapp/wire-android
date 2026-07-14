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
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.MeetingId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.meeting.MeetingOccurrence
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
        val result = meeting(startTime = startTime, endTime = endTime).toMeetingItem(time = currentTime, ongoingCallStatus = null)
        assertEquals(expected, result)
    }

    @Test
    fun givenMeetingEndedWithinBuffer_whenMappingToMeetingItem_thenStatusIsOngoing() {
        val startTime = currentTime - 30.minutes
        val endTime = currentTime - 5.minutes
        val expected = meetingItem(Ongoing(startTime = startTime, scheduledEndTime = endTime, ongoingCallStatus = ongoingCall))
        val result = meeting(startTime = startTime, endTime = endTime).toMeetingItem(time = currentTime, ongoingCallStatus = ongoingCall)
        assertEquals(expected, result)
    }

    @Test
    fun givenMeetingHasNoEndTime_whenMappingToMeetingItem_thenStatusIsOngoing() {
        val startTime = currentTime - 30.minutes
        val expected = meetingItem(Ongoing(startTime = startTime, scheduledEndTime = null, ongoingCallStatus = ongoingCall))
        val result = meeting(startTime = startTime, endTime = null).toMeetingItem(time = currentTime, ongoingCallStatus = ongoingCall)
        assertEquals(expected, result)
    }

    @Test
    fun givenMeetingEndedAfterBuffer_whenMappingToMeetingItem_thenStatusIsEnded() {
        val startTime = currentTime - 60.minutes
        val endTime = currentTime - 10.minutes
        val expected = meetingItem(Ended(startTime = startTime, endTime = endTime))
        val result = meeting(startTime = startTime, endTime = endTime).toMeetingItem(time = currentTime, ongoingCallStatus = ongoingCall)
        assertEquals(expected, result)
    }

    @Test
    fun givenCallHasEstablishedTime_whenMappingToOngoingCallStatus_thenCurrentCallEstablishedTimeIsMapped() {
        val expected = OngoingCallStatus(currentCallEstablishedTime = currentTime - 20.minutes, isSelfUserAttending = true)
        val result = call(establishedTime = ESTABLISHED_TIME).toOngoingCallStatus()
        assertEquals(expected, result)
    }

    @Test
    fun givenCallHasNoEstablishedTime_whenMappingToOngoingCallStatus_thenCurrentCallEstablishedTimeIsNull() {
        val expected = OngoingCallStatus(currentCallEstablishedTime = null, isSelfUserAttending = true)
        val result = call(establishedTime = null).toOngoingCallStatus()
        assertEquals(expected, result)
    }

    @Test
    fun givenActiveCallStatusMeaningSelfUserIsAttending_whenMappingToOngoingCallStatus_thenSelfUserIsAttending() {
        // for these three statuses, the self user always is participating in the call
        listOf(CallStatus.STARTED, CallStatus.ANSWERED, CallStatus.ESTABLISHED).forEach { callStatus ->
            val expected = OngoingCallStatus(currentCallEstablishedTime = currentTime - 20.minutes, isSelfUserAttending = true)
            val result = call(status = callStatus).toOngoingCallStatus()
            assertEquals(expected, result, "Failed for $callStatus")
        }
    }

    @Test
    fun givenActiveCallStatusMeaningSelfUserIsNotAttending_whenMappingToOngoingCallStatus_thenSelfUserIsNotAttending() {
        // for these two statuses, the self user is not participating in the call but the call is still ongoing, meaning the user can join
        listOf(CallStatus.INCOMING, CallStatus.STILL_ONGOING).forEach { callStatus ->
            val expected = OngoingCallStatus(currentCallEstablishedTime = currentTime - 20.minutes, isSelfUserAttending = false)
            val result = call(status = callStatus).toOngoingCallStatus()
            assertEquals(expected, result, "Failed for $callStatus")
        }
    }

    @Test
    fun givenClosedCallStatus_whenMappingToOngoingCallStatus_thenStatusIsNull() {
        // for these statuses, the call is closed, meaning it's not ongoing and the user cannot join anymore
        listOf(CallStatus.MISSED, CallStatus.CLOSED_INTERNALLY, CallStatus.CLOSED, CallStatus.REJECTED).forEach { callStatus ->
            val result = call(status = callStatus).toOngoingCallStatus()
            assertEquals(null, result, "Failed for $callStatus")
        }
    }

    private fun meeting(startTime: Instant, endTime: Instant?) = MeetingOccurrence(
        occurrenceId = "$MEETING_ID-occurrence",
        meetingId = MEETING_ID,
        conversationId = CONVERSATION_ID,
        conversationName = TITLE,
        conversationType = CONVERSATION_TYPE,
        title = TITLE,
        startTime = startTime,
        endTime = endTime,
        occurrenceStartTime = startTime,
        occurrenceEndTime = endTime,
        recurrence = MeetingOccurrence.Recurrence(frequency = MeetingOccurrence.Recurrence.Frequency.WEEKLY, interval = 1L, until = null),
        selfRole = MeetingOccurrence.SelfRole.Creator,
    )

    private fun meetingItem(status: MeetingItem.Status) = MeetingItem(
        occurrenceId = "$MEETING_ID-occurrence",
        meetingId = MEETING_ID,
        conversationId = CONVERSATION_ID,
        belongingType = BELONGING_TYPE,
        repeatingInterval = RepeatingInterval.Weekly,
        title = TITLE,
        status = status,
        selfRole = SelfRole.Creator,
    )

    private fun call(
        status: CallStatus = CallStatus.ESTABLISHED,
        establishedTime: String? = ESTABLISHED_TIME
    ) = Call(
        conversationId = CONVERSATION_ID,
        status = status,
        isMuted = false,
        isCameraOn = true,
        isCbrEnabled = false,
        callerId = QualifiedID("some_id", "some_domain"),
        conversationName = "some_name",
        conversationType = Conversation.Type.Group.Regular,
        callerName = "some_name",
        callerTeamName = "some_team_name",
        establishedTime = establishedTime,
    )

    private companion object {
        val MEETING_ID = MeetingId(value = "meeting-id", domain = "wire.com")
        const val TITLE = "Engineering sync"
        const val ESTABLISHED_TIME = "2026-01-01T11:40:00.000Z"
        val currentTime = Instant.parse("2026-01-01T12:00:00Z")
        val CONVERSATION_ID = ConversationId(value = "conversation-id", domain = "wire.com")
        val BELONGING_TYPE = BelongingType.Group(name = TITLE)
        val CONVERSATION_TYPE = MeetingOccurrence.ConversationType.Group
        val ongoingCall = OngoingCallStatus(currentCallEstablishedTime = currentTime - 20.minutes, isSelfUserAttending = true)
    }
}
