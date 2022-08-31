package com.wire.android.ui.common.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.WireActivityViewModel
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.kalium.logic.configuration.server.ServerConfig

@Composable
internal fun CustomBEDeeplinkDialog(
    wireActivityViewModel: WireActivityViewModel
) {
    with(wireActivityViewModel) {
        WireDialog(
            title = stringResource(R.string.custom_backend_dialog_title),
            text = LocalContext.current.resources.stringWithStyledArgs(
                R.string.custom_backend_dialog_body,
                MaterialTheme.wireTypography.body01,
                MaterialTheme.wireTypography.body02,
                colorsScheme().onBackground,
                colorsScheme().onBackground,
                customBackendDialogState.serverLinks.title,
                customBackendDialogState.serverLinks.teams
            ),

            buttonsHorizontalAlignment = true,
            onDismiss = { dismissCustomBackendDialog() },
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = { dismissCustomBackendDialog() },
                text = stringResource(id = R.string.label_cancel),
                state = WireButtonState.Default
            ),
            optionButton1Properties = WireDialogButtonProperties(
                onClick = {
                    customBackendDialogProceedButtonClicked(customBackendDialogState.serverLinks)
                },
                text = stringResource(id = R.string.label_proceed),
                type = WireDialogButtonType.Primary,
                state =
                WireButtonState.Default
            )
        )
    }
}


data class CustomBEDeeplinkDialogState(val serverLinks: ServerConfig.Links = ServerConfig.DEFAULT, val shouldShowDialog: Boolean = false)
