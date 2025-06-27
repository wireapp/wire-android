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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.util.dialogErrorStrings

@Composable
internal fun DeleteMessageDialog(state: DeleteMessageDialogState, actions: DeleteMessageDialogHelper) {
    if (state is DeleteMessageDialogState.Visible) {
        when (state.type) {
            DeleteMessageDialogType.ForEveryone -> {
                DeleteMessageDialog(
                    state = state,
                    onDialogDismiss = actions::onDeleteDialogDismissed,
                    onDeleteForMe = actions::showDeleteMessageForYourselfDialog,
                    onDeleteForEveryone = { messageId: String ->
                        actions.onDeleteMessage(
                            messageId = messageId,
                            deleteForEveryone = true,
                        )
                    },
                )
                if (state.error is DeleteMessageError.GenericError) {
                    DeleteMessageErrorDialog(state.error, actions::clearDeleteMessageError)
                }
            }
            DeleteMessageDialogType.ForYourself -> {
                DeleteMessageForYourselfDialog(
                    state = state,
                    onDialogDismiss = actions::onDeleteDialogDismissed,
                    onDeleteForMe = { messageId: String ->
                        actions.onDeleteMessage(
                            messageId = messageId,
                            deleteForEveryone = false,
                        )
                    },
                )
                if (state.error is DeleteMessageError.GenericError) {
                    DeleteMessageErrorDialog(state.error, actions::clearDeleteMessageError)
                }
            }
        }
    }
}

@Composable
private fun DeleteMessageDialog(
    state: DeleteMessageDialogState.Visible,
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
    state: DeleteMessageDialogState.Visible,
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

@Composable
private fun DeleteMessageErrorDialog(error: DeleteMessageError.GenericError, onDialogDismiss: () -> Unit) {
    val (title, message) = error.coreFailure.dialogErrorStrings(
        LocalContext.current.resources
    )
    WireDialog(
        title = title,
        text = message,
        onDismiss = onDialogDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDialogDismiss,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    )
}
