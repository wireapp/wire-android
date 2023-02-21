/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.ui.home.messagecomposer.attachment

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.common.AttachmentButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorPickingAttachment
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.messagecomposer.AttachmentInnerState
import com.wire.android.ui.home.messagecomposer.AttachmentState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.debug.LocalFeatureVisibilityFlags
import com.wire.android.util.getTempWritableImageUri
import com.wire.android.util.getTempWritableVideoUri
import com.wire.android.util.permission.UseCameraRequestFlow
import com.wire.android.util.permission.UseStorageRequestFlow
import com.wire.android.util.permission.rememberCaptureVideoFlow
import com.wire.android.util.permission.rememberCurrentLocationFlow
import com.wire.android.util.permission.rememberOpenFileBrowserFlow
import com.wire.android.util.permission.rememberOpenGalleryFlow
import com.wire.android.util.permission.rememberRecordAudioRequestFlow
import com.wire.android.util.permission.rememberTakePictureFlow
import kotlinx.coroutines.launch
import okio.Path
import okio.Path.Companion.toPath

@Composable
fun AttachmentOptions(
    attachmentInnerState: AttachmentInnerState,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onMessageComposerError: (ConversationSnackbarMessages) -> Unit,
    isFileSharingEnabled: Boolean,
    tempCachePath: Path,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
    ) {
        Divider(color = MaterialTheme.wireColorScheme.outline)
        AttachmentOptionsComponent(
            attachmentInnerState,
            onSendAttachment,
            onMessageComposerError,
            isFileSharingEnabled,
            tempCachePath
        )
    }
}

@Composable
private fun AttachmentOptionsComponent(
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

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fullWidth: Dp = with(LocalDensity.current) { constraints.maxWidth.toDp() }
        val minColumnWidth: Dp = dimensions().spacing80x
        val minPadding: Dp = dimensions().spacing8x
        val visibleAttachmentOptions = attachmentOptions.filter { it.shouldShow }
        val params by remember(fullWidth, visibleAttachmentOptions.size) {
            derivedStateOf {
                calculateGridParams(minPadding, minColumnWidth, fullWidth, visibleAttachmentOptions.size)
            }
        }
        val (columns, contentPadding) = params

        LazyVerticalGrid(
            columns = columns,
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalArrangement = Arrangement.Center
        ) {
            visibleAttachmentOptions.forEach { option ->
                if (option.shouldShow) {
                    item { AttachmentButton(stringResource(option.text), option.icon) { option.onClick() } }
                }
            }
        }
    }
}

private fun calculateGridParams(minPadding: Dp, minColumnWidth: Dp, fullWidth: Dp, itemsCount: Int): Pair<GridCells, PaddingValues> {
    val availableWidth = fullWidth - (minPadding * 2)
    val currentMaxColumns = availableWidth / minColumnWidth
    return if (currentMaxColumns <= itemsCount) {
        GridCells.Adaptive(minColumnWidth) to PaddingValues(minPadding)
    } else {
        val currentPadding = (availableWidth - (minColumnWidth * itemsCount)) / 2
        GridCells.Fixed(itemsCount) to PaddingValues(vertical = minPadding, horizontal = currentPadding)
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
fun FileBrowserFlow(onFilePicked: (Uri) -> Unit): UseStorageRequestFlow {
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
            if (hasTakenPicture) {
                onPictureTaken(imageAttachmentUri)
            }
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
            if (hasCapturedVideo) {
                onVideoCaptured(videoAttachmentUri)
            }
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

    return buildList {
        val localFeatureVisibilityFlags = LocalFeatureVisibilityFlags.current

        with(localFeatureVisibilityFlags) {
            add(
                AttachmentOptionItem(
                    isFileSharingEnabled,
                    R.string.attachment_share_file,
                    R.drawable.ic_attach_file
                ) { fileFlow.launch() }
            )
            add(
                AttachmentOptionItem(
                    isFileSharingEnabled,
                    R.string.attachment_share_image,
                    R.drawable.ic_gallery
                ) { galleryFlow.launch() }
            )
            add(
                AttachmentOptionItem(
                    isFileSharingEnabled,
                    R.string.attachment_take_photo,
                    R.drawable.ic_camera
                ) { cameraFlow.launch() }
            )
            add(
                AttachmentOptionItem(
                    isFileSharingEnabled,
                    R.string.attachment_record_video,
                    R.drawable.ic_video
                ) { captureVideoFlow.launch() }
            )
            if (AudioMessagesIcon) {
                add(
                    AttachmentOptionItem(
                        isFileSharingEnabled,
                        R.string.attachment_voice_message,
                        R.drawable.ic_mic_on
                    ) { recordAudioFlow.launch() })
            }
            if (ShareLocationIcon) {
                add(
                    AttachmentOptionItem(
                        text = R.string.attachment_share_location,
                        icon = R.drawable.ic_location
                    ) { shareCurrentLocationFlow.launch() }
                )
            }
        }
    }
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
