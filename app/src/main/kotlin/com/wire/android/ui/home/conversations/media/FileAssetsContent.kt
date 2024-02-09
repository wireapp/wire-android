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
package com.wire.android.ui.home.conversations.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.wire.android.R
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.home.conversations.MessageItem
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.usecase.UIPagingItem
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.theme.wireColorScheme
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.Flow

@Composable
fun FileAssetsContent(
    groupedAssetMessageList: Flow<PagingData<UIPagingItem>>,
    audioMessagesState: PersistentMap<String, AudioState> = persistentMapOf(),
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
    groupedAssetMessageList: LazyPagingItems<UIPagingItem>,
    audioMessagesState: PersistentMap<String, AudioState>,
    onAudioItemClicked: (String) -> Unit,
    onAssetItemClicked: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            count = groupedAssetMessageList.itemCount,
            key = groupedAssetMessageList.itemKey {
                when (it) {
                    is UIPagingItem.Label -> it.date
                    is UIPagingItem.Message -> it.uiMessage.header.messageId
                }
            },
            contentType = groupedAssetMessageList.itemContentType { it }
        ) { index ->
            val uiPagingItem: UIPagingItem = groupedAssetMessageList[index] ?: return@items

            when (uiPagingItem) {
                is UIPagingItem.Label -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = dimensions().spacing6x,
                            // first label should not have top padding
                            top = if (index == 0) dimensions().spacing0x else dimensions().spacing6x,
                        )
                ) {
                    FolderHeader(
                        name = uiPagingItem.date.uppercase(),
                        modifier = Modifier
                            .background(MaterialTheme.wireColorScheme.background)
                            .fillMaxWidth()
                    )
                }

                is UIPagingItem.Message -> {
                    when (val message = uiPagingItem.uiMessage) {
                        is UIMessage.Regular -> {
                            MessageItem(
                                message = message,
                                conversationDetailsData = ConversationDetailsData.None,
                                audioMessagesState = audioMessagesState,
                                onLongClicked = { },
                                onAssetMessageClicked = onAssetItemClicked,
                                onAudioClick = onAudioItemClicked,
                                onChangeAudioPosition = { _, _ -> },
                                onImageMessageClicked = { _, _ -> },
                                onOpenProfile = { _ -> },
                                onReactionClicked = { _, _ -> },
                                onResetSessionClicked = { _, _ -> },
                                onSelfDeletingMessageRead = { },
                                onLinkClick = { },
                                defaultBackgroundColor = colorsScheme().backgroundVariant,
                                shouldDisplayMessageStatus = false,
                                shouldDisplayFooter = false,
                                onReplyClickable = null
                            )
                        }

                        is UIMessage.System -> {}
                    }
                }
            }
        }
        item {
            if (groupedAssetMessageList.loadState.append is LoadState.Loading) {
                WireCircularProgressIndicator(
                    progressColor = MaterialTheme.wireColorScheme.onBackground,
                )
            }
        }
    }
}
