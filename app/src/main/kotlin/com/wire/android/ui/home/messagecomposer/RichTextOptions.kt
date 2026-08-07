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
package com.wire.android.ui.home.messagecomposer

import androidx.annotation.StringRes
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.common.R as commonR

@Composable
fun RichTextOptions(
    onRichTextHeaderButtonClicked: () -> Unit,
    onRichTextBoldButtonClicked: () -> Unit,
    onRichTextItalicButtonClicked: () -> Unit,
    onCloseRichTextEditingButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    useKeyboardNavigation: Boolean = false,
) {
    val focusRequesters = remember { List(RICH_TEXT_OPTION_COUNT) { FocusRequester() } }
    if (useKeyboardNavigation) {
        LaunchedEffect(Unit) { focusRequesters.first().requestFocus() }
    }
    Column(modifier.wrapContentSize()) {
        HorizontalDivider(color = MaterialTheme.wireColorScheme.outline)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Absolute.SpaceEvenly,
            modifier = Modifier.wrapContentSize()
                .height(dimensions().spacing56x)
        ) {
            val iconModifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = dimensions().spacing0x)

            if (useKeyboardNavigation) {
                KeyboardRichTextOption(
                    index = 0,
                    focusRequesters = focusRequesters,
                    contentDescription = R.string.content_description_conversation_rich_text_header,
                    onClick = onRichTextHeaderButtonClicked,
                    modifier = iconModifier,
                ) { isFocused ->
                    HeaderButton(
                        modifier = Modifier.focusProperties { canFocus = false },
                        onRichTextHeaderButtonClicked = onRichTextHeaderButtonClicked,
                        isFocused = isFocused,
                    )
                }
                KeyboardRichTextOption(
                    index = 1,
                    focusRequesters = focusRequesters,
                    contentDescription = R.string.content_description_conversation_rich_text_bold,
                    onClick = onRichTextBoldButtonClicked,
                    modifier = iconModifier,
                ) { isFocused ->
                    BoldButton(
                        modifier = Modifier.focusProperties { canFocus = false },
                        onRichTextBoldButtonClicked = onRichTextBoldButtonClicked,
                        isFocused = isFocused,
                    )
                }
                KeyboardRichTextOption(
                    index = 2,
                    focusRequesters = focusRequesters,
                    contentDescription = R.string.content_description_conversation_rich_text_italic,
                    onClick = onRichTextItalicButtonClicked,
                    modifier = iconModifier,
                ) { isFocused ->
                    ItalicButton(
                        modifier = Modifier.focusProperties { canFocus = false },
                        onRichTextItalicButtonClicked = onRichTextItalicButtonClicked,
                        isFocused = isFocused,
                    )
                }
                KeyboardRichTextOption(
                    index = 3,
                    focusRequesters = focusRequesters,
                    contentDescription = R.string.content_description_close_button,
                    onClick = onCloseRichTextEditingButtonClicked,
                ) { isFocused ->
                    CloseButton(
                        modifier = Modifier.focusProperties { canFocus = false },
                        onCloseRichTextEditingButtonClicked = onCloseRichTextEditingButtonClicked,
                        isFocused = isFocused,
                        useKeyboardFocusStyle = true,
                    )
                }
            } else {
                HeaderButton(onRichTextHeaderButtonClicked, iconModifier)
                BoldButton(onRichTextBoldButtonClicked, iconModifier)
                ItalicButton(onRichTextItalicButtonClicked, iconModifier)
                CloseButton(onCloseRichTextEditingButtonClicked)
            }
        }
    }
}

@Composable
private fun HeaderButton(
    onRichTextHeaderButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isFocused: Boolean = false,
) {
    WireSecondaryIconButton(
        onButtonClicked = onRichTextHeaderButtonClicked,
        iconResource = R.drawable.ic_rich_text_header,
        contentDescription = R.string.content_description_conversation_rich_text_header,
        modifier = modifier
            .padding(start = dimensions().spacing8x),
        fillMaxWidth = true,
        shape = RoundedCornerShape(
            topStart = MaterialTheme.wireDimensions.buttonCornerSize,
            bottomStart = MaterialTheme.wireDimensions.buttonCornerSize,
            topEnd = MaterialTheme.wireDimensions.spacing0x,
            bottomEnd = MaterialTheme.wireDimensions.spacing0x
        ),
        colors = focusedSecondaryButtonColors(isFocused),
    )
}

