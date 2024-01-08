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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.wireColorScheme

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
    colors: SwitchColors = wireSwitchColors()
) {
    Switch(checked, onCheckedChange, modifier, thumbContent, enabled, colors, interactionSource)
}

@Composable
fun wireSwitchColors(
    checkedThumbColor: Color = MaterialTheme.wireColorScheme.switchEnabledThumb,
    checkedTrackColor: Color = MaterialTheme.wireColorScheme.switchEnabledChecked,
    checkedBorderColor: Color = Color.Transparent,
    checkedIconColor: Color = MaterialTheme.wireColorScheme.switchEnabledChecked,
    uncheckedThumbColor: Color = MaterialTheme.wireColorScheme.switchEnabledThumb,
    uncheckedTrackColor: Color = MaterialTheme.wireColorScheme.switchEnabledUnchecked,
    uncheckedBorderColor: Color = Color.Transparent,
    uncheckedIconColor: Color = MaterialTheme.wireColorScheme.switchEnabledUnchecked,
    disabledCheckedThumbColor: Color = MaterialTheme.wireColorScheme.switchDisabledThumb,
    disabledCheckedTrackColor: Color = MaterialTheme.wireColorScheme.switchDisabledChecked,
    disabledCheckedBorderColor: Color = Color.Transparent,
    disabledCheckedIconColor: Color = MaterialTheme.wireColorScheme.switchDisabledChecked,
    disabledUncheckedThumbColor: Color = MaterialTheme.wireColorScheme.switchDisabledThumb,
    disabledUncheckedTrackColor: Color = MaterialTheme.wireColorScheme.switchDisabledUnchecked,
    disabledUncheckedBorderColor: Color = Color.Transparent,
    disabledUncheckedIconColor: Color = MaterialTheme.wireColorScheme.switchDisabledUnchecked,
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

@Preview("Wire switch on")
@Composable
fun PreviewWireSwitchOn() {
    WireSwitch(checked = true, onCheckedChange = {})
}

@Preview("Wire switch off")
@Composable
fun PreviewWireSwitchOff() {
    WireSwitch(checked = false, onCheckedChange = {})
}

@Preview("Wire switch disabled on")
@Composable
fun PreviewWireSwitchDisabledOn() {
    WireSwitch(checked = true, enabled = false, onCheckedChange = {})
}

@Preview("Wire switch disabled off")
@Composable
fun PreviewWireSwitchDisabledOff() {
    WireSwitch(checked = false, enabled = false, onCheckedChange = {})
}
