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
 */

package com.wire.android.ui.home.conversations.details.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState

@Composable
internal fun LeaveConversationGroupDialog(
    dialogState: VisibilityState<GroupDialogState>,
    isLoading: Boolean,
    onLeaveGroup: (GroupDialogState) -> Unit,
) {
    VisibilityState(dialogState) {
        LeaveConversationDialog(
            conversationName = it.conversationName,
            isLoading = isLoading,
            onDismiss = dialogState::dismiss,
            onLeave = { onLeaveGroup(it) }
        )
    }
}

@Composable
fun LeaveConversationDialog(
    conversationName: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onLeave: () -> Unit,
) {
    WireDialog(
        title = stringResource(id = R.string.leave_group_conversation_dialog_title, conversationName),
        text = stringResource(id = R.string.leave_group_conversation_dialog_description),
        buttonsHorizontalAlignment = true,
        onDismiss = onDismiss,
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(id = R.string.label_cancel),
            state = WireButtonState.Default
        ),
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onLeave,
            text = stringResource(id = R.string.label_leave),
            type = WireDialogButtonType.Primary,
            state = if (isLoading)
                WireButtonState.Disabled
            else
                WireButtonState.Error,
            loading = isLoading
        )
    )
}
