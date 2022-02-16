package com.wire.android.ui.userprofile.image

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wire.android.util.extension.checkPermission


@Composable
fun rememberTakePictureFlow(
    onPicturePicked: (Bitmap?) -> Unit,
    onPermissionDenied: () -> Unit
): TakePictureFlow {
    val context = LocalContext.current

    val takePictureLauncher: ManagedActivityResultLauncher<Void?, Bitmap?> = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { nullableBitmap ->
        nullableBitmap?.let { onPicturePicked(it) }
    }

    val requestPermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                takePictureLauncher.launch()
            } else {
                onPermissionDenied()
            }
        }

    return remember {
        TakePictureFlow(context, takePictureLauncher, requestPermissionLauncher)
    }
}

class TakePictureFlow(
    private val context: Context,
    private val takePictureLauncher: ManagedActivityResultLauncher<Void?, Bitmap?>,
    private val cameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    fun launch() {
        if (context.checkPermission(android.Manifest.permission.CAMERA)) {
            takePictureLauncher.launch()
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
}

