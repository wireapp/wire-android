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
fun rememberRecordAudioRequestFlow(
    shouldPersistUri: (Boolean) -> Unit,
    onPermissionDenied: () -> Unit,
    onAudioRecordedUri: Uri
): RecordAudioRequestFlow {
    val context = LocalContext.current

    val requestPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allPermissionGranted = permissions.values.all { true }
            if (allPermissionGranted) {
                // TODO: launch record audio flow
            } else {
                onPermissionDenied()
            }
        }

    return remember {
        RecordAudioRequestFlow(context, requestPermissionLauncher)
    }
}

class RecordAudioRequestFlow(
    private val context: Context,
    private val audioRecordPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>
) {
    fun launch() {
        if (context.checkPermission(android.Manifest.permission.RECORD_AUDIO) &&
            context.checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) {
            // TODO: launch record audio flow
        } else {
            audioRecordPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }
}
