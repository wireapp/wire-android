/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui.authentication.devices.register

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType

@Composable
internal fun AuthenticationFailureDialog(
    failure: AuthenticationFailure,
    onDismiss: () -> Unit,
) {
    val strings = failure.dialogStrings()
    WireDialog(
        title = stringResource(strings.title),
        text = stringResource(strings.message),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    )
}

private fun AuthenticationFailure.dialogStrings(): AuthenticationFailureDialogStrings = when (this) {
    AuthenticationFailure.NoNetwork -> AuthenticationFailureDialogStrings(
        R.string.error_no_network_title,
        R.string.error_no_network_message,
    )

    AuthenticationFailure.ServerMiscommunication -> AuthenticationFailureDialogStrings(
        R.string.error_server_miscommunication_title,
        R.string.error_server_miscommunication_message,
    )

    AuthenticationFailure.Unknown -> AuthenticationFailureDialogStrings(
        R.string.error_unknown_title,
        R.string.error_unknown_message,
    )
}

private data class AuthenticationFailureDialogStrings(
    @StringRes val title: Int,
    @StringRes val message: Int,
)
