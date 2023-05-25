/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.authentication.login

import androidx.annotation.StringRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.wire.android.R
import com.wire.android.navigation.rememberTrackingAnimatedNavController
import com.wire.android.navigation.smoothSlideInFromRight
import com.wire.android.navigation.smoothSlideOutFromLeft
import com.wire.android.ui.authentication.ServerTitle
import com.wire.android.ui.authentication.login.email.LoginEmailScreen
import com.wire.android.ui.authentication.login.email.LoginEmailVerificationCodeScreen
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel
import com.wire.android.ui.authentication.login.sso.LoginSSOScreen
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.dialogs.FeatureDisabledWithProxyDialogContent
import com.wire.android.ui.common.dialogs.FeatureDisabledWithProxyDialogState
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.dialogErrorStrings
import kotlinx.coroutines.launch

@Composable
fun LoginScreen() {
    val loginViewModel: LoginViewModel = hiltViewModel()
    val loginEmailViewModel: LoginEmailViewModel = hiltViewModel()

    LoginContent(
        onBackPressed = { loginViewModel.navigateBack() },
        loginViewModel,
        loginEmailViewModel,
        ssoLoginResult = loginViewModel.ssoLoginResult
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun LoginContent(
    onBackPressed: () -> Unit,
    viewModel: LoginViewModel,
    loginEmailViewModel: LoginEmailViewModel,
    ssoLoginResult: DeepLinkResult.SSOLogin?
) {
    val navController = rememberTrackingAnimatedNavController() { LoginNavigationItem.fromRoute(it)?.itemName }
    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedNavHost(
            navController = navController,
            startDestination = LoginNavigationItem.MAIN_LOGIN_FORM_SELECTION.route
        ) {
            composable(
                route = LoginNavigationItem.MAIN_LOGIN_FORM_SELECTION.route,
                enterTransition = { smoothSlideInFromRight() },
                exitTransition = { smoothSlideOutFromLeft() },
                content = { MainLoginContent(onBackPressed, viewModel, loginEmailViewModel, ssoLoginResult) }
            )
            composable(
                route = LoginNavigationItem.EMAIL_SECOND_FACTOR_INPUT.route,
                enterTransition = { smoothSlideInFromRight() },
                exitTransition = { smoothSlideOutFromLeft() },
                content = { LoginEmailVerificationCodeScreen(loginEmailViewModel) }
            )
        }
    }
    val targetRoute: String = if (loginEmailViewModel.secondFactorVerificationCodeState.isCodeInputNecessary) {
        LoginNavigationItem.EMAIL_SECOND_FACTOR_INPUT.route
    } else {
        LoginNavigationItem.MAIN_LOGIN_FORM_SELECTION.route
    }
    if (navController.currentDestination?.route != targetRoute) {
        navController.navigate(targetRoute)
    }
}

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalPagerApi::class,
    ExperimentalFoundationApi::class,
)
@Composable
private fun MainLoginContent(
    onBackPressed: () -> Unit,
    viewModel: LoginViewModel,
    loginEmailViewModel: LoginEmailViewModel,
    ssoLoginResult: DeepLinkResult.SSOLogin?
) {

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val initialPageIndex = if (ssoLoginResult == null) LoginTabItem.EMAIL.ordinal else LoginTabItem.SSO.ordinal
    val pagerState = rememberPagerState(initialPage = initialPageIndex)

    val ssoDisabledWithProxyDialogState = rememberVisibilityState<FeatureDisabledWithProxyDialogState>()
    FeatureDisabledWithProxyDialogContent(dialogState = ssoDisabledWithProxyDialogState)

    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.rememberTopBarElevationState().value,
                title = stringResource(R.string.login_title),
                subtitleContent = {
                    if (viewModel.serverConfig.isOnPremises) {
                        ServerTitle(
                            serverLinks = viewModel.serverConfig,
                            style = MaterialTheme.wireTypography.body01
                        )
                    }
                },
                onNavigationPressed = onBackPressed
            ) {
                WireTabRow(
                    tabs = LoginTabItem.values().toList(),
                    selectedTabIndex = pagerState.calculateCurrentTab(),
                    onTabChange = {

                        if (viewModel.loginState.isProxyEnabled) {
                            if (pagerState.currentPage != LoginTabItem.SSO.ordinal) {
                                ssoDisabledWithProxyDialogState.show(
                                    ssoDisabledWithProxyDialogState.savedState ?: FeatureDisabledWithProxyDialogState(
                                        R.string.sso_not_supported_dialog_description
                                    )
                                )
                            }
                        } else {
                            scope.launch { pagerState.animateScrollToPage(it) }
                        }
                    },
                    modifier = Modifier.padding(
                        start = MaterialTheme.wireDimensions.spacing16x,
                        end = MaterialTheme.wireDimensions.spacing16x
                    ),
                    divider = {} // no divider
                )
            }
        },
        modifier = Modifier.fillMaxHeight(),
    ) { internalPadding ->
        var focusedTabIndex: Int by remember { mutableStateOf(initialPageIndex) }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            HorizontalPager(
                state = pagerState,
                count = LoginTabItem.values().size,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(internalPadding)
            ) { pageIndex ->
                when (LoginTabItem.values()[pageIndex]) {
                    LoginTabItem.EMAIL -> LoginEmailScreen(loginEmailViewModel, scrollState)
                    LoginTabItem.SSO -> LoginSSOScreen(ssoLoginResult)
                }
            }
            if (!pagerState.isScrollInProgress && focusedTabIndex != pagerState.currentPage) {
                LaunchedEffect(Unit) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    focusedTabIndex = pagerState.currentPage
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoginErrorDialog(
    error: LoginError,
    onDialogDismiss: () -> Unit,
    updateTheApp: () -> Unit,
    ssoLoginResult: DeepLinkResult.SSOLogin? = null
) {
    val dialogErrorData: LoginDialogErrorData = when (error) {
        is LoginError.DialogError.InvalidCredentialsError -> LoginDialogErrorData(
            stringResource(R.string.login_error_invalid_credentials_title),
            stringResource(R.string.login_error_invalid_credentials_message),
            onDialogDismiss
        )

        is LoginError.DialogError.UserAlreadyExists -> LoginDialogErrorData(
            stringResource(R.string.login_error_user_already_logged_in_title),
            stringResource(R.string.login_error_user_already_logged_in_message),
            onDialogDismiss
        )

        is LoginError.DialogError.ProxyError -> {
            LoginDialogErrorData(
                stringResource(R.string.error_socket_title),
                stringResource(R.string.error_socket_message),
                onDialogDismiss
            )
        }

        is LoginError.DialogError.GenericError -> {
            val strings = error.coreFailure.dialogErrorStrings(LocalContext.current.resources)
            LoginDialogErrorData(
                strings.title,
                strings.message,
                onDialogDismiss
            )
        }

        is LoginError.DialogError.InvalidSSOCodeError -> LoginDialogErrorData(
            stringResource(R.string.login_error_invalid_credentials_title),
            stringResource(R.string.login_error_invalid_sso_code),
            onDialogDismiss
        )

        is LoginError.DialogError.InvalidSSOCookie -> LoginDialogErrorData(
            stringResource(R.string.login_sso_error_invalid_cookie_title),
            stringResource(R.string.login_sso_error_invalid_cookie_message),
            onDialogDismiss
        )

        is LoginError.DialogError.SSOResultError -> {
            with(ssoLoginResult as DeepLinkResult.SSOLogin.Failure) {
                LoginDialogErrorData(
                    stringResource(R.string.sso_error_dialog_title),
                    stringResource(R.string.sso_error_dialog_message, this.ssoError.errorCode),
                    onDialogDismiss
                )
            }
        }

        is LoginError.DialogError.ServerVersionNotSupported -> LoginDialogErrorData(
            title = stringResource(R.string.api_versioning_server_version_not_supported_title),
            body = stringResource(R.string.api_versioning_server_version_not_supported_message),
            onDismiss = onDialogDismiss,
            actionTextId = R.string.label_close,
            dismissOnClickOutside = false
        )

        is LoginError.DialogError.ClientUpdateRequired -> LoginDialogErrorData(
            title = stringResource(R.string.api_versioning_client_update_required_title),
            body = stringResource(R.string.api_versioning_client_update_required_message),
            onDismiss = onDialogDismiss,
            actionTextId = R.string.label_update,
            onAction = updateTheApp,
            dismissOnClickOutside = false
        )

        LoginError.DialogError.PasswordNeededToRegisterClient -> TODO()

        else -> LoginDialogErrorData(
            stringResource(R.string.error_unknown_title),
            stringResource(R.string.error_unknown_message),
            onDialogDismiss
        )
    }

    WireDialog(
        title = dialogErrorData.title,
        text = dialogErrorData.body,
        onDismiss = dialogErrorData.onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            text = stringResource(dialogErrorData.actionTextId),
            onClick = dialogErrorData.onAction,
            type = WireDialogButtonType.Primary
        ),
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = dialogErrorData.dismissOnClickOutside,
            usePlatformDefaultWidth = false
        )
    )
}

data class LoginDialogErrorData(
    val title: String,
    val body: String,
    val onDismiss: () -> Unit,
    @StringRes val actionTextId: Int = R.string.label_ok,
    val onAction: () -> Unit = onDismiss,
    val dismissOnClickOutside: Boolean = true
)

enum class LoginTabItem(@StringRes override val titleResId: Int) : TabItem {
    EMAIL(R.string.login_tab_email),
    SSO(R.string.login_tab_sso);
}

@Preview
@Composable
private fun PreviewLoginScreen() {
    WireTheme(isPreview = true) {
        MainLoginContent(onBackPressed = { }, hiltViewModel(), hiltViewModel(), ssoLoginResult = null)
    }
}
