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

package com.wire.android.ui.common.textfield

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun wireTextFieldColors(
    focusColor: Color = MaterialTheme.colorScheme.primary,
    errorColor: Color = MaterialTheme.colorScheme.error,
    successColor: Color = MaterialTheme.wireColorScheme.positive,
    borderColor: Color = MaterialTheme.wireColorScheme.secondaryButtonEnabledOutline,
    placeholderColor: Color = MaterialTheme.wireColorScheme.secondaryText,
    textColor: Color = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled,
    disabledTextColor: Color = MaterialTheme.wireColorScheme.onSecondaryButtonDisabled,
    backgroundColor: Color = MaterialTheme.wireColorScheme.secondaryButtonEnabled,
    disabledBackgroundColor: Color = MaterialTheme.wireColorScheme.secondaryButtonDisabled,
    labelColor: Color = MaterialTheme.wireColorScheme.secondaryText,
    descriptionColor: Color = MaterialTheme.wireColorScheme.secondaryText,
): WireTextFieldColors = object : WireTextFieldColors {

    @Composable
    override fun textColor(state: WireTextFieldState): State<Color> =
        animateColorAsState(if (state is WireTextFieldState.Disabled) disabledTextColor else textColor)

    @Composable
    override fun backgroundColor(state: WireTextFieldState): State<Color> =
        animateColorAsState(if (state is WireTextFieldState.Disabled) disabledBackgroundColor else backgroundColor)

    @Composable
    override fun placeholderColor(state: WireTextFieldState): State<Color> = animateColorAsState(placeholderColor)

    @Composable
    override fun labelMandatoryColor(state: WireTextFieldState): State<Color> = animateColorAsState(errorColor)

    @Composable
    override fun descriptionColor(state: WireTextFieldState): State<Color> = animateColorAsState(
        when (state) {
            is WireTextFieldState.Error -> errorColor
            else -> descriptionColor
        }
    )

    @Composable
    override fun iconColor(state: WireTextFieldState): State<Color> = animateColorAsState(
        when (state) {
            WireTextFieldState.Disabled -> disabledTextColor
            is WireTextFieldState.Error -> errorColor
            WireTextFieldState.Success -> successColor
            else -> textColor
        }
    )

    @Composable
    override fun labelColor(state: WireTextFieldState, interactionSource: InteractionSource): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()
        return animateColorAsState(
            when {
                state is WireTextFieldState.Error -> errorColor
                state is WireTextFieldState.Success -> successColor
                focused -> focusColor
                else -> labelColor
            }
        )
    }

    @Composable
    override fun borderColor(state: WireTextFieldState, interactionSource: InteractionSource): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()
        return animateColorAsState(
            when {
                state is WireTextFieldState.Error -> errorColor
                state is WireTextFieldState.Success -> successColor
                focused -> focusColor
                else -> borderColor
            }
        )
    }

    @Composable
    override fun cursorColor(state: WireTextFieldState): State<Color> =
        animateColorAsState(if (state is WireTextFieldState.Error) errorColor else focusColor)
}

@Stable
interface WireTextFieldColors {
    @Composable
    fun textColor(state: WireTextFieldState): State<Color>

    @Composable
    fun backgroundColor(state: WireTextFieldState): State<Color>

    @Composable
    fun placeholderColor(state: WireTextFieldState): State<Color>

    @Composable
    fun labelColor(state: WireTextFieldState, interactionSource: InteractionSource): State<Color>

    @Composable
    fun labelMandatoryColor(state: WireTextFieldState): State<Color>

    @Composable
    fun descriptionColor(state: WireTextFieldState): State<Color>

    @Composable
    fun iconColor(state: WireTextFieldState): State<Color>

    @Composable
    fun borderColor(state: WireTextFieldState, interactionSource: InteractionSource): State<Color>

    @Composable
    fun cursorColor(state: WireTextFieldState): State<Color>
}
