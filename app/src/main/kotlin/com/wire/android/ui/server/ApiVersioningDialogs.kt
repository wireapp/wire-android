package com.wire.android.ui.server

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.theme.WireTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ApiVersioningDialog(
    title: String,
    text: String,
    actionText: String,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    isPreview: Boolean = false
) {
    WireDialog(
        title = title,
        text = text,
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            text = actionText,
            onClick = onAction,
            type = WireDialogButtonType.Primary
        ),
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = isPreview  // for some reason, @Preview doesn't work well with width other than platform default
        )
    )
}

@Composable
fun ClientUpdateRequiredDialog(
    onClose: () -> Unit,
    onUpdate: () -> Unit,
    isPreview: Boolean = false
) {
    ApiVersioningDialog(
        title = stringResource(id = R.string.api_versioning_client_update_required_title),
        text = stringResource(id = R.string.api_versioning_client_update_required_message),
        actionText = stringResource(id = R.string.label_update),
        onDismiss = onClose,
        onAction = onUpdate,
        isPreview = isPreview
    )
}

@Composable
fun ServerVersionNotSupportedDialog(
    onClose: () -> Unit,
    isPreview: Boolean = false
) {
    ApiVersioningDialog(
        title = stringResource(id = R.string.api_versioning_server_version_not_supported_title),
        text = stringResource(id = R.string.api_versioning_server_version_not_supported_message),
        actionText = stringResource(id = R.string.label_close),
        onDismiss = onClose,
        onAction = onClose,
        isPreview = isPreview
    )
}

@Preview
@Composable
fun ClientUpdateRequiredDialogPreview() {
    ClientUpdateRequiredDialog(onClose = {}, onUpdate = {}, isPreview = true)
}

@Preview
@Composable
fun ServerVersionNotSupportedDialogPreview() {
    ServerVersionNotSupportedDialog(onClose = {}, isPreview = true)
}
