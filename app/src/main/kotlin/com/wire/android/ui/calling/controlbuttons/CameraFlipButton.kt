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
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun CameraFlipButton(
    isOnFrontCamera: Boolean = false,
    onCameraFlipButtonClicked: () -> Unit,
    size: Dp = dimensions().defaultCallingControlsSize,
    modifier: Modifier = Modifier,
) {
    WireCallControlButton(
        isSelected = !isOnFrontCamera,
        iconResId = when (isOnFrontCamera) {
            true -> R.drawable.ic_camera_flipped
            false -> R.drawable.ic_camera_flip
        },
        contentDescription = when (isOnFrontCamera) {
            true -> R.string.content_description_calling_flip_camera_on
            false -> R.string.content_description_calling_flip_camera_off
        },
        onClick = onCameraFlipButtonClicked,
        size = size,
        modifier = modifier,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewCameraFlipButtonOn() = WireTheme {
    CameraFlipButton(isOnFrontCamera = true, onCameraFlipButtonClicked = { })
}

@PreviewMultipleThemes
@Composable
fun PreviewCameraFlipButtonOff() = WireTheme {
    CameraFlipButton(isOnFrontCamera = false, onCameraFlipButtonClicked = { })
}
