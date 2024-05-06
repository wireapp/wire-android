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

package com.wire.android.ui.authentication.login.sso

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.authentication.login.LoginError
import com.wire.android.ui.authentication.login.LoginErrorDialog
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dialogs.CustomServerDialog
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.deeplink.DeepLinkResult
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun LoginSSOScreen(
    onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean) -> Unit,
    onRemoveDeviceNeeded: () -> Unit,
    ssoLoginResult: DeepLinkResult.SSOLogin?,
    scrollState: ScrollState = rememberScrollState()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val loginSSOViewModel: LoginSSOViewModel = hiltViewModel()

    LaunchedEffect(ssoLoginResult) {
        loginSSOViewModel.handleSSOResult(ssoLoginResult, onSuccess)
    }
    LoginSSOContent(
        scrollState = scrollState,
        loginState = loginSSOViewModel.loginState,
        onCodeChange = loginSSOViewModel::onSSOCodeChange,
        onErrorDialogDismiss = loginSSOViewModel::onDialogDismiss,
        onRemoveDeviceOpen = {
            loginSSOViewModel.clearLoginErrors()
            onRemoveDeviceNeeded()
        },
        // TODO: replace with retrieved ServerConfig from sso login
        onLoginButtonClick = loginSSOViewModel::login,
        ssoLoginResult = ssoLoginResult,
        onCustomServerDialogDismiss = loginSSOViewModel::onCustomServerDialogDismiss,
        onCustomServerDialogConfirm = loginSSOViewModel::onCustomServerDialogConfirm
    )

    LaunchedEffect(loginSSOViewModel) {
        loginSSOViewModel.openWebUrl.onEach { CustomTabsHelper.launchUrl(context, it) }.launchIn(scope)
    }
}

@Composable
private fun LoginSSOContent(
    scrollState: ScrollState,
    loginState: LoginState,
    onCodeChange: (TextFieldValue) -> Unit,
    onErrorDialogDismiss: () -> Unit,
    onRemoveDeviceOpen: () -> Unit,
    onLoginButtonClick: () -> Unit,
    onCustomServerDialogDismiss: () -> Unit,
    onCustomServerDialogConfirm: () -> Unit,
    ssoLoginResult: DeepLinkResult.SSOLogin?
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(scrollState)
            .padding(MaterialTheme.wireDimensions.spacing16x)
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.spacing32x))
        SSOCodeInput(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.wireDimensions.spacing16x),
            ssoCode = loginState.userInput,
            onCodeChange = onCodeChange,
            error = when (loginState.loginError) {
                LoginError.TextFieldError.InvalidValue -> stringResource(R.string.login_error_invalid_sso_code_format)
                else -> null
            }
        )
        Spacer(modifier = Modifier.weight(1f))
        LoginButton(
            modifier = Modifier.fillMaxWidth(),
            loading = loginState.ssoLoginLoading,
            enabled = loginState.ssoLoginEnabled,
            onClick = onLoginButtonClick
        )
    }
    if (loginState.loginError is LoginError.DialogError) {
        LoginErrorDialog(loginState.loginError, onErrorDialogDismiss, {}, ssoLoginResult)
    } else if (loginState.loginError is LoginError.TooManyDevicesError) {
        onRemoveDeviceOpen()
    }

    if (loginState.customServerDialogState != null) {
        CustomServerDialog(
            serverLinks = loginState.customServerDialogState.serverLinks,
            onDismiss = onCustomServerDialogDismiss,
            onConfirm = onCustomServerDialogConfirm
        )
    }
}

@Composable
private fun SSOCodeInput(
    modifier: Modifier,
    ssoCode: TextFieldValue,
    error: String?,
    onCodeChange: (TextFieldValue) -> Unit
) {
    WireTextField(
        value = ssoCode,
        onValueChange = onCodeChange,
        labelText = stringResource(R.string.login_sso_code_label),
        state = if (error != null) WireTextFieldState.Error(error) else WireTextFieldState.Default,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        modifier = modifier.testTag("ssoCodeField")
    )
}

@Composable
private fun LoginButton(modifier: Modifier, loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier) {
        val text = if (loading) stringResource(R.string.label_logging_in) else stringResource(R.string.label_login)
        WirePrimaryButton(
            text = text,
            onClick = onClick,
            state = if (enabled) WireButtonState.Default else WireButtonState.Disabled,
            loading = loading,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ssoLoginButton")
        )
    }
}

@Preview
@Composable
fun PreviewLoginSSOScreen() {
    WireTheme {
        LoginSSOContent(rememberScrollState(), LoginState(), {}, {}, {}, {}, {}, {}, null)
    }
}
