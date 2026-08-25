/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wire.android.R
import com.wire.android.ui.common.R as commonR
import com.wire.android.navigation.HomeDestination
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.navigation.runtime.startup.HomeRoute
import com.wire.android.ui.analytics.AnalyticsUsageViewModel
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.search.SearchBarState
import com.wire.android.ui.common.search.rememberSearchbarState
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.common.topappbar.ConversationFilterState
import com.wire.android.ui.common.topappbar.rememberConversationFilterState
import com.wire.android.ui.home.conversations.ConversationCompletionAction
import com.wire.android.ui.home.conversations.ConversationCompletionNavigation3ResultType
import com.wire.android.ui.home.conversations.ConversationCompletionResult
import com.wire.android.ui.home.conversations.ConversationAuxId
import com.wire.android.ui.home.conversations.ConversationFoldersNavigation3ResultType
import com.wire.android.ui.home.conversations.ConversationFoldersRoute
import com.wire.android.ui.home.conversations.ConversationRoute
import com.wire.android.ui.home.conversations.ConversationRouteId
import com.wire.android.ui.home.drawer.HomeDrawerViewModel
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState
import com.wire.android.ui.home.conversationslist.filter.toTopBarTitle
import com.wire.android.ui.home.conversationslist.ConversationsNavigationActions
import com.wire.android.ui.home.conversationslist.all.AllConversationsContent
import com.wire.android.ui.analyticsUsageViewModel
import com.wire.android.util.ui.LazyListStateProvider
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.rememberLazyListStateProvider
import com.wire.android.util.permission.rememberShowNotificationsPermissionFlow
import com.wire.kalium.logic.data.conversation.ConversationFilter
import com.wire.android.ui.userprofile.UserProfileQualifiedId
import com.wire.android.ui.userprofile.ConnectionRequestIgnoredNavigation3ResultType
import com.wire.android.ui.userprofile.other.ConnectionRequestIgnoredResult
import com.wire.android.ui.userprofile.other.OtherUserProfileRoute
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavResultRequestId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Stable semantic identity of the content displayed inside the Home chrome.
 *
 * These are deliberately not Navigation 3 back-stack entries: switching a drawer item preserves
 * the Home entry, drawer state, search state and per-item list state exactly like the old nested
 * Home graph did.
 */
internal enum class HomeTopLevelDestination {
    CONVERSATIONS,
    SETTINGS,
    VAULT,
    ARCHIVE,
    WHATS_NEW,
    CELLS,
    MEETINGS,
}

internal enum class HomeExternalDestination {
    SUPPORT,
    TEAM_MANAGEMENT,
}

internal sealed interface HomeNavigation3Target {
    data class TopLevel(val destination: HomeTopLevelDestination) : HomeNavigation3Target
    data class External(val destination: HomeExternalDestination) : HomeNavigation3Target
}

internal fun HomeDestination.toNavigation3Target(): HomeNavigation3Target = when (this) {
    HomeDestination.Conversations -> HomeNavigation3Target.TopLevel(HomeTopLevelDestination.CONVERSATIONS)
    HomeDestination.Settings -> HomeNavigation3Target.TopLevel(HomeTopLevelDestination.SETTINGS)
    HomeDestination.Vault -> HomeNavigation3Target.TopLevel(HomeTopLevelDestination.VAULT)
    HomeDestination.Archive -> HomeNavigation3Target.TopLevel(HomeTopLevelDestination.ARCHIVE)
    HomeDestination.WhatsNew -> HomeNavigation3Target.TopLevel(HomeTopLevelDestination.WHATS_NEW)
    HomeDestination.Cells -> HomeNavigation3Target.TopLevel(HomeTopLevelDestination.CELLS)
    HomeDestination.Meetings -> HomeNavigation3Target.TopLevel(HomeTopLevelDestination.MEETINGS)
    HomeDestination.Support -> HomeNavigation3Target.External(HomeExternalDestination.SUPPORT)
    HomeDestination.TeamManagement -> HomeNavigation3Target.External(HomeExternalDestination.TEAM_MANAGEMENT)
}

internal fun HomeTopLevelDestination.toHomeDestination(): HomeDestination = when (this) {
    HomeTopLevelDestination.CONVERSATIONS -> HomeDestination.Conversations
    HomeTopLevelDestination.SETTINGS -> HomeDestination.Settings
    HomeTopLevelDestination.VAULT -> HomeDestination.Vault
    HomeTopLevelDestination.ARCHIVE -> HomeDestination.Archive
    HomeTopLevelDestination.WHATS_NEW -> HomeDestination.WhatsNew
    HomeTopLevelDestination.CELLS -> HomeDestination.Cells
    HomeTopLevelDestination.MEETINGS -> HomeDestination.Meetings
}

