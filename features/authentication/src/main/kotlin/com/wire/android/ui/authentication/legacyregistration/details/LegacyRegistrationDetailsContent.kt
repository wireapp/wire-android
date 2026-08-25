/* Wire Copyright (C) 2026 Wire Swiss GmbH */
@file:Suppress("MatchingDeclarationName")

package com.wire.android.ui.authentication.legacyregistration.details

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Text
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.newauthentication.login.NewAuthContainer
import com.wire.android.ui.newauthentication.login.NewAuthHeader
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
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val nameFocusRequester = remember { FocusRequester() }
    NewAuthContainer(
        modifier = modifier,
        header = {
            NewAuthHeader(
                title = {
                    Text(
                        text = text.title,
                        style = MaterialTheme.wireTypography.title01,
                        modifier = Modifier.semantics { heading() },
                    )
                    serverTitle()
                },
                canNavigateBack = true,
                onNavigateBack = onBackPressed,
            )
        },
        contentPadding = dimensions().spacing16x,
    ) {
        LegacyRegistrationDetailsForm(
            state = state,
            text = text,
            emailTextState = emailTextState,
            nameTextState = nameTextState,
            passwordTextState = passwordTextState,
            confirmPasswordTextState = confirmPasswordTextState,
            nameFocusRequester = nameFocusRequester,
            onKeyboardDismiss = { keyboard?.hide() },
            emailError = emailError,
            privacyPolicy = privacyPolicy,
        )
        LaunchedEffect(Unit) {
            nameFocusRequester.requestFocus()
            keyboard?.show()
        }
        LegacyRegistrationDetailsActions(
            state = state,
            continueLabel = text.continueLabel,
            onContinuePressed = onContinuePressed,
            footer = footer,
            dialogs = dialogs,
        )
    }
}
