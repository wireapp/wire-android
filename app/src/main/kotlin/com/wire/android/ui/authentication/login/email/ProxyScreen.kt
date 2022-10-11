package com.wire.android.ui.authentication.login.email

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.authentication.login.LoginError
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ProxyScreen() {
    val loginEmailViewModel: LoginEmailViewModel = hiltViewModel()
    val loginEmailState: LoginState = loginEmailViewModel.loginState
    ProxyContent(
        loginState = loginEmailState,
        onUserIdentifierChange = { loginEmailViewModel.onUserIdentifierChange(it) },
        onPasswordChange = { loginEmailViewModel.onPasswordChange(it) },
        onDialogDismiss = loginEmailViewModel::onDialogDismiss,
        onRemoveDeviceOpen = loginEmailViewModel::onTooManyDevicesError,
        forgotPasswordUrl = loginEmailViewModel.serverConfig.forgotPassword,
        serverTitle = loginEmailViewModel.serverConfig.title
    )
}

@Composable
private fun ProxyContent(
    loginState: LoginState,
    onUserIdentifierChange: (TextFieldValue) -> Unit,
    onPasswordChange: (TextFieldValue) -> Unit,
    onDialogDismiss: () -> Unit,
    onRemoveDeviceOpen: () -> Unit,
    forgotPasswordUrl: String,
    serverTitle: String
) {
    Column(
        modifier = Modifier
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
            },
            serverTitle = serverTitle
        )
        PasswordInput(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.wireDimensions.spacing16x),
            password = loginState.password,
            onPasswordChange = onPasswordChange
        )

        Spacer(modifier = Modifier.weight(1f))

    }

//    if (loginState.loginError is LoginError.DialogError && loginState.loginError !is LoginError.DialogError.InvalidSession) {
//        LoginErrorDialog(loginState.loginError, onDialogDismiss)
//    } else if (loginState.loginError is LoginError.TooManyDevicesError) {
//        onRemoveDeviceOpen()
//    }
}

@Composable
private fun UserIdentifierInput(
    modifier: Modifier,
    userIdentifier: TextFieldValue,
    error: String?,
    onUserIdentifierChange: (TextFieldValue) -> Unit,
    // todo: temporary to show to pointing server
    serverTitle: String
) {
    WireTextField(
        value = userIdentifier,
        onValueChange = onUserIdentifierChange,
        placeholderText = stringResource(R.string.login_user_identifier_placeholder),
        labelText = stringResource(R.string.login_user_identifier_label) + " on $serverTitle",
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

@Preview
@Composable
private fun LoginEmailScreenPreview() {
    WireTheme(isPreview = true) {
        ProxyContent(
            loginState = LoginState(),
            onUserIdentifierChange = { },
            onPasswordChange = { },
            onDialogDismiss = { },
            onRemoveDeviceOpen = { },
            forgotPasswordUrl = "",
            serverTitle = "Test Server"
        )
    }
}
