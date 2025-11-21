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
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.navigation.WireRootNavGraph

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import coil.annotation.ExperimentalCoilApi
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.wire.android.R
import com.wire.android.feature.cells.ui.destinations.PublicLinkScreenDestination
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.WireSheetValue
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.edit.DeleteItemMenuOption
import com.wire.android.ui.edit.DownloadAssetExternallyOption
import com.wire.android.ui.edit.MessageDetailsMenuOption
import com.wire.android.ui.edit.ReactionOption
import com.wire.android.ui.edit.ReplyMessageOption
import com.wire.android.ui.edit.ShareAssetMenuOption
import com.wire.android.ui.edit.SharePublicLinkMenuOption
import com.wire.android.ui.home.conversations.MediaGallerySnackbarMessages
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialog
import com.wire.android.ui.home.conversations.mock.mockedPrivateAsset
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.permission.rememberWriteStoragePermissionFlow
import com.wire.android.util.startFileShareIntent
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.SnackBarMessageHandler
import com.wire.android.util.ui.openDownloadFolder

@OptIn(ExperimentalCoilApi::class)
@Destination<WireRootNavGraph>(
    navArgs = MediaGalleryNavArgs::class,
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
    val context = LocalContext.current
    val onSaveImageWriteStorageRequest = rememberWriteStoragePermissionFlow(
        onPermissionGranted = {
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
        dialogState = mediaGalleryViewModel.deleteMessageDialogState,
        deleteMessage = mediaGalleryViewModel::deleteMessage,
    )

    MediaGalleryContent(
        state = viewModelState,
        onCloseClick = navigator::navigateBack,
        onOptionsClick = mediaGalleryViewModel::onOptionsClick,
        modifier = modifier,
    )

    if (viewModelState.menuItems.isNotEmpty()) {
        MediaGalleryOptionsBottomSheetLayout(
            menuItems = viewModelState.menuItems,
            onMenuIntent = { mediaGalleryViewModel.onMenuIntent(it) },
            onDismiss = { mediaGalleryViewModel.onOptionsDismissed() },
        )
    }

    SnackBarMessageHandler(mediaGalleryViewModel.snackbarMessage) { messageCode ->
        when (messageCode) {
            is MediaGallerySnackbarMessages.OnImageDownloaded -> {
                openDownloadFolder(context) // Show downloads folder when clicking on Snackbar cta button
            }
        }
    }

    HandleActions(mediaGalleryViewModel.actions) { action ->
        when (action) {
            is MediaGalleryAction.Share -> context.startFileShareIntent(action.path, action.assetName)
            is MediaGalleryAction.ShowDetails -> {
                resultNavigator.setResult(
                    MediaGalleryNavBackArgs(
                        messageId = action.messageId,
                        isSelfAsset = action.isSelfAsset,
                        mediaGalleryActionType = MediaGalleryActionType.DETAIL
                    )
                )
                resultNavigator.navigateBack()
            }

            is MediaGalleryAction.React -> {
                resultNavigator.setResult(
                    MediaGalleryNavBackArgs(
                        messageId = action.messageId,
                        emoji = action.emoji,
                        mediaGalleryActionType = MediaGalleryActionType.REACT
                    )
                )
                resultNavigator.navigateBack()
            }

            is MediaGalleryAction.Reply -> {
                resultNavigator.setResult(
                    MediaGalleryNavBackArgs(
                        messageId = action.messageId,
                        mediaGalleryActionType = MediaGalleryActionType.REPLY
                    )
                )
                resultNavigator.navigateBack()
            }

            MediaGalleryAction.Download -> { onSaveImageWriteStorageRequest.launch() }
            is MediaGalleryAction.SharePublicLink -> {
                navigator.navigate(
                    NavigationCommand(
                        PublicLinkScreenDestination(
                            assetId = action.assetId,
                            fileName = action.assetName,
                            publicLinkId = action.publicLinkId,
                            isFolder = false,
                        )
                    )
                )
                mediaGalleryViewModel.onOptionsDismissed()
            }

            MediaGalleryAction.ShowError -> showErrorMessage(context)
            MediaGalleryAction.Close -> navigator.navigateBack()
        }
    }
}

fun showErrorMessage(context: Context) {
    Toast.makeText(context, R.string.label_general_error, Toast.LENGTH_SHORT).show()
}

@Composable
private fun MediaGalleryContent(
    state: MediaGalleryViewState,
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
                state.imageAsset?.let {
                    ZoomableImage(
                        modifier = Modifier.align(Alignment.Center),
                        image = it,
                        contentDescription = stringResource(R.string.content_description_image_message)
                    )
                }
            }
        }
    )
}

@Composable
private fun MediaGalleryOptionsBottomSheetLayout(
    menuItems: List<MediaGalleryMenuItem>,
    onMenuIntent: (MenuIntent) -> Unit,
    onDismiss: () -> Unit,
) {

    val sheetState: WireModalSheetState<Unit> = rememberWireModalSheetState(WireSheetValue.Expanded(Unit))
    val onOptionsClick: (MenuIntent) -> Unit = remember { { sheetState.hide { onMenuIntent(it) } } }

    val menuItems: List<@Composable () -> Unit> = buildList {
        menuItems.forEach { item ->
            when (item) {
                MediaGalleryMenuItem.REACT -> add {
                    ReactionOption(emptySet(), { onOptionsClick(MenuIntent.React(it)) })
                }
                MediaGalleryMenuItem.SHOW_DETAILS -> add {
                    MessageDetailsMenuOption { onOptionsClick(MenuIntent.ShowDetails) }
                }
                MediaGalleryMenuItem.REPLY -> add {
                    ReplyMessageOption { onOptionsClick(MenuIntent.Reply) }
                }
                MediaGalleryMenuItem.DOWNLOAD -> add {
                    DownloadAssetExternallyOption { onOptionsClick(MenuIntent.Download) }
                }
                MediaGalleryMenuItem.SHARE -> add {
                    ShareAssetMenuOption { onOptionsClick(MenuIntent.Share) }
                }
                MediaGalleryMenuItem.SHARE_PUBLIC_LINK -> add {
                    SharePublicLinkMenuOption { onOptionsClick(MenuIntent.Share) }
                }
                MediaGalleryMenuItem.DELETE -> add {
                    DeleteItemMenuOption { onOptionsClick(MenuIntent.Delete) }
                }
            }
        }
    }

    WireModalSheetLayout(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        sheetContent = {
            WireMenuModalSheetContent(menuItems = menuItems)
        }
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewMediaGalleryScreen() = WireTheme {
    MediaGalleryContent(
        state = MediaGalleryViewState(
            imageAsset = MediaGalleryImage.PrivateAsset(mockedPrivateAsset()),
            screenTitle = "Media Gallery",
        ),
        onCloseClick = {},
        onOptionsClick = {}
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewMediaGalleryOptionsBottomSheetLayout() = WireTheme {
     MediaGalleryOptionsBottomSheetLayout(
         onMenuIntent = {},
         onDismiss = {},
         menuItems = emptyList(),
     )
}
