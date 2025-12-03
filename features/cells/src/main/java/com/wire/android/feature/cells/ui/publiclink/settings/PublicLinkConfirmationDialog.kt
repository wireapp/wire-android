/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.cells.ui.publiclink.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.theme.WireTheme

@Composable
private fun PublicLinkConfirmationDialog(
    title: String,
    message: String,
    actionText: String,
    onResult: (confirmed: Boolean) -> Unit,
) {
    WireDialog(
        title = title,
        text = message,
        onDismiss = { onResult(false) },
        optionButton1Properties = WireDialogButtonProperties(
            onClick = { onResult(true) },
            text = actionText,
            type = WireDialogButtonType.Primary,
        ),
        dismissButtonProperties = WireDialogButtonProperties(
            text = stringResource(id = R.string.cancel),
            onClick = { onResult(false) }
        ),
        buttonsHorizontalAlignment = false,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)
    )
}

@Composable
internal fun RemovePasswordDialog(onResult: (confirmed: Boolean) -> Unit) =
    PublicLinkConfirmationDialog(
        title = stringResource(R.string.public_link_password_remove_dialog_title),
        message = stringResource(R.string.public_link_password_remove_dialog_message),
        actionText = stringResource(R.string.public_link_password_remove_dialog_action),
        onResult = onResult,
    )

@Composable
internal fun RemovePublicLinkDialog(onResult: (confirmed: Boolean) -> Unit) =
    PublicLinkConfirmationDialog(
        title = stringResource(R.string.public_link_remove_dialog_title),
        message = stringResource(R.string.public_link_remove_dialog_message),
        actionText = stringResource(R.string.public_link_remove_dialog_action),
        onResult = onResult,
    )

@PreviewMultipleThemes
@Composable
private fun PreviewRemovePasswordDialog() {
    WireTheme {
        RemovePasswordDialog(
            onResult = {},
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewRemoveLinkDialog() {
    WireTheme {
        RemovePublicLinkDialog(
            onResult = {},
        )
    }
}
