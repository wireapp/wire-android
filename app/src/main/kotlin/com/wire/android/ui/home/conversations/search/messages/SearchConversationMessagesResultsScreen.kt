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
import androidx.compose.runtime.Composable
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.messages.item.MessageContainerItem
import com.wire.android.ui.home.conversations.messages.item.SwipableMessageConfiguration
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.flowOf

@Composable
fun SearchConversationMessagesResultsScreen(
    lazyPagingMessages: LazyPagingItems<UIMessage>,
    searchQuery: String = "",
    onMessageClick: (messageId: String) -> Unit
) {
    LazyColumn {
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
                        audioMessagesState = persistentMapOf(),
                        onLongClicked = { },
                        onAssetMessageClicked = { },
                        onAudioClick = { },
                        onChangeAudioPosition = { _, _ -> },
                        onImageMessageClicked = { _, _ -> },
                        onOpenProfile = { },
                        onReactionClicked = { _, _ -> },
                        onResetSessionClicked = { _, _ -> },
                        onSelfDeletingMessageRead = { },
                        isContentClickable = true,
                        onMessageClick = onMessageClick,
                        defaultBackgroundColor = colorsScheme().backgroundVariant,
                        shouldDisplayMessageStatus = false,
                        shouldDisplayFooter = false,
                        onReplyClickable = null,
                        swipableMessageConfiguration = SwipableMessageConfiguration.NotSwipable
                    )
                }

                is UIMessage.System -> {}
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
fun previewSearchConversationMessagesResultsScreen() {
    WireTheme {
        SearchConversationMessagesResultsScreen(
            lazyPagingMessages = flowOf(
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
