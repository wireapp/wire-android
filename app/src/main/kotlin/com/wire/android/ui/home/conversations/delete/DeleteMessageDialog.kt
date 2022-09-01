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
internal fun DeleteMessageDialog(state: DeleteMessageDialogsState, actions: DeleteMessageDialogHelper) {

    if (state is DeleteMessageDialogsState.States) {
        when {
            state.forEveryone is DeleteMessageDialogActiveState.Visible -> {
                DeleteMessageDialog(
                    state = state.forEveryone,
                    onDialogDismiss = actions::onDeleteDialogDismissed,
                    onDeleteForMe = actions::showDeleteMessageForYourselfDialog,
                    onDeleteForEveryone = actions::onDeleteMessage,
                )
                if (state.forEveryone.error is DeleteMessageError.GenericError) {
                    DeleteMessageErrorDialog(state.forEveryone.error, actions::clearDeleteMessageError)
                }
            }
            state.forYourself is DeleteMessageDialogActiveState.Visible -> {

                if (state.forYourself.error is DeleteMessageError.GenericError) {
                    DeleteMessageErrorDialog(state.forYourself.error, actions::clearDeleteMessageError)
                } else {
                    DeleteMessageForYourselfDialog(
                        state = state.forYourself,
                        onDialogDismiss = actions::onDeleteDialogDismissed,
                        onDeleteForMe = actions::onDeleteMessage
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteMessageDialog(
    state: DeleteMessageDialogActiveState.Visible,
    onDialogDismiss: () -> Unit,
    onDeleteForMe: (String) -> Unit,
    onDeleteForEveryone: (String, Boolean) -> Unit,
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
            onClick = { onDeleteForEveryone(state.messageId, true) },
            text = stringResource(R.string.label_delete_for_everyone),
            type = WireDialogButtonType.Primary,
            state = if (state.loading) WireButtonState.Disabled else WireButtonState.Error,
            loading = state.loading
        ),
        buttonsHorizontalAlignment = false
    )
}

@Composable
private fun DeleteMessageForYourselfDialog(
    state: DeleteMessageDialogActiveState.Visible,
    onDialogDismiss: () -> Unit,
    onDeleteForMe: (String, Boolean) -> Unit,
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
            onClick = { onDeleteForMe(state.messageId, false) },
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
