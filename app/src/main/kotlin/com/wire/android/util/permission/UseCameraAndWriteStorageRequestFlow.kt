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

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import com.wire.android.util.extension.checkPermission

class UseCameraAndWriteStorageRequestFlow(
    private val context: Context,
    private val targetMediaFileUri: Uri,
    private val activityLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
    private val cameraPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>
) {

    private val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            arrayOf(android.Manifest.permission.CAMERA)
        }

    fun launch() {
        if (requiredPermissions.all { context.checkPermission(it) }) {
            activityLauncher.launch(targetMediaFileUri)
        } else {
            cameraPermissionLauncher.launch(requiredPermissions)
        }
    }
}
