package com.wire.android.ui.authentication.login

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalOverScrollConfiguration
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
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.appBarElevation
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.kalium.logic.configuration.ServerConfig
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
fun LoginScreen(serverConfig: ServerConfig, ssoLoginResult: DeepLinkResult.SSOLogin?) {
    val loginViewModel: LoginViewModel = hiltViewModel()
    LoginContent(
        onBackPressed = { loginViewModel.navigateBack() },
        //todo: temporary to show the remoteConfig
        serverTitle = serverConfig.title,
        serverConfig = serverConfig,
        ssoLoginResult = ssoLoginResult
    )
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class, ExperimentalPagerApi::class, ExperimentalFoundationApi::class)
@Composable
private fun LoginContent(
    onBackPressed: () -> Unit,
    serverTitle: String,
    serverConfig: ServerConfig,
    ssoLoginResult: DeepLinkResult.SSOLogin?
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val initialPageIndex = if (ssoLoginResult == null) LoginTabItem.EMAIL.ordinal else LoginTabItem.SSO.ordinal
    val pagerState = rememberPagerState(initialPage = initialPageIndex)
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.appBarElevation(),
                title = "${stringResource(R.string.login_title)} $serverTitle",
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
    ) {
        var focusedTabIndex: Int by remember { mutableStateOf(initialPageIndex) }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        CompositionLocalProvider(LocalOverScrollConfiguration provides null) {
            HorizontalPager(
                state = pagerState,
                count = LoginTabItem.values().size,
                modifier = Modifier.fillMaxWidth()
            ) { pageIndex ->
                when (LoginTabItem.values()[pageIndex]) {
                    LoginTabItem.EMAIL -> LoginEmailScreen(serverConfig, scrollState)
                    LoginTabItem.SSO -> LoginSSOScreen(ssoLoginResult, scrollState)
                }
            }
            if(!pagerState.isScrollInProgress && focusedTabIndex != pagerState.currentPage)
                LaunchedEffect(Unit) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    focusedTabIndex = pagerState.currentPage
                }
        }
    }
}

enum class LoginTabItem(@StringRes override val titleResId: Int) : TabItem {
    EMAIL(R.string.login_tab_email),
    SSO(R.string.login_tab_sso);
}

@Preview
@Composable
private fun LoginScreenPreview() {
    WireTheme(isPreview = true) {
        LoginContent(onBackPressed = { }, serverTitle = "", serverConfig = ServerConfig.STAGING, ssoLoginResult = null)
    }
}

