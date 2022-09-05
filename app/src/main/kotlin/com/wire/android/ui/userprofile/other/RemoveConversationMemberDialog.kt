package com.wire.android.ui.userprofile.other

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.PreservedState
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.userprofile.group.RemoveConversationMemberState

@Composable
internal fun RemoveConversationMemberDialog(
    dialogState: VisibilityState<RemoveConversationMemberState>,
    isLoading: Boolean,
    onRemoveConversationMember: (RemoveConversationMemberState) -> Unit,
) {

    VisibilityState(dialogState) { state ->
        WireDialog(
            title = stringResource(R.string.dialog_remove_conversation_member_title),
            text = stringResource(
                R.string.dialog_remove_conversation_member_description,
                state.fullName,
                state.userName
            ),
            buttonsHorizontalAlignment = true,
            onDismiss = dialogState::dismiss,
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = dialogState::dismiss,
                text = stringResource(id = R.string.label_cancel),
                state = WireButtonState.Default
            ),
            optionButton1Properties = WireDialogButtonProperties(
                onClick = { onRemoveConversationMember(state) },
                text = stringResource(id = R.string.label_remove),
                type = WireDialogButtonType.Primary,
                state = if (isLoading)
                    WireButtonState.Disabled
                else
                    WireButtonState.Error,
                loading = (isLoading)
            )
        )
    }

}
