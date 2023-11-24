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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.model.Clickable
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.MediaGalleryScreenDestination
import com.wire.android.ui.home.conversations.model.MediaAssetImage
import com.wire.android.ui.home.conversations.model.messagetypes.asset.UIAsset
import com.wire.kalium.logic.data.id.ConversationId

@RootNavGraph
@Destination(
    navArgsDelegate = ConversationMediaNavArgs::class,
    style = PopUpNavigationAnimation::class
)
@Composable
fun ConversationMediaScreen(navigator: Navigator) {
    val viewModel: ConversationAssetMessagesViewModel = hiltViewModel()
    val state: ConversationAssetMessagesViewState = viewModel.viewState

    Content(
        state = state,
        onNavigationPressed = { navigator.navigateBack() },
        onImageFullScreenMode = { conversationId, messageId ->
            navigator.navigate(
                NavigationCommand(
                    MediaGalleryScreenDestination(
                        conversationId = conversationId,
                        messageId = messageId,
                        isSelfAsset = false, // TODO KBX
                        isEphemeral = false
                    )
                )
            )
        },
    )
}

@Composable
private fun Content(
    state: ConversationAssetMessagesViewState,
    onNavigationPressed: () -> Unit = {},
    onImageFullScreenMode: (conversationId: ConversationId, messageId: String) -> Unit,
) {

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
            uiAssetList = state.messages,
            modifier = Modifier.padding(padding),
            onImageFullScreenMode = onImageFullScreenMode
        )

    }
}

@Composable
fun AssetList(
    uiAssetList: List<UIAsset>,
    modifier: Modifier,
    onImageFullScreenMode: (conversationId: ConversationId, messageId: String) -> Unit,
) {
    val scrollState = rememberLazyGridState()
    val canScrollForward by remember {
        derivedStateOf {
            scrollState.canScrollForward
        }
    }

    // act when end of list reached
    LaunchedEffect(canScrollForward) {
        appLogger.d("KBX can scroll forward $canScrollForward")

    }

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
            LazyVerticalGrid(
                state = scrollState,
                columns = GridCells.Fixed(COLUMN_COUNT),
                modifier = Modifier.padding(
                    horizontal = dimensions().spacing12x,
                    vertical = dimensions().spacing8x
                )
            ) {
                items(uiAssetList.size, key = { index -> uiAssetList[index].messageId }) { index ->
                    val uiAsset = uiAssetList[index]
                    val currentOnImageClick = remember(uiAsset) {
                        Clickable(enabled = true, onClick = {
                            onImageFullScreenMode(uiAsset.conversationId, uiAsset.messageId)
                        }, onLongClick = {
                        })
                    }
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(all = dimensions().spacing2x)
                    ) {
                        MediaAssetImage(
                            asset = null, // TODO KBX
                            width = imageSize,
                            height = imageSize,
                            downloadStatus = uiAsset.downloadStatus,
                            onImageClick = currentOnImageClick,
                            assetPath = uiAsset.downloadedAssetPath
                        )
                    }
                }
            }
        })
}

private const val COLUMN_COUNT = 4
