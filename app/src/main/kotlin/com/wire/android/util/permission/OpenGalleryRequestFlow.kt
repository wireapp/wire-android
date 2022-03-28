package com.wire.android.util.permission

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberOpenGalleryFlow(
    onGalleryItemPicked: (Uri) -> Unit,
    onPermissionDenied: () -> Unit
): UseStorageRequestFlow {
    val context = LocalContext.current

    val openGalleryLauncher: ManagedActivityResultLauncher<String, Uri?> = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { onChosenPictureUri ->
        onChosenPictureUri?.let { onGalleryItemPicked(it) }
    }

    val requestPermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openGalleryLauncher.launch(MIME_TYPE)
            } else {
                onPermissionDenied()
            }
        }

    return remember {
        UseStorageRequestFlow(MIME_TYPE, context, openGalleryLauncher, requestPermissionLauncher)
    }
}

private const val MIME_TYPE = "image/*"
