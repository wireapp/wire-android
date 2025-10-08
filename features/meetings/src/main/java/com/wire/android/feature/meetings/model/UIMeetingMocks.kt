/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.meetings.model

import com.wire.android.feature.meetings.model.UIMeeting.BelongingType
import com.wire.android.feature.meetings.model.UIMeeting.Status
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.kalium.logic.data.id.ConversationId
import kotlin.time.Duration.Companion.minutes

val CurrentTimeScope.endedPrivateChannelMeeting
    get() = UIMeeting(
        meetingId = "id1",
        conversationId = ConversationId("cid1", "domain"),
        title = "Ended Private Channel Meeting",
        belongingType = BelongingType.Channel(name = "Private Channel Name", isPrivateChannel = true),
        status = Status.Ended(
            startedTime = currentTime().minus(90.minutes),
            endedTime = currentTime().minus(60.minutes)
        ),
    )

val CurrentTimeScope.ongoingAttendingOneOnOneMeeting
    get() = UIMeeting(
        meetingId = "id2",
        conversationId = ConversationId("cid2", "domain"),
        title = "Ongoing Attending 1:1 Meeting",
        belongingType = BelongingType.OneOnOne(username = "John Doe", avatar = UserAvatarData()),
        status = Status.Ongoing(
            startedTime = currentTime().minus(15.minutes),
            ongoingCallStatus = UIMeeting.OngoingCallStatus(
                currentCallStartedTime = currentTime().minus(15.minutes),
                isSelfUserAttending = true,
            )
        )
    )

val CurrentTimeScope.grouplessOngoingMeeting
    get() = UIMeeting(
        meetingId = "id3",
        conversationId = ConversationId("cid3", "domain"),
        title = "Groupless Ongoing Meeting",
        belongingType = BelongingType.Groupless(
            avatars = listOf(
                UserAvatarData(nameBasedAvatar = NameBasedAvatar(fullName = "Alice", accentColor = 1)),
                UserAvatarData(),
                UserAvatarData(nameBasedAvatar = NameBasedAvatar(fullName = "Bob", accentColor = 2)),
                UserAvatarData(),
                UserAvatarData(nameBasedAvatar = NameBasedAvatar(fullName = "Charlie", accentColor = 3)),
                UserAvatarData(),
                UserAvatarData(nameBasedAvatar = NameBasedAvatar(fullName = "Diana", accentColor = 4)),
            ),
            limit = 5
        ),
        status = Status.Ongoing(startedTime = currentTime().minus(10.minutes))
    )

val CurrentTimeScope.scheduledChannelMeetingStartingSoon
    get() = UIMeeting(
        meetingId = "id4",
        conversationId = ConversationId("cid4", "domain"),
        title = "Scheduled Channel Meeting Starting Soon",
        belongingType = BelongingType.Channel(name = "Channel Name", isPrivateChannel = false),
        status = Status.Scheduled(
            startTime = currentTime().plus(5.minutes),
            endTime = currentTime().plus(65.minutes),
        )
    )

val CurrentTimeScope.scheduledRepeatingGroupMeeting
    get() = UIMeeting(
        meetingId = "id5",
        conversationId = ConversationId("cid5", "domain"),
        title = "Scheduled Group Meeting",
        belongingType = BelongingType.Group(name = "Group Name"),
        status = Status.Scheduled(
            startTime = currentTime().plus(60.minutes),
            endTime = currentTime().plus(90.minutes),
            repeatingInterval = UIMeeting.RepeatingInterval.Weekly,
        )
    )
