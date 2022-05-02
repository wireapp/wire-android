package com.wire.android.ui.home.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.ImageAsset
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediaGalleryScreen(mediaGalleryViewModel: MediaGalleryViewModel = hiltViewModel()) {
    val uiState = mediaGalleryViewModel.mediaGalleryViewState
    val mediaGalleryScreenState = rememberMediaGalleryScreenState()

    with(uiState) {
        MenuModalSheetLayout(
            sheetState = mediaGalleryScreenState.modalBottomSheetState,
            menuItems = EditGalleryMenuItems(onDeleteMessage = {}),
            content = {
                Scaffold(
                    topBar = {
                        MediaGalleryScreenTopAppBar(
                            title = screenTitle ?: stringResource(R.string.media_gallery_default_title_name),
                            onCloseClick = mediaGalleryViewModel::navigateBack,
                            onOptionsClick = mediaGalleryScreenState::showEditContextMenu
                        )
                    },
                    content = { MediaGalleryContent(mediaGalleryViewModel.imageAssetId) }
                )
            }
        )
    }
}

@Composable
fun MediaGalleryContent(imageAsset: ImageAsset.PrivateAsset) {
    val imageLoader = hiltViewModel<MediaGalleryViewModel>().wireSessionImageLoader

    Box(Modifier.fillMaxWidth().fillMaxHeight().background(colorsScheme().surface)) {
        ZoomableImage(imageAsset = imageAsset, contentDescription = stringResource(R.string.content_description_user_avatar), imageLoader = imageLoader)
    }
}

@Composable
fun EditGalleryMenuItems(
    onDeleteMessage: () -> Unit
): List<@Composable () -> Unit> {
    return buildList {
        add {
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
