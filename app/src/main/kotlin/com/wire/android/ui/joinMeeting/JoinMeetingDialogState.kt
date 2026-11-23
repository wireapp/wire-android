/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.joinMeeting

import com.wire.kalium.logic.data.user.UserId

sealed interface JoinMeetingDialogState {
    data object Idle : JoinMeetingDialogState
    data object Loading : JoinMeetingDialogState
    data class PasswordRequired(val loading: Boolean = false, val error: Error? = null) : JoinMeetingDialogState
    data object Success : JoinMeetingDialogState

    sealed interface Error : JoinMeetingDialogState {
        data object InvalidPassword : Error
        data object NotFound : Error
        data object Unavailable : Error
        data object Generic : Error
    }
}

/** Meeting guest-link parameters and the account that should use them. */
data class MeetingJoinLink(
    val code: String,
    val key: String,
    val domain: String,
    val targetSessionId: UserId? = null,
)
