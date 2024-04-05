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

package com.wire.android.ui.common.textfield

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldLineLimits
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.Tint
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun WireTextField2(
    state: TextFieldState,
    onValueChange: (String) -> Unit,
    readOnly: Boolean = false,
    lineLimits: TextFieldLineLimits,
    maxTextLength: Int = 8000,
    keyboardOptions: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, autoCorrect = true),
    keyboardActions: KeyboardActions = KeyboardActions(),
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    placeholderText: String? = null,
    labelText: String? = null,
    labelMandatoryIcon: Boolean = false,
    descriptionText: String? = null,
    fieldState: WireTextFieldState = WireTextFieldState.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = MaterialTheme.wireTypography.body01,
    placeholderTextStyle: TextStyle = MaterialTheme.wireTypography.body01,
    placeholderAlignment: Alignment.Horizontal = Alignment.Start,
    inputMinHeight: Dp = MaterialTheme.wireDimensions.textFieldMinHeight,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.textFieldCornerSize),
    colors: WireTextFieldColors = wireTextFieldColors(),
    modifier: Modifier = Modifier,
    onSelectedLineIndexChanged: (Int) -> Unit = { },
    onLineBottomYCoordinateChanged: (Float) -> Unit = { },
    shouldDetectTaps: Boolean = false,
    testTag: String = String.EMPTY,
    onTap: (Offset) -> Unit = { },
) {
    val enabled = fieldState !is WireTextFieldState.Disabled
//    var updatedText by remember { mutableStateOf(value) }


    Column(modifier = modifier) {
        if (labelText != null) {
            Label(labelText, labelMandatoryIcon, fieldState, interactionSource, colors)
        }
        BasicTextField2(
            state = state,
//            state = state,
//            onValueChange = {
//                updatedText = if (singleLine || maxLines == 1) {
//                    it.replace("\n", "")
//                } else it
//
//                if (updatedText.length > maxTextLength) {
//                    updatedText = updatedText.take(maxTextLength)
////                        selection = TextRange(updatedText.text.length - 1) // TODO KBX
//                }
//
//                onValueChange(updatedText)
//            },
            textStyle = textStyle.copy(color = colors.textColor(state = fieldState).value, textDirection = TextDirection.ContentOrLtr),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            lineLimits = lineLimits,
            readOnly = readOnly,
            enabled = enabled,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
//            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = colors.backgroundColor(fieldState).value, shape = shape)
                .border(width = 1.dp, color = colors.borderColor(fieldState, interactionSource).value, shape = shape)
                .semantics {
                    (labelText ?: placeholderText ?: descriptionText)?.let {
                        contentDescription = it
                    }
                }
                .testTag(testTag),
            decorator = { innerTextField ->
                InnerText(
                    innerTextField,
                    state,
                    leadingIcon,
                    trailingIcon,
                    placeholderText,
                    fieldState,
                    placeholderTextStyle,
                    placeholderAlignment,
                    inputMinHeight,
                    colors,
                    shouldDetectTaps,
                    onTap
                )
            },
//            onTextLayout = {
//                val lineOfText = it.getLineForOffset(value.selection.end)
//                val bottomYCoordinate = it.getLineBottom(lineOfText)
//                onSelectedLineIndexChanged(lineOfText)
//                onLineBottomYCoordinateChanged(bottomYCoordinate)
//            }
        )
        val bottomText = when {
            fieldState is WireTextFieldState.Error && fieldState.errorText != null -> fieldState.errorText
            !descriptionText.isNullOrEmpty() -> descriptionText
            else -> String.EMPTY
        }
        AnimatedVisibility(visible = bottomText.isNotEmpty()) {
            Text(
                text = bottomText,
                style = MaterialTheme.wireTypography.label04,
                textAlign = TextAlign.Start,
                color = colors.descriptionColor(fieldState).value,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InnerText(
    innerTextField: @Composable () -> Unit,
    state: TextFieldState,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    placeholderText: String? = null,
    textFieldState: WireTextFieldState = WireTextFieldState.Default,
    placeholderTextStyle: TextStyle = MaterialTheme.wireTypography.body01,
    placeholderAlignment: Alignment.Horizontal = Alignment.Start,
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
            else -> textFieldState.icon()?.Icon(Modifier.padding(horizontal = 16.dp))
        }
        if (leadingIcon != null) {
            Box(contentAlignment = Alignment.Center) {
                Tint(contentColor = colors.iconColor(textFieldState).value, content = leadingIcon)
            }
        }

        Box(
            Modifier
                .weight(1f)
                .padding(
                    start = if (leadingIcon == null) 16.dp else 0.dp,
                    end = if (trailingOrStateIcon == null) 16.dp else 0.dp,
                    top = 2.dp, bottom = 2.dp
                )
        ) {
            if (state.text.isEmpty() && placeholderText != null) {
                Text(
                    text = placeholderText,
                    style = placeholderTextStyle,
                    color = colors.placeholderColor(textFieldState).value,
                    modifier = Modifier
                        .align(placeholderAlignment.toAlignment())
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
                propagateMinConstraints = true
            ) {
                innerTextField()
            }
        }
        if (trailingOrStateIcon != null) {
            Box(contentAlignment = Alignment.Center) {
                Tint(contentColor = colors.iconColor(textFieldState).value, content = trailingOrStateIcon)
            }
        }
    }
}

private fun Alignment.Horizontal.toAlignment(): Alignment = Alignment { size, space, layoutDirection ->
    IntOffset(this@toAlignment.align(size.width, space.width, layoutDirection), 0)
}

@Preview(name = "Default WireTextField")
@Composable
fun PreviewWireTextField2() {
    WireTextField(
        value = TextFieldValue("text"),
        onValueChange = {},
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Default WireTextField with labels")
@Composable
fun PreviewWireTextFieldLabels2() {
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
fun PreviewWireTextFieldDenseSearch2() {
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
fun PreviewWireTextFieldDisabled2() {
    WireTextField(
        value = TextFieldValue("text"),
        onValueChange = {},
        state = WireTextFieldState.Disabled,
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Error WireTextField")
@Composable
fun PreviewWireTextFieldError2() {
    WireTextField(
        value = TextFieldValue("text"),
        onValueChange = {},
        state = WireTextFieldState.Error("error"),
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(name = "Success WireTextField")
@Composable
fun PreviewWireTextFieldSuccess2() {
    WireTextField(
        value = TextFieldValue("text"),
        onValueChange = {},
        state = WireTextFieldState.Success,
        modifier = Modifier.padding(16.dp)
    )
}
