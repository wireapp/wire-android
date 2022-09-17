package com.wire.android.ui.userprofile.avatarpicker

import android.content.Context
import android.net.Uri
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.wire.android.ui.common.imagepreview.AvatarPickerFlow
import com.wire.android.ui.common.imagepreview.rememberPickPictureState
import com.wire.android.util.ui.UIText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberAvatarPickerState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    modalBottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
    onImageSelected: (Uri) -> Unit,
    onPictureTaken: () -> Unit,
    targetPictureFileUri: Uri
): AvatarPickerState {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val avatarPickerFlow: AvatarPickerFlow = rememberPickPictureState(onImageSelected, onPictureTaken, targetPictureFileUri)

    return remember(avatarPickerFlow) {
        AvatarPickerState(
            context,
            coroutineScope,
            snackbarHostState,
            modalBottomSheetState,
            avatarPickerFlow,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
class AvatarPickerState(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    val snackbarHostState: SnackbarHostState,
    val modalBottomSheetState: ModalBottomSheetState,
    private val avatarPickerFlow: AvatarPickerFlow,
) {

    fun showModalBottomSheet() {
        coroutineScope.launch { modalBottomSheetState.show() }
    }

    fun showSnackbar(uiText: UIText) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(uiText.asString(context.resources))
        }
    }

    private fun openImageSource(imageSource: ImageSource) {
        avatarPickerFlow.launch(imageSource)
        coroutineScope.launch { modalBottomSheetState.hide() }
    }

    fun openCamera() {
        openImageSource(ImageSource.Camera)
    }

    fun openGallery() {
        openImageSource(ImageSource.Gallery)
    }
}

sealed class ImageSource {
    object Camera : ImageSource()
    object Gallery : ImageSource()
}
