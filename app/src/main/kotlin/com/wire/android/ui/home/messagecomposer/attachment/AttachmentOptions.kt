package com.wire.android.ui.home.messagecomposer.attachment

import android.content.Context
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.AttachmentButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.messagecomposer.AttachmentState
import com.wire.android.util.DEFAULT_FILE_MIME_TYPE
import com.wire.android.util.getMimeType
import com.wire.android.util.orDefault
import com.wire.android.util.permission.UseCameraRequestFlow
import com.wire.android.util.permission.UseStorageRequestFlow
import com.wire.android.util.permission.rememberCaptureVideoFlow
import com.wire.android.util.permission.rememberCurrentLocationFlow
import com.wire.android.util.permission.rememberOpenFileBrowserFlow
import com.wire.android.util.permission.rememberOpenGalleryFlow
import com.wire.android.util.permission.rememberRecordAudioRequestFlow
import com.wire.android.util.permission.rememberTakePictureFlow
import com.wire.android.util.toByteArray
import java.io.IOException

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AttachmentOptionsComponent(
    onAttachmentStateChanged: (AttachmentState) -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onError: (String) -> Unit
) {
    val attachmentOptions = buildAttachmentOptionItems(onAttachmentStateChanged)

    // handle view states
//    when (val state = viewModel.attachmentState) {
//        is AttachmentState.Initial -> appLogger.d("Not picked yet")
//        is AttachmentState.Picked -> onSendAttachment(state.attachmentBundle)
//        is AttachmentState.Error -> {
//            // FIXME. later on expand to other possible errors
//            onError(stringResource(R.string.error_unknown_message))
//            viewModel.resetViewState()
//        }
//    }

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
private fun FileBrowserFlow(onAttachmentStateChanged: (AttachmentState) -> Unit): UseStorageRequestFlow {
    val context = LocalContext.current
    return rememberOpenFileBrowserFlow(
        onFileBrowserItemPicked = { pickedFileUri ->
            val pickedAttachmentState = pickAttachment(context, pickedFileUri)
            onAttachmentStateChanged(pickedAttachmentState)
        },
        onPermissionDenied = { /* TODO: Implement denied permission rationale */ }
    )
}

@Composable
private fun GalleryFlow(onAttachmentStateChanged: (AttachmentState) -> Unit): UseStorageRequestFlow {
    val context = LocalContext.current
    return rememberOpenGalleryFlow(
        onGalleryItemPicked = { pickedPictureUri ->
            val pickedAttachmentState = pickAttachment(context, pickedPictureUri)
            onAttachmentStateChanged(pickedAttachmentState)
        },
        onPermissionDenied = { /* TODO: Implement denied permission rationale */ }
    )
}

@Composable
private fun TakePictureFlow(): UseCameraRequestFlow {
    return rememberTakePictureFlow(
        shouldPersistUri = { /* TODO: call vm to share raw pic data */ },
        onPictureTakenUri = Uri.EMPTY, // TODO: get uri from fileprovider (FileUtil.kt)
        onPermissionDenied = { /* TODO: Implement denied permission rationale */ }
    )
}

@Composable
private fun CaptureVideoFlow(): UseCameraRequestFlow {
    return rememberCaptureVideoFlow(
        shouldPersistUri = { /* TODO: call vm to share raw pic data */ },
        onVideoCapturedUri = Uri.EMPTY, // TODO: get uri from fileprovider (FileUtil.kt)
        onPermissionDenied = { /* TODO: Implement denied permission rationale */ }
    )
}

@Composable
private fun ShareCurrentLocationFlow() =
    rememberCurrentLocationFlow(onLocationPicked = { /*TODO*/ }, onPermissionDenied = { /* TODO: Implement denied permission rationale */ })

@Composable
private fun RecordAudioFlow() =
    rememberRecordAudioRequestFlow(
        shouldPersistUri = { /* TODO: call vm to share raw pic data */ },
        onAudioRecordedUri = Uri.EMPTY,
        onPermissionDenied = { /* TODO: Implement denied permission rationale */ }
    )

@Composable
private fun buildAttachmentOptionItems(onAttachmentStateChanged: (AttachmentState) -> Unit): List<AttachmentOptionItem> {
    val fileFlow = FileBrowserFlow(onAttachmentStateChanged)
    val galleryFlow = GalleryFlow(onAttachmentStateChanged)
    val cameraFlow = TakePictureFlow()
    val captureVideoFlow = CaptureVideoFlow()
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

private fun pickAttachment(context: Context, attachmentUri: Uri): AttachmentState {
    return try {
        val attachment =
            AttachmentBundle(
                attachmentUri.getMimeType(context).orDefault(DEFAULT_FILE_MIME_TYPE),
                attachmentUri.toByteArray(context)
            )
        AttachmentState.Picked(attachment)
    } catch (e: IOException) {
        AttachmentState.Error
    }
}

private fun resetAttachmentToNotPicked() {
    //attachmentState = AttachmentState.NotPicked
}

private data class AttachmentOptionItem(
    @StringRes val text: Int,
    @DrawableRes val icon: Int,
    val onClick: () -> Unit
)

@Preview(showBackground = true)
@Composable
fun PreviewAttachmentComponents() {
    AttachmentOptionsComponent({}, {}, {})
}
