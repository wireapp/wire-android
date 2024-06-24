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
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wire.android.util.extension.getActivity

/**
 * Flow that will launch gallery browser to select a picture.
 * This will handle the permissions request in case there is no permission granted for the storage.
 *
 * @param onGalleryItemPicked action that will be executed when selecting a media result from [ActivityResultContract]
 * @param onPermissionDenied action to be executed when the permissions is denied
 */
@Composable
fun <T> rememberOpenGalleryFlow(
    contract: ActivityResultContract<String, T>,
    onGalleryItemPicked: (T) -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit,
): UseStorageRequestFlow<T> {
    val context = LocalContext.current
    val openGalleryLauncher: ManagedActivityResultLauncher<String, T> =
        rememberLauncherForActivityResult(
            contract
        ) { onChosenPictureUri ->
            onGalleryItemPicked(onChosenPictureUri)
        }

    val requestPermissionLauncher: ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openGalleryLauncher.launch(MIME_TYPE)
            } else {
                context.getActivity()?.let {
                    it.checkStoragePermission(onPermissionDenied) {
                        onPermissionPermanentlyDenied(PermissionDenialType.Gallery)
                    }
                }
            }
        }

    return remember {
        UseStorageRequestFlow(MIME_TYPE, context, openGalleryLauncher, requestPermissionLauncher)
    }
}

private const val MIME_TYPE = "image/*"