/**
 * Drawer destinations share one [HomeRoute] entry, so the Navigation 3 back stack cannot restore
 * Conversations for us. Preserve the legacy nested-Home behavior explicitly: back from any
 * secondary Home destination first returns to the conversation list.
 */
internal fun HomeTopLevelDestination.backDestination(): HomeTopLevelDestination? =
    HomeTopLevelDestination.CONVERSATIONS.takeUnless { this == it }

/**
 * The typed boundary between the Home shell and children that are migrated independently.
 *
 * No generated direction or NavController crosses this API. A child can therefore move to a typed
 * route without changing the drawer/top-bar owner.
 */
internal interface HomeNavigation3Actions {
    val conversations: ConversationsNavigationActions
    val topLevel: HomeTopLevelNavigation3Actions

    fun onRequirement(requirement: HomeRequirement)
    fun openNewConversation()
    fun openSelfProfile()
    fun openExternal(destination: HomeExternalDestination)
}

internal object HomeNavigation3Contribution {
    fun entryProviderInstallers(
        runtime: WireNavigation3Runtime,
        actions: HomeNavigation3Actions,
    ): List<WireEntryProviderInstaller> = listOf(homeNavigation3Entries(runtime, actions))
}

internal fun homeNavigation3Entries(
    runtime: WireNavigation3Runtime,
    actions: HomeNavigation3Actions,
): WireEntryProviderInstaller = {
    wireEntry<HomeRoute> { route ->
        HomeNavigation3Entry(route, runtime, actions)
    }
}

