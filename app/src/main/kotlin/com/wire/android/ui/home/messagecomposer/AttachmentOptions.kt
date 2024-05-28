/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.ui.home.messagecomposer

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.ui.common.AttachmentButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.debug.LocalFeatureVisibilityFlags
import com.wire.android.util.permission.PermissionDenialType
import com.wire.android.util.permission.UseCameraAndWriteStorageRequestFlow
import com.wire.android.util.permission.UseCameraRequestFlow
import com.wire.android.util.permission.UseStorageRequestFlow
import com.wire.android.util.permission.rememberCaptureVideoFlow
import com.wire.android.util.permission.rememberOpenFileBrowserFlow
import com.wire.android.util.permission.rememberOpenGalleryFlow
import com.wire.android.util.permission.rememberTakePictureFlow
import com.wire.android.util.ui.KeyboardHeight

@Composable
fun AttachmentOptionsComponent(
    onImagesPicked: (List<Uri>) -> Unit,
    onAttachmentPicked: (UriAsset) -> Unit,
    onRecordAudioMessageClicked: () -> Unit,
    tempWritableImageUri: Uri?,
    tempWritableVideoUri: Uri?,
    isFileSharingEnabled: Boolean,
    onLocationPickerClicked: () -> Unit,
    onCaptureVideoPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val attachmentOptions = buildAttachmentOptionItems(
        isFileSharingEnabled = isFileSharingEnabled,
        tempWritableImageUri = tempWritableImageUri,
        tempWritableVideoUri = tempWritableVideoUri,
        onImagesPicked = onImagesPicked,
        onFilePicked = onAttachmentPicked,
        onRecordAudioMessageClicked = onRecordAudioMessageClicked,
        onLocationPickerClicked = onLocationPickerClicked,
        onPermissionPermanentlyDenied = onCaptureVideoPermissionPermanentlyDenied
    )

    val labelStyle = MaterialTheme.wireTypography.button03

    /**
     * Calculate the maximum text width among a list of attachment options.
     */
    val maxTextWidth: Int = attachmentOptions
        .map { optionItem ->
            val label = stringResource(optionItem.text)
            val longestLabel = if (label.contains(" ")) {
                label.split(" ").maxBy { it.length }
            } else {
                label
            }
            // Measure the width of the longest label using the specified typography style
            textMeasurer.measure(
                longestLabel,
                labelStyle
            ).size.width
        }
        .maxBy { it }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fullWidth: Dp = with(density) { constraints.maxWidth.toDp() }
        val minPadding: Dp = dimensions().spacing2x
        val minColumnWidth: Dp = with(density) { maxTextWidth.toDp() + dimensions().spacing28x }
        val visibleAttachmentOptions = attachmentOptions.filter { it.shouldShow }
        val params by remember(fullWidth, visibleAttachmentOptions.size) {
            derivedStateOf {
                calculateGridParams(minPadding, minColumnWidth, fullWidth, visibleAttachmentOptions.size)
            }
        }
        val (columns, contentPadding) = params

        LazyVerticalGrid(
            columns = columns,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalArrangement = Arrangement.Center
        ) {
            visibleAttachmentOptions.forEach { option ->
                if (option.shouldShow) {
                    item { AttachmentButton(stringResource(option.text), option.icon, labelStyle) { option.onClick() } }
                }
            }
        }
    }
}

private fun calculateGridParams(
    minPadding: Dp,
    minColumnWidth: Dp,
    fullWidth: Dp,
    itemsCount: Int
): Pair<GridCells, PaddingValues> {
    // Calculate the width available for columns by subtracting the minimum padding from both sides
    val availableWidth = fullWidth - (minPadding * 2)
    // Determine the maximum number of columns that can fit in the available width
    val currentMaxColumns = availableWidth / minColumnWidth
    // Check if the maximum number of columns is less than or equal to the number of items
    return if (currentMaxColumns <= itemsCount) {
        // If so, use adaptive grid cells with the minimum column width and minimum padding
        GridCells.Adaptive(minColumnWidth) to PaddingValues(minPadding)
    } else {
        // Otherwise, calculate the padding needed to center the columns
        val currentPadding = (availableWidth - (minColumnWidth * itemsCount)) / 2
        // Use fixed grid cells with the exact number of items and calculated padding
        GridCells.Fixed(itemsCount) to PaddingValues(vertical = minPadding, horizontal = currentPadding)
    }
}

