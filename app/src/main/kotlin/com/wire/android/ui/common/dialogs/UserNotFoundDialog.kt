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

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun UserNotFoundDialog(
    onActionButtonClicked: () -> Unit
) {
    UserNotFoundDialogContent(
        onConfirm = onActionButtonClicked,
        onDismiss = onActionButtonClicked,
        buttonText = R.string.label_ok,
        dialogProperties = wireDialogPropertiesBuilder(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}

@Composable
fun UserNotFoundDialogContent(
    @StringRes buttonText: Int,
    onConfirm: () -> Unit,
    dialogProperties: DialogProperties = wireDialogPropertiesBuilder(),
    onDismiss: () -> Unit
) {
    WireDialog(
        title = stringResource(R.string.connection_label_user_not_found_warning_title),
        text = stringResource(R.string.connection_label_user_not_found_warning_description),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            text = stringResource(buttonText),
            onClick = onConfirm,
            type = WireDialogButtonType.Primary
        ),
        properties = dialogProperties
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewUserNotFoundDialog() {
    WireTheme {
        UserNotFoundDialogContent(onConfirm = { }, onDismiss = { }, buttonText = R.string.label_ok)
    }
}
