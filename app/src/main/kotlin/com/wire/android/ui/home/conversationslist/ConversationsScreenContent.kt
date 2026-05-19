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

package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.ramcosta.composedestinations.generated.app.destinations.BrowseChannelsScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.ConversationFoldersScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.ConversationScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.DebugConversationScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.NewConversationSearchPeopleScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.OtherUserProfileScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.PromoteAdminScreenDestination
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.feature.analytics.AnonymousAnalyticsManagerImpl
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.rememberNavigator
import com.wire.android.ui.calling.ongoing.getOngoingCallIntent
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.bottomsheet.conversation.ConversationOptionsModalSheetLayout
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetState
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.dialogs.calling.JoinAnywayDialog
import com.wire.android.ui.common.rowitem.LoadingListContent
import com.wire.android.ui.common.search.SearchBarState
import com.wire.android.ui.common.search.rememberSearchbarState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.debug.conversation.DebugConversationScreenNavArgs
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState
import com.wire.android.ui.home.conversations.promoteadmin.PromoteAdminNavArgs
import com.wire.android.ui.home.conversationslist.common.ConversationList
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationItemType
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.ui.home.conversationslist.search.SearchConversationsEmptyContent
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.SnackBarMessageHandler
import com.wire.android.util.ui.collectAsLazyPagingItemsWithLifecycle
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

/**
 * This is a base for creating screens for displaying list of conversations.
 * Can be used to create proper navigation destination for different sources of conversations, like archive.
 */
