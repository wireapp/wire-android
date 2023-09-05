/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.util.permission

import android.content.Context
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wire.android.util.extension.checkPermission
import com.wire.android.util.extension.getActivity

@Composable
fun rememberCallingRecordAudioBluetoothRequestFlow(
    onAudioBluetoothPermissionGranted: () -> Unit,
    onAudioBluetoothPermissionDenied: () -> Unit,
    onAudioBluetoothPermissionPermanentlyDenied: () -> Unit,
): CallingAudioRequestFlow {
    val context = LocalContext.current

    val requestPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var permissionsGranted = true
            permissions.values.forEach { if (!it) permissionsGranted = false }

            if (permissionsGranted) {
                onAudioBluetoothPermissionGranted()
            } else {
                context.getActivity()?.let {
                    if (it.shouldShowRequestPermissionRationale(android.Manifest.permission.RECORD_AUDIO) ||
                        it.shouldShowRequestPermissionRationale(android.Manifest.permission.BLUETOOTH_CONNECT)
                    ) {
                        onAudioBluetoothPermissionDenied()
                    } else {
                        onAudioBluetoothPermissionPermanentlyDenied()
                    }
                }
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
