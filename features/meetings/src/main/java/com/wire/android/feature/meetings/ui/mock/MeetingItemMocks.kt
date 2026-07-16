@file:Suppress("MagicNumber")
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
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.util.CurrentTimeProvider
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.MeetingId
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.minus
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

val CurrentTimeProvider.endedPrivateChannelMeeting
    get() = MeetingItem(
        occurrenceId = "oid1",
        meetingId = MeetingId("id1", "domain"),
        conversationId = ConversationId("cid1", "domain"),
        title = "Ended Private Channel Meeting",
        belongingType = BelongingType.Channel(name = "Private Channel Name", isPrivateChannel = true),
        repeatingInterval = MeetingItem.RepeatingInterval.Never,
        selfRole = MeetingItem.SelfRole.Creator,
        status = Status.Ended(
            startTime = currentTime().fullMinutes().minus(1.days).minus(120.minutes),
            endTime = currentTime().fullMinutes().minus(1.days).minus(90.minutes),
        ),
    )

val CurrentTimeProvider.ongoingAttendingOneOnOneMeeting
    get() = MeetingItem(
        occurrenceId = "oid2",
        meetingId = MeetingId("id2", "domain"),
        conversationId = ConversationId("cid2", "domain"),
        title = "Ongoing Attending 1:1 Meeting",
        belongingType = BelongingType.OneOnOne(username = "John Doe", avatar = UserAvatarData()),
        repeatingInterval = MeetingItem.RepeatingInterval.Never,
        selfRole = MeetingItem.SelfRole.Creator,
        status = Status.Ongoing(
            startTime = currentTime().fullMinutes().minus(15.minutes),
            scheduledEndTime = currentTime().fullMinutes().plus(45.minutes),
            ongoingCallStatus = MeetingItem.OngoingCallStatus(
                currentCallEstablishedTime = currentTime().fullMinutes().minus(15.minutes),
                isSelfUserAttending = true,
            )
        )
    )

private val avatars = persistentListOf(
    UserAvatarData(nameBasedAvatar = NameBasedAvatar(fullName = "Alice", accentColor = 1)),
    UserAvatarData(),
    UserAvatarData(nameBasedAvatar = NameBasedAvatar(fullName = "Bob", accentColor = 2)),
    UserAvatarData(),
    UserAvatarData(nameBasedAvatar = NameBasedAvatar(fullName = "Charlie", accentColor = 3)),
    UserAvatarData(),
    UserAvatarData(nameBasedAvatar = NameBasedAvatar(fullName = "Diana", accentColor = 4)),
)

val CurrentTimeProvider.grouplessOngoingMeeting
    get() = MeetingItem(
        occurrenceId = "oid3",
        meetingId = MeetingId("id3", "domain"),
        conversationId = ConversationId("cid3", "domain"),
        title = "Groupless Ongoing Meeting",
        repeatingInterval = MeetingItem.RepeatingInterval.Never,
        belongingType = BelongingType.Groupless(avatars = avatars, limit = 5),
        selfRole = MeetingItem.SelfRole.Creator,
        status = Status.Ongoing(
            startTime = currentTime().fullMinutes().minus(10.minutes),
            scheduledEndTime = currentTime().fullMinutes().plus(1.minutes)
        ),
    )

val CurrentTimeProvider.scheduledChannelMeetingStartingSoon
    get() = MeetingItem(
        occurrenceId = "oid4",
        meetingId = MeetingId("id4", "domain"),
        conversationId = ConversationId("cid4", "domain"),
        title = "Scheduled Channel Meeting Starting Soon",
        repeatingInterval = MeetingItem.RepeatingInterval.Never,
        belongingType = BelongingType.Channel(name = "Channel Name", isPrivateChannel = false),
        selfRole = MeetingItem.SelfRole.Creator,
        status = Status.Scheduled(
            startTime = currentTime().fullMinutes().plus(1.minutes),
            endTime = currentTime().fullMinutes().plus(65.minutes),
        )
    )

