/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.authentication.create.email

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.feature.authentication.R as AuthenticationR
import com.wire.android.navigation.routes.auth.CreateAccountDetailsRoute
import com.wire.android.navigation.routes.auth.CreateAccountEmailRoute
import com.wire.android.navigation.routes.auth.CreateAccountRegistrationInfo
import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.create.common.createAccountFlowPolicy
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.SupportPage
import com.wire.android.util.supportUrlResource
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.configuration.server.ServerConfig

@Composable
internal fun CreateAccountEmailRouteScreen(
    route: CreateAccountEmailRoute,
    viewModel: CreateAccountEmailViewModel<CreateAccountRouteFlowType, ServerConfig.Links, CoreFailure>,
    onNavigateBack: () -> Unit,
    onLogin: () -> Unit,
    onDetailsRequested: (CreateAccountDetailsRoute) -> Unit,
) {
    val context = LocalContext.current
    val learnMoreUrl = supportUrlResource(SupportPage.CREATE_ACCOUNT)
    val policy = route.type.createAccountFlowPolicy()
    with(viewModel) {
        val termsUrl = tosUrl()
        CreateAccountEmailContent(
            state = emailState,
            emailTextState = emailTextState,
            text = CreateAccountEmailText(
                title = stringResource(policy.titleResId),
                subtitle = stringResource(policy.emailSubtitleResId),
                emailPlaceholder = stringResource(AuthenticationR.string.create_account_email_placeholder),
                emailLabel = stringResource(AuthenticationR.string.create_account_email_label),
                alreadyInUseError = stringResource(AuthenticationR.string.create_account_email_already_in_use_error),
                blacklistedEmailError = stringResource(AuthenticationR.string.create_account_email_blacklisted_error),
                domainBlockedError = stringResource(AuthenticationR.string.create_account_email_domain_blocked_error),
                invalidEmailError = stringResource(AuthenticationR.string.create_account_email_invalid_error),
                learnMoreLabel = stringResource(R.string.label_learn_more),
                existingAccountPrompt = stringResource(
                    com.wire.android.feature.authentication.R.string.create_account_email_footer_text,
                ),
                loginLabel = stringResource(R.string.label_login),
                continueLabel = stringResource(R.string.label_continue),
            ),
            termsText = CreateAccountEmailTermsText(
                title = stringResource(AuthenticationR.string.create_account_email_terms_dialog_title),
                description = stringResource(AuthenticationR.string.create_account_email_terms_dialog_text),
                cancelLabel = stringResource(R.string.label_cancel),
                continueLabel = stringResource(R.string.label_continue),
                viewPolicyLabel = stringResource(AuthenticationR.string.create_account_email_terms_dialog_view_policy),
            ),
            onBackPressed = onNavigateBack,
            onContinuePressed = ::onEmailContinue,
            onLoginPressed = onLogin,
            onTermsDialogDismiss = ::onTermsDialogDismiss,
            onTermsAccept = ::onTermsAccept,
            onViewPolicyPressed = { CustomTabsHelper.launchUrl(context, termsUrl) },
            onLearnMorePressed = { CustomTabsHelper.launchUrl(context, learnMoreUrl) },
            onErrorDismiss = ::onEmailErrorDismiss,
            subtitleContent = {
                if (serverConfig.isOnPremises) {
                    ServerTitle(
                        serverLinks = serverConfig,
                        style = MaterialTheme.wireTypography.body01,
                    )
                }
            },
            genericFailureContent = { failure, onDismiss -> CoreFailureErrorDialog(failure, onDismiss) },
        )

        LaunchedEffect(emailState.success) {
            if (emailState.success) {
                onDetailsRequested(
                    CreateAccountDetailsRoute(
                        type = route.type,
                        registrationInfo = CreateAccountRegistrationInfo(
                            email = emailTextState.text.trim().toString().lowercase(),
                        ),
                        customServerConfig = route.customServerConfig,
                        flowId = route.flowId,
                    )
                )
            }
        }
    }
}
