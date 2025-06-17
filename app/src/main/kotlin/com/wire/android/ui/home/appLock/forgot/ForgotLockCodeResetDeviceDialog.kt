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
package com.wire.android.ui.home.appLock.forgot

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.stringWithStyledArgs

@Composable
fun ForgotLockCodeResetDeviceDialog(
    passwordTextState: TextFieldState,
    username: String,
    isPasswordRequired: Boolean,
    isPasswordValid: Boolean,
    isResetDeviceEnabled: Boolean,
    onResetDeviceClicked: () -> Unit,
    onDialogDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var keyboardController: SoftwareKeyboardController? = null
    val onDialogDismissHideKeyboard: () -> Unit = {
        keyboardController?.hide()
        onDialogDismissed()
    }
    WireDialog(
        modifier = modifier,
        title = stringResource(R.string.settings_forgot_lock_screen_reset_device),
        text = if (isPasswordRequired) {
            LocalContext.current.resources.stringWithStyledArgs(
                R.string.settings_forgot_lock_screen_reset_device_description,
                MaterialTheme.wireTypography.body01,
                MaterialTheme.wireTypography.body02,
                colorsScheme().onBackground,
                colorsScheme().onBackground,
                username
            )
        } else {
            AnnotatedString(stringResource(id = R.string.settings_forgot_lock_screen_reset_device_without_password_description))
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
        if (isPasswordRequired) {
            // keyboard controller from outside the Dialog doesn't work inside its content so we have to pass the state
            // to the dialog's content and use keyboard controller from there
            keyboardController = LocalSoftwareKeyboardController.current
            WirePasswordTextField(
                textState = passwordTextState,
                state = when {
                    !isPasswordValid -> WireTextFieldState.Error(stringResource(id = R.string.remove_device_invalid_password))
                    else -> WireTextFieldState.Default
                },
                autoFill = false,
                keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
                onKeyboardAction = { keyboardController?.hide() },
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
        properties = wireDialogPropertiesBuilder(dismissOnBackPress = false, dismissOnClickOutside = false),
        onDismiss = {},
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewForgotLockCodeResetDeviceDialog() {
    WireTheme {
        ForgotLockCodeResetDeviceDialog(TextFieldState(), "Username", false, true, true, {}, {})
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewForgotLockCodeResetDeviceWithoutPasswordDialog() {
    WireTheme {
        ForgotLockCodeResetDeviceDialog(TextFieldState(), "Username", true, true, true, {}, {})
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewForgotLockCodeResettingDeviceDialog() {
    WireTheme {
        ForgotLockCodeResettingDeviceDialog()
    }
}
