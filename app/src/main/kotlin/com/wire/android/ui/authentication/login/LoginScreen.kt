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

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.style.TransitionAnimationType
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
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.E2EIEnrollmentScreenDestination
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.destinations.InitialSyncScreenDestination
import com.wire.android.ui.destinations.RemoveDeviceScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.dialogErrorStrings
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import kotlinx.coroutines.launch

@RootNavGraph
@WireDestination(
    navArgsDelegate = LoginNavArgs::class
)
@Composable
fun LoginScreen(
    navigator: Navigator,
    loginNavArgs: LoginNavArgs,
    loginEmailViewModel: LoginEmailViewModel = hiltViewModel()
) {

    LoginContent(
        onBackPressed = navigator::navigateBack,
        onSuccess = { initialSyncCompleted, isE2EIRequired ->
            val destination = if (isE2EIRequired) E2EIEnrollmentScreenDestination
            else if (initialSyncCompleted) HomeScreenDestination
            else InitialSyncScreenDestination

            navigator.navigate(NavigationCommand(destination, BackStackMode.CLEAR_WHOLE))
        },
        onRemoveDeviceNeeded = {
            navigator.navigate(NavigationCommand(RemoveDeviceScreenDestination, BackStackMode.CLEAR_WHOLE))
        },
        loginEmailViewModel = loginEmailViewModel,
        ssoLoginResult = loginNavArgs.ssoLoginResult
    )
}

