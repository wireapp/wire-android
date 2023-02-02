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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.wireTextFieldColors
import com.wire.android.ui.theme.wireTypography

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MessageComposerInputRow(
    transition: Transition<MessageComposeInputState>,
    messageComposeInputState: MessageComposeInputState,
    onMessageTextChanged: (TextFieldValue) -> Unit = { },
    onInputFocusChanged: (Boolean) -> Unit = { },
    onSendButtonClicked: () -> Unit = { },
    onSelectedLineIndexChanged: (Int) -> Unit = { },
    onLineBottomYCoordinateChanged: (Float) -> Unit = { },
    onAdditionalOptionButtonClicked: () -> Unit = { },
    onEditSaveButtonClicked: () -> Unit = { },
    onEditCancelButtonClicked: () -> Unit = { },
) {
    Row(
        verticalAlignment = when (messageComposeInputState) {
            is MessageComposeInputState.Active -> Alignment.Bottom
            is MessageComposeInputState.Inactive -> Alignment.CenterVertically
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        transition.AnimatedVisibility(
            visible = { it is MessageComposeInputState.Inactive }
        ) {
            Box(modifier = Modifier.padding(start = dimensions().spacing8x)) {
                AdditionalOptionButton(messageComposeInputState.attachmentOptionsDisplayed) {
                    onAdditionalOptionButtonClicked()
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(weight = 1f, fill = true)
                .wrapContentSize()
        ) {

            MessageComposerInput(
                messageText = messageComposeInputState.messageText,
                onMessageTextChanged = onMessageTextChanged,
                messageComposerInputState = messageComposeInputState,
                onFocusChanged = onInputFocusChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        when (messageComposeInputState) {
                            is MessageComposeInputState.Active ->
                                when (messageComposeInputState.size) {
                                    MessageComposeInputSize.COLLAPSED ->
                                        Modifier.heightIn(max = dimensions().messageComposerActiveInputMaxHeight)

                                    MessageComposeInputSize.EXPANDED ->
                                        Modifier.fillMaxHeight()
                                }

                            is MessageComposeInputState.Inactive ->
                                Modifier.wrapContentHeight()
                        }
                    )
                    .animateContentSize(),
                onSelectedLineIndexChanged = onSelectedLineIndexChanged,
                onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged
            )

            transition.AnimatedVisibility(
                visible = { it.isEditMessage },
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MessageEditActions(
                    onEditSaveButtonClicked = onEditSaveButtonClicked,
                    onEditCancelButtonClicked = onEditCancelButtonClicked,
                    editButtonEnabled = messageComposeInputState.editSaveButtonEnabled
                )
            }
        }
        transition.AnimatedVisibility(
            visible = { it.isNewMessage },
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            MessageSendActions(
                onSendButtonClicked = onSendButtonClicked,
                sendButtonEnabled = messageComposeInputState.sendButtonEnabled
            )
        }
    }
}

@Composable
private fun MessageComposerInput(
    messageText: TextFieldValue,
    onMessageTextChanged: (TextFieldValue) -> Unit,
    messageComposerInputState: MessageComposeInputState,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onSelectedLineIndexChanged: (Int) -> Unit = { },
    onLineBottomYCoordinateChanged: (Float) -> Unit = { }
) {
    WireTextField(
        value = messageText,
        onValueChange = onMessageTextChanged,
        colors = wireTextFieldColors(
            backgroundColor = Color.Transparent,
            borderColor = Color.Transparent,
            focusColor = Color.Transparent
        ),
        singleLine = messageComposerInputState is MessageComposeInputState.Inactive,
        maxLines = Int.MAX_VALUE,
        textStyle = MaterialTheme.wireTypography.body01,
        // Add an extra space so that the cursor is placed one space before "Type a message"
        placeholderText = " " + stringResource(R.string.label_type_a_message),
        modifier = modifier.then(
            Modifier
                .onFocusChanged { focusState ->
                    onFocusChanged(focusState.isFocused)
                }
        ),
        onSelectedLineIndexChanged = onSelectedLineIndexChanged,
        onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged
    )
}

@Preview
@Composable
fun PreviewMessageComposerInputRowInactive() {
    val state = MessageComposeInputState.Inactive()
    MessageComposerInputRow(updateTransition(targetState = state, label = ""), state)
}
@Preview
@Composable
fun PreviewMessageComposerInputRowActiveCollapsed() {
    val state = MessageComposeInputState.Active(size = MessageComposeInputSize.COLLAPSED)
    MessageComposerInputRow(updateTransition(targetState = state, label = ""), state)
}
@Preview
@Composable
fun PreviewMessageComposerInputRowActiveCollapsedSendEnabled() {
    val state = MessageComposeInputState.Active(messageText = TextFieldValue("text"), size = MessageComposeInputSize.COLLAPSED)
    MessageComposerInputRow(updateTransition(targetState = state, label = ""), state)
}
@Preview
@Composable
fun PreviewMessageComposerInputRowActiveExpanded() {
    val state = MessageComposeInputState.Active(size = MessageComposeInputSize.EXPANDED)
    MessageComposerInputRow(updateTransition(targetState = state, label = ""), state)
}
@Preview
@Composable
fun PreviewMessageComposerInputRowActiveEdit() {
    val state = MessageComposeInputState.Active(
        messageText = TextFieldValue("original text"),
        type = MessageComposeInputType.EditMessage("", "original text")
    )
    MessageComposerInputRow(updateTransition(targetState = state, label = ""), state)
}
@Preview
@Composable
fun PreviewMessageComposerInputRowActiveEditSaveEnabled() {
    val state = MessageComposeInputState.Active(
        messageText = TextFieldValue("current text"),
        type = MessageComposeInputType.EditMessage("", "original text")
    )
    MessageComposerInputRow(updateTransition(targetState = state, label = ""), state)
}
