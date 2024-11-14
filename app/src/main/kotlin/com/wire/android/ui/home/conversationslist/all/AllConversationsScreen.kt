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
<<<<<<< HEAD
            lazyListState = currentLazyListState,
            emptyListContent = { ConversationsEmptyContent(filter = ConversationFilter.ALL) }
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
            lazyListState = currentLazyListState,
            emptyListContent = { ConversationsEmptyContent(filter = ConversationFilter.FAVORITES) }
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
            lazyListState = currentLazyListState,
            emptyListContent = { ConversationsEmptyContent(filter = ConversationFilter.GROUPS) }
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
            lazyListState = currentLazyListState,
            emptyListContent = { ConversationsEmptyContent(filter = ConversationFilter.ONE_ON_ONE, domain = it) }
=======
            lazyListState = lazyListStateFor(HomeDestination.Conversations),
            emptyListContent = { AllConversationsEmptyContent() }
>>>>>>> 3a0040700 (fix: blank lists or flickering on Home Destinations - wrong lazyListStates [WPB-14276] (#3627))
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
