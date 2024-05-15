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
package com.wire.android.ui.common.textfield

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun Label(
    labelText: String,
    labelMandatoryIcon: Boolean = false,
    state: WireTextFieldState = WireTextFieldState.Default,
    interactionSource: InteractionSource = remember { MutableInteractionSource() },
    colors: WireTextFieldColors = wireTextFieldColors()
) {
    Row {
        Text(
            text = labelText,
            style = MaterialTheme.wireTypography.label01,
            color = colors.labelColor(state, interactionSource).value,
            modifier = Modifier.padding(bottom = dimensions().spacing4x, end = dimensions().spacing4x)
        )
        if (labelMandatoryIcon) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_input_mandatory),
                tint = colors.labelMandatoryColor(state).value,
                contentDescription = "",
                modifier = Modifier.padding(top = dimensions().spacing2x)
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewLabel() = WireTheme {
    Label("Label", true)
}
