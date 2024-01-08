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
import androidx.activity.compose.ManagedActivityResultLauncher
import com.wire.android.util.extension.checkPermission

class UseCameraRequestFlow(
    private val context: Context,
    private val targetMediaFileUri: Uri,
    private val activityLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
    private val cameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    fun launch() {
        if (context.checkPermission(android.Manifest.permission.CAMERA)) {
            activityLauncher.launch(targetMediaFileUri)
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
}
