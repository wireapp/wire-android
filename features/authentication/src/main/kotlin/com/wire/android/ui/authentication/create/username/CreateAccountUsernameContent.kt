/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.authentication.create.username

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.wire.android.ui.common.animation.ShakeAnimation
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.textfield.DefaultEmailDone
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.textfield.forceLowercase
import com.wire.android.ui.common.textfield.maxLengthWithCallback
import com.wire.android.ui.common.textfield.patternWithCallback
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import java.util.regex.Pattern

typealias CreateAccountUsernameLayout = @Composable (
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
) -> Unit

@Immutable
data class CreateAccountUsernameText(
    val title: String,
    val description: String,
    val usernamePlaceholder: String,
    val usernameLabel: String,
    val usernameDescription: String,
    val usernameTakenError: String,
    val mentionContentDescription: String,
    val confirmLabel: String,
)

@Suppress("LongParameterList")
@Composable
fun <FailureT> CreateAccountUsernameContent(
    textState: TextFieldState,
    state: CreateAccountUsernameViewState<FailureT>,
    text: CreateAccountUsernameText,
    @DrawableRes mentionIconResId: Int,
    onContinuePressed: () -> Unit,
    onErrorDismiss: () -> Unit,
    layout: CreateAccountUsernameLayout,
    modifier: Modifier = Modifier,
    genericFailureContent: @Composable (FailureT, () -> Unit) -> Unit,
) {
    layout(
        {
            Text(
                text = text.title,
                style = MaterialTheme.wireTypography.title01,
                modifier = Modifier.semantics { heading() },
            )
        },
        {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = modifier,
            ) {
                Text(
                    text = text.description,
                    style = MaterialTheme.wireTypography.body01,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaterialTheme.wireDimensions.spacing16x,
                            vertical = MaterialTheme.wireDimensions.spacing24x,
                        ),
                )

                UsernameField(
                    textState = textState,
                    error = state.error,
                    text = text,
                    mentionIconResId = mentionIconResId,
                    onErrorDismiss = onErrorDismiss,
                    genericFailureContent = genericFailureContent,
                )

                Spacer(modifier = Modifier.weight(1f))
                WirePrimaryButton(
                    text = text.confirmLabel,
                    onClick = onContinuePressed,
                    fillMaxWidth = true,
                    loading = state.loading,
                    state = if (state.continueEnabled) WireButtonState.Default else WireButtonState.Disabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.wireDimensions.spacing16x),
                )
            }
        },
    )
}

@Composable
private fun <FailureT> UsernameField(
    textState: TextFieldState,
    error: CreateAccountUsernameError<FailureT>,
    text: CreateAccountUsernameText,
    @DrawableRes mentionIconResId: Int,
    onErrorDismiss: () -> Unit,
    genericFailureContent: @Composable (FailureT, () -> Unit) -> Unit,
) {
    if (error is CreateAccountUsernameError.Generic) {
        genericFailureContent(error.failure, onErrorDismiss)
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    ShakeAnimation { animate ->
        WireTextField(
            textState = textState,
            placeholderText = text.usernamePlaceholder,
            labelText = text.usernameLabel,
            inputTransformation = InputTransformation
                .forceLowercase()
                .patternWithCallback(USERNAME_PATTERN, animate)
                .maxLengthWithCallback(MAX_USERNAME_LENGTH, animate),
            leadingIcon = {
                Icon(
                    painter = painterResource(mentionIconResId),
                    contentDescription = text.mentionContentDescription,
                    modifier = Modifier.padding(
                        start = MaterialTheme.wireDimensions.spacing16x,
                        end = MaterialTheme.wireDimensions.spacing8x,
                    ),
                )
            },
            state = when (error) {
                CreateAccountUsernameError.UsernameTaken -> WireTextFieldState.Error(text.usernameTakenError)
                CreateAccountUsernameError.UsernameInvalid -> WireTextFieldState.Error(text.usernameDescription)
                else -> WireTextFieldState.Default
            },
            descriptionText = text.usernameDescription,
            keyboardOptions = KeyboardOptions.DefaultEmailDone,
            onKeyboardAction = { keyboardController?.hide() },
            modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing16x),
        )
    }
}

@MultipleThemePreviews
@Composable
private fun PreviewCreateAccountUsernameScreen() = WireTheme {
    CreateAccountUsernameContent(
        textState = TextFieldState(),
        state = CreateAccountUsernameViewState<Unit>(),
        text = CreateAccountUsernameText(
            title = "Set username",
            description = "Enter your username.",
            usernamePlaceholder = "Username",
            usernameLabel = "USERNAME",
            usernameDescription = "Use letters, numbers, dots, dashes, and underscores.",
            usernameTakenError = "Username already taken",
            mentionContentDescription = "Mention",
            confirmLabel = "Confirm",
        ),
        mentionIconResId = android.R.drawable.ic_menu_edit,
        onContinuePressed = {},
        onErrorDismiss = {},
        layout = { title, content ->
            Column {
                title()
                content()
            }
        },
        genericFailureContent = { _, _ -> },
    )
}

private const val MAX_USERNAME_LENGTH = 255
private val USERNAME_PATTERN: Pattern = Pattern.compile("^[a-z0-9._-]*$")
