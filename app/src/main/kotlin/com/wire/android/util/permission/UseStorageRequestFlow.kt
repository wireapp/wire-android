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

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.wire.android.util.extension.checkPermission

class UseStorageRequestFlow(
    private val mimeType: String,
    private val context: Context,
    private val browseStorageActivityLauncher: ManagedActivityResultLauncher<String, Uri?>,
    private val accessFilePermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    fun launch() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (context.checkPermission(READ_EXTERNAL_STORAGE)) {
                browseStorageActivityLauncher.launch(mimeType)
            } else {
                accessFilePermissionLauncher.launch(READ_EXTERNAL_STORAGE)
            }
        } else {
            browseStorageActivityLauncher.launch(mimeType)
        }
    }
}

fun AppCompatActivity.checkStoragePermission(
    onPermissionDenied: () -> Unit,
    onPermissionPermanentlyDenied: () -> Unit
) {
    if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) &&
        shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE)
    ) {
        onPermissionDenied()
    } else {
        onPermissionPermanentlyDenied()
    }
}