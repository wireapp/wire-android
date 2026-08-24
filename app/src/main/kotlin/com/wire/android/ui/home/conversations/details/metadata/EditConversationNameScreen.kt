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

package com.wire.android.ui.home.conversations.details.metadata

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.wire.android.ui.common.groupname.GroupMetadataState
import com.wire.android.ui.common.groupname.GroupMetadataState.NewGroupError
import com.wire.android.ui.common.groupname.GroupNameMode
import com.wire.android.ui.common.groupname.GroupNameScreen
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
internal fun EditConversationNameRouteScreen(
    viewModel: EditConversationMetadataViewModel,
    onNavigateBack: () -> Unit,
    onCompleted: (Boolean) -> Unit,
) {
    with(viewModel) {
        LaunchedEffect(editConversationState.completed) {
            when (editConversationState.completed) {
                EditConversationMetadataState.Completed.Success -> onCompleted(true)
                EditConversationMetadataState.Completed.Failure -> onCompleted(false)
                EditConversationMetadataState.Completed.None -> Unit // No action needed
            }
        }
        GroupNameScreen(
            newGroupState = editConversationState.toGroupMetadataState(),
            newGroupNameTextState = editConversationNameTextState,
            onGroupNameErrorAnimated = ::onGroupNameErrorAnimated,
            onContinuePressed = ::saveNewGroupName,
            onBackPressed = onNavigateBack,
        )
    }
}

private fun EditConversationMetadataState.toGroupMetadataState() = GroupMetadataState(
    originalGroupName = originalGroupName,
    animatedGroupNameError = animatedGroupNameError,
    continueEnabled = continueEnabled,
    isChannel = isChannel,
    error = when (error) {
        EditConversationMetadataState.NameError.None -> NewGroupError.None
        EditConversationMetadataState.NameError.Empty -> NewGroupError.TextFieldError.GroupNameEmptyError
        EditConversationMetadataState.NameError.TooLong -> NewGroupError.TextFieldError.GroupNameExceedLimitError
    },
    mode = GroupNameMode.EDITION,
)

@Composable
@PreviewMultipleThemes
fun PreviewNewGroupScreen() = WireTheme {
    GroupNameScreen(GroupMetadataState(mode = GroupNameMode.EDITION), TextFieldState(), {}, {}, {})
}
