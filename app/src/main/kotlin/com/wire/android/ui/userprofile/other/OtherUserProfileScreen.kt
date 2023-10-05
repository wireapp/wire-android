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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.wire.android.ui.userprofile.other

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.dialogs.BlockUserDialogContent
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.common.dialogs.UnblockUserDialogContent
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topBarElevation
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.connection.ConnectionActionButton
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.DeviceDetailsScreenDestination
import com.wire.android.ui.home.conversations.details.dialog.ClearConversationContentDialog
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.userprofile.common.EditableState
import com.wire.android.ui.userprofile.common.UserProfileInfo
import com.wire.android.ui.userprofile.group.RemoveConversationMemberState
import com.wire.android.ui.userprofile.other.bottomsheet.OtherUserBottomSheetState
import com.wire.android.ui.userprofile.other.bottomsheet.OtherUserProfileBottomSheetContent
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch

@RootNavGraph
@Destination(
    navArgsDelegate = OtherUserProfileNavArgs::class,
    style = PopUpNavigationAnimation::class,
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserProfileScreen(
    navigator: Navigator,
    navArgs: OtherUserProfileNavArgs,
    viewModel: OtherUserProfileScreenViewModel = hiltViewModel(),
    resultNavigator: ResultBackNavigator<String>
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val sheetState = rememberWireModalSheetState()
    val openBottomSheet: () -> Unit = remember { { scope.launch { sheetState.show() } } }
    val closeBottomSheet: () -> Unit = remember { { scope.launch { sheetState.hide() } } }

    OtherProfileScreenContent(
        scope = scope,
        state = viewModel.state,
        requestInProgress = viewModel.requestInProgress,
        sheetState = sheetState,
        openBottomSheet = openBottomSheet,
        closeBottomSheet = closeBottomSheet,
        eventsHandler = viewModel,
        bottomSheetEventsHandler = viewModel,
        onIgnoreConnectionRequest = {
            resultNavigator.setResult(it)
            resultNavigator.navigateBack()
        },
        onOpenConversation = { navigator.navigate(NavigationCommand(ConversationScreenDestination(it), BackStackMode.UPDATE_EXISTED)) },
        onOpenDeviceDetails = { navigator.navigate(NavigationCommand(DeviceDetailsScreenDestination(navArgs.userId, it.clientId))) },
        navigateBack = navigator::navigateBack,
        navigationIconType = NavigationIconType.Close,
    )

    LaunchedEffect(Unit) {
        viewModel.infoMessage.collect {
            snackbarHostState.showSnackbar(it.asString(context.resources))
        }
    }
    LaunchedEffect(Unit) {
        viewModel.closeBottomSheet.collect {
            sheetState.hide()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("UnusedCrossfadeTargetStateParameter", "LongParameterList")
@Composable
fun OtherProfileScreenContent(
    scope: CoroutineScope,
    state: OtherUserProfileState,
    navigationIconType: NavigationIconType,
    requestInProgress: Boolean,
    sheetState: WireModalSheetState,
    openBottomSheet: () -> Unit,
    closeBottomSheet: () -> Unit,
    eventsHandler: OtherUserProfileEventsHandler,
    bottomSheetEventsHandler: OtherUserProfileBottomSheetEventsHandler,
    onIgnoreConnectionRequest: (String) -> Unit = { },
    onOpenConversation: (ConversationId) -> Unit = {},
    onOpenDeviceDetails: (Device) -> Unit = {},
    navigateBack: () -> Unit = {}
) {
    val otherUserProfileScreenState = rememberOtherUserProfileScreenState()
    val blockUserDialogState = rememberVisibilityState<BlockUserDialogState>()
    val unblockUserDialogState = rememberVisibilityState<UnblockUserDialogState>()
    val removeMemberDialogState = rememberVisibilityState<RemoveConversationMemberState>()
    val clearConversationDialogState = rememberVisibilityState<DialogState>()
    val getBottomSheetVisibility: () -> Boolean = remember(sheetState) { { sheetState.isVisible } }
    val bottomSheetState = remember { OtherUserBottomSheetState() }
    bottomSheetState.setContents(state.conversationSheetContent, state.groupState)
    val openConversationBottomSheet: () -> Unit = remember(bottomSheetState) {
        {
            bottomSheetState.toConversation()
            openBottomSheet()
        }
    }
    val openChangeRoleBottomSheet: () -> Unit = remember(bottomSheetState) {
        {
            bottomSheetState.toChangeRole()
            openBottomSheet()
        }
    }

    LaunchedEffect(bottomSheetState) {
        snapshotFlow { sheetState.isVisible }.collect(FlowCollector { isVisible ->
            // without clearing BottomSheet after every closing there could be strange UI behaviour.
            // Example: open some big BottomSheet (ConversationBS), close it, then open small BS (ChangeRoleBS) ->
            // in that case user will see ChangeRoleBS at the center of the screen (just for few milliseconds)
            // and then it moves to the bottom.
            // It happens cause when `sheetState.show()` is called, it calculates animation offset by the old BS height (which was big)
            // To avoid such case we clear BS content on every closing
            if (!isVisible) bottomSheetState.clearBottomSheetState()
        })
    }

    val tabItems by remember(state) {
        derivedStateOf {
            listOfNotNull(
                state.groupState?.let { OtherUserProfileTabItem.GROUP },
                OtherUserProfileTabItem.DETAILS,
                OtherUserProfileTabItem.DEVICES
            )
        }
    }
    val initialPage = 0
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { tabItems.size })
    val lazyListStates = OtherUserProfileTabItem.values().associateWith { rememberLazyListState() }
    val currentTabState by remember(state, pagerState) {
        derivedStateOf { if (state.isDataLoading) 0 else pagerState.calculateCurrentTab() }
    }
    val maxBarElevation = MaterialTheme.wireDimensions.topBarShadowElevation
    val tabBarElevationState by remember(tabItems, lazyListStates, currentTabState) {
        derivedStateOf { lazyListStates[tabItems[currentTabState]]?.topBarElevation(maxBarElevation) ?: 0.dp }
    }

    if (!requestInProgress) {
        blockUserDialogState.dismiss()
        unblockUserDialogState.dismiss()
        removeMemberDialogState.dismiss()
        clearConversationDialogState.dismiss()
    }

    CollapsingTopBarScaffold(
        topBarHeader = { elevation ->
            TopBarHeader(
                state = state,
                navigationIconType = navigationIconType,
                elevation = elevation,
                onNavigateBack = navigateBack,
                openConversationBottomSheet = openConversationBottomSheet
            )
        },
        topBarCollapsing = { TopBarCollapsing(state) },
        topBarFooter = { TopBarFooter(state, pagerState, tabBarElevationState, tabItems, currentTabState, scope) },
        content = {
            Content(
                state = state,
                pagerState = pagerState,
                tabItems = tabItems,
                otherUserProfileScreenState = otherUserProfileScreenState,
                lazyListStates = lazyListStates,
                openChangeRoleBottomSheet = openChangeRoleBottomSheet,
                openRemoveConversationMemberDialog = removeMemberDialogState::show,
                getOtherUserClients = eventsHandler::observeClientList,
                onDeviceClick = onOpenDeviceDetails
            )
        },
        bottomBar = {
            ContentFooter(
                state,
                maxBarElevation,
                onIgnoreConnectionRequest,
                onOpenConversation
            )
        },
        isSwipeable = state.connectionState == ConnectionState.ACCEPTED
    )

    WireModalSheetLayout(
        sheetState = sheetState,
        coroutineScope = scope,
        sheetContent = {
            OtherUserProfileBottomSheetContent(
                getBottomSheetVisibility = getBottomSheetVisibility,
                bottomSheetState = bottomSheetState,
                eventsHandler = bottomSheetEventsHandler,
                blockUser = blockUserDialogState::show,
                unblockUser = unblockUserDialogState::show,
                clearContent = clearConversationDialogState::show,
                closeBottomSheet = closeBottomSheet,
            )
        }
    )

    BlockUserDialogContent(
        dialogState = blockUserDialogState,
        onBlock = eventsHandler::onBlockUser,
        isLoading = requestInProgress,
    )
    UnblockUserDialogContent(
        dialogState = unblockUserDialogState,
        onUnblock = eventsHandler::onUnblockUser,
        isLoading = requestInProgress,
    )
    RemoveConversationMemberDialog(
        dialogState = removeMemberDialogState,
        onRemoveConversationMember = eventsHandler::onRemoveConversationMember,
        isLoading = requestInProgress,
    )
    ClearConversationContentDialog(
        dialogState = clearConversationDialogState,
        isLoading = requestInProgress,
        onClearConversationContent = {
            bottomSheetEventsHandler.onClearConversationContent(it)
        }
    )
}

@Composable
private fun TopBarHeader(
    state: OtherUserProfileState,
    navigationIconType: NavigationIconType,
    elevation: Dp,
    onNavigateBack: () -> Unit,
    openConversationBottomSheet: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onNavigateBack,
        navigationIconType = navigationIconType,
        title = stringResource(id = R.string.user_profile_title),
        elevation = elevation,
        actions = {
            if (state.conversationSheetContent != null) {
                MoreOptionIcon(
                    onButtonClicked = openConversationBottomSheet,
                    state = if (state.isMetadataEmpty()) WireButtonState.Disabled else WireButtonState.Default
                )
            }
        }
    )
}

@Composable
private fun TopBarCollapsing(state: OtherUserProfileState) {
    Crossfade(targetState = state, label = "OtherUserProfileScreenTopBarCollapsing") { targetState ->
        UserProfileInfo(
            userId = targetState.userId,
            isLoading = targetState.isAvatarLoading,
            avatarAsset = targetState.userAvatarAsset,
            fullName = targetState.fullName,
            userName = targetState.userName,
            teamName = targetState.teamName,
            membership = targetState.membership,
            editableState = EditableState.NotEditable,
            modifier = Modifier.padding(bottom = dimensions().spacing16x),
            connection = targetState.connectionState
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopBarFooter(
    state: OtherUserProfileState,
    pagerState: PagerState,
    tabBarElevation: Dp,
    tabItems: List<OtherUserProfileTabItem>,
    currentTab: Int,
    scope: CoroutineScope
) {
    if (state.connectionState == ConnectionState.ACCEPTED) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Content(
    state: OtherUserProfileState,
    pagerState: PagerState,
    tabItems: List<OtherUserProfileTabItem>,
    otherUserProfileScreenState: OtherUserProfileScreenState,
    lazyListStates: Map<OtherUserProfileTabItem, LazyListState>,
    openChangeRoleBottomSheet: () -> Unit,
    openRemoveConversationMemberDialog: (RemoveConversationMemberState) -> Unit,
    getOtherUserClients: () -> Unit,
    onDeviceClick: (Device) -> Unit
) {

    Crossfade(targetState = tabItems to state, label = "OtherUserProfile") { (tabItems, state) ->
        Column {
            if (!state.isDataLoading) {
                OtherUserConnectionStatusInfo(state.connectionState, state.membership)
            }
            when {
                state.isDataLoading || state.botService != null -> Box {} // no content visible while loading
                state.connectionState == ConnectionState.ACCEPTED -> {
                    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                        HorizontalPager(
                            modifier = Modifier.fillMaxSize(),
                            state = pagerState,
                        ) { pageIndex ->
                            when (val tabItem = tabItems[pageIndex]) {
                                OtherUserProfileTabItem.DETAILS ->
                                    OtherUserProfileDetails(state, otherUserProfileScreenState, lazyListStates[tabItem]!!)

                                OtherUserProfileTabItem.GROUP ->
                                    OtherUserProfileGroup(
                                        state,
                                        lazyListStates[tabItem]!!,
                                        openRemoveConversationMemberDialog,
                                        openChangeRoleBottomSheet
                                    )

                                OtherUserProfileTabItem.DEVICES -> {
                                    getOtherUserClients()
                                    OtherUserDevicesScreen(
                                        lazyListState = lazyListStates[tabItem]!!,
                                        state = state,
                                        onDeviceClick = onDeviceClick
                                    )
                                }
                            }
                        }
                    }
                }

                state.groupState != null -> {
                    OtherUserProfileGroup(
                        state,
                        lazyListStates[OtherUserProfileTabItem.DETAILS]!!,
                        openRemoveConversationMemberDialog,
                        openChangeRoleBottomSheet
                    )
                }

                else -> Box {}
            }
        }
    }
}

@Composable
private fun ContentFooter(
    state: OtherUserProfileState,
    maxBarElevation: Dp,
    onIgnoreConnectionRequest: (String) -> Unit = {},
    onOpenConversation: (ConversationId) -> Unit = {}
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
                if (!state.isMetadataEmpty() && state.membership != Membership.Service) {
                    ConnectionActionButton(
                        state.userId,
                        state.userName,
                        state.connectionState,
                        onIgnoreConnectionRequest,
                        onOpenConversation
                    )
                }
            }
        }
    }
}

enum class OtherUserProfileTabItem(@StringRes override val titleResId: Int) : TabItem {
    GROUP(R.string.user_profile_group_tab),
    DETAILS(R.string.user_profile_details_tab),
    DEVICES(R.string.user_profile_devices_tab);
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(name = "Connected")
fun PreviewOtherProfileScreenContent() {
    WireTheme(isPreview = true) {
        OtherProfileScreenContent(
            rememberCoroutineScope(),
            OtherUserProfileState.PREVIEW.copy(connectionState = ConnectionState.ACCEPTED),
            NavigationIconType.Back,
            false,
            rememberWireModalSheetState(),
            {}, {}, OtherUserProfileEventsHandler.PREVIEW,
            OtherUserProfileBottomSheetEventsHandler.PREVIEW
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(name = "Not Connected")
fun PreviewOtherProfileScreenContentNotConnected() {
    WireTheme(isPreview = true) {
        OtherProfileScreenContent(
            rememberCoroutineScope(),
            OtherUserProfileState.PREVIEW.copy(connectionState = ConnectionState.CANCELLED),
            NavigationIconType.Back,
            false,
            rememberWireModalSheetState(),
            {}, {}, OtherUserProfileEventsHandler.PREVIEW,
            OtherUserProfileBottomSheetEventsHandler.PREVIEW,
        )
    }
}
