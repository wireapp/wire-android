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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY
import com.wire.android.util.ui.PreviewMultipleThemes

/**
 * Hybrid text field that uses new BasicTextField2 which resolves multiple issues that old ones had. It's been renamed to BasicTextField
 * as well in the newest compose version. The difference is that this new text field takes TextFieldState, all other BasicTextFields
 * which take TextFieldValue or String with onValueChange callback are the previous generation ones.
 * This hybrid is created to allow us to still pass TextFieldValue and onValueChange callback but already use the new text input version.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun WireTextField2(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholderText: String? = null,
    labelText: String? = null,
    labelMandatoryIcon: Boolean = false,
    descriptionText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    readOnly: Boolean = false,
    state: WireTextFieldState = WireTextFieldState.Default,
    maxLines: Int = 1,
    singleLine: Boolean = true,
    maxTextLength: Int = 8000,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences,
        autoCorrect = true
    ),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    scrollState: ScrollState = rememberScrollState(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
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
    val textState = rememberTextFieldState(value.text, value.selection)
    val lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(1, maxLines)

    Column(modifier = modifier) {
        if (labelText != null) {
            Label(labelText, labelMandatoryIcon, state, interactionSource, colors)
        }

        BasicTextField(
            state = textState,
            textStyle = textStyle.copy(color = colors.textColor(state = state).value, textDirection = TextDirection.ContentOrLtr),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            lineLimits = lineLimits,
            inputTransformation = InputTransformation.maxLength(maxTextLength),
            scrollState = scrollState,
            readOnly = readOnly,
            enabled = state !is WireTextFieldState.Disabled,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = colors.backgroundColor(state).value, shape = shape)
                .border(width = 1.dp, color = colors.borderColor(state, interactionSource).value, shape = shape)
                .semantics {
                    (labelText ?: placeholderText ?: descriptionText)?.let {
                        contentDescription = it
                    }
                }
                .testTag(testTag)
                .then(
                    StateSyncingModifier(
                        state = textState,
                        value = value,
                        onValueChanged = onValueChange
                    )
                ),
            decorator = { innerTextField ->
                InnerText(
                    innerTextField = innerTextField,
                    shouldShowPlaceholder = textState.text.isEmpty(),
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    placeholderText = placeholderText,
                    state = state,
                    placeholderTextStyle = placeholderTextStyle,
                    placeholderAlignment = placeholderAlignment,
                    inputMinHeight = inputMinHeight,
                    colors = colors,
                    shouldDetectTaps = shouldDetectTaps,
                    onTap = onTap,
                )
            },
            onTextLayout = {
                it()?.let {
                    val lineOfText = it.getLineForOffset(textState.text.selection.end)
                    val bottomYCoordinate = it.getLineBottom(lineOfText)
                    onSelectedLineIndexChanged(lineOfText)
                    onLineBottomYCoordinateChanged(bottomYCoordinate)
                }
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

@PreviewMultipleThemes
@Composable
fun PreviewWireTextField2() = WireTheme {
    WireTextField2(
        value = TextFieldValue("text"),
        onValueChange = {},
        modifier = Modifier.padding(16.dp)
    )
}
