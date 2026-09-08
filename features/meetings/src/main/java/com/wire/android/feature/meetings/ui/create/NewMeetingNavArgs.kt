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
package com.wire.android.feature.meetings.ui.create

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.wire.android.feature.meetings.R
import com.wire.kalium.logic.data.id.MeetingId
import kotlinx.serialization.Serializable
import com.wire.android.ui.common.R as CommonUiR

data class NewMeetingNavArgs(
    val type: NewMeetingType
)

@Serializable
sealed interface NewMeetingType {
    @get:StringRes
    val title: Int

    @get:StringRes
    val action: Int

    @get:DrawableRes
    val icon: Int

    @Serializable
    data object MeetNow : NewMeetingType {
        override val title = R.string.new_meeting_now
        override val action = R.string.new_meeting_action_start
        override val icon = CommonUiR.drawable.ic_video_call
    }

    @Serializable
    data object Schedule : NewMeetingType {
        override val title = R.string.new_meeting_schedule
        override val action = R.string.new_meeting_action_schedule
        override val icon = CommonUiR.drawable.ic_calendar
    }

    @Serializable
    data class Edit(val id: MeetingId) : NewMeetingType {
        override val title = R.string.new_meeting_edit
        override val action = R.string.new_meeting_action_edit
        override val icon = CommonUiR.drawable.ic_calendar
    }
}
