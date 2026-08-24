/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.login

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.wire.android.feature.authentication.R
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.UIText
import kotlinx.coroutines.launch

enum class LoginTabItem(@StringRes val titleResId: Int) : TabItem {
    EMAIL(R.string.login_tab_email),
    SSO(R.string.login_tab_sso);
    override val title: UIText = UIText.StringResource(titleResId)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoginScreenContent(
    showBackendSetup: Boolean,
    initialTab: LoginTabItem,
    title: String,
    backContentDescription: Int,
    isProxyEnabled: Boolean,
    onBackPressed: () -> Unit,
    onSsoBlocked: () -> Unit,
    emailContent: @Composable () -> Unit,
    ssoContent: @Composable () -> Unit,
    backendConfigContent: @Composable ColumnScope.() -> Unit,
    subtitleContent: @Composable ColumnScope.() -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialTab.ordinal, pageCount = { LoginTabItem.entries.size })
    WireScaffold(
        modifier = modifier.fillMaxHeight(),
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = androidx.compose.foundation.rememberScrollState().rememberTopBarElevationState().value,
                title = title,
                subtitleContent = subtitleContent,
                onNavigationPressed = onBackPressed,
                navigationIconType = NavigationIconType.Back(backContentDescription),
            ) {
                if (!showBackendSetup) WireTabRow(
                    tabs = LoginTabItem.entries.toList(),
                    selectedTabIndex = pagerState.calculateCurrentTab(),
                    onTabChange = { index ->
                        when (loginTabChange(isProxyEnabled, pagerState.currentPage, index)) {
                            LoginTabChange.BlockLeavingEmail -> onSsoBlocked()
                            LoginTabChange.Animate -> scope.launch { pagerState.animateScrollToPage(index) }
                            LoginTabChange.Ignore -> Unit
                        }
                    },
                    modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing16x),
                )
            }
        },
    ) { padding ->
        if (showBackendSetup) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(MaterialTheme.wireDimensions.spacing16x),
                content = backendConfigContent,
            )
        }
        else {
            var focusedTab by remember { mutableStateOf(initialTab.ordinal) }
            val keyboard = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current
            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().padding(padding)) { page ->
                    when (LoginTabItem.entries[page]) {
                        LoginTabItem.EMAIL -> emailContent()
                        LoginTabItem.SSO -> ssoContent()
                    }
                }
                if (!pagerState.isScrollInProgress && focusedTab != pagerState.currentPage) {
                    LaunchedEffect(pagerState.currentPage) {
                        keyboard?.hide()
                        focusManager.clearFocus()
                        focusedTab = pagerState.currentPage
                    }
                }
            }
        }
    }
}

fun initialLoginTab(hasSsoResult: Boolean, hasSsoAutoLogin: Boolean): LoginTabItem =
    if (hasSsoResult || hasSsoAutoLogin) LoginTabItem.SSO else LoginTabItem.EMAIL

fun shouldShowBackendSetup(isBackendConfigured: Boolean, backendConfigurationSucceeded: Boolean): Boolean =
    !isBackendConfigured || backendConfigurationSucceeded

enum class LoginTabChange { Animate, BlockLeavingEmail, Ignore }

fun loginTabChange(isProxyEnabled: Boolean, currentPage: Int, targetPage: Int): LoginTabChange =
    when {
        !isProxyEnabled -> LoginTabChange.Animate
        currentPage == LoginTabItem.EMAIL.ordinal -> LoginTabChange.BlockLeavingEmail
        else -> LoginTabChange.Ignore
    }
