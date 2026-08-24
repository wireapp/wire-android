/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.login

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.ui.authentication.BackendConfigSuccessContent
import com.wire.android.ui.authentication.MissingBackendConfigContent
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.login.email.AppLoginEmailViewModel
import com.wire.android.ui.authentication.login.email.LoginEmailScreen
import com.wire.android.ui.authentication.login.email.LoginEmailState
import com.wire.android.ui.authentication.login.sso.LoginSSOScreen
import com.wire.android.ui.common.dialogs.FeatureDisabledWithProxyDialogContent
import com.wire.android.ui.common.dialogs.FeatureDisabledWithProxyDialogState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.kalium.logic.data.user.UserId
import com.wire.android.feature.authentication.R as AuthenticationR

@Composable
internal fun MainLoginContent(
    onBackPressed: () -> Unit,
    onSuccess: (Boolean, Boolean, UserId) -> Unit,
    onRemoveDeviceNeeded: (UserId) -> Unit,
    loginNavArgs: LoginNavArgs,
    loginEmailViewModel: AppLoginEmailViewModel,
    ssoLoginResult: DeepLinkResult.SSOLogin?,
    ssoCodeAutoLogin: SSOCodeAutoLogin?,
) {
    val scroll = rememberScrollState()
    val backendState = loginEmailViewModel.loginState.backendConfigState
    val showSetup = shouldShowBackendSetup(
        loginEmailViewModel.isBackendConfigured,
        backendState == LoginEmailState.BackendConfigState.Success,
    )
    val ssoDialog = rememberVisibilityState<FeatureDisabledWithProxyDialogState>()
    FeatureDisabledWithProxyDialogContent(ssoDialog)
    LoginScreenContent(
        showBackendSetup = showSetup,
        initialTab = initialLoginTab(ssoLoginResult != null, ssoCodeAutoLogin != null),
        title = stringResource(if (showSetup) AuthenticationR.string.missing_backend_config_title else AuthenticationR.string.login_title),
        backContentDescription = AuthenticationR.string.content_description_login_back_btn,
        isProxyEnabled = loginEmailViewModel.serverConfig.isProxyEnabled,
        onBackPressed = onBackPressed,
        onSsoBlocked = {
            ssoDialog.show(
                ssoDialog.savedState ?: FeatureDisabledWithProxyDialogState(
                    AuthenticationR.string.sso_not_supported_dialog_description,
                ),
            )
        },
        emailContent = { LoginEmailScreen(onSuccess, onRemoveDeviceNeeded, loginEmailViewModel, scroll) },
        ssoContent = { LoginSSOScreen(onSuccess, onRemoveDeviceNeeded, loginNavArgs, ssoLoginResult, ssoCodeAutoLogin) },
        backendConfigContent = { BackendConfiguration(backendState, loginEmailViewModel) },
        subtitleContent = {
            if (!showSetup && loginEmailViewModel.serverConfig.isOnPremises) {
                ServerTitle(
                    serverLinks = loginEmailViewModel.serverConfig,
                    style = MaterialTheme.wireTypography.body01,
                )
            }
        },
    )
}

@Composable
private fun BackendConfiguration(state: LoginEmailState.BackendConfigState, viewModel: AppLoginEmailViewModel) {
    if (state == LoginEmailState.BackendConfigState.Success) {
        BackendConfigSuccessContent(
            modifier = Modifier.fillMaxWidth(),
            onContinue = viewModel::onBackendConfigSuccessContinue,
        )
    } else {
        MissingBackendConfigContent(
            modifier = Modifier.fillMaxWidth(),
            errorText = (state == LoginEmailState.BackendConfigState.Error)
                .takeIf { it }
                ?.let { stringResource(AuthenticationR.string.missing_backend_config_error) },
            isLoading = state == LoginEmailState.BackendConfigState.Loading,
            onConfigurationLinkEntered = viewModel::onBackendConfigLinkEntered,
        )
    }
}
