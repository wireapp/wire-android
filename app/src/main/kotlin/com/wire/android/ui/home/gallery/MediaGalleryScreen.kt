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

package com.wire.android.ui.home.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.wire.android.R
import com.wire.android.model.ImageAsset
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.WireSheetValue
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.bottomsheet.show
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.conversations.MediaGallerySnackbarMessages
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialog
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogState
import com.wire.android.ui.home.conversations.edit.assetMessageOptionsMenuItems
import com.wire.android.ui.home.conversations.edit.assetOptionsMenuItems
import com.wire.android.ui.home.conversations.mock.mockedPrivateAsset
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.permission.rememberWriteStoragePermissionFlow
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.SnackBarMessageHandler
import com.wire.android.util.ui.openDownloadFolder

@WireDestination(
    navArgsDelegate = MediaGalleryNavArgs::class,
    style = PopUpNavigationAnimation::class,
)
@Composable
fun MediaGalleryScreen(
    navigator: Navigator,
    resultNavigator: ResultBackNavigator<MediaGalleryNavBackArgs>,
    modifier: Modifier = Modifier,
    mediaGalleryViewModel: MediaGalleryViewModel = hiltViewModel()
) {
    val permissionPermanentlyDeniedDialogState =
        rememberVisibilityState<PermissionPermanentlyDeniedDialogState>()

    val viewModelState = mediaGalleryViewModel.mediaGalleryViewState
    val bottomSheetState: WireModalSheetState<Unit> = rememberWireModalSheetState()
    val context = LocalContext.current
    val onSaveImageWriteStorageRequest = rememberWriteStoragePermissionFlow(
        onPermissionGranted = {
            bottomSheetState.hide()
            mediaGalleryViewModel.saveImageToExternalStorage()
        },
        onPermissionDenied = { /** Nothing to do **/ },
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

    LaunchedEffect(viewModelState.messageDeleted) {
        if (viewModelState.messageDeleted) navigator.navigateBack()
    }

    DeleteMessageDialog(
        state = viewModelState.deleteMessageDialogState,
        actions = mediaGalleryViewModel.deleteMessageHelper,
    )

    MediaGalleryContent(
        state = viewModelState,
        imageAsset = mediaGalleryViewModel.imageAsset,
        onCloseClick = navigator::navigateBack,
        onOptionsClick = bottomSheetState::show,
        modifier = modifier,
    )

    MediaGalleryOptionsBottomSheetLayout(
        sheetState = bottomSheetState,
        isEphemeral = mediaGalleryViewModel.mediaGalleryViewState.isEphemeral,
        messageBottomSheetOptionsEnabled = viewModelState.messageBottomSheetOptionsEnabled,
        deleteAsset = mediaGalleryViewModel::deleteCurrentImage,
        showDetails = {
            resultNavigator.setResult(
                MediaGalleryNavBackArgs(
                    messageId = mediaGalleryViewModel.imageAsset.messageId,
                    isSelfAsset = mediaGalleryViewModel.imageAsset.isSelfAsset,
                    mediaGalleryActionType = MediaGalleryActionType.DETAIL
                )
            )
            resultNavigator.navigateBack()
        },
        shareAsset = { mediaGalleryViewModel.shareAsset(context) },
        reply = {
            resultNavigator.setResult(
                MediaGalleryNavBackArgs(
                    messageId = mediaGalleryViewModel.imageAsset.messageId,
                    mediaGalleryActionType = MediaGalleryActionType.REPLY
                )
            )
            resultNavigator.navigateBack()
        },
        react = { emoji ->
            resultNavigator.setResult(
                MediaGalleryNavBackArgs(
                    messageId = mediaGalleryViewModel.imageAsset.messageId,
                    emoji = emoji,
                    mediaGalleryActionType = MediaGalleryActionType.REACT
                )
            )
            resultNavigator.navigateBack()
        },
        downloadAsset = onSaveImageWriteStorageRequest::launch
    )

    SnackBarMessageHandler(mediaGalleryViewModel.snackbarMessage) { messageCode ->
        when (messageCode) {
            is MediaGallerySnackbarMessages.OnImageDownloaded -> {
                openDownloadFolder(context) // Show downloads folder when clicking on Snackbar cta button
            }
        }
    }
}

@Composable
private fun MediaGalleryContent(
    state: MediaGalleryViewState,
    imageAsset: ImageAsset.Remote,
    onCloseClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WireScaffold(
        modifier = modifier,
        topBar = {
            MediaGalleryScreenTopAppBar(
                title = state.screenTitle
                    ?: stringResource(R.string.media_gallery_default_title_name),
                onCloseClick = onCloseClick,
                onOptionsClick = onOptionsClick,
            )
        },
        content = { internalPadding ->
            Box(
                modifier = Modifier
                    .padding(internalPadding)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(colorsScheme().surface)
            ) {
                ZoomableImage(
                    modifier = Modifier.align(Alignment.Center),
                    imageAsset = imageAsset,
                    contentDescription = stringResource(R.string.content_description_image_message)
                )
            }
        }
    )
}

@Composable
private fun MediaGalleryOptionsBottomSheetLayout(
    sheetState: WireModalSheetState<Unit>,
    isEphemeral: Boolean,
    messageBottomSheetOptionsEnabled: Boolean,
    deleteAsset: () -> Unit,
    showDetails: () -> Unit,
    shareAsset: () -> Unit,
    reply: () -> Unit,
    react: (String) -> Unit,
    downloadAsset: () -> Unit,
) {
    val onDeleteClick: () -> Unit = remember { { sheetState.hide(deleteAsset) } }
    val onShowDetailsClick: () -> Unit = remember { { sheetState.hide(showDetails) } }
    val onShareAssetClick: () -> Unit = remember { { sheetState.hide(shareAsset) } }
    val onReplyClick: () -> Unit = remember { { sheetState.hide(reply) } }
    val onReactClick: (String) -> Unit = remember { { emoji -> sheetState.hide { react(emoji) } } }
    WireModalSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            WireMenuModalSheetContent(
                menuItems = if (messageBottomSheetOptionsEnabled) {
                    assetMessageOptionsMenuItems(
                        isUploading = false,
                        isEphemeral = isEphemeral,
                        onReplyClick = onReplyClick,
                        onReactionClick = onReactClick,
                        onDetailsClick = onShowDetailsClick,
                        onDeleteClick = onDeleteClick,
                        onShareAsset = onShareAssetClick,
                        onDownloadAsset = downloadAsset,
                    )
                } else {
                    assetOptionsMenuItems(
                        isUploading = false,
                        isEphemeral = isEphemeral,
                        onDeleteClick = onDeleteClick,
                        onShareAsset = onShareAssetClick,
                        onDownloadAsset = downloadAsset,
                    )
                }
            )
        }
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewMediaGalleryScreen() = WireTheme {
    MediaGalleryContent(
        state = MediaGalleryViewState(
            screenTitle = "Media Gallery",
            messageBottomSheetOptionsEnabled = true,
            deleteMessageDialogState = DeleteMessageDialogState.Hidden,
        ),
        imageAsset = mockedPrivateAsset(),
        onCloseClick = {},
        onOptionsClick = {}
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewMediaGalleryOptionsBottomSheetLayout() = WireTheme {
     MediaGalleryOptionsBottomSheetLayout(
        sheetState = rememberWireModalSheetState(initialValue = WireSheetValue.Expanded(Unit)),
        isEphemeral = false,
        messageBottomSheetOptionsEnabled = true,
        deleteAsset = {},
        showDetails = {},
        shareAsset = {},
        reply = {},
        react = {},
        downloadAsset = {}
    )
}
