/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.home.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireSwitch
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun SettingsOptionSwitch(
    switchState: SwitchState,
    trailingOnText: String? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (switchState is SwitchState.Visible) {
            if (switchState.isOnOffVisible) {
                HorizontalSpace.x8()
                Text(
                    text = stringResource(if (switchState.value) R.string.label_on else R.string.label_off),
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.onBackground
                )
            }
            if (trailingOnText != null) {
                HorizontalSpace.x2()
                Text(
                    text = trailingOnText,
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                )
            }
            HorizontalSpace.x8()
            if (switchState.isSwitchVisible) {
                WireSwitch(
                    checked = switchState.value,
                    enabled = switchState is SwitchState.Enabled,
                    onCheckedChange = (switchState as? SwitchState.Enabled)?.onCheckedChange
                )
            }
        }
    }
}

sealed class SwitchState {
    data object None : SwitchState()
    sealed class Visible(
        open val value: Boolean = false,
        open val isOnOffVisible: Boolean = true,
        open val isSwitchVisible: Boolean = true
    ) : SwitchState()

    data class Enabled(
        override val value: Boolean = false,
        override val isOnOffVisible: Boolean = true,
        val onCheckedChange: ((Boolean) -> Unit)?
    ) : Visible(value = value, isOnOffVisible = isOnOffVisible, isSwitchVisible = true)

    data class Disabled(
        override val value: Boolean = false,
        override val isOnOffVisible: Boolean = true
    ) : Visible(value = value, isOnOffVisible = isOnOffVisible, isSwitchVisible = true)

    data class TextOnly(
        override val value: Boolean = false,
    ) : Visible(value = value, isOnOffVisible = true, isSwitchVisible = false)
}
