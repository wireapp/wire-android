package com.wire.android.ui.authentication.login.email

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.authentication.login.LoginError
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ProxyScreen() {
    val loginEmailViewModel: LoginEmailViewModel = hiltViewModel()
    val proxyState: LoginState = loginEmailViewModel.loginState
    ProxyContent(
        proxyState = proxyState,
        apiProxyUrl = loginEmailViewModel.serverConfig.apiProxy?.host,
        onProxyIdentifierChange = { loginEmailViewModel.onProxyIdentifierChange(it) },
        onProxyPasswordChange = { loginEmailViewModel.onProxyPasswordChange(it) },
    )
}

@Composable
private fun ProxyContent(
    proxyState: LoginState,
    apiProxyUrl: String?,
    onProxyIdentifierChange: (TextFieldValue) -> Unit,
    onProxyPasswordChange: (TextFieldValue) -> Unit,
) {
    Column(
        modifier = Modifier
    ) {
        Divider(color = MaterialTheme.wireColorScheme.divider, thickness = Dp.Hairline)
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
        labelText = stringResource(R.string.label_proxy_password),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
        modifier = modifier.testTag("passwordField"),
        autofillTypes = listOf()
    )
}

@Preview
@Composable
fun PreviewProxyScreen() {
    WireTheme(isPreview = true) {
        ProxyContent(
            proxyState = LoginState(),
            apiProxyUrl = "",
            onProxyIdentifierChange = { },
            onProxyPasswordChange = { },
        )
    }
}
