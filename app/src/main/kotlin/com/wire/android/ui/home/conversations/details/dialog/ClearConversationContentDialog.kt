package com.wire.android.ui.home.conversations.details.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.home.conversationslist.model.DialogState

@Composable
fun ClearConversationContentDialog(
    dialogState: VisibilityState<DialogState>,
    isLoading: Boolean,
    onClearConversationContent: (DialogState) -> Unit
) {
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            dialogState.dismiss()
        }
    }

    VisibilityState(dialogState) {
        WireDialog(
            title = "Clear content?",
            text = "This will clear the previous conversation history on all your devices. You remain in the ${stringResource(id = it.conversationTypeDetail.labelResource)} and have access to all new ${stringResource(it.conversationTypeDetail.labelResource)} activity.",
            buttonsHorizontalAlignment = true,
            onDismiss = dialogState::dismiss,
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = dialogState::dismiss,
                text = stringResource(id = R.string.label_cancel),
                state = WireButtonState.Default
            ),
            optionButton1Properties = WireDialogButtonProperties(
                onClick = { onClearConversationContent(it) },
                text = "Clear content",
                type = WireDialogButtonType.Primary,
                state =
                if (isLoading)
                    WireButtonState.Disabled
                else
                    WireButtonState.Error,
                loading = isLoading
            )
        )
    }
}
