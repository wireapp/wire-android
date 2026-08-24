/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.legacyregistration.details

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.DefaultEmailDone
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.wireDimensions

@Composable
internal fun <FailureT> LegacyRegistrationDetailsForm(
    state: LegacyRegistrationDetailsState<FailureT>,
    text: LegacyRegistrationDetailsText,
    emailTextState: TextFieldState,
    nameTextState: TextFieldState,
    passwordTextState: TextFieldState,
    confirmPasswordTextState: TextFieldState,
    nameFocusRequester: FocusRequester,
    onKeyboardDismiss: () -> Unit,
    emailError: @Composable () -> Unit,
    privacyPolicy: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        EmailField(
            state = state,
            text = text,
            textState = emailTextState,
            onKeyboardDismiss = onKeyboardDismiss,
            emailError = emailError,
        )
        NameField(
            text = text,
            textState = nameTextState,
            focusRequester = nameFocusRequester,
        )
        PasswordFields(
            state = state,
            text = text,
            passwordTextState = passwordTextState,
            confirmPasswordTextState = confirmPasswordTextState,
            onKeyboardDismiss = onKeyboardDismiss,
        )
        privacyPolicy()
    }
}

@Composable
private fun <FailureT> EmailField(
    state: LegacyRegistrationDetailsState<FailureT>,
    text: LegacyRegistrationDetailsText,
    textState: TextFieldState,
    onKeyboardDismiss: () -> Unit,
    emailError: @Composable () -> Unit,
) {
    WireTextField(
        textState = textState,
        placeholderText = text.emailPlaceholder,
        labelText = text.emailLabel,
        labelMandatoryIcon = true,
        state = if (state.error.isEmailError()) WireTextFieldState.Error() else WireTextFieldState.Default,
        keyboardOptions = KeyboardOptions.DefaultEmailDone,
        onKeyboardAction = { onKeyboardDismiss() },
        modifier = Modifier
            .padding(horizontal = MaterialTheme.wireDimensions.spacing16x)
            .testTag("emailField"),
    )
    AnimatedContent(state.error.isEmailError()) { isEmailError ->
        if (isEmailError) emailError() else VerticalSpace.x16()
    }
}

@Composable
private fun NameField(
    text: LegacyRegistrationDetailsText,
    textState: TextFieldState,
    focusRequester: FocusRequester,
) {
    WireTextField(
        textState = textState,
        placeholderText = text.namePlaceholder,
        labelText = text.nameLabel,
        labelMandatoryIcon = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            autoCorrectEnabled = true,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
        ),
        modifier = Modifier
            .padding(
                start = MaterialTheme.wireDimensions.spacing16x,
                end = MaterialTheme.wireDimensions.spacing16x,
                bottom = MaterialTheme.wireDimensions.spacing16x,
            )
            .focusRequester(focusRequester)
            .testTag("name"),
    )
}
