package com.wire.android.ui.authentication.login

import android.content.Context
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
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
import com.wire.android.ui.common.appBarElevation
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.DialogErrorStrings
import com.wire.android.util.dialogErrorStrings
import com.wire.kalium.logic.configuration.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
fun LoginScreen(serverConfig: ServerConfig) {
    val scope = rememberCoroutineScope()
    val loginViewModel: LoginViewModel = hiltViewModel()
    val loginState: LoginState = loginViewModel.loginState
    LoginContent(
        loginState = loginState,
        onUserIdentifierChange = { loginViewModel.onUserIdentifierChange(it) },
        onBackPressed = { loginViewModel.navigateBack() },
        onPasswordChange = { loginViewModel.onPasswordChange(it) },
        onDialogDismiss = { loginViewModel.onDialogDismiss() },
        onRemoveDeviceOpen = { loginViewModel.onTooManyDevicesError() },
        onLoginButtonClick = suspend { loginViewModel.login(serverConfig) },
        accountsBaseUrl = serverConfig.accountsBaseUrl,
        //todo: temporary to show the remoteConfig
        serverTitle = serverConfig.title,
        scope = scope
    )
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun LoginContent(
    loginState: LoginState,
    onUserIdentifierChange: (TextFieldValue) -> Unit,
    onBackPressed: () -> Unit,
    onPasswordChange: (TextFieldValue) -> Unit,
    onDialogDismiss: () -> Unit,
    onRemoveDeviceOpen: () -> Unit,
    onLoginButtonClick: suspend () -> Unit,
    accountsBaseUrl: String,
    serverTitle: String,
    scope: CoroutineScope
) {
    val scrollState = rememberScrollState()
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.appBarElevation(),
                title = "${stringResource(R.string.login_title)} $serverTitle",
                onNavigationPressed = onBackPressed
            )
        }
    ) {
        Column(modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(scrollState)
            .padding(MaterialTheme.wireDimensions.spacing16x)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            UserIdentifierInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.wireDimensions.spacing16x),
                userIdentifier = loginState.userIdentifier,
                onUserIdentifierChange = onUserIdentifierChange,
                error = when (loginState.loginError) {
                    LoginError.TextFieldError.InvalidUserIdentifierError -> stringResource(R.string.login_error_invalid_user_identifier)
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
                accountsBaseUrl = accountsBaseUrl
            )
            Spacer(modifier = Modifier.weight(1f))

            LoginButton(
                modifier = Modifier.fillMaxWidth(),
                loading = loginState.loading,
                enabled = loginState.loginEnabled
            ) {
                scope.launch {
                    onLoginButtonClick()
                }
            }
        }

        if (loginState.loginError is LoginError.DialogError) {
            val (title, message) = when (loginState.loginError) {
                LoginError.DialogError.InvalidCredentialsError -> DialogErrorStrings(
                    stringResource(id = R.string.login_error_invalid_credentials_title),
                    stringResource(id = R.string.login_error_invalid_credentials_message)
                )
                // TODO: sync with design about the error message
                LoginError.DialogError.UserAlreadyExists -> DialogErrorStrings("User Already LoggedIn", "UserAlreadyLoggedIn")
                is LoginError.DialogError.GenericError -> {
                    loginState.loginError.coreFailure.dialogErrorStrings(LocalContext.current.resources)
                }
            }
            WireDialog(
                title = title,
                text = message,
                onDismiss = onDialogDismiss,
                confirmButtonProperties = WireDialogButtonProperties(
                    onClick = onDialogDismiss,
                    text = stringResource(id = R.string.label_ok),
                    type = WireDialogButtonType.Primary,
                )
            )
        } else if (loginState.loginError is LoginError.TooManyDevicesError) {
            onRemoveDeviceOpen()
        }
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
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false, imeAction = ImeAction.Done),
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
                ).testTag("Forgot password?")
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
            modifier = Modifier.fillMaxWidth().testTag("loginButton")
        )
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    val scope = rememberCoroutineScope()
    WireTheme(isPreview = true) {
        LoginContent(
            loginState = LoginState(),
            onUserIdentifierChange = { },
            onBackPressed = { },
            onPasswordChange = { },
            onDialogDismiss = { },
            onRemoveDeviceOpen = { },
            onLoginButtonClick = suspend { },
            accountsBaseUrl = "",
            serverTitle = "",
            scope = scope
        )
    }
}

