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

package com.wire.android.ui.home.gallery

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.home.conversations.MediaGallerySnackbarMessages
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialog
import com.wire.android.ui.home.conversations.edit.AssetEditMenuItems
import com.wire.android.util.permission.rememberWriteStorageRequestFlow
import com.wire.android.util.ui.openDownloadFolder

@RootNavGraph
@Destination(
    navArgsDelegate = MediaGalleryNavArgs::class,
    style = PopUpNavigationAnimation::class,
)
@Composable
fun MediaGalleryScreen(
    navigator: Navigator,
    mediaGalleryViewModel: MediaGalleryViewModel = hiltViewModel(),
    resultNavigator: ResultBackNavigator<MediaGalleryNavBackArgs>
) {
    val viewModelState = mediaGalleryViewModel.mediaGalleryViewState
    val mediaGalleryScreenState = rememberMediaGalleryScreenState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val onSaveImageWriteStorageRequest = rememberWriteStorageRequestFlow(
        onGranted = {
            mediaGalleryScreenState.showContextualMenu(false)
            mediaGalleryViewModel.saveImageToExternalStorage()
        }, onDenied = {
            // TODO
        })

    with(viewModelState) {
        Scaffold(
            topBar = {
                MediaGalleryScreenTopAppBar(
                    title = screenTitle
                        ?: stringResource(R.string.media_gallery_default_title_name),
                    onCloseClick = navigator::navigateBack,
                    onOptionsClick = { mediaGalleryScreenState.showContextualMenu(true) }
                )
            },
            content = { internalPadding ->
                Box(modifier = Modifier.padding(internalPadding)) {
                    MediaGalleryContent(navigator, mediaGalleryViewModel, mediaGalleryScreenState)
                }
            },
            snackbarHost = {
                SwipeDismissSnackbarHost(
                    hostState = mediaGalleryScreenState.snackbarHostState,
                    modifier = Modifier.fillMaxWidth()
                )
            },
        )
        MenuModalSheetLayout(
            sheetState = mediaGalleryScreenState.modalBottomSheetState,
            coroutineScope = scope,
            menuItems = AssetEditMenuItems(
                isEphemeral = viewModelState.isEphemeral,
                onDeleteClick = {
                    mediaGalleryScreenState.showContextualMenu(false)
                    mediaGalleryViewModel.deleteCurrentImage()
                },
                onDetailsClick = {
                    mediaGalleryScreenState.showContextualMenu(false)
                    resultNavigator.setResult(
                        MediaGalleryNavBackArgs(
                            messageId = mediaGalleryViewModel.imageAsset.messageId,
                            isSelfAsset = mediaGalleryViewModel.imageAsset.isSelfAsset,
                            mediaGalleryActionType = MediaGalleryActionType.DETAIL
                        )
                    )
                    resultNavigator.navigateBack()
                },
                onShareAsset = {
                    mediaGalleryScreenState.showContextualMenu(false)
                    mediaGalleryViewModel.shareAsset(context)
                },
                onDownloadAsset = onSaveImageWriteStorageRequest::launch,
                onReplyClick = {
                    mediaGalleryScreenState.showContextualMenu(false)
                    resultNavigator.setResult(
                        MediaGalleryNavBackArgs(
                            messageId = mediaGalleryViewModel.imageAsset.messageId,
                            mediaGalleryActionType = MediaGalleryActionType.REPLY
                        )
                    )
                    resultNavigator.navigateBack()
                },
                onReactionClick = { emoji ->
                    mediaGalleryScreenState.showContextualMenu(false)
                    resultNavigator.setResult(
                        MediaGalleryNavBackArgs(
                            messageId = mediaGalleryViewModel.imageAsset.messageId,
                            emoji = emoji,
                            mediaGalleryActionType = MediaGalleryActionType.REACT
                        )
                    )
                    resultNavigator.navigateBack()
                },
                onOpenAsset = null
            )
        )
    }
}

@Composable
fun MediaGalleryContent(navigator: Navigator, viewModel: MediaGalleryViewModel, mediaGalleryScreenState: MediaGalleryScreenState) {
    val context = LocalContext.current
    val uiState = viewModel.mediaGalleryViewState
    suspend fun showSnackbarMessage(message: String, actionLabel: String?, messageCode: MediaGallerySnackbarMessages) {
        val snackbarResult = mediaGalleryScreenState.snackbarHostState.showSnackbar(message = message, actionLabel = actionLabel)
        when {
            // Show downloads folder when clicking on Snackbar cta button
            messageCode is MediaGallerySnackbarMessages.OnImageDownloaded && snackbarResult == SnackbarResult.ActionPerformed -> {
                openDownloadFolder(context)
            }
        }
    }

    // Snackbar logic
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { messageCode ->
            val (message, actionLabel) = getSnackbarMessage(messageCode, context.resources)
            showSnackbarMessage(message, actionLabel, messageCode)
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(colorsScheme().surface)
    ) {
        ZoomableImage(
            imageAsset = viewModel.imageAsset,
            contentDescription = stringResource(R.string.content_description_image_message)
        )
    }

    DeleteMessageDialog(
        state = uiState.deleteMessageDialogsState,
        actions = viewModel.deleteMessageHelper,
        onDeleted = navigator::navigateBack
    )
}

private fun getSnackbarMessage(messageCode: MediaGallerySnackbarMessages, resources: Resources): Pair<String, String?> {
    val msg = when (messageCode) {
        is MediaGallerySnackbarMessages.OnImageDownloaded -> resources.getString(R.string.media_gallery_on_image_downloaded)
        is MediaGallerySnackbarMessages.OnImageDownloadError -> resources.getString(R.string.media_gallery_on_image_downloaded)
        is MediaGallerySnackbarMessages.DeletingMessageError -> resources.getString(R.string.error_conversation_deleting_message)
    }
    val actionLabel = when (messageCode) {
        is MediaGallerySnackbarMessages.OnImageDownloaded -> resources.getString(R.string.label_show)
        else -> null
    }
    return msg to actionLabel
}
