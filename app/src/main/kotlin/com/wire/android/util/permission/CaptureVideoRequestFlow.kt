package com.wire.android.util.permission

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberCaptureVideoFlow(
    shouldPersistUri: (Boolean) -> Unit,
    onPermissionDenied: () -> Unit,
    onVideoCapturedUri: Uri
): UseCameraRequestFlow {
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
        UseCameraRequestFlow(context, onVideoCapturedUri, captureVideoLauncher, requestPermissionLauncher)
    }
}
