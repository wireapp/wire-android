package com.wire.android.ui.userprofile.other

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.wire.android.R
import com.wire.android.model.PreservedState
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topBarElevation
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.userprofile.common.EditableState
import com.wire.android.ui.userprofile.common.UserProfileInfo
import com.wire.android.ui.userprofile.group.RemoveConversationMemberState
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.user.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun OtherUserProfileScreen(viewModel: OtherUserProfileScreenViewModel = hiltViewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    OtherProfileScreenContent(
        state = viewModel.state,
        removeMemberDialogState = viewModel.removeConversationMemberDialogState,
        onSendConnectionRequest = viewModel::sendConnectionRequest,
        onOpenConversation = viewModel::openConversation,
        onCancelConnectionRequest = viewModel::cancelConnectionRequest,
        ignoreConnectionRequest = viewModel::ignoreConnectionRequest,
        acceptConnectionRequest = viewModel::acceptConnectionRequest,
        onNavigateBack = viewModel::navigateBack,
        changeMemberRole = viewModel::changeMemberRole,
        openRemoveConversationMemberDialog = viewModel::openRemoveConversationMemberDialog,
        hideRemoveConversationMemberDialog = viewModel::hideRemoveConversationMemberDialog,
        onRemoveConversationMember = viewModel::removeConversationMember,
        snackbarHostState = snackbarHostState
    )
    LaunchedEffect(Unit) {
        viewModel.infoMessage.collect { snackbarHostState.showSnackbar(it.asString(context.resources)) }
    }
}

