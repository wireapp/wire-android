/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication.login.email

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import com.wire.android.feature.authentication.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.focusedBorder
import com.wire.android.ui.common.textfield.DefaultEmailNext
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WireAutoFillType
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Immutable
data class LoginEmailPresentationState(
    val loading: Boolean = false,
    val loginEnabled: Boolean = false,
    val userIdentifierEnabled: Boolean = true,
    val invalidUserIdentifier: Boolean = false,
    val invalidProxyIdentifier: Boolean = false,
    val showInvalidCredentialsError: Boolean = false,
    val proxyAuthRequired: Boolean = false,
    val apiProxyUrl: String? = null,
)

@Immutable
data class LoginEmailText(
    val wireCredentials: String,
    val userIdentifierLabel: String,
    val userIdentifierDescription: String,
    val invalidUserIdentifier: String,
    val passwordDescription: String,
    val invalidCredentials: String,
    val forgotPassword: String,
    val openLinkDescription: String,
    val proxyCredentials: String,
    val proxyDescription: (String) -> String,
    val login: String,
    val loggingIn: String,
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoginEmailContent(
    state: LoginEmailPresentationState,
    text: LoginEmailText,
    userIdentifierTextState: TextFieldState,
    passwordTextState: TextFieldState,
    proxyIdentifierTextState: TextFieldState,
    proxyPasswordTextState: TextFieldState,
    onLoginButtonClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    fillMaxHeight: Boolean = true,
) {
    Column(modifier = modifier.let { if (fillMaxHeight) it.fillMaxHeight() else it.wrapContentHeight() }) {
        Column(
            modifier = Modifier
                .let { if (fillMaxHeight) it.weight(1f, true) else it }
                .verticalScroll(scrollState)
                .padding(MaterialTheme.wireDimensions.spacing16x)
                .semantics { testTagsAsResourceId = true },
        ) {
            if (state.proxyAuthRequired) {
                Text(
                    text = text.wireCredentials,
                    style = MaterialTheme.wireTypography.title03.copy(color = colorsScheme().secondaryText),
                    modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.wireDimensions.spacing16x).semantics { heading() },
                )
            }
            val userFieldState = when {
                !state.userIdentifierEnabled -> WireTextFieldState.Disabled
                state.invalidUserIdentifier -> WireTextFieldState.Error(text.invalidUserIdentifier)
                state.showInvalidCredentialsError -> WireTextFieldState.Error()
                else -> WireTextFieldState.Default
            }
            WireTextField(
                autoFillType = WireAutoFillType.Login,
                textState = userIdentifierTextState,
                placeholderText = stringResource(R.string.login_user_identifier_placeholder),
                labelText = text.userIdentifierLabel,
                state = userFieldState,
                semanticDescription = text.userIdentifierDescription,
                keyboardOptions = KeyboardOptions.DefaultEmailNext,
                modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.wireDimensions.spacing16x).testTag("emailField"),
                testTag = "userIdentifierInput",
            )
            val keyboardController = LocalSoftwareKeyboardController.current
            WirePasswordTextField(
                textState = passwordTextState,
                state = if (state.showInvalidCredentialsError) WireTextFieldState.Error() else WireTextFieldState.Default,
                keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
                onKeyboardAction = { keyboardController?.hide() },
                semanticDescription = text.passwordDescription,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.wireDimensions.spacing16x)
                    .testTag("passwordField"),
                autoFill = true,
                testTag = "PasswordInput",
            )
            if (state.showInvalidCredentialsError) Text(
                text = text.invalidCredentials,
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.wireDimensions.spacing16x)
                    .testTag("invalidCredentialsError"),
            )
            ForgotPasswordLink(
                label = text.forgotPassword,
                openLinkDescription = text.openLinkDescription,
                onClick = onForgotPasswordClick,
                modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.wireDimensions.spacing16x),
            )
            if (state.proxyAuthRequired) ProxyScreen(
                proxyIdentifierState = proxyIdentifierTextState,
                proxyPasswordState = proxyPasswordTextState,
                text = text, invalidIdentifier = state.invalidProxyIdentifier,
                apiProxyUrl = state.apiProxyUrl,
            )
            Spacer(Modifier.weight(1f))
        }
        Surface(color = MaterialTheme.wireColorScheme.surface, modifier = Modifier.semantics { testTagsAsResourceId = true }) {
            Box(Modifier.padding(MaterialTheme.wireDimensions.spacing16x)) {
                LoginButtonContent(
                    loading = state.loading,
                    enabled = state.loginEnabled,
                    onClick = onLoginButtonClick,
                    modifier = Modifier.fillMaxWidth(),
                    text = text.login,
                    loadingText = text.loggingIn,
                )
            }
        }
    }
}