@Suppress("ComplexMethod", "NestedBlockDepth", "Wrapping")
@Composable
fun ConversationsScreenContent(
    navigator: Navigator,
    searchBarState: SearchBarState,
    emptyListContent: @Composable (domain: String) -> Unit = {},
    lazyListState: LazyListState = rememberLazyListState(),
    loadingListContent: @Composable (LazyListState) -> Unit = { LoadingListContent(it) },
    conversationsSource: ConversationsSource = ConversationsSource.MAIN,
    conversationListViewModel: ConversationListViewModel = when {
        LocalInspectionMode.current -> ConversationListViewModelPreview()
        else -> hiltViewModel<ConversationListViewModelImpl, ConversationListViewModelImpl.Factory>(
            key = "list_$conversationsSource",
            creationCallback = { factory ->
                factory.create(conversationsSource = conversationsSource)
            }
        )
    },
    conversationListCallViewModel: ConversationListCallViewModel = when {
        LocalInspectionMode.current -> ConversationListCallViewModelPreview
        else -> hiltViewModel<ConversationListCallViewModelImpl>(key = "call_$conversationsSource")
    },
) {
    val sheetState = rememberWireModalSheetState<ConversationSheetState>()
    val permissionPermanentlyDeniedDialogState = rememberVisibilityState<PermissionPermanentlyDeniedDialogState>()

    val context = LocalContext.current

    LaunchedEffect(searchBarState.isSearchActive) {
        if (searchBarState.isSearchActive) {
            conversationListViewModel.refreshMissingMetadata()
        }
    }

    LaunchedEffect(searchBarState.searchQueryTextState.text) {
        conversationListViewModel.searchQueryChanged(searchBarState.searchQueryTextState.text.toString())
    }

    HandleActions(conversationListCallViewModel.actions) { action ->
        when (action) {
            is ConversationListCallViewActions.JoinedCall -> {
                AnonymousAnalyticsManagerImpl.sendEvent(event = AnalyticsEvent.CallJoined)
                getOngoingCallIntent(context, action.conversationId.toString(), action.userId.toString()).run {
                    context.startActivity(this)
                }
            }
        }
    }

    val onEditConversationItem: (ConversationItem) -> Unit = remember {
        {
            sheetState.show(ConversationSheetState(it.conversationId))
        }
    }

    val onOpenConversation: (ConversationItem) -> Unit = remember(navigator) {
        {
            navigator.navigate(NavigationCommand(ConversationScreenDestination(it.conversationId)))
        }
    }
    val onOpenUserProfile: (UserId) -> Unit = remember(navigator) {
        {
            navigator.navigate(NavigationCommand(OtherUserProfileScreenDestination(it)))
        }
    }
    val onJoinCall: (ConversationId) -> Unit = remember {
        {
            conversationListCallViewModel.joinOngoingCall(it)
        }
    }
    val onNewConversationClicked: () -> Unit = remember {
        {
            navigator.navigate(NavigationCommand(NewConversationSearchPeopleScreenDestination))
        }
    }

    val onPlayPauseCurrentAudio: () -> Unit = remember {
        {
            conversationListViewModel.playPauseCurrentAudio()
        }
    }

    val onStopCurrentAudio: () -> Unit = remember {
        {
            conversationListViewModel.stopCurrentAudio()
        }
    }

    when (val state = conversationListViewModel.conversationListState) {
        is ConversationListState.Paginated -> {
            val lazyPagingItems = state.conversations.collectAsLazyPagingItemsWithLifecycle()
            val loadingState = lazyPagingItems.loadingState()
            val itemSnapshotCache by conversationListViewModel.itemSnapshotCache.collectAsStateWithLifecycle()

            // While still loading we keep the previous search-bar visibility and skip the
            // "no conversations" branch so the screen doesn't flicker on back-navigation.
            if (loadingState == PagedLoadingState.Loaded) {
                searchBarState.searchVisibleChanged(lazyPagingItems.itemCount > 0 || searchBarState.isSearchActive)
            }

            val onBrowsePublicChannels = remember(navigator) {
                { navigator.navigate(NavigationCommand(BrowseChannelsScreenDestination)) }
            }
            val onAudioPermissionPermanentlyDenied = remember {
                {
                    permissionPermanentlyDeniedDialogState.show(
                        PermissionPermanentlyDeniedDialogState.Visible(
                            R.string.app_permission_dialog_title,
                            R.string.call_permission_dialog_description
                        )
                    )
                }
            }

            when {
                // live items present — render the paged list
                lazyPagingItems.itemCount > 0 -> ConversationList(
                    lazyPagingConversations = lazyPagingItems,
                    lazyListState = lazyListState,
                    onOpenConversation = onOpenConversation,
                    onEditConversation = onEditConversationItem,
                    onOpenUserProfile = onOpenUserProfile,
                    onJoinCall = onJoinCall,
                    onAudioPermissionPermanentlyDenied = onAudioPermissionPermanentlyDenied,
                    onPlayPauseCurrentAudio = onPlayPauseCurrentAudio,
                    onStopCurrentAudio = onStopCurrentAudio,
                    onBrowsePublicChannels = onBrowsePublicChannels,
                )
                // paging-compose has not yet replayed the cached PagingData (transient empty
                // frame). If we have a ViewModel-side snapshot from a previous session, render
                // it so the user sees continuity instead of a skeleton or empty state.
                loadingState == PagedLoadingState.Reloading && itemSnapshotCache.isNotEmpty() -> ConversationList(
                    conversationItemsSnapshot = itemSnapshotCache,
                    lazyListState = lazyListState,
                    onOpenConversation = onOpenConversation,
                    onEditConversation = onEditConversationItem,
                    onOpenUserProfile = onOpenUserProfile,
                    onJoinCall = onJoinCall,
                    onAudioPermissionPermanentlyDenied = onAudioPermissionPermanentlyDenied,
                    onPlayPauseCurrentAudio = onPlayPauseCurrentAudio,
                    onStopCurrentAudio = onStopCurrentAudio,
                    onBrowsePublicChannels = onBrowsePublicChannels,
                )
                // very first load ever — no cached items to fall back on, show the skeleton
                loadingState != PagedLoadingState.Loaded -> loadingListContent(lazyListState)
                // when there is no conversation in any folder
                searchBarState.isSearchActive -> SearchConversationsEmptyContent(onNewConversationClicked = onNewConversationClicked)
                else -> emptyListContent(state.domain)
            }
        }

        is ConversationListState.NotPaginated -> {
            val hasConversations = state.conversations.isNotEmpty() && state.conversations.any { it.value.isNotEmpty() }
            searchBarState.searchVisibleChanged(isSearchVisible = hasConversations || searchBarState.isSearchActive)
            when {
                // when conversation list is not yet fetched, show loading indicator
                state.isLoading -> loadingListContent(lazyListState)
                // when there is at least one conversation in any folder
                hasConversations -> ConversationList(
                    lazyListState = lazyListState,
                    conversationListItems = state.conversations,
                    onOpenConversation = onOpenConversation,
                    onEditConversation = onEditConversationItem,
                    onOpenUserProfile = onOpenUserProfile,
                    onJoinCall = onJoinCall,
                    onAudioPermissionPermanentlyDenied = {
                        permissionPermanentlyDeniedDialogState.show(
                            PermissionPermanentlyDeniedDialogState.Visible(
                                R.string.app_permission_dialog_title,
                                R.string.call_permission_dialog_description
                            )
                        )
                    },
                    onPlayPauseCurrentAudio = onPlayPauseCurrentAudio,
                    onStopCurrentAudio = onStopCurrentAudio
                )
                // when there is no conversation in any folder
                searchBarState.isSearchActive -> SearchConversationsEmptyContent(onNewConversationClicked = onNewConversationClicked)
                else -> emptyListContent(state.domain)
            }
        }
    }

    VisibilityState(conversationListCallViewModel.joinCallDialogState) { callConversationId ->
        appLogger.i("$TAG showing showJoinAnywayDialog..")
        JoinAnywayDialog(
            onDismiss = conversationListCallViewModel.joinCallDialogState::dismiss,
            onConfirm = { conversationListCallViewModel.joinAnyway(callConversationId) }
        )
    }

    PermissionPermanentlyDeniedDialog(
        dialogState = permissionPermanentlyDeniedDialogState,
        hideDialog = permissionPermanentlyDeniedDialogState::dismiss
    )

    ConversationOptionsModalSheetLayout(
        sheetState = sheetState,
        openConversationFolders = { navigator.navigate(NavigationCommand(ConversationFoldersScreenDestination(it))) },
        onPromoteAdmin = { navigator.navigate(NavigationCommand(PromoteAdminScreenDestination(PromoteAdminNavArgs(it)))) },
        openConversationDebugMenu = { conversationId ->
            navigator.navigate(
                NavigationCommand(
                    DebugConversationScreenDestination(
                navArgs = DebugConversationScreenNavArgs(conversationId)
            )
                )
            )
        },
    )

    SnackBarMessageHandler(infoMessages = conversationListViewModel.infoMessage, onEmitted = {
        sheetState.hide()
    })
}

