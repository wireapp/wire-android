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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.BuildConfig
import com.wire.android.BuildConfig.ENABLE_NEW_REGISTRATION
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.feature.authentication.R as AuthenticationR
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.PreFilledUserIdentifierType
import com.wire.android.ui.authentication.login.email.AppLoginEmailViewModel
import com.wire.android.ui.authentication.login.isProxyAuthRequired
import com.wire.android.ui.authentication.welcome.isProxyEnabled
import com.wire.android.ui.common.R as CommonR
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.clearAutofillTree
import com.wire.android.ui.common.typography
import com.wire.android.ui.newauthentication.login.NewAuthHeader
import com.wire.android.ui.newauthentication.login.NewAuthSubtitle
import com.wire.android.util.CustomTabsHelper
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.user.UserId

internal sealed interface NewLoginPasswordScreenAction {
    data class Success(val initialSyncCompleted: Boolean, val isE2EIRequired: Boolean, val userId: UserId) : NewLoginPasswordScreenAction
    data class RemoveDevice(val userId: UserId) : NewLoginPasswordScreenAction
    data object Canceled : NewLoginPasswordScreenAction
    data class VerificationRequired(val navArgs: LoginNavArgs) : NewLoginPasswordScreenAction
    data class CreateAccountSelector(val serverConfig: ServerConfig.Links, val email: String) : NewLoginPasswordScreenAction
    data class CreatePersonalAccount(val serverConfig: ServerConfig.Links) : NewLoginPasswordScreenAction
}

/** App-owned route adapter: it contains all navigation, dialogs, policies and concrete Kalium aliases. */
@Composable
internal fun NewLoginPasswordRouteScreen(
    navArgs: LoginNavArgs,
    loginEmailViewModel: AppLoginEmailViewModel,
    canNavigateBack: Boolean,
    onAction: (NewLoginPasswordScreenAction) -> Boolean,
) {
    clearAutofillTree()
    LoginStateNavigationAndDialogs(loginEmailViewModel, onAction)
    LaunchedEffect(loginEmailViewModel.secondFactorVerificationCodeState) {
        if (loginEmailViewModel.secondFactorVerificationCodeState.isCodeInputNecessary) {
            onAction(
                NewLoginPasswordScreenAction.VerificationRequired(
                    LoginNavArgs(
                        loginPasswordPath = navArgs.loginPasswordPath,
                        userHandle = PreFilledUserIdentifierType.PreFilled(loginEmailViewModel.userIdentifierTextState.text.toString()),
                    ),
                ),
            )
        }
    }

    val serverConfig = loginEmailViewModel.serverConfig
    val state = loginEmailViewModel.loginState
    val context = LocalContext.current
    NewLoginPasswordContent(
        presentation = NewLoginPasswordPresentation(
            userIdentifierEnabled = state.userIdentifierEnabled,
            loginEnabled = state.loginEnabled,
            loading = state.flowState is LoginState.Loading,
            invalidIdentifier = state.flowState is LoginState.Error.TextFieldError.InvalidValue,
            showInvalidCredentialsError = state.showInvalidCredentialsError,
            proxyAuthenticationRequired = serverConfig.isProxyAuthRequired,
            showCreateAccount = BuildConfig.ALLOW_ACCOUNT_CREATION &&
                !serverConfig.isProxyEnabled() &&
                (navArgs.loginPasswordPath?.isCloudAccountCreationPossible ?: true),
        ),
        sharedText = NewLoginPasswordSharedText(
            invalidEmail = stringResource(AuthenticationR.string.login_error_invalid_email),
            invalidCredentials = stringResource(AuthenticationR.string.login_error_invalid_credentials_message),
            forgotPassword = stringResource(R.string.login_forgot_password),
            forgotPasswordContentDescription = stringResource(CommonR.string.content_description_open_link_label),
            proxyDescription = serverConfig.apiProxy?.host?.let { stringResource(R.string.proxy_credential_description, it) },
            invalidProxyIdentifier = stringResource(R.string.login_error_invalid_user_identifier),
            proxyIdentifierLabel = stringResource(AuthenticationR.string.login_proxy_identifier_label),
            proxyIdentifierPlaceholder = stringResource(AuthenticationR.string.login_user_identifier_placeholder),
            proxyPasswordLabel = stringResource(AuthenticationR.string.label_proxy_password),
            createAccountContentDescription = stringResource(R.string.content_description_self_profile_new_account_btn),
            passwordContentDescription = stringResource(R.string.content_description_login_password_field),
        ),
        userIdentifierTextState = loginEmailViewModel.userIdentifierTextState,
        passwordTextState = loginEmailViewModel.passwordTextState,
        proxyIdentifierState = loginEmailViewModel.proxyIdentifierTextState,
        proxyPasswordState = loginEmailViewModel.proxyPasswordTextState,
        onLoginButtonClick = loginEmailViewModel::login,
        onCreateAccount = {
            if (ENABLE_NEW_REGISTRATION) {
                onAction(
                    NewLoginPasswordScreenAction.CreateAccountSelector(
                        serverConfig,
                        loginEmailViewModel.userIdentifierTextState.text.toString(),
                    ),
                )
            } else {
                onAction(NewLoginPasswordScreenAction.CreatePersonalAccount(serverConfig))
            }
        },
        onForgotPassword = {
            CustomTabsHelper.launchUrl(context, serverConfig.forgotPassword).also {
                appLogger.d(serverConfig.forgotPassword)
            }
        },
        header = {
            NewAuthHeader(
                title = {
                    if (serverConfig.isOnPremises) {
                        ServerTitle(
                            serverLinks = serverConfig,
                            style = typography().title01,
                            textColor = colorsScheme().onSurface,
                            titleResId = R.string.enterprise_login_on_prem_welcome_title,
                            modifier = Modifier.padding(bottom = dimensions().spacing24x),
                        )
                    }
                    NewAuthSubtitle(stringResource(AuthenticationR.string.enterprise_login_password_title))
                },
                canNavigateBack = canNavigateBack,
                onNavigateBack = loginEmailViewModel::cancelLogin,
            )
        },
    )
    BackHandler(onBack = loginEmailViewModel::cancelLogin)
}
