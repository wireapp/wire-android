/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.server

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.wireDialogPropertiesBuilder

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
        properties = wireDialogPropertiesBuilder(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = isPreview // for some reason, @Preview doesn't work well with width other than platform default
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
fun PreviewClientUpdateRequiredDialog() {
    ClientUpdateRequiredDialog(onClose = {}, onUpdate = {}, isPreview = true)
}

@Preview
@Composable
fun PreviewServerVersionNotSupportedDialog() {
    ServerVersionNotSupportedDialog(onClose = {}, isPreview = true)
}
