/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.legacyregistration.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.DefaultEmailDone
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.newauthentication.login.NewAuthContainer
import com.wire.android.ui.newauthentication.login.NewAuthHeader
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

data class LegacyRegistrationDetailsText(
    val title: String,
    val emailPlaceholder: String,
    val emailLabel: String,
    val namePlaceholder: String,
    val nameLabel: String,
    val passwordPlaceholder: String,
    val passwordDescription: String,
    val confirmPasswordPlaceholder: String,
    val confirmPasswordLabel: String,
    val invalidPassword: String,
    val passwordsDoNotMatch: String,
    val continueLabel: String,
)

@Composable
fun <FailureT> LegacyRegistrationDetailsContent(
    state: LegacyRegistrationDetailsState<FailureT>,
    emailTextState: TextFieldState,
    nameTextState: TextFieldState,
    passwordTextState: TextFieldState,
    confirmPasswordTextState: TextFieldState,
    text: LegacyRegistrationDetailsText,
    serverTitle: @Composable () -> Unit,
    emailError: @Composable () -> Unit,
    privacyPolicy: @Composable () -> Unit,
    footer: @Composable () -> Unit,
    dialogs: @Composable () -> Unit,
    onBackPressed: () -> Unit,
    onContinuePressed: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val nameFocus = remember { FocusRequester() }
    NewAuthContainer(
        header = {
            NewAuthHeader(
                title = {
                    Text(text.title, style = MaterialTheme.wireTypography.title01)
                    serverTitle()
                },
                canNavigateBack = true,
                onNavigateBack = onBackPressed,
            )
        },
        contentPadding = dimensions().spacing16x,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WireTextField(
                textState = emailTextState,
                placeholderText = text.emailPlaceholder,
                labelText = text.emailLabel,
                labelMandatoryIcon = true,
                state = if (state.error.isEmailError()) WireTextFieldState.Error() else WireTextFieldState.Default,
                keyboardOptions = KeyboardOptions.DefaultEmailDone,
                onKeyboardAction = { keyboard?.hide() },
                modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing16x).testTag("emailField"),
            )
            emailError()
            WireTextField(
                textState = nameTextState,
                placeholderText = text.namePlaceholder,
                labelText = text.nameLabel,
                labelMandatoryIcon = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    autoCorrectEnabled = true,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)
                    .focusRequester(nameFocus).testTag("name"),
            )
            WirePasswordTextField(
                textState = passwordTextState,
                placeholderText = text.passwordPlaceholder,
                descriptionText = text.passwordDescription,
                labelMandatoryIcon = true,
                autoFill = false,
                keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Next),
                state = if (state.error is LegacyRegistrationDetailsState.DetailsError.PasswordError.InvalidPasswordError) {
                    WireTextFieldState.Error(text.invalidPassword)
                } else WireTextFieldState.Default,
                modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing16x).testTag("password"),
            )
            WirePasswordTextField(
                textState = confirmPasswordTextState,
                placeholderText = text.confirmPasswordPlaceholder,
                labelText = text.confirmPasswordLabel,
                labelMandatoryIcon = true,
                autoFill = false,
                keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
                onKeyboardAction = { keyboard?.hide() },
                state = when (state.error) {
                    LegacyRegistrationDetailsState.DetailsError.PasswordError.InvalidPasswordError ->
                        WireTextFieldState.Error(text.invalidPassword)
                    LegacyRegistrationDetailsState.DetailsError.PasswordError.PasswordsNotMatchingError ->
                        WireTextFieldState.Error(text.passwordsDoNotMatch)
                    else -> WireTextFieldState.Default
                },
                modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x).testTag("confirmPassword"),
            )
            privacyPolicy()
        }
        LaunchedEffect(Unit) { nameFocus.requestFocus(); keyboard?.show() }
        WirePrimaryButton(
            text = text.continueLabel,
            onClick = onContinuePressed,
            loading = state.loading,
            fillMaxWidth = true,
            state = if (state.continueEnabled) WireButtonState.Default else WireButtonState.Disabled,
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.wireDimensions.spacing16x),
        )
        footer()
        dialogs()
    }
}
