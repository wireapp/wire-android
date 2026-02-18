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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.wire.android.R
import com.wire.android.feature.cells.ui.destinations.ConversationFilesScreenDestination
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topBarElevation
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.android.ui.common.R as commonR

@WireDestination(
    navArgsDelegate = SearchConversationMessagesNavArgs::class,
    style = PopUpNavigationAnimation::class
)
@Composable
fun SearchConversationMessagesScreen(
    navigator: Navigator,
    searchConversationMessagesViewModel: SearchConversationMessagesViewModel = hiltViewModel()
) {
    SearchConversationMessagesResultContent(
        isCellsConversation = searchConversationMessagesViewModel.isCellsConversation,
        searchQueryTextState = searchConversationMessagesViewModel.searchQueryTextState,
        state = searchConversationMessagesViewModel.searchConversationMessagesState,
        onMessageClick = { messageId ->
            navigator.navigate(
                NavigationCommand(
                    ConversationScreenDestination(
                        navArgs = ConversationNavArgs(
                            conversationId = searchConversationMessagesViewModel.searchConversationMessagesState.conversationId,
                            searchedMessageId = messageId
                        )
                    ),
                    BackStackMode.UPDATE_EXISTED
                )
            )
        },
        onCloseSearchClicked = navigator::navigateBack,
        onSearchFilesButtonClick = {
            navigator.navigate(
                NavigationCommand(
                    ConversationFilesScreenDestination(
                        conversationId = searchConversationMessagesViewModel.conversationId.toString(),
                        breadcrumbs = arrayOf(searchConversationMessagesViewModel.groupName),
                        isSearchByDefaultActive = true
                    )
                )
            )
        }
    )
}

@Composable
fun SearchConversationMessagesResultContent(
    isCellsConversation: Boolean,
    searchQueryTextState: TextFieldState,
    state: SearchConversationMessagesState,
    onMessageClick: (messageId: String) -> Unit,
    onCloseSearchClicked: () -> Unit,
    onSearchFilesButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    val maxAppBarElevation = MaterialTheme.wireDimensions.topBarShadowElevation
    WireScaffold(
        modifier = modifier,
        topBar = {
            Column {
                Surface(
                    shadowElevation = lazyListState.topBarElevation(maxAppBarElevation),
                    color = MaterialTheme.wireColorScheme.background
                ) {
                    SearchTopBar(
                        isSearchActive = true, // we want the search to be always active and back arrow visible on this particular screen
                        searchBarHint = stringResource(id = R.string.label_search_messages),
                        searchQueryTextState = searchQueryTextState,
                        onCloseSearchClicked = onCloseSearchClicked,
                        isLoading = state.isLoading,
                        focusRequester = remember { FocusRequester() },
                    )
                }
                if (isCellsConversation) {
                    WireSecondaryButton(
                        modifier = Modifier.padding(
                            start = dimensions().spacing8x,
                            end = dimensions().spacing8x,
                            top = dimensions().spacing8x,
                            bottom = dimensions().spacing16x,
                        ),
                        text = stringResource(R.string.conversation_search_files_button),
                        onClick = onSearchFilesButtonClick,
                        minSize = MaterialTheme.wireDimensions.buttonMinSize,
                        fillMaxWidth = true,
                        trailingIcon = {
                            Icon(
                                painter = painterResource(commonR.drawable.ic_chevron_right),
                                contentDescription = null
                            )
                        },
                        onClickDescription = stringResource(id = R.string.conversation_search_files_button)
                    )
                }
            }
        },
        content = { internalPadding ->
            Column(modifier = Modifier.padding(internalPadding)) {
                val lazyPagingMessages = state.searchResult.collectAsLazyPagingItems()

                if (searchQueryTextState.text.isEmpty()) {
                    SearchConversationMessagesEmptyScreen()
                } else {
                    if (lazyPagingMessages.itemCount > 0) {
                        SearchConversationMessagesResultsScreen(
                            lazyPagingMessages = lazyPagingMessages,
                            lazyListState = lazyListState,
                            searchQuery = state.searchQuery,
                            onMessageClick = onMessageClick
                        )
                    } else {
                        SearchConversationMessagesNoResultsScreen()
                    }
                }
            }
        }
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchConversationMessagesScreen() = WireTheme {
    SearchConversationMessagesResultContent(
        isCellsConversation = false,
        searchQueryTextState = TextFieldState(),
        state = SearchConversationMessagesState(ConversationId("conversationId", "domain")),
        onMessageClick = {},
        onCloseSearchClicked = {},
        onSearchFilesButtonClick = {}
    )
}
