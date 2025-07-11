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

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import com.wire.android.util.extension.checkPermission
import com.wire.android.util.extension.getActivity

/**
 * Flow that will handle given permissions request in case there is no permission granted.
 * @param permissions permissions required
 * @param onAllPermissionsGranted action that will be executed when all permissions are granted
 * @param onAnyPermissionDenied action to be executed when the permission is denied
 * @param onAnyPermissionPermanentlyDenied action to be executed when the permission is permanently denied
 */
@Composable
fun rememberCheckPermissionsRequestFlow(
    permissions: Array<String>,
    onAllPermissionsGranted: () -> Unit,
    onAnyPermissionDenied: () -> Unit,
    onAnyPermissionPermanentlyDenied: () -> Unit,
    key: Any? = null
): RequestLauncher {
    val context = LocalContext.current

    val permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { areGranted ->
            if (areGranted.isNotEmpty()) {
                if (areGranted.all { it.value }) {
                    onAllPermissionsGranted()
                } else {
                    context.getActivity()?.let { activity ->
                        val shouldShowRequestPermissionRationaleForAnyPermission = permissions
                            .map { permission -> shouldShowRequestPermissionRationale(activity, permission) }
                            .any { it }

                        if (shouldShowRequestPermissionRationaleForAnyPermission) {
                            onAnyPermissionDenied()
                        } else {
                            onAnyPermissionPermanentlyDenied()
                        }
                    }
                }
            }
        }

    return remember(key) {
        RequestLauncher {
            when {
                permissions.isEmpty() -> onAllPermissionsGranted()
                permissions.all { context.checkPermission(it) } -> onAllPermissionsGranted()
                else -> permissionLauncher.launch(permissions)
            }
        }
    }
}
