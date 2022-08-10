package com.wire.android.ui.common.dialogs

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.kalium.logic.data.user.UserId

@Suppress("MatchingDeclarationName")
data class BlockUserDialogState(val userName: String, val userId: UserId)

@Composable
fun BlockUserDialogContent(
    state: BlockUserDialogState?,
    dismiss: () -> Unit = {},
    onBlock: (UserId, String) -> Unit = { _, _ -> }
) {
    if (state != null)
        BlockUserDialog(state, dismiss, onBlock)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockUserDialog(
    state: BlockUserDialogState,
    dismiss: () -> Unit = {},
    onBlock: (UserId, String) -> Unit = { _, _ -> }
) {
    WireDialog(
        title = stringResource(id = R.string.block_user_dialog_title),
        text = stringResource(id = R.string.block_user_dialog_body, state.userName),
        onDismiss = dismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = { onBlock(state.userId, state.userName) },
            text = stringResource(id = R.string.block_user_dialog_confirm_button),
            type = WireDialogButtonType.Primary,
            state = WireButtonState.Error
        ),
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = dismiss,
            text = stringResource(id = android.R.string.cancel),
            type = WireDialogButtonType.Secondary,
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = false)
@Composable
private fun BlockUserDialogPreview() {
    BlockUserDialog(BlockUserDialogState("SomeUser", UserId("someId", "someDomain")))
}
