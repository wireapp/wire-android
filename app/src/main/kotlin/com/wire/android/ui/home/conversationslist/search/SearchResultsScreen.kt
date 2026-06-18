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

package com.wire.android.ui.home.conversationslist.search

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.generated.app.destinations.ConversationScreenDestination
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.rememberNavigator
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.search.SearchBarState
import com.wire.android.ui.common.search.rememberSearchbarState
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.searchResultsViewModel
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.PreviewMultipleThemes

@Suppress("LongParameterList", "UNUSED_PARAMETER")
@Composable
fun SearchResultsScreen(
    navigator: Navigator,
    searchBarState: SearchBarState,
    emptySearchResultFocusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    firstConversationFocusRequester: FocusRequester? = null,
    onConversationSearchResultOpened: () -> Unit = {},
    viewModel: SearchResultsViewModel? = when {
        LocalInspectionMode.current -> null
        else -> searchResultsViewModel()
    },
) {
    var selectedTab by rememberSaveable { mutableStateOf(SearchResultsTab.CONVERSATIONS) }
    val messagesSearchState by viewModel
        ?.messagesSearchState
        ?.collectAsStateWithLifecycle()
        ?: mutableStateOf(MessagesSearchState.EmptyQuery)

    LaunchedEffect(searchBarState.searchQueryTextState.text) {
        viewModel?.onSearchQueryChanged(searchBarState.searchQueryTextState.text.toString())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(
                emptySearchResultFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
            )
            .focusable()
    ) {
        WireTabRow(
            tabs = SearchResultsTab.entries,
            selectedTabIndex = selectedTab.ordinal,
            onTabChange = { selectedTab = SearchResultsTab.entries[it] }
        )

        when (selectedTab) {
            SearchResultsTab.CONVERSATIONS -> ConversationsSearchResults(
                navigator = navigator,
                searchBarState = searchBarState,
                lazyListState = lazyListState,
                emptySearchResultFocusRequester = emptySearchResultFocusRequester,
                firstConversationFocusRequester = firstConversationFocusRequester,
                onConversationOpened = onConversationSearchResultOpened,
                modifier = Modifier.weight(1f)
            )

            SearchResultsTab.MESSAGES -> MessagesSearchResults(
                state = messagesSearchState,
                onMessageClick = { message ->
                    navigator.navigate(message.toConversationNavigationCommand())
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

internal fun UIMessage.toConversationNavigationCommand() = NavigationCommand(
    ConversationScreenDestination(
        navArgs = ConversationNavArgs(
            conversationId = conversationId,
            searchedMessageId = header.messageId
        )
    ),
    BackStackMode.UPDATE_EXISTED
)

private enum class SearchResultsTab(@StringRes val titleResId: Int) : TabItem {
    CONVERSATIONS(R.string.conversations_screen_title),
    MESSAGES(R.string.search_results_messages_tab_title);

    override val title: UIText = UIText.StringResource(titleResId)
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchResultsScreen() = WireTheme {
    SearchResultsScreen(
        navigator = rememberNavigator { },
        searchBarState = rememberSearchbarState(),
        emptySearchResultFocusRequester = null
    )
}