@Composable
fun FileBrowserFlow(
    onFilePicked: (Uri) -> Unit,
    onPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit
): UseStorageRequestFlow<Uri?> {
    return rememberOpenFileBrowserFlow(
        contract = ActivityResultContracts.GetContent(),
        onFileBrowserItemPicked = { uri ->
            uri?.let(onFilePicked)
        },
        onPermissionDenied = { /* Nothing to do */ },
        onPermissionPermanentlyDenied = onPermissionPermanentlyDenied
    )
}

@Composable
fun MultipleFileBrowserFlow(
    onFilesPicked: (List<Uri>) -> Unit,
    onPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit
): UseStorageRequestFlow<List<Uri>> {
    return rememberOpenFileBrowserFlow(
        contract = ActivityResultContracts.GetMultipleContents(),
        onFileBrowserItemPicked = { uris ->
            if (uris.isNotEmpty()) {
                onFilesPicked(uris)
            }
        },
        onPermissionDenied = { /* Nothing to do */ },
        onPermissionPermanentlyDenied = onPermissionPermanentlyDenied
    )
}

@Composable
private fun MultipleGalleryFlow(
    onImagesPicked: (List<Uri>) -> Unit,
    onPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit
): UseStorageRequestFlow<List<Uri>> {
    return rememberOpenGalleryFlow(
        contract = ActivityResultContracts.GetMultipleContents(),
        onGalleryItemPicked = { uris ->
            if (uris.isNotEmpty()) {
                onImagesPicked(uris)
            }
        },
        onPermissionDenied = { /* Nothing to do */ },
        onPermissionPermanentlyDenied = onPermissionPermanentlyDenied
    )
}

@Composable
private fun TakePictureFlow(
    tempWritableVideoUri: Uri?,
    onPictureTaken: (Uri) -> Unit,
    onPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit
): UseCameraRequestFlow? {
    tempWritableVideoUri?.let {
        return rememberTakePictureFlow(
            onPictureTaken = { hasTakenPicture ->
                if (hasTakenPicture) {
                    onPictureTaken(it)
                }
            },
            targetPictureFileUri = it,
            onPermissionDenied = { /* Nothing to do */ },
            onPermissionPermanentlyDenied = onPermissionPermanentlyDenied
        )
    }
    return null
}

@Composable
private fun captureVideoFlow(
    tempWritableVideoUri: Uri?,
    onVideoCaptured: (Uri) -> Unit,
    onPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit,
): UseCameraAndWriteStorageRequestFlow? {
    tempWritableVideoUri?.let { uri ->
        return rememberCaptureVideoFlow(
            onVideoRecorded = { onVideoCaptured(uri) },
            targetVideoFileUri = uri,
            onPermissionDenied = { /** Nothing to do here when permission is denied once */ },
            onPermissionPermanentlyDenied = onPermissionPermanentlyDenied
        )
    }
    return null
}

