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

package com.wire.android.ui.common.textfield

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.Tint
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY

@Composable
internal fun WireTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    maxTextLength: Int = 8000,
    keyboardOptions: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, autoCorrect = true),
    keyboardActions: KeyboardActions = KeyboardActions(),
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    placeholderText: String? = null,
    labelText: String? = null,
    labelMandatoryIcon: Boolean = false,
    descriptionText: String? = null,
    state: WireTextFieldState = WireTextFieldState.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = MaterialTheme.wireTypography.body01,
    placeholderTextStyle: TextStyle = MaterialTheme.wireTypography.body01,
    inputMinHeight: Dp = MaterialTheme.wireDimensions.textFieldMinHeight,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.textFieldCornerSize),
    colors: WireTextFieldColors = wireTextFieldColors(),
    modifier: Modifier = Modifier,
    onSelectedLineIndexChanged: (Int) -> Unit = { },
    onLineBottomYCoordinateChanged: (Float) -> Unit = { },
    shouldDetectTaps: Boolean = false,
    onTap: (Offset) -> Unit = { },
) {
    val enabled = state !is WireTextFieldState.Disabled
    var updatedText by remember { mutableStateOf(value) }

    Column(modifier = modifier) {
        if (labelText != null) {
            Label(labelText, labelMandatoryIcon, state, interactionSource, colors)
        }
        BasicTextField(
            value = value,
            onValueChange = {
                updatedText = if (singleLine || maxLines == 1) {
                    it.copy(it.text.replace("\n", ""))
                } else it

                if (updatedText.text.length > maxTextLength) {
                    updatedText = TextFieldValue(
                        text = updatedText.text.take(maxTextLength),
                        selection = TextRange(updatedText.text.length - 1)
                    )
                }

                onValueChange(updatedText)
            },
            textStyle = textStyle.copy(color = colors.textColor(state = state).value, textDirection = TextDirection.ContentOrLtr),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            readOnly = readOnly,
            enabled = enabled,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = colors.backgroundColor(state).value, shape = shape)
                .border(width = 1.dp, color = colors.borderColor(state, interactionSource).value, shape = shape)
                .semantics {
                    (labelText ?: placeholderText ?: descriptionText)?.let {
                        contentDescription = it
                    }
                },
            decorationBox = { innerTextField ->
                InnerText(
                    innerTextField,
                    value,
                    leadingIcon,
                    trailingIcon,
                    placeholderText,
                    state,
                    placeholderTextStyle,
                    inputMinHeight,
                    colors,
                    shouldDetectTaps,
                    onTap
                )
            },
            onTextLayout = {
                val lineOfText = it.getLineForOffset(value.selection.end)
                val bottomYCoordinate = it.getLineBottom(lineOfText)
                onSelectedLineIndexChanged(lineOfText)
                onLineBottomYCoordinateChanged(bottomYCoordinate)
            }
        )
        val bottomText = when {
            state is WireTextFieldState.Error && state.errorText != null -> state.errorText
            !descriptionText.isNullOrEmpty() -> descriptionText
            else -> String.EMPTY
        }
        AnimatedVisibility(visible = bottomText.isNotEmpty()) {
            Text(
                text = bottomText,
                style = MaterialTheme.wireTypography.label04,
                textAlign = TextAlign.Start,
                color = colors.descriptionColor(state).value,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun Label(
    labelText: String,
    labelMandatoryIcon: Boolean,
    state: WireTextFieldState,
    interactionSource: InteractionSource,
    colors: WireTextFieldColors
) {
    Row {
        Text(
            text = labelText,
            style = MaterialTheme.wireTypography.label01,
            color = colors.labelColor(state, interactionSource).value,
            modifier = Modifier.padding(bottom = 4.dp, end = 4.dp)
        )
        if (labelMandatoryIcon) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_input_mandatory),
                tint = colors.labelMandatoryColor(state).value,
                contentDescription = "",
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun InnerText(
    innerTextField: @Composable () -> Unit,
    value: TextFieldValue,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    placeholderText: String? = null,
    state: WireTextFieldState = WireTextFieldState.Default,
    placeholderTextStyle: TextStyle = MaterialTheme.wireTypography.body01,
    inputMinHeight: Dp = 48.dp,
    colors: WireTextFieldColors = wireTextFieldColors(),
    shouldDetectTaps: Boolean = false,
    onClick: (Offset) -> Unit = { }
) {
    var modifier: Modifier = Modifier
    if (shouldDetectTaps) {
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onTap = onClick)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = inputMinHeight)
    ) {

        val trailingOrStateIcon: @Composable (() -> Unit)? = when {
            trailingIcon != null -> trailingIcon
            else -> state.icon()?.Icon(Modifier.padding(horizontal = 16.dp))
        }
        if (leadingIcon != null) {
            Box(contentAlignment = Alignment.Center) {
                Tint(contentColor = colors.iconColor(state).value, content = leadingIcon)
            }
        }
        Box(Modifier.weight(1f)) {
            val padding = Modifier.padding(
                start = if (leadingIcon == null) 16.dp else 0.dp,
                end = if (trailingOrStateIcon == null) 16.dp else 0.dp,
                top = 2.dp, bottom = 2.dp
            )
            if (value.text.isEmpty() && placeholderText != null) {
                Text(
                    text = placeholderText,
                    style = placeholderTextStyle,
                    color = colors.placeholderColor(state).value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(padding)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(padding),
                propagateMinConstraints = true
            ) {
                innerTextField()
            }
        }
        if (trailingOrStateIcon != null) {
            Box(contentAlignment = Alignment.Center) {
                Tint(contentColor = colors.iconColor(state).value, content = trailingOrStateIcon)
            }
        }
    }
}

@Preview(name = "Default WireTextField")
@Composable
fun PreviewWireTextField() {
    WireTextField(
        value = TextFieldValue("text"),
        onValueChange = {},
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Default WireTextField with labels")
@Composable
fun PreviewWireTextFieldLabels() {
    WireTextField(
        value = TextFieldValue("text"),
        labelText = "label",
        labelMandatoryIcon = true,
        descriptionText = "description",
        onValueChange = {},
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Dense Search WireTextField")
@Composable
fun PreviewWireTextFieldDenseSearch() {
    WireTextField(
        value = TextFieldValue(""),
        placeholderText = "Search",
        leadingIcon = { IconButton(modifier = Modifier.height(40.dp), onClick = {}) { Icon(Icons.Filled.Search, "") } },
        trailingIcon = { IconButton(modifier = Modifier.height(40.dp), onClick = {}) { Icon(Icons.Filled.Close, "") } },
        onValueChange = {},
        inputMinHeight = 40.dp,
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Disabled WireTextField")
@Composable
fun PreviewWireTextFieldDisabled() {
    WireTextField(
        value = TextFieldValue("text"),
        onValueChange = {},
        state = WireTextFieldState.Disabled,
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Error WireTextField")
@Composable
fun PreviewWireTextFieldError() {
    WireTextField(
        value = TextFieldValue("text"),
        onValueChange = {},
        state = WireTextFieldState.Error("error"),
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Success WireTextField")
@Composable
fun PreviewWireTextFieldSuccess() {
    WireTextField(
        value = TextFieldValue("text"),
        onValueChange = {},
        state = WireTextFieldState.Success,
        modifier = Modifier.padding(16.dp)
    )
}

sealed class WireTextFieldState {
    object Default : WireTextFieldState()
    data class Error(val errorText: String? = null) : WireTextFieldState()
    object Success : WireTextFieldState()
    object Disabled : WireTextFieldState()

    fun icon(): ImageVector? = when (this) {
        is Error -> Icons.Filled.ErrorOutline
        is Success -> Icons.Filled.Check
        else -> null
    }
}
