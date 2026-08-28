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

package com.wire.android.ui.home.conversationslist.all

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import com.wire.android.navigation.HomeDestination
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.search.rememberSearchbarState
import com.wire.android.ui.home.HomeShellState
import com.wire.android.ui.home.conversations.conversationFoldersViewModel
import com.wire.android.ui.home.conversations.folder.ConversationFoldersStateArgs
import com.wire.android.ui.home.conversations.folder.ConversationFoldersVM
import com.wire.android.ui.home.conversationslist.ConversationListViewModelPreview
import com.wire.android.ui.home.conversationslist.ConversationsScreenContent
import com.wire.android.ui.home.conversationslist.ConversationListNavigationActions
import com.wire.android.ui.home.conversationslist.common.previewConversationItemsFlow
import com.wire.android.ui.home.conversationslist.filter.ConversationFilterSheetContent
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.conversation.ConversationFilter

@Composable
internal fun AllConversationsContent(
    homeShellState: HomeShellState,
    navigationActions: ConversationListNavigationActions,
    foldersViewModel: ConversationFoldersVM =
        conversationFoldersViewModel(ConversationFoldersStateArgs(null)),
) {
    with(homeShellState) {
        Crossfade(
            targetState = homeShellState.currentConversationFilter,
            label = "Conversation filter change animation",
        ) { filter ->
            ConversationsScreenContent(
                navigationActions = navigationActions,
                searchBarState = searchBarState,
                conversationsSource = when (filter) {
                    is ConversationFilter.All -> ConversationsSource.MAIN
                    is ConversationFilter.Favorites -> ConversationsSource.FAVORITES
                    is ConversationFilter.Groups -> ConversationsSource.GROUPS
                    is ConversationFilter.OneOnOne -> ConversationsSource.ONE_ON_ONE
                    is ConversationFilter.Folder -> ConversationsSource.FOLDER(filter.folderId, filter.folderName)
                    is ConversationFilter.Channels -> ConversationsSource.CHANNELS
                },
                lazyListState = lazyListStateFor(HomeDestination.Conversations, filter),
                emptySearchResultFocusRequester = emptySearchResultFocusRequester,
                firstConversationFocusRequester = firstConversationFocusRequester,
                onConversationOpened = homeShellState::requestClearSearchOnNextResume,
                emptyListContent = {
                    ConversationsEmptyContent(
                        filter = filter,
                        onBrowseChannels = navigationActions.browseChannels,
                    )
                }
            )
        }
        WireModalSheetLayout(
            sheetState = conversationsFilterBottomSheetState,
            sheetContent = { sheetData ->
                ConversationFilterSheetContent(
                    currentFilter = homeShellState.currentConversationFilter,
                    folders = foldersViewModel.state().folders,
                    onChangeFilter = { filter ->
                        conversationsFilterBottomSheetState.hide()
                        homeShellState.changeConversationFilter(filter)
                    },
                )
            }
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewAllConversationsEmptyScreen() = WireTheme {
    ConversationsScreenContent(
        navigationActions = previewConversationListNavigationActions(),
        searchBarState = rememberSearchbarState(),
        conversationsSource = ConversationsSource.MAIN,
        emptyListContent = { ConversationsEmptyContent(onBrowseChannels = {}) },
        conversationListViewModel = ConversationListViewModelPreview(previewConversationItemsFlow(list = listOf())),
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewAllConversationsEmptySearchScreen() = WireTheme {
    ConversationsScreenContent(
        navigationActions = previewConversationListNavigationActions(),
        searchBarState = rememberSearchbarState(initialIsSearchActive = true, searchQueryTextState = TextFieldState(initialText = "er")),
        conversationsSource = ConversationsSource.MAIN,
        emptyListContent = { ConversationsEmptyContent(onBrowseChannels = {}) },
        conversationListViewModel = ConversationListViewModelPreview(previewConversationItemsFlow(searchQuery = "er", list = listOf())),
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewAllConversationsSearchScreen() = WireTheme {
    ConversationsScreenContent(
        navigationActions = previewConversationListNavigationActions(),
        searchBarState = rememberSearchbarState(initialIsSearchActive = true, searchQueryTextState = TextFieldState(initialText = "er")),
        conversationsSource = ConversationsSource.MAIN,
        emptyListContent = { ConversationsEmptyContent(onBrowseChannels = {}) },
        conversationListViewModel = ConversationListViewModelPreview(previewConversationItemsFlow("er")),
    )
}

private fun previewConversationListNavigationActions() = ConversationListNavigationActions(
    openConversation = {},
    openUserProfile = {},
    startConversation = {},
    browseChannels = {},
    openConversationFolders = {},
    promoteAdmin = {},
    openDebugMenu = {},
)
