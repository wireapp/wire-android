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
 *
 *
 */

package com.wire.android.ui.home.conversations.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.model.Clickable
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.model.MediaAssetImage
import com.wire.android.ui.home.conversations.model.MessageGenericAsset
import com.wire.android.ui.home.conversations.model.MessageImage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.messagetypes.asset.UIAsset

@RootNavGraph
@Destination(
    navArgsDelegate = ConversationMediaNavArgs::class,
    style = PopUpNavigationAnimation::class
)
@Composable
fun ConversationMediaScreen(navigator: Navigator) {
    val viewModel: ConversationAssetMessagesViewModel = hiltViewModel()
    val state: ConversationAssetMessagesViewState = viewModel.conversationViewState

    Content(
        state = state,
        onNavigationPressed = { navigator.navigateBack() }
    )
}

@Composable
private fun Content(
    state: ConversationAssetMessagesViewState,
    onNavigationPressed: () -> Unit = {},
) {
    val lazyPagingMessages: LazyPagingItems<UIAsset> = state.messages.collectAsLazyPagingItems()

    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                title = stringResource(id = R.string.label_conversation_media),
                navigationIconType = NavigationIconType.Back,
                onNavigationPressed = onNavigationPressed
            )
        },
    ) { padding ->
        AssetList(
            lazyPagingMessages = lazyPagingMessages,
            modifier = Modifier.padding(padding)
        )

    }
}

@Composable
fun AssetList(
    lazyPagingMessages: LazyPagingItems<UIAsset>,
    modifier: Modifier,
//    lazyListState: LazyListState,
//    audioMessagesState: Map<String, AudioState>,
//    onAssetItemClicked: (String) -> Unit,
//    onImageFullScreenMode: (UIMessage.Regular, Boolean) -> Unit,
//    onOpenProfile: (String) -> Unit,
//    onAudioItemClicked: (String) -> Unit,
//    onChangeAudioPosition: (String, Int) -> Unit,
//    onReactionClicked: (String, String) -> Unit,
//    onLinkClick: (String) -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val horizontalPadding = dimensions().spacing12x * 2
    val spaceBetweenItems = dimensions().spacing2x * (COLUMN_COUNT - 1) // For 4 columns, there are 3 spaces between items
    val totalPadding = horizontalPadding + spaceBetweenItems
    val imageSize = (screenWidth - totalPadding) / COLUMN_COUNT
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = colorsScheme().backgroundVariant),
        content = {
//            LazyColumn(
////                state = lazyListState,
//                reverseLayout = false,
//                // calculating bottom padding to have space for [UsersTypingIndicator]
////                contentPadding = PaddingValues(
////                    bottom = dimensions().typingIndicatorHeight - dimensions().messageItemVerticalPadding
////                ),
//                modifier = Modifier
//                    .fillMaxSize()
//            ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(COLUMN_COUNT),
                modifier = Modifier.padding(
                    horizontal = dimensions().spacing12x,
                    vertical = dimensions().spacing8x
                )
            ) {

                items(lazyPagingMessages.itemCount, key = { index -> lazyPagingMessages[index]?.imageMessage?.assetId?.toString() ?: "asset$index" }) { index ->
                    val content = lazyPagingMessages[index] ?: return@items
                    val currentOnImageClick = remember(content) {
                        Clickable(enabled = true, onClick = {
//                            onImageMessageClicked(
//                                message,
//                                source == MessageSource.Self
//                            )
                        }, onLongClick = {
//                            onLongClicked(message)
                        })
                    }
//                    when (content) {
//                        is UIMessageContent.ImageMessage -> {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .padding(all = dimensions().spacing2x)
                            ) {
                                MediaAssetImage(
                                    asset = content.imageMessage.asset,
                                    width = imageSize,
                                    height = imageSize,
                                    downloadStatus = content.imageMessage.downloadStatus,
                                    onImageClick = currentOnImageClick
                                )
                            }
//                        }

//                        is UIMessageContent.AssetMessage -> {
//                            Box(
//                                modifier = Modifier
//                                    .height(dimensions().spacing80x)
//                                    .padding(all = dimensions().spacing2x)
//                            ) {
//                                MessageGenericAsset(
//                                    assetName = content.assetName,
//                                    assetExtension = content.assetExtension,
//                                    assetSizeInBytes = content.assetSizeInBytes,
//                                    assetUploadStatus = content.uploadStatus,
//                                    assetDownloadStatus = content.downloadStatus,
//                                    onAssetClick = currentOnImageClick
//                                )
//                            }
//                        }

//                        else -> {
//                            appLogger.d("KBX $content")
//                        }
//                    }
                }
            }
        })
}

private const val COLUMN_COUNT = 4
