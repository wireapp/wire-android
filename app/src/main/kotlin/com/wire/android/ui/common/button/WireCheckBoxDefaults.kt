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
 *
 *
 */

package com.wire.android.ui.common.button

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun wireCheckBoxColors() = wireCheckBoxColors(
    checkedCheckmarkColor = MaterialTheme.wireColorScheme.checkedCheckmarkColor,
    uncheckedCheckmarkColor = MaterialTheme.wireColorScheme.uncheckedCheckmarkColor,
    checkedBoxColor = MaterialTheme.wireColorScheme.checkedBoxColor,
    uncheckedBoxColor = MaterialTheme.wireColorScheme.uncheckedBoxColor,
    disabledCheckedBoxColor = MaterialTheme.wireColorScheme.disabledCheckedBoxColor,
    disabledUncheckedBoxColor = MaterialTheme.wireColorScheme.disabledUncheckedBoxColor,
    disabledIndeterminateBoxColor = MaterialTheme.wireColorScheme.disabledIndeterminateBoxColor,
    checkedBorderColor = MaterialTheme.wireColorScheme.checkedCheckBoxBorderColor,
    uncheckedBorderColor = MaterialTheme.wireColorScheme.uncheckedCheckBoxBorderColor,
    disabledBorderColor = MaterialTheme.wireColorScheme.disabledCheckedBoxColor,
    disabledIndeterminateBorderColor = MaterialTheme.wireColorScheme.disabledIndeterminateCheckBoxBorderColor,
)

@Composable
fun wireRadioButtonColors() = object : RadioButtonColors {

    @Composable
    override fun radioColor(enabled: Boolean, selected: Boolean): State<Color> {
        val target = if (enabled) {
            if (selected) MaterialTheme.wireColorScheme.checkedBoxColor
            else MaterialTheme.wireColorScheme.uncheckedBoxColor
        } else {
            MaterialTheme.wireColorScheme.disabledUncheckedBoxColor
        }

        // If not enabled 'snap' to the disabled state, as there should be no animations between
        // enabled / disabled.
        return if (enabled) {
            val duration = BOX_OUT_DURATION
            animateColorAsState(target, tween(durationMillis = duration))
        } else {
            rememberUpdatedState(target)
        }
    }
}

@Composable
fun wireCheckBoxColors(
    checkedCheckmarkColor: Color,
    uncheckedCheckmarkColor: Color,
    checkedBoxColor: Color,
    uncheckedBoxColor: Color,
    disabledCheckedBoxColor: Color,
    disabledUncheckedBoxColor: Color,
    disabledIndeterminateBoxColor: Color,
    checkedBorderColor: Color,
    uncheckedBorderColor: Color,
    disabledBorderColor: Color,
    disabledIndeterminateBorderColor: Color
) = object : CheckboxColors {

    @Composable
    override fun borderColor(enabled: Boolean, state: ToggleableState): State<Color> {
        val target = if (enabled) {
            when (state) {
                ToggleableState.On, ToggleableState.Indeterminate -> checkedBorderColor
                ToggleableState.Off -> uncheckedBorderColor
            }
        } else {
            when (state) {
                ToggleableState.Indeterminate -> disabledIndeterminateBorderColor
                ToggleableState.On, ToggleableState.Off -> disabledBorderColor
            }
        }

        // If not enabled 'snap' to the disabled state, as there should be no animations between
        // enabled / disabled.
        return if (enabled) {
            val duration = if (state == ToggleableState.Off) BOX_OUT_DURATION else BOX_IN_DURATION
            animateColorAsState(target, tween(durationMillis = duration))
        } else {
            rememberUpdatedState(target)
        }
    }

    @Composable
    override fun boxColor(enabled: Boolean, state: ToggleableState): State<Color> {
        val target = if (enabled) {
            when (state) {
                ToggleableState.On, ToggleableState.Indeterminate -> checkedBoxColor
                ToggleableState.Off -> uncheckedBoxColor
            }
        } else {
            when (state) {
                ToggleableState.On -> disabledCheckedBoxColor
                ToggleableState.Indeterminate -> disabledIndeterminateBoxColor
                ToggleableState.Off -> disabledUncheckedBoxColor
            }
        }

        // If not enabled 'snap' to the disabled state, as there should be no animations between
        // enabled / disabled.
        return if (enabled) {
            val duration = if (state == ToggleableState.Off) BOX_OUT_DURATION else BOX_IN_DURATION
            animateColorAsState(target, tween(durationMillis = duration))
        } else {
            rememberUpdatedState(target)
        }
    }

    @Composable
    override fun checkmarkColor(state: ToggleableState): State<Color> {
        val target = if (state == ToggleableState.Off) {
            uncheckedCheckmarkColor
        } else {
            checkedCheckmarkColor
        }

        val duration = if (state == ToggleableState.Off) BOX_OUT_DURATION else BOX_IN_DURATION
        return animateColorAsState(target, tween(durationMillis = duration))
    }
}

private const val BOX_OUT_DURATION = 100
private const val BOX_IN_DURATION = 50
