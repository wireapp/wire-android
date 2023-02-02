/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.newconversation.newgroup

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.common.groupname.GroupMetadataState
import com.wire.android.ui.common.groupname.GroupNameScreen

@Composable
fun NewGroupScreen(
    newGroupState: GroupMetadataState,
    onGroupNameChange: (TextFieldValue) -> Unit,
    onContinuePressed: () -> Unit,
    onGroupNameErrorAnimated: () -> Unit,
    onBackPressed: () -> Unit
) {
    GroupNameScreen(
        newGroupState = newGroupState,
        onGroupNameChange = onGroupNameChange,
        onContinuePressed = onContinuePressed,
        onGroupNameErrorAnimated = onGroupNameErrorAnimated,
        onBackPressed = onBackPressed
    )
}

@Composable
@Preview
fun PreviewNewGroupScreen() {
    GroupNameScreen(
        GroupMetadataState(),
        {},
        {},
        {},
        {}
    )
}