@Composable
private fun LoginContent(
    onBackPressed: () -> Unit,
    onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean) -> Unit,
    onRemoveDeviceNeeded: () -> Unit,
    loginEmailViewModel: LoginEmailViewModel,
    ssoLoginResult: DeepLinkResult.SSOLogin?
) {
    Column(modifier = Modifier.fillMaxSize()) {
        /*
         TODO: we can change it to be a nested navigation graph when Compose Destinations 2.0 is released,
               right now it's not possible to make start destination for nested graph with mandatory arguments.
               More on that here: https://github.com/raamcosta/compose-destinations/issues/185
         */
        AnimatedContent(
            targetState = loginEmailViewModel.secondFactorVerificationCodeState.isCodeInputNecessary,
            transitionSpec = { TransitionAnimationType.SLIDE.enterTransition.togetherWith(TransitionAnimationType.SLIDE.exitTransition) }
        ) { isCodeInputNecessary ->
            if (isCodeInputNecessary) {
                LoginEmailVerificationCodeScreen(loginEmailViewModel)
            } else {
                MainLoginContent(onBackPressed, onSuccess, onRemoveDeviceNeeded, loginEmailViewModel, ssoLoginResult)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainLoginContent(
    onBackPressed: () -> Unit,
    onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean) -> Unit,
    onRemoveDeviceNeeded: () -> Unit,
    loginEmailViewModel: LoginEmailViewModel,
    ssoLoginResult: DeepLinkResult.SSOLogin?
) {

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val initialPageIndex = if (ssoLoginResult == null) LoginTabItem.EMAIL.ordinal else LoginTabItem.SSO.ordinal
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { LoginTabItem.values().size }
    )

    val ssoDisabledWithProxyDialogState = rememberVisibilityState<FeatureDisabledWithProxyDialogState>()
    FeatureDisabledWithProxyDialogContent(dialogState = ssoDisabledWithProxyDialogState)

    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.rememberTopBarElevationState().value,
                title = stringResource(R.string.login_title),
                subtitleContent = {
                    if (loginEmailViewModel.serverConfig.isOnPremises) {
                        ServerTitle(
                            serverLinks = loginEmailViewModel.serverConfig,
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

                        if (loginEmailViewModel.serverConfig.isProxyEnabled) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(internalPadding)
            ) { pageIndex ->
                when (LoginTabItem.values()[pageIndex]) {
                    LoginTabItem.EMAIL -> LoginEmailScreen(onSuccess, onRemoveDeviceNeeded, loginEmailViewModel, scrollState)
                    LoginTabItem.SSO -> LoginSSOScreen(onSuccess, onRemoveDeviceNeeded, ssoLoginResult)
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

@Composable
fun LoginErrorDialog(
    error: LoginState.Error,
    onDialogDismiss: () -> Unit,
    updateTheApp: () -> Unit,
    ssoLoginResult: DeepLinkResult.SSOLogin? = null
) {
    val dialogErrorData: LoginDialogErrorData = when (error) {
        is LoginState.Error.DialogError.InvalidCredentialsError -> LoginDialogErrorData(
            title = stringResource(R.string.login_error_invalid_credentials_title),
            body = AnnotatedString(stringResource(R.string.login_error_invalid_credentials_message)),
            onDismiss = onDialogDismiss
        )

        is LoginState.Error.DialogError.UserAlreadyExists -> LoginDialogErrorData(
            title = stringResource(R.string.login_error_user_already_logged_in_title),
            body = AnnotatedString(stringResource(R.string.login_error_user_already_logged_in_message)),
            onDismiss = onDialogDismiss
        )

        is LoginState.Error.DialogError.ProxyError -> {
            LoginDialogErrorData(
                title = stringResource(R.string.error_socket_title),
                body = AnnotatedString(stringResource(R.string.error_socket_message)),
                onDismiss = onDialogDismiss
            )
        }

        is LoginState.Error.DialogError.GenericError -> {
            val strings = error.coreFailure.dialogErrorStrings(LocalContext.current.resources)
            LoginDialogErrorData(
                strings.title,
                strings.annotatedMessage,
                onDialogDismiss
            )
        }

        is LoginState.Error.DialogError.InvalidSSOCodeError -> LoginDialogErrorData(
            title = stringResource(R.string.login_error_invalid_credentials_title),
            body = AnnotatedString(stringResource(R.string.login_error_invalid_sso_code)),
            onDismiss = onDialogDismiss
        )

        is LoginState.Error.DialogError.InvalidSSOCookie -> LoginDialogErrorData(
            title = stringResource(R.string.login_sso_error_invalid_cookie_title),
            body = AnnotatedString(stringResource(R.string.login_sso_error_invalid_cookie_message)),
            onDismiss = onDialogDismiss
        )

        is LoginState.Error.DialogError.SSOResultError -> {
            with(ssoLoginResult as DeepLinkResult.SSOLogin.Failure) {
                LoginDialogErrorData(
                    title = stringResource(R.string.sso_error_dialog_title),
                    body = AnnotatedString(stringResource(R.string.sso_error_dialog_message, this.ssoError.errorCode)),
                    onDismiss = onDialogDismiss
                )
            }
        }

        is LoginState.Error.DialogError.ServerVersionNotSupported -> LoginDialogErrorData(
            title = stringResource(R.string.api_versioning_server_version_not_supported_title),
            body = AnnotatedString(stringResource(R.string.api_versioning_server_version_not_supported_message)),
            onDismiss = onDialogDismiss,
            actionTextId = R.string.label_close,
            dismissOnClickOutside = false
        )

        is LoginState.Error.DialogError.ClientUpdateRequired -> LoginDialogErrorData(
            title = stringResource(R.string.api_versioning_client_update_required_title),
            body = AnnotatedString(stringResource(R.string.api_versioning_client_update_required_message)),
            onDismiss = onDialogDismiss,
            actionTextId = R.string.label_update,
            onAction = updateTheApp,
            dismissOnClickOutside = false
        )

        LoginState.Error.DialogError.PasswordNeededToRegisterClient -> TODO()

        else -> LoginDialogErrorData(
            title = stringResource(R.string.error_unknown_title),
            body = AnnotatedString(stringResource(R.string.error_unknown_message)),
            onDismiss = onDialogDismiss
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
    val body: AnnotatedString,
    val onDismiss: () -> Unit,
    @StringRes val actionTextId: Int = R.string.label_ok,
    val onAction: () -> Unit = onDismiss,
    val dismissOnClickOutside: Boolean = true
)

enum class LoginTabItem(@StringRes val titleResId: Int) : TabItem {
    EMAIL(R.string.login_tab_email),
    SSO(R.string.login_tab_sso);
    override val title: UIText = UIText.StringResource(titleResId)
}

@PreviewMultipleThemes
@Composable
private fun PreviewLoginScreen() = WireTheme {
    WireTheme {
        MainLoginContent(
            onBackPressed = {},
            onSuccess = { _, _ -> },
            onRemoveDeviceNeeded = {},
            loginEmailViewModel = hiltViewModel(),
            ssoLoginResult = null
        )
    }
}
