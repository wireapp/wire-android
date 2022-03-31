package com.wire.android.util.permission

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

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
                onPermissionDenied()
            }
        }

    return remember {
        UseCameraRequestFlow(context, targetPictureFileUri, takePictureLauncher, requestCameraPermissionLauncher)
    }
}
