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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.common.AttachmentButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.messagecomposer.AttachmentInnerState
import com.wire.android.ui.home.messagecomposer.AttachmentState
import com.wire.android.util.getWritableImageAttachment
import com.wire.android.util.getWritableVideoAttachment
import com.wire.android.util.permission.UseCameraRequestFlow
import com.wire.android.util.permission.UseStorageRequestFlow
import com.wire.android.util.permission.rememberCaptureVideoFlow
import com.wire.android.util.permission.rememberCurrentLocationFlow
import com.wire.android.util.permission.rememberOpenFileBrowserFlow
import com.wire.android.util.permission.rememberOpenGalleryFlow
import com.wire.android.util.permission.rememberRecordAudioRequestFlow
import com.wire.android.util.permission.rememberTakePictureFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AttachmentOptionsComponent(
    attachmentInnerState: AttachmentInnerState,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val attachmentOptions = buildAttachmentOptionItems { pickedUri -> scope.launch { attachmentInnerState.pickAttachment(pickedUri) } }
    configureStateHandling(attachmentInnerState, onSendAttachment, onError)

    LazyVerticalGrid(
        cells = GridCells.Adaptive(dimensions().spacing80x),
        contentPadding = PaddingValues(dimensions().spacing8x),
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        attachmentOptions.forEach { option ->
            item { AttachmentButton(stringResource(option.text), option.icon) { option.onClick() } }
        }
    }
}

@Composable
private fun configureStateHandling(
    attachmentInnerState: AttachmentInnerState,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onError: (String) -> Unit
) {
    when (val state = attachmentInnerState.attachmentState) {
        is AttachmentState.NotPicked -> appLogger.d("Not picked yet")
        is AttachmentState.Picked -> {
            onSendAttachment(state.attachmentBundle)
            attachmentInnerState.resetAttachmentState()
        }
        is AttachmentState.Error -> {
            // FIXME. later on expand to other possible errors
            onError(stringResource(R.string.error_unknown_message))
            attachmentInnerState.resetAttachmentState()
        }
    }
}

@Composable
private fun FileBrowserFlow(onFilePicked: (Uri) -> Unit): UseStorageRequestFlow {
    return rememberOpenFileBrowserFlow(
        onFileBrowserItemPicked = { pickedFileUri -> onFilePicked(pickedFileUri) },
        onPermissionDenied = { /* TODO: Implement denied permission rationale */ }
    )
}

@Composable
private fun GalleryFlow(onFilePicked: (Uri) -> Unit): UseStorageRequestFlow {
    return rememberOpenGalleryFlow(
        onGalleryItemPicked = { pickedPictureUri -> onFilePicked(pickedPictureUri) },
        onPermissionDenied = { /* TODO: Implement denied permission rationale */ }
    )
}

@Composable
private fun TakePictureFlow(onPictureTaken: (Uri) -> Unit): UseCameraRequestFlow {
    val context = LocalContext.current
    val imageAttachmentUri = getWritableImageAttachment(context)
    return rememberTakePictureFlow(
        onPictureTaken = { onPictureTaken(imageAttachmentUri) },
        targetPictureFileUri = imageAttachmentUri,
        onPermissionDenied = { /* TODO: Implement denied permission rationale */ }
    )
}

@Composable
private fun CaptureVideoFlow(onVideoCaptured: (Uri) -> Unit): UseCameraRequestFlow {
    val context = LocalContext.current
    val videoAttachmentUri = getWritableVideoAttachment(context)
    return rememberCaptureVideoFlow(
        onVideoRecorded = { onVideoCaptured(videoAttachmentUri) },
        targetVideoFileUri = videoAttachmentUri,
        onPermissionDenied = { /* TODO: Implement denied permission rationale */ }
    )
}

@Composable
private fun ShareCurrentLocationFlow() =
    rememberCurrentLocationFlow(onLocationPicked = { /*TODO*/ }, onPermissionDenied = { /* TODO: Implement denied permission rationale */ })

@Composable
private fun RecordAudioFlow() =
    rememberRecordAudioRequestFlow(
        onAudioRecorded = { /* TODO: call vm to share raw pic data */ },
        targetAudioFileUri = Uri.EMPTY,
        onPermissionDenied = { /* TODO: Implement denied permission rationale */ }
    )

@Composable
private fun buildAttachmentOptionItems(onFilePicked: (Uri) -> Unit): List<AttachmentOptionItem> {
    val fileFlow = FileBrowserFlow(onFilePicked)
    val galleryFlow = GalleryFlow(onFilePicked)
    val cameraFlow = TakePictureFlow(onFilePicked)
    val captureVideoFlow = CaptureVideoFlow(onFilePicked)
    val shareCurrentLocationFlow = ShareCurrentLocationFlow()
    val recordAudioFlow = RecordAudioFlow()

    return listOf(
        AttachmentOptionItem(R.string.attachment_share_file, R.drawable.ic_attach_file) { fileFlow.launch() },
        AttachmentOptionItem(R.string.attachment_share_image, R.drawable.ic_gallery) { galleryFlow.launch() },
        AttachmentOptionItem(R.string.attachment_take_photo, R.drawable.ic_camera) { cameraFlow.launch() },
        AttachmentOptionItem(R.string.attachment_record_video, R.drawable.ic_video_icon) { captureVideoFlow.launch() },
        AttachmentOptionItem(R.string.attachment_voice_message, R.drawable.ic_mic_on) { recordAudioFlow.launch() },
        AttachmentOptionItem(R.string.attachment_share_location, R.drawable.ic_location) { shareCurrentLocationFlow.launch() }
    )
}

private data class AttachmentOptionItem(@StringRes val text: Int, @DrawableRes val icon: Int, val onClick: () -> Unit)

@Preview(showBackground = true)
@Composable
fun PreviewAttachmentComponents() {
    val context = LocalContext.current
    AttachmentOptionsComponent(AttachmentInnerState(context), {}, {})
}
