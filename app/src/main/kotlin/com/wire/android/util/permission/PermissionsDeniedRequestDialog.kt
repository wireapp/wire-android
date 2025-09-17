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
package com.wire.android.util.permission

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.util.extension.openAppInfoScreen

/**
 * Allows to show a dialog to the user when the permission is denied.
 * Suggesting, to go to app settings and handle manually the permission.
 *
 * Useful for when an action is not possible without the permission.
 */
@Composable
fun PermissionsDeniedRequestDialog(
    @StringRes body: Int,
    @StringRes title: Int = R.string.app_permission_dialog_title,
    @StringRes positiveButton: Int = R.string.app_permission_dialog_settings_positive_button,
    @StringRes negativeButton: Int = R.string.app_permission_dialog_settings_negative_button,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    WireDialog(
        title = stringResource(id = title),
        text = stringResource(id = body),
        onDismiss = onDismiss,
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(id = negativeButton),
            state = WireButtonState.Default
        ),
        optionButton1Properties = WireDialogButtonProperties(
            onClick = {
                context.openAppInfoScreen()
                onDismiss()
            },
            text = stringResource(id = positiveButton),
            type = WireDialogButtonType.Primary,
            state = WireButtonState.Default
        )
    )
}
