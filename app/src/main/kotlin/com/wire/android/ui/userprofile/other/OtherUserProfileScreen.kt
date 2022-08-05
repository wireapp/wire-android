package com.wire.android.ui.userprofile.other

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalOverScrollConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.wire.android.R
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.topBarElevation
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.userprofile.common.EditableState
import com.wire.android.ui.userprofile.common.UserProfileInfo
import kotlinx.coroutines.launch

@Composable
fun OtherUserProfileScreen(viewModel: OtherUserProfileScreenViewModel = hiltViewModel()) {
    OtherProfileScreenContent(
        state = viewModel.state,
        operationState = viewModel.connectionOperationState,
        onSendConnectionRequest = viewModel::sendConnectionRequest,
        onOpenConversation = viewModel::openConversation,
        onCancelConnectionRequest = viewModel::cancelConnectionRequest,
        ignoreConnectionRequest = viewModel::ignoreConnectionRequest,
        acceptConnectionRequest = viewModel::acceptConnectionRequest,
        onRemoveFromConversation = viewModel::removeFromConversation,
        onNavigateBack = viewModel::navigateBack
    )
}

@SuppressLint("UnusedCrossfadeTargetStateParameter")
@OptIn(ExperimentalPagerApi::class, ExperimentalFoundationApi::class)
@Composable
fun OtherProfileScreenContent(
    state: OtherUserProfileState,
    operationState: ConnectionOperationState?,
    onSendConnectionRequest: () -> Unit,
    onOpenConversation: () -> Unit,
    onCancelConnectionRequest: () -> Unit,
    acceptConnectionRequest: () -> Unit,
    ignoreConnectionRequest: () -> Unit,
    onNavigateBack: () -> Unit,
    onRemoveFromConversation: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val otherUserProfileScreenState = rememberOtherUserProfileScreenState(snackbarHostState)
    val tabItems by remember(state) {
        derivedStateOf { listOfNotNull(state.groupState?.let { OtherUserProfileTabItem.GROUP }, OtherUserProfileTabItem.DETAILS) }
    }
    val initialPage = 0
    val pagerState = rememberPagerState(initialPage = initialPage)
    val lazyListStates = OtherUserProfileTabItem.values().associateWith { rememberLazyListState() }
    val currentTabState by remember(state, pagerState) {
        derivedStateOf { if (state.isDataLoading) 0 else pagerState.calculateCurrentTab() }
    }
    val maxBarElevation = MaterialTheme.wireDimensions.topBarShadowElevation
    val tabBarElevationState by remember(tabItems, lazyListStates, currentTabState) {
        derivedStateOf { lazyListStates[tabItems[currentTabState]]?.topBarElevation(maxBarElevation) ?: 0.dp }
    }
    val scope = rememberCoroutineScope()

    handleOperationMessages(snackbarHostState, operationState)

    CollapsingTopBarScaffold(
        snackbarHost = {
            SwipeDismissSnackbarHost(
                hostState = otherUserProfileScreenState.snackbarHostState,
                modifier = Modifier.fillMaxWidth()
            )
        },
        topBarHeader = { elevation ->
            WireCenterAlignedTopAppBar(
                onNavigationPressed = onNavigateBack,
                title = stringResource(id = R.string.user_profile_title),
                elevation = elevation,
                actions = {
                    if (state.connectionStatus is ConnectionStatus.Connected) {
                        MoreOptionIcon({ })
                    }
                }
            )
        },
        topBarCollapsing = {
            Crossfade(targetState = state.isDataLoading) {
                UserProfileInfo(
                    isLoading = state.isAvatarLoading,
                    avatarAsset = state.userAvatarAsset,
                    fullName = state.fullName,
                    userName = state.userName,
                    teamName = state.teamName,
                    membership = state.membership,
                    editableState = EditableState.NotEditable,
                    modifier = Modifier.padding(bottom = dimensions().spacing16x)
                )
            }
        },
        topBarFooter = {
            if (state.connectionStatus == ConnectionStatus.Connected) {
                AnimatedVisibility(
                    visible = !state.isDataLoading,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Surface(
                        shadowElevation = tabBarElevationState,
                        color = MaterialTheme.wireColorScheme.background
                    ) {
                        WireTabRow(
                            tabs = tabItems,
                            selectedTabIndex = currentTabState,
                            onTabChange = { scope.launch { pagerState.animateScrollToPage(it) } },
                            divider = {} // no divider
                        )
                    }
                }
            }
        },
        content = {
            Crossfade(targetState = state) { state ->
                when {
                    state.isDataLoading || state.botService != null -> Box {} // no content visible while loading
                    state.connectionStatus is ConnectionStatus.Connected ->
                        CompositionLocalProvider(LocalOverScrollConfiguration provides null) {
                            HorizontalPager(
                                modifier = Modifier.fillMaxSize(),
                                state = pagerState,
                                count = tabItems.size
                            ) { pageIndex ->
                                when (val tabItem = tabItems[pageIndex]) {
                                    OtherUserProfileTabItem.DETAILS ->
                                        OtherUserProfileDetails(state, otherUserProfileScreenState, lazyListStates[tabItem]!!)
                                    OtherUserProfileTabItem.GROUP ->
                                        OtherUserProfileGroup(
                                            state = state.groupState!!,
                                            lazyListState = lazyListStates[tabItem]!!,
                                            onRemoveFromConversation = onRemoveFromConversation
                                        )
                                }
                            }
                        }
                    else -> {
                        OtherUserConnectionStatusInfo(state.connectionStatus, state.membership)
                    }
                }
            }
        },
        contentFooter = {
            AnimatedVisibility(
                visible = !state.isDataLoading,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Surface(
                    shadowElevation = maxBarElevation,
                    color = MaterialTheme.wireColorScheme.background
                ) {
                    Box(modifier = Modifier.padding(all = dimensions().spacing16x)) {
                        // TODO show open conversation button for service bots after AR-2135
                        if (state.membership != Membership.Service) {
                            OtherUserConnectionActionButton(
                                state.connectionStatus,
                                onSendConnectionRequest,
                                onOpenConversation,
                                onCancelConnectionRequest,
                                acceptConnectionRequest,
                                ignoreConnectionRequest
                            )
                        }
                    }
                }
            }
        }, isSwipeable = state.connectionStatus == ConnectionStatus.Connected
    )
}

