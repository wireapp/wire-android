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
    enabledRipple = MaterialTheme.wireColorScheme.primaryButtonRipple,
    disabled = MaterialTheme.wireColorScheme.primaryButtonDisabled,
    onDisabled = MaterialTheme.wireColorScheme.onPrimaryButtonDisabled,
    disabledOutline = MaterialTheme.wireColorScheme.primaryButtonDisabled,
    disabledRipple = MaterialTheme.wireColorScheme.primaryButtonRipple,
    selected = MaterialTheme.wireColorScheme.primaryButtonSelected,
    onSelected = MaterialTheme.wireColorScheme.onPrimaryButtonSelected,
    selectedOutline = MaterialTheme.wireColorScheme.primaryButtonSelected,
    selectedRipple = MaterialTheme.wireColorScheme.primaryButtonRipple,
    error = MaterialTheme.wireColorScheme.error,
    onError = MaterialTheme.wireColorScheme.onError,
    errorOutline = MaterialTheme.wireColorScheme.error,
    errorRipple = MaterialTheme.wireColorScheme.primaryButtonRipple,
    positive = MaterialTheme.wireColorScheme.positive,
    onPositive = MaterialTheme.wireColorScheme.onPositive,
    positiveOutline = MaterialTheme.wireColorScheme.positive,
    positiveRipple = MaterialTheme.wireColorScheme.primaryButtonRipple,
)

@Composable
fun wireSendPrimaryButtonColors() = wirePrimaryButtonColors().copy(
    disabled = MaterialTheme.wireColorScheme.onSecondaryButtonDisabled,
    onDisabled = MaterialTheme.wireColorScheme.onPrimary
)

@Composable
fun wireSecondaryButtonColors() = wireButtonColors(
    enabled = MaterialTheme.wireColorScheme.secondaryButtonEnabled,
    onEnabled = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled,
    enabledOutline = MaterialTheme.wireColorScheme.secondaryButtonEnabledOutline,
    enabledRipple = MaterialTheme.wireColorScheme.secondaryButtonRipple,
    disabled = MaterialTheme.wireColorScheme.secondaryButtonDisabled,
    onDisabled = MaterialTheme.wireColorScheme.onSecondaryButtonDisabled,
    disabledOutline = MaterialTheme.wireColorScheme.secondaryButtonDisabledOutline,
    disabledRipple = MaterialTheme.wireColorScheme.secondaryButtonRipple,
    selected = MaterialTheme.wireColorScheme.secondaryButtonSelected,
    onSelected = MaterialTheme.wireColorScheme.onSecondaryButtonSelected,
    selectedOutline = MaterialTheme.wireColorScheme.secondaryButtonSelectedOutline,
    selectedRipple = MaterialTheme.wireColorScheme.secondaryButtonRipple,
    error = MaterialTheme.wireColorScheme.secondaryButtonEnabled,
    onError = MaterialTheme.wireColorScheme.error,
    errorOutline = MaterialTheme.wireColorScheme.secondaryButtonEnabledOutline,
    errorRipple = MaterialTheme.wireColorScheme.secondaryButtonRipple,
    positive = MaterialTheme.wireColorScheme.secondaryButtonEnabled,
    onPositive = MaterialTheme.wireColorScheme.positive,
    positiveOutline = MaterialTheme.wireColorScheme.secondaryButtonEnabledOutline,
    positiveRipple = MaterialTheme.wireColorScheme.secondaryButtonRipple,
)

