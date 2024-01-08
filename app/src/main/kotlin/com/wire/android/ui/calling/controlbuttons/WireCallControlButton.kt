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

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.wireSecondaryButtonColors
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions

@Composable
fun WireCallControlButton(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    icon: @Composable (iconColor: Color) -> Unit,
) {
    val iconColor = if (isSelected) colorsScheme().onCallingControlButtonActive else colorsScheme().onCallingControlButtonInactive
    WireSecondaryButton(
        modifier = modifier.size(dimensions().defaultCallingControlsSize),
        onClick = {},
        leadingIcon = { icon(iconColor) },
        shape = CircleShape,
        colors = with(colorsScheme()) {
            wireSecondaryButtonColors().copy(
                selected = callingControlButtonActive,
                selectedOutline = callingControlButtonActiveOutline,
                onSelected = onCallingControlButtonActive,
                enabled = callingControlButtonInactive,
                enabledOutline = callingControlButtonInactiveOutline,
                onEnabled = onCallingControlButtonInactive
            )
        },
        state = if (isSelected) WireButtonState.Selected else WireButtonState.Default
    )
}
