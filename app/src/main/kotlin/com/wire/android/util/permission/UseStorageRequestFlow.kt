package com.wire.android.util.permission

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import com.wire.android.util.extension.checkPermission

class UseStorageRequestFlow(
    private val mimeType: String,
    private val context: Context,
    private val browseStorageActivityLauncher: ManagedActivityResultLauncher<String, Uri?>,
    private val accessFilePermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    fun launch() {
        if (context.checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            browseStorageActivityLauncher.launch(mimeType)
        } else {
            accessFilePermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}
