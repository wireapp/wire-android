package com.wire.android.util.permission

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Flow that will launch file browser to select a file.
 * This will handle the permissions request in case there is no permission granted for the storage.
 *
 * @param onFileBrowserItemPicked action that will be executed when selecting a file result from [ActivityResultContract]
 * @param onPermissionDenied action to be executed when the permissions is denied
 */
@Composable
fun rememberOpenFileBrowserFlow(
    onFileBrowserItemPicked: (Uri) -> Unit,
    onPermissionDenied: () -> Unit
): UseStorageRequestFlow {
    val context = LocalContext.current

    val openFileBrowserLauncher: ManagedActivityResultLauncher<String, Uri?> = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { onChosenFileUri ->
        onChosenFileUri?.let { onFileBrowserItemPicked(it) }
    }

    val requestPermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openFileBrowserLauncher.launch(MIME_TYPE)
            } else {
                onPermissionDenied()
            }
        }

    return remember {
        UseStorageRequestFlow(MIME_TYPE, context, openFileBrowserLauncher, requestPermissionLauncher)
    }
}

private const val MIME_TYPE = "*/*"
