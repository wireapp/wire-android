package com.wire.android.ui.home

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wire.android.R
import com.wire.android.navigation.HomeNavigationItem
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.hiltSavedStateViewModel
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
import com.wire.android.ui.home.conversationslist.ConversationListState
import com.wire.android.ui.home.conversationslist.ConversationListViewModel
import com.wire.android.ui.home.sync.FeatureFlagNotificationViewModel
import com.wire.android.util.CustomTabsHelper
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun HomeScreen(
    backNavArgs: ImmutableMap<String, Any> = persistentMapOf(),
    homeViewModel: HomeViewModel = hiltSavedStateViewModel(backNavArgs = backNavArgs),
    featureFlagNotificationViewModel: FeatureFlagNotificationViewModel = hiltViewModel(),
    commonTopAppBarViewModel: CommonTopAppBarViewModel = hiltViewModel(),
    conversationListViewModel: ConversationListViewModel = hiltViewModel(), // TODO: move required elements from this one to HomeViewModel?
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

    val homeState = homeViewModel.homeState
    handleWelcomeNewUserDialog(
        homeState = homeViewModel.homeState,
        dismissDialog = homeViewModel::dismissWelcomeMessage
    )
    handleFeatureFlagChangedNotification(
        featureFlagState = featureFlagNotificationViewModel.featureFlagState,
        hideDialogStatus = featureFlagNotificationViewModel::hideDialogStatus
    )

    HomeContent(
        connectivityState = commonTopAppBarViewModel.connectivityState,
        homeState = homeState,
        homeStateHolder = homeScreenState,
        conversationListState = conversationListViewModel.conversationListState,
        onReturnToCallClick = commonTopAppBarViewModel::openOngoingCallScreen,
        onNewConversationClick = conversationListViewModel::openNewConversation,
        onSelfUserClick = homeViewModel::navigateToSelfUserProfile,
        navigateToItem = homeViewModel::navigateTo
    )

    BackHandler(homeScreenState.searchBarState.isSearchActive) {
        homeScreenState.searchBarState.closeSearch()
    }
}

@Composable
fun handleWelcomeNewUserDialog(
    homeState: HomeState,
    dismissDialog: () -> Unit,
    context: Context = LocalContext.current
) {
    if (homeState.shouldDisplayWelcomeMessage) {
        val welcomeToNewAndroidUrl = stringResource(id = R.string.url_welcome_to_new_android)
        WireDialog(
            title = stringResource(id = R.string.welcome_migration_dialog_title),
            text = stringResource(id = R.string.welcome_migration_dialog_content),
            onDismiss = dismissDialog,
            optionButton1Properties = WireDialogButtonProperties(
                onClick = {
                    dismissDialog.invoke()
                    CustomTabsHelper.launchUrl(context, welcomeToNewAndroidUrl)
                },
                text = stringResource(id = R.string.label_learn_more),
                type = WireDialogButtonType.Primary,
            ),
            optionButton2Properties = WireDialogButtonProperties(
                onClick = { /* todo: hide and persist flag */ },
                text = stringResource(id = R.string.welcome_migration_dialog_continue),
                type = WireDialogButtonType.Primary,
            )
        )
    }
}