@Composable
@Suppress("CyclomaticComplexMethod")
private fun HomeNavigation3Entry(
    route: HomeRoute,
    runtime: WireNavigation3Runtime,
    actions: HomeNavigation3Actions,
    homeViewModel: HomeViewModel = homeViewModel(),
    appSyncViewModel: AppSyncViewModel = appSyncViewModel(),
    homeDrawerViewModel: HomeDrawerViewModel = homeDrawerViewModel(),
    analyticsUsageViewModel: AnalyticsUsageViewModel = analyticsUsageViewModel(),
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val selectedDestination = rememberSaveable(route.entryId.value) {
        mutableStateOf(HomeTopLevelDestination.CONVERSATIONS)
    }
    var conversationRequestIdValue by rememberSaveable(route.entryId.value) {
        mutableStateOf<String?>(null)
    }
    var userProfileRequestIdValue by rememberSaveable(route.entryId.value) {
        mutableStateOf<String?>(null)
    }
    var folderRequestIdValue by rememberSaveable(route.entryId.value) {
        mutableStateOf<String?>(null)
    }
    val shellState = rememberHomeNavigation3ShellState(selectedDestination)
    val lifecycleOwner = LocalLifecycleOwner.current
    val notificationsPermissionDeniedDialogState =
        rememberVisibilityState<PermissionPermanentlyDeniedDialogState>()
    val showNotificationsFlow = rememberShowNotificationsPermissionFlow(
        onPermissionGranted = { /* no-op */ },
        onPermissionDenied = {
            notificationsPermissionDeniedDialogState.show(
                PermissionPermanentlyDeniedDialogState.Visible(
                    title = commonR.string.app_permission_dialog_title,
                    description = R.string.notifications_permission_dialog_description,
                )
            )
        },
        onPermissionPermanentlyDenied = { /* no-op */ },
    )

    LaunchedEffect(route.entryId.value, homeViewModel) {
        homeViewModel.checkRequirements()
    }
    HandleActions(homeViewModel.actions, actions::onRequirement)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                appSyncViewModel.startSyncingAppConfig()
                shellState.clearSearchOnResumeIfRequested()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(route.entryId.value) {
        showNotificationsFlow.launch()
    }
    val currentEntryId = runtime.navigator.currentRoute?.entryId
    LaunchedEffect(
        currentEntryId,
        conversationRequestIdValue,
        userProfileRequestIdValue,
        folderRequestIdValue,
    ) {
        if (currentEntryId != route.entryId) return@LaunchedEffect

        conversationRequestIdValue?.let(::WireNavResultRequestId)?.let { requestId ->
            when (val result = runtime.consumeResult(requestId, ConversationCompletionNavigation3ResultType)) {
                is WireNavResult.Value -> {
                    snackbarHostState.showSnackbar(
                        result.value.toHomeSnackBarMessage().uiText.asString(context.resources)
                    )
                    conversationRequestIdValue = null
                }

                WireNavResult.Canceled -> conversationRequestIdValue = null
                null -> Unit
            }
        }
        userProfileRequestIdValue?.let(::WireNavResultRequestId)?.let { requestId ->
            when (val result = runtime.consumeResult(requestId, ConnectionRequestIgnoredNavigation3ResultType)) {
                is WireNavResult.Value -> {
                    snackbarHostState.showSnackbar(
                        result.value.toHomeSnackBarMessage().uiText.asString(context.resources)
                    )
                    userProfileRequestIdValue = null
                }

                WireNavResult.Canceled -> userProfileRequestIdValue = null
                null -> Unit
            }
        }
        folderRequestIdValue?.let(::WireNavResultRequestId)?.let { requestId ->
            when (val result = runtime.consumeResult(requestId, ConversationFoldersNavigation3ResultType)) {
                is WireNavResult.Value -> {
                    snackbarHostState.showSnackbar(result.value.message)
                    folderRequestIdValue = null
                }

                WireNavResult.Canceled -> folderRequestIdValue = null
                null -> Unit
            }
        }
    }

    val conversationsNavigationActions = remember(route.sessionId, runtime, actions.conversations) {
        actions.conversations.copy(
            openConversation = { conversationId ->
                val requestId = runtime.navigateForResult(
                    destination = ConversationRoute(
                        sessionId = route.sessionId,
                        conversationId = ConversationRouteId(conversationId.value, conversationId.domain),
                    ),
                    resultType = ConversationCompletionNavigation3ResultType,
                )
                if (requestId == null) {
                    actions.conversations.openConversation(conversationId)
                } else {
                    conversationRequestIdValue = requestId.value
                }
            },
            openUserProfile = { userId ->
                val requestId = runtime.navigateForResult(
                    destination = OtherUserProfileRoute(
                        sessionId = route.sessionId,
                        targetUserId = UserProfileQualifiedId(userId.value, userId.domain),
                    ),
                    resultType = ConnectionRequestIgnoredNavigation3ResultType,
                )
                if (requestId == null) {
                    actions.conversations.openUserProfile(userId)
                } else {
                    userProfileRequestIdValue = requestId.value
                }
            },
            openConversationFolders = { args ->
                folderRequestIdValue = runtime.navigateForResult(
                    destination = ConversationFoldersRoute(
                        sessionId = route.sessionId,
                        conversationId = ConversationAuxId(
                            args.conversationId.value,
                            args.conversationId.domain,
                        ),
                        conversationName = args.conversationName,
                        currentFolderId = args.currentFolderId,
                    ),
                    resultType = ConversationFoldersNavigation3ResultType,
                )?.value
            },
        )
    }

    if (analyticsUsageViewModel.state.shouldDisplayDialog) {
        AnalyticsUsageDialog(
            agreeOption = analyticsUsageViewModel::agreeAnalyticsUsage,
            declineOption = analyticsUsageViewModel::declineAnalyticsUsage,
        )
    }

    HomeContent(
        homeState = homeViewModel.homeState,
        homeDrawerState = homeDrawerViewModel.drawerState,
        homeStateHolder = shellState,
        onNewConversationClick = actions::openNewConversation,
        onSelfUserClick = actions::openSelfProfile,
        onNavigateToHomeItem = { item ->
            when (val target = item.toNavigation3Target()) {
                is HomeNavigation3Target.TopLevel -> shellState.select(target.destination)
                is HomeNavigation3Target.External -> actions.openExternal(target.destination)
            }
        },
        content = {
            when (shellState.selectedDestination) {
                HomeTopLevelDestination.CONVERSATIONS -> AllConversationsContent(
                    homeShellState = shellState,
                    navigationActions = conversationsNavigationActions,
                )

                else -> HomeNavigation3TopLevelContent(
                    destination = shellState.selectedDestination,
                    shellState = shellState,
                    sessionId = route.sessionId,
                    runtime = runtime,
                    actions = actions.topLevel,
                    conversationsNavigationActions = conversationsNavigationActions,
                )
            }
        },
    )

    val topLevelBackDestination = shellState.selectedDestination.backDestination()
    BackHandler(topLevelBackDestination != null) {
        topLevelBackDestination?.let(shellState::select)
    }
    BackHandler(shellState.drawerState.isOpen) {
        shellState.closeDrawer()
    }
    BackHandler(shellState.searchBarState.isSearchActive) {
        shellState.searchBarState.closeSearch()
    }
    PermissionPermanentlyDeniedDialog(
        dialogState = notificationsPermissionDeniedDialogState,
        hideDialog = notificationsPermissionDeniedDialogState::dismiss,
    )
}

