package com.wire.android.ui.common.imagepreview

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.wire.android.ui.userprofile.image.ImageSource
import com.wire.android.util.ImageUtil.Companion.postProcessCapturedAvatar
import com.wire.android.util.getDefaultAvatarUri
import com.wire.android.util.getShareableAvatarUri
import com.wire.android.util.permission.UseCameraRequestFlow
import com.wire.android.util.permission.UseStorageRequestFlow
import com.wire.android.util.permission.rememberOpenGalleryFlow
import com.wire.android.util.permission.rememberTakePictureFlow
import kotlinx.coroutines.launch

class AvatarPickerFlow(
    var pictureState: PictureState,
    private val takePictureFlow: UseCameraRequestFlow,
    private val openGalleryFlow: UseStorageRequestFlow
) {
    fun launch(imageSource: ImageSource) {
        when (imageSource) {
            ImageSource.Camera -> takePictureFlow.launch()
            ImageSource.Gallery -> openGalleryFlow.launch()
        }
    }
}

@Composable
fun rememberPickPictureState(): AvatarPickerFlow {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pictureState: PictureState by remember {
        mutableStateOf(PictureState.Initial(getDefaultAvatarUri(context)))
    }
    val onChosenPictureUri = getShareableAvatarUri(context)
    val takePictureFLow = rememberTakePictureFlow(
        shouldPersistUri = { wasSaved ->
            if (wasSaved) {
                scope.launch {
                    postProcessCapturedAvatar(onChosenPictureUri, context)
                }
                pictureState = PictureState.Picked(onChosenPictureUri)
            }
        },
        onPermissionDenied = {
            // TODO: Implement denied permission rationale
        },
        onPictureTakenUri = onChosenPictureUri
    )

    val openGalleryFlow = rememberOpenGalleryFlow(
        onGalleryItemPicked = { pickedPictureUri ->
            pictureState = PictureState.Picked(pickedPictureUri)
        },
        onPermissionDenied = {
            // TODO: Implement denied permission rationale
        }
    )

    return remember(pictureState) {
        AvatarPickerFlow(pictureState, takePictureFLow, openGalleryFlow)
    }
}

sealed class PictureState(open val avatarUri: Uri) {
    data class Initial(override val avatarUri: Uri) : PictureState(avatarUri)
    data class Picked(override val avatarUri: Uri) : PictureState(avatarUri)
}
