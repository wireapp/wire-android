/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

package com.wire.android.ui.common.button

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.PreviewMultipleThemes

@Composable
fun WireSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
        .scale(scaleX = 0.75f, scaleY = 0.75f)
        .size(width = 36.dp, height = 24.dp),
    thumbContent: @Composable () -> Unit = { },
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SwitchColors = wireSwitchColors(),
    toggleActionDescription: String? = null
) {
    Switch(
        checked,
        onCheckedChange,
        modifier.semantics { toggleActionDescription?.let { onClick(it) { false } } },
        thumbContent,
        enabled,
        colors,
        interactionSource
    )
}

@Composable
fun wireSwitchColors(
    checkedThumbColor: Color = MaterialTheme.wireColorScheme.surface,
    checkedTrackColor: Color = MaterialTheme.wireColorScheme.positive,
    checkedBorderColor: Color = Color.Transparent,
    checkedIconColor: Color = MaterialTheme.wireColorScheme.onSurface,
    uncheckedThumbColor: Color = MaterialTheme.wireColorScheme.surface,
    uncheckedTrackColor: Color = MaterialTheme.wireColorScheme.onPrimaryButtonDisabled,
    uncheckedBorderColor: Color = Color.Transparent,
    uncheckedIconColor: Color = MaterialTheme.wireColorScheme.onPrimaryButtonDisabled,
    disabledCheckedThumbColor: Color = MaterialTheme.wireColorScheme.onPrimaryButtonDisabled,
    disabledCheckedTrackColor: Color = MaterialTheme.wireColorScheme.primaryButtonDisabled,
    disabledCheckedBorderColor: Color = Color.Transparent,
    disabledCheckedIconColor: Color = MaterialTheme.wireColorScheme.primaryButtonDisabled,
    disabledUncheckedThumbColor: Color = MaterialTheme.wireColorScheme.onPrimaryButtonDisabled,
    disabledUncheckedTrackColor: Color = MaterialTheme.wireColorScheme.primaryButtonDisabled,
    disabledUncheckedBorderColor: Color = Color.Transparent,
    disabledUncheckedIconColor: Color = MaterialTheme.wireColorScheme.primaryButtonDisabled,
): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = checkedThumbColor,
    checkedTrackColor = checkedTrackColor,
    checkedBorderColor = checkedBorderColor,
    checkedIconColor = checkedIconColor,
    uncheckedThumbColor = uncheckedThumbColor,
    uncheckedTrackColor = uncheckedTrackColor,
    uncheckedBorderColor = uncheckedBorderColor,
    uncheckedIconColor = uncheckedIconColor,
    disabledCheckedThumbColor = disabledCheckedThumbColor,
    disabledCheckedTrackColor = disabledCheckedTrackColor,
    disabledCheckedBorderColor = disabledCheckedBorderColor,
    disabledCheckedIconColor = disabledCheckedIconColor,
    disabledUncheckedThumbColor = disabledUncheckedThumbColor,
    disabledUncheckedTrackColor = disabledUncheckedTrackColor,
    disabledUncheckedBorderColor = disabledUncheckedBorderColor,
    disabledUncheckedIconColor = disabledUncheckedIconColor
)

@PreviewMultipleThemes
@Composable
fun PreviewWireSwitchOn() = WireTheme {
    WireSwitch(checked = true, onCheckedChange = {})
}

@PreviewMultipleThemes
@Composable
fun PreviewWireSwitchOff() = WireTheme {
    WireSwitch(checked = false, onCheckedChange = {})
}

@PreviewMultipleThemes
@Composable
fun PreviewWireSwitchDisabledOn() = WireTheme {
    WireSwitch(checked = true, enabled = false, onCheckedChange = {})
}

@PreviewMultipleThemes
@Composable
fun PreviewWireSwitchDisabledOff() = WireTheme {
    WireSwitch(checked = false, enabled = false, onCheckedChange = {})
}
