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

package com.wire.android.util.permission

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wire.android.util.extension.getActivity

/**
 * Flow that will launch the camera for taking a photo.
 * This will handle the permissions request in case there is no permission granted for the camera.
 *
 * @param onPictureTaken action that will be executed for Camera's [ActivityResultContract]
 * @param onPermissionDenied action to be executed when the permissions is denied
 * @param targetPictureFileUri target file where the media will be stored
 */
@Composable
fun rememberTakePictureFlow(
    onPictureTaken: (Boolean) -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit,
    targetPictureFileUri: Uri
): UseCameraRequestFlow {
    val context = LocalContext.current

    val takePictureLauncher: ManagedActivityResultLauncher<Uri, Boolean> = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { hasTakenPicture ->
        onPictureTaken(hasTakenPicture)
    }

    val requestCameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                takePictureLauncher.launch(targetPictureFileUri)
            } else {
                context.getActivity()?.let {
                    it.checkCameraWithStoragePermission(onPermissionDenied) {
                        onPermissionPermanentlyDenied(
                            PermissionDenialType.TakePicture
                        )
                    }
                }
            }
        }

    return remember {
        UseCameraRequestFlow(context, targetPictureFileUri, takePictureLauncher, requestCameraPermissionLauncher)
    }
}
