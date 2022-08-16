package com.wire.android.ui.authentication.login

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.wire.android.R
import com.wire.android.ui.authentication.login.email.LoginEmailScreen
import com.wire.android.ui.authentication.login.sso.LoginSSOScreen
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.DialogErrorStrings
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.dialogErrorStrings
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
fun LoginScreen(ssoLoginResult: DeepLinkResult.SSOLogin?) {
    val loginViewModel: LoginViewModel = hiltViewModel()
    LoginContent(
        onBackPressed = { loginViewModel.navigateBack() },
        loginViewModel,
        loginViewModel.loginState,
        ssoLoginResult = ssoLoginResult
    )
}

@OptIn(
    ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class, ExperimentalPagerApi::class, ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class
)
@Composable
private fun LoginContent(
    onBackPressed: () -> Unit,
    viewModel: LoginViewModel,
    loginState: LoginState,
    ssoLoginResult: DeepLinkResult.SSOLogin?
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val initialPageIndex = if (ssoLoginResult == null) LoginTabItem.EMAIL.ordinal else LoginTabItem.SSO.ordinal
    val pagerState = rememberPagerState(initialPage = initialPageIndex)
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.rememberTopBarElevationState().value,
                title = stringResource(R.string.login_title),
                onNavigationPressed = onBackPressed
            ) {
                WireTabRow(
                    tabs = LoginTabItem.values().toList(),
                    selectedTabIndex = pagerState.calculateCurrentTab(),
                    onTabChange = { scope.launch { pagerState.animateScrollToPage(it) } },
                    modifier = Modifier.padding(
                        start = MaterialTheme.wireDimensions.spacing16x,
                        end = MaterialTheme.wireDimensions.spacing16x,
                        top = MaterialTheme.wireDimensions.spacing16x
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
        if (loginState.loginError is LoginError.DialogError.InvalidSession) {
            LoginErrorDialog(loginState.loginError, viewModel::onDialogDismiss)
        }
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            HorizontalPager(
                state = pagerState,
                count = LoginTabItem.values().size,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(internalPadding)
            ) { pageIndex ->
                when (LoginTabItem.values()[pageIndex]) {
                    LoginTabItem.EMAIL -> LoginEmailScreen(scrollState)
                    LoginTabItem.SSO -> LoginSSOScreen(ssoLoginResult)
                }
            }
            if (!pagerState.isScrollInProgress && focusedTabIndex != pagerState.currentPage)
                LaunchedEffect(Unit) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    focusedTabIndex = pagerState.currentPage
                }
        }
    }
}

@Composable
fun LoginErrorDialog(
    error: LoginError,
    onDialogDismiss: () -> Unit,
    ssoLoginResult: DeepLinkResult.SSOLogin? = null
) {
    val (title, message) = when (error) {
        is LoginError.DialogError.InvalidCredentialsError -> DialogErrorStrings(
            stringResource(id = R.string.login_error_invalid_credentials_title),
            stringResource(id = R.string.login_error_invalid_credentials_message)
        )

        is LoginError.DialogError.UserAlreadyExists -> DialogErrorStrings(
            stringResource(id = R.string.login_error_user_already_logged_in_title),
            stringResource(id = R.string.login_error_user_already_logged_in_message)
        )

        is LoginError.DialogError.InvalidSession.SessionExpired -> DialogErrorStrings(
            stringResource(id = R.string.session_expired_error_title),
            stringResource(id = R.string.session_expired_error_message)
        )

        is LoginError.DialogError.InvalidSession.DeletedAccount -> DialogErrorStrings(
            stringResource(id = R.string.deleted_user_error_title),
            stringResource(id = R.string.deleted_user_error_message)
        )

        is LoginError.DialogError.InvalidSession.RemovedClient -> DialogErrorStrings(
            stringResource(id = R.string.removed_client_error_title),
            stringResource(id = R.string.removed_client_error_message)
        )


        is LoginError.DialogError.GenericError ->
            error.coreFailure.dialogErrorStrings(LocalContext.current.resources)

        is LoginError.DialogError.InvalidCodeError -> DialogErrorStrings(
            title = stringResource(id = R.string.login_error_invalid_credentials_title),
            message = stringResource(id = R.string.login_error_invalid_sso_code)
        )

        is LoginError.DialogError.InvalidSSOCookie -> DialogErrorStrings(
            stringResource(id = R.string.login_sso_error_invalid_cookie_title),
            stringResource(id = R.string.login_sso_error_invalid_cookie_message)
        )

        is LoginError.DialogError.SSOResultError -> {
            with(ssoLoginResult as DeepLinkResult.SSOLogin.Failure) {
                DialogErrorStrings(
                    stringResource(R.string.sso_erro_dialog_title),
                    stringResource(R.string.sso_erro_dialog_message, this.ssoError.errorCode)
                )
            }
        }

        LoginError.DialogError.PasswordNeededToRegisterClient -> TODO()
        else -> DialogErrorStrings(
            stringResource(R.string.error_unknown_title),
            stringResource(R.string.error_unknown_message)
        )
    }
    WireDialog(
        title = title,
        text = message,
        onDismiss = onDialogDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDialogDismiss,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    )
}

enum class LoginTabItem(@StringRes override val titleResId: Int) : TabItem {
    EMAIL(R.string.login_tab_email),
    SSO(R.string.login_tab_sso);
}

@OptIn(ExperimentalMaterialApi::class)
@Preview
@Composable
private fun LoginScreenPreview() {
    WireTheme(isPreview = true) {
        LoginContent(onBackPressed = { }, hiltViewModel(), LoginState(), ssoLoginResult = null)
    }
}
