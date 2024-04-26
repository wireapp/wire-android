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

package com.wire.android.ui.common.button

import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun wireCheckBoxColors() = CheckboxDefaults.colors(
    uncheckedColor = MaterialTheme.wireColorScheme.uncheckedColor,
    checkedColor = MaterialTheme.colorScheme.primary,
    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
    disabledCheckedColor = MaterialTheme.wireColorScheme.disabledCheckedColor,
    disabledUncheckedColor = MaterialTheme.wireColorScheme.disabledUncheckedColor,
    disabledIndeterminateColor = MaterialTheme.wireColorScheme.disabledIndeterminateColor
)

@Composable
fun wireRadioButtonColors() = RadioButtonDefaults.colors(
    selectedColor = colorsScheme().primary,
    unselectedColor = colorsScheme().uncheckedColor,
    disabledSelectedColor = colorsScheme().disabledCheckedColor,
    disabledUnselectedColor = colorsScheme().disabledUncheckedColor
)
