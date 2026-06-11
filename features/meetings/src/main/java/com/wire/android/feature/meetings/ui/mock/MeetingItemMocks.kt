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

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.wire.android.feature.meetings.mapper.toMeeting
import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.feature.meetings.model.MeetingItem.BelongingType
import com.wire.android.feature.meetings.model.MeetingItem.SelfRole
import com.wire.android.feature.meetings.model.MeetingItem.Status
import com.wire.android.feature.meetings.ui.MeetingsTabItem
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.util.CurrentTimeProvider
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.min
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

val CurrentTimeProvider.endedPrivateChannelMeeting
    get() = MeetingItem(
        meetingId = "id1",
        conversationId = ConversationId("cid1", "domain"),
        title = "Ended Private Channel Meeting",
        belongingType = BelongingType.Channel(name = "Private Channel Name", isPrivateChannel = true),
        repeatingInterval = null,
        selfRole = MeetingItem.SelfRole.Admin,
        status = Status.Ended(
            startTime = currentTime().fullMinutes().minus(1.days).minus(120.minutes),
            endTime = currentTime().fullMinutes().minus(1.days).minus(90.minutes),
        ),
    )

val CurrentTimeProvider.ongoingAttendingOneOnOneMeeting
    get() = MeetingItem(
        meetingId = "id2",
        conversationId = ConversationId("cid2", "domain"),
        title = "Ongoing Attending 1:1 Meeting",
        belongingType = BelongingType.OneOnOne(username = "John Doe", avatar = UserAvatarData()),
        repeatingInterval = null,
        selfRole = MeetingItem.SelfRole.Admin,
        status = Status.Ongoing(
            startTime = currentTime().fullMinutes().minus(15.minutes),
            scheduledEndTime = currentTime().fullMinutes().plus(45.minutes),
            ongoingCallStatus = MeetingItem.OngoingCallStatus(
                currentCallStartedTime = currentTime().fullMinutes().minus(15.minutes),
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
        meetingId = "id3",
        conversationId = ConversationId("cid3", "domain"),
        title = "Groupless Ongoing Meeting",
        repeatingInterval = null,
        belongingType = BelongingType.Groupless(avatars = avatars, limit = 5),
        selfRole = MeetingItem.SelfRole.Admin,
        status = Status.Ongoing(
            startTime = currentTime().fullMinutes().minus(10.minutes),
            scheduledEndTime = currentTime().fullMinutes().plus(1.minutes)
        ),
    )

val CurrentTimeProvider.scheduledChannelMeetingStartingSoon
    get() = MeetingItem(
        meetingId = "id4",
        conversationId = ConversationId("cid4", "domain"),
        title = "Scheduled Channel Meeting Starting Soon",
        repeatingInterval = null,
        belongingType = BelongingType.Channel(name = "Channel Name", isPrivateChannel = false),
        selfRole = MeetingItem.SelfRole.Admin,
        status = Status.Scheduled(
            startTime = currentTime().fullMinutes().plus(1.minutes),
            endTime = currentTime().fullMinutes().plus(65.minutes),
        )
    )

val CurrentTimeProvider.scheduledRepeatingGroupMeeting
    get() = MeetingItem(
        meetingId = "id5",
        conversationId = ConversationId("cid5", "domain"),
        title = "Scheduled Group Meeting",
        repeatingInterval = MeetingItem.RepeatingInterval.Weekly,
        belongingType = BelongingType.Group(name = "Group Name"),
        selfRole = MeetingItem.SelfRole.Admin,
        status = Status.Scheduled(
            startTime = currentTime().fullMinutes().plus(1.days).plus(60.minutes),
            endTime = currentTime().fullMinutes().plus(1.days).plus(90.minutes),
        )
    )

val CurrentTimeProvider.pastMeetingMocks
    get() = listOf(
        endedPrivateChannelMeeting,
        MeetingItem(
            meetingId = "past1",
            conversationId = ConversationId("cid", "domain"),
            title = "Ended Groupless Meeting",
            repeatingInterval = null,
            belongingType = BelongingType.Groupless(avatars = avatars, limit = 5),
            selfRole = MeetingItem.SelfRole.Admin,
            status = Status.Ended(
                startTime = currentTime().fullMinutes().minus(1.days).minus(120.minutes),
                endTime = currentTime().fullMinutes().minus(1.days).minus(60.minutes),
            ),
        ),
        MeetingItem(
            meetingId = "past2",
            conversationId = ConversationId("cid", "domain"),
            title = "Ended Channel Meeting",
            repeatingInterval = null,
            belongingType = BelongingType.Channel(name = "Channel Name", isPrivateChannel = false),
            selfRole = MeetingItem.SelfRole.Admin,
            status = Status.Ended(
                startTime = currentTime().fullMinutes().minus(1.days).minus(60.minutes),
                endTime = currentTime().fullMinutes().minus(1.days).minus(30.minutes),
            ),
        ),
        MeetingItem(
            meetingId = "past3",
            conversationId = ConversationId("cid", "domain"),
            title = "Ended 1:1 Meeting",
            repeatingInterval = null,
            belongingType = BelongingType.OneOnOne(username = "John Doe", avatar = UserAvatarData()),
            selfRole = MeetingItem.SelfRole.Admin,
            status = Status.Ended(
                startTime = currentTime().fullMinutes().minus(1.days).minus(30.minutes),
                endTime = currentTime().fullMinutes().minus(1.days),
            ),
        ),
        MeetingItem(
            meetingId = "past4",
            conversationId = ConversationId("cid", "domain"),
            title = "Ended Group Meeting",
            repeatingInterval = null,
            belongingType = BelongingType.Group(name = "Group Name"),
            selfRole = MeetingItem.SelfRole.Admin,
            status = Status.Ended(
                startTime = currentTime().fullMinutes().minus(2.days).minus(120.minutes),
                endTime = currentTime().fullMinutes().minus(2.days).minus(90.minutes),
            ),
        ),
        MeetingItem(
            meetingId = "past5",
            conversationId = ConversationId("cid", "domain"),
            title = "Ended Channel Meeting",
            repeatingInterval = null,
            belongingType = BelongingType.Channel(name = "Channel Name", isPrivateChannel = true),
            selfRole = MeetingItem.SelfRole.Admin,
            status = Status.Ended(
                startTime = currentTime().fullMinutes().minus(2.days).minus(60.minutes),
                endTime = currentTime().fullMinutes().minus(2.days),
            ),
        ),
        MeetingItem(
            meetingId = "past6",
            conversationId = ConversationId("cid", "domain"),
            title = "Ended Groupless Meeting",
            repeatingInterval = null,
            belongingType = BelongingType.Groupless(avatars = avatars.take(2).toPersistentList(), limit = 5),
            selfRole = MeetingItem.SelfRole.Admin,
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
            meetingId = "next2",
            conversationId = ConversationId("cid", "domain"),
            title = "Scheduled 1:1 Meeting",
            repeatingInterval = MeetingItem.RepeatingInterval.Monthly,
            belongingType = BelongingType.OneOnOne(username = "John Doe", avatar = UserAvatarData()),
            selfRole = MeetingItem.SelfRole.Admin,
            status = Status.Scheduled(
                startTime = currentTime().fullMinutes().plus(2.days).minus(60.minutes),
                endTime = currentTime().fullMinutes().plus(2.days).minus(30.minutes),
            ),
        ),
        MeetingItem(
            meetingId = "next3",
            conversationId = ConversationId("cid", "domain"),
            title = "Scheduled Private Channel Meeting",
            repeatingInterval = MeetingItem.RepeatingInterval.Monthly,
            belongingType = BelongingType.Channel(name = "Channel Name", isPrivateChannel = true),
            selfRole = MeetingItem.SelfRole.Admin,
            status = Status.Scheduled(
                startTime = currentTime().fullMinutes().plus(2.days).minus(60.minutes),
                endTime = currentTime().fullMinutes().plus(2.days),
            ),
        ),
        MeetingItem(
            meetingId = "next4",
            conversationId = ConversationId("cid", "domain"),
            title = "Scheduled Groupless Meeting",
            repeatingInterval = null,
            belongingType = BelongingType.Groupless(avatars = avatars.take(2).toPersistentList(), limit = 5),
            selfRole = MeetingItem.SelfRole.Admin,
            status = Status.Scheduled(
                startTime = currentTime().fullMinutes().plus(3.days).minus(60.minutes),
                endTime = currentTime().fullMinutes().plus(3.days),
            ),
        )
    )

class MeetingMocksProvider(val currentTimeProvider: CurrentTimeProvider) {
    fun getItems(showingAll: Boolean, type: MeetingsTabItem) = when (type) {
        MeetingsTabItem.PAST -> currentTimeProvider.pastMeetingMocks
        MeetingsTabItem.NEXT -> currentTimeProvider.nextMeetingMocks
    }.filter { meeting ->
        val meetingLocalDate = meeting.status.startTime.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val currentLocalDate = currentTimeProvider.currentTime().toLocalDateTime(TimeZone.currentSystemDefault()).date
        when {
            !showingAll && type == MeetingsTabItem.PAST -> currentLocalDate.minus(1, DateTimeUnit.DAY) <= meetingLocalDate
            !showingAll && type == MeetingsTabItem.NEXT -> currentLocalDate.plus(1, DateTimeUnit.DAY) >= meetingLocalDate
            else -> true
        }
    }

    fun getItem(meetingId: String): MeetingItem? =
        (currentTimeProvider.pastMeetingMocks + currentTimeProvider.nextMeetingMocks).find { it.meetingId == meetingId }

    fun getPagingSource(type: MeetingsTabItem, showingAll: Boolean) = object : PagingSource<Int, Meeting>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Meeting> {
            val items = getItems(showingAll = showingAll, type = type).map { it.toMeeting() }
            val maxItems = if (!showingAll) items.size else items.size * 10 // if showingAll then generate 10x items by copying them
            val offset = params.key ?: 0
            val pageSize = min(params.loadSize, maxItems - offset) // if less items remain than the load size, only load remaining ones
            val nextOffset = if (offset + pageSize >= maxItems) null else offset + pageSize // null if there are no more items to load
            val pageItems = List(pageSize) { index ->
                val multiplier = (offset + index) / items.size // calculate how many times we have looped through the original items list
                val timeOffset = when (type) {
                    MeetingsTabItem.PAST -> -(multiplier * 4).days // for past meetings, each loop goes further back in time
                    MeetingsTabItem.NEXT -> (multiplier * 4).days // for next meetings, each loop goes further forward in time
                }
                items[(offset + index) % items.size].let {
                    it.copy(
                        meetingId = "${it.meetingId}_$multiplier",
                        startTime = it.startTime.plus(timeOffset),
                        endTime = it.endTime?.plus(timeOffset),
                    )
                }
            }
            delay(1500) // simulate loading delay
            return LoadResult.Page(data = pageItems, prevKey = null, nextKey = nextOffset)
        }

        override fun getRefreshKey(state: PagingState<Int, Meeting>): Int? {
            return state.anchorPosition?.let { anchorPosition ->
                state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                    ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
            }
        }
    }

    fun getPagingDataFlow(type: MeetingsTabItem, showingAll: Boolean): Flow<PagingData<Meeting>> {
        val pageSize = getItems(showingAll = true, type = type).size
        return Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
            pagingSourceFactory = {
                getPagingSource(type = type, showingAll = showingAll)
            }
        ).flow
    }

    companion object {
        val Default by lazy {
            MeetingMocksProvider(CurrentTimeProvider.Default) // time initialized when accessed for the first time
        }
    }
}

// remove seconds and milliseconds from Instant for better readability in the UI
private fun Instant.fullMinutes() = this.minus(this.toEpochMilliseconds() % 60_000, DateTimeUnit.MILLISECOND)

// temporary entity class to be used until we have a real data source to fetch meetings from kalium
data class Meeting(
    val meetingId: String,
    val conversationId: ConversationId,
    val belongingType: BelongingType,
    val title: String,
    val startTime: Instant,
    val endTime: Instant?,
    val repeatingInterval: MeetingItem.RepeatingInterval?,
    val ongoingCallStatus: MeetingItem.OngoingCallStatus?,
    val selfRole: SelfRole,
)
