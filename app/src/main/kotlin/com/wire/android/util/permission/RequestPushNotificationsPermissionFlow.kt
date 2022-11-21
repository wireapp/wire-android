package com.wire.android.util.permission

import android.content.Context
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wire.android.util.extension.checkPermission

class RequestPushNotificationsPermissionFlow(
    private val context: Context,
    private val notificationPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {

    fun launch() {
        if (!context.checkPermission(android.Manifest.permission.POST_NOTIFICATIONS)) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun rememberRequestPushNotificationsPermissionFlow(
    onPermissionDenied: () -> Unit,
): RequestPushNotificationsPermissionFlow {
    val context = LocalContext.current

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) { onPermissionDenied() }
    }

    return remember {
        RequestPushNotificationsPermissionFlow(context, requestPermissionLauncher)
    }
}
