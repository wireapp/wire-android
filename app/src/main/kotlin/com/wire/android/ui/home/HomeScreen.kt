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
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ramcosta.composedestinations.generated.app.destinations.ConversationFoldersScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.ConversationScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.NewConversationSearchPeopleScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.OtherUserProfileScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.SelfUserProfileScreenDestination
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.navigation.HomeDestination
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.navigation.handleNavigation
import com.wire.android.ui.analytics.AnalyticsUsageViewModel
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState
import com.wire.android.ui.home.conversations.details.GroupConversationActionType
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsNavBackArgs
import com.wire.android.ui.home.conversations.folder.ConversationFoldersNavBackArgs
import com.wire.android.ui.home.drawer.HomeDrawerState
import com.wire.android.ui.home.drawer.HomeDrawerViewModel
import com.wire.android.ui.analyticsUsageViewModel
import com.wire.android.util.permission.rememberShowNotificationsPermissionFlow
import kotlinx.coroutines.launch

@WireRootDestination
@Composable
fun HomeScreen(
    navigator: Navigator,
    groupDetailsScreenResultRecipient:
    ResultRecipient<ConversationScreenDestination, GroupConversationDetailsNavBackArgs>,
    otherUserProfileScreenResultRecipient: ResultRecipient<OtherUserProfileScreenDestination, String>,
    conversationFoldersScreenResultRecipient:
    ResultRecipient<ConversationFoldersScreenDestination, ConversationFoldersNavBackArgs>,
    homeViewModel: HomeViewModel = homeViewModel(),
    appSyncViewModel: AppSyncViewModel = appSyncViewModel(),
    homeDrawerViewModel: HomeDrawerViewModel = homeDrawerViewModel(),
    analyticsUsageViewModel: AnalyticsUsageViewModel = analyticsUsageViewModel(),
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
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }
    val fabFocusRequester = remember { FocusRequester() }
    val drawerFocusRequester = remember { FocusRequester() }
    val firstDrawerItemFocusRequester = remember { FocusRequester() }
    val lastDrawerItemFocusRequester = remember { FocusRequester() }
    var isDrawerSheetFocused by remember { mutableStateOf(false) }

    LaunchedEffect(homeStateHolder.drawerState.isOpen) {
        if (homeStateHolder.drawerState.isOpen) {
            withFrameNanos { }
            if (!firstDrawerItemFocusRequester.requestFocus()) {
                drawerFocusRequester.requestFocus()
            }
        }
    }

    with(homeStateHolder) {
        fun closeHomeDrawer() {
            focusManager.clearFocus(force = true)
            closeDrawer()
        }

        fun requestDrawerItemFocus(isShiftPressed: Boolean) {
            if (isShiftPressed) {
                lastDrawerItemFocusRequester.requestFocus()
            } else {
                firstDrawerItemFocusRequester.requestFocus()
            }
        }

        fun openWireHomeDestination(item: HomeDestination) {
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

        val drawerSheetFocusTrapState = HomeDrawerSheetFocusTrapState(
            enabled = drawerState.isOpen,
            focusRequester = drawerFocusRequester,
            isSheetFocused = isDrawerSheetFocused
        )
        val drawerSheetFocusTrapActions = HomeDrawerSheetFocusTrapActions(
            onSheetFocusChanged = { isDrawerSheetFocused = it },
            onItemFocusRequested = ::requestDrawerItemFocus,
            onClose = ::closeHomeDrawer
        )

        ModalNavigationDrawer(
            modifier = modifier.homeDrawerKeyboardNavigation(
                isDrawerOpen = drawerState.isOpen,
                isDrawerSheetFocused = isDrawerSheetFocused,
                onDrawerItemFocusRequested = ::requestDrawerItemFocus,
                onCloseDrawer = ::closeHomeDrawer
            ),
            drawerState = drawerState,
            drawerContent = {
                HomeDrawerSheet(
                    currentRoute = currentNavigationItem.direction.route,
                    homeDrawerState = homeDrawerState,
                    focusTrapState = drawerSheetFocusTrapState,
                    focusTrapActions = drawerSheetFocusTrapActions,
                    firstItemFocusRequester = firstDrawerItemFocusRequester,
                    lastItemFocusRequester = lastDrawerItemFocusRequester,
                    onNavigateToHomeItem = ::openWireHomeDestination,
                    onCloseDrawer = ::closeHomeDrawer
                )
            },
            gesturesEnabled = drawerState.isOpen,
            content = {
                HomeScaffold(
                    homeState = homeState,
                    state = remember(homeStateHolder) { HomeScaffoldState(homeStateHolder) },
                    drawerState = drawerState,
                    focusRequesters = HomeScaffoldFocusRequesters(
                        search = searchFocusRequester,
                        fab = fabFocusRequester
                    ),
                    actions = HomeScaffoldActions(
                        onDrawerItemFocusRequested = ::requestDrawerItemFocus,
                        onNewConversationClick = onNewConversationClick,
                        onSelfUserClick = onSelfUserClick,
                        onHamburgerMenuClick = ::openDrawer
                    )
                )
            }
        )
    }
}
