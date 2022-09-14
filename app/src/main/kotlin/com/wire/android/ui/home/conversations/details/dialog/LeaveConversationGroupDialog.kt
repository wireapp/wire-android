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
import com.wire.android.ui.home.conversationslist.model.GroupDialogState

@Composable
internal fun LeaveConversationGroupDialog(
    dialogState: VisibilityState<GroupDialogState>,
    isLoading: Boolean,
    onLeaveGroup: (GroupDialogState) -> Unit,
) {
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            dialogState.dismiss()
        }
    }

    VisibilityState(dialogState) {
        WireDialog(
            title = stringResource(id = R.string.leave_group_conversation_dialog_title, it.conversationName),
            text = stringResource(id = R.string.leave_group_conversation_dialog_description),
            buttonsHorizontalAlignment = true,
            onDismiss = dialogState::dismiss,
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = dialogState::dismiss,
                text = stringResource(id = R.string.label_cancel),
                state = WireButtonState.Default
            ),
            optionButton1Properties = WireDialogButtonProperties(
                onClick = { onLeaveGroup(it) },
                text = stringResource(id = R.string.label_leave),
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
