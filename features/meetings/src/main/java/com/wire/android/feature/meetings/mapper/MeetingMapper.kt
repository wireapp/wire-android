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
import com.wire.android.model.ImageAsset
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.meeting.Meeting
import com.wire.kalium.logic.data.meeting.MeetingOccurrence
import com.wire.kalium.logic.data.user.UserId
import kotlin.time.Duration.Companion.minutes
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.Instant

private val BUFFER_TIME = 5.minutes

fun MeetingOccurrence.toMeetingItem(
    time: Instant,
    ongoingCallStatus: MeetingItem.OngoingCallStatus?,
    selfUserId: UserId,
): MeetingItem = MeetingItem(
    occurrenceId = occurrenceId,
    meetingId = meeting.meetingId,
    conversationId = meeting.conversationId,
    belongingType = toBelongingType(selfUserId),
    repeatingInterval = meeting.recurrence?.toRepeatingInterval(),
    title = meeting.title,
    status = when {
        occurrenceStartTime > time -> Status.Scheduled(
            startTime = occurrenceStartTime,
            endTime = occurrenceEndTime
        )

        occurrenceStartTime < time && occurrenceEndTime + BUFFER_TIME < time -> Status.Ended(
            startTime = occurrenceStartTime,
            endTime = occurrenceEndTime
        )

        else -> Status.Ongoing(
            startTime = occurrenceStartTime,
            endTime = occurrenceEndTime,
            ongoingCallStatus = ongoingCallStatus
        )
    },
    selfRole = selfRole.toItemSelfRole()
)

fun Meeting.Recurrence.toRepeatingInterval(): MeetingItem.RepeatingInterval = MeetingItem.RepeatingInterval(frequency, interval.toInt())

fun MeetingOccurrence.SelfRole.toItemSelfRole(): MeetingItem.SelfRole = when (this) {
    MeetingOccurrence.SelfRole.Creator -> MeetingItem.SelfRole.Creator
    MeetingOccurrence.SelfRole.Member -> MeetingItem.SelfRole.Member
}

private fun MeetingOccurrence.toBelongingType(selfId: UserId): MeetingItem.BelongingType = when (val conversationType = conversationType) {
    is MeetingOccurrence.ConversationType.Meeting -> MeetingItem.BelongingType.Groupless(
        avatars = participants.sortedBy {
            when (it.userId) {
                meeting.creatorId -> 0
                selfId -> 1
                else -> 2
            }
        }.map(::toUserAvatarData).toImmutableList(),
    )

    is MeetingOccurrence.ConversationType.Group -> MeetingItem.BelongingType.Group(
        name = conversationName,
    )

    is MeetingOccurrence.ConversationType.Channel -> MeetingItem.BelongingType.Channel(
        name = conversationName,
        isPrivateChannel = conversationType.isPrivateChannel
    )

    is MeetingOccurrence.ConversationType.OneOnOne -> MeetingItem.BelongingType.OneOnOne(
        username = conversationName,
        avatar = participants.firstOrNull { it.userId != selfId }?.let(::toUserAvatarData) ?: UserAvatarData(),
    )
}

private fun toUserAvatarData(avatar: MeetingOccurrence.Participant): UserAvatarData = UserAvatarData(
    asset = avatar.assetId?.let { ImageAsset.UserAvatarAsset(it) },
    nameBasedAvatar = NameBasedAvatar(avatar.name, avatar.accentColor)
)

fun Call.toOngoingCallStatus() = when (status) {
    CallStatus.STARTED,
    CallStatus.ANSWERED,
    CallStatus.ESTABLISHED,
    CallStatus.INCOMING,
    CallStatus.STILL_ONGOING -> MeetingItem.OngoingCallStatus(
        currentCallEstablishedTime = establishedTime,
        isSelfUserAttending = status.isSelfUserAttending(),
    )

    else -> null // only calls in these states above are considered ongoing, other statuses mean the call is closed
}

fun CallStatus.isSelfUserAttending() = this in listOf(CallStatus.STARTED, CallStatus.ANSWERED, CallStatus.ESTABLISHED)
