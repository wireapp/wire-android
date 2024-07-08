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

import android.content.ActivityNotFoundException
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable

/**
 * Flow that will launch the given [ActivityResultContract].
 * This will handle given permissions request in case there is no permission granted.
 *
 * @param resultContract [ActivityResultContract] to be launched
 * @param input input for the given [ActivityResultContract]
 * @param requiredPermissions permissions required for the given [ActivityResultContract]
 * @param onResult action that will be executed when the given [ActivityResultContract] returns a result
 * @param onAnyPermissionDenied action to be executed when the permission is denied
 * @param onAnyPermissionPermanentlyDenied action to be executed when the permission is permanently denied
 * @param onActivityNotFound action to be executed when no activity to handle given [ActivityResultContract] is found
 */
@Composable
fun <P, T> rememberCheckPermissionsAndLaunchIntentRequestFlow(
    resultContract: ActivityResultContract<P, T>,
    input: P,
    requiredPermissions: Array<String>,
    onResult: (T) -> Unit,
    onAnyPermissionDenied: () -> Unit,
    onAnyPermissionPermanentlyDenied: () -> Unit,
    onActivityNotFound: () -> Unit,
): RequestLauncher {

    val intentLauncher: ManagedActivityResultLauncher<P, T> =
        rememberLauncherForActivityResult(resultContract) { result ->
            onResult(result)
        }

    return rememberCheckPermissionsRequestFlow(
        permissions = requiredPermissions,
        onAllPermissionsGranted = {
            try {
                intentLauncher.launch(input)
            } catch (e: ActivityNotFoundException) {
                onActivityNotFound()
            }
        },
        onAnyPermissionDenied = onAnyPermissionDenied,
        onAnyPermissionPermanentlyDenied = onAnyPermissionPermanentlyDenied,
    )
}
