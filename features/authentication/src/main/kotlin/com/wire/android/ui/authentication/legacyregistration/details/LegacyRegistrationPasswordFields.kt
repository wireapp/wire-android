/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.legacyregistration.details

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.wireDimensions

@Composable
internal fun <FailureT> PasswordFields(
    state: LegacyRegistrationDetailsState<FailureT>,
    text: LegacyRegistrationDetailsText,
    passwordTextState: TextFieldState,
    confirmPasswordTextState: TextFieldState,
    onKeyboardDismiss: () -> Unit,
) {
    Column {
        WirePasswordTextField(
            textState = passwordTextState,
            placeholderText = text.passwordPlaceholder,
            descriptionText = text.passwordDescription,
            labelMandatoryIcon = true,
            autoFill = false,
            keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Next),
            state = passwordFieldState(state.error, text),
            modifier = Modifier
                .padding(horizontal = MaterialTheme.wireDimensions.spacing16x)
                .testTag("password"),
        )
        WirePasswordTextField(
            textState = confirmPasswordTextState,
            placeholderText = text.confirmPasswordPlaceholder,
            labelText = text.confirmPasswordLabel,
            labelMandatoryIcon = true,
            autoFill = false,
            keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
            onKeyboardAction = { onKeyboardDismiss() },
            state = confirmPasswordFieldState(state.error, text),
            modifier = Modifier
                .padding(MaterialTheme.wireDimensions.spacing16x)
                .testTag("confirmPassword"),
        )
    }
}

private fun passwordFieldState(
    error: LegacyRegistrationDetailsState.DetailsError,
    text: LegacyRegistrationDetailsText,
): WireTextFieldState = if (error is LegacyRegistrationDetailsState.DetailsError.PasswordError.InvalidPasswordError) {
    WireTextFieldState.Error(text.invalidPassword)
} else {
    WireTextFieldState.Default
}

private fun confirmPasswordFieldState(
    error: LegacyRegistrationDetailsState.DetailsError,
    text: LegacyRegistrationDetailsText,
): WireTextFieldState = when (error) {
    LegacyRegistrationDetailsState.DetailsError.PasswordError.InvalidPasswordError ->
        WireTextFieldState.Error(text.invalidPassword)
    LegacyRegistrationDetailsState.DetailsError.PasswordError.PasswordsNotMatchingError ->
        WireTextFieldState.Error(text.passwordsDoNotMatch)
    else -> WireTextFieldState.Default
}
