/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */
package com.wire.android.util.permission

import android.Manifest
import android.content.Context
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wire.android.util.extension.checkPermission
import com.wire.android.util.extension.getActivity

@Composable
fun rememberCallingCameraRequestFlow(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionPermanentlyDenied: () -> Unit
): CallingCameraRequestFlow {
    val context = LocalContext.current

    val requestPermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                onPermissionGranted()
            } else {
                context.getActivity()?.let {
                    if (it.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        onPermissionDenied()
                    } else {
                        onPermissionPermanentlyDenied()
                    }
                }
            }
        }

    return remember {
        CallingCameraRequestFlow(context, onPermissionGranted, requestPermissionLauncher)
    }
}

class CallingCameraRequestFlow(
    private val context: Context,
    private val permissionGranted: () -> Unit,
    private val audioRecordPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    fun launch() {
        val cameraPermissionEnabled = context.checkPermission(Manifest.permission.CAMERA)

        if (cameraPermissionEnabled) {
            permissionGranted()
        } else {
            audioRecordPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}
