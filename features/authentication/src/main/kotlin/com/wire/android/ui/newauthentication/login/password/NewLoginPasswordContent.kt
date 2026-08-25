/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.newauthentication.login.password

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.feature.authentication.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.focusedBorder
import com.wire.android.ui.common.textfield.DefaultEmailNext
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WireAutoFillType
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.typography
import com.wire.android.ui.newauthentication.login.NewAuthContainer

@Immutable
data class NewLoginPasswordPresentation(
    val userIdentifierEnabled: Boolean,
    val loginEnabled: Boolean,
    val loading: Boolean,
    val invalidIdentifier: Boolean,
    val showInvalidCredentialsError: Boolean,
    val proxyAuthenticationRequired: Boolean,
    val showCreateAccount: Boolean,
)

/** Strings owned by the host because they are shared with the legacy login flow. */
@Immutable
data class NewLoginPasswordSharedText(
    val invalidEmail: String,
    val invalidCredentials: String,
    val forgotPassword: String,
    val forgotPasswordContentDescription: String,
    val proxyDescription: String?,
    val invalidProxyIdentifier: String,
    val proxyIdentifierLabel: String,
    val proxyIdentifierPlaceholder: String,
    val proxyPasswordLabel: String,
    val createAccountContentDescription: String,
    val passwordContentDescription: String,
)

