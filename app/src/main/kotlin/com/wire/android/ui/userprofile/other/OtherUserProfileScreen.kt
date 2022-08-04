package com.wire.android.ui.userprofile.other

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalOverScrollConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.dialogs.BlockUserDialogContent
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topBarElevation
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.userprofile.common.EditableState
import com.wire.android.ui.userprofile.common.UserProfileInfo
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class,
    ExperimentalPagerApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun OtherUserProfileScreen(viewModel: OtherUserProfileScreenViewModel = hiltViewModel()) {
    OtherProfileScreenContent(
        state = viewModel.state,
        snackBarState = viewModel.snackBarState,
        clearSnackBarState = viewModel::clearSnackBarState,
        onSendConnectionRequest = viewModel::sendConnectionRequest,
        onOpenConversation = viewModel::openConversation,
        onCancelConnectionRequest = viewModel::cancelConnectionRequest,
        ignoreConnectionRequest = viewModel::ignoreConnectionRequest,
        acceptConnectionRequest = viewModel::acceptConnectionRequest,
        onNavigateBack = viewModel::navigateBack,
        onDismissBlockUserDialog = viewModel::onDismissBlockUserDialog,
        onBlockUserClicked = viewModel::onBlockUserClicked,
        onBlockUser = viewModel::blockUser,
        onMutingConversationStatusChange = viewModel::muteConversation,
        addConversationToFavourites = viewModel::addConversationToFavourites,
        moveConversationToArchive = viewModel::moveConversationToArchive,
        clearConversationContent = viewModel::clearConversationContent,
        moveConversationToFolder = viewModel::moveConversationToFolder,
    )
}

@SuppressLint("UnusedCrossfadeTargetStateParameter", "LongParameterList")
@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class,
    ExperimentalPagerApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun OtherProfileScreenContent(
    state: OtherUserProfileState,
    snackBarState: SnackBarState?,
    clearSnackBarState: () -> Unit,
    onSendConnectionRequest: () -> Unit,
    onOpenConversation: () -> Unit,
    onCancelConnectionRequest: () -> Unit,
    acceptConnectionRequest: () -> Unit,
    ignoreConnectionRequest: () -> Unit,
    onNavigateBack: () -> Unit,
    onDismissBlockUserDialog: () -> Unit,
    onBlockUserClicked: (UserId, String) -> Unit,
    onBlockUser: (UserId, String) -> Unit,
    onMutingConversationStatusChange: (ConversationId?, MutedConversationStatus) -> Unit,
    addConversationToFavourites: () -> Unit,
    moveConversationToFolder: () -> Unit,
    moveConversationToArchive: () -> Unit,
    clearConversationContent: () -> Unit,
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

    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

    handleOperationMessages(snackbarHostState, snackBarState, sheetState::hide, clearSnackBarState)

    val sheetContent = getBottomSheetContent(
        bottomSheetState = state.bottomSheetState,
        onMutingConversationStatusChange = onMutingConversationStatusChange,
        addConversationToFavourites = addConversationToFavourites,
        moveConversationToArchive = moveConversationToArchive,
        clearConversationContent = clearConversationContent,
        moveConversationToFolder = moveConversationToFolder,
        blockUser = onBlockUserClicked
    )

    Surface {
        WireModalSheetLayout(
            sheetState = sheetState,
            coroutineScope = scope,
            sheetContent = sheetContent
        ) {
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
                            if (state.connectionStatus in listOf(ConnectionState.ACCEPTED, ConnectionState.BLOCKED)) {
                                MoreOptionIcon({
                                    state.setBottomSheetContentToConversation()
                                    scope.launch { sheetState.show() }
                                })
                            }
                        }
                    )
                },
                topBarCollapsing = {
                    Crossfade(targetState = state.isDataLoading) {
                        UserProfileInfo(
                            connection = state.connectionStatus,
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
                    if (state.connectionStatus == ConnectionState.ACCEPTED) {
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
                            state.isDataLoading -> Box {} // no content visible while loading
                            state.connectionStatus == ConnectionState.ACCEPTED ->
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
                                                OtherUserProfileGroup(state.groupState!!, lazyListStates[tabItem]!!)
                                        }
                                    }
                                }
                            state.connectionStatus == ConnectionState.BLOCKED -> {
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
                }, isSwipeable = state.connectionStatus == ConnectionState.ACCEPTED
            )
        }

        BlockUserDialogContent(
            state = state.blockUserDialogSate,
            dismiss = onDismissBlockUserDialog,
            onBlock = onBlockUser
        )
    }
}

enum class OtherUserProfileTabItem(@StringRes override val titleResId: Int) : TabItem {
    GROUP(R.string.user_profile_group_tab),
    DETAILS(R.string.user_profile_details_tab);
}

@Composable
private fun handleOperationMessages(
    snackbarHostState: SnackbarHostState,
    operationState: SnackBarState?,
    closeBottomSheet: suspend () -> Unit,
    onMessageShown: () -> Unit
) {
    operationState?.let { errorType ->
        val message = when (errorType) {
            is SnackBarState.ConnectionRequestError -> stringResource(id = R.string.connection_request_sent_error)
            is SnackBarState.SuccessConnectionSentRequest -> stringResource(id = R.string.connection_request_sent)
            is SnackBarState.LoadUserInformationError -> stringResource(id = R.string.error_unknown_message)
            is SnackBarState.SuccessConnectionAcceptRequest -> stringResource(id = R.string.connection_request_accepted)
            is SnackBarState.SuccessConnectionCancelRequest -> stringResource(id = R.string.connection_request_canceled)
            is SnackBarState.BlockingUserOperationError -> stringResource(id = R.string.error_blocking_user)
            is SnackBarState.BlockingUserOperationSuccess -> stringResource(id = R.string.blocking_user_success, errorType.name)
            is SnackBarState.MutingOperationError -> stringResource(id = R.string.error_updating_muting_setting)
        }
        LaunchedEffect(errorType) {
            closeBottomSheet()
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }
}

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class,
    ExperimentalPagerApi::class,
    ExperimentalFoundationApi::class
)
@Composable
@Preview(name = "Connected")
fun OtherProfileScreenContentPreview() {
    WireTheme(isPreview = true) {
        OtherProfileScreenContent(
            OtherUserProfileState.PREVIEW.copy(connectionStatus = ConnectionState.ACCEPTED), null,
            {}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, { _, _ -> }, { _, _ -> }, {}, {}, {}, {}
        )
    }
}

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class,
    ExperimentalPagerApi::class,
    ExperimentalFoundationApi::class
)
@Composable
@Preview(name = "Not Connected")
fun OtherProfileScreenContentNotConnectedPreview() {
    WireTheme(isPreview = true) {
        OtherProfileScreenContent(
            OtherUserProfileState.PREVIEW.copy(connectionStatus = ConnectionState.CANCELLED), null,
            {}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, { _, _ -> }, { _, _ -> }, {}, {}, {}, {}
        )
    }
}
