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

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topBarElevation
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.MediaGalleryScreenDestination
import com.wire.android.ui.home.conversations.model.messagetypes.asset.UIAssetMessage
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.ui.home.conversations.MessageItem
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.messagetypes.asset.UIAssetMessage
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.coroutines.launch
import com.wire.kalium.util.map.forEachIndexed
import kotlinx.coroutines.launch

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
        onImageFullScreenMode = { conversationId, messageId, isSelfAsset ->
            navigator.navigate(
                NavigationCommand(
                    MediaGalleryScreenDestination(
                        conversationId = conversationId,
                        messageId = messageId,
                        isSelfAsset = isSelfAsset,
                        isEphemeral = false
                    )
                )
            )
        },
        continueAssetLoading = { shouldContinue ->
            viewModel.continueLoading(shouldContinue)
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Content(
    state: ConversationAssetMessagesViewState,
    onNavigationPressed: () -> Unit = {},
    onImageFullScreenMode: (conversationId: ConversationId, messageId: String, isSelfAsset: Boolean) -> Unit,
    continueAssetLoading: (shouldContinue: Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val lazyListStates: List<LazyListState> = ConversationMediaScreenTabItem.entries.map { rememberLazyListState() }
    val initialPageIndex = ConversationMediaScreenTabItem.PICTURES.ordinal
    val pagerState = rememberPagerState(initialPage = initialPageIndex, pageCount = { ConversationMediaScreenTabItem.entries.size })
    val maxAppBarElevation = MaterialTheme.wireDimensions.topBarShadowElevation
    val currentTabState by remember { derivedStateOf { pagerState.calculateCurrentTab() } }
    val elevationState by remember { derivedStateOf { lazyListStates[currentTabState].topBarElevation(maxAppBarElevation) } }

    WireScaffold(
        modifier = Modifier
            .background(color = colorsScheme().backgroundVariant),
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = elevationState,
                title = stringResource(id = R.string.label_conversation_media),
                navigationIconType = NavigationIconType.Back,
                onNavigationPressed = onNavigationPressed,
                bottomContent = {
                    WireTabRow(
                        tabs = ConversationMediaScreenTabItem.entries,
                        selectedTabIndex = currentTabState,
                        onTabChange = { scope.launch { pagerState.animateScrollToPage(it) } }
                    )
                }
            )
        },
    ) { padding ->
        var focusedTabIndex: Int by remember { mutableStateOf(initialPageIndex) }

        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding)
            ) { pageIndex ->
                when (ConversationMediaScreenTabItem.entries[pageIndex]) {
                    ConversationMediaScreenTabItem.PICTURES -> PicturesContent(
                        uiAssetMessageList = state.messages,
                        onImageFullScreenMode = onImageFullScreenMode,
                        continueAssetLoading = continueAssetLoading
                    )
                    ConversationMediaScreenTabItem.FILES -> FilesContent()
                }
            }

            LaunchedEffect(pagerState.isScrollInProgress, focusedTabIndex, pagerState.currentPage) {
                if (!pagerState.isScrollInProgress && focusedTabIndex != pagerState.currentPage) {
                    focusedTabIndex = pagerState.currentPage
                }
            }
        }
    }
}

@Composable
private fun PicturesContent(
    uiAssetMessageList: List<UIAssetMessage>,
    onImageFullScreenMode: (conversationId: ConversationId, messageId: String, isSelfAsset: Boolean) -> Unit,
    continueAssetLoading: (shouldContinue: Boolean) -> Unit
) {
    if (uiAssetMessageList.isEmpty()) {
        EmptyMediaContentScreen(
            text = stringResource(R.string.label_conversation_pictures_empty)
        )
    } else {
        AssetGrid(
            uiAssetMessageList = uiAssetMessageList,
        var focusedTabIndex: Int by remember { mutableStateOf(initialPageIndex) }

        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding)
            ) { pageIndex ->
                when (ConversationMediaScreenTabItem.entries[pageIndex]) {
                    ConversationMediaScreenTabItem.PICTURES -> PicturesContent(
                        groupedImageMessageList = state.imageMessages,
                        onImageFullScreenMode = onImageFullScreenMode,
                        continueAssetLoading = continueAssetLoading
                    )
                    ConversationMediaScreenTabItem.FILES -> FilesContent(
                        groupedAssetMessageList = state.assetMessages
                    )
                }
            }

            LaunchedEffect(pagerState.isScrollInProgress, focusedTabIndex, pagerState.currentPage) {
                if (!pagerState.isScrollInProgress && focusedTabIndex != pagerState.currentPage) {
                    focusedTabIndex = pagerState.currentPage
                }
            }
        }
    }
}

