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
package com.wire.android.ui.home.conversations.media.preview
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.navigation.WireRootNavGraph

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.error.ErrorIcon
import com.wire.android.ui.common.image.WireImage
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.remove.RemoveIcon
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.AssetTooLargeDialog
import com.wire.android.ui.home.conversations.media.CheckAssetRestrictionsViewModel
import com.wire.android.ui.home.conversations.media.RestrictionCheckState
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.sharing.ImportedMediaAsset
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath

@Destination<WireRootNavGraph>(
    navArgs = ImagesPreviewNavArgs::class,
    style = PopUpNavigationAnimation::class
)
@Composable
fun ImagesPreviewScreen(
    navigator: Navigator,
    resultNavigator: ResultBackNavigator<ImagesPreviewNavBackArgs>,
    imagesPreviewViewModel: ImagesPreviewViewModel = hiltViewModel(),
    checkAssetRestrictionsViewModel: CheckAssetRestrictionsViewModel = hiltViewModel()
) {
    LaunchedEffect(checkAssetRestrictionsViewModel.state) {
        with(checkAssetRestrictionsViewModel.state) {
            if (this is RestrictionCheckState.Success) {
                resultNavigator.setResult(ImagesPreviewNavBackArgs(this.assetBundleList))
                resultNavigator.navigateBack()
            }
        }
    }
    Content(
        previewState = imagesPreviewViewModel.viewState,
        onNavigationPressed = navigator::navigateBack,
        onSendMessages = { mediaAssets ->
            checkAssetRestrictionsViewModel.checkRestrictions(importedMediaList = mediaAssets)
        },
        onSelected = imagesPreviewViewModel::onSelected,
        onRemoveAsset = imagesPreviewViewModel::onRemove
    )

    AssetTooLargeDialog(
        dialogState = checkAssetRestrictionsViewModel.state.assetTooLargeDialogState,
        hideDialog = checkAssetRestrictionsViewModel::hideDialog
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Content(
    previewState: ImagesPreviewState,
    onSendMessages: (List<ImportedMediaAsset>) -> Unit,
    onSelected: (index: Int) -> Unit,
    onNavigationPressed: () -> Unit = {},
    onRemoveAsset: (index: Int) -> Unit
) {
    val configuration = LocalConfiguration.current
    val pagerState = rememberPagerState(pageCount = { previewState.assetBundleList.size })
    val scope = rememberCoroutineScope()
    LaunchedEffect(key1 = previewState.selectedIndex) {
        if (previewState.selectedIndex != pagerState.settledPage) {
            scope.launch {
                pagerState.animateScrollToPage(previewState.selectedIndex)
            }
        }
    }

    LaunchedEffect(key1 = pagerState.settledPage) {
        if (previewState.selectedIndex != pagerState.settledPage) {
            onSelected(pagerState.settledPage)
        }
    }

    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                title = previewState.conversationName,
                navigationIconType = NavigationIconType.Back(),
                onNavigationPressed = onNavigationPressed,
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.wireColorScheme.background)
                    .height(dimensions().spacing80x)
            ) {
                WireDivider(color = MaterialTheme.wireColorScheme.outline)
                Row(
                    modifier = Modifier
                        .weight(1F)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalSpace.x16()
                    WireSecondaryButton(
                        modifier = Modifier.weight(1F),
                        text = stringResource(id = R.string.label_cancel),
                        onClick = onNavigationPressed
                    )
                    HorizontalSpace.x16()
                    WirePrimaryButton(
                        modifier = Modifier.weight(1F),
                        text = stringResource(id = R.string.import_media_send_button_title),
                        leadingIcon = {
                            Image(
                                painter = painterResource(id = R.drawable.ic_send),
                                contentDescription = null,
                                modifier = Modifier.padding(end = dimensions().spacing12x),
                                colorFilter = ColorFilter.tint(colorsScheme().onPrimaryButtonEnabled)
                            )
                        },
                        onClick = {
                            onSendMessages(previewState.assetBundleList)
                        }
                    )
                    HorizontalSpace.x16()
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                HorizontalPager(
                    state = pagerState,
                    key = { index: Int ->
                        previewState.assetBundleList.getOrNull(index)?.assetBundle?.key ?: ""
                    },
                    modifier = Modifier
                        .width(configuration.screenWidthDp.dp)
                        .fillMaxHeight(),
                ) { index ->
                    val assetBundle = previewState.assetBundleList[index].assetBundle

                    when (assetBundle.assetType) {
                        AttachmentType.IMAGE -> WireImage(
                            modifier = Modifier
                                .width(configuration.screenWidthDp.dp)
                                .fillMaxHeight(),
                            model = previewState.assetBundleList[index].assetBundle.dataPath.toFile(),
                            contentDescription = previewState.assetBundleList[index].assetBundle.fileName
                        )

                        AttachmentType.GENERIC_FILE,
                        AttachmentType.AUDIO,
                        AttachmentType.VIDEO -> AssetFilePreview(
                            assetName = assetBundle.fileName,
                            sizeInBytes = assetBundle.dataSize
                        )
                    }
                }
            }

            if (previewState.isLoading) {
                WireCircularProgressIndicator(
                    progressColor = MaterialTheme.wireColorScheme.onBackground,
                    modifier = Modifier.align(Alignment.Center),
                    size = dimensions().spacing24x
                )
            }

            if (previewState.assetBundleList.size > 1) {
                ThumbnailsRow(
                    previewState = previewState,
                    onSelected = onSelected,
                    onRemoveAsset = onRemoveAsset
                )
            }
        }
    }
}

