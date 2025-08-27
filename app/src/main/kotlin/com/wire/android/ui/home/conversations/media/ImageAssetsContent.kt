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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.mock.mockUIAssetMessage
import com.wire.android.ui.home.conversations.model.MediaAssetImage
import com.wire.android.ui.home.conversations.usecase.UIImageAssetPagingItem
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.MessageAssetStatus
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

@Composable
fun ImageAssetsContent(
    imageMessageList: Flow<PagingData<UIImageAssetPagingItem>>,
    assetStatuses: PersistentMap<String, MessageAssetStatus>,
    onImageClicked: (conversationId: ConversationId, messageId: String, isSelfAsset: Boolean) -> Unit = { _, _, _ -> },
    onImageLongClicked: (messageId: String, isMyMessage: Boolean) -> Unit = { _, _ -> }
) {

    val lazyPagingMessages = imageMessageList.collectAsLazyPagingItems()

    if (lazyPagingMessages.itemCount > 0) {
        ImageAssetGrid(
            uiAssetMessageList = lazyPagingMessages,
            assetStatuses = assetStatuses,
            onImageClicked = onImageClicked,
            onImageLongClicked = onImageLongClicked,
        )
    } else {
        EmptyMediaContentScreen(
            text = stringResource(R.string.label_conversation_pictures_empty)
        )
    }
}

@Composable
private fun ImageAssetGrid(
    uiAssetMessageList: LazyPagingItems<UIImageAssetPagingItem>,
    assetStatuses: PersistentMap<String, MessageAssetStatus>,
    onImageClicked: (conversationId: ConversationId, messageId: String, isSelfAsset: Boolean) -> Unit,
    onImageLongClicked: (messagId: String, isMyMessage: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = dimensions().spacing2x
    val outer = dimensions().spacing12x
    val configuration = LocalConfiguration.current

    val screenWidth = configuration.screenWidthDp.dp

    val horizontalPadding = dimensions().spacing12x
    val itemSpacing = dimensions().spacing2x * 2
    val totalItemSpacing = itemSpacing * COLUMN_COUNT
    val availableWidth = screenWidth - horizontalPadding - totalItemSpacing
    val itemSize = availableWidth / COLUMN_COUNT
    val dpSize = DpSize(itemSize, itemSize)

    LazyVerticalGrid(
        columns = GridCells.Fixed(COLUMN_COUNT),
        modifier = modifier
            .fillMaxSize()
            .background(colorsScheme().surfaceContainerLow),
        contentPadding = PaddingValues(horizontal = outer, vertical = spacing),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement   = Arrangement.spacedBy(spacing),
    ) {
        items(
            count = uiAssetMessageList.itemCount,
            key = {
                when (val item = uiAssetMessageList[it]) {
                    is UIImageAssetPagingItem.Asset -> item.uiAssetMessage.assetId
                    is UIImageAssetPagingItem.Label -> item.date
                    null -> "$it"
                }
            },
            contentType = uiAssetMessageList.itemContentType { it },
            span = { index ->
                when (uiAssetMessageList[index]) {
                    is UIImageAssetPagingItem.Asset -> GridItemSpan(1)
                    is UIImageAssetPagingItem.Label -> GridItemSpan(COLUMN_COUNT)
                    null -> GridItemSpan(1)
                }
            }
        ) { index ->
            when (val item = uiAssetMessageList[index]) {
                is UIImageAssetPagingItem.Asset -> {
                    val uiAsset = item.uiAssetMessage
                    val click = remember(uiAsset) {
                        Clickable(
                            enabled = true,
                            onClick = { onImageClicked(uiAsset.conversationId, uiAsset.messageId, uiAsset.isSelfAsset) },
                            onLongClick = { onImageLongClicked(uiAsset.messageId, uiAsset.isSelfAsset) }
                        )
                    }

                    MediaAssetImage(
                        asset = null,
                        transferStatus = assetStatuses[uiAsset.messageId]?.transferStatus,
                        onImageClick = click,
                        assetPath = uiAsset.assetPath,
                        size = dpSize,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )
                }

                is UIImageAssetPagingItem.Label -> {
                    val label = item.date
                    FolderHeader(
                        name = label.uppercase(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.wireColorScheme.background)
                            .padding(top = if (index == 0) dimensions().spacing0x else dimensions().spacing6x,
                                bottom = dimensions().spacing6x)
                    )
                }

                null -> Unit
            }
        }
    }
}

private const val COLUMN_COUNT = 3

@PreviewMultipleThemes
@Composable
fun PreviewImageAssetsEmptyContent() = WireTheme {
    ImageAssetsContent(imageMessageList = emptyFlow(), assetStatuses = persistentMapOf())
}

@PreviewMultipleThemes
@Composable
fun PreviewImageAssetsContent() = WireTheme {
    val (flowOfAssets, assetStatuses) = mockImages()
    ImageAssetsContent(imageMessageList = flowOfAssets, assetStatuses = assetStatuses,)
}

fun mockImages(): Pair<Flow<PagingData<UIImageAssetPagingItem>>, PersistentMap<String, MessageAssetStatus>> {
    val msg1 = mockUIAssetMessage().copy(assetId = "asset1", messageId = "msg1")
    val msg2 = mockUIAssetMessage().copy(assetId = "asset2", messageId = "msg2")
    val msg3 = mockUIAssetMessage().copy(assetId = "asset3", messageId = "msg3")
    val conversationId = ConversationId("value", "domain")
    val flowOfAssets = flowOf(
        PagingData.from(
            listOf(
                UIImageAssetPagingItem.Label("October"),
                UIImageAssetPagingItem.Asset(msg1),
                UIImageAssetPagingItem.Asset(msg2),
                UIImageAssetPagingItem.Asset(msg3),
            )
        )
    )
    val assetsStatuses = persistentMapOf(
        msg1.messageId to MessageAssetStatus(msg1.messageId, conversationId, AssetTransferStatus.SAVED_EXTERNALLY),
        msg2.messageId to MessageAssetStatus(msg2.messageId, conversationId, AssetTransferStatus.NOT_DOWNLOADED),
        msg3.messageId to MessageAssetStatus(msg3.messageId, conversationId, AssetTransferStatus.DOWNLOAD_IN_PROGRESS)
    )
    return flowOfAssets to assetsStatuses
}
