package com.wire.android.ui.authentication.login.sso

import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
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
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.dialogErrorStrings
import com.wire.kalium.logic.configuration.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LoginSSOScreen(
    serverConfig: ServerConfig,
    ssoLoginResult: DeepLinkResult.SSOLogin?,
    scrollState: ScrollState = rememberScrollState()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val loginSSOViewModel: LoginSSOViewModel = hiltViewModel()
    val loginSSOState: LoginSSOState = loginSSOViewModel.loginState
    LoginSSOContent(
        scrollState = scrollState,
        loginSSOState = loginSSOState,
        onCodeChange = { loginSSOViewModel.onSSOCodeChange(it) },
        onDialogDismiss = { loginSSOViewModel.onDialogDismiss() },
        onLoginButtonClick = suspend { loginSSOViewModel.login(serverConfig) },
        scope = scope
    )

    if(ssoLoginResult is DeepLinkResult.SSOLogin.Success)
        loginSSOViewModel.establishSSOSession(ssoLoginResult)

    LaunchedEffect(loginSSOViewModel) {
        loginSSOViewModel.openWebUrl
            .onEach { CustomTabsHelper.launchUrl(context, it) }
            .launchIn(scope)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LoginSSOContent(
    scrollState: ScrollState,
    loginSSOState: LoginSSOState,
    onCodeChange: (TextFieldValue) -> Unit,
    onDialogDismiss: () -> Unit,
    onLoginButtonClick: suspend () -> Unit,
    scope: CoroutineScope
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
            ssoCode = loginSSOState.ssoCode,
            onCodeChange = onCodeChange,
            error = when (loginSSOState.loginSSOError) {
                LoginSSOError.TextFieldError.InvalidCodeError -> stringResource(R.string.login_error_invalid_sso_code)
                else -> null
            }
        )
        Spacer(modifier = Modifier.weight(1f))
        LoginButton(
            modifier = Modifier.fillMaxWidth(),
            loading = loginSSOState.loading,
            enabled = loginSSOState.loginEnabled
        ) { scope.launch { onLoginButtonClick() } }
    }
    if (loginSSOState.loginSSOError is LoginSSOError.DialogError) {
        val (title, message) = when (loginSSOState.loginSSOError) {
            is LoginSSOError.DialogError.GenericError -> {
                loginSSOState.loginSSOError.coreFailure.dialogErrorStrings(LocalContext.current.resources)
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
        placeholderText = stringResource(R.string.login_sso_code_placeholder),
        labelText = stringResource(R.string.login_sso_code_label),
        state = if (error != null) WireTextFieldState.Error(error) else WireTextFieldState.Default,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        modifier = modifier.testTag("ssoCodeField")
    )
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
                .testTag("ssoLoginButton")
        )
    }
}

@Preview
@Composable
private fun LoginSSOScreenPreview() {
    WireTheme(isPreview = true) {
        LoginSSOContent(rememberScrollState(), LoginSSOState(), {}, {}, suspend {}, rememberCoroutineScope())
    }
}
