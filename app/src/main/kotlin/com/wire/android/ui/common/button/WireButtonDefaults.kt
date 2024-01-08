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

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun wirePrimaryButtonColors() = wireButtonColors(
    enabled = MaterialTheme.wireColorScheme.primaryButtonEnabled,
    onEnabled = MaterialTheme.wireColorScheme.onPrimaryButtonEnabled,
    enabledOutline = MaterialTheme.wireColorScheme.primaryButtonEnabled,
    disabled = MaterialTheme.wireColorScheme.primaryButtonDisabled,
    onDisabled = MaterialTheme.wireColorScheme.onPrimaryButtonDisabled,
    disabledOutline = MaterialTheme.wireColorScheme.primaryButtonDisabled,
    selected = MaterialTheme.wireColorScheme.primaryButtonSelected,
    onSelected = MaterialTheme.wireColorScheme.onPrimaryButtonSelected,
    selectedOutline = MaterialTheme.wireColorScheme.primaryButtonSelected,
    error = MaterialTheme.wireColorScheme.error,
    onError = MaterialTheme.wireColorScheme.onError,
    errorOutline = MaterialTheme.wireColorScheme.error,
    positive = MaterialTheme.wireColorScheme.positive,
    onPositive = MaterialTheme.wireColorScheme.onPositive,
    positiveOutline = MaterialTheme.wireColorScheme.positive,
    ripple = MaterialTheme.wireColorScheme.primaryButtonRipple
)

@Composable
fun wireSendPrimaryButtonColors() = wirePrimaryButtonColors().copy(
    disabled = MaterialTheme.wireColorScheme.onSecondaryButtonDisabled,
    onDisabled = MaterialTheme.wireColorScheme.onPrimary
)

@Composable
fun wireSecondaryButtonColors(
    enabled: Color = MaterialTheme.wireColorScheme.secondaryButtonEnabled,
    onEnabled: Color = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled,
    enabledOutline: Color = MaterialTheme.wireColorScheme.secondaryButtonEnabledOutline,
    disabled: Color = MaterialTheme.wireColorScheme.secondaryButtonDisabled,
    onDisabled: Color = MaterialTheme.wireColorScheme.onSecondaryButtonDisabled,
    disabledOutline: Color = MaterialTheme.wireColorScheme.secondaryButtonDisabledOutline,
    selected: Color = MaterialTheme.wireColorScheme.secondaryButtonSelected,
    onSelected: Color = MaterialTheme.wireColorScheme.onSecondaryButtonSelected,
    selectedOutline: Color = MaterialTheme.wireColorScheme.secondaryButtonSelectedOutline,
    error: Color = MaterialTheme.wireColorScheme.secondaryButtonEnabled,
    onError: Color = MaterialTheme.wireColorScheme.error,
    errorOutline: Color = MaterialTheme.wireColorScheme.secondaryButtonEnabledOutline,
    positive: Color = MaterialTheme.wireColorScheme.secondaryButtonEnabled,
    onPositive: Color = MaterialTheme.wireColorScheme.positive,
    positiveOutline: Color = MaterialTheme.wireColorScheme.secondaryButtonEnabledOutline,
    ripple: Color = MaterialTheme.wireColorScheme.secondaryButtonRipple
) = wireButtonColors(
    enabled = enabled,
    onEnabled = onEnabled,
    enabledOutline = enabledOutline,
    disabled = disabled,
    onDisabled = onDisabled,
    disabledOutline = disabledOutline,
    selected = selected,
    onSelected = onSelected,
    selectedOutline = selectedOutline,
    error = error,
    onError = onError,
    errorOutline = errorOutline,
    positive = positive,
    onPositive = onPositive,
    positiveOutline = positiveOutline,
    ripple = ripple
)

