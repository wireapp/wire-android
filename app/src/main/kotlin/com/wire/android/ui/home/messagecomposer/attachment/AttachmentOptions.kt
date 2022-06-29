package com.wire.android.ui.home.messagecomposer.attachment

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.common.AttachmentButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorPickingAttachment
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.messagecomposer.AttachmentInnerState
import com.wire.android.ui.home.messagecomposer.AttachmentState
import com.wire.android.util.getTempWritableImageUri
import com.wire.android.util.getTempWritableVideoUri
import com.wire.android.util.permission.*
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import kotlinx.coroutines.launch
import okio.Path
import okio.Path.Companion.toPath

@Composable
fun AttachmentOptionsComponent(
    attachmentInnerState: AttachmentInnerState,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onError: (ConversationSnackbarMessages) -> Unit,
    isFileSharingEnabled: Boolean,
    tempCachePath: Path,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val attachmentOptions = buildAttachmentOptionItems(tempCachePath, isFileSharingEnabled) { pickedUri ->
        scope.launch {
            attachmentInnerState.pickAttachment(pickedUri, tempCachePath)
        }
    }
    configureStateHandling(attachmentInnerState, onSendAttachment, onError)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(dimensions().spacing80x),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(dimensions().spacing8x),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        attachmentOptions.forEach { option ->
            if (option.shouldShow) {
                item { AttachmentButton(stringResource(option.text), option.icon) { option.onClick() } }
            }
        }
    }
}

@Composable
private fun configureStateHandling(
    attachmentInnerState: AttachmentInnerState,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onError: (ConversationSnackbarMessages) -> Unit
) {
    when (val state = attachmentInnerState.attachmentState) {
        is AttachmentState.NotPicked -> appLogger.d("Not picked yet")
        is AttachmentState.Picked -> {
            onSendAttachment(state.attachmentBundle)
            attachmentInnerState.resetAttachmentState()
        }
        is AttachmentState.Error -> {
            onError(ErrorPickingAttachment)
            attachmentInnerState.resetAttachmentState()
        }
    }
}

@Composable
private fun FileBrowserFlow(onFilePicked: (Uri) -> Unit): UseStorageRequestFlow {
    return rememberOpenFileBrowserFlow(
        onFileBrowserItemPicked = onFilePicked,
        onPermissionDenied = { /* TODO: Implement denied permission rationale */ }
    )
}

@Composable
private fun GalleryFlow(onFilePicked: (Uri) -> Unit): UseStorageRequestFlow {
    return rememberOpenGalleryFlow(
        onGalleryItemPicked = onFilePicked,
        onPermissionDenied = { /* TODO: Implement denied permission rationale */ }
    )
}

@Composable
private fun TakePictureFlow(tempCachePath: Path, onPictureTaken: (Uri) -> Unit): UseCameraRequestFlow {
    val context = LocalContext.current
    val imageAttachmentUri = context.getTempWritableImageUri(tempCachePath)
    return rememberTakePictureFlow(
        onPictureTaken = { hasTakenPicture ->
            if (hasTakenPicture)
                onPictureTaken(imageAttachmentUri)
        },
        targetPictureFileUri = imageAttachmentUri,
        onPermissionDenied = { /* TODO: Implement denied permission rationale */ }
    )
}

@Composable
private fun CaptureVideoFlow(tempCachePath: Path, onVideoCaptured: (Uri) -> Unit): UseCameraRequestFlow {
    val context = LocalContext.current
    val videoAttachmentUri = context.getTempWritableVideoUri(tempCachePath)
    return rememberCaptureVideoFlow(
        onVideoRecorded = { hasCapturedVideo ->
            if (hasCapturedVideo)
                onVideoCaptured(videoAttachmentUri)
        },
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
private fun buildAttachmentOptionItems(
    tempCachePath: Path,
    isFileSharingEnabled: Boolean,
    onFilePicked: (Uri) -> Unit
): List<AttachmentOptionItem> {
    val fileFlow = FileBrowserFlow(onFilePicked)
    val galleryFlow = GalleryFlow(onFilePicked)
    val cameraFlow = TakePictureFlow(tempCachePath, onFilePicked)
    val captureVideoFlow = CaptureVideoFlow(tempCachePath, onFilePicked)
    val shareCurrentLocationFlow = ShareCurrentLocationFlow()
    val recordAudioFlow = RecordAudioFlow()

    return listOf(
        AttachmentOptionItem(isFileSharingEnabled, R.string.attachment_share_file, R.drawable.ic_attach_file) { fileFlow.launch() },
        AttachmentOptionItem(isFileSharingEnabled, R.string.attachment_share_image, R.drawable.ic_gallery) { galleryFlow.launch() },
        AttachmentOptionItem(isFileSharingEnabled, R.string.attachment_take_photo, R.drawable.ic_camera) { cameraFlow.launch() },
        AttachmentOptionItem(isFileSharingEnabled, R.string.attachment_record_video, R.drawable.ic_video) { captureVideoFlow.launch() },
        AttachmentOptionItem(isFileSharingEnabled, R.string.attachment_voice_message, R.drawable.ic_mic_on) { recordAudioFlow.launch() },
        AttachmentOptionItem(
            text = R.string.attachment_share_location,
            icon = R.drawable.ic_location
        ) { shareCurrentLocationFlow.launch() }
    )
}

private data class AttachmentOptionItem(
    val shouldShow: Boolean = true,
    @StringRes val text: Int,
    @DrawableRes val icon: Int,
    val onClick: () -> Unit
)

@Preview(showBackground = true)
@Composable
fun PreviewAttachmentComponents() {
    val context = LocalContext.current
    AttachmentOptionsComponent(AttachmentInnerState(context), {}, {}, isFileSharingEnabled = true, tempCachePath = "".toPath())
}
