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

package com.wire.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.PreviewMultipleThemes

@Composable
fun WireRadioButton(
    checked: Boolean,
    modifier: Modifier = Modifier,
    onButtonChecked: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = MaterialTheme.wireColorScheme
    val dimensions = MaterialTheme.wireDimensions

    val radioModifier = if (onButtonChecked != null) {
        Modifier.selectable(
            selected = checked,
            enabled = enabled,
            role = Role.RadioButton,
            interactionSource = interactionSource,
            indication = null,
            onClick = onButtonChecked
        )
    } else {
        Modifier
    }

    WireRadioButtonIndicator(
        checked = checked,
        enabled = enabled,
        focused = focused,
        selectedColor = colors.primary,
        selectedContentColor = colors.surface,
        unselectedColor = colors.secondaryText,
        unselectedBackgroundColor = colors.background,
        disabledSelectedColor = colors.primaryButtonDisabled,
        disabledUnselectedColor = colors.secondaryButtonDisabledOutline,
        modifier = modifier
            .then(radioModifier)
            .size(dimensions.radioButtonSize)
    )
}

@Composable
private fun WireRadioButtonIndicator(
    checked: Boolean,
    enabled: Boolean,
    focused: Boolean,
    selectedColor: Color,
    selectedContentColor: Color,
    unselectedColor: Color,
    unselectedBackgroundColor: Color,
    disabledSelectedColor: Color,
    disabledUnselectedColor: Color,
    modifier: Modifier = Modifier
) {
    val dimensions = MaterialTheme.wireDimensions

    when {
        checked && focused && enabled -> {
            Box(
                modifier = modifier
                    .border(
                        width = dimensions.radioButtonFocusBorderWidth,
                        color = selectedColor,
                        shape = CircleShape
                    )
                    .padding(dimensions.radioButtonFocusGapWidth)
                    .background(
                        color = selectedColor,
                        shape = CircleShape
                    )
                    .border(
                        width = dimensions.radioButtonSelectedFocusGapWidth,
                        color = selectedContentColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                RadioButtonInnerDot(selectedContentColor)
            }
        }

        checked -> {
            val color = if (enabled) selectedColor else disabledSelectedColor
            Box(
                modifier = modifier
                    .background(
                        color = color,
                        shape = CircleShape
                    )
                    .border(
                        width = dimensions.radioButtonSelectedBorderWidth,
                        color = color,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                RadioButtonInnerDot(selectedContentColor)
            }
        }

        focused && enabled -> {
            Box(
                modifier = modifier
                    .background(
                        color = unselectedBackgroundColor,
                        shape = CircleShape
                    )
                    .border(
                        width = dimensions.radioButtonUnselectedBorderWidth,
                        color = selectedColor,
                        shape = CircleShape
                    )
            )
        }

        else -> {
            Box(
                modifier = modifier
                    .background(
                        color = unselectedBackgroundColor,
                        shape = CircleShape
                    )
                    .border(
                        width = dimensions.radioButtonUnselectedBorderWidth,
                        color = if (enabled) unselectedColor else disabledUnselectedColor,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun RadioButtonInnerDot(color: Color) {
    val dimensions = MaterialTheme.wireDimensions

    Box(
        modifier = Modifier
            .size(dimensions.radioButtonInnerDotSize)
            .background(
                color = color,
                shape = CircleShape
            )
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewWireRadioButtons() = WireTheme {
    Column {
        WireRadioButton(checked = true, enabled = true, onButtonChecked = {})
        WireRadioButton(checked = false, enabled = true, onButtonChecked = {})
        WireRadioButton(checked = true, enabled = false, onButtonChecked = {})
        WireRadioButton(checked = false, enabled = false, onButtonChecked = {})
    }
}
