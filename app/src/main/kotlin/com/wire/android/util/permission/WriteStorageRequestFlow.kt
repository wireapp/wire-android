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

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
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

class WriteStorageRequestFlow(
    private val context: Context,
    private val actionIfAlreadyGranted: () -> Unit,
    private val accessFilePermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    fun launch() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (context.checkPermission(WRITE_EXTERNAL_STORAGE)) {
                actionIfAlreadyGranted()
            } else {
                accessFilePermissionLauncher.launch(WRITE_EXTERNAL_STORAGE)
            }
        } else {
            actionIfAlreadyGranted()
        }
    }
}

@Composable
fun rememberWriteStorageRequestFlow(
    onGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit,
): WriteStorageRequestFlow {
    val context = LocalContext.current
    val requestWriteStoragePermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) onGranted()
            else {
                context.getActivity()?.let {
                    it.checkWriteStoragePermission(onPermissionDenied) {
                        onPermissionPermanentlyDenied(
                            PermissionDenialType.WriteFile
                        )
                    }
                }
            }
        }
    return remember {
        WriteStorageRequestFlow(
            context,
            onGranted,
            requestWriteStoragePermissionLauncher
        )
    }
}
