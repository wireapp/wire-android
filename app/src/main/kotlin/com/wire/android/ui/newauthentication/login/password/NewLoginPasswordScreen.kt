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

import com.wire.android.navigation.annotation.app.WireNewLoginDestination
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.BuildConfig
import com.wire.android.BuildConfig.ENABLE_NEW_REGISTRATION
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.AuthSlideNavigationAnimation
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.login.DomainClaimedByOrg
import com.wire.android.ui.authentication.login.LoginErrorDialog
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.PreFilledUserIdentifierType
import com.wire.android.ui.authentication.login.WireAuthBackgroundLayout
import com.wire.android.ui.authentication.login.email.ForgotPasswordLabel
import com.wire.android.ui.authentication.login.email.LoginButton
import com.wire.android.ui.authentication.login.email.LoginEmailState
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel
import com.wire.android.ui.authentication.login.email.ProxyIdentifierInput
import com.wire.android.ui.authentication.login.email.ProxyPasswordInput
import com.wire.android.ui.authentication.login.isProxyAuthRequired
import com.wire.android.ui.authentication.login.toLoginDialogErrorData
import com.wire.android.ui.authentication.welcome.isProxyEnabled
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.EmailAlreadyInUseClaimedDomainDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.EdgeToEdgePreview
import com.wire.android.ui.common.textfield.DefaultEmailNext
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WireAutoFillType
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.textfield.clearAutofillTree
import com.wire.android.ui.common.typography
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.ramcosta.composedestinations.generated.app.destinations.CreateAccountSelectorScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.CreatePersonalAccountOverviewScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.E2EIEnrollmentScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.InitialSyncScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.NewLoginVerificationCodeScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.RemoveDeviceScreenDestination
import com.wire.android.ui.newauthentication.login.NewAuthContainer
import com.wire.android.ui.newauthentication.login.NewAuthHeader
import com.wire.android.ui.newauthentication.login.NewAuthSubtitle
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.configuration.server.ServerConfig

