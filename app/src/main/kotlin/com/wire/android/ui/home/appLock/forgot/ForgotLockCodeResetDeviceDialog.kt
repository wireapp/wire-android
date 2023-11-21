/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.home.appLock.forgot

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.DialogProperties
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.stringWithStyledArgs

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ForgotLockCodeResetDeviceDialog(
    username: String,
    isPasswordNotRequired: Boolean,
    isPasswordValid: Boolean,
    isResetDeviceEnabled: Boolean,
    onPasswordChanged: (TextFieldValue) -> Unit,
    onResetDeviceClicked: () -> Unit,
    onDialogDismissed: () -> Unit
) {
    var backupPassword by remember { mutableStateOf(TextFieldValue("")) }
    var keyboardController: SoftwareKeyboardController? = null
    val onDialogDismissHideKeyboard: () -> Unit = {
        keyboardController?.hide()
        onDialogDismissed()
    }
    WireDialog(
        title = stringResource(R.string.settings_forgot_lock_screen_reset_device),
        text = if (isPasswordNotRequired) {
            AnnotatedString(stringResource(id = R.string.settings_forgot_lock_screen_reset_device_without_password_description))
        } else {
            LocalContext.current.resources.stringWithStyledArgs(
                R.string.settings_forgot_lock_screen_reset_device_description,
                MaterialTheme.wireTypography.body01,
                MaterialTheme.wireTypography.body02,
                colorsScheme().onBackground,
                colorsScheme().onBackground,
                username
            )
        },
        onDismiss = onDialogDismissHideKeyboard,
        buttonsHorizontalAlignment = false,
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onDialogDismissHideKeyboard,
            text = stringResource(id = R.string.label_cancel),
            state = WireButtonState.Default
        ),
        optionButton1Properties = WireDialogButtonProperties(
            onClick = {
                keyboardController?.hide()
                onResetDeviceClicked()
            },
            text = stringResource(id = R.string.settings_forgot_lock_screen_reset_device),
            type = WireDialogButtonType.Primary,
            state = if (!isResetDeviceEnabled) WireButtonState.Disabled else WireButtonState.Error
        )
    ) {
        if (!isPasswordNotRequired) {
            // keyboard controller from outside the Dialog doesn't work inside its content so we have to pass the state
            // to the dialog's content and use keyboard controller from there
            keyboardController = LocalSoftwareKeyboardController.current
            WirePasswordTextField(
                state = when {
                    !isPasswordValid -> WireTextFieldState.Error(stringResource(id = R.string.remove_device_invalid_password))
                    else -> WireTextFieldState.Default
                },
                value = backupPassword,
                onValueChange = {
                    backupPassword = it
                    onPasswordChanged(it)
                },
                autofill = false,
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                modifier = Modifier.padding(bottom = dimensions().spacing16x)
            )
        }
    }
}

@Composable
fun ForgotLockCodeResettingDeviceDialog() {
    WireDialog(
        title = stringResource(R.string.settings_forgot_lock_screen_please_wait_label),
        titleLoading = true,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        onDismiss = {},
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewForgotLockCodeResetDeviceDialog() {
    WireTheme {
        ForgotLockCodeResetDeviceDialog("Username", false, true, true, {}, {}, {})
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewForgotLockCodeResetDeviceWithoutPasswordDialog() {
    WireTheme {
        ForgotLockCodeResetDeviceDialog("Username", true, true, true, {}, {}, {})
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewForgotLockCodeResettingDeviceDialog() {
    WireTheme {
        ForgotLockCodeResettingDeviceDialog()
    }
}
