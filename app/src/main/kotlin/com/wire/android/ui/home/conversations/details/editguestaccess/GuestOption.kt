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

package com.wire.android.ui.home.conversations.details.editguestaccess

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.home.conversations.details.options.GroupOptionWithSwitch

@Composable
fun GuestOption(
    isSwitchEnabled: Boolean,
    isSwitchVisible: Boolean,
    switchState: Boolean,
    isLoading: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GroupOptionWithSwitch(
        switchClickable = isSwitchEnabled,
        switchVisible = isSwitchVisible,
        switchState = switchState,
        onClick = onCheckedChange,
        isLoading = isLoading,
        title = R.string.conversation_options_guests_label,
        subTitle = when {
            isSwitchEnabled -> R.string.conversation_options_guest_description
            isSwitchVisible -> R.string.conversation_options_guest_not_editable_description
            else -> null
        }
    )
}

@Preview
@Composable
fun PreviewGuestOption() {
    GuestOption(
        isSwitchEnabled = false,
        isSwitchVisible = true,
        switchState = true,
        isLoading = false,
        onCheckedChange = {}
    )
}