@Composable
private fun PicturesContent(
    groupedImageMessageList: List<UIAssetMessage>,
    onImageFullScreenMode: (conversationId: ConversationId, messageId: String, isSelfAsset: Boolean) -> Unit,
    continueAssetLoading: (shouldContinue: Boolean) -> Unit
) {
    if (groupedImageMessageList.isEmpty()) {
        EmptyMediaContentScreen(
            text = stringResource(R.string.label_conversation_pictures_empty)
        )
    } else {
        ImageAssetGrid(
            uiAssetMessageList = groupedImageMessageList,
            onImageFullScreenMode = onImageFullScreenMode,
            continueAssetLoading = continueAssetLoading
        )
    }
}

@Composable
private fun FilesContent(
    groupedAssetMessageList: Map<String, List<UIMessage>>
) {
    if (groupedAssetMessageList.isEmpty()) {
        EmptyMediaContentScreen(
            text = stringResource(R.string.label_conversation_files_empty)
        )
    } else {
        AssetMessagesListContent(groupedAssetMessageList = groupedAssetMessageList)
    }
}

@Composable
private fun AssetMessagesListContent(
    groupedAssetMessageList: Map<String, List<UIMessage>>
) {
    LazyColumn {
        groupedAssetMessageList.forEachIndexed { index, entry ->
            val label = entry.key
            item(key = entry.key) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = dimensions().spacing6x,
                            // first label should not have top padding
                            top = if (index == 0) dimensions().spacing0x else dimensions().spacing6x,
                        )
                ) {
                    FolderHeader(
                        name = label.uppercase(),
                        modifier = Modifier
                            .background(MaterialTheme.wireColorScheme.background)
                            .fillMaxWidth()
                    )
                }
            }

            items(
                count = entry.value.size,
                key = { entry.value[it].header.messageId }
            ) {
                when (val message = entry.value[it]) {
                    is UIMessage.Regular -> {
                        MessageItem(
                            message = message,
                            conversationDetailsData = ConversationDetailsData.None,
                            audioMessagesState = emptyMap(), // TODO(Media): handle audio state
                            onAudioClick = {}, // TODO(Media): handle audio click
                            onChangeAudioPosition = {_, _ -> },
                            onLongClicked = {},
                            onAssetMessageClicked = {}, // TODO(Media): handle asset click
                            onImageMessageClicked = {_, _ -> },
                            onOpenProfile = {_ -> },
                            onReactionClicked = {_, _ -> },
                            onResetSessionClicked = {_, _ -> },
                            onSelfDeletingMessageRead = {},
                            defaultBackgroundColor = colorsScheme().backgroundVariant,
                            shouldDisplayMessageStatus = false,
                            shouldDisplayFooter = false,
                            onLinkClick = {}
                        )
                    }

                    is UIMessage.System -> {}
                }
            }
        }
    }
}



enum class ConversationMediaScreenTabItem(@StringRes override val titleResId: Int) : TabItem {
    PICTURES(R.string.label_conversation_pictures),
    FILES(R.string.label_conversation_files);
}

@PreviewMultipleThemes
@Composable
fun previewConversationMediaScreenEmptyContent() {
    WireTheme {
        Content(
            state = ConversationAssetMessagesViewState(),
            onImageFullScreenMode = {_, _, _ -> },
            continueAssetLoading = {}
        )
    }
}
