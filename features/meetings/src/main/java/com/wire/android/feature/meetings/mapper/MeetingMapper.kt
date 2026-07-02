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
import com.wire.android.feature.meetings.model.MeetingItem.Status
import com.wire.android.feature.meetings.ui.mock.Meeting
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.util.DateTimeUtil
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

private val BUFFER_TIME = 5.minutes

fun Meeting.toMeetingItem(time: Instant, ongoingCallStatus: MeetingItem.OngoingCallStatus?): MeetingItem = MeetingItem(
    meetingId = meetingId,
    conversationId = conversationId,
    belongingType = belongingType,
    repeatingInterval = repeatingInterval,
    title = title,
    status = when {
        startTime > time && endTime != null -> Status.Scheduled(
            startTime = startTime,
            endTime = endTime
        )

        startTime < time && endTime != null && endTime + BUFFER_TIME < time -> Status.Ended(
            startTime = startTime,
            endTime = endTime
        )

        else -> Status.Ongoing(
            startTime = startTime,
            scheduledEndTime = endTime,
            ongoingCallStatus = ongoingCallStatus
        )
    },
    selfRole = selfRole,
)

fun Call.toOngoingCallStatus() = when (status) {
    CallStatus.STARTED,
    CallStatus.ANSWERED,
    CallStatus.ESTABLISHED,
    CallStatus.INCOMING,
    CallStatus.STILL_ONGOING -> MeetingItem.OngoingCallStatus(
        currentCallEstablishedTime = establishedTime?.let {
            Instant.fromEpochMilliseconds(DateTimeUtil.fromIsoDateTimeStringToEpochMillis(it))
        },
        isSelfUserAttending = status in listOf(CallStatus.STARTED, CallStatus.ANSWERED, CallStatus.ESTABLISHED),
    )
    else -> null // only calls in these states above are considered ongoing, other statuses mean the call is closed
}

fun MeetingItem.toMeeting(): Meeting = Meeting(
    meetingId = meetingId,
    conversationId = conversationId,
    belongingType = belongingType,
    title = title,
    startTime = status.startTime,
    endTime = when (status) {
        is Status.Scheduled -> status.endTime
        is Status.Ongoing -> status.scheduledEndTime
        is Status.Ended -> status.endTime
    },
    repeatingInterval = repeatingInterval,
    selfRole = selfRole
)
