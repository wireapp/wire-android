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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.wire.android.R
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.dialogs.BlockUserDialogContent
import com.wire.android.ui.common.dialogs.UnblockUserDialogContent
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.userprofile.common.EditableState
import com.wire.android.ui.userprofile.common.UserProfileInfo
import com.wire.kalium.logic.data.user.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class, InternalCoroutinesApi::class)
@Composable
fun OtherUserProfileScreen(viewModel: OtherUserProfileScreenViewModel = hiltViewModel()) {
    val screenState = rememberOtherUserProfileScreenState()

    OtherProfileScreenContent(
        screenState = screenState,
        viewModelState = viewModel.state,
        eventsHandler = viewModel as OtherUserProfileEventsHandler,
        footerEventsHandler = viewModel as OtherUserProfileFooterEventsHandler
    )

    if (!viewModel.requestInProgress) {
        screenState.dismissDialogs()
    }

    LaunchedEffect(Unit) {
        viewModel.infoMessage.collect {
            screenState.closeBottomSheet()
            screenState.showSnackbar(it)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { screenState.sheetState.isVisible }.collect { isVisible ->
            // without clearing BottomSheet after every closing there could be strange UI behaviour.
            // Example: open some big BottomSheet (ConversationBS), close it, then open small BS (ChangeRoleBS) ->
            // in that case user will see ChangeRoleBS at the center of the screen (just for few milliseconds)
            // and then it moves to the bottom.
            // It happens cause when `sheetState.show()` is called, it calculates animation offset by the old BS height (which was big)
            // To avoid such case we clear BS content on every closing
//            if (!isVisible) viewModel.clearBottomSheetState()
        }
    }

}

@SuppressLint("UnusedCrossfadeTargetStateParameter", "LongParameterList")
@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalPagerApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
fun OtherProfileScreenContent(
    screenState: OtherUserProfileScreenState,
    viewModelState: OtherUserProfileState,
    eventsHandler: OtherUserProfileEventsHandler,
    footerEventsHandler: OtherUserProfileFooterEventsHandler
) {
    with(viewModelState) {
        with(screenState) {
            val screenContent = @Composable {
                val otherProfilePagerState = rememberOtherUserProfilePagerState(
                    showGroupOption = groupInfoAvailiblity is GroupInfoAvailibility.Available
                )

                CollapsingTopBarScaffold(
                    snackbarHost = {
                        SwipeDismissSnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    topBarHeader = { elevation ->
                        TopBarHeader(
                            state = viewModelState,
                            elevation = elevation,
                            onNavigateBack = eventsHandler::navigateBack,
                            openConversationBottomSheet = {
                                bottomSheetEventsHandler.setBottomSheetStateToConversation()
                                openBottomSheet()
                            })
                    },
                    topBarCollapsing = { TopBarCollapsing(viewModelState) },
                    topBarFooter = {
                        with(otherProfilePagerState) {
                            TopBarFooter(
                                state = viewModelState,
                                pagerState = pagerState,
                                tabBarElevation = tabBarElevationState,
                                tabItems = tabItems,
                                currentTab = currentTabState,
                                scope = coroutineScope
                            )
                        }
                    },
                    content = {
                        with(otherProfilePagerState) {
                            Content(
                                state = viewModelState,
                                pagerState = pagerState,
                                tabItems = tabItems,
                                details = {
                                    OtherUserProfileDetails(
                                        lazyListState = tabItemsLazyListState[OtherUserProfileTabItem.DETAILS]!!,
                                        email = email,
                                        phoneNumber = phone,
                                        onCopy = ::copy
                                    )
                                },
                                group = {
                                    OtherUserProfileGroup(
                                        state = viewModelState,
                                        lazyListState = tabItemsLazyListState[OtherUserProfileTabItem.GROUP]!!,
                                        onRemoveFromConversation = removeMemberDialogState::show,
                                        openChangeRoleBottomSheet = {
                                            eventsHandler.setBottomSheetStateToChangeRole()
                                            openBottomSheet()
                                        }
                                    )
                                },
                                devices = {
                                    LaunchedEffect(Unit) {
                                        eventsHandler.fetchOtherUserClients()
                                    }

                                    OtherUserDevicesScreen(
                                        lazyListState = tabItemsLazyListState[OtherUserProfileTabItem.DEVICES]!!,
                                        fullName = fullName,
                                        otherUserClients = otherUserClients
                                    )
                                }
                            )
                        }
                    },
                    bottomBar = {
                        ContentFooter(
                            viewModelState,
                            otherProfilePagerState.topBarMaxBarElevation,
                            footerEventsHandler,
                            unblockUserDialogState::show
                        )
                    },
                    isSwipeable = connectionState == ConnectionState.ACCEPTED
                )
            }

            if (connectionState in listOf(ConnectionState.ACCEPTED, ConnectionState.BLOCKED)) {
                WireModalSheetLayout(
                    sheetState = sheetState,
                    coroutineScope = coroutineScope,
                    sheetContent = {
                        OtherUserProfileBottomSheetContent(

                        )
                    }
                ) {
                    screenContent()
                }
            } else {
                screenContent()
            }

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
        }
    }
}

@Composable
private fun TopBarHeader(
    state: OtherUserProfileState,
    elevation: Dp,
    onNavigateBack: () -> Unit,
    openConversationBottomSheet: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onNavigateBack,
        title = stringResource(id = R.string.user_profile_title),
        elevation = elevation,
        actions = {
            if (state.connectionState in listOf(ConnectionState.ACCEPTED, ConnectionState.BLOCKED)) {
                MoreOptionIcon(openConversationBottomSheet)
            }
        }
    )
}

@Composable
private fun TopBarCollapsing(state: OtherUserProfileState) {
    Crossfade(targetState = state.isLoading) { isLoading ->
        UserProfileInfo(
            isLoading = isLoading,
            avatarAsset = state.userAvatarAsset,
            fullName = state.fullName,
            userName = state.userName,
            teamName = state.teamName,
            membership = state.membership,
            editableState = EditableState.NotEditable,
            modifier = Modifier.padding(bottom = dimensions().spacing16x),
            connection = state.connectionState
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
    if (state.connectionState == ConnectionState.ACCEPTED) {
        AnimatedVisibility(
            visible = !state.isLoading,
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
    details: @Composable () -> Unit,
    group: @Composable () -> Unit,
    devices: @Composable () -> Unit
) {
    Crossfade(targetState = tabItems to state) { (tabItems, state) ->
        when {
            state.isLoading || state.botService != null -> Box {} // no content visible while loading
            state.connectionState == ConnectionState.ACCEPTED ->
                CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                    HorizontalPager(
                        modifier = Modifier.fillMaxSize(),
                        state = pagerState,
                        count = tabItems.size
                    ) { pageIndex ->
                        when (tabItems[pageIndex]) {
                            OtherUserProfileTabItem.DETAILS -> {
                                details()
                            }
                            OtherUserProfileTabItem.GROUP -> {
                                group()
                            }
                            OtherUserProfileTabItem.DEVICES -> {
                                devices()
                            }
                        }
                    }
                }
            state.connectionState == ConnectionState.BLOCKED -> Box {} // no content visible for blocked users
            else -> {
                OtherUserConnectionStatusInfo(state.connectionState, state.membership)
            }
        }
    }
}

@Composable
private fun ContentFooter(
    state: OtherUserProfileState,
    maxBarElevation: Dp,
    footerEventsHandler: OtherUserProfileFooterEventsHandler,
    onUnblockUser: (UnblockUserDialogState) -> Unit
) {
    AnimatedVisibility(
        visible = !state.isLoading,
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
                        state.connectionState,
                        footerEventsHandler::onSendConnectionRequest,
                        footerEventsHandler::onOpenConversation,
                        footerEventsHandler::onCancelConnectionRequest,
                        footerEventsHandler::onAcceptConnectionRequest,
                        footerEventsHandler::onIgnoreConnectionRequest
                    ) { onUnblockUser(UnblockUserDialogState(state.userName, state.userId)) }
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
//
//@OptIn(ExperimentalMaterialApi::class)
//@Composable
//@Preview(name = "Connected")
//fun OtherProfileScreenContentPreview() {
//    WireTheme(isPreview = true) {
//        OtherProfileScreenContent(
//            rememberCoroutineScope(),
//            OtherUserProfileState.PREVIEW.copy(connectionState = ConnectionState.ACCEPTED), false,
//            rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
//            {}, {}, OtherUserProfileEventsHandler.PREVIEW,
//            OtherUserProfileFooterEventsHandler.PREVIEW, OtherUserProfileBottomSheetEventsHandler.PREVIEW
//        )
//    }
//}
//
//@OptIn(ExperimentalMaterialApi::class)
//@Composable
//@Preview(name = "Not Connected")
//fun OtherProfileScreenContentNotConnectedPreview() {
//    WireTheme(isPreview = true) {
//        OtherProfileScreenContent(
//            rememberCoroutineScope(),
//            OtherUserProfileState.PREVIEW.copy(connectionState = ConnectionState.CANCELLED), false,
//            rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
//            {}, {}, OtherUserProfileEventsHandler.PREVIEW,
//            OtherUserProfileFooterEventsHandler.PREVIEW, OtherUserProfileBottomSheetEventsHandler.PREVIEW
//        )
//    }
//}
