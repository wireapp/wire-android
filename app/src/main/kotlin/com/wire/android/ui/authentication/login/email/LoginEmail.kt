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
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.DialogErrorStrings
import com.wire.android.util.dialogErrorStrings
import com.wire.kalium.logic.configuration.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LoginEmailScreen(
    serverConfig: ServerConfig,
    scrollState: ScrollState = rememberScrollState()
) {
    val scope = rememberCoroutineScope()
    val loginEmailViewModel: LoginEmailViewModel = hiltViewModel()
    val loginEmailState: LoginEmailState = loginEmailViewModel.loginState
    LoginEmailContent(
        scrollState = scrollState,
        loginEmailState = loginEmailState,
        onUserIdentifierChange = { loginEmailViewModel.onUserIdentifierChange(it) },
        onPasswordChange = { loginEmailViewModel.onPasswordChange(it) },
        onDialogDismiss = { loginEmailViewModel.onDialogDismiss() },
        onRemoveDeviceOpen = { loginEmailViewModel.onTooManyDevicesError() },
        onLoginButtonClick = suspend { loginEmailViewModel.login(serverConfig) },
        accountsBaseUrl = serverConfig.accountsBaseUrl,
        scope = scope
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LoginEmailContent(
    scrollState: ScrollState,
    loginEmailState: LoginEmailState,
    onUserIdentifierChange: (TextFieldValue) -> Unit,
    onPasswordChange: (TextFieldValue) -> Unit,
    onDialogDismiss: () -> Unit,
    onRemoveDeviceOpen: () -> Unit,
    onLoginButtonClick: suspend () -> Unit,
    accountsBaseUrl: String,
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
            userIdentifier = loginEmailState.userIdentifier,
            onUserIdentifierChange = onUserIdentifierChange,
            error = when (loginEmailState.loginEmailError) {
                LoginEmailError.TextFieldError.InvalidUserIdentifierError -> stringResource(R.string.login_error_invalid_user_identifier)
                else -> null
            }
        )
        PasswordInput(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.wireDimensions.spacing16x),
            password = loginEmailState.password,
            onPasswordChange = onPasswordChange
        )
        ForgotPasswordLabel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.wireDimensions.spacing16x),
            accountsBaseUrl = accountsBaseUrl
        )
        Spacer(modifier = Modifier.weight(1f))

        LoginButton(
            modifier = Modifier.fillMaxWidth(),
            loading = loginEmailState.loading,
            enabled = loginEmailState.loginEnabled
        ) {
            scope.launch {
                onLoginButtonClick()
            }
        }
    }

    if (loginEmailState.loginEmailError is LoginEmailError.DialogError) {
        val (title, message) = when (loginEmailState.loginEmailError) {
            LoginEmailError.DialogError.InvalidCredentialsError -> DialogErrorStrings(
                stringResource(id = R.string.login_error_invalid_credentials_title),
                stringResource(id = R.string.login_error_invalid_credentials_message)
            )
            // TODO: sync with design about the error message
            LoginEmailError.DialogError.UserAlreadyExists -> DialogErrorStrings("User Already LoggedIn", "UserAlreadyLoggedIn")
            is LoginEmailError.DialogError.GenericError -> {
                loginEmailState.loginEmailError.coreFailure.dialogErrorStrings(LocalContext.current.resources)
            }
        }
        WireDialog(
            title = title,
            text = message,
            onDismiss = onDialogDismiss,
            optionButton1Properties = WireDialogButtonProperties(
                onClick = onDialogDismiss,
                text = stringResource(id = R.string.label_ok),
                type = WireDialogButtonType.Primary,
            )
        )
    } else if (loginEmailState.loginEmailError is LoginEmailError.TooManyDevicesError) {
        onRemoveDeviceOpen()
    }
}

@Composable
private fun UserIdentifierInput(
    modifier: Modifier,
    userIdentifier: TextFieldValue,
    error: String?,
    onUserIdentifierChange: (TextFieldValue) -> Unit
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
private fun ForgotPasswordLabel(modifier: Modifier, accountsBaseUrl: String) {
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
                    onClick = { openForgotPasswordPage(context, accountsBaseUrl) }
                )
                .testTag("Forgot password?")
        )
    }
}

private fun openForgotPasswordPage(context: Context, accountsBaseUrl: String) {
    val url = "https://${accountsBaseUrl}/forgot"
    CustomTabsHelper.launchUrl(context, url)
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
            loginEmailState = LoginEmailState(),
            onUserIdentifierChange = { },
            onPasswordChange = { },
            onDialogDismiss = { },
            onRemoveDeviceOpen = { },
            onLoginButtonClick = suspend { },
            accountsBaseUrl = "",
            scope = scope
        )
    }
}
