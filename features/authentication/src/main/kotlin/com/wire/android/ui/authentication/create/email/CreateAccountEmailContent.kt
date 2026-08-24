/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.authentication.create.email

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.DefaultEmailDone
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Immutable
data class CreateAccountEmailText(
    val title: String,
    val subtitle: String,
    val emailPlaceholder: String,
    val emailLabel: String,
    val alreadyInUseError: String,
    val blacklistedEmailError: String,
    val domainBlockedError: String,
    val invalidEmailError: String,
    val learnMoreLabel: String,
    val existingAccountPrompt: String,
    val loginLabel: String,
    val continueLabel: String,
)

@Immutable
data class CreateAccountEmailTermsText(
    val title: String,
    val description: String,
    val cancelLabel: String,
    val continueLabel: String,
    val viewPolicyLabel: String,
)

@Suppress("LongParameterList")
@Composable
fun <FlowT, FailureT> CreateAccountEmailContent(
    state: CreateAccountEmailViewState<FlowT, FailureT>,
    emailTextState: TextFieldState,
    text: CreateAccountEmailText,
    termsText: CreateAccountEmailTermsText,
    onBackPressed: () -> Unit,
    onContinuePressed: () -> Unit,
    onLoginPressed: () -> Unit,
    onTermsDialogDismiss: () -> Unit,
    onTermsAccept: () -> Unit,
    onViewPolicyPressed: () -> Unit,
    onLearnMorePressed: () -> Unit,
    onErrorDismiss: () -> Unit,
    subtitleContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    genericFailureContent: @Composable (FailureT, () -> Unit) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                title = text.title,
                onNavigationPressed = onBackPressed,
                subtitleContent = subtitleContent,
            )
        },
    ) { internalPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.padding(internalPadding),
        ) {
            val keyboardController = LocalSoftwareKeyboardController.current
            Text(
                text = text.subtitle,
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.wireDimensions.spacing16x,
                        vertical = MaterialTheme.wireDimensions.spacing24x,
                    )
                    .testTag("createTeamText"),
            )
            WireTextField(
                textState = emailTextState,
                placeholderText = text.emailPlaceholder,
                labelText = text.emailLabel,
                state = if (state.error is CreateAccountEmailViewState.EmailError.None) {
                    WireTextFieldState.Default
                } else {
                    WireTextFieldState.Error()
                },
                keyboardOptions = KeyboardOptions.DefaultEmailDone,
                onKeyboardAction = { keyboardController?.hide() },
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.wireDimensions.spacing16x)
                    .testTag("emailField")
                    .focusRequester(focusRequester),
            )
            AnimatedVisibility(visible = state.error !is CreateAccountEmailViewState.EmailError.None) {
                EmailErrorText(state.error, text, onLearnMorePressed)
            }
            Spacer(modifier = Modifier.weight(1f))
            EmailFooter(
                state = state,
                text = text,
                onLoginPressed = onLoginPressed,
                onContinuePressed = onContinuePressed,
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }
    if (state.termsDialogVisible) {
        TermsConditionsDialog(
            text = termsText,
            onDialogDismiss = onTermsDialogDismiss,
            onContinuePressed = onTermsAccept,
            onViewPolicyPressed = onViewPolicyPressed,
        )
    }
    val dialogError = state.error as? CreateAccountEmailViewState.EmailError.DialogError.GenericError<FailureT>
    if (dialogError != null) genericFailureContent(dialogError.coreFailure, onErrorDismiss)
}

