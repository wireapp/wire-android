package com.wire.android.ui.common.button

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.MaterialTheme
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
            val duration = if (state == ToggleableState.Off) BoxOutDuration else BoxInDuration
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
            val duration = if (state == ToggleableState.Off) BoxOutDuration else BoxInDuration
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

        val duration = if (state == ToggleableState.Off) BoxOutDuration else BoxInDuration
        return animateColorAsState(target, tween(durationMillis = duration))
    }

}

private const val BoxOutDuration = 100
private const val BoxInDuration = 50
