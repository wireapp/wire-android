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

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.model.SnackBarMessage
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.SureAboutMessagingInDegradedConversationDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.image.WireImage
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.home.conversations.AssetTooLargeDialog
import com.wire.android.ui.home.conversations.SureAboutMessagingDialogState
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.conversations.sendmessage.SendMessageAction
import com.wire.android.ui.home.conversations.sendmessage.SendMessageState
import com.wire.android.ui.home.conversations.sendmessage.SendMessageViewModel
import com.wire.android.ui.home.messagecomposer.model.ComposableMessageBundle
import com.wire.android.ui.home.messagecomposer.model.MessageBundle
import com.wire.android.ui.legalhold.dialog.subject.LegalHoldSubjectMessageDialog
import com.wire.android.ui.sharing.ImportedMediaAsset
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.SharedFlow
import okio.Path.Companion.toPath

@RootNavGraph
@Destination(
    navArgsDelegate = ImagesPreviewNavArgs::class,
    style = PopUpNavigationAnimation::class
)
@Composable
fun ImagesPreviewScreen(
    navigator: Navigator,
    imagesPreviewViewModel: ImagesPreviewViewModel = hiltViewModel(),
    sendMessageViewModel: SendMessageViewModel = hiltViewModel()
) {
    LaunchedEffect(sendMessageViewModel.viewState.afterMessageSendAction) {
        when (val action = sendMessageViewModel.viewState.afterMessageSendAction) {
            SendMessageAction.NavigateBack -> navigator.navigateBack()
            is SendMessageAction.NavigateToConversation -> navigator.navigate(
                NavigationCommand(
                    ConversationScreenDestination(action.conversationId),
                    BackStackMode.REMOVE_CURRENT
                )
            )

            SendMessageAction.NavigateToHome -> navigator.navigate(
                NavigationCommand(
                    HomeScreenDestination(),
                    BackStackMode.REMOVE_CURRENT
                )
            )

            SendMessageAction.None -> {}
        }
    }

    Content(
        previewState = imagesPreviewViewModel.viewState,
        sendState = sendMessageViewModel.viewState,
        onNavigationPressed = { navigator.navigateBack() },
        onSendMessages = sendMessageViewModel::trySendMessages,
        onSelected = imagesPreviewViewModel::onSelected
    )

    AssetTooLargeDialog(
        dialogState = sendMessageViewModel.assetTooLargeDialogState,
        hideDialog = sendMessageViewModel::hideAssetTooLargeError
    )

    SureAboutMessagingInDegradedConversationDialog(
        dialogState = sendMessageViewModel.sureAboutMessagingDialogState,
        sendAnyway = sendMessageViewModel::acceptSureAboutSendingMessage,
        hideDialog = sendMessageViewModel::dismissSureAboutSendingMessage
    )

    (sendMessageViewModel.sureAboutMessagingDialogState as? SureAboutMessagingDialogState.Visible.ConversationUnderLegalHold)?.let {
        LegalHoldSubjectMessageDialog(
            dialogDismissed = sendMessageViewModel::dismissSureAboutSendingMessage,
            sendAnywayClicked = sendMessageViewModel::acceptSureAboutSendingMessage,
        )
    }

    SnackBarMessage(sendMessageViewModel.infoMessage)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Content(
    previewState: ImagesPreviewState,
    sendState: SendMessageState,
    onNavigationPressed: () -> Unit = {},
    onSendMessages: (List<MessageBundle>) -> Unit,
    onSelected: (index: Int) -> Unit
) {
    val configuration = LocalConfiguration.current
    val pagerState = rememberPagerState(pageCount = { previewState.assetUriList.size })
    LaunchedEffect(key1 = previewState.selectedIndex) {
        if (previewState.selectedIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(previewState.selectedIndex)
        }
    }

    LaunchedEffect(key1 = pagerState.currentPage) {
        if (previewState.selectedIndex != pagerState.currentPage) {
            onSelected(pagerState.currentPage)
        }
    }

    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                title = previewState.conversationName,
                navigationIconType = NavigationIconType.Back,
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
                        loading = sendState.inProgress,
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
                            onSendMessages(
                                previewState.assetUriList.map {
                                    ComposableMessageBundle.AttachmentPickedBundle(
                                        previewState.conversationId,
                                        UriAsset(Uri.fromFile(it.assetBundle.dataPath.toFile()))
                                    )
                                }

                            )
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
                    modifier = Modifier
                        .width(configuration.screenWidthDp.dp)
                        .fillMaxHeight(),
                ) { index ->
                    WireImage(
                        modifier = Modifier
                            .width(configuration.screenWidthDp.dp)
                            .fillMaxHeight(),
                        model = previewState.assetUriList[index].assetBundle.dataPath.toFile(),
                        contentDescription = previewState.assetUriList[index].assetBundle.fileName
                    )
                }
            }

            LazyRow(
                modifier = Modifier
                    .padding(bottom = dimensions().spacing8x)
                    .height(dimensions().spacing72x)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
                contentPadding = PaddingValues(start = dimensions().spacing16x, end = dimensions().spacing16x)
            ) {
                items(
                    count = previewState.assetUriList.size,
                ) { index ->
                    Box(
                        modifier = Modifier
                            .width(dimensions().spacing72x)
                            .fillMaxHeight()
                    ) {
                        AssetPreview(
                            modifier = Modifier
                                .size(dimensions().spacing64x)
                                .align(Alignment.BottomStart),
                            asset = previewState.assetUriList[index],
                            isSelected = previewState.selectedIndex == index,
                            onClick = { onSelected(index) }
                        )
                        RemoveAssetButton(modifier = Modifier.align(Alignment.TopEnd), onClick = {})
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssetPreview(
    modifier: Modifier = Modifier,
    asset: ImportedMediaAsset,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clip(shape = RoundedCornerShape(dimensions().messageAssetBorderRadius))
            .background(
                color = MaterialTheme.wireColorScheme.onPrimary,
                shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
            )
            .border(
                width = if (isSelected) {
                    dimensions().spacing2x
                } else {
                    dimensions().spacing1x
                },
                color = if (isSelected) {
                    MaterialTheme.wireColorScheme.primary
                } else {
                    MaterialTheme.wireColorScheme.secondaryButtonDisabledOutline
                },
                shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = {},
            )
    ) {
        WireImage(
            modifier = Modifier.fillMaxSize(),
            model = asset.assetBundle.dataPath.toFile(),
            contentScale = ContentScale.Crop,
            contentDescription = asset.assetBundle.fileName
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RemoveAssetButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Icon(
        modifier = modifier
            .combinedClickable(onClick = onClick)
            .clip(shape = CircleShape)
            .background(color = MaterialTheme.wireColorScheme.inverseSurface)
            .padding(dimensions().spacing6x),
        painter = painterResource(id = R.drawable.ic_close), contentDescription = "test",
        tint = MaterialTheme.wireColorScheme.inverseOnSurface
    )
}

@Composable
private fun SnackBarMessage(infoMessages: SharedFlow<SnackBarMessage>) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(Unit) {
        infoMessages.collect {
            snackbarHostState.showSnackbar(
                message = it.uiText.asString(context.resources)
            )
        }
    }
}


@PreviewMultipleThemes
@Composable
fun PreviewImagesPreviewScreen() {
    WireTheme {
        Content(
            previewState = ImagesPreviewState(
                ConversationId("value", "domain"),
                "Conversation",
                persistentListOf(
                    ImportedMediaAsset(
                        AssetBundle(
                            "key",
                            "image/png",
                            "".toPath(),
                            20,
                            "preview",
                            assetType = AttachmentType.IMAGE
                        ),
                        assetSizeExceeded = null
                    )
                ),
            ),
            sendState = SendMessageState(inProgress = false),
            onNavigationPressed = {},
            onSendMessages = {},
            onSelected = {}
        )
    }
}
