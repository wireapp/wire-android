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

/**
 * Flow that will launch the camera to record a video.
 * This will handle the permissions request in case there is no permission granted for the camera.
 *
 * @param onVideoRecorded action that will be executed for Camera's [ActivityResultContract]
 * @param onPermissionDenied action to be executed when the permissions is denied
 * @param targetVideoFileUri target file where the media will be stored
 */
@Composable
fun rememberCaptureVideoFlow(
    onVideoRecorded: (Boolean) -> Unit,
    onPermissionDenied: () -> Unit,
    targetVideoFileUri: Uri
): UseCameraAndWriteStorageRequestFlow {
    val context = LocalContext.current

    val captureVideoLauncher: ManagedActivityResultLauncher<Uri, Boolean> = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { hasCapturedVideo ->
        onVideoRecorded(hasCapturedVideo)
    }

    val requestVideoPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { areGranted ->
            if (areGranted.all { it.value }) {
                captureVideoLauncher.launch(targetVideoFileUri)
            } else {
                onPermissionDenied()
            }
        }

    return remember {
        UseCameraAndWriteStorageRequestFlow(context, targetVideoFileUri, captureVideoLauncher, requestVideoPermissionLauncher)
    }
}
