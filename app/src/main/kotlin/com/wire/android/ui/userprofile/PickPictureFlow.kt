package com.wire.android.ui.userprofile

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wire.android.util.extension.checkPermission


@Composable
internal fun rememberPickPictureFlow(onPicturePicked: (Bitmap?) -> Unit): PickPictureFlow {
    val context = LocalContext.current

    val takePictureLauncher: ManagedActivityResultLauncher<Void?, Bitmap?> = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { nullableBitmap ->
        onPicturePicked(nullableBitmap)
    }

    val cameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                takePictureLauncher.launch()
            } else {
                //denied permission from the user
                Log.d("TEST", "permission is needed to change the profile picture")
            }
        }

    return remember {
        PickPictureFlow(context, takePictureLauncher, cameraPermissionLauncher)
    }
}

internal class PickPictureFlow(
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
