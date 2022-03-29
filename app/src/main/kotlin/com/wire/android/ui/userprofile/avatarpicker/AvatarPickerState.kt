package com.wire.android.ui.userprofile.avatarpicker

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.wire.android.ui.common.imagepreview.AvatarPickerFlow
import com.wire.android.ui.common.imagepreview.rememberPickPictureState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberAvatarPickerState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    modalBottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
): AvatarPickerState {
    val pickPictureState = rememberPickPictureState()

    return remember(pickPictureState) {
        AvatarPickerState(coroutineScope, modalBottomSheetState, pickPictureState)
    }
}

@OptIn(ExperimentalMaterialApi::class)
class AvatarPickerState(
    private val coroutineScope: CoroutineScope,
    val modalBottomSheetState: ModalBottomSheetState,
    val avatarPickerFlow: AvatarPickerFlow,
) {
    fun showModalBottomSheet() {
        coroutineScope.launch { modalBottomSheetState.show() }
    }

    fun openImageSource(imageSource: ImageSource) {
        avatarPickerFlow.launch(imageSource)
        coroutineScope.launch { modalBottomSheetState.hide() }
    }
}

sealed class ImageSource {
    object Camera : ImageSource()
    object Gallery : ImageSource()
}


