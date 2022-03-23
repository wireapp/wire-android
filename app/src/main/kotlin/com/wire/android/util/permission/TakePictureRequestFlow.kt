package com.wire.android.util.permission

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberTakePictureFlow(
    shouldPersistUri: (Boolean) -> Unit,
    onPermissionDenied: () -> Unit,
    onPictureTakenUri: Uri
): UseCameraRequestFlow {
    val context = LocalContext.current

    val takePictureLauncher: ManagedActivityResultLauncher<Uri, Boolean> = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { hasTakenPicture ->
        shouldPersistUri(hasTakenPicture)
    }

    val requestPermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                takePictureLauncher.launch(onPictureTakenUri)
            } else {
                onPermissionDenied()
            }
        }

    return remember {
        UseCameraRequestFlow(context, onPictureTakenUri, takePictureLauncher, requestPermissionLauncher)
    }
}
