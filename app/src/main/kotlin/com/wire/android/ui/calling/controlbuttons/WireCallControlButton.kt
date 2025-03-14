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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.button.wireSecondaryButtonColors
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun WireCallControlButton(
    isSelected: Boolean,
    @DrawableRes iconResId: Int,
    @StringRes contentDescription: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    width: Dp = dimensions().defaultCallingControlsWidth,
    height: Dp = dimensions().defaultCallingControlsHeight,
    iconSize: Dp = dimensions().defaultCallingControlsIconSize
) {
    WireSecondaryIconButton(
        onButtonClicked = onClick,
        iconResource = iconResId,
        shape = CircleShape,
        colors = with(colorsScheme()) {
            wireSecondaryButtonColors().copy(
                selected = inverseSurface,
                selectedOutline = inverseSurface,
                onSelected = inverseOnSurface,
                selectedRipple = inverseOnSurface,
                enabled = secondaryButtonEnabled,
                enabledOutline = secondaryButtonDisabledOutline,
                onEnabled = onSecondaryButtonEnabled,
                enabledRipple = secondaryButtonRipple,
            )
        },
        contentDescription = contentDescription,
        state = when {
            !isEnabled -> WireButtonState.Disabled
            isSelected -> WireButtonState.Selected
            else -> WireButtonState.Default
        },
        minSize = DpSize(width, height),
        minClickableSize = DpSize(width, height),
        iconSize = iconSize,
        modifier = modifier,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewWireCallControlButton() = WireTheme {
    WireCallControlButton(
        isSelected = false,
        iconResId = R.drawable.ic_camera_off,
        contentDescription = 0,
        onClick = { }
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewWireCallControlButtonSelected() = WireTheme {
    WireCallControlButton(
        isSelected = true,
        iconResId = R.drawable.ic_camera_on,
        contentDescription = 0,
        onClick = { }
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewWireCallControlButtonDisabled() = WireTheme {
    WireCallControlButton(
        isSelected = false,
        isEnabled = false,
        iconResId = R.drawable.ic_camera_on,
        contentDescription = 0,
        onClick = { }
    )
}