@Composable
fun wireTertiaryButtonColors() = wireButtonColors(
    enabled = MaterialTheme.wireColorScheme.tertiaryButtonEnabled,
    onEnabled = MaterialTheme.wireColorScheme.onTertiaryButtonEnabled,
    enabledOutline = MaterialTheme.wireColorScheme.tertiaryButtonEnabled,
    enabledRipple = MaterialTheme.wireColorScheme.tertiaryButtonRipple,
    disabled = MaterialTheme.wireColorScheme.tertiaryButtonDisabled,
    onDisabled = MaterialTheme.wireColorScheme.onTertiaryButtonDisabled,
    disabledOutline = MaterialTheme.wireColorScheme.tertiaryButtonDisabled,
    disabledRipple = MaterialTheme.wireColorScheme.tertiaryButtonRipple,
    selected = MaterialTheme.wireColorScheme.tertiaryButtonSelected,
    onSelected = MaterialTheme.wireColorScheme.onTertiaryButtonSelected,
    selectedOutline = MaterialTheme.wireColorScheme.tertiaryButtonSelectedOutline,
    selectedRipple = MaterialTheme.wireColorScheme.tertiaryButtonRipple,
    error = MaterialTheme.wireColorScheme.tertiaryButtonEnabled,
    onError = MaterialTheme.wireColorScheme.error,
    errorOutline = MaterialTheme.wireColorScheme.tertiaryButtonEnabled,
    errorRipple = MaterialTheme.wireColorScheme.tertiaryButtonRipple,
    positive = MaterialTheme.wireColorScheme.tertiaryButtonEnabled,
    onPositive = MaterialTheme.wireColorScheme.positive,
    positiveOutline = MaterialTheme.wireColorScheme.tertiaryButtonEnabled,
    positiveRipple = MaterialTheme.wireColorScheme.tertiaryButtonRipple,
)

@Suppress("ParameterListWrapping")
@Composable
private fun wireButtonColors(
    enabled: Color, onEnabled: Color, enabledOutline: Color, enabledRipple: Color,
    disabled: Color, onDisabled: Color, disabledOutline: Color, disabledRipple: Color,
    selected: Color, onSelected: Color, selectedOutline: Color, selectedRipple: Color,
    error: Color, onError: Color, errorOutline: Color, errorRipple: Color,
    positive: Color, onPositive: Color, positiveOutline: Color, positiveRipple: Color,
) = WireButtonColors(
    enabled, onEnabled, enabledOutline, enabledRipple,
    disabled, onDisabled, disabledOutline, disabledRipple,
    selected, onSelected, selectedOutline, selectedRipple,
    error, onError, errorOutline, errorRipple,
    positive, onPositive, positiveOutline, positiveRipple,
)

@Stable
data class WireButtonColors(
    val enabled: Color,
    val onEnabled: Color,
    val enabledOutline: Color,
    val enabledRipple: Color,
    val disabled: Color,
    val onDisabled: Color,
    val disabledOutline: Color,
    val disabledRipple: Color,
    val selected: Color,
    val onSelected: Color,
    val selectedOutline: Color,
    val selectedRipple: Color,
    val error: Color,
    val onError: Color,
    val errorOutline: Color,
    val errorRipple: Color,
    val positive: Color,
    val onPositive: Color,
    val positiveOutline: Color,
    val positiveRipple: Color,
) {

    @Composable
    fun containerColor(state: WireButtonState): State<Color> = animateColorAsState(
        when (state) {
            WireButtonState.Default -> enabled
            WireButtonState.Disabled -> disabled
            WireButtonState.Selected -> selected
            WireButtonState.Error -> error
            WireButtonState.Positive -> positive
        }
    )

    @Composable
    fun outlineColor(state: WireButtonState): State<Color> = animateColorAsState(
        when (state) {
            WireButtonState.Default -> enabledOutline
            WireButtonState.Disabled -> disabledOutline
            WireButtonState.Selected -> selectedOutline
            WireButtonState.Error -> errorOutline
            WireButtonState.Positive -> positiveOutline
        }
    )

    @Composable
    fun contentColor(state: WireButtonState): State<Color> = animateColorAsState(
        when (state) {
            WireButtonState.Default -> onEnabled
            WireButtonState.Disabled -> onDisabled
            WireButtonState.Selected -> onSelected
            WireButtonState.Error -> onError
            WireButtonState.Positive -> onPositive
        }
    )

    @Composable
    fun rippleColor(state: WireButtonState): State<Color> = animateColorAsState(
        when (state) {
            WireButtonState.Default -> enabledRipple
            WireButtonState.Disabled -> disabledRipple
            WireButtonState.Selected -> selectedRipple
            WireButtonState.Error -> errorRipple
            WireButtonState.Positive -> positiveRipple
        }
    )
}

sealed class WireButtonState {
    data object Default : WireButtonState()
    data object Disabled : WireButtonState()
    data object Selected : WireButtonState()
    data object Error : WireButtonState()
    data object Positive : WireButtonState()
}
