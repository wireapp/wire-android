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
package com.wire.android.feature.meetings.ui.mock

import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.feature.meetings.model.MeetingItem.BelongingType
import com.wire.android.feature.meetings.model.MeetingItem.Status
import com.wire.android.feature.meetings.ui.MeetingsTabItem
import com.wire.android.feature.meetings.ui.util.CurrentTimeScope
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

val CurrentTimeScope.endedPrivateChannelMeeting
    get() = MeetingItem(
        meetingId = "id1",
        conversationId = ConversationId("cid1", "domain"),
        title = "Ended Private Channel Meeting",
        belongingType = BelongingType.Channel(name = "Private Channel Name", isPrivateChannel = true),
        status = Status.Ended(
            startTime = currentTime().minus(1.days).minus(120.minutes),
            endTime = currentTime().minus(1.days).minus(90.minutes),
        ),
    )

val CurrentTimeScope.ongoingAttendingOneOnOneMeeting
    get() = MeetingItem(
        meetingId = "id2",
        conversationId = ConversationId("cid2", "domain"),
        title = "Ongoing Attending 1:1 Meeting",
        belongingType = BelongingType.OneOnOne(username = "John Doe", avatar = UserAvatarData()),
        status = Status.Ongoing(
            startTime = currentTime().minus(15.minutes),
            ongoingCallStatus = MeetingItem.OngoingCallStatus(
                currentCallStartedTime = currentTime().minus(15.minutes),
                isSelfUserAttending = true,
            )
        )
    )

private val avatars = listOf(
    UserAvatarData(nameBasedAvatar = NameBasedAvatar(fullName = "Alice", accentColor = 1)),
    UserAvatarData(),
    UserAvatarData(nameBasedAvatar = NameBasedAvatar(fullName = "Bob", accentColor = 2)),
    UserAvatarData(),
    UserAvatarData(nameBasedAvatar = NameBasedAvatar(fullName = "Charlie", accentColor = 3)),
    UserAvatarData(),
    UserAvatarData(nameBasedAvatar = NameBasedAvatar(fullName = "Diana", accentColor = 4)),
)

val CurrentTimeScope.grouplessOngoingMeeting
    get() = MeetingItem(
        meetingId = "id3",
        conversationId = ConversationId("cid3", "domain"),
        title = "Groupless Ongoing Meeting",
        belongingType = BelongingType.Groupless(avatars = avatars, limit = 5),
        status = Status.Ongoing(startTime = currentTime().minus(10.minutes)),
    )

