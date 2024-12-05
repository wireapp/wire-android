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

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import com.wire.android.navigation.FolderNavArgs
import com.wire.android.navigation.HomeDestination
import com.wire.android.navigation.HomeNavGraph
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.rememberNavigator
import com.wire.android.ui.common.topappbar.search.rememberSearchbarState
import com.wire.android.ui.home.HomeStateHolder
import com.wire.android.ui.home.conversationslist.ConversationListViewModelPreview
import com.wire.android.ui.home.conversationslist.ConversationsScreenContent
import com.wire.android.ui.home.conversationslist.common.previewConversationFoldersFlow
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.conversation.ConversationFilter
import kotlinx.coroutines.flow.flowOf

@HomeNavGraph(start = true)
@WireDestination
@Composable
fun AllConversationsScreen(homeStateHolder: HomeStateHolder) {
    with(homeStateHolder) {
        ConversationsScreenContent(
            navigator = navigator,
            searchBarState = searchBarState,
            conversationsSource = ConversationsSource.MAIN,
            lazyListState = lazyListStateFor(HomeDestination.Conversations),
            emptyListContent = { ConversationsEmptyContent(filter = ConversationFilter.All) }
        )
    }
}

@HomeNavGraph
@WireDestination
@Composable
fun FavoritesConversationsScreen(homeStateHolder: HomeStateHolder) {
    with(homeStateHolder) {
        ConversationsScreenContent(
            navigator = navigator,
            searchBarState = searchBarState,
            conversationsSource = ConversationsSource.FAVORITES,
            lazyListState = lazyListStateFor(HomeDestination.Favorites),
            emptyListContent = { ConversationsEmptyContent(filter = ConversationFilter.Favorites) }
        )
    }
}

@HomeNavGraph
@WireDestination(navArgsDelegate = FolderNavArgs::class)
@Composable
fun FolderConversationsScreen(homeStateHolder: HomeStateHolder, args: FolderNavArgs) {
    with(homeStateHolder) {
        ConversationsScreenContent(
            navigator = navigator,
            searchBarState = searchBarState,
            conversationsSource = ConversationsSource.FOLDER(args.folderId, args.folderName),
            emptyListContent = { ConversationsEmptyContent(filter = ConversationFilter.Folder(args.folderId, args.folderName)) }
        )
    }
}

@HomeNavGraph
@WireDestination
@Composable
fun GroupConversationsScreen(homeStateHolder: HomeStateHolder) {
    with(homeStateHolder) {
        ConversationsScreenContent(
            navigator = navigator,
            searchBarState = searchBarState,
            conversationsSource = ConversationsSource.GROUPS,
            lazyListState = lazyListStateFor(HomeDestination.Group),
            emptyListContent = { ConversationsEmptyContent(filter = ConversationFilter.Groups) }
        )
    }
}

@HomeNavGraph
@WireDestination
@Composable
fun OneOnOneConversationsScreen(homeStateHolder: HomeStateHolder) {
    with(homeStateHolder) {
        ConversationsScreenContent(
            navigator = navigator,
            searchBarState = searchBarState,
            conversationsSource = ConversationsSource.ONE_ON_ONE,
            lazyListState = lazyListStateFor(HomeDestination.OneOnOne),
            emptyListContent = { ConversationsEmptyContent(filter = ConversationFilter.OneOnOne, domain = it) }
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
        emptyListContent = { ConversationsEmptyContent() },
        conversationListViewModel = ConversationListViewModelPreview(flowOf()),
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewAllConversationsEmptySearchScreen() = WireTheme {
    ConversationsScreenContent(
        navigator = rememberNavigator {},
        searchBarState = rememberSearchbarState(searchQueryTextState = TextFieldState(initialText = "er")),
        conversationsSource = ConversationsSource.MAIN,
        emptyListContent = { ConversationsEmptyContent() },
        conversationListViewModel = ConversationListViewModelPreview(flowOf()),
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewAllConversationsSearchScreen() = WireTheme {
    ConversationsScreenContent(
        navigator = rememberNavigator {},
        searchBarState = rememberSearchbarState(searchQueryTextState = TextFieldState(initialText = "er")),
        conversationsSource = ConversationsSource.MAIN,
        emptyListContent = { ConversationsEmptyContent() },
        conversationListViewModel = ConversationListViewModelPreview(previewConversationFoldersFlow("er")),
    )
}
