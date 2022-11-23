package com.wire.android.util.permission

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import com.wire.android.util.extension.checkPermission

class UseStorageRequestFlow(
    private val mimeType: String,
    private val context: Context,
    private val browseStorageActivityLauncher: ManagedActivityResultLauncher<String, Uri?>,
    private val accessFilePermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    fun launch() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (context.checkPermission(READ_EXTERNAL_STORAGE)) {
                browseStorageActivityLauncher.launch(mimeType)
            } else {
                accessFilePermissionLauncher.launch(READ_EXTERNAL_STORAGE)
            }
        } else {
            browseStorageActivityLauncher.launch(mimeType)
        }
    }
}
