package com.wire.android.util.permission

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wire.android.util.extension.checkPermission

class WriteStorageRequestFlow(
    private val context: Context,
    private val actionIfAlreadyGranted: () -> Unit,
    private val accessFilePermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    fun launch() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (context.checkPermission(WRITE_EXTERNAL_STORAGE)) {
                actionIfAlreadyGranted()
            } else {
                accessFilePermissionLauncher.launch(WRITE_EXTERNAL_STORAGE)
            }
        } else {
            actionIfAlreadyGranted()
        }
    }
}

@Composable
fun rememberWriteStorageRequestFlow(onGranted: () -> Unit, onDenied: () -> Unit): WriteStorageRequestFlow {
    val context = LocalContext.current
    val requestWriteStoragePermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) onGranted()
            else onDenied()
        }
    return remember { WriteStorageRequestFlow(context, onGranted, requestWriteStoragePermissionLauncher) }
}
