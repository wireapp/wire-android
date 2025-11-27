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
package com.wire.android.feature.cells.ui.publiclink.settings.password

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
fun PasswordNotAvailableDialog(
    onResetPassword: () -> Unit,
    onDismiss: () -> Unit,
) {
    WireDialog(
        title = stringResource(R.string.public_link_missing_password_dialog_title),
        text = stringResource(R.string.public_link_missing_password_dialog_message),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onResetPassword,
            text = stringResource(R.string.public_link_missing_password_dialog_message_action),
            type = WireDialogButtonType.Primary,
        ),
        dismissButtonProperties = WireDialogButtonProperties(
            text = stringResource(id = R.string.cancel),
            onClick = onDismiss
        ),
        buttonsHorizontalAlignment = false,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewPasswordDialog() {
    WireTheme {
        PasswordNotAvailableDialog(
            onResetPassword = {},
            onDismiss = {},
        )
    }
}
