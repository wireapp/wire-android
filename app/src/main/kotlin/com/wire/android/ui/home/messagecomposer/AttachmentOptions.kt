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
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.AttachmentButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.ConversationActionPermissionType
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.debug.LocalFeatureVisibilityFlags
import com.wire.android.util.permission.FileType
import com.wire.android.util.permission.RequestLauncher
import com.wire.android.util.permission.rememberCaptureVideoFlow
import com.wire.android.util.permission.rememberChooseMultipleFilesFlow
import com.wire.android.util.permission.rememberChooseSingleFileFlow
import com.wire.android.util.permission.rememberTakePictureFlow

@Composable
fun AttachmentOptionsComponent(
    optionsVisible: Boolean,
    onImagesPicked: (List<Uri>) -> Unit,
    onAttachmentPicked: (UriAsset) -> Unit,
    onRecordAudioMessageClicked: () -> Unit,
    tempWritableImageUri: Uri?,
    tempWritableVideoUri: Uri?,
    isFileSharingEnabled: Boolean,
    onLocationPickerClicked: () -> Unit,
    onPermissionPermanentlyDenied: (type: ConversationActionPermissionType) -> Unit,
    modifier: Modifier = Modifier,
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
        onPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
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

    BoxWithConstraints(modifier.fillMaxSize()) {
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
        val numberOfColumns = (fullWidth / minColumnWidth).toInt()

        LazyVerticalGrid(
            columns = columns,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalArrangement = Arrangement.Center
        ) {
            visibleAttachmentOptions.forEachIndexed { index, option ->
                if (option.shouldShow) {
                    item {
                        val column = index % numberOfColumns
                        val reverseIndex = visibleAttachmentOptions.size - 1 - index

                        var startAnimation by remember { mutableStateOf(false) }

                        val animatedScale by animateFloatAsState(
                            targetValue = if (startAnimation) 1.0f else 0.0f,
                            animationSpec = keyframes {
                                durationMillis = 150
                                if (startAnimation) {
                                    1.2f at 50 using FastOutSlowInEasing
                                    1.0f at 100 using FastOutSlowInEasing
                                } else {
                                    1.0f at 0 using FastOutSlowInEasing
                                    0.0f at 50 using FastOutSlowInEasing
                                }
                            }, label = "attachmentsAnimation"
                        )

                        LaunchedEffect(optionsVisible) {
                            val delayMillis = if (optionsVisible) column * 50L else reverseIndex * 25L
                            kotlinx.coroutines.delay(delayMillis)
                            startAnimation = optionsVisible
                        }

                        AttachmentButton(
                            icon = option.icon,
                            labelStyle = labelStyle,
                            modifier = Modifier.scale(animatedScale),
                            text = stringResource(option.text)
                        ) { option.onClick() }
                    }
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
fun rememberSingleFileBrowserFlow(
    onFilePicked: (Uri) -> Unit,
    onPermissionPermanentlyDenied: () -> Unit,
): RequestLauncher = rememberChooseSingleFileFlow(
    fileType = FileType.Any,
    onFileBrowserItemPicked = { uri ->
        uri?.let(onFilePicked)
    },
    onPermissionDenied = { /* Nothing to do */ },
    onPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
)

@Composable
fun rememberMultipleFileBrowserFlow(
    onFilesPicked: (List<Uri>) -> Unit,
    onPermissionPermanentlyDenied: () -> Unit,
): RequestLauncher = rememberChooseMultipleFilesFlow(
    fileType = FileType.Any,
    onFileBrowserItemPicked = { uris ->
        if (uris.isNotEmpty()) {
            onFilesPicked(uris)
        }
    },
    onPermissionDenied = { /* Nothing to do */ },
    onPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
)

@Composable
private fun rememberMultipleGalleryFlow(
    onImagesPicked: (List<Uri>) -> Unit,
    onPermissionPermanentlyDenied: () -> Unit,
): RequestLauncher = rememberChooseMultipleFilesFlow(
    fileType = FileType.Image,
    onFileBrowserItemPicked = { uris ->
        if (uris.isNotEmpty()) {
            onImagesPicked(uris)
        }
    },
    onPermissionDenied = { /* Nothing to do */ },
    onPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
)

@Composable
private fun rememberTakePictureFlow(
    tempWritableVideoUri: Uri?,
    onPictureTaken: (Uri) -> Unit,
    onPermissionPermanentlyDenied: () -> Unit,
): RequestLauncher? =
    tempWritableVideoUri?.let {
        rememberTakePictureFlow(
            onPictureTaken = { hasTakenPicture ->
                if (hasTakenPicture) {
                    onPictureTaken(it)
                }
            },
            targetPictureFileUri = it,
            onPermissionDenied = { /* Nothing to do */ },
            onPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
        )
    }

@Composable
private fun rememberCaptureVideoFlow(
    tempWritableVideoUri: Uri?,
    onVideoCaptured: (Uri) -> Unit,
    onPermissionPermanentlyDenied: () -> Unit,
): RequestLauncher? =
    tempWritableVideoUri?.let { uri ->
        rememberCaptureVideoFlow(
            onVideoCaptured = { hasCapturedVideo ->
                if (hasCapturedVideo) {
                    onVideoCaptured(uri)
                }
            },
            targetVideoFileUri = uri,
            onPermissionDenied = { /** Nothing to do here when permission is denied once */ },
            onPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
        )
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
    onPermissionPermanentlyDenied: (type: ConversationActionPermissionType) -> Unit,
): List<AttachmentOptionItem> {
    val fileFlow = rememberMultipleFileBrowserFlow(
        { onImagesPicked(it) },
        { onPermissionPermanentlyDenied(ConversationActionPermissionType.ChooseFile) },
    )
    val galleryFlow = rememberMultipleGalleryFlow(
        { onImagesPicked(it) },
        { onPermissionPermanentlyDenied(ConversationActionPermissionType.ChooseImage) },
    )
    val takePictureFlow = rememberTakePictureFlow(
        tempWritableImageUri,
        { onFilePicked(UriAsset(it, false)) },
        { onPermissionPermanentlyDenied(ConversationActionPermissionType.TakePicture) },
    )
    val captureVideoFlow = rememberCaptureVideoFlow(
        tempWritableVideoUri,
        { onFilePicked(UriAsset(it, true)) },
        { onPermissionPermanentlyDenied(ConversationActionPermissionType.CaptureVideo) },
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
                ) { takePictureFlow?.launch() }
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
    WireTheme {
        AttachmentOptionsComponent(
            optionsVisible = true,
            onImagesPicked = {},
            onAttachmentPicked = {},
            isFileSharingEnabled = true,
            tempWritableImageUri = null,
            tempWritableVideoUri = null,
            onRecordAudioMessageClicked = {},
            onLocationPickerClicked = {},
            onPermissionPermanentlyDenied = {},
        )
    }
}

@Preview(name = "Small Screen", widthDp = 320, heightDp = 480, showBackground = true)
@Composable
fun PreviewAttachmentOptionsComponentSmallScreen() {
    WireTheme {
        Box(
            contentAlignment = Alignment.BottomCenter
        ) {
            AttachmentOptionsComponent(
                optionsVisible = true,
                onAttachmentPicked = {},
                onImagesPicked = {},
                isFileSharingEnabled = true,
                tempWritableImageUri = null,
                tempWritableVideoUri = null,
                onRecordAudioMessageClicked = {},
                onLocationPickerClicked = {},
                onPermissionPermanentlyDenied = {},
            )
        }
    }
}

@Preview(name = "Normal Screen", widthDp = 360, heightDp = 640)
@Composable
fun PreviewAttachmentOptionsComponentNormalScreen() {
    WireTheme {
        Box(
            contentAlignment = Alignment.BottomCenter
        ) {
            AttachmentOptionsComponent(
                optionsVisible = true,
                onAttachmentPicked = {},
                onImagesPicked = {},
                isFileSharingEnabled = true,
                tempWritableImageUri = null,
                tempWritableVideoUri = null,
                onRecordAudioMessageClicked = {},
                onLocationPickerClicked = {},
                onPermissionPermanentlyDenied = {},
            )
        }
    }
}

@Preview(name = "Tablet Screen", widthDp = 600, heightDp = 960)
@Composable
fun PreviewAttachmentOptionsComponentTabledScreen() {
    WireTheme {
        Box(
            contentAlignment = Alignment.BottomCenter
        ) {
            AttachmentOptionsComponent(
                optionsVisible = true,
                onAttachmentPicked = {},
                onImagesPicked = {},
                isFileSharingEnabled = true,
                tempWritableImageUri = null,
                tempWritableVideoUri = null,
                onRecordAudioMessageClicked = {},
                onLocationPickerClicked = {},
                onPermissionPermanentlyDenied = {},
            )
        }
    }
}
