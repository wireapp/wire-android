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

package com.wire.android.ui.home.conversations.delete

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.visbility.VisibilityState

@Composable
internal fun DeleteMessageDialog(
    dialogState: VisibilityState<DeleteMessageDialogState>,
    deleteMessage: (messageId: String, deleteForEveryone: Boolean) -> Unit,
) {
    VisibilityState(dialogState) { state ->
        when (state.type) {
            DeleteMessageDialogType.ForEveryone -> {
                DeleteMessageDialog(
                    state = state,
                    onDialogDismiss = dialogState::dismiss,
                    onDeleteForMe = { dialogState.update { it.copy(type = DeleteMessageDialogType.ForYourself) } },
                    onDeleteForEveryone = { messageId: String -> deleteMessage(messageId, true) },
                )
            }
            DeleteMessageDialogType.ForYourself -> {
                DeleteMessageForYourselfDialog(
                    state = state,
                    onDialogDismiss = dialogState::dismiss,
                    onDeleteForMe = { messageId: String -> deleteMessage(messageId, false) },
                )
            }
        }
    }
}

@Composable
private fun DeleteMessageDialog(
    state: DeleteMessageDialogState,
    onDialogDismiss: () -> Unit,
    onDeleteForMe: (String) -> Unit,
    onDeleteForEveryone: (String) -> Unit,
) {
    WireDialog(
        title = stringResource(R.string.delete_message_dialog_title),
        text = stringResource(R.string.delete_message_dialog_message),
        onDismiss = onDialogDismiss,
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onDialogDismiss,
            text = stringResource(id = R.string.label_cancel),
            state = WireButtonState.Default
        ),
        optionButton1Properties = WireDialogButtonProperties(
            onClick = { onDeleteForMe(state.messageId) },
            text = stringResource(R.string.label_delete_for_me),
            type = WireDialogButtonType.Primary,
            state = WireButtonState.Error
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = { onDeleteForEveryone(state.messageId) },
            text = stringResource(R.string.label_delete_for_everyone),
            type = WireDialogButtonType.Primary,
            state = if (state.loading) WireButtonState.Disabled else WireButtonState.Error,
            loading = state.loading
        ),
        buttonsHorizontalAlignment = false,
    )
}

@Composable
private fun DeleteMessageForYourselfDialog(
    state: DeleteMessageDialogState,
    onDialogDismiss: () -> Unit,
    onDeleteForMe: (String) -> Unit,
) {
    WireDialog(
        title = stringResource(R.string.delete_message_for_yourself_dialog_title),
        text = stringResource(R.string.delete_message_for_yourself_dialog_message),
        onDismiss = onDialogDismiss,
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onDialogDismiss,
            text = stringResource(id = R.string.label_cancel),
            state = WireButtonState.Default
        ),
        optionButton1Properties = WireDialogButtonProperties(
            onClick = { onDeleteForMe(state.messageId) },
            text = stringResource(R.string.label_delete_for_me),
            type = WireDialogButtonType.Primary,
            state = if (state.loading) WireButtonState.Disabled else WireButtonState.Error,
            loading = state.loading
        )
    )
}
