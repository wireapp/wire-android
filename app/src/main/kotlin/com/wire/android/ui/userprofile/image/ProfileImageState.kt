package com.wire.android.ui.userprofile.image

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterialApi::class)
class ProfileImageState constructor(
    val coroutineScope: CoroutineScope,
    val modalBottomSheetState: ModalBottomSheetState,
    val takePictureFLow: TakePictureFlow,
    val openGalleryFlow: OpenGalleryFlow,
) {

    fun showModalBottomSheet() {
        coroutineScope.launch { modalBottomSheetState.show() }
    }

    fun openCamera() {
        takePictureFLow.launch()
    }

    fun openGallery() {
        openGalleryFlow.launch()
    }

}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberProfileImageState(
    onPictureTaken: (Bitmap?) -> Unit,
    onGalleryItemPicked: (Uri?) -> Unit,
    onPermissionDenied: () -> Unit,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    modalBottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
): ProfileImageState {
    val takePictureFLow = rememberTakePictureFlow(onPictureTaken, onPermissionDenied)
    val openGalleryFlow = rememberOpenGalleryFlow(onGalleryItemPicked, onPermissionDenied)

    return remember {
        ProfileImageState(
            coroutineScope = coroutineScope,
            modalBottomSheetState = modalBottomSheetState,
            takePictureFLow = takePictureFLow,
            openGalleryFlow = openGalleryFlow,
        )
    }
}