@Composable
fun wireTertiaryButtonColors() = wireButtonColors(
    enabled = MaterialTheme.wireColorScheme.tertiaryButtonEnabled,
    onEnabled = MaterialTheme.wireColorScheme.onTertiaryButtonEnabled,
    enabledOutline = MaterialTheme.wireColorScheme.tertiaryButtonEnabled,
    disabled = MaterialTheme.wireColorScheme.tertiaryButtonDisabled,
    onDisabled = MaterialTheme.wireColorScheme.onTertiaryButtonDisabled,
    disabledOutline = MaterialTheme.wireColorScheme.tertiaryButtonDisabled,
    selected = MaterialTheme.wireColorScheme.tertiaryButtonSelected,
    onSelected = MaterialTheme.wireColorScheme.onTertiaryButtonSelected,
    selectedOutline = MaterialTheme.wireColorScheme.tertiaryButtonSelectedOutline,
    error = MaterialTheme.wireColorScheme.tertiaryButtonEnabled,
    onError = MaterialTheme.wireColorScheme.error,
    errorOutline = MaterialTheme.wireColorScheme.tertiaryButtonEnabled,
    positive = MaterialTheme.wireColorScheme.tertiaryButtonEnabled,
    onPositive = MaterialTheme.wireColorScheme.positive,
    positiveOutline = MaterialTheme.wireColorScheme.tertiaryButtonEnabled,
    ripple = MaterialTheme.wireColorScheme.tertiaryButtonRipple
)

@Composable
private fun wireButtonColors(
    enabled: Color, onEnabled: Color, enabledOutline: Color,
    disabled: Color, onDisabled: Color, disabledOutline: Color,
    selected: Color, onSelected: Color, selectedOutline: Color,
    error: Color, onError: Color, errorOutline: Color,
    positive: Color, onPositive: Color, positiveOutline: Color,
    ripple: Color
) = WireButtonColors(
    enabled, onEnabled, enabledOutline,
    disabled, onDisabled, disabledOutline,
    selected, onSelected, selectedOutline,
    error, onError, errorOutline,
    positive, onPositive, positiveOutline,
    ripple
)

@Stable
data class WireButtonColors(
    val enabled: Color,
    val onEnabled: Color,
    val enabledOutline: Color,
    val disabled: Color,
    val onDisabled: Color,
    val disabledOutline: Color,
    val selected: Color,
    val onSelected: Color,
    val selectedOutline: Color,
    val error: Color,
    val onError: Color,
    val errorOutline: Color,
    val positive: Color,
    val onPositive: Color,
    val positiveOutline: Color,
    val ripple: Color
) {

    @Composable
    fun containerColor(state: WireButtonState, interactionSource: InteractionSource): State<Color> = animateColorAsState(
        when (state) {
            WireButtonState.Default -> enabled
            WireButtonState.Disabled -> disabled
            WireButtonState.Selected -> selected
            WireButtonState.Error -> error
            WireButtonState.Positive -> positive
        }
    )

    @Composable
    fun outlineColor(state: WireButtonState, interactionSource: InteractionSource): State<Color> = animateColorAsState(
        when (state) {
            WireButtonState.Default -> enabledOutline
            WireButtonState.Disabled -> disabledOutline
            WireButtonState.Selected -> selectedOutline
            WireButtonState.Error -> errorOutline
            WireButtonState.Positive -> positiveOutline
        }
    )

    @Composable
    fun contentColor(state: WireButtonState, interactionSource: InteractionSource): State<Color> = animateColorAsState(
        when (state) {
            WireButtonState.Default -> onEnabled
            WireButtonState.Disabled -> onDisabled
            WireButtonState.Selected -> onSelected
            WireButtonState.Error -> onError
            WireButtonState.Positive -> onPositive
        }
    )

    @Composable
    fun rippleColor(): Color = ripple
}

sealed class WireButtonState {
    object Default : WireButtonState()
    object Disabled : WireButtonState()
    object Selected : WireButtonState()
    object Error : WireButtonState()
    object Positive : WireButtonState()
}
