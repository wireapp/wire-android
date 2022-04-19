package com.wire.android.util.permission

import android.content.Context
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wire.android.util.extension.checkPermission

@Composable
fun rememberCallingRecordAudioBluetoothRequestFlow(
    onAudioBluetoothPermissionGranted: () -> Unit,
    onAudioBluetoothPermissionDenied: () -> Unit,
): CallingAudioRequestFlow {
    val context = LocalContext.current

    val requestPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allPermissionGranted = permissions.values.all { true }
            if (allPermissionGranted) {
                onAudioBluetoothPermissionGranted()
            } else {
                onAudioBluetoothPermissionDenied()
            }
        }

    return remember {
        CallingAudioRequestFlow(context, onAudioBluetoothPermissionGranted, requestPermissionLauncher)
    }
}

class CallingAudioRequestFlow(
    private val context: Context,
    private val permissionGranted: () -> Unit,
    private val audioRecordPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>
) {
    fun launch() {
        if (context.checkPermission(android.Manifest.permission.RECORD_AUDIO) &&
            context.checkPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
        ) {
            permissionGranted()
        } else {
            audioRecordPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }
    }
}
