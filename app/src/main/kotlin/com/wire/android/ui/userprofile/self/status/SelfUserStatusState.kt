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

package com.wire.android.ui.userprofile.self.status

import com.wire.android.ui.userprofile.self.dialog.StatusDialogData
import com.wire.kalium.logic.data.user.UserAvailabilityStatus

data class SelfUserStatusState(
    val availabilityStatus: UserAvailabilityStatus = UserAvailabilityStatus.NONE,
    val emoji: String? = null,
    val message: String = "",
    val isTeamMember: Boolean = false,
    val statusDialogData: StatusDialogData? = null,
    val isSaving: Boolean = false,
)

const val DEFAULT_STATUS_EMOJI = "\uD83D\uDCAC"
const val MAX_STATUS_TEXT_LENGTH = 50

fun resolveStatusEmoji(emoji: String?, message: String): String? =
    if (!emoji.isNullOrBlank()) {
        emoji
    } else if (message.trim().isBlank()) {
        null
    } else {
        DEFAULT_STATUS_EMOJI
    }

fun buildTextStatus(emoji: String?, message: String): String? {
    val trimmedMessage = message.trim().take(MAX_STATUS_TEXT_LENGTH)
    val trimmedEmoji = emoji?.takeIf { it.isNotBlank() }
    if (trimmedMessage.isBlank()) return trimmedEmoji ?: " "

    val resolvedEmoji = resolveStatusEmoji(trimmedEmoji, trimmedMessage) ?: return trimmedMessage
    return "$resolvedEmoji $trimmedMessage"
}
