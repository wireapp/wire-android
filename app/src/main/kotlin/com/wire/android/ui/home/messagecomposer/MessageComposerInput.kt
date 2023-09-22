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

package com.wire.android.ui.home.messagecomposer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Divider
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldColors
import com.wire.android.ui.home.conversations.messages.QuotedMessagePreview
import com.wire.android.ui.home.messagecomposer.attachments.AdditionalOptionButton
import com.wire.android.ui.home.messagecomposer.state.MessageComposition
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionType
import com.wire.android.ui.home.messagecomposer.state.MessageType
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun ActiveMessageComposerInput(
    messageComposition: MessageComposition,
    isTextExpanded: Boolean,
    inputType: MessageCompositionType,
    inputFocused: Boolean,
    onMessageTextChanged: (TextFieldValue) -> Unit,
    onSendButtonClicked: () -> Unit,
    onEditButtonClicked: () -> Unit,
    onChangeSelfDeletionClicked: () -> Unit,
    onToggleInputSize: () -> Unit,
    onCancelReply: () -> Unit,
    onCancelEdit: () -> Unit,
    onInputFocusedChanged: (Boolean) -> Unit,
    onSelectedLineIndexChanged: (Int) -> Unit,
    onLineBottomYCoordinateChanged: (Float) -> Unit,
    showOptions: Boolean,
    onPlusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .wrapContentSize()
            .background(inputType.backgroundColor())
    ) {
        Divider(color = MaterialTheme.wireColorScheme.outline)
        if (showOptions) {
            CollapseButton(
                isCollapsed = !isTextExpanded,
                onCollapseClick = onToggleInputSize
            )
        }

        messageComposition.quotedMessage?.let { quotedMessage ->
            Box(modifier = Modifier.padding(horizontal = dimensions().spacing8x)) {
                QuotedMessagePreview(
                    quotedMessageData = quotedMessage,
                    onCancelReply = onCancelReply
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalAlignment = Alignment.Bottom
        ) {
            val stretchToMaxParentConstraintHeightOrWithInBoundary = if (isTextExpanded) {
                Modifier.fillMaxHeight()
            } else {
                Modifier.heightIn(max = dimensions().messageComposerActiveInputMaxHeight)
            }.weight(1f)

            if (!showOptions) {
                AdditionalOptionButton(
                    isSelected = false,
                    onClick = {
                        onPlusClick()
                    },
                    modifier = Modifier.padding(start = dimensions().spacing8x)
                )
            }

            MessageComposerTextInput(
                inputFocused = inputFocused,
                colors = inputType.inputTextColor(),
                messageText = messageComposition.messageTextFieldValue,
                placeHolderText = inputType.labelText(),
                onMessageTextChanged = onMessageTextChanged,
                singleLine = false,
                onFocusChanged = onInputFocusedChanged,
                onSelectedLineIndexChanged = onSelectedLineIndexChanged,
                onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged,
                modifier = stretchToMaxParentConstraintHeightOrWithInBoundary
            )

            if (showOptions) {
                Row(Modifier.wrapContentSize()) {
                    if (inputType is MessageCompositionType.Composing) {
                        when (val messageType = inputType.messageType.value) {
                            is MessageType.Normal -> {
                                MessageSendActions(
                                    onSendButtonClicked = onSendButtonClicked,
                                    sendButtonEnabled = inputType.isSendButtonEnabled
                                )
                            }

                            is MessageType.SelfDeleting -> {
                                SelfDeletingActions(
                                    onSendButtonClicked = onSendButtonClicked,
                                    sendButtonEnabled = inputType.isSendButtonEnabled,
                                    selfDeletionTimer = messageType.selfDeletionTimer,
                                    onChangeSelfDeletionClicked = onChangeSelfDeletionClicked
                                )
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
        when (inputType) {
            is MessageCompositionType.Editing -> {
                MessageEditActions(
                    onEditSaveButtonClicked = onEditButtonClicked,
                    onEditCancelButtonClicked = onCancelEdit,
                    editButtonEnabled = inputType.isEditButtonEnabled
                )
            }

            else -> {}
        }
    }
}

@Composable
private fun MessageComposerTextInput(
    inputFocused: Boolean,
    colors: WireTextFieldColors,
    singleLine: Boolean,
    messageText: TextFieldValue,
    placeHolderText: String,
    onMessageTextChanged: (TextFieldValue) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    onSelectedLineIndexChanged: (Int) -> Unit = { },
    onLineBottomYCoordinateChanged: (Float) -> Unit = { },
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isReadOnly by remember { mutableStateOf(false) }

    LaunchedEffect(inputFocused) {
        if (inputFocused) {
            isReadOnly = false
            keyboardController?.show()
            focusRequester.requestFocus()
        } else {
            isReadOnly = true
            keyboardController?.hide()
        }
    }

    LaunchedEffect(isPressed) {
        if(isPressed) {
            onFocusChanged(true)
        }
    }

    WireTextField(
        value = messageText,
        onValueChange = onMessageTextChanged,
        colors = colors,
        singleLine = singleLine,
        maxLines = Int.MAX_VALUE,
        textStyle = MaterialTheme.wireTypography.body01,
        // Add an extra space so that the cursor is placed one space before "Type a message"
        placeholderText = " $placeHolderText",
        readOnly = isReadOnly,
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onFocusChanged(focusState.isFocused)
                }
            },
        interactionSource = interactionSource,
        onSelectedLineIndexChanged = onSelectedLineIndexChanged,
        onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged
    )
}

@Composable
fun CollapseButton(
    isCollapsed: Boolean,
    onCollapseClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val collapseButtonRotationDegree by animateFloatAsState(targetValue = if (isCollapsed) 0F else 180f)

        IconButton(
            onClick = {
                onCollapseClick()
            },
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_collapse),
                contentDescription = stringResource(R.string.content_description_drop_down_icon),
                tint = colorsScheme().onSecondaryButtonDisabled,
                modifier = Modifier.rotate(collapseButtonRotationDegree)
            )
        }
    }
}
