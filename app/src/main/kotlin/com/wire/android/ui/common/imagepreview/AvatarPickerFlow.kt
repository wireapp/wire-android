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

package com.wire.android.ui.common.imagepreview

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.wire.android.ui.userprofile.avatarpicker.ImageSource
import com.wire.android.util.permission.FileType
import com.wire.android.util.permission.RequestLauncher
import com.wire.android.util.permission.rememberChooseSingleFileFlow
import com.wire.android.util.permission.rememberTakePictureFlow

class AvatarPickerFlow(
    private val takePictureFlow: RequestLauncher,
    private val openGalleryFlow: RequestLauncher,
) {
    fun launch(imageSource: ImageSource) {
        when (imageSource) {
            ImageSource.Camera -> takePictureFlow.launch()
            ImageSource.Gallery -> openGalleryFlow.launch()
        }
    }
}

@Composable
fun rememberPickPictureState(
    onImageSelected: (Uri) -> Unit,
    onPictureTaken: () -> Unit,
    targetPictureFileUri: Uri,
    onCameraPermissionPermanentlyDenied: () -> Unit,
    onGalleryPermissionPermanentlyDenied: () -> Unit,
): AvatarPickerFlow {

    val takePictureFLow = rememberTakePictureFlow(
        onPictureTaken = { wasSaved -> if (wasSaved) onPictureTaken() },
        onPermissionDenied = { /* Nothing to do */ },
        onPermissionPermanentlyDenied = onCameraPermissionPermanentlyDenied,
        targetPictureFileUri = targetPictureFileUri,
    )

    val openGalleryFlow = rememberChooseSingleFileFlow(
        fileType = FileType.Image,
        onFileBrowserItemPicked = { pickedPictureUri -> pickedPictureUri?.let { onImageSelected(it) } },
        onPermissionDenied = { /* Nothing to do */ },
        onPermissionPermanentlyDenied = onGalleryPermissionPermanentlyDenied,
    )

    return remember {
        AvatarPickerFlow(takePictureFLow, openGalleryFlow)
    }
}
