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
fun rememberRecordAudioRequestFlow(
    onPermissionAllowed: () -> Unit,
    onPermissionDenied: () -> Unit,
    onAudioPermissionPermanentlyDenied: () -> Unit
): RecordAudioRequestFlow {
    val context = LocalContext.current

    val requestPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allPermissionGranted = permissions.all { it.value }
            if (allPermissionGranted) {
                onPermissionAllowed()
            } else {
                context.getActivity()?.let {
                    if (it.shouldShowRequestPermissionRationale(android.Manifest.permission.RECORD_AUDIO) ||
                        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && it.shouldShowRequestPermissionRationale(
                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                        ))
                    ) {
                        onPermissionDenied()
                    } else {
                        onAudioPermissionPermanentlyDenied()
                    }
                }
            }
        }

    return remember {
        RecordAudioRequestFlow(
            context,
            requestPermissionLauncher,
            onPermissionAllowed
        )
    }
}

class RecordAudioRequestFlow(
    private val context: Context,
    private val audioRecordPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>,
    private val onPermissionAllowed: () -> Unit
) {
    fun launch() {
        if (checkRecordAudioPermissions(context = context)) {
            onPermissionAllowed()
        } else {
            audioRecordPermissionLauncher.launch(getRecordAudioPermissions())
        }
    }
}

private fun getRecordAudioPermissions() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(android.Manifest.permission.RECORD_AUDIO)
    } else {
        arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

private fun checkRecordAudioPermissions(
    context: Context
): Boolean =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        context.checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    } else {
        true
    } && context.checkPermission(android.Manifest.permission.RECORD_AUDIO)
