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

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.home.conversations.MessageItem
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun SearchConversationMessagesResultsScreen(
    searchResult: List<UIMessage>,
    onMessageClick: (messageId: String) -> Unit
) {
    LazyColumn {
        items(searchResult) { message ->
            when (message) {
                is UIMessage.Regular -> {
                    MessageItem(
                        message = message,
                        conversationDetailsData = ConversationDetailsData.None,
                        audioMessagesState = mapOf(),
                        onLongClicked = { },
                        onAssetMessageClicked = { },
                        onAudioClick = { },
                        onChangeAudioPosition = { _, _ -> },
                        onImageMessageClicked = { _, _ -> },
                        onOpenProfile = { },
                        onReactionClicked = { _, _ -> },
                        onResetSessionClicked = { _, _ -> },
                        onSelfDeletingMessageRead = { },
                        defaultBackgroundColor = colorsScheme().backgroundVariant,
                        shouldDisplayMessageStatus = false,
                        shouldDisplayFooter = false,
                        onMessageClick = onMessageClick
                    )
                }
                is UIMessage.System -> { }
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
fun previewSearchConversationMessagesResultsScreen() {
    WireTheme {
        SearchConversationMessagesResultsScreen(
            searchResult = listOf(
                mockMessageWithText,
                mockMessageWithText,
            ),
            onMessageClick = {}
        )
    }
}
