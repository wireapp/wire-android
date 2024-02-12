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
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wire.android.util.extension.checkPermission
import com.wire.android.util.extension.getActivity

@Composable
fun rememberCurrentLocationFlow(
    onPermissionAllowed: () -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionPermanentlyDenied: () -> Unit
): CurrentLocationRequestFlow {
    val context = LocalContext.current

    val requestPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allPermissionGranted = permissions.all { it.value }
            if (allPermissionGranted) {
                onPermissionAllowed()
            } else {
                context.getActivity()?.let {
                    if (it.shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION) ||
                        it.shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    ) {
                        onPermissionDenied()
                    } else {
                        onPermissionPermanentlyDenied()
                    }
                }
            }
        }

    return remember {
        CurrentLocationRequestFlow(context, requestPermissionLauncher, onPermissionAllowed)
    }
}

class CurrentLocationRequestFlow(
    private val context: Context,
    private val locationPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>,
    private val onPermissionAllowed: () -> Unit
) {
    fun launch() {
        if (checkLocationPermissions(context)) {
            onPermissionAllowed()
        } else {
            locationPermissionLauncher.launch(getLocationPermissions())
        }
    }
}

private fun getLocationPermissions() =
    arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

private fun checkLocationPermissions(
    context: Context
): Boolean = context.checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) &&
        context.checkPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
