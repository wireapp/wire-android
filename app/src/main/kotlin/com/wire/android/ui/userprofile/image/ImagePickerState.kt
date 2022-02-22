package com.wire.android.ui.userprofile.image

import android.graphics.Bitmap
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.wire.android.ui.common.imagepreview.PicturePickerFlow
import com.wire.android.ui.common.imagepreview.rememberPickPictureState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberProfileImageState(
    initialBitmap: Bitmap,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    modalBottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
): ImagePickerState {
    val pickPictureState = rememberPickPictureState(initialBitmap = initialBitmap)

    return remember(pickPictureState) {
        ImagePickerState(coroutineScope, modalBottomSheetState, pickPictureState)
    }
}

@OptIn(ExperimentalMaterialApi::class)
class ImagePickerState(
    private val coroutineScope: CoroutineScope,
    val modalBottomSheetState: ModalBottomSheetState,
    val picturePickerFlow: PicturePickerFlow,
) {

    fun showModalBottomSheet() {
        coroutineScope.launch { modalBottomSheetState.show() }
    }

    fun openImageSource(imageSource: ImageSource) {
        picturePickerFlow.launch(imageSource)
        coroutineScope.launch { modalBottomSheetState.hide() }
    }
}

sealed class ImageSource {
    object Camera : ImageSource()
    object Gallery : ImageSource()
}