@Composable
private fun handleFeatureFlagChangedNotification(
    featureFlagState: FeatureFlagState,
    hideDialogStatus: () -> Unit,
) {
    if (featureFlagState.showFileSharingDialog) {
        val text: String = if (featureFlagState.isFileSharingEnabledState) {
            stringResource(id = R.string.sharing_files_enabled)
        } else {
            stringResource(id = R.string.sharing_files_disabled)
        }

        WireDialog(
            title = stringResource(id = R.string.team_settings_changed),
            text = text,
            onDismiss = hideDialogStatus,
            optionButton1Properties = WireDialogButtonProperties(
                onClick = hideDialogStatus,
                text = stringResource(id = R.string.label_ok),
                type = WireDialogButtonType.Primary,
            )
        )
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeContent(
    connectivityState: ConnectivityUIState,
    homeState: HomeState,
    homeStateHolder: HomeStateHolder,
    conversationListState: ConversationListState,
    onReturnToCallClick: () -> Unit,
    onNewConversationClick: () -> Unit,
    onSelfUserClick: () -> Unit,
    navigateToItem: (NavigationItem) -> Unit
) {
    with(homeStateHolder) {
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
                    navigateToHomeItem = ::navigateTo,
                    navigateToItem = navigateToItem,
                    onCloseDrawer = ::closeDrawer
                )
            },
            gesturesEnabled = drawerState.isOpen,
            content = {
                with(currentNavigationItem) {
                    WireModalSheetLayout(
                        sheetState = bottomSheetState,
                        coroutineScope = coroutineScope,
                        // we want to render "nothing" instead of doing a if/else check
                        // on homeBottomSheetContent and wrap homeContent() into WireModalSheetLayout
                        // or render it without WireModalSheetLayout to avoid
                        // recomposing the homeContent() when homeBottomSheetContent
                        // changes from null to "something"
                        sheetContent = homeBottomSheetContent ?: { }
                    ) {
                        CollapsingTopBarScaffold(
                            snapOnFling = false,
                            keepElevationWhenCollapsed = true,
                            topBarHeader = { elevation ->
                                Column(modifier = Modifier.animateContentSize()) {
                                    CommonTopAppBar(
                                        connectivityUIState = connectivityState,
                                        onReturnToCallClick = onReturnToCallClick
                                    )
                                    AnimatedVisibility(visible = !searchBarState.isSearchActive) {
                                        HomeTopBar(
                                            avatarAsset = homeState.avatarAsset,
                                            status = homeState.status,
                                            title = stringResource(currentNavigationItem.title),
                                            elevation = elevation,
                                            onHamburgerMenuClick = ::openDrawer,
                                            onNavigateToSelfUserProfile = onSelfUserClick
                                        )
                                    }
                                }
                            },
                            snackbarHost = {
                                SwipeDismissSnackbarHost(
                                    hostState = snackBarHostState,
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
                                        searchQuery = searchBarState.searchQuery,
                                        onSearchQueryChanged = searchBarState::searchQueryChanged,
                                        onInputClicked = searchBarState::openSearch,
                                        onCloseSearchClicked = searchBarState::closeSearch,
                                    )
                            },
                            content = {
                                NavHost(
                                    navController = navController,
                                    // For now we only support Conversations screen
                                    startDestination = HomeNavigationItem.Conversations.route
                                ) {
                                    HomeNavigationItem.values()
                                        .forEach { item ->
                                            composable(
                                                route = item.route,
                                                content = item.content(homeStateHolder)
                                            )
                                        }
                                }
                            },
                            floatingActionButton = {
                                AnimatedVisibility(
                                    visible = currentNavigationItem.withNewConversationFab && !searchBarState.isSearchActive,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
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
                                            conversationListState = conversationListState
                                        ),
                                        navController = navController
                                    )
                                }
                            }
                        )
                    }
                }
            }
        )
    }
}

@Suppress("ComplexMethod")
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
            HomeSnackbarState.UnblockingUserOperationError -> stringResource(id = R.string.error_unblocking_user)
            HomeSnackbarState.None -> ""
            is HomeSnackbarState.DeletedConversationGroupSuccess ->
                stringResource(id = R.string.conversation_group_removed_success, messageType.groupName)

            HomeSnackbarState.LeftConversationSuccess -> stringResource(id = R.string.left_conversation_group_success)
            HomeSnackbarState.LeaveConversationError -> stringResource(id = R.string.leave_group_conversation_error)
            HomeSnackbarState.DeleteConversationGroupError -> stringResource(id = R.string.delete_group_conversation_error)
            is HomeSnackbarState.ClearConversationContentFailure -> stringResource(
                if (messageType.isGroup)
                    R.string.group_content_delete_failure else
                    R.string.conversation_content_delete_failure
            )

            is HomeSnackbarState.ClearConversationContentSuccess -> stringResource(
                if (messageType.isGroup)
                    R.string.group_content_deleted else
                    R.string.conversation_content_deleted
            )
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
    conversationListState: ConversationListState
): List<WireBottomNavigationItemData> = map { homeNavigationItem ->
    when (homeNavigationItem) {
        HomeNavigationItem.Conversations -> homeNavigationItem.toBottomNavigationItemData(conversationListState.newActivityCount)
        HomeNavigationItem.Calls -> homeNavigationItem.toBottomNavigationItemData(conversationListState.missedCallsCount)
        HomeNavigationItem.Mentions -> homeNavigationItem.toBottomNavigationItemData(conversationListState.unreadMentionsCount)
        else -> homeNavigationItem.toBottomNavigationItemData(0L)
    }
}
