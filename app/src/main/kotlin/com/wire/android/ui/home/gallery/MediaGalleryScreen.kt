package com.wire.android.ui.home.gallery

import android.app.DownloadManager
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.home.conversations.MediaGallerySnackbarMessages

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediaGalleryScreen(mediaGalleryViewModel: MediaGalleryViewModel = hiltViewModel()) {
    val uiState = mediaGalleryViewModel.mediaGalleryViewState
    val mediaGalleryScreenState = rememberMediaGalleryScreenState()
    val scope = rememberCoroutineScope()

    with(uiState) {
        MenuModalSheetLayout(
            sheetState = mediaGalleryScreenState.modalBottomSheetState,
            coroutineScope = scope,
            menuItems = EditGalleryMenuItems(
                onDeleteMessage = {
                    mediaGalleryScreenState.showContextualMenu(false)
                    mediaGalleryViewModel.deleteCurrentImageMessage()
                },
                onDownloadImage = {
                    mediaGalleryScreenState.showContextualMenu(false)
                    mediaGalleryViewModel.saveImageToExternalStorage()
                }
            ),
            content = {
                Scaffold(
                    topBar = {
                        MediaGalleryScreenTopAppBar(
                            title = screenTitle ?: stringResource(R.string.media_gallery_default_title_name),
                            onCloseClick = mediaGalleryViewModel::navigateBack,
                            onOptionsClick = { mediaGalleryScreenState.showContextualMenu(true) }
                        )
                    },
                    content = { internalPadding ->
                        Box(modifier = Modifier.padding(internalPadding)) {
                            MediaGalleryContent(mediaGalleryViewModel, mediaGalleryScreenState)
                        }
                    },
                    snackbarHost = {
                        SwipeDismissSnackbarHost(
                            hostState = mediaGalleryScreenState.snackbarHostState,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                )
            }
        )
    }
}

@Composable
fun MediaGalleryContent(viewModel: MediaGalleryViewModel, mediaGalleryScreenState: MediaGalleryScreenState) {
    val imageLoader = hiltViewModel<MediaGalleryViewModel>().wireSessionImageLoader
    val context = LocalContext.current
    val uiState = viewModel.mediaGalleryViewState

    suspend fun showSnackbarMessage(message: String, actionLabel: String?, messageCode: MediaGallerySnackbarMessages) {
        val snackbarResult = mediaGalleryScreenState.snackbarHostState.showSnackbar(message = message, actionLabel = actionLabel)
        appLogger.d("Snackbar result is -> $snackbarResult")
        when {
            // Show downloads folder when clicking on Snackbar cta button
            messageCode is MediaGallerySnackbarMessages.OnImageDownloaded && snackbarResult == SnackbarResult.ActionPerformed -> {
                context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
            }
        }
    }

    // Snackbar logic
    uiState.onSnackbarMessage?.let { messageCode ->
        val (message, actionLabel) = getSnackbarMessage(messageCode)
        LaunchedEffect(message) {
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
            imageAsset = viewModel.imageAssetId,
            contentDescription = stringResource(R.string.content_description_image_message),
            imageLoader = imageLoader
        )
    }
}

@Composable
private fun getSnackbarMessage(messageCode: MediaGallerySnackbarMessages): Pair<String, String?> {
    val msg = when (messageCode) {
        is MediaGallerySnackbarMessages.OnImageDownloaded -> stringResource(R.string.media_gallery_on_image_downloaded)
    }
    val actionLabel = when (messageCode) {
        is MediaGallerySnackbarMessages.OnImageDownloaded -> stringResource(R.string.label_show)
        else -> null
    }
    return msg to actionLabel
}

@Composable
fun EditGalleryMenuItems(
    onDownloadImage: () -> Unit,
    onDeleteMessage: () -> Unit
): List<@Composable () -> Unit> {
    return buildList {
        add {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.secondary) {
                MenuBottomSheetItem(
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_download,
                            contentDescription = stringResource(R.string.content_description_download_icon),
                        )
                    },
                    title = stringResource(R.string.label_download),
                    onItemClick = onDownloadImage
                )
            }
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                MenuBottomSheetItem(
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_delete,
                            contentDescription = stringResource(R.string.content_description_delete_the_message),
                        )
                    },
                    title = stringResource(R.string.label_delete),
                    onItemClick = onDeleteMessage
                )
            }
        }
    }
}
