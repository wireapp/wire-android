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
fun rememberCaptureVideoFlow(
    shouldPersistUri: (Boolean) -> Unit,
    onPermissionDenied: () -> Unit,
    onVideoCapturedUri: Uri // Uri where the camera will persist the video
): CaptureVideoFlow {
    val context = LocalContext.current

    val captureVideoLauncher: ManagedActivityResultLauncher<Uri, Boolean> = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { hasCapturedVideo ->
        shouldPersistUri(hasCapturedVideo)
    }

    val requestPermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                captureVideoLauncher.launch(onVideoCapturedUri)
            } else {
                onPermissionDenied()
            }
        }

    return remember {
        CaptureVideoFlow(context, onVideoCapturedUri, captureVideoLauncher, requestPermissionLauncher)
    }
}

class CaptureVideoFlow(
    private val context: Context,
    private val onVideoCapturedUri: Uri,
    private val captureVideoLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
    private val cameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    fun launch() {
        if (context.checkPermission(android.Manifest.permission.CAMERA)) {
            captureVideoLauncher.launch(onVideoCapturedUri)
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
}