enum class OtherUserProfileTabItem(@StringRes override val titleResId: Int) : TabItem {
    GROUP(R.string.user_profile_group_tab),
    DETAILS(R.string.user_profile_details_tab);
}

@Composable
private fun handleOperationMessages(
    snackbarHostState: SnackbarHostState,
    operationState: ConnectionOperationState?
) {
    operationState?.let { errorType ->
        val message = when (errorType) {
            is ConnectionOperationState.ConnectionRequestError -> stringResource(id = R.string.connection_request_sent_error)
            is ConnectionOperationState.SuccessConnectionSentRequest -> stringResource(id = R.string.connection_request_sent)
            is ConnectionOperationState.LoadUserInformationError -> stringResource(id = R.string.error_unknown_message)
            is ConnectionOperationState.SuccessConnectionAcceptRequest -> stringResource(id = R.string.connection_request_accepted)
            is ConnectionOperationState.SuccessConnectionCancelRequest -> stringResource(id = R.string.connection_request_canceled)
        }
        LaunchedEffect(errorType) {
            snackbarHostState.showSnackbar(message)
        }
    }
}

@Composable
@Preview(name = "Connected")
fun OtherProfileScreenContentPreview() {
    WireTheme(isPreview = true) {
        OtherProfileScreenContent(
            OtherUserProfileState.PREVIEW.copy(connectionStatus = ConnectionStatus.Connected), null, {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@Composable
@Preview(name = "Not Connected")
fun OtherProfileScreenContentNotConnectedPreview() {
    WireTheme(isPreview = true) {
        OtherProfileScreenContent(
            OtherUserProfileState.PREVIEW.copy(connectionStatus = ConnectionStatus.NotConnected), null, {}, {}, {}, {}, {}, {}, {}
        )
    }
}
