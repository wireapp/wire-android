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
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.navigation.WireRootNavGraph

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.navigation.HomeDestination
import com.wire.android.navigation.HomeNavGraph
import com.wire.android.navigation.rememberNavigator
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.search.rememberSearchbarState
import com.wire.android.ui.home.HomeStateHolder
import com.wire.android.ui.home.conversations.folder.ConversationFoldersStateArgs
import com.wire.android.ui.home.conversations.folder.ConversationFoldersVM
import com.wire.android.ui.home.conversations.folder.ConversationFoldersVMImpl
import com.wire.android.ui.home.conversationslist.ConversationListViewModelPreview
import com.wire.android.ui.home.conversationslist.ConversationsScreenContent
import com.wire.android.ui.home.conversationslist.common.previewConversationItemsFlow
import com.wire.android.ui.home.conversationslist.filter.ConversationFilterSheetContent
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.conversation.ConversationFilter

@HomeNavGraph(start = true)
@Destination<WireRootNavGraph>
@Composable
fun AllConversationsScreen(
    homeStateHolder: HomeStateHolder,
    foldersViewModel: ConversationFoldersVM =
        hiltViewModel<ConversationFoldersVMImpl, ConversationFoldersVMImpl.Factory>(
            creationCallback = { it.create(ConversationFoldersStateArgs(null)) }
        ),
) {
    with(homeStateHolder) {
        Crossfade(
            targetState = homeStateHolder.currentConversationFilter,
            label = "Conversation filter change animation",
        ) { filter ->
            ConversationsScreenContent(
                navigator = navigator,
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
                emptyListContent = { ConversationsEmptyContent(filter = filter, navigator = navigator) }
            )
        }
        WireModalSheetLayout(
            sheetState = conversationsFilterBottomSheetState,
            sheetContent = { sheetData ->
                ConversationFilterSheetContent(
                    currentFilter = homeStateHolder.currentConversationFilter,
                    folders = foldersViewModel.state().folders,
                    onChangeFilter = { filter ->
                        conversationsFilterBottomSheetState.hide()
                        homeStateHolder.changeConversationFilter(filter)
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
        navigator = rememberNavigator {},
        searchBarState = rememberSearchbarState(),
        conversationsSource = ConversationsSource.MAIN,
        emptyListContent = { ConversationsEmptyContent(navigator = rememberNavigator {}) },
        conversationListViewModel = ConversationListViewModelPreview(previewConversationItemsFlow(list = listOf())),
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewAllConversationsEmptySearchScreen() = WireTheme {
    ConversationsScreenContent(
        navigator = rememberNavigator {},
        searchBarState = rememberSearchbarState(initialIsSearchActive = true, searchQueryTextState = TextFieldState(initialText = "er")),
        conversationsSource = ConversationsSource.MAIN,
        emptyListContent = { ConversationsEmptyContent(navigator = rememberNavigator {}) },
        conversationListViewModel = ConversationListViewModelPreview(previewConversationItemsFlow(searchQuery = "er", list = listOf())),
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewAllConversationsSearchScreen() = WireTheme {
    ConversationsScreenContent(
        navigator = rememberNavigator {},
        searchBarState = rememberSearchbarState(initialIsSearchActive = true, searchQueryTextState = TextFieldState(initialText = "er")),
        conversationsSource = ConversationsSource.MAIN,
        emptyListContent = { ConversationsEmptyContent(navigator = rememberNavigator {}) },
        conversationListViewModel = ConversationListViewModelPreview(previewConversationItemsFlow("er")),
    )
}
