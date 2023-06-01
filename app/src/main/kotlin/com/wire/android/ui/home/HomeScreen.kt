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

package com.wire.android.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
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
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.home.conversationslist.ConversationListState
import com.wire.android.ui.home.conversationslist.ConversationListViewModel
import com.wire.android.ui.home.sync.FeatureFlagNotificationViewModel
import com.wire.android.util.permission.rememberRequestPushNotificationsPermissionFlow
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    backNavArgs: ImmutableMap<String, Any> = persistentMapOf(),
    homeViewModel: HomeViewModel = hiltSavedStateViewModel(backNavArgs = backNavArgs),
    featureFlagNotificationViewModel: FeatureFlagNotificationViewModel = hiltViewModel(),
    conversationListViewModel: ConversationListViewModel = hiltViewModel(), // TODO: move required elements from this one to HomeViewModel?
) {
    homeViewModel.checkRequirements()
    featureFlagNotificationViewModel.loadInitialSync()
    val homeScreenState = rememberHomeScreenState()
    val showNotificationsFlow = rememberRequestPushNotificationsPermissionFlow(
        onPermissionDenied = { /** TODO: Show a dialog rationale explaining why the permission is needed **/ })

    LaunchedEffect(homeViewModel.savedStateHandle) {
        homeViewModel.checkPendingSnackbarState()?.let(homeScreenState::setSnackBarState)
        showNotificationsFlow.launch()
    }

    handleSnackBarMessage(
        snackbarHostState = homeScreenState.snackBarHostState,
        conversationListSnackBarState = homeScreenState.snackbarState,
        onMessageShown = homeScreenState::clearSnackbarMessage
    )

    val homeState = homeViewModel.homeState
    if (homeViewModel.homeState.shouldDisplayWelcomeMessage) {
        WelcomeNewUserDialog(
            dismissDialog = homeViewModel::dismissWelcomeMessage
        )
    }

    with(featureFlagNotificationViewModel.featureFlagState) {
        if (showFileSharingDialog) {
            FileRestrictionDialog(
                isFileSharingEnabled = featureFlagNotificationViewModel.featureFlagState.showFileSharingDialog,
                hideDialogStatus = featureFlagNotificationViewModel::dismissFileSharingDialog
            )
        }

        if (shouldShowGuestRoomLinkDialog) {
            GuestRoomLinkFeatureFlagDialog(
                isGuestRoomLinkEnabled = isGuestRoomLinkEnabled,
                onDismiss = featureFlagNotificationViewModel::dismissGuestRoomLinkDialog
            )
        }

        if (shouldShowSelfDeletingMessagesDialog) {
            SelfDeletingMessagesDialog(
                areSelfDeletingMessagesEnabled = areSelfDeletedMessagesEnabled,
                enforcedTimeout = enforcedTimeoutDuration,
                hideDialogStatus = featureFlagNotificationViewModel::dismissSelfDeletingMessagesDialog
            )
        }
    }

    HomeContent(
        homeState = homeState,
        homeStateHolder = homeScreenState,
        conversationListState = conversationListViewModel.conversationListState,
        onNewConversationClick = conversationListViewModel::openNewConversation,
        onSelfUserClick = homeViewModel::navigateToSelfUserProfile,
        navigateToItem = homeViewModel::navigateTo
    )

    BackHandler(homeScreenState.drawerState.isOpen) {
        homeScreenState.coroutineScope.launch {
            homeScreenState.drawerState.close()
        }
    }
    BackHandler(homeScreenState.searchBarState.isSearchActive) {
        homeScreenState.searchBarState.closeSearch()
    }
}

@Composable
fun HomeContent(
    homeState: HomeState,
    homeStateHolder: HomeStateHolder,
    conversationListState: ConversationListState,
    onNewConversationClick: () -> Unit,
    onSelfUserClick: () -> Unit,
    navigateToItem: (NavigationItem) -> Unit
) {
    with(homeStateHolder) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    drawerTonalElevation = 0.dp,
                    drawerShape = RectangleShape,
                    modifier = Modifier.padding(end = dimensions().homeDrawerSheetEndPadding)
                ) {
                    HomeDrawer(
                        // TODO: logFilePath does not belong in the UI logic
                        logFilePath = homeState.logFilePath,
                        currentRoute = currentNavigationItem.route,
                        navigateToHomeItem = ::navigateTo,
                        navigateToItem = navigateToItem,
                        onCloseDrawer = ::closeDrawer
                    )
                }
            },
            gesturesEnabled = drawerState.isOpen,
            content = {
                with(currentNavigationItem) {

                    CollapsingTopBarScaffold(
                        snapOnFling = false,
                        keepElevationWhenCollapsed = true,
                        topBarHeader = { elevation ->
                            Column(modifier = Modifier.animateContentSize()) {
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
                            if (currentNavigationItem.isSearchable) {
                                SearchTopBar(
                                    isSearchActive = searchBarState.isSearchActive,
                                    searchBarHint = stringResource(R.string.search_bar_conversations_hint),
                                    searchQuery = searchBarState.searchQuery,
                                    onSearchQueryChanged = searchBarState::searchQueryChanged,
                                    onInputClicked = searchBarState::openSearch,
                                    onCloseSearchClicked = searchBarState::closeSearch,
                                )
                            }
                        },
                        content = {
                            /**
                             * This "if" is a workaround, otherwise it can crash because of the SubcomposeLayout's nature.
                             * We need to communicate to the sub-compositions when they are to be disposed by the parent and ignore
                             * compositions in the round they are to be disposed. More here:
                             * https://github.com/google/accompanist/issues/1487
                             * https://issuetracker.google.com/issues/268422136
                             * https://issuetracker.google.com/issues/254645321
                             */
                            if (LocalLifecycleOwner.current.lifecycle.currentState != Lifecycle.State.DESTROYED) {
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

                    WireModalSheetLayout(
                        sheetState = bottomSheetState,
                        coroutineScope = coroutineScope,
                        // we want to render "nothing" instead of doing a if/else check
                        // on homeBottomSheetContent and wrap homeContent() into WireModalSheetLayout
                        // or render it without WireModalSheetLayout to avoid
                        // recomposing the homeContent() when homeBottomSheetContent
                        // changes from null to "something"
                        sheetContent = homeBottomSheetContent ?: { }
                    )
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
            is HomeSnackbarState.DeletedConversationGroupSuccess -> stringResource(
                id = R.string.conversation_group_removed_success,
                messageType.groupName
            )

            HomeSnackbarState.LeftConversationSuccess -> stringResource(id = R.string.left_conversation_group_success)
            HomeSnackbarState.LeaveConversationError -> stringResource(id = R.string.leave_group_conversation_error)
            HomeSnackbarState.DeleteConversationGroupError -> stringResource(id = R.string.delete_group_conversation_error)
            is HomeSnackbarState.ClearConversationContentFailure -> stringResource(
                if (messageType.isGroup) R.string.group_content_delete_failure
                else R.string.conversation_content_delete_failure
            )

            is HomeSnackbarState.ClearConversationContentSuccess -> stringResource(
                if (messageType.isGroup) R.string.group_content_deleted else R.string.conversation_content_deleted
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