val CurrentTimeProvider.scheduledRepeatingGroupMeeting
    get() = MeetingItem(
        occurrenceId = "oid5",
        meetingId = MeetingId("id5", "domain"),
        conversationId = ConversationId("cid5", "domain"),
        title = "Scheduled Group Meeting",
        repeatingInterval = MeetingItem.RepeatingInterval.Weekly,
        belongingType = BelongingType.Group(name = "Group Name"),
        selfRole = MeetingItem.SelfRole.Creator,
        status = Status.Scheduled(
            startTime = currentTime().fullMinutes().plus(1.days).plus(60.minutes),
            endTime = currentTime().fullMinutes().plus(1.days).plus(90.minutes),
        )
    )

val CurrentTimeProvider.pastMeetingMocks
    get() = listOf(
        endedPrivateChannelMeeting,
        MeetingItem(
            occurrenceId = "poid1",
            meetingId = MeetingId("past1", "domain"),
            conversationId = ConversationId("cid", "domain"),
            title = "Ended Groupless Meeting",
            repeatingInterval = MeetingItem.RepeatingInterval.Never,
            belongingType = BelongingType.Groupless(avatars = avatars, limit = 5),
            selfRole = MeetingItem.SelfRole.Creator,
            status = Status.Ended(
                startTime = currentTime().fullMinutes().minus(1.days).minus(120.minutes),
                endTime = currentTime().fullMinutes().minus(1.days).minus(60.minutes),
            ),
        ),
        MeetingItem(
            occurrenceId = "poid2",
            meetingId = MeetingId("past2", "domain"),
            conversationId = ConversationId("cid", "domain"),
            title = "Ended Channel Meeting",
            repeatingInterval = MeetingItem.RepeatingInterval.Never,
            belongingType = BelongingType.Channel(name = "Channel Name", isPrivateChannel = false),
            selfRole = MeetingItem.SelfRole.Creator,
            status = Status.Ended(
                startTime = currentTime().fullMinutes().minus(1.days).minus(60.minutes),
                endTime = currentTime().fullMinutes().minus(1.days).minus(30.minutes),
            ),
        ),
        MeetingItem(
            occurrenceId = "poid3",
            meetingId = MeetingId("past3", "domain"),
            conversationId = ConversationId("cid", "domain"),
            title = "Ended 1:1 Meeting",
            repeatingInterval = MeetingItem.RepeatingInterval.Never,
            belongingType = BelongingType.OneOnOne(username = "John Doe", avatar = UserAvatarData()),
            selfRole = MeetingItem.SelfRole.Creator,
            status = Status.Ended(
                startTime = currentTime().fullMinutes().minus(1.days).minus(30.minutes),
                endTime = currentTime().fullMinutes().minus(1.days),
            ),
        ),
        MeetingItem(
            occurrenceId = "poid4",
            meetingId = MeetingId("past4", "domain"),
            conversationId = ConversationId("cid", "domain"),
            title = "Ended Group Meeting",
            repeatingInterval = MeetingItem.RepeatingInterval.Never,
            belongingType = BelongingType.Group(name = "Group Name"),
            selfRole = MeetingItem.SelfRole.Creator,
            status = Status.Ended(
                startTime = currentTime().fullMinutes().minus(2.days).minus(120.minutes),
                endTime = currentTime().fullMinutes().minus(2.days).minus(90.minutes),
            ),
        ),
        MeetingItem(
            occurrenceId = "poid5",
            meetingId = MeetingId("past5", "domain"),
            conversationId = ConversationId("cid", "domain"),
            title = "Ended Channel Meeting",
            repeatingInterval = MeetingItem.RepeatingInterval.Never,
            belongingType = BelongingType.Channel(name = "Channel Name", isPrivateChannel = true),
            selfRole = MeetingItem.SelfRole.Creator,
            status = Status.Ended(
                startTime = currentTime().fullMinutes().minus(2.days).minus(60.minutes),
                endTime = currentTime().fullMinutes().minus(2.days),
            ),
        ),
        MeetingItem(
            occurrenceId = "poid6",
            meetingId = MeetingId("past6", "domain"),
            conversationId = ConversationId("cid", "domain"),
            title = "Ended Groupless Meeting",
            repeatingInterval = MeetingItem.RepeatingInterval.Never,
            belongingType = BelongingType.Groupless(avatars = avatars.take(2).toPersistentList(), limit = 5),
            selfRole = MeetingItem.SelfRole.Creator,
            status = Status.Scheduled(
                startTime = currentTime().fullMinutes().minus(3.days).minus(60.minutes),
                endTime = currentTime().fullMinutes().minus(3.days),
            ),
        )
    )