private enum class PagedLoadingState {
    /** First time the user sees this list — no cached items exist, show the skeleton. */
    InitialLoad,
    /** Returning to the screen mid-refresh after cache invalidation — keep the list view
     *  so the user doesn't see the skeleton or empty state flash. */
    Reloading,
    /** Loaded (or settled empty). The UI can use itemCount to choose a branch. */
    Loaded,
}

@Composable
private fun LazyPagingItems<ConversationItemType>.loadingState(): PagedLoadingState {
    // paging-compose 3.3.x emits an empty LazyPagingItems on each new composition before
    // the (re)load finishes. We distinguish "first ever load" from "reload after the cache
    // was invalidated while we were away" so that the second case keeps rendering the list
    // view (which will populate on the next frame) instead of flashing the skeleton.
    var hasEverHadItems by rememberSaveable { mutableStateOf(false) }
    if (itemCount > 0) {
        hasEverHadItems = true
    }
    val refreshing = loadState.refresh is LoadState.Loading && itemCount == 0
    return when {
        refreshing && !hasEverHadItems -> PagedLoadingState.InitialLoad
        refreshing && hasEverHadItems -> PagedLoadingState.Reloading
        else -> PagedLoadingState.Loaded
    }
}

private const val TAG = "BaseConversationsScreen"

@PreviewMultipleThemes
@Composable
fun PreviewConversationsScreenContent() = WireTheme {
    ConversationsScreenContent(navigator = rememberNavigator { }, searchBarState = rememberSearchbarState())
}
