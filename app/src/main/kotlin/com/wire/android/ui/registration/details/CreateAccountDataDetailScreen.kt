/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.registration.details

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.feature.authentication.R as AuthenticationR
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.legacyregistration.details.LegacyRegistrationDetailsContent
import com.wire.android.ui.authentication.legacyregistration.details.LegacyRegistrationDetailsText
import com.wire.android.util.EMPTY

/** Thin host route: navigation and browser/dialog slots stay in the Android app. */
@Composable
internal fun CreateAccountDataDetailRouteScreen(
    viewModel: CreateAccountDataDetailViewModel,
    onNavigateBack: () -> Unit,
    onCodeRequested: (CreateAccountDataNavArgs) -> Unit,
) {
    val state = viewModel.state
    LaunchedEffect(state.success) {
        if (state.success) {
            viewModel.onCodeSentHandled()
            onCodeRequested(viewModel.createCodeNavArgs())
        }
    }
    LegacyRegistrationDetailsContent(
        state = state,
        emailTextState = viewModel.emailTextState,
        nameTextState = viewModel.nameTextState,
        passwordTextState = viewModel.passwordTextState,
        confirmPasswordTextState = viewModel.confirmPasswordTextState,
        text = LegacyRegistrationDetailsText(
            title = stringResource(AuthenticationR.string.create_personal_account_title),
            emailPlaceholder = stringResource(AuthenticationR.string.create_account_email_placeholder),
            emailLabel = stringResource(AuthenticationR.string.create_account_email_label),
            namePlaceholder = stringResource(AuthenticationR.string.create_account_details_name_placeholder),
            nameLabel = stringResource(AuthenticationR.string.create_account_details_name_label),
            passwordPlaceholder = stringResource(AuthenticationR.string.create_account_details_password_placeholder),
            passwordDescription = stringResource(R.string.create_account_details_password_description),
            confirmPasswordPlaceholder = stringResource(AuthenticationR.string.create_account_details_password_confirm_placeholder),
            confirmPasswordLabel = stringResource(AuthenticationR.string.create_account_details_confirm_password_label),
            invalidPassword = stringResource(AuthenticationR.string.create_account_details_password_error),
            passwordsDoNotMatch = stringResource(AuthenticationR.string.create_account_details_password_not_matching_error),
            continueLabel = stringResource(R.string.label_continue),
        ),
        serverTitle = { LegacyRegistrationServerTitle(viewModel.serverConfig) },
        emailError = { LegacyRegistrationEmailError(state.error) },
        privacyPolicy = {
            LegacyRegistrationPrivacyPolicy(
                serverConfig = viewModel.serverConfig,
                accepted = state.privacyPolicyAccepted,
                onAccepted = viewModel::onPrivacyPolicyAccepted,
            )
        },
        footer = {
            Row {
                LegacyRegistrationTeamBackLink(
                    teamCreationUrl = viewModel.teamCreationUrl() + stringResource(
                        AuthenticationR.string.create_account_email_backlink_to_team_suffix_url,
                    ),
                )
            }
        },
        dialogs = {
            LegacyRegistrationDetailsDialogs(
                state = state,
                tosUrl = viewModel.tosUrl(),
                onTermsDismiss = viewModel::onTermsDialogDismiss,
                onTermsAccept = viewModel::onTermsAccept,
                onErrorDismiss = viewModel::onErrorDismiss,
            )
        },
        onBackPressed = onNavigateBack,
        onContinuePressed = viewModel::onDetailsContinue,
    )
}

internal fun CreateAccountDataDetailViewModel.createCodeNavArgs(): CreateAccountDataNavArgs =
    createAccountNavArgs.copy(
        userRegistrationInfo = createAccountNavArgs.userRegistrationInfo.copy(
            email = emailTextState.text.toString().trim(),
            name = nameTextState.text.toString().trim(),
            password = passwordTextState.text.toString(),
            teamName = String.EMPTY,
        ),
    )
