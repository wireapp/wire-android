/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.navigation.AdjustDestinationStylesForTablets
import com.wire.android.navigation.HomeDestination
import com.wire.android.navigation.HomeDestination.FabOptions
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.navigation.handleNavigation
import com.wire.android.ui.NavGraphs
import com.wire.android.ui.analytics.AnalyticsUsageViewModel
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.button.FloatingActionButton
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.ConversationFoldersScreenDestination
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.NewConversationSearchPeopleScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.destinations.SelfUserProfileScreenDestination
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState
import com.wire.android.ui.home.conversations.details.GroupConversationActionType
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsNavBackArgs
import com.wire.android.ui.home.conversations.folder.ConversationFoldersNavBackArgs
import com.wire.android.ui.home.drawer.HomeDrawer
import com.wire.android.ui.home.drawer.HomeDrawerState
import com.wire.android.ui.home.drawer.HomeDrawerViewModel
import com.wire.android.util.permission.rememberShowNotificationsPermissionFlow
import kotlinx.coroutines.launch

@WireDestination
@Composable
fun HomeScreen(
    navigator: Navigator,
    groupDetailsScreenResultRecipient: ResultRecipient<ConversationScreenDestination, GroupConversationDetailsNavBackArgs>,
    otherUserProfileScreenResultRecipient: ResultRecipient<OtherUserProfileScreenDestination, String>,
    conversationFoldersScreenResultRecipient:
    ResultRecipient<ConversationFoldersScreenDestination, ConversationFoldersNavBackArgs>,
    homeViewModel: HomeViewModel = hiltViewModel(),
    appSyncViewModel: AppSyncViewModel = hiltViewModel(),
    homeDrawerViewModel: HomeDrawerViewModel = hiltViewModel(),
    analyticsUsageViewModel: AnalyticsUsageViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    homeViewModel.checkRequirements()

    HandleActions(homeViewModel.actions) { action ->
        action.navigate(navigator::navigate)
    }

    val homeScreenState = rememberHomeScreenState(navigator)
    val notificationsPermissionDeniedDialogState = rememberVisibilityState<PermissionPermanentlyDeniedDialogState>()
    val showNotificationsPermissionDeniedDialog = {
        notificationsPermissionDeniedDialogState.show(
            PermissionPermanentlyDeniedDialogState.Visible(
                title = R.string.app_permission_dialog_title,
                description = R.string.notifications_permission_dialog_description,
            )
        )
    }
    val showNotificationsFlow =
        rememberShowNotificationsPermissionFlow(
            onPermissionGranted = { /* do nothing */ },
            onPermissionDenied = showNotificationsPermissionDeniedDialog,
            onPermissionPermanentlyDenied = { /* do nothing */ },
        )

    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                appSyncViewModel.startSyncingAppConfig()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(homeViewModel.savedStateHandle) {
        showNotificationsFlow.launch()
    }

    val homeState = homeViewModel.homeState

    if (analyticsUsageViewModel.state.shouldDisplayDialog) {
        AnalyticsUsageDialog(
            agreeOption = analyticsUsageViewModel::agreeAnalyticsUsage,
            declineOption = analyticsUsageViewModel::declineAnalyticsUsage
        )
    }

    HomeContent(
        homeState = homeState,
        homeDrawerState = homeDrawerViewModel.drawerState,
        homeStateHolder = homeScreenState,
        onNewConversationClick = { navigator.navigate(NavigationCommand(NewConversationSearchPeopleScreenDestination)) },
        onSelfUserClick = {
            // Temporarily stopping sending ui.clicked-profile event
            // homeViewModel.sendOpenProfileEvent()
            navigator.navigate(NavigationCommand(SelfUserProfileScreenDestination))
        },
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
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar((HomeSnackBarMessage.LeftConversationSuccess.uiText.asString(context.resources)))
                        }
                    }

                    GroupConversationActionType.DELETE_GROUP -> {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                HomeSnackBarMessage.DeletedConversationGroupSuccess(result.value.conversationName).uiText.asString(
                                    context.resources
                                )
                            )
                        }
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
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        HomeSnackBarMessage.SuccessConnectionIgnoreRequest(result.value).uiText.asString(context.resources)
                    )
                }
            }
        }
    }

    conversationFoldersScreenResultRecipient.onNavResult { result ->
        when (result) {
            NavResult.Canceled -> {}
            is NavResult.Value -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(result.value.message)
                }
            }
        }
    }

    PermissionPermanentlyDeniedDialog(
        dialogState = notificationsPermissionDeniedDialogState,
        hideDialog = notificationsPermissionDeniedDialogState::dismiss
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeContent(
    homeState: HomeState,
    homeDrawerState: HomeDrawerState,
    homeStateHolder: HomeStateHolder,
    onNewConversationClick: () -> Unit,
    onSelfUserClick: () -> Unit,
    modifier: Modifier = Modifier,
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
            modifier = modifier,
            drawerState = drawerState,
            drawerContent = {
                BoxWithConstraints {
                    val width = min(this.maxWidth - dimensions().homeDrawerSheetEndPadding, DrawerDefaults.MaximumDrawerWidth)
                    ModalDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.surface,
                        drawerTonalElevation = 0.dp,
                        drawerShape = RectangleShape,
                        modifier = Modifier.width(width)
                    ) {
                        HomeDrawer(
                            currentRoute = currentNavigationItem.direction.route,
                            homeDrawerState = homeDrawerState,
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
                    topBarHeader = {
                        AnimatedVisibility(
                            modifier = Modifier.background(MaterialTheme.colorScheme.background),
                            visible = !searchBarState.isSearchActive,
                            enter = fadeIn() + expandVertically(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            HomeTopBar(
                                title = currentTitle.asString(),
                                currentConversationFilter = currentConversationFilter,
                                currentCellsFilters = currentCellsFilters,
                                navigationItem = currentNavigationItem,
                                userAvatarData = homeState.userAvatarData,
                                elevation = dimensions().spacing0x, // CollapsingTopBarScaffold manages applied elevation
                                withLegalHoldIndicator = homeState.shouldDisplayLegalHoldIndicator,
                                shouldShowCreateTeamUnreadIndicator = homeState.shouldShowCreateTeamUnreadIndicator,
                                onHamburgerMenuClick = ::openDrawer,
                                onNavigateToSelfUserProfile = onSelfUserClick,
                                onOpenConversationFilter = {
                                    homeStateHolder.conversationsFilterBottomSheetState.show(Unit)
                                },
                                onOpenFilesFilter = {
                                    homeStateHolder.cellsFilterBottomSheetState.show(Unit)
                                }
                            )
                        }
                    },
                    topBarCollapsing = {
                        currentNavigationItem.searchBar?.let { searchBar ->
                            AnimatedVisibility(
                                visible = searchBarState.isSearchVisible,
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut() + slideOutVertically()
                            ) {
                                SearchTopBar(
                                    isSearchActive = searchBarState.isSearchActive,
                                    searchBarHint = stringResource(searchBar.hint),
                                    searchQueryTextState = searchBarState.searchQueryTextState,
                                    onActiveChanged = searchBarState::searchActiveChanged,
                                )
                            }
                        }
                    },
                    collapsingEnabled = !searchBarState.isSearchActive,
                    contentLazyListState = homeStateHolder.lazyListStateFor(currentNavigationItem, currentConversationFilter),
                    content = {
                        /**
                         * This "if" is a workaround, otherwise it can crash because of the SubcomposeLayout's nature.
                         * We need to communicate to the sub-compositions when they are to be disposed by the parent and ignore
                         * compositions in the round they are to be disposed. More here:
                         * https://github.com/google/accompanist/issues/1487
                         * https://issuetracker.google.com/issues/268422136
                         * https://issuetracker.google.com/issues/254645321
                         */
                        val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
                        if (lifecycleState != Lifecycle.State.DESTROYED) {
                            val navHostEngine = rememberAnimatedNavHostEngine(
                                rootDefaultAnimations = RootNavGraphDefaultAnimations.ACCOMPANIST_FADING
                            )

                            AdjustDestinationStylesForTablets()
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
                            visible = currentNavigationItem.fab != null && !searchBarState.isSearchActive,
                            enter = scaleIn(),
                            exit = scaleOut(),
                        ) {
                            var currentFab by remember { mutableStateOf(currentNavigationItem.fab ?: FabOptions.NewConversation) }
                            // to keep the fab during the exit animation, we need to keep last known (non-null) fab data
                            if (currentNavigationItem.fab != null) currentFab = currentNavigationItem.fab!!

                            FloatingActionButton(
                                text = stringResource(currentFab.text),
                                icon = {
                                    Image(
                                        painter = painterResource(currentFab.icon),
                                        contentDescription = stringResource(currentFab.contentDescription),
                                        contentScale = ContentScale.FillBounds,
                                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                                        modifier = Modifier
                                            .padding(start = dimensions().spacing4x, top = dimensions().spacing2x)
                                            .size(dimensions().fabIconSize)
                                    )
                                },
                                onClick = {
                                    when (currentNavigationItem.fab) {
                                        FabOptions.NewConversation -> onNewConversationClick()
                                        FabOptions.NewMeeting -> homeStateHolder.newMeetingBottomSheetState.show(Unit)
                                        else -> { /* no-op */ }
                                    }
                                }
                            )
                        }
                    }
                )
            }
        )
    }
}
