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

import android.os.Parcelable
import androidx.annotation.StringRes
import com.wire.android.feature.meetings.R
import com.wire.android.model.UserAvatarData
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.datetime.Instant
import kotlinx.parcelize.Parcelize
import kotlin.time.Duration

sealed interface MeetingListItem

data class MeetingItem(
    val meetingId: String,
    val conversationId: ConversationId,
    val belongingType: BelongingType,
    val title: String,
    val status: Status,
) : MeetingListItem {
    @Parcelize
    enum class RepeatingInterval(@StringRes val nameResId: Int) : Parcelable {
        Daily(R.string.meeting_repeating_daily),
        Weekly(R.string.meeting_repeating_weekly),
        BiWeekly(R.string.meeting_repeating_biweekly),
        Monthly(R.string.meeting_repeating_monthly),
        Annually(R.string.meeting_repeating_annually)
    }

    sealed interface BelongingType {
        data class Group(val name: String) : BelongingType
        data class Channel(val name: String, val isPrivateChannel: Boolean) : BelongingType
        data class OneOnOne(val username: String, val avatar: UserAvatarData) : BelongingType
        data class Groupless(val avatars: List<UserAvatarData>, val limit: Int = GROUPLESS_AVATARS_LIMIT) : BelongingType
    }

    sealed interface Status {
        val startTime: Instant
        data class Scheduled(
            override val startTime: Instant, // scheduled start time
            val endTime: Instant, // scheduled end time
            val repeatingInterval: RepeatingInterval? = null, // null for one-time meetings
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

    data class OngoingCallStatus(
        val currentCallStartedTime: Instant, // time when the current call started (there can be many calls one after another in a meeting)
        val isSelfUserAttending: Boolean // is the current user attending the ongoing call
    )
}

private const val GROUPLESS_AVATARS_LIMIT = 5
