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
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wire.android.util.extension.getActivity

/**
 * Flow that will launch the camera to record a video.
 * This will handle the permissions request in case there is no permission granted for the camera.
 *
 * @param onVideoRecorded action that will be executed for Camera's [ActivityResultContract]
 * @param onPermissionDenied action to be executed when the permission is denied
 * @param onPermissionPermanentlyDenied action to be executed when the permission is permanently denied
 * @param targetVideoFileUri target file where the media will be stored
 */
@Composable
fun rememberCaptureVideoFlow(
    onVideoRecorded: (Boolean) -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit,
    targetVideoFileUri: Uri
): UseCameraAndWriteStorageRequestFlow {
    val context = LocalContext.current

    val captureVideoLauncher: ManagedActivityResultLauncher<Uri, Boolean> =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CaptureVideo()
        ) { hasCapturedVideo ->
            if (hasCapturedVideo) {
                onVideoRecorded(true)
            }
        }

    val requestVideoPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { areGranted ->
            if (areGranted.all { it.value }) {
                captureVideoLauncher.launch(targetVideoFileUri)
            } else {
                context.getActivity()?.let {
                    it.checkCameraWithStoragePermission(onPermissionDenied) {
                        onPermissionPermanentlyDenied(
                            PermissionDenialType.CaptureVideo
                        )
                    }
                }
            }
        }

    return remember {
        UseCameraAndWriteStorageRequestFlow(
            context,
            targetVideoFileUri,
            captureVideoLauncher,
            requestVideoPermissionLauncher
        )
    }
}

fun AppCompatActivity.checkCameraWithStoragePermission(
    onPermissionDenied: () -> Unit,
    onPermissionPermanentlyDenied: () -> Unit
) {
    if (shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA) ||
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && shouldShowRequestPermissionRationale(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ))
    ) {
        onPermissionDenied()
    } else {
        onPermissionPermanentlyDenied()
    }
}
