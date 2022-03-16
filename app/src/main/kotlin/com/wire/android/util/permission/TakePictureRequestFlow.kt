package com.wire.android.util.permission

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wire.android.util.extension.checkPermission

@Composable
fun rememberTakePictureFlow(
    shouldPersistUri: (Boolean) -> Unit,
    onPermissionDenied: () -> Unit,
    onPictureTakenUri: Uri // Uri where the camera will persist the picture
): TakePictureFlow {
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
        TakePictureFlow(context, onPictureTakenUri, takePictureLauncher, requestPermissionLauncher)
    }
}

class TakePictureFlow(
    private val context: Context,
    private val onPictureTakenUri: Uri,
    private val takePictureLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
    private val cameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    fun launch() {
        if (context.checkPermission(android.Manifest.permission.CAMERA)) {
            takePictureLauncher.launch(onPictureTakenUri)
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
}

