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

import androidx.compose.runtime.Stable
import com.wire.android.feature.meetings.R
import com.wire.android.model.UserAvatarData
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.MeetingId
import com.wire.kalium.logic.data.meeting.MeetingOccurrence
import com.wire.kalium.logic.data.meeting.MeetingOccurrence.Recurrence.Frequency
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.Instant
import kotlin.time.Duration

sealed interface MeetingListItem

@Stable
data class MeetingItem(
    val occurrenceId: String,
    val meetingId: MeetingId,
    val conversationId: ConversationId,
    val belongingType: BelongingType,
    val repeatingInterval: RepeatingInterval?,
    val title: String,
    val status: Status,
    val selfRole: SelfRole,
) : MeetingListItem {
    @Stable
    data class RepeatingInterval(val frequency: Frequency, val interval: Int) {
        val label: UIText = when (frequency) {
            Frequency.DAILY -> UIText.PluralResource(R.plurals.meeting_repeating_days, interval, interval)
            Frequency.WEEKLY -> UIText.PluralResource(R.plurals.meeting_repeating_weeks, interval, interval)
        }

        companion object {
            val Supported: ImmutableList<RepeatingInterval> = MeetingOccurrence.Recurrence.SUPPORTED_RECURRENCES
                .map { (frequency, interval) -> RepeatingInterval(frequency, interval.toInt()) }.toPersistentList()
        }
    }

    @Stable
    sealed interface BelongingType {
        data class Group(val name: String) : BelongingType
        data class Channel(val name: String, val isPrivateChannel: Boolean) : BelongingType
        data class OneOnOne(val username: String, val avatar: UserAvatarData) : BelongingType
        data class Groupless(val avatars: ImmutableList<UserAvatarData>, val limit: Int = GROUPLESS_AVATARS_LIMIT) : BelongingType
    }

    @Stable
    sealed interface Status {
        val startTime: Instant

        data class Scheduled(
            override val startTime: Instant, // scheduled start time
            val endTime: Instant, // scheduled end time
        ) : Status

        data class Ongoing(
            override val startTime: Instant, // time when the meeting actually started
            val scheduledEndTime: Instant? = null, // null for ad-hoc meetings
            val ongoingCallStatus: OngoingCallStatus? = null, // null if the call is not ongoing / hasn't started yet
        ) : Status

        data class Ended(
            override val startTime: Instant, // time when the meeting actually started
            val endTime: Instant // time when the meeting actually ended
        ) : Status {
            val duration: Duration = endTime - startTime
        }
    }

    @Stable
    data class OngoingCallStatus(
        val currentCallEstablishedTime: Instant?, // there can be many calls one after another in a meeting, so take the current one
        val isSelfUserAttending: Boolean // is the current user attending the ongoing call
    )

    @Stable
    enum class SelfRole { Creator, Member }
}

private const val GROUPLESS_AVATARS_LIMIT = 5
