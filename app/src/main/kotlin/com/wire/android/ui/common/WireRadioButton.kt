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

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.button.wireRadioButtonColors
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun WireRadioButton(
    checked: Boolean,
    modifier: Modifier = Modifier,
    onButtonChecked: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    RadioButton(
        modifier = modifier,
        selected = checked,
        enabled = enabled,
        onClick = onButtonChecked,
        colors = wireRadioButtonColors()
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