@SuppressLint("UnusedCrossfadeTargetStateParameter")
@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalPagerApi::class,
)
@Composable
fun OtherProfileScreenContent(
    state: OtherUserProfileState,
    removeMemberDialogState: PreservedState<RemoveConversationMemberState>?,
    onSendConnectionRequest: () -> Unit,
    onOpenConversation: () -> Unit,
    onCancelConnectionRequest: () -> Unit,
    acceptConnectionRequest: () -> Unit,
    ignoreConnectionRequest: () -> Unit,
    onNavigateBack: () -> Unit,
    changeMemberRole: (Member.Role) -> Unit,
    openRemoveConversationMemberDialog: () -> Unit,
    hideRemoveConversationMemberDialog: () -> Unit,
    onRemoveConversationMember: (PreservedState<RemoveConversationMemberState>) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
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

    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val openChangeRoleBottomSheet: () -> Unit = remember { { scope.launch { sheetState.show() } } }
    val closeChangeRoleBottomSheet: () -> Unit = remember { { scope.launch { sheetState.hide() } } }

    WireModalSheetLayout(
        sheetState = sheetState,
        coroutineScope = rememberCoroutineScope(),
        sheetContent = {
            EditGroupRoleBottomSheet(state, changeMemberRole, closeChangeRoleBottomSheet)
        }
    ) {
        CollapsingTopBarScaffold(
            snackbarHost = {
                SwipeDismissSnackbarHost(
                    hostState = otherUserProfileScreenState.snackbarHostState,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            topBarHeader = { elevation -> TopBarHeader(state = state, elevation = elevation, onNavigateBack, openChangeRoleBottomSheet) },
            topBarCollapsing = { TopBarCollapsing(state) },
            topBarFooter = { TopBarFooter(state, pagerState, tabBarElevationState, tabItems, currentTabState, scope) },
            content = {
                Content(
                    state,
                    pagerState,
                    tabItems,
                    otherUserProfileScreenState,
                    lazyListStates,
                    openChangeRoleBottomSheet,
                    openRemoveConversationMemberDialog
                )
            },
            contentFooter = {
                ContentFooter(
                    state,
                    maxBarElevation,
                    onSendConnectionRequest,
                    onOpenConversation,
                    onCancelConnectionRequest,
                    acceptConnectionRequest,
                    ignoreConnectionRequest
                )
            },
            isSwipeable = state.connectionStatus == ConnectionState.ACCEPTED
        )
    }
    RemoveConversationMemberDialog(
        dialogState = removeMemberDialogState,
        onDialogDismiss = hideRemoveConversationMemberDialog,
        onRemoveConversationMember = onRemoveConversationMember
    )
}

@Composable
private fun TopBarHeader(
    state: OtherUserProfileState,
    elevation: Dp,
    onNavigateBack: () -> Unit,
    openChangeRoleBottomSheet: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onNavigateBack,
        title = stringResource(id = R.string.user_profile_title),
        elevation = elevation,
        actions = {
            if (state.connectionStatus in listOf(ConnectionState.ACCEPTED, ConnectionState.BLOCKED)) {
                MoreOptionIcon(openChangeRoleBottomSheet)
            }
        }
    )
}

@SuppressLint("UnusedCrossfadeTargetStateParameter")
@Composable
private fun TopBarCollapsing(state: OtherUserProfileState) {
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
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun TopBarFooter(
    state: OtherUserProfileState,
    pagerState: PagerState,
    tabBarElevation: Dp,
    tabItems: List<OtherUserProfileTabItem>,
    currentTab: Int,
    scope: CoroutineScope
) {
    if (state.connectionStatus == ConnectionState.ACCEPTED) {
        AnimatedVisibility(
            visible = !state.isDataLoading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Surface(
                shadowElevation = tabBarElevation,
                color = MaterialTheme.wireColorScheme.background
            ) {
                WireTabRow(
                    tabs = tabItems,
                    selectedTabIndex = currentTab,
                    onTabChange = { scope.launch { pagerState.animateScrollToPage(it) } },
                    divider = {} // no divider
                )
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class, ExperimentalFoundationApi::class)
@Composable
private fun Content(
    state: OtherUserProfileState,
    pagerState: PagerState,
    tabItems: List<OtherUserProfileTabItem>,
    otherUserProfileScreenState: OtherUserProfileScreenState,
    lazyListStates: Map<OtherUserProfileTabItem, LazyListState>,
    openChangeRoleBottomSheet: () -> Unit,
    openRemoveConversationMemberDialog: () -> Unit
) {
    Crossfade(targetState = state) { state ->
        when {
            state.isDataLoading || state.botService != null -> Box {} // no content visible while loading
            state.connectionStatus == ConnectionState.ACCEPTED ->
                CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
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
                                    state.groupState!!,
                                    lazyListStates[tabItem]!!,
                                    openRemoveConversationMemberDialog,
                                    openChangeRoleBottomSheet
                                )
                        }
                    }
                }
            else -> {
                OtherUserConnectionStatusInfo(state.connectionStatus, state.membership)
            }
        }
    }
}

@Composable
private fun ContentFooter(
    state: OtherUserProfileState,
    maxBarElevation: Dp,
    onSendConnectionRequest: () -> Unit,
    onOpenConversation: () -> Unit,
    onCancelConnectionRequest: () -> Unit,
    acceptConnectionRequest: () -> Unit,
    ignoreConnectionRequest: () -> Unit,
) {
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
}

enum class OtherUserProfileTabItem(@StringRes override val titleResId: Int) : TabItem {
    GROUP(R.string.user_profile_group_tab),
    DETAILS(R.string.user_profile_details_tab);
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview(name = "Connected")
fun OtherProfileScreenContentPreview() {
    WireTheme(isPreview = true) {
        OtherProfileScreenContent(
            OtherUserProfileState.PREVIEW.copy(connectionStatus = ConnectionState.ACCEPTED),
            null,
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview(name = "Not Connected")
fun OtherProfileScreenContentNotConnectedPreview() {
    WireTheme(isPreview = true) {
        OtherProfileScreenContent(
            OtherUserProfileState.PREVIEW.copy(connectionStatus = ConnectionState.CANCELLED),
            null,
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}
        )
    }
}
