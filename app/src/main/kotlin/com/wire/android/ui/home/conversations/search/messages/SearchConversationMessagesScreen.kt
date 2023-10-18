/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.home.conversations.model.UIMessage

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
    val searchBarState = rememberSearchBarConversationMessagesState()

    with(searchConversationMessagesViewModel.searchConversationMessagesState) {
        CollapsingTopBarScaffold(
            topBarHeader = { },
            topBarCollapsing = {
                val onInputClicked: () -> Unit = remember { { searchBarState.openSearch() } }
                val onCloseSearchClicked: () -> Unit = remember {
                    {
                        searchBarState.closeSearch()
                        navigator.navigateBack()
                    }
                }

                SearchTopBar(
                    isSearchActive = searchBarState.isSearchActive,
                    searchBarHint = stringResource(id = R.string.label_search_messages),
                    searchQuery = searchQuery,
                    onSearchQueryChanged = searchConversationMessagesViewModel::searchQueryChanged,
                    onInputClicked = onInputClicked,
                    onCloseSearchClicked = onCloseSearchClicked,
                    modifier = Modifier.padding(top = dimensions().spacing24x)
                )
            },
            content = {
                SearchConversationMessagesResultScreen(
                    searchQuery = searchQuery.text,
                    noneSearchSucceed = noneSearchSucceed,
                    searchResult = searchResult
                )
                BackHandler(enabled = searchBarState.isSearchActive) {
                    searchBarState.closeSearch()
                }
            },
            bottomBar = { },
            snapOnFling = false,
            keepElevationWhenCollapsed = true
        )
    }
}

@Composable
fun SearchConversationMessagesResultScreen(
    searchQuery: String,
    noneSearchSucceed: Boolean,
    searchResult: List<UIMessage>
) {
    if (searchQuery.isEmpty()) {
        SearchConversationMessagesEmptyScreen()
    } else {
        if (noneSearchSucceed) {
            SearchConversationMessagesNoResultsScreen()
        } else {
            SearchConversationMessagesResultsScreen(
                searchResult = searchResult
            )
        }
    }
}
