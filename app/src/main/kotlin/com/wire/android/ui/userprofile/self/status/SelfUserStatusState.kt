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

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.ui.userprofile.self.dialog.StatusDialogData
import com.wire.kalium.logic.data.user.UserAvailabilityStatus

data class SelfUserStatusState(
    val availabilityStatus: UserAvailabilityStatus = UserAvailabilityStatus.NONE,
    val emoji: String? = null,
    val message: String = "",
    val savedEmoji: String? = null,
    val savedMessage: String = "",
    val isTeamMember: Boolean = false,
    val quickStatusPresets: List<QuickStatusPreset> = defaultQuickStatusPresets(),
    val statusDialogData: StatusDialogData? = null,
    val isSaving: Boolean = false,
)

data class QuickStatusPreset(
    val emoji: String,
    @StringRes val labelResId: Int,
)

fun defaultQuickStatusPresets(): List<QuickStatusPreset> = listOf(
    QuickStatusPreset(
        emoji = "\uD83C\uDFA7",
        labelResId = R.string.user_profile_quick_status_in_meeting,
    ),
    QuickStatusPreset(
        emoji = "\uD83E\uDD15",
        labelResId = R.string.user_profile_quick_status_out_sick,
    ),
    QuickStatusPreset(
        emoji = "\uD83D\uDCF5",
        labelResId = R.string.user_profile_quick_status_out_of_office,
    ),
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
