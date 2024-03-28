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
package com.wire.android.ui.home.conversations.search.messages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.model.UIMessage
import kotlinx.coroutines.flow.Flow

@RootNavGraph
@Destination(
    navArgsDelegate = SearchConversationMessagesNavArgs::class,
    style = PopUpNavigationAnimation::class
)
@Composable
fun SearchConversationMessagesScreen(
    navigator: Navigator,
    searchConversationMessagesViewModel: SearchConversationMessagesViewModel = hiltViewModel()
) {
    with(searchConversationMessagesViewModel.searchConversationMessagesState) {
        WireScaffold(
            topBar = {
                SearchTopBar(
                    isSearchActive = true, // we want the search to be always active and back arrow visible on this particular screen
                    searchBarHint = stringResource(id = R.string.label_search_messages),
                    searchQuery = searchQuery,
                    onSearchQueryChanged = searchConversationMessagesViewModel::searchQueryChanged,
                    modifier = Modifier.padding(top = dimensions().spacing24x),
                    onCloseSearchClicked = navigator::navigateBack,
                    isLoading = isLoading
                )
            },
            content = { internalPadding ->
                Column(modifier = Modifier.padding(internalPadding)) {
                    SearchConversationMessagesResultContent(
                        searchQuery = searchQuery.text,
                        searchResult = searchResult,
                        onMessageClick = { messageId ->
                            navigator.navigate(
                                NavigationCommand(
                                    ConversationScreenDestination(
                                        navArgs = ConversationNavArgs(
                                            conversationId = conversationId,
                                            searchedMessageId = messageId
                                        )
                                    ),
                                    BackStackMode.UPDATE_EXISTED
                                )
                            )
                        }
                    )
                }
            }
        )
    }
}

@Composable
fun SearchConversationMessagesResultContent(
    searchQuery: String,
    searchResult: Flow<PagingData<UIMessage>>,
    onMessageClick: (messageId: String) -> Unit
) {
    val lazyPagingMessages = searchResult.collectAsLazyPagingItems()

    if (searchQuery.isEmpty()) {
        SearchConversationMessagesEmptyScreen()
    } else {
        if (lazyPagingMessages.itemCount > 0) {
            SearchConversationMessagesResultsScreen(
                lazyPagingMessages = lazyPagingMessages,
                searchQuery = searchQuery,
                onMessageClick = onMessageClick
            )
        } else {
            SearchConversationMessagesNoResultsScreen()
        }
    }
}
