package com.wire.android.ui.common

import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun WireSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: @Composable () -> Unit = { },
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SwitchColors = wireSwitchColors()
) {
    Switch(checked, onCheckedChange, modifier, thumbContent, enabled, interactionSource, colors)
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
fun WireSwitchOnPreview() {
    WireSwitch(checked = true, onCheckedChange = {})
}

@Preview("Wire switch off")
@Composable
fun WireSwitchOffPreview() {
    WireSwitch(checked = false, onCheckedChange = {})
}

@Preview("Wire switch disabled on")
@Composable
fun WireSwitchDisabledOnPreview() {
    WireSwitch(checked = true, enabled = false, onCheckedChange = {})
}

@Preview("Wire switch disabled off")
@Composable
fun WireSwitchDisabledOffPreview() {
    WireSwitch(checked = false, enabled = false, onCheckedChange = {})
}
