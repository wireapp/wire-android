package com.wire.android.ui.common.textfield

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.lang.Integer.min

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CodeTextField(
    codeLength: Int = integerResource(id = R.integer.code_length),
    value: TextFieldValue,
    onValueChange: (CodeFieldValue) -> Unit,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.corner4x),
    colors: WireTextFieldColors = wireTextFieldColors(),
    textStyle: TextStyle = MaterialTheme.wireTypography.code01,
    state: WireTextFieldState = WireTextFieldState.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    horizontalSpacing: Dp = MaterialTheme.wireDimensions.spacing8x,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val enabled = state !is WireTextFieldState.Disabled
    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = modifier,
    ) {
        BasicTextField(
            value = value,
            onValueChange = {
                val textDigits = it.text.filter { it.isDigit() } // don't allow characters other than digits to be entered
                    .let { it.substring(0, min(codeLength, it.length)) } // don't allow more digits than required
                onValueChange(
                    CodeFieldValue(
                        text = TextFieldValue(text = textDigits, selection = TextRange(textDigits.length)),
                        isFullyFilled = textDigits.length == codeLength
                    )
                )
            },
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, autoCorrect = false, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            interactionSource = interactionSource,
            decorationBox = {
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(codeLength) { index ->
                        if (index != 0) Spacer(modifier = Modifier.width(horizontalSpacing))
                        Digit(
                            char = value.text.getOrNull(index),
                            shape = shape,
                            colors = colors,
                            textStyle = textStyle,
                            selected = index == value.text.length,
                            state = state
                        )
                    }
                }
            })
        val bottomText = when {
            state is WireTextFieldState.Error && state.errorText != null -> state.errorText
            else -> String.EMPTY
        }
        AnimatedVisibility(visible = bottomText.isNotEmpty()) {
            Text(
                text = bottomText,
                style = MaterialTheme.wireTypography.label04,
                color = colors.descriptionColor(state).value,
                modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing4x)
            )
        }
    }
}

@Composable
private fun Digit(
    char: Char? = null,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.textFieldCornerSize),
    colors: WireTextFieldColors = wireTextFieldColors(),
    textStyle: TextStyle = MaterialTheme.wireTypography.body01,
    state: WireTextFieldState = WireTextFieldState.Default,
    selected: Boolean = false
) {
    val interactionSource = object : InteractionSource {
        private val focusInteraction: FocusInteraction.Focus = FocusInteraction.Focus()
        override val interactions: Flow<Interaction> = flow {
            emit(if(selected) focusInteraction else FocusInteraction.Unfocus(focusInteraction))
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(color = colors.backgroundColor(state).value, shape = shape)
            .border(width = 1.dp, color = colors.borderColor(state, interactionSource).value, shape = shape)
            .size(width = MaterialTheme.wireDimensions.codeFieldItemWidth, height = MaterialTheme.wireDimensions.codeFieldItemHeight)
    ) {
        Text(
            text = char?.toString() ?: "",
            color = colors.textColor(state = state).value,
            style = textStyle,
            textAlign = TextAlign.Center,
        )
    }
}

data class CodeFieldValue(val text: TextFieldValue, val isFullyFilled: Boolean)
