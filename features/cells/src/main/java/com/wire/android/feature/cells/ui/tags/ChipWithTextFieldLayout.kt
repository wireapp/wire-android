/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.cells.ui.tags

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.wire.android.feature.cells.R
import com.wire.android.ui.common.chip.WireFilterChip
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.textfield.WireTextFieldColors
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.textfield.wireTextFieldColors
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipAndTextFieldLayout(
    textFieldState: TextFieldState,
    onRemoveLastTag: () -> Unit,
    modifier: Modifier = Modifier,
    isValidTag: () -> Boolean = { false },
    onDone: (String) -> Unit = { _ -> },
    textStyle: TextStyle = MaterialTheme.wireTypography.body01,
    style: WireTextFieldState = WireTextFieldState.Default,
    colors: WireTextFieldColors = wireTextFieldColors(),
    chipsLayout: @Composable FlowRowScope.() -> Unit
) {

    val showErrorMessage = remember { mutableStateOf(false) }

    LaunchedEffect(textFieldState.text) {
        showErrorMessage.value = !isValidTag() && textFieldState.text.isNotEmpty()
    }

    val focusRequester = remember {
        FocusRequester()
    }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }
    Column {
        FlowRow(
            modifier = modifier
                .background(colorsScheme().surfaceVariant)
                .padding(dimensions().spacing16x)
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x),
            verticalArrangement = Arrangement.Bottom,
        ) {

            chipsLayout()

            Box(
                modifier = Modifier
                    .height(dimensions().spacing56x)
                    .widthIn(min = dimensions().spacing100x)
                    .weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { keyEvent ->
                            if (
                                keyEvent.key == Key.Backspace &&
                                keyEvent.type == KeyEventType.KeyDown &&
                                textFieldState.text.isEmpty()
                            ) {
                                onRemoveLastTag()
                                true
                            } else {
                                false
                            }
                        },
                    state = textFieldState,
                    textStyle = textStyle.copy(
                        color = colors.textColor(WireTextFieldState.Default).value,
                    ),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    onKeyboardAction = KeyboardActionHandler { _ ->
                        if (isValidTag()) {
                            onDone(textFieldState.text.toString())
                        }
                    },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorator = TextFieldDecorator { innerTextField ->
                        Box {
                            if (textFieldState.text.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.enter_name_label),
                                    color = colors.placeholderColor(style).value,
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
        AnimatedVisibility(showErrorMessage.value) {
            Text(
                modifier = Modifier
                    .padding(start = dimensions().spacing16x, bottom = dimensions().spacing8x),
                text = stringResource(R.string.invalid_tag_name_error),
                style = MaterialTheme.wireTypography.label04,
                color = colorsScheme().error,
            )
        }
    }
}

@Composable
@MultipleThemePreviews
fun PreviewChipAndTextFieldLayout() {
    WireTheme {
        ChipAndTextFieldLayout(
            textFieldState = TextFieldState(),
            onRemoveLastTag = {},
        ) {
            WireFilterChip(
                modifier = Modifier.align(Alignment.CenterVertically),
                label = "Test",
                isSelected = true,
            )
        }
    }
}
