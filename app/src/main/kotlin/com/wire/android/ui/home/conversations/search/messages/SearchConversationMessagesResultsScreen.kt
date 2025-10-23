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

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.messages.item.MessageClickActions
import com.wire.android.ui.home.conversations.messages.item.MessageContainerItem
import com.wire.android.ui.home.conversations.messages.item.SwipeableMessageConfiguration
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun SearchConversationMessagesResultsScreen(
    lazyPagingMessages: LazyPagingItems<UIMessage>,
    onMessageClick: (messageId: String) -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    searchQuery: String = ""
) {
    LazyColumn(state = lazyListState, modifier = modifier) {
        items(
            count = lazyPagingMessages.itemCount,
            key = lazyPagingMessages.itemKey { it.header.messageId },
            contentType = lazyPagingMessages.itemContentType { it }
        ) { index ->
            val message: UIMessage = lazyPagingMessages[index]
                ?: return@items // We can draw a placeholder here, as we fetch the next page of messages

            when (message) {
                is UIMessage.Regular -> {
                    MessageContainerItem(
                        message = message,
                        conversationDetailsData = ConversationDetailsData.None(null),
                        searchQuery = searchQuery,
                        clickActions = MessageClickActions.FullItem(
                            onFullMessageLongClicked = null,
                            onFullMessageClicked = onMessageClick,
                        ),
                        onSelfDeletingMessageRead = { },
                        shouldDisplayMessageStatus = false,
                        shouldDisplayFooter = false,
                        swipeableMessageConfiguration = SwipeableMessageConfiguration.NotSwipeable,
                        failureInteractionAvailable = false,
                    )
                }

                is UIMessage.System -> {}
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchConversationMessagesResultsScreen() {
    WireTheme {
        SearchConversationMessagesResultsScreen(
            lazyPagingMessages = MutableStateFlow(
                PagingData.from(
                    listOf<UIMessage>(
                        mockMessageWithText.copy(header = mockMessageWithText.header.copy(messageId = "1")),
                        mockMessageWithText.copy(header = mockMessageWithText.header.copy(messageId = "2")),
                    )
                )
            ).collectAsLazyPagingItems(),
            onMessageClick = {}
        )
    }
}
