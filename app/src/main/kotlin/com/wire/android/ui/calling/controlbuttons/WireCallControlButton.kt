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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.button.wireSecondaryButtonColors
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions

@Composable
fun WireCallControlButton(
    isSelected: Boolean,
    @DrawableRes iconResId: Int,
    @StringRes contentDescription: Int,
    onClick: () -> Unit,
    size: Dp = dimensions().defaultCallingControlsSize,
    iconSize: Dp = dimensions().defaultCallingControlsIconSize,
    modifier: Modifier = Modifier,
) {
    WireSecondaryIconButton(
        onButtonClicked = onClick,
        iconResource = iconResId,
        shape = CircleShape,
        colors = with(colorsScheme()) {
            wireSecondaryButtonColors().copy(
                selected = callingControlButtonActive,
                selectedOutline = callingControlButtonActiveOutline,
                onSelected = onCallingControlButtonActive,
                selectedRipple = onCallingControlButtonActive,
                enabled = callingControlButtonInactive,
                enabledOutline = callingControlButtonInactiveOutline,
                onEnabled = onCallingControlButtonInactive,
                enabledRipple = onCallingControlButtonInactive,
            )
        },
        contentDescription = contentDescription,
        state = if (isSelected) WireButtonState.Selected else WireButtonState.Default,
        minSize = DpSize(size, size),
        minClickableSize = DpSize(size, size),
        iconSize = iconSize,
        modifier = modifier.size(size),
    )
}