@Composable
private fun BoldButton(
    onRichTextBoldButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isFocused: Boolean = false,
) {
    WireSecondaryIconButton(
        onButtonClicked = onRichTextBoldButtonClicked,
        iconResource = R.drawable.ic_rich_text_bold,
        contentDescription = R.string.content_description_conversation_rich_text_bold,
        modifier = modifier,
        fillMaxWidth = true,
        shape = RoundedCornerShape(
            topStart = MaterialTheme.wireDimensions.spacing0x,
            bottomStart = MaterialTheme.wireDimensions.spacing0x,
            topEnd = MaterialTheme.wireDimensions.spacing0x,
            bottomEnd = MaterialTheme.wireDimensions.spacing0x
        ),
        colors = focusedSecondaryButtonColors(isFocused),
    )
}

@Composable
private fun ItalicButton(
    onRichTextItalicButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isFocused: Boolean = false,
) {
    WireSecondaryIconButton(
        onButtonClicked = onRichTextItalicButtonClicked,
        iconResource = R.drawable.ic_rich_text_italic,
        contentDescription = R.string.content_description_conversation_rich_text_italic,
        modifier = modifier,
        fillMaxWidth = true,
        shape = RoundedCornerShape(
            topStart = MaterialTheme.wireDimensions.spacing0x,
            bottomStart = MaterialTheme.wireDimensions.spacing0x,
            topEnd = MaterialTheme.wireDimensions.buttonCornerSize,
            bottomEnd = MaterialTheme.wireDimensions.buttonCornerSize
        ),
        colors = focusedSecondaryButtonColors(isFocused),
    )
}

@Composable
private fun CloseButton(
    onCloseRichTextEditingButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isFocused: Boolean = false,
    useKeyboardFocusStyle: Boolean = false,
) {
    if (useKeyboardFocusStyle) {
        WireSecondaryIconButton(
            onButtonClicked = onCloseRichTextEditingButtonClicked,
            iconResource = commonR.drawable.ic_close,
            contentDescription = R.string.content_description_close_button,
            colors = focusedSecondaryButtonColors(isFocused),
            modifier = modifier.padding(end = dimensions().spacing8x),
        )
    } else {
        IconButton(
            onClick = onCloseRichTextEditingButtonClicked,
            modifier = modifier.padding(end = dimensions().spacing8x),
        ) {
            Icon(
                painter = painterResource(commonR.drawable.ic_close),
                contentDescription = stringResource(R.string.content_description_close_button),
            )
        }
    }
}

@Composable
private fun KeyboardRichTextOption(
    index: Int,
    focusRequesters: List<FocusRequester>,
    @StringRes contentDescription: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Boolean) -> Unit,
) {
    val description = stringResource(contentDescription)
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .focusRequester(focusRequesters[index])
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                        onClick()
                        true
                    }

                    Key.Tab, Key.DirectionLeft, Key.DirectionRight -> {
                        val move = when {
                            event.key == Key.DirectionLeft || event.isShiftPressed -> RichTextFocusMove.Previous
                            else -> RichTextFocusMove.Next
                        }
                        focusRequesters[richTextFocusTargetIndex(index, focusRequesters.size, move)].requestFocus()
                        true
                    }

                    else -> false
                }
            }
            .semantics {
                this.contentDescription = description
                role = Role.Button
            }
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        content(isFocused)
    }
}

@Composable
private fun focusedSecondaryButtonColors(isFocused: Boolean) = messageComposerSecondaryButtonColors().let { colors ->
    if (isFocused) colors.copy(enabled = colors.focused) else colors
}

private const val RICH_TEXT_OPTION_COUNT = 4

internal enum class RichTextFocusMove { Previous, Next }

internal fun richTextFocusTargetIndex(
    currentIndex: Int,
    itemCount: Int,
    move: RichTextFocusMove,
): Int = when (move) {
    RichTextFocusMove.Previous -> (currentIndex - 1 + itemCount) % itemCount
    RichTextFocusMove.Next -> (currentIndex + 1) % itemCount
}