@Composable
private fun BoxScope.ThumbnailsRow(
    previewState: ImagesPreviewState,
    onSelected: (index: Int) -> Unit,
    onRemoveAsset: (index: Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .padding(bottom = dimensions().spacing8x)
            .height(dimensions().spacing80x)
            .align(Alignment.BottomCenter),
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
        contentPadding = PaddingValues(start = dimensions().spacing16x, end = dimensions().spacing16x)
    ) {
        items(
            count = previewState.assetBundleList.size,
        ) { index ->
            Box(
                modifier = Modifier
                    .width(dimensions().spacing80x)
                    .fillMaxHeight()
            ) {
                AssetTilePreview(
                    modifier = Modifier
                        .size(dimensions().spacing64x)
                        .align(Alignment.Center),
                    assetBundle = previewState.assetBundleList[index].assetBundle,
                    isSelected = previewState.selectedIndex == index,
                    showOnlyExtension = true,
                    onClick = { onSelected(index) }
                )

                if (previewState.assetBundleList.size > 1) {
                    RemoveIcon(
                        modifier = Modifier.align(Alignment.TopEnd),
                        onClick = {
                            onRemoveAsset(index)
                        },
                        contentDescription = stringResource(id = R.string.remove_asset_description)
                    )
                }
                if (previewState.assetBundleList[index].assetSizeExceeded != null) {
                    ErrorIcon(
                        stringResource(id = R.string.asset_attention_description),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewImagesPreviewScreenMultipleAssets() {
    WireTheme {
        Content(
            previewState = ImagesPreviewState(
                ConversationId("value", "domain"),
                selectedIndex = 0,
                conversationName = "Conversation",
                assetBundleList = persistentListOf(
                    ImportedMediaAsset(
                        AssetBundle(
                            "key",
                            "image/png",
                            "".toPath(),
                            20,
                            "preview.png",
                            assetType = AttachmentType.IMAGE
                        ),
                        assetSizeExceeded = null
                    ),
                    ImportedMediaAsset(
                        AssetBundle(
                            "key1",
                            "video/mp4",
                            "".toPath(),
                            20,
                            "preview.mp4",
                            assetType = AttachmentType.VIDEO
                        ),
                        assetSizeExceeded = null
                    ),
                    ImportedMediaAsset(
                        AssetBundle(
                            "key2",
                            "audio/mp3",
                            "".toPath(),
                            20,
                            "preview.mp3",
                            assetType = AttachmentType.AUDIO
                        ),
                        assetSizeExceeded = 20
                    ),
                    ImportedMediaAsset(
                        AssetBundle(
                            "key3",
                            "document/pdf",
                            "".toPath(),
                            20,
                            "preview.pdf",
                            assetType = AttachmentType.GENERIC_FILE
                        ),
                        assetSizeExceeded = null
                    )
                ),
            ),
            onNavigationPressed = {},
            onSendMessages = {},
            onSelected = {},
            onRemoveAsset = {}
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewImagesPreviewScreenSingleAsset() {
    WireTheme {
        Content(
            previewState = ImagesPreviewState(
                ConversationId("value", "domain"),
                selectedIndex = 0,
                conversationName = "Conversation",
                assetBundleList = persistentListOf(
                    ImportedMediaAsset(
                        AssetBundle(
                            "key",
                            "image/png",
                            "".toPath(),
                            20,
                            "preview.png",
                            assetType = AttachmentType.IMAGE
                        ),
                        assetSizeExceeded = null
                    )
                ),
            ),
            onNavigationPressed = {},
            onSendMessages = {},
            onSelected = {},
            onRemoveAsset = {}
        )
    }
}
