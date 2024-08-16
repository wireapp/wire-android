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

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.wire.android.R
import com.wire.android.model.ImageAsset
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.show
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.conversations.MediaGallerySnackbarMessages
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialog
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogActiveState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogsState
import com.wire.android.ui.home.conversations.edit.assetMessageOptionsMenuItems
import com.wire.android.ui.home.conversations.edit.assetOptionsMenuItems
import com.wire.android.ui.home.conversations.mock.mockedPrivateAsset
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.permission.rememberWriteStoragePermissionFlow
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.openDownloadFolder
import kotlinx.coroutines.flow.SharedFlow

@RootNavGraph
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
    val mediaGalleryScreenState = rememberMediaGalleryScreenState()
    val context = LocalContext.current
    val onSaveImageWriteStorageRequest = rememberWriteStoragePermissionFlow(
        onPermissionGranted = {
            mediaGalleryScreenState.modalBottomSheetState.hide()
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

    DeleteMessageDialog(
        state = viewModelState.deleteMessageDialogsState,
        actions = mediaGalleryViewModel.deleteMessageHelper,
        onDeleted = navigator::navigateBack
    )

    MediaGalleryContent(
        state = viewModelState,
        imageAsset = mediaGalleryViewModel.imageAsset,
        onCloseClick = navigator::navigateBack,
        onOptionsClick = mediaGalleryScreenState.modalBottomSheetState::show,
        modifier = modifier,
    )

    MediaGalleryOptionsBottomSheetLayout(
        sheetState = mediaGalleryScreenState.modalBottomSheetState,
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

    SnackbarMessageHandler(mediaGalleryViewModel.snackbarMessage)
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

@Composable
private fun SnackbarMessageHandler(snackbarMessage: SharedFlow<MediaGallerySnackbarMessages>) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    LaunchedEffect(Unit) {
        snackbarMessage.collect { messageCode ->
            val (message, actionLabel) = getSnackbarMessage(messageCode, context.resources)
            val snackbarResult = snackbarHostState.showSnackbar(message = message, actionLabel = actionLabel)
            when {
                // Show downloads folder when clicking on Snackbar cta button
                messageCode is MediaGallerySnackbarMessages.OnImageDownloaded && snackbarResult == SnackbarResult.ActionPerformed -> {
                    openDownloadFolder(context)
                }
            }
        }
    }
}

private fun getSnackbarMessage(messageCode: MediaGallerySnackbarMessages, resources: Resources): Pair<String, String?> {
    val msg = when (messageCode) {
        is MediaGallerySnackbarMessages.OnImageDownloaded -> resources.getString(R.string.media_gallery_on_image_downloaded)
        is MediaGallerySnackbarMessages.OnImageDownloadError -> resources.getString(R.string.media_gallery_on_image_download_error)
        is MediaGallerySnackbarMessages.DeletingMessageError -> resources.getString(R.string.error_conversation_deleting_message)
    }
    val actionLabel = when (messageCode) {
        is MediaGallerySnackbarMessages.OnImageDownloaded -> resources.getString(R.string.label_show)
        else -> null
    }
    return msg to actionLabel
}

@PreviewMultipleThemes
@Composable
fun PreviewMediaGalleryScreen() = WireTheme {
    MediaGalleryContent(
        state = MediaGalleryViewState(
            screenTitle = "Media Gallery",
            messageBottomSheetOptionsEnabled = true,
            deleteMessageDialogsState = DeleteMessageDialogsState.States(
                forYourself = DeleteMessageDialogActiveState.Hidden,
                forEveryone = DeleteMessageDialogActiveState.Hidden
            )
        ),
        imageAsset = mockedPrivateAsset(),
        onCloseClick = {},
        onOptionsClick = {}
    )
}
