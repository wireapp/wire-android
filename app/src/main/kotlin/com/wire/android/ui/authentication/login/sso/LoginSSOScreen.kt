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

package com.wire.android.ui.authentication.login.sso

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.authentication.loginSSOViewModel
import com.wire.android.ui.authentication.login.AppLoginDialogError
import com.wire.android.ui.authentication.login.LoginErrorDialog
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.SSOCodeAutoLogin
import com.wire.android.ui.authentication.login.toLoginDialogErrorData
import com.wire.android.ui.common.dialogs.CustomServerDetailsDialog
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun LoginSSOScreen(
    onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean, userId: UserId) -> Unit,
    onRemoveDeviceNeeded: (UserId) -> Unit,
    loginNavArgs: LoginNavArgs,
    ssoLoginResult: DeepLinkResult.SSOLogin?,
    ssoCodeAutoLogin: SSOCodeAutoLogin?,
    loginSSOViewModel: AppLoginSSOViewModel = loginSSOViewModel(loginNavArgs),
    scrollState: ScrollState = rememberScrollState()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(ssoLoginResult) {
        loginSSOViewModel.handleSSOResult(
            ssoLoginResult,
        )
    }

    // Handle SSO code auto-login from intent parameter
    LaunchedEffect(ssoCodeAutoLogin) {
        ssoCodeAutoLogin?.let {
            loginSSOViewModel.handleSSOCodeAutoLogin(
                ssoCode = it.ssoCode,
                autoInitiateLogin = it.autoInitiateLogin,
                nomadServiceUrl = it.nomadServiceUrl,
                cookieLabel = it.cookieLabel,
            )
        }
    }
    com.wire.android.ui.authentication.login.sso.LoginSSOContent(
        scrollState = scrollState,
        ssoCodeTextState = loginSSOViewModel.ssoTextState,
        state = com.wire.android.ui.authentication.login.sso.LoginSSOPresentationState(
            loading = loginSSOViewModel.loginState.flowState is LoginState.Loading,
            loginEnabled = loginSSOViewModel.loginState.loginEnabled,
            invalidCode = loginSSOViewModel.loginState.flowState is LoginState.Error.TextFieldError.InvalidValue,
        ),
        text = com.wire.android.ui.authentication.login.sso.LoginSSOText(
            login = stringResource(R.string.label_login),
            loggingIn = stringResource(R.string.label_logging_in),
        ),
        onLoginButtonClick = loginSSOViewModel::login,
    )

    LoginSSOHostDialogs(loginSSOViewModel, onRemoveDeviceNeeded)

    LaunchedEffect(loginSSOViewModel) {
        loginSSOViewModel.openWebUrl.onEach { (url, serverConfig) ->
            CustomTabsHelper.launchUrl(context, url)
        }.launchIn(scope)
    }
    LaunchedEffect(loginSSOViewModel.loginState.flowState) {
        (loginSSOViewModel.loginState.flowState as? LoginState.Success<*>)?.let {
            onSuccess(it.initialSyncCompleted, it.isE2EIRequired, it.userId as UserId)
        }
    }
}

@Composable
private fun LoginSSOHostDialogs(
    loginSSOViewModel: AppLoginSSOViewModel,
    onRemoveDeviceOpen: (UserId) -> Unit,
) {
    val loginSSOState = loginSSOViewModel.loginState
    val flowState = loginSSOState.flowState
    if (flowState is LoginState.Error.DialogError<*, *>) {
        LoginErrorDialog((flowState as AppLoginDialogError).toLoginDialogErrorData(), loginSSOViewModel::clearLoginErrors)
    } else if (flowState is LoginState.Error.TooManyDevicesError<*>) {
        loginSSOViewModel.clearLoginErrors()
        onRemoveDeviceOpen(flowState.userId as UserId)
    }

    loginSSOState.customServerDialogState?.let { customServerDialogState ->
        CustomServerDetailsDialog(
            serverLinks = customServerDialogState.serverLinks,
            onDismiss = loginSSOViewModel::onCustomServerDialogDismiss,
            onConfirm = loginSSOViewModel::onCustomServerDialogConfirm
        )
    }

    if (loginSSOState.showSsoIdentityChangedDialog) {
        SsoIdentityChangedDialog(
            onDismiss = loginSSOViewModel::onSsoIdentityChangeDismissed,
            onConfirm = loginSSOViewModel::onSsoIdentityChangeConfirmed,
        )
    }
}
