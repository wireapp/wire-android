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
    ripple = MaterialTheme.wireColorScheme.primaryButtonRipple
)

@Composable
fun wireSecondaryButtonColors() = wireButtonColors(
    enabled = MaterialTheme.wireColorScheme.secondaryButtonEnabled,
    onEnabled = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled,
    enabledOutline = MaterialTheme.wireColorScheme.secondaryButtonEnabledOutline,
    disabled = MaterialTheme.wireColorScheme.secondaryButtonDisabled,
    onDisabled = MaterialTheme.wireColorScheme.onSecondaryButtonDisabled,
    disabledOutline = MaterialTheme.wireColorScheme.secondaryButtonDisabledOutline,
    selected = MaterialTheme.wireColorScheme.secondaryButtonSelected,
    onSelected = MaterialTheme.wireColorScheme.onSecondaryButtonSelected,
    selectedOutline = MaterialTheme.wireColorScheme.secondaryButtonSelectedOutline,
    error = MaterialTheme.wireColorScheme.secondaryButtonEnabled,
    onError = MaterialTheme.wireColorScheme.error,
    errorOutline = MaterialTheme.wireColorScheme.error,
    ripple = MaterialTheme.wireColorScheme.secondaryButtonRipple
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
    errorOutline = MaterialTheme.wireColorScheme.error,
    ripple = MaterialTheme.wireColorScheme.tertiaryButtonRipple
)

@Composable
private fun wireButtonColors(
    enabled: Color,     onEnabled: Color,   enabledOutline: Color,
    disabled: Color,    onDisabled: Color,  disabledOutline: Color,
    selected: Color,    onSelected: Color,  selectedOutline: Color,
    error: Color,       onError: Color,     errorOutline: Color,
    ripple: Color
) = object: WireButtonColors {
    @Composable
    override fun containerColor(state: WireButtonState, interactionSource: InteractionSource): State<Color> = animateColorAsState(
            when(state) {
                WireButtonState.Default -> enabled
                WireButtonState.Disabled -> disabled
                WireButtonState.Selected -> selected
                WireButtonState.Error -> error
            }
        )

    @Composable
    override fun outlineColor(state: WireButtonState, interactionSource: InteractionSource): State<Color> = animateColorAsState(
        when(state) {
            WireButtonState.Default -> enabledOutline
            WireButtonState.Disabled -> disabledOutline
            WireButtonState.Selected -> selectedOutline
            WireButtonState.Error -> errorOutline
        }
    )

    @Composable
    override fun contentColor(state: WireButtonState, interactionSource: InteractionSource): State<Color> = animateColorAsState(
            when(state) {
                WireButtonState.Default -> onEnabled
                WireButtonState.Disabled -> onDisabled
                WireButtonState.Selected -> onSelected
                WireButtonState.Error -> onError
            }
        )

    @Composable
    override fun rippleColor(): Color = ripple
}

@Stable
interface WireButtonColors {

    @Composable
    fun containerColor(state: WireButtonState, interactionSource: InteractionSource): State<Color>

    @Composable
    fun contentColor(state: WireButtonState, interactionSource: InteractionSource): State<Color>

    @Composable
    fun outlineColor(state: WireButtonState, interactionSource: InteractionSource): State<Color>

    @Composable
    fun rippleColor(): Color
}

sealed class WireButtonState {
    object Default : WireButtonState()
    object Disabled : WireButtonState()
    object Selected : WireButtonState()
    object Error : WireButtonState()
}