@Composable
fun ForgotPasswordLink(
    label: String,
    openLinkDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = colorsScheme().primary,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        val interactionSource = remember { MutableInteractionSource() }
        val focused = interactionSource.collectIsFocusedAsState()
        Text(
            text = label,
            style = MaterialTheme.wireTypography.body02.copy(textDecoration = TextDecoration.Underline, color = textColor),
            textAlign = TextAlign.Center,
            modifier = Modifier.focusedBorder(focused.value).clickable(
                interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick,
                onClickLabel = openLinkDescription,
            ).testTag("Forgot password?"),
        )
    }
}

@Composable
fun ProxyScreen(
    text: LoginEmailText,
    proxyIdentifierState: TextFieldState,
    proxyPasswordState: TextFieldState,
    invalidIdentifier: Boolean,
    apiProxyUrl: String?,
    modifier: Modifier = Modifier,
) = Column(modifier) {
    androidx.compose.material3.HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.wireColorScheme.divider)
    Text(
        text = text.proxyCredentials,
        style = MaterialTheme.wireTypography.title03.copy(color = colorsScheme().secondaryText),
        modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.wireDimensions.spacing16x).semantics { heading() },
    )
    apiProxyUrl?.let {
        Text(
            text = text.proxyDescription(it),
            style = MaterialTheme.wireTypography.body01.copy(color = colorsScheme().onBackground),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.wireDimensions.spacing16x),
        )
    }
    WireTextField(
        textState = proxyIdentifierState, placeholderText = stringResource(R.string.login_user_identifier_placeholder),
        labelText = stringResource(R.string.login_proxy_identifier_label),
        state = if (invalidIdentifier) WireTextFieldState.Error(text.invalidUserIdentifier) else WireTextFieldState.Default,
        keyboardOptions = KeyboardOptions.DefaultEmailNext,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = MaterialTheme.wireDimensions.spacing16x)
            .testTag("emailField"),
    )
    val keyboardController = LocalSoftwareKeyboardController.current
    WirePasswordTextField(
        textState = proxyPasswordState,
        labelText = stringResource(R.string.label_proxy_password),
        keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
        onKeyboardAction = { keyboardController?.hide() },
        modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.wireDimensions.spacing16x).testTag("passwordField"),
        autoFill = false,
    )
    Spacer(Modifier.weight(1f))
}

@Composable
fun ProxyIdentifierInput(proxyIdentifierState: TextFieldState, error: String?, modifier: Modifier = Modifier) {
    WireTextField(
        textState = proxyIdentifierState,
        placeholderText = stringResource(R.string.login_user_identifier_placeholder),
        labelText = stringResource(R.string.login_proxy_identifier_label),
        state = error?.let(WireTextFieldState::Error) ?: WireTextFieldState.Default,
        keyboardOptions = KeyboardOptions.DefaultEmailNext,
        modifier = modifier.testTag("emailField"),
    )
}

@Composable
fun ProxyPasswordInput(proxyPasswordState: TextFieldState, modifier: Modifier = Modifier) {
    val keyboardController = LocalSoftwareKeyboardController.current
    WirePasswordTextField(
        textState = proxyPasswordState,
        labelText = stringResource(R.string.label_proxy_password),
        keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
        onKeyboardAction = { keyboardController?.hide() },
        modifier = modifier.testTag("passwordField"),
        autoFill = false,
    )
}

@Composable
fun LoginButtonContent(
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    loadingText: String,
) {
    Column(modifier = modifier) {
        WirePrimaryButton(
            text = if (loading) loadingText else text,
            onClick = onClick,
            state = if (enabled && !loading) WireButtonState.Default else WireButtonState.Disabled,
            loading = loading,
            modifier = Modifier.testTag("loginButton"),
        )
    }
}
