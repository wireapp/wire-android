package com.wire.android.ui.common.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.VisibilityStateExt
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.stringWithStyledArgs

@Composable
internal fun CustomBEDeeplinkDialog(
    dialogState: VisibilityState<CustomBEDeeplinkDialogState>
) {
    VisibilityStateExt(dialogState) {
        WireDialog(
            title = stringResource(R.string.custom_backend_dialog_title),
            text = LocalContext.current.resources.stringWithStyledArgs(
                R.string.custom_backend_dialog_body,
                MaterialTheme.wireTypography.body01,
                MaterialTheme.wireTypography.body02,
                colorsScheme().onBackground,
                colorsScheme().onBackground, it.backendName, it.backendUrl
            ),

            buttonsHorizontalAlignment = true,
            onDismiss = dialogState::dismiss,
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = dialogState::dismiss,
                text = stringResource(id = R.string.label_cancel),
                state = WireButtonState.Default
            ),
            optionButton1Properties = WireDialogButtonProperties(
                onClick = { },
                text = stringResource(id = R.string.label_proceed),
                type = WireDialogButtonType.Primary,
                state =
                WireButtonState.Default
            )
        )
    }
}

data class CustomBEDeeplinkDialogState(val backendName: String = "", val backendUrl: String = "", val shouldShowDialog: Boolean = false)
