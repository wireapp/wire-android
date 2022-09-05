package com.wire.android.ui.common.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.userprofile.other.UnblockUserDialogState
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.kalium.logic.data.user.UserId

@Composable
fun UnblockUserDialogContent(
    dialogState: VisibilityState<UnblockUserDialogState>,
    isLoading: Boolean,
    onUnblock: (UserId) -> Unit = { }
) {
    VisibilityState(dialogState) { state ->
        WireDialog(
            title = stringResource(id = R.string.unblock_user_dialog_title),
            text = LocalContext.current.resources.stringWithStyledArgs(
                R.string.unblock_user_dialog_body,
                MaterialTheme.wireTypography.body01,
                MaterialTheme.wireTypography.body02,
                colorsScheme().onBackground,
                colorsScheme().onBackground,
                state.username
            ),
            onDismiss = dialogState::dismiss,
            optionButton1Properties = WireDialogButtonProperties(
                onClick = { onUnblock(state.userId) },
                text = stringResource(id = R.string.unblock_user_dialog_confirm_button),
                type = WireDialogButtonType.Primary,
                state = if (isLoading)
                    WireButtonState.Disabled
                else
                    WireButtonState.Default,
            ),
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = dialogState::dismiss,
                text = stringResource(id = android.R.string.cancel),
                type = WireDialogButtonType.Secondary,
            )
        )
    }
}
