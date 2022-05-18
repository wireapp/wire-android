package com.wire.android.util.permission

import android.content.Context
import androidx.activity.compose.ManagedActivityResultLauncher
import com.wire.android.util.extension.checkPermission

class WriteToExternalStorageRequestFlow(
    private val context: Context,
    private val onPermissionGranted: () -> Unit,
    private val writeToExternalStoragePermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    fun launch() {
        if (context.checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            onPermissionGranted()
        } else {
            writeToExternalStoragePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
}
