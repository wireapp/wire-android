package com.wire.android.util.permission

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import com.wire.android.util.extension.checkPermission

class UseCameraRequestFlow(
    private val context: Context,
    private val targetMediaFileUri: Uri,
    private val activityLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
    private val cameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    fun launch() {
        if (context.checkPermission(android.Manifest.permission.CAMERA)) {
            activityLauncher.launch(targetMediaFileUri)
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
}
