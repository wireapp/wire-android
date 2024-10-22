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
package com.wire.android.ui.common.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
internal fun CustomServerNoNetworkDialog(
    onTryAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    WireDialog(
        title = stringResource(R.string.custom_backend_error_title),
        text = stringResource(R.string.custom_backend_error_no_internet_connection_body),
        onDismiss = onDismiss,
        buttonsHorizontalAlignment = false,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = {
                onTryAgain()
                onDismiss()
            },
            text = stringResource(id = R.string.custom_backend_error_no_internet_connection_try_again),
            type = WireDialogButtonType.Primary,
            state = WireButtonState.Default
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(id = R.string.label_cancel),
            type = WireDialogButtonType.Secondary,
            state = WireButtonState.Default
        )
    )
}

data class CustomServerNoNetworkDialogState(val customServerUrl: String) : CustomServerDialogState()

@PreviewMultipleThemes
@Composable
fun PreviewCustomServerNoNetworkDialog() = WireTheme {
    CustomServerNoNetworkDialog(
        onTryAgain = {},
        onDismiss = {}
    )
}