internal fun ConversationCompletionResult.toHomeSnackBarMessage(): HomeSnackBarMessage =
    when (action) {
        ConversationCompletionAction.LEAVE_GROUP -> HomeSnackBarMessage.LeftConversationSuccess
        ConversationCompletionAction.DELETE_GROUP ->
            HomeSnackBarMessage.DeletedConversationGroupSuccess(conversationName)
    }

internal fun ConnectionRequestIgnoredResult.toHomeSnackBarMessage(): HomeSnackBarMessage =
    HomeSnackBarMessage.SuccessConnectionIgnoreRequest(userName)

@Suppress("ComposeMutableParameters")
@Composable
private fun rememberHomeNavigation3ShellState(
    selectedDestinationState: MutableState<HomeTopLevelDestination>,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
): HomeNavigation3ShellState {
    val searchBarState = rememberSearchbarState()
    val conversationFilterState = rememberConversationFilterState()
    val conversationsFilterBottomSheetState = rememberWireModalSheetState<Unit>()
    val newMeetingBottomSheetState = rememberWireModalSheetState<Unit>()
    val lazyListStateProvider = rememberLazyListStateProvider<String>()
    val currentNavigationItemState = remember(selectedDestinationState) {
        derivedStateOf { selectedDestinationState.value.toHomeDestination() }
    }

    return remember {
        HomeNavigation3ShellState(
            coroutineScope = coroutineScope,
            drawerState = drawerState,
            searchBarState = searchBarState,
            conversationsFilterBottomSheetState = conversationsFilterBottomSheetState,
            newMeetingBottomSheetState = newMeetingBottomSheetState,
            currentNavigationItemState = currentNavigationItemState,
            selectedDestinationState = selectedDestinationState,
            conversationFilterState = conversationFilterState,
            lazyListStateProvider = lazyListStateProvider,
        )
    }
}

@Suppress("LongParameterList")
private class HomeNavigation3ShellState(
    override val coroutineScope: CoroutineScope,
    override val drawerState: DrawerState,
    override val searchBarState: SearchBarState,
    override val conversationsFilterBottomSheetState: WireModalSheetState<Unit>,
    override val newMeetingBottomSheetState: WireModalSheetState<Unit>,
    private val currentNavigationItemState: State<HomeDestination>,
    private val selectedDestinationState: MutableState<HomeTopLevelDestination>,
    private val conversationFilterState: ConversationFilterState,
    private val lazyListStateProvider: LazyListStateProvider<String>,
) : HomeShellState {
    override val emptySearchResultFocusRequester = androidx.compose.ui.focus.FocusRequester()
    override val firstConversationFocusRequester = androidx.compose.ui.focus.FocusRequester()

    val selectedDestination: HomeTopLevelDestination
        get() = selectedDestinationState.value

    override val currentNavigationItem: HomeDestination
        get() = currentNavigationItemState.value

    override val currentConversationFilter: ConversationFilter
        get() = conversationFilterState.filter

    override val currentTitle: UIText
        get() = when (currentNavigationItem) {
            HomeDestination.Conversations -> currentConversationFilter.toTopBarTitle()
            else -> currentNavigationItem.title
        }

    fun select(destination: HomeTopLevelDestination) {
        selectedDestinationState.value = destination
    }

    override fun lazyListStateFor(
        destination: HomeDestination,
        conversationFilter: ConversationFilter,
    ): LazyListState = lazyListStateProvider.get(
        destination.itemName + if (destination == HomeDestination.Conversations) ":$conversationFilter" else ""
    )

    override fun closeDrawer() {
        coroutineScope.launch { drawerState.close() }
    }

    override fun openDrawer() {
        coroutineScope.launch { drawerState.open() }
    }

    override fun changeConversationFilter(filter: ConversationFilter) {
        lazyListStateFor(HomeDestination.Conversations, currentConversationFilter).requestScrollToItem(0, 0)
        conversationFilterState.changeFilter(filter)
    }

    override fun requestClearSearchOnNextResume() {
        searchBarState.requestClearSearchOnNextResume()
    }

    override fun clearSearchOnResumeIfRequested() {
        searchBarState.clearSearchOnResumeIfRequested()
    }
}