/** Feature-owned password form. Browser, navigation, concrete backend and policy all stay in the host. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NewLoginPasswordContent(
    presentation: NewLoginPasswordPresentation,
    sharedText: NewLoginPasswordSharedText,
    userIdentifierTextState: TextFieldState,
    passwordTextState: TextFieldState,
    proxyIdentifierState: TextFieldState,
    proxyPasswordState: TextFieldState,
    onLoginButtonClick: () -> Unit,
    onCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
) {
    NewAuthContainer(
        header = header,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.wrapContentHeight()) {
            Column(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
                PasswordEmailInput(
                    userIdentifierState = userIdentifierTextState,
                    error = presentation.invalidIdentifier.then { sharedText.invalidEmail },
                    isEnabled = presentation.userIdentifierEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = dimensions().spacing8x),
                )
                PasswordInput(
                    passwordState = passwordTextState,
                    state = if (presentation.showInvalidCredentialsError) WireTextFieldState.Error() else WireTextFieldState.Default,
                    contentDescription = sharedText.passwordContentDescription,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = dimensions().spacing8x)
                        .testTag("PasswordInput"),
                )
                if (presentation.showInvalidCredentialsError) {
                    Text(
                        text = sharedText.invalidCredentials,
                        style = typography().body01,
                        color = colorsScheme().error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensions().spacing16x)
                            .testTag("invalidCredentialsError"),
                    )
                }
                if (presentation.proxyAuthenticationRequired) {
                    ForgotPasswordLabel(
                        text = sharedText.forgotPassword,
                        contentDescription = sharedText.forgotPasswordContentDescription,
                        onClick = onForgotPassword,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensions().spacing24x),
                    )
                    ProxyContent(
                        proxyIdentifierState = proxyIdentifierState,
                        proxyPasswordState = proxyPasswordState,
                        invalidIdentifierError = presentation.invalidIdentifier.then { sharedText.invalidProxyIdentifier },
                        proxyDescription = sharedText.proxyDescription,
                        proxyIdentifierLabel = sharedText.proxyIdentifierLabel,
                        proxyIdentifierPlaceholder = sharedText.proxyIdentifierPlaceholder,
                        proxyPasswordLabel = sharedText.proxyPasswordLabel,
                    )
                }
                LoginNextButton(
                    loading = presentation.loading,
                    enabled = presentation.loginEnabled,
                    onClick = onLoginButtonClick,
                )
                if (!presentation.proxyAuthenticationRequired) {
                    ForgotPasswordLabel(
                        text = sharedText.forgotPassword,
                        contentDescription = sharedText.forgotPasswordContentDescription,
                        onClick = onForgotPassword,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensions().spacing24x),
                    )
                }
                if (presentation.showCreateAccount) {
                    CreateAccountContent(
                        onCreateAccountClicked = onCreateAccount,
                        contentDescription = sharedText.createAccountContentDescription,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordEmailInput(
    userIdentifierState: TextFieldState,
    error: String?,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    WireTextField(
        autoFillType = WireAutoFillType.Login,
        textState = userIdentifierState,
        placeholderText = stringResource(R.string.login_email_placeholder),
        labelText = stringResource(R.string.login_email_label),
        state = when {
            !isEnabled -> WireTextFieldState.Disabled
            error != null -> WireTextFieldState.Error(error)
            else -> WireTextFieldState.Default
        },
        semanticDescription = stringResource(R.string.content_description_login_email_field),
        keyboardOptions = KeyboardOptions.DefaultEmailNext,
        modifier = modifier.testTag("emailField"),
        testTag = "userIdentifierInput",
    )
}

@Composable
private fun PasswordInput(
    passwordState: TextFieldState,
    state: WireTextFieldState,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    WirePasswordTextField(
        textState = passwordState,
        state = state,
        keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
        onKeyboardAction = { keyboardController?.hide() },
        semanticDescription = contentDescription,
        modifier = modifier.testTag("passwordField"),
        autoFill = true,
        testTag = "PasswordInput",
    )
}

@Composable
private fun ProxyContent(
    proxyIdentifierState: TextFieldState,
    proxyPasswordState: TextFieldState,
    invalidIdentifierError: String?,
    proxyDescription: String?,
    proxyIdentifierLabel: String,
    proxyIdentifierPlaceholder: String,
    proxyPasswordLabel: String,
) {
    Column {
        proxyDescription?.let {
            Text(
                text = it,
                textAlign = TextAlign.Center,
                style = typography().body01.copy(color = colorsScheme().onBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensions().spacing24x, top = dimensions().spacing8x),
            )
        }
        WireTextField(
            textState = proxyIdentifierState,
            placeholderText = proxyIdentifierPlaceholder,
            labelText = proxyIdentifierLabel,
            state = if (invalidIdentifierError != null) WireTextFieldState.Error(invalidIdentifierError) else WireTextFieldState.Default,
            keyboardOptions = KeyboardOptions.DefaultEmailNext,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimensions().spacing8x)
                .testTag("emailField"),
        )
        val keyboardController = LocalSoftwareKeyboardController.current
        WirePasswordTextField(
            textState = proxyPasswordState,
            labelText = proxyPasswordLabel,
            keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
            onKeyboardAction = { keyboardController?.hide() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimensions().spacing24x)
                .testTag("passwordField"),
            autoFill = false,
        )
    }
}

@Composable
private fun LoginNextButton(loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensions().spacing8x)
            .testTag("LoginNextButton"),
    ) {
        WirePrimaryButton(
            text = stringResource(R.string.enterprise_login_next),
            onClick = onClick,
            state = if (enabled && !loading) WireButtonState.Default else WireButtonState.Disabled,
            loading = loading,
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth().testTag("loginButton"),
        )
    }
}

@Composable
private fun ForgotPasswordLabel(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused = interactionSource.collectIsFocusedAsState()
        Text(
            text = text,
            style = typography().body02.copy(textDecoration = TextDecoration.Underline, color = colorsScheme().onSurface),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .focusedBorder(isFocused.value)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                    onClickLabel = contentDescription,
                )
                .testTag("Forgot password?"),
        )
    }
}

@Composable
private fun CreateAccountContent(
    onCreateAccountClicked: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(dimensions().buttonCornerSize),
        border = BorderStroke(dimensions().spacing1x, colorsScheme().outline),
        color = colorsScheme().surfaceContainerLow,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(dimensions().spacing8x),
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused = interactionSource.collectIsFocusedAsState()
            Text(
                text = stringResource(R.string.enterprise_login_create_account_label),
                style = typography().body01,
                color = colorsScheme().onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.enterprise_login_create_account_text_button),
                style = typography().body02.copy(textDecoration = TextDecoration.Underline, color = colorsScheme().onSurface),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .focusedBorder(isFocused.value)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onCreateAccountClicked,
                        onClickLabel = contentDescription,
                    )
                    .testTag("Create account"),
            )
        }
    }
}

private inline fun Boolean.then(value: () -> String): String? = if (this) value() else null
