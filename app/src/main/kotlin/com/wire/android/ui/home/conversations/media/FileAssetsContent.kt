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
package com.wire.android.ui.home.conversations.media

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.wire.android.R
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.home.conversations.MessageItem
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.model.UIMessage
import kotlinx.coroutines.flow.Flow

@Composable
fun FileAssetsContent(
    groupedAssetMessageList: Flow<PagingData<UIMessage>>,
    audioMessagesState: Map<String, AudioState> = emptyMap(),
    onAudioItemClicked: (String) -> Unit,
    onAssetItemClicked: (String) -> Unit
) {
    val lazyPagingMessages = groupedAssetMessageList.collectAsLazyPagingItems()

    if (lazyPagingMessages.itemCount > 0) {
        AssetMessagesListContent(
            groupedAssetMessageList = lazyPagingMessages,
            audioMessagesState = audioMessagesState,
            onAudioItemClicked = onAudioItemClicked,
            onAssetItemClicked = onAssetItemClicked
        )
    } else {
        EmptyMediaContentScreen(
            text = stringResource(R.string.label_conversation_files_empty)
        )
    }
}

@Composable
private fun AssetMessagesListContent(
    groupedAssetMessageList: LazyPagingItems<UIMessage>,
    audioMessagesState: Map<String, AudioState>,
    onAudioItemClicked: (String) -> Unit,
    onAssetItemClicked: (String) -> Unit,
) {
    LazyColumn {
        items(
            count = groupedAssetMessageList.itemCount,
            key = groupedAssetMessageList.itemKey { it.header.messageId },
            contentType = groupedAssetMessageList.itemContentType { it }
        ) { index ->
            val message: UIMessage = groupedAssetMessageList[index] ?: return@items
            when (message) {
                is UIMessage.Regular -> {
                    MessageItem(
                        message = message,
                        conversationDetailsData = ConversationDetailsData.None,
                        audioMessagesState = audioMessagesState,
                        onAudioClick = onAudioItemClicked,
                        onChangeAudioPosition = { _, _ -> },
                        onLongClicked = { },
                        onAssetMessageClicked = onAssetItemClicked,
                        onImageMessageClicked = { _, _ -> },
                        onOpenProfile = { _ -> },
                        onReactionClicked = { _, _ -> },
                        onResetSessionClicked = { _, _ -> },
                        onSelfDeletingMessageRead = { },
                        defaultBackgroundColor = colorsScheme().backgroundVariant,
                        shouldDisplayMessageStatus = false,
                        shouldDisplayFooter = false,
                        onLinkClick = { }
                    )
                }

                is UIMessage.System -> {}
            }
        }
    }
}