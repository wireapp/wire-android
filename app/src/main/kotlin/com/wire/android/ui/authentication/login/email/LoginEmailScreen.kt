package com.wire.android.ui.authentication.login.email

import android.content.Context
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.authentication.login.LoginError
import com.wire.android.ui.authentication.login.LoginErrorDialog
import com.wire.android.ui.authentication.login.LoginState

import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LoginEmailScreen(
    scrollState: ScrollState = rememberScrollState()
) {
    val scope = rememberCoroutineScope()
    val loginEmailViewModel: LoginEmailViewModel = hiltViewModel()
    val loginEmailState: LoginState = loginEmailViewModel.loginState
    LoginEmailContent(
        scrollState = scrollState,
        loginState = loginEmailState,
        onUserIdentifierChange = { loginEmailViewModel.onUserIdentifierChange(it) },
        onPasswordChange = { loginEmailViewModel.onPasswordChange(it) },
        onDialogDismiss = loginEmailViewModel::onDialogDismiss,
        onRemoveDeviceOpen = loginEmailViewModel::onTooManyDevicesError,
        onLoginButtonClick = suspend { loginEmailViewModel.login() },
        forgotPasswordUrl = loginEmailViewModel.serverConfig.forgotPassword,
        scope = scope
    )
}

@Composable
private fun LoginEmailContent(
    scrollState: ScrollState,
    loginState: LoginState,
    onUserIdentifierChange: (TextFieldValue) -> Unit,
    onPasswordChange: (TextFieldValue) -> Unit,
    onDialogDismiss: () -> Unit,
    onRemoveDeviceOpen: () -> Unit,
    onLoginButtonClick: suspend () -> Unit,
    forgotPasswordUrl: String,
    scope: CoroutineScope
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(scrollState)
            .padding(MaterialTheme.wireDimensions.spacing16x)
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.spacing32x))
        UserIdentifierInput(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.wireDimensions.spacing16x),
            userIdentifier = loginState.userIdentifier,
            onUserIdentifierChange = onUserIdentifierChange,
            error = when (loginState.loginError) {
                LoginError.TextFieldError.InvalidValue -> stringResource(R.string.login_error_invalid_user_identifier)
                else -> null
            }
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
        Spacer(modifier = Modifier.weight(1f))

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

    if (loginState.loginError is LoginError.DialogError) {
        LoginErrorDialog(loginState.loginError, onDialogDismiss)
    } else if (loginState.loginError is LoginError.TooManyDevicesError) {
        onRemoveDeviceOpen()
    }
}

@Composable
private fun UserIdentifierInput(
    modifier: Modifier,
    userIdentifier: TextFieldValue,
    error: String?,
    onUserIdentifierChange: (TextFieldValue) -> Unit,
) {
    WireTextField(
        value = userIdentifier,
        onValueChange = onUserIdentifierChange,
        placeholderText = stringResource(R.string.login_user_identifier_placeholder),
        labelText = stringResource(R.string.login_user_identifier_label),
        state = if (error != null) WireTextFieldState.Error(error) else WireTextFieldState.Default,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        modifier = modifier.testTag("emailField")
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
        modifier = modifier.testTag("passwordField")
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

@OptIn(ExperimentalAnimationApi::class)
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

@Preview
@Composable
private fun LoginEmailScreenPreview() {
    val scope = rememberCoroutineScope()
    WireTheme(isPreview = true) {
        LoginEmailContent(
            scrollState = rememberScrollState(),
            loginState = LoginState(),
            onUserIdentifierChange = { },
            onPasswordChange = { },
            onDialogDismiss = { },
            onRemoveDeviceOpen = { },
            onLoginButtonClick = suspend { },
            forgotPasswordUrl = "",
            scope = scope
        )
    }
}
