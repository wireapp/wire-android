package com.wire.android.ui.common.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.PreservedState
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.kalium.logic.data.user.UserId

@Composable
fun BlockUserDialogContent(
    dialogState: PreservedState<BlockUserDialogState>?,
    dismiss: () -> Unit = {},
    onBlock: (UserId, String) -> Unit = { _, _ -> }
) {
    VisibilityState(dialogState) { preservedState ->
        WireDialog(
            title = stringResource(id = R.string.block_user_dialog_title),
            text = stringResource(id = R.string.block_user_dialog_body, preservedState.state.userName),
            onDismiss = dismiss,
            optionButton1Properties = WireDialogButtonProperties(
                onClick = { onBlock(preservedState.state.userId, preservedState.state.userName) },
                text = stringResource(id = R.string.block_user_dialog_confirm_button),
                type = WireDialogButtonType.Primary,
                state = if (preservedState is PreservedState.Loading)
                    WireButtonState.Disabled
                else
                    WireButtonState.Error,
            ),
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = dismiss,
                text = stringResource(id = android.R.string.cancel),
                type = WireDialogButtonType.Secondary,
            )
        )
    }
}

data class BlockUserDialogState(val userName: String, val userId: UserId)