@WireNewLoginDestination(
    navArgs = LoginNavArgs::class,
    style = AuthSlideNavigationAnimation::class,
)
@Composable
fun NewLoginPasswordScreen(
    navigator: Navigator,
    navArgs: LoginNavArgs,
    loginEmailViewModel: LoginEmailViewModel = hiltViewModel()
) {
    clearAutofillTree()
    LoginStateNavigationAndDialogs(loginEmailViewModel, navigator)

    LaunchedEffect(loginEmailViewModel.secondFactorVerificationCodeState) {
        if (loginEmailViewModel.secondFactorVerificationCodeState.isCodeInputNecessary) {
            val verificationCodeNavArgs = LoginNavArgs(
                loginPasswordPath = navArgs.loginPasswordPath,
                userHandle = PreFilledUserIdentifierType.PreFilled(loginEmailViewModel.userIdentifierTextState.text.toString())
            )
            navigator.navigate(NavigationCommand(NewLoginVerificationCodeScreenDestination(verificationCodeNavArgs)))
        }
    }

    LoginPasswordContent(
        serverConfig = loginEmailViewModel.serverConfig,
        loginEmailState = loginEmailViewModel.loginState,
        userIdentifierTextState = loginEmailViewModel.userIdentifierTextState,
        proxyIdentifierState = loginEmailViewModel.proxyIdentifierTextState,
        proxyPasswordState = loginEmailViewModel.proxyPasswordTextState,
        passwordTextState = loginEmailViewModel.passwordTextState,
        onLoginButtonClick = loginEmailViewModel::login,
        onCreateAccount = {
            if (ENABLE_NEW_REGISTRATION) {
                navigator.navigate(
                    NavigationCommand(
                        CreateAccountSelectorScreenDestination(
                            customServerConfig = loginEmailViewModel.serverConfig,
                            email = loginEmailViewModel.userIdentifierTextState.text.toString()
                        )
                    )
                )
            } else {
                navigator.navigate(NavigationCommand(CreatePersonalAccountOverviewScreenDestination(loginEmailViewModel.serverConfig)))
            }
        },
        canNavigateBack = navigator.navController.previousBackStackEntry != null, // if there is a previous screen to navigate back to
        navigateBack = loginEmailViewModel::cancelLogin,
        isCloudAccountCreationPossible = navArgs.loginPasswordPath?.isCloudAccountCreationPossible ?: true,
    )

    BackHandler {
        loginEmailViewModel.cancelLogin()
    }
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
    onLoginButtonClick: () -> Unit,
    onCreateAccount: () -> Unit,
    canNavigateBack: Boolean,
    navigateBack: () -> Unit,
    isCloudAccountCreationPossible: Boolean,
) {
    NewAuthContainer(
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
                    NewAuthSubtitle(
                        title = stringResource(id = R.string.enterprise_login_password_title),
                    )
                },
                canNavigateBack = canNavigateBack,
                onNavigateBack = navigateBack
            )
        }
    ) {
        Column(modifier = Modifier.wrapContentHeight()) {
            Column(
                modifier = Modifier.semantics {
                    testTagsAsResourceId = true
                }
            ) {
                EmailInput(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = dimensions().spacing8x),
                    userIdentifierState = userIdentifierTextState,
                    error = when (loginEmailState.flowState) {
                        is LoginState.Error.TextFieldError.InvalidValue -> stringResource(R.string.login_error_invalid_email)
                        else -> null
                    },
                    isEnabled = loginEmailState.userIdentifierEnabled,
                )
                PasswordInput(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = dimensions().spacing8x)
                        .testTag("PasswordInput"),
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
                        .padding(bottom = dimensions().spacing8x)
                        .testTag("LoginNextButton"),

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
                if (BuildConfig.ALLOW_ACCOUNT_CREATION &&
                    !serverConfig.isProxyEnabled() &&
                    isCloudAccountCreationPossible
                ) {
                    CreateAccountContent(
                        onCreateAccountClicked = onCreateAccount,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
fun EmailInput(
    userIdentifierState: TextFieldState,
    error: String?,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    WireTextField(
        autoFillType = WireAutoFillType.Login,
        textState = userIdentifierState,
        placeholderText = stringResource(R.string.login_email_placeholder),
        labelText = stringResource(R.string.login_email_label),
        state = when {
            !isEnabled -> WireTextFieldState.Disabled
            error != null -> WireTextFieldState.Error(error)
            else -> WireTextFieldState.Default
        },
        semanticDescription = stringResource(R.string.content_description_login_email_field),
        keyboardOptions = KeyboardOptions.DefaultEmailNext,
        modifier = modifier.testTag("emailField"),
        testTag = "userIdentifierInput"
    )
}

@Composable
fun PasswordInput(passwordState: TextFieldState, modifier: Modifier = Modifier) {
    val keyboardController = LocalSoftwareKeyboardController.current
    WirePasswordTextField(
        textState = passwordState,
        keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
        onKeyboardAction = { keyboardController?.hide() },
        semanticDescription = stringResource(R.string.content_description_login_password_field),
        modifier = modifier.testTag("passwordField"),
        autoFill = true,
        testTag = "PasswordInput"
    )
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
private fun CreateAccountContent(onCreateAccountClicked: () -> Unit, modifier: Modifier = Modifier) {
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

@Composable
fun LoginStateNavigationAndDialogs(viewModel: LoginEmailViewModel, navigator: Navigator) {
    val state = viewModel.loginState.flowState
    val emailAlreadyInUseClaimedDomainDialogState = rememberVisibilityState<DomainClaimedByOrg.Claimed>()
    val handleLoginStateNavigation: (LoginState) -> Unit = {
        when (it) {
            is LoginState.Success -> {
                val destination = when {
                    it.isE2EIRequired -> E2EIEnrollmentScreenDestination
                    it.initialSyncCompleted -> HomeScreenDestination
                    else -> InitialSyncScreenDestination
                }
                navigator.navigate(NavigationCommand(destination, BackStackMode.CLEAR_WHOLE))
            }

            is LoginState.Error.TooManyDevicesError -> {
                viewModel.clearLoginErrors()
                navigator.navigate(NavigationCommand(RemoveDeviceScreenDestination, BackStackMode.CLEAR_WHOLE))
            }

            is LoginState.Canceled -> {
                navigator.navigateBack()
            }

            else -> {
                /* do nothing */
            }
        }
    }
    LaunchedEffect(state) {
        val isDomainClaimedByOrg = viewModel.loginNavArgs.loginPasswordPath?.isDomainClaimedByOrg
        val isStateCompleted = state is LoginState.Success || state is LoginState.Error.TooManyDevicesError
        if (isStateCompleted && isDomainClaimedByOrg is DomainClaimedByOrg.Claimed) {
            emailAlreadyInUseClaimedDomainDialogState.show(isDomainClaimedByOrg)
        } else {
            handleLoginStateNavigation(state)
        }
    }
    if (state is LoginState.Error.DialogError) {
        LoginErrorDialog(state.toLoginDialogErrorData(), viewModel::clearLoginErrors)
    }
    EmailAlreadyInUseClaimedDomainDialog(
        dialogState = emailAlreadyInUseClaimedDomainDialogState,
        onDismiss = {
            emailAlreadyInUseClaimedDomainDialogState.dismiss()
            handleLoginStateNavigation(state)
        }
    )
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
                onLoginButtonClick = {},
                onCreateAccount = {},
                canNavigateBack = true,
                navigateBack = {},
                isCloudAccountCreationPossible = true,
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewNewLoginPasswordScreenWithProxy() = WireTheme {
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
                onLoginButtonClick = {},
                onCreateAccount = {},
                canNavigateBack = false,
                navigateBack = {},
                isCloudAccountCreationPossible = true,
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewNewLoginPasswordScreenWithCloudDisabledAccountCreation() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout {
            LoginPasswordContent(
                serverConfig = ServerConfig.DEFAULT,
                loginEmailState = LoginEmailState(),
                userIdentifierTextState = TextFieldState(),
                passwordTextState = TextFieldState(),
                proxyIdentifierState = TextFieldState(),
                proxyPasswordState = TextFieldState(),
                onLoginButtonClick = {},
                onCreateAccount = {},
                canNavigateBack = false,
                navigateBack = {},
                isCloudAccountCreationPossible = false,
            )
        }
    }
}