val CurrentTimeScope.scheduledChannelMeetingStartingSoon
    get() = MeetingItem(
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
    get() = MeetingItem(
        meetingId = "id5",
        conversationId = ConversationId("cid5", "domain"),
        title = "Scheduled Group Meeting",
        belongingType = BelongingType.Group(name = "Group Name"),
        status = Status.Scheduled(
            startTime = currentTime().plus(1.days).plus(60.minutes),
            endTime = currentTime().plus(1.days).plus(90.minutes),
            repeatingInterval = MeetingItem.RepeatingInterval.Weekly,
        )
    )

val CurrentTimeScope.pastMeetingMocks
    get() = listOf(
        endedPrivateChannelMeeting,
        MeetingItem(
            meetingId = "past1",
            conversationId = ConversationId("cid", "domain"),
            title = "Ended Groupless Meeting",
            belongingType = BelongingType.Groupless(avatars = avatars, limit = 5),
            status = Status.Ended(
                startTime = currentTime().minus(1.days).minus(120.minutes),
                endTime = currentTime().minus(1.days).minus(60.minutes),
            ),
        ),
        MeetingItem(
            meetingId = "past2",
            conversationId = ConversationId("cid", "domain"),
            title = "Ended Channel Meeting",
            belongingType = BelongingType.Channel(name = "Channel Name", isPrivateChannel = false),
            status = Status.Ended(
                startTime = currentTime().minus(1.days).minus(60.minutes),
                endTime = currentTime().minus(1.days).minus(30.minutes),
            ),
        ),
        MeetingItem(
            meetingId = "past3",
            conversationId = ConversationId("cid", "domain"),
            title = "Ended 1:1 Meeting",
            belongingType = BelongingType.OneOnOne(username = "John Doe", avatar = UserAvatarData()),
            status = Status.Ended(
                startTime = currentTime().minus(1.days).minus(30.minutes),
                endTime = currentTime().minus(1.days),
            ),
        ),
        MeetingItem(
            meetingId = "past4",
            conversationId = ConversationId("cid", "domain"),
            title = "Ended Group Meeting",
            belongingType = BelongingType.Group(name = "Group Name"),
            status = Status.Ended(
                startTime = currentTime().minus(2.days).minus(120.minutes),
                endTime = currentTime().minus(2.days).minus(90.minutes),
            ),
        ),
        MeetingItem(
            meetingId = "past5",
            conversationId = ConversationId("cid", "domain"),
            title = "Ended Channel Meeting",
            belongingType = BelongingType.Channel(name = "Channel Name", isPrivateChannel = true),
            status = Status.Ended(
                startTime = currentTime().minus(2.days).minus(60.minutes),
                endTime = currentTime().minus(2.days),
            ),
        ),
        MeetingItem(
            meetingId = "past6",
            conversationId = ConversationId("cid", "domain"),
            title = "Ended Groupless Meeting",
            belongingType = BelongingType.Groupless(avatars = avatars.take(2), limit = 5),
            status = Status.Scheduled(
                startTime = currentTime().minus(3.days).minus(60.minutes),
                endTime = currentTime().minus(3.days),
            ),
        )
    )

val CurrentTimeScope.nextMeetingMocks
    get() = listOf(
        ongoingAttendingOneOnOneMeeting,
        grouplessOngoingMeeting,
        scheduledChannelMeetingStartingSoon,
        scheduledRepeatingGroupMeeting,
        MeetingItem(
            meetingId = "next2",
            conversationId = ConversationId("cid", "domain"),
            title = "Scheduled 1:1 Meeting",
            belongingType = BelongingType.OneOnOne(username = "John Doe", avatar = UserAvatarData()),
            status = Status.Scheduled(
                startTime = currentTime().plus(2.days).minus(60.minutes),
                endTime = currentTime().plus(2.days).minus(30.minutes),
                repeatingInterval = MeetingItem.RepeatingInterval.Monthly,
            ),
        ),
        MeetingItem(
            meetingId = "next3",
            conversationId = ConversationId("cid", "domain"),
            title = "Scheduled Private Channel Meeting",
            belongingType = BelongingType.Channel(name = "Channel Name", isPrivateChannel = true),
            status = Status.Scheduled(
                startTime = currentTime().plus(2.days).minus(60.minutes),
                endTime = currentTime().plus(2.days),
                repeatingInterval = MeetingItem.RepeatingInterval.Monthly,
            ),
        ),
        MeetingItem(
            meetingId = "next4",
            conversationId = ConversationId("cid", "domain"),
            title = "Scheduled Groupless Meeting",
            belongingType = BelongingType.Groupless(avatars = avatars.take(2), limit = 5),
            status = Status.Scheduled(
                startTime = currentTime().plus(3.days).minus(60.minutes),
                endTime = currentTime().plus(3.days),
            ),
        )
    )

fun CurrentTimeScope.meetingMocks(showingAll: Boolean, type: MeetingsTabItem) = when (type) {
    MeetingsTabItem.PAST -> pastMeetingMocks
    MeetingsTabItem.NEXT -> nextMeetingMocks
}.filter {
    val localDate = it.status.startTime.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val currentLocalDate = currentTime().toLocalDateTime(TimeZone.currentSystemDefault()).date
    when {
        !showingAll && type == MeetingsTabItem.PAST -> currentLocalDate.minus(1, DateTimeUnit.DAY) <= localDate
        !showingAll && type == MeetingsTabItem.NEXT -> currentLocalDate.plus(1, DateTimeUnit.DAY) >= localDate
        else -> true
    }
}

val CurrentTimeScope.meetingMocks
    get() = listOf(
        endedPrivateChannelMeeting,
        ongoingAttendingOneOnOneMeeting,
        grouplessOngoingMeeting,
        scheduledChannelMeetingStartingSoon,
        scheduledRepeatingGroupMeeting,
    )
