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
    val proxyState: LoginState = loginEmailViewModel.loginState
    ProxyContent(
        proxyState = proxyState,
        onProxyIdentifierChange = { loginEmailViewModel.onProxyIdentifierChange(it) },
        onProxyPasswordChange = { loginEmailViewModel.onProxyPasswordChange(it) },
    )
}

@Composable
private fun ProxyContent(
    proxyState: LoginState,
    onProxyIdentifierChange: (TextFieldValue) -> Unit,
    onProxyPasswordChange: (TextFieldValue) -> Unit,
) {
    Column(
        modifier = Modifier
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.spacing32x))
        ProxyIdentifierInput(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.wireDimensions.spacing16x),
            proxyIdentifier = proxyState.proxyIdentifier,
            onProxyUserIdentifierChange = onProxyIdentifierChange,
            error = when (proxyState.loginError) {
                LoginError.TextFieldError.InvalidValue -> stringResource(R.string.login_error_invalid_user_identifier)
                else -> null
            },
        )
        ProxyPasswordInput(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.wireDimensions.spacing16x),
            proxyPassword = proxyState.proxyPassword,
            onProxyPasswordChange = onProxyPasswordChange
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
private fun ProxyIdentifierInput(
    modifier: Modifier,
    proxyIdentifier: TextFieldValue,
    error: String?,
    onProxyUserIdentifierChange: (TextFieldValue) -> Unit,
) {
    WireTextField(
        value = proxyIdentifier,
        onValueChange = onProxyUserIdentifierChange,
        placeholderText = stringResource(R.string.login_user_identifier_placeholder),
        labelText = stringResource(R.string.login_proxy_identifier_label),
        state = if (error != null) WireTextFieldState.Error(error) else WireTextFieldState.Default,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        modifier = modifier.testTag("emailField")
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ProxyPasswordInput(modifier: Modifier, proxyPassword: TextFieldValue, onProxyPasswordChange: (TextFieldValue) -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    WirePasswordTextField(
        value = proxyPassword,
        onValueChange = onProxyPasswordChange,
        imeAction = ImeAction.Done,
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
        modifier = modifier.testTag("passwordField")
    )
}

@Preview
@Composable
private fun ProxyScreenPreview() {
    WireTheme(isPreview = true) {
        ProxyContent(
            proxyState = LoginState(),
            onProxyIdentifierChange = { },
            onProxyPasswordChange = { },
        )
    }
}
