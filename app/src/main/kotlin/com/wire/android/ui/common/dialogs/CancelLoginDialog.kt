package com.wire.android.ui.common.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.visbility.VisibilityState

@Composable
fun CancelLoginDialogContent(
    dialogState: VisibilityState<CancelLoginDialogState>,
    onActionButtonClicked: () -> Unit,
    onProceedButtonClicked: () -> Unit
) {
    VisibilityState(dialogState) { state ->
        WireDialog(
            title = stringResource(R.string.cancel_login_dialog_title),
            text = stringResource(R.string.cancel_login_dialog_description),
            onDismiss = dialogState::dismiss,
            optionButton1Properties =
            WireDialogButtonProperties(
                onClick = onActionButtonClicked,
                text = stringResource(R.string.cancel_login_button_label),
                type = WireDialogButtonType.Secondary
            ),
            optionButton2Properties = WireDialogButtonProperties(
                onClick = onProceedButtonClicked,
                text = stringResource(id = R.string.label_proceed),
                type = WireDialogButtonType.Primary,
            ),
            buttonsHorizontalAlignment = false
        )
    }
}
