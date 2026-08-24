/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.authentication.login.email

import android.content.Context
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.authentication.login.AppLoginDialogError
import com.wire.android.ui.authentication.login.AppLoginState
import com.wire.android.ui.authentication.login.DomainClaimedByOrg
import com.wire.android.ui.authentication.login.LoginErrorDialog
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.toLoginDialogErrorData
import com.wire.android.ui.common.R as commonR
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.EmailAlreadyInUseClaimedDomainDialog
import com.wire.android.ui.common.textfield.clearAutofillTree
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.util.CustomTabsHelper
import com.wire.kalium.logic.data.user.UserId

@Composable
fun LoginEmailScreen(
    onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean, userId: UserId) -> Unit,
    onRemoveDeviceNeeded: (UserId) -> Unit,
    loginEmailViewModel: AppLoginEmailViewModel,
    scrollState: ScrollState = rememberScrollState(),
    fillMaxHeight: Boolean = true,
) {
    val context = LocalContext.current
    clearAutofillTree()

    com.wire.android.ui.authentication.login.email.LoginEmailContent(
        scrollState = scrollState,
        state = com.wire.android.ui.authentication.login.email.LoginEmailPresentationState(
            loading = loginEmailViewModel.loginState.flowState is LoginState.Loading,
            loginEnabled = loginEmailViewModel.loginState.loginEnabled,
            userIdentifierEnabled = loginEmailViewModel.loginState.userIdentifierEnabled,
            invalidUserIdentifier = loginEmailViewModel.loginState.flowState is LoginState.Error.TextFieldError.InvalidValue,
            invalidProxyIdentifier = loginEmailViewModel.loginState.flowState is LoginState.Error.TextFieldError.InvalidValue,
            showInvalidCredentialsError = loginEmailViewModel.loginState.showInvalidCredentialsError,
            proxyAuthRequired = loginEmailViewModel.serverConfig.isProxyAuthRequired,
            apiProxyUrl = loginEmailViewModel.serverConfig.apiProxy?.host,
        ),
        text = com.wire.android.ui.authentication.login.email.LoginEmailText(
            wireCredentials = stringResource(R.string.label_wire_credentials),
            userIdentifierLabel = stringResource(R.string.login_user_identifier_label),
            userIdentifierDescription = stringResource(R.string.content_description_login_user_identifier_field),
            invalidUserIdentifier = stringResource(R.string.login_error_invalid_user_identifier),
            passwordDescription = stringResource(R.string.content_description_login_password_field),
            invalidCredentials = stringResource(R.string.login_error_invalid_credentials_message),
            forgotPassword = stringResource(R.string.login_forgot_password),
            openLinkDescription = stringResource(commonR.string.content_description_open_link_label),
            proxyCredentials = stringResource(R.string.label_proxy_credentials),
            proxyDescription = { host -> context.getString(R.string.proxy_credential_description, host) },
            login = stringResource(R.string.label_login),
            loggingIn = stringResource(R.string.label_logging_in),
        ),
        userIdentifierTextState = loginEmailViewModel.userIdentifierTextState,
        proxyIdentifierTextState = loginEmailViewModel.proxyIdentifierTextState,
        proxyPasswordTextState = loginEmailViewModel.proxyPasswordTextState,
        passwordTextState = loginEmailViewModel.passwordTextState,
        onLoginButtonClick = loginEmailViewModel::login,
        onForgotPasswordClick = { openForgotPasswordPage(context, loginEmailViewModel.serverConfig.forgotPassword) },
        fillMaxHeight = fillMaxHeight,
    )

    LoginEmailStateNavigationAndDialogs(
        state = loginEmailViewModel.loginState.flowState,
        domainClaimedByOrg = loginEmailViewModel.domainClaimedByOrg,
        onClearLoginErrors = loginEmailViewModel::clearLoginErrors,
        onSuccess = onSuccess,
        onRemoveDeviceNeeded = onRemoveDeviceNeeded,
    )
}

@Composable
private fun LoginEmailStateNavigationAndDialogs(
    state: AppLoginState,
    domainClaimedByOrg: DomainClaimedByOrg?,
    onClearLoginErrors: () -> Unit,
    onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean, userId: UserId) -> Unit,
    onRemoveDeviceNeeded: (UserId) -> Unit,
) {
    val emailAlreadyInUseClaimedDomainDialogState = rememberVisibilityState<DomainClaimedByOrg.Claimed>()
    val handleLoginStateNavigation: (AppLoginState) -> Unit = {
        when (it) {
            is LoginState.Success<*> -> onSuccess(it.initialSyncCompleted, it.isE2EIRequired, it.userId as UserId)
            is LoginState.Error.TooManyDevicesError<*> -> {
                onClearLoginErrors()
                onRemoveDeviceNeeded(it.userId as UserId)
            }
            else -> {
                /* do nothing */
            }
        }
    }

    LaunchedEffect(state) {
        val isStateCompleted = state is LoginState.Success<*> || state is LoginState.Error.TooManyDevicesError<*>
        if (isStateCompleted && domainClaimedByOrg is DomainClaimedByOrg.Claimed) {
            emailAlreadyInUseClaimedDomainDialogState.show(domainClaimedByOrg)
        } else {
            handleLoginStateNavigation(state)
        }
    }

    if (state is LoginState.Error.DialogError<*, *>) {
        LoginErrorDialog((state as AppLoginDialogError).toLoginDialogErrorData(), onClearLoginErrors)
    }
    EmailAlreadyInUseClaimedDomainDialog(
        dialogState = emailAlreadyInUseClaimedDomainDialogState,
        onDismiss = {
            emailAlreadyInUseClaimedDomainDialogState.dismiss()
            handleLoginStateNavigation(state)
        }
    )
}

@Composable
fun ForgotPasswordLabel(
    forgotPasswordUrl: String,
    modifier: Modifier = Modifier,
    textColor: Color = colorsScheme().primary,
) {
    val context = LocalContext.current
    com.wire.android.ui.authentication.login.email.ForgotPasswordLink(
        label = stringResource(R.string.login_forgot_password),
        openLinkDescription = stringResource(commonR.string.content_description_open_link_label),
        onClick = { openForgotPasswordPage(context, forgotPasswordUrl) },
        modifier = modifier,
        textColor = textColor,
    )
}

@Composable
fun LoginButton(
    loading: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.label_login),
    loadingText: String = stringResource(R.string.label_logging_in),
    onClick: () -> Unit,
) = com.wire.android.ui.authentication.login.email.LoginButtonContent(
    loading = loading,
    enabled = enabled,
    onClick = onClick,
    modifier = modifier,
    text = text,
    loadingText = loadingText,
)

private fun openForgotPasswordPage(context: Context, forgotPasswordUrl: String) {
    CustomTabsHelper.launchUrl(context, forgotPasswordUrl).also {
        appLogger.d(forgotPasswordUrl)
    }
}