@Composable
private fun <FailureT> EmailErrorText(
    error: CreateAccountEmailViewState.EmailError<FailureT>,
    text: CreateAccountEmailText,
    onLearnMorePressed: () -> Unit,
) {
    val learnMoreTag = "learn_more"
    val annotatedText = buildAnnotatedString {
        append(
            if (error is CreateAccountEmailViewState.EmailError.TextFieldError) {
                when (error) {
                    CreateAccountEmailViewState.EmailError.TextFieldError.AlreadyInUseError -> text.alreadyInUseError
                    CreateAccountEmailViewState.EmailError.TextFieldError.BlacklistedEmailError -> text.blacklistedEmailError
                    CreateAccountEmailViewState.EmailError.TextFieldError.DomainBlockedError -> text.domainBlockedError
                    CreateAccountEmailViewState.EmailError.TextFieldError.InvalidEmailError -> text.invalidEmailError
                }
            } else {
                ""
            }
        )
        if (error is CreateAccountEmailViewState.EmailError.TextFieldError.AlreadyInUseError) {
            append(" ")
            pushStringAnnotation(tag = learnMoreTag, annotation = learnMoreTag)
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.wireColorScheme.onBackground,
                    fontWeight = MaterialTheme.wireTypography.label05.fontWeight,
                    fontSize = MaterialTheme.wireTypography.label05.fontSize,
                    textDecoration = TextDecoration.Underline,
                )
            ) { append(text.learnMoreLabel) }
            pop()
        }
    }
    ClickableText(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = MaterialTheme.wireDimensions.spacing8x,
                horizontal = MaterialTheme.wireDimensions.spacing16x,
            ),
        style = MaterialTheme.wireTypography.label04.copy(
            color = MaterialTheme.wireColorScheme.error,
            textAlign = TextAlign.Start,
        ),
        text = annotatedText,
        onClick = { offset ->
            if (annotatedText.getStringAnnotations(learnMoreTag, offset, offset).isNotEmpty()) onLearnMorePressed()
        },
    )
}

@Composable
private fun <FlowT, FailureT> EmailFooter(
    state: CreateAccountEmailViewState<FlowT, FailureT>,
    text: CreateAccountEmailText,
    onLoginPressed: () -> Unit,
    onContinuePressed: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing16x),
        ) {
            Text(
                text = "${text.existingAccountPrompt} ",
                style = MaterialTheme.wireTypography.body02,
                textAlign = TextAlign.Center,
            )
            Text(
                text = text.loginLabel,
                style = MaterialTheme.wireTypography.body02.copy(textDecoration = TextDecoration.Underline),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onLoginPressed,
                ),
            )
        }
        WirePrimaryButton(
            text = text.continueLabel,
            onClick = onContinuePressed,
            fillMaxWidth = true,
            loading = state.loading,
            state = if (state.continueEnabled) WireButtonState.Default else WireButtonState.Disabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.wireDimensions.spacing16x),
        )
    }
}

@Composable
private fun TermsConditionsDialog(
    text: CreateAccountEmailTermsText,
    onDialogDismiss: () -> Unit,
    onContinuePressed: () -> Unit,
    onViewPolicyPressed: () -> Unit,
) {
    WireDialog(
        title = text.title,
        text = text.description,
        onDismiss = onDialogDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onContinuePressed,
            text = text.continueLabel,
            type = WireDialogButtonType.Primary,
        ),
    ) {
        Column {
            WireSecondaryButton(
                text = text.cancelLabel,
                onClick = onDialogDismiss,
                fillMaxWidth = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.wireDimensions.spacing8x)
                    .testTag("cancelButton"),
            )
            WireSecondaryButton(
                text = text.viewPolicyLabel,
                onClick = onViewPolicyPressed,
                fillMaxWidth = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("viewTC"),
            )
        }
    }
}

@MultipleThemePreviews
@Composable
fun PreviewCreateAccountEmailScreen() = WireTheme {
    CreateAccountEmailContent(
        emailTextState = TextFieldState(),
        state = CreateAccountEmailViewState<Unit, Unit>(Unit),
        text = CreateAccountEmailText(
            title = "Create account",
            subtitle = "Enter your email",
            emailPlaceholder = "Email",
            emailLabel = "EMAIL",
            alreadyInUseError = "Already in use",
            blacklistedEmailError = "Blacklisted",
            domainBlockedError = "Domain blocked",
            invalidEmailError = "Invalid email",
            learnMoreLabel = "Learn more",
            existingAccountPrompt = "Already have an account?",
            loginLabel = "Log in",
            continueLabel = "Continue",
        ),
        termsText = CreateAccountEmailTermsText(
            title = "Terms and conditions",
            description = "Accept the terms to continue.",
            cancelLabel = "Cancel",
            continueLabel = "Continue",
            viewPolicyLabel = "View policy",
        ),
        onBackPressed = {},
        onContinuePressed = {},
        onLoginPressed = {},
        onTermsDialogDismiss = {},
        onTermsAccept = {},
        onViewPolicyPressed = {},
        onLearnMorePressed = {},
        onErrorDismiss = {},
        subtitleContent = {},
        genericFailureContent = { _, _ -> },
    )
}
