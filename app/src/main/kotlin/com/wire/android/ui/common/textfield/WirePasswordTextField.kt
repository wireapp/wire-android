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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.android.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WirePasswordTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    readOnly: Boolean = false,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    placeholderText: String? = stringResource(R.string.login_password_placeholder),
    labelText: String? = stringResource(R.string.login_password_label),
    labelMandatoryIcon: Boolean = false,
    descriptionText: String? = null,
    state: WireTextFieldState = WireTextFieldState.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    textStyle: TextStyle = LocalTextStyle.current.copy(fontSize = 15.sp, textAlign = TextAlign.Start),
    placeHolderTextStyle: TextStyle = LocalTextStyle.current.copy(fontSize = 15.sp, textAlign = TextAlign.Start),
    inputMinHeight: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    colors: WireTextFieldColors = wireTextFieldColors(),
    modifier: Modifier = Modifier,
    autofillTypes: List<AutofillType> = listOf(AutofillType.Password)
) {
    var passwordVisibility by remember { mutableStateOf(false) }
    AutoFillTextField(
        autofillTypes = autofillTypes,
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        singleLine = true,
        maxLines = 1,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false, imeAction = imeAction),
        keyboardActions = keyboardActions,
        placeholderText = placeholderText,
        labelText = labelText,
        labelMandatoryIcon = labelMandatoryIcon,
        descriptionText = descriptionText,
        state = state,
        interactionSource = interactionSource,
        textStyle = textStyle,
        placeholderTextStyle = placeHolderTextStyle,
        inputMinHeight = inputMinHeight,
        shape = shape,
        colors = colors,
        modifier = modifier,
        visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val image = if (passwordVisibility) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                Icon(
                    imageVector = image,
                    contentDescription = stringResource(
                        if (!passwordVisibility) R.string.content_description_reveal_password
                        else R.string.content_description_hide_password
                    ),
                    modifier = Modifier
                        .size(20.dp)
                        .testTag("hidePassword")
                )
            }
        },
    )
}


@OptIn(ExperimentalComposeUiApi::class)
@Preview(name = "Default WirePasswordTextField")
@Composable
fun PreviewWirePasswordTextField() {
    WirePasswordTextField(
        value = TextFieldValue(""),
        onValueChange = {},
        modifier = Modifier.padding(16.dp)
    )
}
