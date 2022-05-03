package com.wire.android.ui.home.gallery

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterialApi::class)
data class MediaGalleryScreenState(
    val modalBottomSheetState: ModalBottomSheetState,
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberMediaGalleryScreenState(
    bottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
): MediaGalleryScreenState {

    return remember {
        MediaGalleryScreenState(
            modalBottomSheetState = bottomSheetState,
        )
    }
}
