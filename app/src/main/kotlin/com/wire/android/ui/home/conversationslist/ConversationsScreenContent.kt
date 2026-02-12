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
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
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
import com.wire.android.ui.destinations.BrowseChannelsScreenDestination
import com.wire.android.ui.destinations.ConversationFoldersScreenDestination
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.DebugConversationScreenDestination
import com.wire.android.ui.destinations.NewConversationSearchPeopleScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState
import com.wire.android.ui.home.conversationslist.common.ConversationList
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationItemType
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.ui.home.conversationslist.search.SearchConversationsEmptyContent
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.SnackBarMessageHandler
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
            val lazyPagingItems = state.conversations.collectAsLazyPagingItems()
            searchBarState.searchVisibleChanged(lazyPagingItems.itemCount > 0 || searchBarState.isSearchActive)
            when {
                // when conversation list is not yet fetched, show loading indicator
                lazyPagingItems.isLoading() -> loadingListContent(lazyListState)
                // when there is at least one conversation
                lazyPagingItems.itemCount > 0 -> ConversationList(
                    lazyPagingConversations = lazyPagingItems,
                    lazyListState = lazyListState,
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
                    onStopCurrentAudio = onStopCurrentAudio,
                    onBrowsePublicChannels = {
                        navigator.navigate(NavigationCommand(BrowseChannelsScreenDestination))
                    }
                )
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

@Composable
private fun LazyPagingItems<ConversationItemType>.isLoading(): Boolean {
    var initialLoadCompleted by remember { mutableStateOf(false) }
    if (loadState.refresh is LoadState.NotLoading) {
        initialLoadCompleted = true
    }
    return !initialLoadCompleted && loadState.refresh == LoadState.Loading && itemCount == 0
}

private const val TAG = "BaseConversationsScreen"

@PreviewMultipleThemes
@Composable
fun PreviewConversationsScreenContent() = WireTheme {
    ConversationsScreenContent(navigator = rememberNavigator { }, searchBarState = rememberSearchbarState())
}
