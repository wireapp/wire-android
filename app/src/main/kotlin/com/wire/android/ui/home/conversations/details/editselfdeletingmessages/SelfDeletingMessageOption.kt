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

package com.wire.android.ui.home.conversations.details.editselfdeletingmessages

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.home.conversations.details.options.GroupOptionWithSwitch

@Composable
fun SelfDeletingMessageOption(
    switchState: Boolean,
    isLoading: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GroupOptionWithSwitch(
        switchClickable = true,
        switchVisible = true,
        switchState = switchState,
        onClick = onCheckedChange,
        isLoading = isLoading,
        title = R.string.self_deleting_messages_option,
        subTitle = R.string.self_deleting_messages_option_description
    )
}

@Preview
@Composable
fun PreviewGuestOption() {
    SelfDeletingMessageOption(
        switchState = true,
        isLoading = false,
        onCheckedChange = {}
    )
}
