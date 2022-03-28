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
): UseCameraRequestFlow {
    val context = LocalContext.current

    val captureVideoLauncher: ManagedActivityResultLauncher<Uri, Boolean> = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { hasCapturedVideo ->
        onVideoRecorded(hasCapturedVideo)
    }

    val requestPermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                captureVideoLauncher.launch(targetVideoFileUri)
            } else {
                onPermissionDenied()
            }
        }

    return remember {
        UseCameraRequestFlow(context, targetVideoFileUri, captureVideoLauncher, requestPermissionLauncher)
    }
}
