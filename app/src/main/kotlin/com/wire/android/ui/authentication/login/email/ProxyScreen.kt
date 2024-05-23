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

package com.wire.android.ui.authentication.login.email

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun ProxyScreen(
    proxyIdentifierState: TextFieldState,
    proxyPasswordState: TextFieldState,
    proxyState: LoginEmailState,
    apiProxyUrl: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.wireColorScheme.divider)
        Text(
            text = stringResource(R.string.label_proxy_credentials),
            style = MaterialTheme.wireTypography.title03.copy(
                color = colorsScheme().labelText
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = MaterialTheme.wireDimensions.spacing16x
                )
        )

        apiProxyUrl?.let {
            Text(
                text = stringResource(R.string.proxy_credential_description, it),
                style = MaterialTheme.wireTypography.body01.copy(color = colorsScheme().onBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = MaterialTheme.wireDimensions.spacing16x
                    )
            )
        }

        ProxyIdentifierInput(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.wireDimensions.spacing16x),
            proxyIdentifierState = proxyIdentifierState,
            error = when (proxyState.flowState) {
                LoginState.Error.TextFieldError.InvalidValue -> stringResource(R.string.login_error_invalid_user_identifier)
                else -> null
            },
        )
        ProxyPasswordInput(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.wireDimensions.spacing16x),
            proxyPasswordState = proxyPasswordState,
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ProxyIdentifierInput(
    modifier: Modifier,
    proxyIdentifierState: TextFieldState,
    error: String?,
) {
    WireTextField(
        textState = proxyIdentifierState,
        placeholderText = stringResource(R.string.login_user_identifier_placeholder),
        labelText = stringResource(R.string.login_proxy_identifier_label),
        state = if (error != null) WireTextFieldState.Error(error) else WireTextFieldState.Default,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        modifier = modifier.testTag("emailField")
    )
}

@Composable
private fun ProxyPasswordInput(
    modifier: Modifier,
    proxyPasswordState: TextFieldState
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    WirePasswordTextField(
        textState = proxyPasswordState,
        labelText = stringResource(R.string.label_proxy_password),
        keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
        onKeyboardAction = { keyboardController?.hide() },
        modifier = modifier.testTag("passwordField"),
        autoFill = false
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewProxyContent() = WireTheme {
    ProxyScreen(
        proxyState = LoginEmailState(),
        apiProxyUrl = "",
        proxyIdentifierState = TextFieldState(),
        proxyPasswordState = TextFieldState(),
    )
}
