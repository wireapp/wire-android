package com.wire.android.ui.userprofile.image

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.wire.android.ui.common.imagepreview.ImagePreviewState
import com.wire.android.util.permission.OpenGalleryFlow
import com.wire.android.util.permission.TakePictureFlow
import com.wire.android.util.permission.rememberOpenGalleryFlow
import com.wire.android.util.permission.rememberTakePictureFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterialApi::class)
class ProfileImageState constructor(
    val coroutineScope: CoroutineScope,
    val previewState: ImagePreviewState,
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
    onPermissionDenied: () -> Unit = {},
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    modalBottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
): ProfileImageState {
    val context = LocalContext.current

    var previewState by remember<MutableState<ImagePreviewState>> {
        mutableStateOf(ImagePreviewState.Initial)
    }

    val takePictureFLow = rememberTakePictureFlow({ nullableBitmap ->
        nullableBitmap?.let {
            previewState = ImagePreviewState.HasData(it)
        }
    }, onPermissionDenied)

    val openGalleryFlow = rememberOpenGalleryFlow({
        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= 29) {
            val source = ImageDecoder.createSource(context.contentResolver, it)

            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, it)
        }

        previewState = ImagePreviewState.HasData(bitmap)
    }, onPermissionDenied)

    return remember(previewState) {
        ProfileImageState(
            previewState = previewState,
            coroutineScope = coroutineScope,
            modalBottomSheetState = modalBottomSheetState,
            takePictureFLow = takePictureFLow,
            openGalleryFlow = openGalleryFlow,
        )
    }
}
