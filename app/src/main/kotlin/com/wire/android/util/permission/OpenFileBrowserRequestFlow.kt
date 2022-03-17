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
fun rememberOpenFileBrowserFlow(
    onFileBrowserItemPicked: (Uri) -> Unit,
    onPermissionDenied: () -> Unit
): OpenFileBrowserFlow {
    val context = LocalContext.current

    val openFileBrowserLauncher: ManagedActivityResultLauncher<String, Uri?> = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { onChosenFileUri ->
        onChosenFileUri?.let { onFileBrowserItemPicked(it) }
    }

    val requestPermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openFileBrowserLauncher.launch("*/*")
            } else {
                onPermissionDenied()
            }
        }

    return remember {
        OpenFileBrowserFlow(context, openFileBrowserLauncher, requestPermissionLauncher)
    }
}

class OpenFileBrowserFlow(
    private val context: Context,
    private val openFileBrowserLauncher: ManagedActivityResultLauncher<String, Uri?>,
    private val accessFilePermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    fun launch() {
        if (context.checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            openFileBrowserLauncher.launch("*/*")
        } else {
            accessFilePermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}
