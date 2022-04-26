package com.wire.android.util.permission

import android.content.Context
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wire.android.appLogger
import com.wire.android.util.extension.checkPermission

@Composable
fun rememberCallingRecordAudioBluetoothRequestFlow(
    onAudioBluetoothPermissionGranted: () -> Unit,
    onAudioBluetoothPermissionDenied: () -> Unit,
): CallingAudioRequestFlow {
    val context = LocalContext.current

    val requestPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var permissionsGranted = true
            permissions.values.forEach { if (!it) permissionsGranted = false }

            if (permissionsGranted) {
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
        val audioPermissionEnabled = context.checkPermission(android.Manifest.permission.RECORD_AUDIO)
        val bluetoothPermissionEnabled = context.checkPermission(android.Manifest.permission.BLUETOOTH_CONNECT)

        val neededPermissions = mutableListOf(
            android.Manifest.permission.RECORD_AUDIO
        )

        val permissionsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            neededPermissions.add(android.Manifest.permission.BLUETOOTH_CONNECT)

            audioPermissionEnabled && bluetoothPermissionEnabled
        } else {
            audioPermissionEnabled
        }

        if (permissionsEnabled) {
            permissionGranted()
        } else {
            audioRecordPermissionLauncher.launch(neededPermissions.toTypedArray())
        }
    }
}
