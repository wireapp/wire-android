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
import com.wire.android.ui.home.conversationslist.model.DialogState

@Composable
fun ClearConversationContentDialog(
    dialogState: VisibilityState<DialogState>,
    isLoading: Boolean,
    onClearConversationContent: (DialogState) -> Unit
) {
    VisibilityState(dialogState) {
        WireDialog(
            title = stringResource(R.string.dialog_clear_content_title),
            text = stringResource(R.string.dialog_clear_content_text, stringResource(it.conversationTypeDetail.labelResource)),
            buttonsHorizontalAlignment = true,
            onDismiss = dialogState::dismiss,
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = dialogState::dismiss,
                text = stringResource(id = R.string.label_cancel),
                state = WireButtonState.Default
            ),
            optionButton1Properties = WireDialogButtonProperties(
                onClick = { onClearConversationContent(it) },
                text = stringResource(R.string.dialog_clear_content_option),
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
