package com.wire.android.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalDrawer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wire.android.R
import com.wire.android.navigation.HomeNavigationItem
import com.wire.android.navigation.NavigationItem
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.FloatingActionButton
import com.wire.android.ui.common.WireBottomNavigationBar
import com.wire.android.ui.common.WireBottomNavigationItemData
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topappbar.CommonTopAppBar
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModel
import com.wire.android.ui.common.topappbar.ConnectivityUIState
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.common.topappbar.search.rememberSearchbarState
import com.wire.android.ui.home.conversationslist.ConversationListState
import com.wire.android.ui.home.conversationslist.ConversationListViewModel
import com.wire.android.ui.home.sync.FeatureFlagNotificationViewModel

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    featureFlagNotificationViewModel: FeatureFlagNotificationViewModel,
    commonTopAppBarViewModel: CommonTopAppBarViewModel,
    conversationListViewModel: ConversationListViewModel, // TODO: move required elements from this one to HomeViewModel?
) {
    homeViewModel.checkRequirements()

    val homeScreenState = rememberHomeScreenState()

    LaunchedEffect(homeViewModel.savedStateHandle) {
        homeViewModel.checkPendingSnackbarState()?.let(homeScreenState::setSnackBarState)
    }

    handleSnackBarMessage(
        snackbarHostState = homeScreenState.snackBarHostState,
        conversationListSnackBarState = homeScreenState.snackbarState,
        onMessageShown = homeScreenState::clearSnackbarMessage
    )

    val featureFlagState = featureFlagNotificationViewModel.featureFlagState

    if (featureFlagState.showFileSharingDialog) {
        val text: String = if (featureFlagState.isFileSharingEnabledState) {
            stringResource(id = R.string.sharing_files_enabled)
        } else {
            stringResource(id = R.string.sharing_files_disabled)
        }

        WireDialog(
            title = stringResource(id = R.string.team_settings_changed),
            text = text,
            onDismiss = { featureFlagNotificationViewModel.hideDialogStatus() },
            optionButton1Properties = WireDialogButtonProperties(
                onClick = { featureFlagNotificationViewModel.hideDialogStatus() },
                text = stringResource(id = R.string.label_ok),
                type = WireDialogButtonType.Primary,
            )
        )
    }

    HomeContent(
        currentNavigationItem = homeScreenState.currentNavigationItem,
        connectivityState = commonTopAppBarViewModel.connectivityState,
        homeState = homeViewModel.homeState,
        homeScreenState = homeScreenState,
        onReturnToCallClick = commonTopAppBarViewModel::openOngoingCallScreen,
        onNewConversationClick = conversationListViewModel::openNewConversation,
        onSelfUserClick = homeViewModel::navigateToSelfUserProfile,
        onHamburgerMenuClick = homeScreenState::openDrawer,
        navigateToItem = homeViewModel::navigateTo
    )
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeContent(
    currentNavigationItem: HomeNavigationItem,
    connectivityState: ConnectivityUIState,
    homeState: HomeState,
    homeScreenState: HomeScreenState,
    onHamburgerMenuClick: () -> Unit,
    onReturnToCallClick: () -> Unit,
    onNewConversationClick: () -> Unit,
    onSelfUserClick: () -> Unit,
    navigateToItem: (NavigationItem) -> Unit
) {
    val searchBarState = rememberSearchbarState()

    with(homeScreenState) {
        ModalDrawer(
            drawerBackgroundColor = MaterialTheme.colorScheme.surface,
            drawerElevation = 0.dp,
            drawerShape = RectangleShape,
            drawerState = drawerState,
            drawerContent = {
                HomeDrawer(
                    //TODO: logFilePath does not belong in the UI logic
                    logFilePath = homeState.logFilePath,
                    currentRoute = currentNavigationItem.route,
                    navigateToHomeItem = homeScreenState::navigateTo,
                    navigateToItem = navigateToItem,
                    onCloseDrawer = homeScreenState::closeDrawer
                )
            },
            gesturesEnabled = drawerState.isOpen,
            content = {
                Crossfade(searchBarState.isSearchActive) { isSearchActive ->
                    with(currentNavigationItem) {
                        WireModalSheetLayout(
                            sheetState = homeScreenState.bottomSheetState,
                            coroutineScope = rememberCoroutineScope(),
                            // we want to render "nothing" instead of doing a if/else check
                            // on homeBottomSheetContent and wrap homeContent() into WireModalSheetLayout
                            // or render it without WireModalSheetLayout to avoid
                            // recomposing the homeContent() when homeBottomSheetContent
                            // changes from null to "something"
                            sheetContent = homeScreenState.homeBottomSheetContent ?: { }
                        ) {
                            CollapsingTopBarScaffold(
                                snapOnFling = false,
                                keepElevationWhenCollapsed = true,
                                topBarHeader = { elevation ->
                                    if (!isSearchActive) {
                                        Column {
                                            CommonTopAppBar(
                                                connectivityUIState = connectivityState,
                                                onReturnToCallClick = onReturnToCallClick
                                            )
                                            HomeTopBar(
                                                avatarAsset = homeState.avatarAsset,
                                                status = homeState.status,
                                                title = stringResource(id = homeScreenState.currentNavigationItem.title),
                                                elevation = elevation,
                                                onHamburgerMenuClick = onHamburgerMenuClick,
                                                onNavigateToSelfUserProfile = onSelfUserClick
                                            )
                                        }
                                    }
                                },
                                snackbarHost = {
                                    SwipeDismissSnackbarHost(
                                        hostState = homeScreenState.snackBarHostState,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                topBarCollapsing = {
                                    if (currentNavigationItem.isSearchable)
                                        SearchTopBar(
                                            isSearchActive = searchBarState.isSearchActive,
                                            searchBarHint = stringResource(
                                                R.string.search_bar_hint,
                                                stringResource(id = title).lowercase()
                                            ),
                                            searchQuery = TextFieldValue(""), // TODO
                                            onSearchQueryChanged = { /* TODO */ },
                                            onInputClicked = { searchBarState.openSearch() },
                                            onCloseSearchClicked = { searchBarState.closeSearch() },
                                        )
                                },
                                content = {
                                    NavHost(
                                        navController = homeScreenState.navController,
                                        // For now we only support Conversations screen
                                        startDestination = HomeNavigationItem.Conversations.route
                                    ) {
                                        HomeNavigationItem.values()
                                            .forEach { item ->
                                                composable(
                                                    route = item.route,
                                                    content = item.content(homeScreenState)
                                                )
                                            }
                                    }
                                },
                                floatingActionButton = {
                                    if (currentNavigationItem.withNewConversationFab && !isSearchActive) {
                                        FloatingActionButton(
                                            text = stringResource(R.string.label_new),
                                            icon = {
                                                Image(
                                                    painter = painterResource(id = R.drawable.ic_conversation),
                                                    contentDescription = stringResource(R.string.content_description_new_conversation),
                                                    contentScale = ContentScale.FillBounds,
                                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                                                    modifier = Modifier
                                                        .padding(start = dimensions().spacing4x, top = dimensions().spacing2x)
                                                        .size(dimensions().fabIconSize)
                                                )
                                            },
                                            onClick = onNewConversationClick
                                        )
                                    }
                                },
                                bottomBar = {
                                    AnimatedVisibility(
                                        visible = currentNavigationItem.withBottomTabs,
                                        enter = slideInVertically(initialOffsetY = { it }),
                                        exit = slideOutVertically(targetOffsetY = { it }),
                                    ) {
                                        WireBottomNavigationBar(
                                            items = HomeNavigationItem.bottomTabItems.toBottomNavigationItems(

                                            ),
                                            navController = homeScreenState.navController
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun handleSnackBarMessage(
    snackbarHostState: SnackbarHostState,
    conversationListSnackBarState: HomeSnackbarState,
    onMessageShown: () -> Unit
) {
    conversationListSnackBarState.let { messageType ->
        val message = when (messageType) {
            is HomeSnackbarState.SuccessConnectionIgnoreRequest ->
                stringResource(id = R.string.connection_request_ignored, messageType.userName)
            is HomeSnackbarState.BlockingUserOperationSuccess ->
                stringResource(id = R.string.blocking_user_success, messageType.userName)
            HomeSnackbarState.MutingOperationError -> stringResource(id = R.string.error_updating_muting_setting)
            HomeSnackbarState.BlockingUserOperationError -> stringResource(id = R.string.error_blocking_user)
            HomeSnackbarState.None -> ""
            is HomeSnackbarState.DeletedConversationGroupSuccess ->
                stringResource(id = R.string.conversation_group_removed_success, messageType.groupName)
            HomeSnackbarState.LeftConversationSuccess -> stringResource(id = R.string.left_conversation_group_success)
            HomeSnackbarState.LeaveConversationError -> stringResource(id = R.string.leave_group_conversation_error)
            HomeSnackbarState.DeleteConversationGroupError -> stringResource(id = R.string.delete_group_conversation_error)
        }
        LaunchedEffect(messageType) {
            if (messageType != HomeSnackbarState.None) {
                snackbarHostState.showSnackbar(message)
                onMessageShown()
            }
        }
    }
}

@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@Composable
private fun List<HomeNavigationItem>.toBottomNavigationItems(
    ConversationListState: ConversationListState
): List<WireBottomNavigationItemData> = map { homeNavigationItem ->
    when (homeNavigationItem) {
        HomeNavigationItem.Conversations -> homeNavigationItem.toBottomNavigationItemData(ConversationListState.newActivityCount)
        HomeNavigationItem.Calls -> homeNavigationItem.toBottomNavigationItemData(ConversationListState.missedCallsCount)
        HomeNavigationItem.Mentions -> homeNavigationItem.toBottomNavigationItemData(ConversationListState.unreadMentionsCount)
        else -> homeNavigationItem.toBottomNavigationItemData(0L)
    }
}
