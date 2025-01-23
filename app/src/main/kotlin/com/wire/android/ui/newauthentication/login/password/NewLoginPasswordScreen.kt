/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

package com.wire.android.ui.newauthentication.login.password

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.style.AuthSlideNavigationAnimation
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.login.LoginErrorDialog
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.WireAuthBackgroundLayout
import com.wire.android.ui.authentication.login.email.ForgotPasswordLabel
import com.wire.android.ui.authentication.login.email.LoginButton
import com.wire.android.ui.authentication.login.email.LoginEmailState
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel
import com.wire.android.ui.authentication.login.email.PasswordInput
import com.wire.android.ui.authentication.login.email.ProxyIdentifierInput
import com.wire.android.ui.authentication.login.email.ProxyPasswordInput
import com.wire.android.ui.authentication.login.email.UserIdentifierInput
import com.wire.android.ui.authentication.login.isProxyAuthRequired
import com.wire.android.ui.authentication.welcome.isProxyEnabled
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.EdgeToEdgePreview
import com.wire.android.ui.common.textfield.clearAutofillTree
import com.wire.android.ui.common.typography
import com.wire.android.ui.destinations.CreatePersonalAccountOverviewScreenDestination
import com.wire.android.ui.destinations.E2EIEnrollmentScreenDestination
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.destinations.InitialSyncScreenDestination
import com.wire.android.ui.destinations.RemoveDeviceScreenDestination
import com.wire.android.ui.newauthentication.login.NewLoginContainer
import com.wire.android.ui.newauthentication.login.NewLoginHeader
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.configuration.server.ServerConfig

@RootNavGraph
@WireDestination(
    navArgsDelegate = LoginNavArgs::class,
    style = AuthSlideNavigationAnimation::class,
)
@Composable
fun NewLoginPasswordScreen(
    navigator: Navigator,
    loginEmailViewModel: LoginEmailViewModel = hiltViewModel()
) {
    LaunchedEffect(loginEmailViewModel.secondFactorVerificationCodeState) {
        if (loginEmailViewModel.secondFactorVerificationCodeState.isCodeInputNecessary) {
            // TODO: handle 2FA code by opening the verification code screen
        }
    }
    LoginPasswordContent(
        serverConfig = loginEmailViewModel.serverConfig,
        loginEmailState = loginEmailViewModel.loginState,
        userIdentifierTextState = loginEmailViewModel.userIdentifierTextState,
        proxyIdentifierState = loginEmailViewModel.proxyIdentifierTextState,
        proxyPasswordState = loginEmailViewModel.proxyPasswordTextState,
        passwordTextState = loginEmailViewModel.passwordTextState,
        onDialogDismiss = loginEmailViewModel::clearLoginErrors,
        onSuccess = { initialSyncCompleted, isE2EIRequired ->
            val destination = when {
                isE2EIRequired -> E2EIEnrollmentScreenDestination
                initialSyncCompleted -> HomeScreenDestination
                else -> InitialSyncScreenDestination
            }
            navigator.navigate(NavigationCommand(destination, BackStackMode.CLEAR_WHOLE))
        },
        onRemoveDeviceOpen = {
            loginEmailViewModel.clearLoginErrors()
            navigator.navigate(NavigationCommand(RemoveDeviceScreenDestination, BackStackMode.CLEAR_WHOLE))
        },
        onLoginButtonClick = loginEmailViewModel::login,
        onUpdateApp = loginEmailViewModel::updateTheApp,
        onCreateAccount = {
            // TODO: should it open "create personal account" or "create team/enterprise account"?
            navigator.navigate(NavigationCommand(CreatePersonalAccountOverviewScreenDestination))
        },
        navigateBack = navigator::navigateBack,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun LoginPasswordContent(
    serverConfig: ServerConfig.Links,
    userIdentifierTextState: TextFieldState,
    passwordTextState: TextFieldState,
    proxyIdentifierState: TextFieldState,
    proxyPasswordState: TextFieldState,
    loginEmailState: LoginEmailState,
    onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean) -> Unit,
    onRemoveDeviceOpen: () -> Unit,
    onDialogDismiss: () -> Unit,
    onLoginButtonClick: () -> Unit,
    onUpdateApp: () -> Unit,
    onCreateAccount: () -> Unit,
    navigateBack: () -> Unit,
) {
    clearAutofillTree()

    LaunchedEffect(loginEmailState.flowState) {
        if (loginEmailState.flowState is LoginState.Success) {
            onSuccess(loginEmailState.flowState.initialSyncCompleted, loginEmailState.flowState.isE2EIRequired)
        } else if (loginEmailState.flowState is LoginState.Error.TooManyDevicesError) {
            onRemoveDeviceOpen()
        }
    }
    if (loginEmailState.flowState is LoginState.Error.DialogError) {
        LoginErrorDialog(loginEmailState.flowState, onDialogDismiss, onUpdateApp)
    }

    NewLoginContainer(
        header = {
            Column {
                if (serverConfig.isOnPremises) {
                    ServerTitle(
                        serverLinks = serverConfig,
                        style = typography().title01,
                        textColor = colorsScheme().onSurface,
                        titleResId = R.string.enterprise_login_on_prem_welcome_title,
                        modifier = Modifier
                            .padding(top = dimensions().spacing24x, start = dimensions().spacing24x, end = dimensions().spacing24x)
                    )
                }
                NewLoginHeader(
                    title = stringResource(id = R.string.enterprise_login_title),
                    canNavigateBack = true,
                    onNavigateBack = navigateBack
                )
            }
        }
    ) {
        Column(modifier = Modifier.wrapContentHeight()) {
            Column(
                modifier = Modifier.semantics {
                    testTagsAsResourceId = true
                }
            ) {
                UserIdentifierInput(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = dimensions().spacing8x),
                    userIdentifierState = userIdentifierTextState,
                    error = when (loginEmailState.flowState) {
                        is LoginState.Error.TextFieldError.InvalidValue -> stringResource(R.string.login_error_invalid_user_identifier)
                        else -> null
                    },
                    isEnabled = loginEmailState.userIdentifierEnabled,
                )
                PasswordInput(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = dimensions().spacing8x),
                    passwordState = passwordTextState,
                )
                if (serverConfig.isProxyAuthRequired) {
                    ForgotPasswordLabel(
                        forgotPasswordUrl = serverConfig.forgotPassword,
                        textColor = colorsScheme().onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensions().spacing24x)
                    )
                    ProxyContent(
                        proxyIdentifierState = proxyIdentifierState,
                        proxyPasswordState = proxyPasswordState,
                        proxyState = loginEmailState,
                        apiProxyUrl = serverConfig.apiProxy?.host,
                    )
                }
                LoginButton(
                    loading = loginEmailState.flowState is LoginState.Loading,
                    enabled = loginEmailState.loginEnabled,
                    text = stringResource(R.string.enterprise_login_next),
                    loadingText = stringResource(R.string.enterprise_login_next),
                    onClick = onLoginButtonClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = dimensions().spacing8x),

                    )
                if (!serverConfig.isProxyAuthRequired) {
                    ForgotPasswordLabel(
                        forgotPasswordUrl = serverConfig.forgotPassword,
                        textColor = colorsScheme().onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensions().spacing24x)
                    )
                }
                if (!serverConfig.isOnPremises && !serverConfig.isProxyEnabled()) {
                    CreateAccountLabel(
                        onCreateAccountClicked = onCreateAccount,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProxyContent(
    proxyIdentifierState: TextFieldState,
    proxyPasswordState: TextFieldState,
    proxyState: LoginEmailState,
    apiProxyUrl: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        apiProxyUrl?.let {
            Text(
                text = stringResource(R.string.proxy_credential_description, it),
                textAlign = TextAlign.Center,
                style = typography().body01.copy(color = colorsScheme().onBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensions().spacing24x, top = dimensions().spacing8x)
            )
        }
        ProxyIdentifierInput(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimensions().spacing8x),
            proxyIdentifierState = proxyIdentifierState,
            error = when (proxyState.flowState) {
                LoginState.Error.TextFieldError.InvalidValue -> stringResource(R.string.login_error_invalid_user_identifier)
                else -> null
            },
        )
        ProxyPasswordInput(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimensions().spacing24x),
            proxyPasswordState = proxyPasswordState,
        )
    }
}

