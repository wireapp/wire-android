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
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.navigation.HomeDestination
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.handleNavigation
import com.wire.android.ui.NavGraphs
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.FloatingActionButton
import com.wire.android.ui.common.WireBottomNavigationBar
import com.wire.android.ui.common.WireBottomNavigationItemData
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.NewConversationSearchPeopleScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.destinations.SelfUserProfileScreenDestination
import com.wire.android.ui.home.conversations.details.GroupConversationActionType
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsNavBackArgs
import com.wire.android.ui.home.conversationslist.ConversationListState
import com.wire.android.ui.home.conversationslist.ConversationListViewModel
import com.wire.android.util.permission.rememberRequestPushNotificationsPermissionFlow
import kotlinx.coroutines.launch

@RootNavGraph
@Destination
@Composable
fun HomeScreen(
    navigator: Navigator,
    homeViewModel: HomeViewModel = hiltViewModel(),
    conversationListViewModel: ConversationListViewModel = hiltViewModel(), // TODO: move required elements from this one to HomeViewModel?,
    groupDetailsScreenResultRecipient: ResultRecipient<ConversationScreenDestination, GroupConversationDetailsNavBackArgs>,
    otherUserProfileScreenResultRecipient: ResultRecipient<OtherUserProfileScreenDestination, String>,
) {
    homeViewModel.checkRequirements() { it.navigate(navigator::navigate) }
    val homeScreenState = rememberHomeScreenState(navigator)
    val showNotificationsFlow = rememberRequestPushNotificationsPermissionFlow(
        onPermissionDenied = { /** TODO: Show a dialog rationale explaining why the permission is needed **/ })

    LaunchedEffect(homeViewModel.savedStateHandle) {
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

    HomeContent(
        homeState = homeState,
        homeStateHolder = homeScreenState,
        conversationListState = conversationListViewModel.conversationListState,
        onNewConversationClick = { navigator.navigate(NavigationCommand(NewConversationSearchPeopleScreenDestination)) },
        onSelfUserClick = remember(navigator) { { navigator.navigate(NavigationCommand(SelfUserProfileScreenDestination)) } }
    )

    BackHandler(homeScreenState.drawerState.isOpen) {
        homeScreenState.coroutineScope.launch {
            homeScreenState.drawerState.close()
        }
    }
    BackHandler(homeScreenState.searchBarState.isSearchActive) {
        homeScreenState.searchBarState.closeSearch()
    }

    groupDetailsScreenResultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> {
                appLogger.i("Error with receiving navigation back args from groupDetails in ConversationScreen")
            }

            is NavResult.Value -> {
                when (result.value.groupConversationActionType) {
                    GroupConversationActionType.LEAVE_GROUP -> {
                        homeScreenState.setSnackBarState(HomeSnackbarState.LeftConversationSuccess)
                    }

                    GroupConversationActionType.DELETE_GROUP -> {
                        val groupDeletedSnackBar = HomeSnackbarState.DeletedConversationGroupSuccess(result.value.conversationName)
                        homeScreenState.setSnackBarState(groupDeletedSnackBar)
                    }
                }
            }
        }
    }

    otherUserProfileScreenResultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> {
                appLogger.i("Error with receiving navigation back args from OtherUserProfile in ConversationScreen")
            }

            is NavResult.Value -> {
                homeScreenState.setSnackBarState(HomeSnackbarState.SuccessConnectionIgnoreRequest(result.value))
            }
        }
    }
}

@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalAnimationApi::class)
@Composable
fun HomeContent(
    homeState: HomeState,
    homeStateHolder: HomeStateHolder,
    conversationListState: ConversationListState,
    onNewConversationClick: () -> Unit,
    onSelfUserClick: () -> Unit,
) {
    val context = LocalContext.current
    with(homeStateHolder) {
        fun openHomeDestination(item: HomeDestination) {
            item.direction.handleNavigation(
                context = context,
                handleOtherDirection = { direction ->
                    navController.navigate(direction.route) {
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) {
                                saveState = true
                            }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                BoxWithConstraints {
                    val maxWidth = min(this.maxWidth - dimensions().homeDrawerSheetEndPadding, DrawerDefaults.MaximumDrawerWidth)
                    ModalDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.surface,
                        drawerTonalElevation = 0.dp,
                        drawerShape = RectangleShape,
                        modifier = Modifier.widthIn(max = maxWidth)
                    ) {
                        HomeDrawer(
                            currentRoute = currentNavigationItem.direction.route,
                            navigateToHomeItem = ::openHomeDestination,
                            onCloseDrawer = ::closeDrawer
                        )
                    }
                }
            },
            gesturesEnabled = drawerState.isOpen,
            content = {
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
                            val navHostEngine = rememberAnimatedNavHostEngine(
                                rootDefaultAnimations = RootNavGraphDefaultAnimations.ACCOMPANIST_FADING
                            )
                            DestinationsNavHost(
                                navGraph = NavGraphs.home,
                                engine = navHostEngine,
                                navController = navController,
                                dependenciesContainerBuilder = {
                                    dependency(homeStateHolder)
                                }
                            )
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
                                items = HomeDestination.bottomTabItems.toBottomNavigationItemData(
                                    conversationListState = conversationListState
                                ),
                                selectedItemRoute = homeStateHolder.currentNavigationItem.direction.route,
                                onItemSelected = { HomeDestination.fromRoute(it.route)?.let { openHomeDestination(it) } }
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
private fun List<HomeDestination>.toBottomNavigationItemData(
    conversationListState: ConversationListState
): List<WireBottomNavigationItemData> = map {
    when (it) {
        HomeDestination.Conversations -> it.toBottomNavigationItemData(conversationListState.newActivityCount)
        HomeDestination.Calls -> it.toBottomNavigationItemData(conversationListState.missedCallsCount)
        HomeDestination.Mentions -> it.toBottomNavigationItemData(conversationListState.unreadMentionsCount)
        else -> it.toBottomNavigationItemData(0L)
    }
}
