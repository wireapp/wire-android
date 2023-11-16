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
 *
 *
 */

package com.wire.android.ui.authentication.login.email

import android.content.Context
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.authentication.login.LoginError
import com.wire.android.ui.authentication.login.LoginErrorDialog
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.rememberBottomBarElevationState
import com.wire.android.ui.common.textfield.AutoFillTextField
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.textfield.clearAutofillTree
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun LoginEmailScreen(
    onSuccess: (initialSyncCompleted: Boolean) -> Unit,
    onRemoveDeviceNeeded: () -> Unit,
    loginEmailViewModel: LoginEmailViewModel,
    scrollState: ScrollState = rememberScrollState()
) {
    val scope = rememberCoroutineScope()
    val loginEmailState: LoginState = loginEmailViewModel.loginState

    clearAutofillTree()

    LoginEmailContent(
        scrollState = scrollState,
        loginState = loginEmailState,
        onUserIdentifierChange = loginEmailViewModel::onUserIdentifierChange,
        onPasswordChange = loginEmailViewModel::onPasswordChange,
        onDialogDismiss = loginEmailViewModel::onDialogDismiss,
        onRemoveDeviceOpen = {
            loginEmailViewModel.clearLoginErrors()
            onRemoveDeviceNeeded()
        },
        onLoginButtonClick = {
            loginEmailViewModel.login(onSuccess)
        },
        onUpdateApp = loginEmailViewModel::updateTheApp,
        forgotPasswordUrl = loginEmailViewModel.serverConfig.forgotPassword,
        scope = scope
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LoginEmailContent(
    scrollState: ScrollState,
    loginState: LoginState,
    onUserIdentifierChange: (TextFieldValue) -> Unit,
    onPasswordChange: (TextFieldValue) -> Unit,
    onDialogDismiss: () -> Unit,
    onRemoveDeviceOpen: () -> Unit,
    onLoginButtonClick: () -> Unit,
    onUpdateApp: () -> Unit,
    forgotPasswordUrl: String,
    scope: CoroutineScope
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
    ) {

        Column(
            modifier = Modifier
                .weight(weight = 1f, fill = true)
                .verticalScroll(scrollState)
                .padding(MaterialTheme.wireDimensions.spacing16x)
                .semantics {
                    testTagsAsResourceId = true
                }
        ) {
            if (loginState.isProxyAuthRequired) {
                Text(
                    text = stringResource(R.string.label_wire_credentials),
                    style = MaterialTheme.wireTypography.title03.copy(
                        color = colorsScheme().labelText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = MaterialTheme.wireDimensions.spacing16x
                        )
                )
            }
            UserIdentifierInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.wireDimensions.spacing16x),
                userIdentifier = loginState.userIdentifier,
                onUserIdentifierChange = onUserIdentifierChange,
                error = when (loginState.loginError) {
                    LoginError.TextFieldError.InvalidValue -> stringResource(R.string.login_error_invalid_user_identifier)
                    else -> null
                },
                isEnabled = loginState.userIdentifierEnabled
            )
            PasswordInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.wireDimensions.spacing16x),
                password = loginState.password,
                onPasswordChange = onPasswordChange
            )
            ForgotPasswordLabel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.wireDimensions.spacing16x),
                forgotPasswordUrl = forgotPasswordUrl
            )
            if (loginState.isProxyAuthRequired) {
                ProxyScreen()
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        Surface(
            shadowElevation = scrollState.rememberBottomBarElevationState().value,
            color = MaterialTheme.wireColorScheme.background,
            modifier = Modifier.semantics {
                testTagsAsResourceId = true
            }
        ) {
            Box(modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)) {
                LoginButton(
                    modifier = Modifier.fillMaxWidth(),
                    loading = loginState.emailLoginLoading,
                    enabled = loginState.emailLoginEnabled
                ) {
                    scope.launch {
                        onLoginButtonClick()
                    }
                }
            }
        }
    }

    if (loginState.loginError is LoginError.DialogError) {
        LoginErrorDialog(loginState.loginError, onDialogDismiss, onUpdateApp)
    } else if (loginState.loginError is LoginError.TooManyDevicesError) {
        onRemoveDeviceOpen()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun UserIdentifierInput(
    modifier: Modifier,
    userIdentifier: TextFieldValue,
    error: String?,
    onUserIdentifierChange: (TextFieldValue) -> Unit,
    isEnabled: Boolean,
) {
    AutoFillTextField(
        autofillTypes = listOf(AutofillType.EmailAddress, AutofillType.Username),
        value = userIdentifier,
        onValueChange = onUserIdentifierChange,
        placeholderText = stringResource(R.string.login_user_identifier_placeholder),
        labelText = stringResource(R.string.login_user_identifier_label),
        state = when {
            !isEnabled -> WireTextFieldState.Disabled
            error != null -> WireTextFieldState.Error(error)
            else -> WireTextFieldState.Default
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        modifier = modifier.testTag("emailField"),
        testTag = "userIdentifierInput"
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PasswordInput(modifier: Modifier, password: TextFieldValue, onPasswordChange: (TextFieldValue) -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    WirePasswordTextField(
        value = password,
        onValueChange = onPasswordChange,
        imeAction = ImeAction.Done,
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
        modifier = modifier.testTag("passwordField"),
        autofill = true,
        testTag = "PasswordInput"
    )
}

@Composable
private fun ForgotPasswordLabel(modifier: Modifier, forgotPasswordUrl: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        val context = LocalContext.current
        Text(
            text = stringResource(R.string.login_forgot_password),
            style = MaterialTheme.wireTypography.body02.copy(
                textDecoration = TextDecoration.Underline,
                color = MaterialTheme.colorScheme.primary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { openForgotPasswordPage(context, forgotPasswordUrl) }
                )
                .testTag("Forgot password?")
        )
    }
}

private fun openForgotPasswordPage(context: Context, forgotPasswordUrl: String) {
    CustomTabsHelper.launchUrl(context, forgotPasswordUrl).also {
        appLogger.d(forgotPasswordUrl)
    }
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
                .testTag("loginButton")
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewLoginEmailScreen() {
    val scope = rememberCoroutineScope()
    WireTheme {
        LoginEmailContent(
            scrollState = rememberScrollState(),
            loginState = LoginState(),
            onUserIdentifierChange = { },
            onPasswordChange = { },
            onDialogDismiss = { },
            onRemoveDeviceOpen = { },
            onLoginButtonClick = { },
            onUpdateApp = {},
            forgotPasswordUrl = "",
            scope = scope
        )
    }
}
