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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.button.GeneratePasswordButton
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.spacers.fillVerticalSpace
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.theme.WireTheme

@Composable
fun ColumnScope.PasswordSetupView(
    isPasswordValid: Boolean,
    showProgress: Boolean,
    passwordTextState: TextFieldState,
    onGeneratePassword: () -> Unit,
    onCopyPassword: () -> Unit,
    onSetPassword: () -> Unit,
) {
    GeneratePasswordButton(
        onClick = onGeneratePassword,
    )
    VerticalSpace.x12()
    WirePasswordTextField(
        textState = passwordTextState,
        labelMandatoryIcon = false,
        labelText = stringResource(R.string.public_link_set_password_text_field_label),
        placeholderText = stringResource(R.string.public_link_set_password_text_field_placeholder),
        keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Next),
        modifier = Modifier
            .testTag("password"),
        autoFill = false
    )
    VerticalSpace.x12()
    WireSecondaryButton(
        leadingIcon = {
            Box(
                modifier = Modifier.padding(end = dimensions().spacing8x)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_password_copy),
                    contentDescription = null,
                )
            }
        },
        state = if (isPasswordValid) WireButtonState.Default else WireButtonState.Disabled,
        text = stringResource(R.string.public_link_set_password_copy_password),
        onClick = onCopyPassword,
    )
    fillVerticalSpace()
    WirePrimaryButton(
        text = stringResource(R.string.public_link_set_password_button),
        loading = showProgress,
        state = if (isPasswordValid && !showProgress) WireButtonState.Default else WireButtonState.Disabled,
        onClick = onSetPassword,
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewPasswordSetupView() {
    WireTheme {
        Column {
            PasswordSetupView(
                isPasswordValid = true,
                showProgress = false,
                passwordTextState = TextFieldState(""),
                onGeneratePassword = {},
                onCopyPassword = {},
                onSetPassword = {}
            )
        }
    }
}
