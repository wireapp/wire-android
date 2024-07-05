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
import android.os.Build
import androidx.compose.runtime.Composable

@Composable
fun rememberCameraPermissionFlow(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionPermanentlyDenied: () -> Unit
): RequestLauncher = rememberCheckPermissionsRequestFlow(
    permissions = arrayOf(Manifest.permission.CAMERA),
    onAllPermissionsGranted = onPermissionGranted,
    onAnyPermissionDenied = onPermissionDenied,
    onAnyPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
)

@Composable
fun rememberRecordAudioPermissionFlow(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionPermanentlyDenied: () -> Unit
): RequestLauncher = rememberCheckPermissionsRequestFlow(
    permissions = arrayOf(Manifest.permission.RECORD_AUDIO),
    onAllPermissionsGranted = onPermissionGranted,
    onAnyPermissionDenied = onPermissionDenied,
    onAnyPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
)

@Composable
fun rememberWriteStoragePermissionFlow(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionPermanentlyDenied: () -> Unit
): RequestLauncher = rememberCheckPermissionsRequestFlow(
    permissions = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> emptyArray()
        else -> arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    },
    onAllPermissionsGranted = onPermissionGranted,
    onAnyPermissionDenied = onPermissionDenied,
    onAnyPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
)

@Composable
fun rememberCurrentLocationPermissionFlow(
    onAllPermissionsGranted: () -> Unit,
    onAnyPermissionDenied: () -> Unit,
    onAnyPermissionPermanentlyDenied: () -> Unit
): RequestLauncher = rememberCheckPermissionsRequestFlow(
    permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
    onAllPermissionsGranted = onAllPermissionsGranted,
    onAnyPermissionDenied = onAnyPermissionDenied,
    onAnyPermissionPermanentlyDenied = onAnyPermissionPermanentlyDenied,
)

@Composable
fun rememberShowNotificationsPermissionFlow(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionPermanentlyDenied: () -> Unit
): RequestLauncher = rememberCheckPermissionsRequestFlow(
    permissions = arrayOf(Manifest.permission.POST_NOTIFICATIONS),
    onAllPermissionsGranted = onPermissionGranted,
    onAnyPermissionDenied = onPermissionDenied,
    onAnyPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
)