val CurrentTimeProvider.nextMeetingMocks
    get() = listOf(
        ongoingAttendingOneOnOneMeeting,
        grouplessOngoingMeeting,
        scheduledChannelMeetingStartingSoon,
        scheduledRepeatingGroupMeeting,
        MeetingItem(
            occurrenceId = "onext2",
            meetingId = MeetingId("next2", "domain"),
            conversationId = ConversationId("cid", "domain"),
            title = "Scheduled 1:1 Meeting",
            repeatingInterval = MeetingItem.RepeatingInterval.Every4Weeks,
            belongingType = BelongingType.OneOnOne(username = "John Doe", avatar = UserAvatarData()),
            selfRole = MeetingItem.SelfRole.Creator,
            status = Status.Scheduled(
                startTime = currentTime().fullMinutes().plus(2.days).minus(60.minutes),
                endTime = currentTime().fullMinutes().plus(2.days).minus(30.minutes),
            ),
        ),
        MeetingItem(
            occurrenceId = "onext3",
            meetingId = MeetingId("next3", "domain"),
            conversationId = ConversationId("cid", "domain"),
            title = "Scheduled Private Channel Meeting",
            repeatingInterval = MeetingItem.RepeatingInterval.Every4Weeks,
            belongingType = BelongingType.Channel(name = "Channel Name", isPrivateChannel = true),
            selfRole = MeetingItem.SelfRole.Creator,
            status = Status.Scheduled(
                startTime = currentTime().fullMinutes().plus(2.days).minus(60.minutes),
                endTime = currentTime().fullMinutes().plus(2.days),
            ),
        ),
        MeetingItem(
            occurrenceId = "onext4",
            meetingId = MeetingId("next4", "domain"),
            conversationId = ConversationId("cid", "domain"),
            title = "Scheduled Groupless Meeting",
            repeatingInterval = MeetingItem.RepeatingInterval.Never,
            belongingType = BelongingType.Groupless(avatars = avatars.take(2).toPersistentList(), limit = 5),
            selfRole = MeetingItem.SelfRole.Creator,
            status = Status.Scheduled(
                startTime = currentTime().fullMinutes().plus(3.days).minus(60.minutes),
                endTime = currentTime().fullMinutes().plus(3.days),
            ),
        )
    )

class MeetingMocksProvider(val currentTimeProvider: CurrentTimeProvider) {
    fun getItems(type: MeetingsTabItem) = when (type) {
        MeetingsTabItem.PAST -> currentTimeProvider.pastMeetingMocks
        MeetingsTabItem.NEXT -> currentTimeProvider.nextMeetingMocks
    }

    fun getItem(occurrenceId: String): MeetingItem? =
        (currentTimeProvider.pastMeetingMocks + currentTimeProvider.nextMeetingMocks).find { it.occurrenceId == occurrenceId }

    companion object {
        val Default by lazy {
            MeetingMocksProvider(CurrentTimeProvider.Default) // time initialized when accessed for the first time
        }
    }
}

// remove seconds and milliseconds from Instant for better readability in the UI
private fun Instant.fullMinutes() = this.minus(this.toEpochMilliseconds() % 60_000, DateTimeUnit.MILLISECOND)
