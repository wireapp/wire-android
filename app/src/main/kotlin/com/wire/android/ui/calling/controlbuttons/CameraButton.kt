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

import android.content.Context
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.util.extension.checkPermission

@Composable
fun CameraButton(
    modifier: Modifier = Modifier.size(dimensions().defaultCallingControlsSize),
    isCameraOn: Boolean = false,
    onCameraPermissionDenied: () -> Unit,
    onCameraButtonClicked: () -> Unit
) {
    val context = LocalContext.current

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onCameraButtonClicked()
        } else {
            onCameraPermissionDenied()
        }
    }

    WireCallControlButton(
        isSelected = isCameraOn,
        modifier = modifier
    ) { iconColor ->
        Icon(
            modifier = Modifier
                .wrapContentSize()
                .clickable(interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false, radius = dimensions().defaultCallingControlsSize / 2),
                    role = Role.Button,
                    onClick = {
                        verifyCameraPermission(
                            context = context,
                            cameraPermissionLauncher = cameraPermissionLauncher,
                            onCameraButtonClicked = onCameraButtonClicked
                        )
                    }),
            painter = painterResource(
                id = if (isCameraOn) {
                    R.drawable.ic_camera_on
                } else {
                    R.drawable.ic_camera_off
                }
            ),
            contentDescription = stringResource(
                id = if (isCameraOn) R.string.content_description_calling_turn_camera_off
                else R.string.content_description_calling_turn_camera_on
            ),
            tint = iconColor
        )
    }
}

private fun verifyCameraPermission(
    context: Context, cameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>, onCameraButtonClicked: () -> Unit
) {
    if (context.checkPermission(android.Manifest.permission.CAMERA).not()) {
        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    } else {
        onCameraButtonClicked()
    }
}

@Preview
@Composable
fun PreviewComposableCameraButton() {
    CameraButton(onCameraPermissionDenied = { }, onCameraButtonClicked = { })
}
