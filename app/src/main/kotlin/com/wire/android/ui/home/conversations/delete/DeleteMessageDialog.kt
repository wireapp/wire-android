package com.wire.android.ui.home.conversations.delete

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.home.conversations.ConversationViewModel
import com.wire.android.ui.home.conversations.DeleteMessageDialogActiveState
import com.wire.android.ui.home.conversations.DeleteMessageDialogsState
import com.wire.android.ui.home.conversations.DeleteMessageError
import com.wire.android.util.dialogErrorStrings

@Composable
internal fun DeleteMessageDialog(conversationViewModel: ConversationViewModel) {
    val deleteMessageDialogsState = conversationViewModel.deleteMessageDialogsState

    if (deleteMessageDialogsState is DeleteMessageDialogsState.States) {
        when {
            deleteMessageDialogsState.forEveryone is DeleteMessageDialogActiveState.Visible -> {
                DeleteMessageDialog(
                    state = deleteMessageDialogsState.forEveryone,
                    onDialogDismiss = conversationViewModel::onDialogDismissed,
                    onDeleteForMe = conversationViewModel::showDeleteMessageForYourselfDialog,
                    onDeleteForEveryone = conversationViewModel::deleteMessage,
                )
                if (deleteMessageDialogsState.forEveryone.error is DeleteMessageError.GenericError) {
                    DeleteMessageErrorDialog(deleteMessageDialogsState.forEveryone.error, conversationViewModel::clearDeleteMessageError)
                }
            }
            deleteMessageDialogsState.forYourself is DeleteMessageDialogActiveState.Visible -> {

                if (deleteMessageDialogsState.forYourself.error is DeleteMessageError.GenericError) {
                    DeleteMessageErrorDialog(deleteMessageDialogsState.forYourself.error, conversationViewModel::clearDeleteMessageError)
                } else {
                    DeleteMessageForYourselfDialog(
                        state = deleteMessageDialogsState.forYourself,
                        onDialogDismiss = conversationViewModel::onDialogDismissed,
                        onDeleteForMe = conversationViewModel::deleteMessage
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
