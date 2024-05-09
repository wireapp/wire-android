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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY
import com.wire.android.util.ui.PreviewMultipleThemes

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WirePasswordTextField(
    textState: TextFieldState,
    placeholderText: String? = stringResource(R.string.login_password_placeholder),
    labelText: String? = stringResource(R.string.login_password_label),
    labelMandatoryIcon: Boolean = false,
    descriptionText: String? = null,
    state: WireTextFieldState = WireTextFieldState.Default,
    autoFill: Boolean = false,
    inputTransformation: InputTransformation = InputTransformation.maxLength(8000),
    textObfuscationMode: TextObfuscationMode = TextObfuscationMode.RevealLastTyped,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    textStyle: TextStyle = MaterialTheme.wireTypography.body01.copy(textAlign = TextAlign.Start),
    placeholderTextStyle: TextStyle = MaterialTheme.wireTypography.body01.copy(textAlign = TextAlign.Start),
    placeholderAlignment: Alignment.Horizontal = Alignment.Start,
    inputMinHeight: Dp = MaterialTheme.wireDimensions.textFieldMinHeight,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.textFieldCornerSize),
    colors: WireTextFieldColors = wireTextFieldColors(),
    modifier: Modifier = Modifier,
    onTap: ((Offset) -> Unit)? = null,
    testTag: String = String.EMPTY,
) {
    val autoFillType = if (autoFill) WireAutoFillType.Password else WireAutoFillType.None
    WireTextFieldLayout(
        shouldShowPlaceholder = textState.text.isEmpty(),
        placeholderText = placeholderText,
        labelText = labelText,
        labelMandatoryIcon = labelMandatoryIcon,
        descriptionText = descriptionText,
        state = state,
        interactionSource = interactionSource,
        placeholderTextStyle = placeholderTextStyle,
        placeholderAlignment = placeholderAlignment,
        inputMinHeight = inputMinHeight,
        shape = shape,
        colors = colors,
        modifier = modifier.autoFill(autoFillType, textState::setTextAndPlaceCursorAtEnd),
        testTag = testTag,
        onTap = onTap,
        innerBasicTextField = { decorator, textFieldModifier ->
            BasicSecureTextField(
                state = textState,
                textStyle = textStyle.copy(color = colors.textColor(state = state).value, textDirection = TextDirection.ContentOrLtr),
                imeAction = imeAction,
                onSubmit = { onImeAction?.invoke().let { onImeAction != null } },
                inputTransformation = inputTransformation,
                textObfuscationMode = textObfuscationMode,
                scrollState = scrollState,
                enabled = state !is WireTextFieldState.Disabled,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                interactionSource = interactionSource,
                modifier = textFieldModifier,
                decorator = decorator,
            )
        }
    )
}

/*
TODO: BasicSecureTextField (value, onValueChange) overload is removed completely in compose foundation 1.7.0,
      for now we can use our custom StateSyncingModifier to sync TextFieldValue with TextFieldState,
      but eventually we should migrate and remove this function when all usages are replaced with the TextFieldState.
*/
@Deprecated("Use the new one with TextFieldState.")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WirePasswordTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholderText: String? = stringResource(R.string.login_password_placeholder),
    labelText: String? = stringResource(R.string.login_password_label),
    labelMandatoryIcon: Boolean = false,
    descriptionText: String? = null,
    state: WireTextFieldState = WireTextFieldState.Default,
    autofill: Boolean,
    maxTextLength: Int = 8000,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    textStyle: TextStyle = MaterialTheme.wireTypography.body01.copy(textAlign = TextAlign.Start),
    placeholderTextStyle: TextStyle = MaterialTheme.wireTypography.body01.copy(textAlign = TextAlign.Start),
    placeholderAlignment: Alignment.Horizontal = Alignment.Start,
    inputMinHeight: Dp = MaterialTheme.wireDimensions.textFieldMinHeight,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.textFieldCornerSize),
    colors: WireTextFieldColors = wireTextFieldColors(),
    modifier: Modifier = Modifier,
    onTap: ((Offset) -> Unit)? = null,
    testTag: String = String.EMPTY,
) {
    val textState = remember { TextFieldState(value.text, value.selection) }
    val autoFillType = if (autofill) WireAutoFillType.Password else WireAutoFillType.None
    WireTextFieldLayout(
        shouldShowPlaceholder = textState.text.isEmpty(),
        placeholderText = placeholderText,
        labelText = labelText,
        labelMandatoryIcon = labelMandatoryIcon,
        descriptionText = descriptionText,
        state = state,
        interactionSource = interactionSource,
        placeholderTextStyle = placeholderTextStyle,
        placeholderAlignment = placeholderAlignment,
        inputMinHeight = inputMinHeight,
        shape = shape,
        colors = colors,
        modifier = modifier.autoFill(autoFillType, textState::setTextAndPlaceCursorAtEnd),
        testTag = testTag,
        onTap = onTap,
        innerBasicTextField = { decorator, textFieldModifier ->
            BasicSecureTextField(
                state = textState,
                textStyle = textStyle.copy(color = colors.textColor(state = state).value, textDirection = TextDirection.ContentOrLtr),
                imeAction = imeAction,
                onSubmit = { onImeAction?.invoke().let { onImeAction != null } },
                inputTransformation = InputTransformation.maxLength(maxTextLength),
                scrollState = scrollState,
                enabled = state !is WireTextFieldState.Disabled,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                interactionSource = interactionSource,
                modifier = textFieldModifier.then(StateSyncingModifier(textState, value, onValueChange)),
                decorator = decorator,
            )
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@PreviewMultipleThemes
@Composable
fun PreviewWirePasswordTextField() = WireTheme {
    WirePasswordTextField(
        textState = rememberTextFieldState(),
        modifier = Modifier.padding(16.dp)
    )
}