@Composable
private fun buildAttachmentOptionItems(
    isFileSharingEnabled: Boolean,
    tempWritableImageUri: Uri?,
    tempWritableVideoUri: Uri?,
    onImagesPicked: (List<Uri>) -> Unit,
    onFilePicked: (UriAsset) -> Unit,
    onRecordAudioMessageClicked: () -> Unit,
    onLocationPickerClicked: () -> Unit,
    onPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit
): List<AttachmentOptionItem> {
    val fileFlow = MultipleFileBrowserFlow(
        remember { { onImagesPicked(it) } },
        onPermissionPermanentlyDenied
    )
    val galleryFlow = MultipleGalleryFlow(
        remember { { onImagesPicked(it) } },
        onPermissionPermanentlyDenied
    )
    val cameraFlow = TakePictureFlow(
        tempWritableImageUri,
        remember { { onFilePicked(UriAsset(it, false)) } },
        onPermissionPermanentlyDenied
    )
    val captureVideoFlow = captureVideoFlow(
        tempWritableVideoUri,
        remember { { onFilePicked(UriAsset(it, true)) } },
        onPermissionPermanentlyDenied
    )

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
                ) { cameraFlow?.launch() }
            )
            add(
                AttachmentOptionItem(
                    isFileSharingEnabled,
                    R.string.attachment_record_video,
                    R.drawable.ic_video
                ) { captureVideoFlow?.launch() }
            )
            if (AudioMessagesIcon) {
                add(
                    AttachmentOptionItem(
                        isFileSharingEnabled,
                        R.string.attachment_voice_message,
                        R.drawable.ic_mic_on,
                        onRecordAudioMessageClicked
                    )
                )
            }
            if (ShareLocationIcon) {
                add(
                    AttachmentOptionItem(
                        text = R.string.attachment_share_location,
                        icon = R.drawable.ic_location
                    ) {
                        onLocationPickerClicked()
                    }
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

@Preview(showBackground = true, locale = "de")
@Composable
fun PreviewAttachmentComponents() {
    AttachmentOptionsComponent(
        onImagesPicked = {},
        onAttachmentPicked = {},
        isFileSharingEnabled = true,
        tempWritableImageUri = null,
        tempWritableVideoUri = null,
        onRecordAudioMessageClicked = {},
        onLocationPickerClicked = {},
        onCaptureVideoPermissionPermanentlyDenied = {}
    )
}

@Preview(name = "Small Screen", widthDp = 320, heightDp = 480, showBackground = true)
@Composable
fun PreviewAttachmentOptionsComponentSmallScreen() {
    Surface {
        Box(
            modifier = Modifier.height(KeyboardHeight.default),
            contentAlignment = Alignment.BottomCenter
        ) {
            AttachmentOptionsComponent(
                onAttachmentPicked = {},
                onImagesPicked = {},
                isFileSharingEnabled = true,
                tempWritableImageUri = null,
                tempWritableVideoUri = null,
                onRecordAudioMessageClicked = {},
                onLocationPickerClicked = {},
                onCaptureVideoPermissionPermanentlyDenied = {}
            )
        }
    }
}

@Preview(name = "Normal Screen", widthDp = 360, heightDp = 640)
@Composable
fun PreviewAttachmentOptionsComponentNormalScreen() {
    Surface {
        Box(
            modifier = Modifier.height(KeyboardHeight.default),
            contentAlignment = Alignment.BottomCenter
        ) {
            AttachmentOptionsComponent(
                onAttachmentPicked = {},
                onImagesPicked = {},
                isFileSharingEnabled = true,
                tempWritableImageUri = null,
                tempWritableVideoUri = null,
                onRecordAudioMessageClicked = {},
                onLocationPickerClicked = {},
                onCaptureVideoPermissionPermanentlyDenied = {}
            )
        }
    }
}

@Preview(name = "Tablet Screen", widthDp = 600, heightDp = 960)
@Composable
fun PreviewAttachmentOptionsComponentTabledScreen() {
    Surface {
        Box(
            modifier = Modifier.height(KeyboardHeight.default),
            contentAlignment = Alignment.BottomCenter
        ) {
            AttachmentOptionsComponent(
                onAttachmentPicked = {},
                onImagesPicked = {},
                isFileSharingEnabled = true,
                tempWritableImageUri = null,
                tempWritableVideoUri = null,
                onRecordAudioMessageClicked = {},
                onLocationPickerClicked = {},
                onCaptureVideoPermissionPermanentlyDenied = {}
            )
        }
    }
}
