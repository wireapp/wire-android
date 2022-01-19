package com.wire.android.ui.common.textfield

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color

@Composable
fun wireTextFieldColors(
    focusColor: Color = MaterialTheme.colors.primary,
    errorColor: Color = MaterialTheme.colors.error,
    successColor: Color = Color.Green, //TODO
    borderColor: Color = MaterialTheme.colors.onSurface.copy(ContentAlpha.disabled),
    placeholderColor: Color = MaterialTheme.colors.onSurface.copy(ContentAlpha.medium),
    textColor: Color = LocalContentColor.current.copy(LocalContentAlpha.current),
    disabledTextColor: Color = textColor.copy(ContentAlpha.disabled),
    backgroundColor: Color = Color.White,
    disabledBackgroundColor: Color = Color.LightGray,
    labelColor: Color = MaterialTheme.colors.onSurface.copy(ContentAlpha.medium),
    descriptionColor: Color = MaterialTheme.colors.onSurface.copy(ContentAlpha.medium),
): WireTextFieldColors {
    return object : WireTextFieldColors {

        @Composable
        override fun textColor(state: WireTextFieldState): State<Color> =
            rememberUpdatedState(if (state is WireTextFieldState.Disabled) disabledTextColor else textColor)

        @Composable
        override fun backgroundColor(state: WireTextFieldState): State<Color> =
            rememberUpdatedState(if (state is WireTextFieldState.Disabled) disabledBackgroundColor else backgroundColor)

        @Composable
        override fun placeholderColor(state: WireTextFieldState): State<Color> = rememberUpdatedState(placeholderColor)

        @Composable
        override fun labelMandatoryColor(state: WireTextFieldState): State<Color> = rememberUpdatedState(errorColor)

        @Composable
        override fun descriptionColor(state: WireTextFieldState): State<Color> = rememberUpdatedState(
            when (state) {
                is WireTextFieldState.Error -> errorColor
                else -> descriptionColor
            }
        )

        @Composable
        override fun iconColor(state: WireTextFieldState): State<Color> = rememberUpdatedState(
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
            return rememberUpdatedState(
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
            val targetValue = when {
                state is WireTextFieldState.Error -> errorColor
                state is WireTextFieldState.Success -> successColor
                focused -> focusColor
                else -> borderColor
            }
            return if (state !is WireTextFieldState.Disabled) animateColorAsState(targetValue, tween(ANIMATION_DURATION))
            else rememberUpdatedState(targetValue)
        }

        @Composable
        override fun cursorColor(state: WireTextFieldState): State<Color> =
            rememberUpdatedState(if (state is WireTextFieldState.Error) errorColor else focusColor)
    }
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

private const val ANIMATION_DURATION = 150
