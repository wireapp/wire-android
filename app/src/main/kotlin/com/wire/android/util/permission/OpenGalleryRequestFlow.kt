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
fun rememberOpenGalleryFlow(
    onGalleryItemPicked: (Uri) -> Unit,
    onPermissionDenied: () -> Unit
): OpenGalleryFlow {
    val context = LocalContext.current

    val openGalleryLauncher: ManagedActivityResultLauncher<String, Uri?> = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { onChosenPictureUri ->
        onChosenPictureUri?.let { onGalleryItemPicked(it) }
    }

    val requestPermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openGalleryLauncher.launch("image/*")
            } else {
                onPermissionDenied()
            }
        }

    return remember {
        OpenGalleryFlow(context, openGalleryLauncher, requestPermissionLauncher)
    }
}

class OpenGalleryFlow(
    private val context: Context,
    private val openGalleryLauncher: ManagedActivityResultLauncher<String, Uri?>,
    private val accessFilePermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    fun launch() {
        if (context.checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            openGalleryLauncher.launch("image/*")
        } else {
            accessFilePermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}

