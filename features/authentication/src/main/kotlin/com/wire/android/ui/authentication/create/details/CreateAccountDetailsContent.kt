/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.wire.android.feature.authentication.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.rememberBottomBarElevationState
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Immutable
data class CreateAccountDetailsSharedText(
    val passwordDescription: String,
    val confirmPasswordLabel: String,
    val invalidPasswordError: String,
    val passwordsNotMatchingError: String,
    val continueLabel: String,
)

@Suppress("LongParameterList")
@Composable
fun <FailureT> CreateAccountDetailsContent(
    state: CreateAccountDetailsViewState<FailureT>,
    title: String,
    showTeamName: Boolean,
    sharedText: CreateAccountDetailsSharedText,
    firstNameTextState: TextFieldState,
    lastNameTextState: TextFieldState,
    passwordTextState: TextFieldState,
    confirmPasswordTextState: TextFieldState,
    teamNameTextState: TextFieldState,
    onBackPressed: () -> Unit,
    onContinuePressed: () -> Unit,
    onErrorDismiss: () -> Unit,
    subtitleContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    genericFailureContent: @Composable (FailureT, () -> Unit) -> Unit,
) {
    val scrollState = rememberScrollState()
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.rememberTopBarElevationState().value,
                title = title,
                onNavigationPressed = onBackPressed,
                subtitleContent = subtitleContent,
            )
        },
    ) { internalPadding ->
        Column(
            modifier = Modifier
                .padding(internalPadding)
                .fillMaxHeight()
        ) {
            val keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                autoCorrectEnabled = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            )
            val keyboardController = LocalSoftwareKeyboardController.current
            val firstNameFocusRequester = remember { FocusRequester() }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.create_personal_account_details_text),
                    style = MaterialTheme.wireTypography.body01,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaterialTheme.wireDimensions.spacing16x,
                            vertical = MaterialTheme.wireDimensions.spacing24x
                        )
                )

                WireTextField(
                    textState = firstNameTextState,
                    placeholderText = stringResource(R.string.create_account_details_first_name_placeholder),
                    labelText = stringResource(R.string.create_account_details_first_name_label),
                    labelMandatoryIcon = true,
                    state = WireTextFieldState.Default,
                    keyboardOptions = keyboardOptions,
                    modifier = Modifier
                        .padding(
                            start = MaterialTheme.wireDimensions.spacing16x,
                            end = MaterialTheme.wireDimensions.spacing16x,
                            bottom = MaterialTheme.wireDimensions.spacing16x
                        )
                        .focusRequester(firstNameFocusRequester)
                        .testTag("firstName"),
                )

                WireTextField(
                    textState = lastNameTextState,
                    placeholderText = stringResource(R.string.create_account_details_last_name_placeholder),
                    labelText = stringResource(R.string.create_account_details_last_name_label),
                    labelMandatoryIcon = true,
                    state = WireTextFieldState.Default,
                    keyboardOptions = keyboardOptions,
                    modifier = Modifier
                        .padding(
                            start = MaterialTheme.wireDimensions.spacing16x,
                            end = MaterialTheme.wireDimensions.spacing16x,
                            bottom = MaterialTheme.wireDimensions.spacing16x
                        )
                        .testTag("lastName"),
                )

                if (showTeamName) {
                    WireTextField(
                        textState = teamNameTextState,
                        placeholderText = stringResource(R.string.create_account_details_team_name_placeholder),
                        labelText = stringResource(R.string.create_account_details_team_name_label),
                        labelMandatoryIcon = true,
                        state = WireTextFieldState.Default,
                        keyboardOptions = keyboardOptions,
                        modifier = Modifier
                            .padding(
                                start = MaterialTheme.wireDimensions.spacing16x,
                                end = MaterialTheme.wireDimensions.spacing16x,
                                bottom = MaterialTheme.wireDimensions.spacing16x
                            )
                            .testTag("teamName"),
                    )
                }

                WirePasswordTextField(
                    textState = passwordTextState,
                    labelMandatoryIcon = true,
                    descriptionText = sharedText.passwordDescription,
                    keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.wireDimensions.spacing16x)
                        .testTag("password"),
                    state = if (state.error is CreateAccountDetailsViewState.DetailsError.TextFieldError.InvalidPasswordError) {
                        WireTextFieldState.Error()
                    } else {
                        WireTextFieldState.Default
                    },
                    autoFill = false,
                )

                WirePasswordTextField(
                    textState = confirmPasswordTextState,
                    labelText = sharedText.confirmPasswordLabel,
                    labelMandatoryIcon = true,
                    keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
                    onKeyboardAction = { keyboardController?.hide() },
                    modifier = Modifier
                        .padding(
                            horizontal = MaterialTheme.wireDimensions.spacing16x,
                            vertical = MaterialTheme.wireDimensions.spacing16x
                        )
                        .testTag("confirmPassword"),
                    state = if (state.error is CreateAccountDetailsViewState.DetailsError.TextFieldError) {
                        when (state.error) {
                            CreateAccountDetailsViewState.DetailsError.TextFieldError.PasswordsNotMatchingError ->
                                WireTextFieldState.Error(sharedText.passwordsNotMatchingError)

                            CreateAccountDetailsViewState.DetailsError.TextFieldError.InvalidPasswordError ->
                                WireTextFieldState.Error(sharedText.invalidPasswordError)

                            else -> WireTextFieldState.Default
                        }
                    } else {
                        WireTextFieldState.Default
                    },
                    autoFill = false,
                )
            }

            LaunchedEffect(Unit) {
                firstNameFocusRequester.requestFocus()
                keyboardController?.show()
            }

            Surface(
                shadowElevation = scrollState.rememberBottomBarElevationState().value,
                color = MaterialTheme.wireColorScheme.background
            ) {
                WirePrimaryButton(
                    modifier = Modifier
                        .padding(MaterialTheme.wireDimensions.spacing16x)
                        .fillMaxWidth(),
                    text = sharedText.continueLabel,
                    onClick = onContinuePressed,
                    fillMaxWidth = true,
                    loading = state.loading,
                    state = if (state.continueEnabled) WireButtonState.Default else WireButtonState.Disabled,
                )
            }
        }
    }
    val dialogError = state.error as? CreateAccountDetailsViewState.DetailsError.DialogError.GenericError<FailureT>
    if (dialogError != null) genericFailureContent(dialogError.coreFailure, onErrorDismiss)
}

@MultipleThemePreviews
@Composable
fun PreviewCreateAccountDetailsScreen() = WireTheme {
    CreateAccountDetailsContent(
        state = CreateAccountDetailsViewState<Unit>(),
        title = "Create account",
        showTeamName = true,
        sharedText = CreateAccountDetailsSharedText(
            passwordDescription = "Password requirements",
            confirmPasswordLabel = "CONFIRM PASSWORD",
            invalidPasswordError = "Invalid password",
            passwordsNotMatchingError = "Passwords do not match",
            continueLabel = "Continue",
        ),
        firstNameTextState = TextFieldState(),
        lastNameTextState = TextFieldState(),
        passwordTextState = TextFieldState(),
        confirmPasswordTextState = TextFieldState(),
        teamNameTextState = TextFieldState(),
        onBackPressed = {},
        onContinuePressed = {},
        onErrorDismiss = {},
        subtitleContent = {},
        genericFailureContent = { _, _ -> },
    )
}
