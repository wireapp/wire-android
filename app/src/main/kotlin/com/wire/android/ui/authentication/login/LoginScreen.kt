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

package com.wire.android.ui.authentication.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.authentication.loginEmailViewModel
import com.wire.android.navigation.style.TransitionAnimationType
import com.wire.android.ui.authentication.BackendConfigSuccessContent
import com.wire.android.ui.authentication.MissingBackendConfigContent
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.login.email.AppLoginEmailViewModel
import com.wire.android.ui.authentication.login.email.LoginEmailScreen
import com.wire.android.ui.authentication.login.email.LoginEmailState
import com.wire.android.ui.authentication.login.email.LoginEmailVerificationCodeScreen
import com.wire.android.ui.authentication.login.sso.LoginSSOScreen
import com.wire.android.ui.common.dialogs.FeatureDisabledWithProxyDialogContent
import com.wire.android.ui.common.dialogs.FeatureDisabledWithProxyDialogState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.UserId

/**
 * Navigation-neutral adapter used by the Navigation 3 host.
 */
@Composable
internal fun LoginRouteScreen(
    loginNavArgs: LoginNavArgs,
    loginEmailViewModel: AppLoginEmailViewModel,
    onBackPressed: () -> Unit,
    onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean, userId: UserId) -> Unit,
    onRemoveDeviceNeeded: (UserId) -> Unit,
) {
    LoginContent(
        onBackPressed = onBackPressed,
        onSuccess = onSuccess,
        onRemoveDeviceNeeded = onRemoveDeviceNeeded,
        loginNavArgs = loginNavArgs,
        loginEmailViewModel = loginEmailViewModel,
        ssoLoginResult = loginNavArgs.ssoLoginResult,
        ssoCodeAutoLogin = loginNavArgs.ssoCodeAutoLogin
    )
}

@Composable
private fun LoginContent(
    onBackPressed: () -> Unit,
    onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean, userId: UserId) -> Unit,
    onRemoveDeviceNeeded: (UserId) -> Unit,
    loginNavArgs: LoginNavArgs,
    loginEmailViewModel: AppLoginEmailViewModel,
    ssoLoginResult: DeepLinkResult.SSOLogin?,
    ssoCodeAutoLogin: SSOCodeAutoLogin?,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = loginSurface(loginEmailViewModel.secondFactorVerificationCodeState.isCodeInputNecessary),
            transitionSpec = {
                TransitionAnimationType.SLIDE.enterTransition.togetherWith(TransitionAnimationType.SLIDE.exitTransition)
            }
        ) { surface ->
            if (surface == LoginSurface.Verification) {
                LoginEmailVerificationCodeScreen(loginEmailViewModel)
            } else {
                MainLoginContent(
                    onBackPressed = onBackPressed,
                    onSuccess = onSuccess,
                    onRemoveDeviceNeeded = onRemoveDeviceNeeded,
                    loginNavArgs = loginNavArgs,
                    loginEmailViewModel = loginEmailViewModel,
                    ssoLoginResult = ssoLoginResult,
                    ssoCodeAutoLogin = ssoCodeAutoLogin
                )
            }
        }
    }
}

@Composable
private fun MainLoginContent(
    onBackPressed: () -> Unit,
    onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean, userId: UserId) -> Unit,
    onRemoveDeviceNeeded: (UserId) -> Unit,
    loginNavArgs: LoginNavArgs,
    loginEmailViewModel: AppLoginEmailViewModel,
    ssoLoginResult: DeepLinkResult.SSOLogin?,
    ssoCodeAutoLogin: SSOCodeAutoLogin?,
) {

    val scrollState = rememberScrollState()
    val isBackendConfigured = loginEmailViewModel.isBackendConfigured
    val backendConfigState = loginEmailViewModel.loginState.backendConfigState
    val shouldShowBackendSetup = com.wire.android.ui.authentication.login.shouldShowBackendSetup(
        isBackendConfigured,
        backendConfigState == LoginEmailState.BackendConfigState.Success,
    )
    val ssoDisabledWithProxyDialogState = rememberVisibilityState<FeatureDisabledWithProxyDialogState>()
    FeatureDisabledWithProxyDialogContent(dialogState = ssoDisabledWithProxyDialogState)
    com.wire.android.ui.authentication.login.LoginScreenContent(
        showBackendSetup = shouldShowBackendSetup,
        initialTab = com.wire.android.ui.authentication.login.initialLoginTab(
            ssoLoginResult != null,
            ssoCodeAutoLogin != null,
        ),
        title = stringResource(
            if (shouldShowBackendSetup) R.string.missing_backend_config_title else R.string.login_title
        ),
        backContentDescription = R.string.content_description_login_back_btn,
        isProxyEnabled = loginEmailViewModel.serverConfig.isProxyEnabled,
        onBackPressed = onBackPressed,
        onSsoBlocked = {
            ssoDisabledWithProxyDialogState.show(
                ssoDisabledWithProxyDialogState.savedState ?: FeatureDisabledWithProxyDialogState(
                    R.string.sso_not_supported_dialog_description
                )
            )
        },
        emailContent = {
            LoginEmailScreen(onSuccess, onRemoveDeviceNeeded, loginEmailViewModel, scrollState)
        },
        ssoContent = {
            LoginSSOScreen(onSuccess, onRemoveDeviceNeeded, loginNavArgs, ssoLoginResult, ssoCodeAutoLogin)
        },
        backendConfigContent = {
            if (backendConfigState == LoginEmailState.BackendConfigState.Success) {
                BackendConfigSuccessContent(
                    modifier = Modifier.fillMaxWidth(),
                    onContinue = loginEmailViewModel::onBackendConfigSuccessContinue,
                )
            } else MissingBackendConfigContent(
                modifier = Modifier.fillMaxWidth(),
                errorText = if (backendConfigState == LoginEmailState.BackendConfigState.Error) {
                    stringResource(R.string.missing_backend_config_error)
                } else null,
                isLoading = backendConfigState == LoginEmailState.BackendConfigState.Loading,
                onConfigurationLinkEntered = loginEmailViewModel::onBackendConfigLinkEntered,
            )
        },
        subtitleContent = {
            if (!shouldShowBackendSetup && loginEmailViewModel.serverConfig.isOnPremises) {
                ServerTitle(
                    serverLinks = loginEmailViewModel.serverConfig,
                    style = MaterialTheme.wireTypography.body01,
                )
            }
        },
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewLoginScreen() = WireTheme {
    WireTheme {
        MainLoginContent(
            onBackPressed = {},
            onSuccess = { _, _, _ -> },
            onRemoveDeviceNeeded = {},
            loginNavArgs = LoginNavArgs(),
            loginEmailViewModel = loginEmailViewModel(LoginNavArgs()),
            ssoLoginResult = null,
            ssoCodeAutoLogin = null
        )
    }
}
