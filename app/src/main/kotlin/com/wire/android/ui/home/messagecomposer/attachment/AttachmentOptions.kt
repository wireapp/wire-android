package com.wire.android.ui.home.messagecomposer.attachment

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.common.AttachmentButton
import com.wire.android.ui.common.dimensions
import com.wire.android.util.permission.UseCameraRequestFlow
import com.wire.android.util.permission.rememberCaptureVideoFlow
import com.wire.android.util.permission.rememberCurrentLocationFlow
import com.wire.android.util.permission.rememberOpenFileBrowserFlow
import com.wire.android.util.permission.rememberOpenGalleryFlow
import com.wire.android.util.permission.rememberTakePictureFlow

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun AttachmentOptionsComponent() {
    val attachmentOptions = buildAttachmentOptionItems()
    LazyVerticalGrid(
        cells = GridCells.Adaptive(dimensions().spacing80x),
        contentPadding = PaddingValues(dimensions().spacing8x),
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        attachmentOptions.forEach { option ->
            item {
                AttachmentButton(stringResource(option.text), option.icon) {
                    option.onClick()
                }
            }
        }
    }
}

@Composable
private fun FileBrowserFlow() = rememberOpenFileBrowserFlow(
    onFileBrowserItemPicked = { pickedFileUri ->
        // TODO: call vm to share raw file data
        appLogger.d("pickedUri is $pickedFileUri")
    },
    onPermissionDenied = { /* TODO: Implement denied permission rationale */ }
)

@Composable
private fun GalleryFlow() = rememberOpenGalleryFlow(
    onGalleryItemPicked = { pickedPictureUri ->
        // TODO: call vm to share raw pic data
        appLogger.d("pickedUri is $pickedPictureUri")
    },
    onPermissionDenied = { /* TODO: Implement denied permission rationale */ }
)

@Composable
private fun TakePictureFlow(): UseCameraRequestFlow {
    return rememberTakePictureFlow(
        shouldPersistUri = { /* TODO: call vm to share raw pic data */ },
        onPermissionDenied = {},
        onPictureTakenUri = Uri.EMPTY // TODO: get uri from fileprovider (FileUtil.kt)
    )
}

@Composable
private fun CaptureVideoFlow(): UseCameraRequestFlow {
    return rememberCaptureVideoFlow(
        shouldPersistUri = { /* TODO: call vm to share raw pic data */ },
        onPermissionDenied = {},
        onVideoCapturedUri = Uri.EMPTY // TODO: get uri from fileprovider (FileUtil.kt)
    )
}

@Composable
private fun ShareCurrentLocationFlow() =
    rememberCurrentLocationFlow(LocalContext.current, onLocationPicked = { /*TODO*/ }, onPermissionDenied = {})

@Composable
private fun buildAttachmentOptionItems(): List<AttachmentOptionItem> {
    val fileFlow = FileBrowserFlow()
    val galleryFlow = GalleryFlow()
    val cameraFlow = TakePictureFlow()
    val captureVideoFlow = CaptureVideoFlow()
    val shareCurrentLocationFlow = ShareCurrentLocationFlow()

    return listOf(
        AttachmentOptionItem(R.string.attachment_share_file, R.drawable.ic_attach_file) { fileFlow.launch() },
        AttachmentOptionItem(R.string.attachment_share_image, R.drawable.ic_gallery) { galleryFlow.launch() },
        AttachmentOptionItem(R.string.attachment_take_photo, R.drawable.ic_camera) { cameraFlow.launch() },
        AttachmentOptionItem(R.string.attachment_record_video, R.drawable.ic_video_icon) { captureVideoFlow.launch() },
        AttachmentOptionItem(R.string.attachment_voice_message, R.drawable.ic_mic_on) {
            // TODO: implement voice message options
        },
        AttachmentOptionItem(R.string.attachment_share_location, R.drawable.ic_location) { shareCurrentLocationFlow.launch() }
    )
}

private data class AttachmentOptionItem(
    @StringRes val text: Int,
    @DrawableRes val icon: Int,
    val onClick: () -> Unit
)

@Preview(showBackground = true)
@Composable
fun PreviewAttachmentComponents() {
    AttachmentOptionsComponent()
}
