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

package com.wire.android.ui.calling.controlbuttons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.permission.PermissionDenialType
import com.wire.android.util.permission.rememberCallingCameraRequestFlow
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun CameraButton(
    isCameraOn: Boolean = false,
    onCameraButtonClicked: () -> Unit,
    onPermissionPermanentlyDenied: (type: PermissionDenialType) -> Unit,
    size: Dp = dimensions().defaultCallingControlsSize,
    modifier: Modifier = Modifier,
) {
    val cameraPermissionCheck = CameraPermissionCheckFlow(
        onPermissionGranted = onCameraButtonClicked,
        onPermanentPermissionDecline = {
            onPermissionPermanentlyDenied(
                PermissionDenialType.CallingCamera
            )
        }
    )

    WireCallControlButton(
        isSelected = isCameraOn,
        iconResId = when (isCameraOn) {
            true -> R.drawable.ic_camera_on
            false -> R.drawable.ic_camera_off
        },
        contentDescription = when (isCameraOn) {
            true -> R.string.content_description_calling_turn_camera_off
            false -> R.string.content_description_calling_turn_camera_on
        },
        onClick = cameraPermissionCheck::launch,
        size = size,
        modifier = modifier,
    )
}

@Composable
private fun CameraPermissionCheckFlow(
    onPermissionGranted: () -> Unit,
    onPermanentPermissionDecline: () -> Unit
) = rememberCallingCameraRequestFlow(
    onPermissionGranted = {
        appLogger.d("Camera permission granted")
        onPermissionGranted()
    },
    onPermissionDenied = { },
    onPermissionPermanentlyDenied = onPermanentPermissionDecline
)

@PreviewMultipleThemes
@Composable
fun PreviewComposableCameraButtonOn() = WireTheme {
    CameraButton(isCameraOn = true, onCameraButtonClicked = { }, onPermissionPermanentlyDenied = { })
}

@PreviewMultipleThemes
@Composable
fun PreviewComposableCameraButtonOff() = WireTheme {
    CameraButton(isCameraOn = false, onCameraButtonClicked = { }, onPermissionPermanentlyDenied = { })
}