@Composable
private fun CreateAccountLabel(onCreateAccountClicked: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(dimensions().buttonCornerSize),
        border = BorderStroke(width = dimensions().spacing1x, color = colorsScheme().outline),
        color = colorsScheme().surfaceContainerLow,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(dimensions().spacing8x)
        ) {
            Text(
                text = stringResource(R.string.enterprise_login_create_account_label),
                style = typography().body01,
                color = colorsScheme().onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.enterprise_login_create_account_text_button),
                style = typography().body02.copy(
                    textDecoration = TextDecoration.Underline,
                    color = colorsScheme().onSurface,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCreateAccountClicked,
                        onClickLabel = stringResource(R.string.content_description_self_profile_new_account_btn)
                    )
                    .testTag("Create account")
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewNewLoginPasswordScreen() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout {
            LoginPasswordContent(
                serverConfig = ServerConfig.DEFAULT,
                loginEmailState = LoginEmailState(),
                userIdentifierTextState = TextFieldState(),
                passwordTextState = TextFieldState(),
                proxyIdentifierState = TextFieldState(),
                proxyPasswordState = TextFieldState(),
                onSuccess = { _, _ -> },
                onDialogDismiss = {},
                onRemoveDeviceOpen = {},
                onLoginButtonClick = {},
                onUpdateApp = {},
                onCreateAccount = {},
                navigateBack = {},
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewNewLoginPasswordWithProxyScreen() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout {
            LoginPasswordContent(
                serverConfig = ServerConfig.DEFAULT.copy(
                    isOnPremises = true,
                    apiProxy = ServerConfig.ApiProxy(host = "some.proxy.com", port = 1234, needsAuthentication = true)
                ),
                loginEmailState = LoginEmailState(),
                userIdentifierTextState = TextFieldState(),
                passwordTextState = TextFieldState(),
                proxyIdentifierState = TextFieldState(),
                proxyPasswordState = TextFieldState(),
                onSuccess = { _, _ -> },
                onDialogDismiss = {},
                onRemoveDeviceOpen = {},
                onLoginButtonClick = {},
                onUpdateApp = {},
                onCreateAccount = {},
                navigateBack = {},
            )
        }
    }
}
