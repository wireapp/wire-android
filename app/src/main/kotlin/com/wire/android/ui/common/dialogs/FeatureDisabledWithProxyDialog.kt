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
fun FeatureDisabledWithProxyDialogContent(
    dialogState: VisibilityState<FeatureDisabledWithProxyDialogState>,
    onActionButtonClicked: (() -> Unit)? = null
) {
    VisibilityState(dialogState) { state ->
        WireDialog(
            title = stringResource(R.string.not_supported_dialog_title),
            text = if (state.teamUrl.isEmpty()) {
                stringResource(state.description)
            } else {
                stringResource(state.description, state.teamUrl)
            },

            onDismiss = dialogState::dismiss,
            optionButton2Properties =
            if (onActionButtonClicked != null) {
                WireDialogButtonProperties(
                    onClick = onActionButtonClicked,
                    text = stringResource(id = R.string.to_team_management_action),
                    type = WireDialogButtonType.Primary
                )
            } else {
                null
            },
            optionButton1Properties = WireDialogButtonProperties(
                onClick = dialogState::dismiss,
                text = stringResource(id = android.R.string.ok),
                type = WireDialogButtonType.Primary,
            ),
            buttonsHorizontalAlignment = false
        )
    }
}
