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

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topBarElevation
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.MediaGalleryScreenDestination
import com.wire.android.ui.home.conversations.DownloadedAssetDialog
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewModel
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.SnackBarMessageHandler
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch

@RootNavGraph
@Destination(
    navArgsDelegate = ConversationMediaNavArgs::class,
    style = PopUpNavigationAnimation::class
)
@Composable
fun ConversationMediaScreen(
    navigator: Navigator,
    conversationAssetMessagesViewModel: ConversationAssetMessagesViewModel = hiltViewModel(),
    conversationMessagesViewModel: ConversationMessagesViewModel = hiltViewModel()
) {
    val permissionPermanentlyDeniedDialogState =
        rememberVisibilityState<PermissionPermanentlyDeniedDialogState>()

    val state: ConversationAssetMessagesViewState = conversationAssetMessagesViewModel.viewState

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
        onAssetItemClicked = conversationMessagesViewModel::downloadOrFetchAssetAndShowDialog,
        audioMessagesState = conversationMessagesViewModel.conversationViewState.audioMessagesState,
        onAudioItemClicked = conversationMessagesViewModel::audioClick,
    )

    DownloadedAssetDialog(
        downloadedAssetDialogState = conversationMessagesViewModel.conversationViewState.downloadedAssetDialogState,
        onSaveFileToExternalStorage = conversationMessagesViewModel::downloadAssetExternally,
        onOpenFileWithExternalApp = conversationMessagesViewModel::downloadAndOpenAsset,
        hideOnAssetDownloadedDialog = conversationMessagesViewModel::hideOnAssetDownloadedDialog,
        onPermissionPermanentlyDenied = {
            permissionPermanentlyDeniedDialogState.show(
                PermissionPermanentlyDeniedDialogState.Visible(
                    title = R.string.app_permission_dialog_title,
                    description = R.string.save_permission_dialog_description
                )
            )
        }
    )

    PermissionPermanentlyDeniedDialog(
        dialogState = permissionPermanentlyDeniedDialogState,
        hideDialog = permissionPermanentlyDeniedDialogState::dismiss
    )

    SnackBarMessageHandler(conversationMessagesViewModel.infoMessage)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Content(
    state: ConversationAssetMessagesViewState,
    onNavigationPressed: () -> Unit = {},
    onImageFullScreenMode: (conversationId: ConversationId, messageId: String, isSelfAsset: Boolean) -> Unit,
    audioMessagesState: PersistentMap<String, AudioState> = persistentMapOf(),
    onAudioItemClicked: (String) -> Unit,
    onAssetItemClicked: (String) -> Unit
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
                    ConversationMediaScreenTabItem.PICTURES -> ImageAssetsContent(
                        imageMessageList = state.imageMessages,
                        assetStatuses = state.assetStatuses,
                        onImageFullScreenMode = onImageFullScreenMode
                    )

                    ConversationMediaScreenTabItem.FILES -> FileAssetsContent(
                        groupedAssetMessageList = state.assetMessages,
                        audioMessagesState = audioMessagesState,
                        assetStatuses = state.assetStatuses,
                        onAudioItemClicked = onAudioItemClicked,
                        onAssetItemClicked = onAssetItemClicked
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

enum class ConversationMediaScreenTabItem(@StringRes val titleResId: Int) : TabItem {
    PICTURES(R.string.label_conversation_pictures),
    FILES(R.string.label_conversation_files);
    override val title: UIText = UIText.StringResource(titleResId)
}

@PreviewMultipleThemes
@Composable
fun previewConversationMediaScreenEmptyContent() {
    WireTheme {
        Content(
            state = ConversationAssetMessagesViewState(),
            onImageFullScreenMode = { _, _, _ -> },
            onAudioItemClicked = { },
            onAssetItemClicked = { }
        )
    }
}
