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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.wireTextFieldColors
import com.wire.android.ui.home.messagecomposer.state.MessageComposeInputSize
import com.wire.android.ui.home.messagecomposer.state.MessageComposeInputState
import com.wire.android.ui.home.messagecomposer.state.MessageComposeInputType
import com.wire.android.ui.theme.wireTypography


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ActiveMessageComposerInputRow(
    activeMessageComposerInput: MessageComposeInputState.Active,
    focusRequester: FocusRequester = FocusRequester(),
    onMessageTextChanged: (TextFieldValue) -> Unit = { },
    onInputFocusChanged: (Boolean) -> Unit = { },
    onSendButtonClicked: () -> Unit = { },
    onSelectedLineIndexChanged: (Int) -> Unit = { },
    onLineBottomYCoordinateChanged: (Float) -> Unit = { },
    onEditSaveButtonClicked: () -> Unit = { },
    onEditCancelButtonClicked: () -> Unit = { },
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .weight(weight = 1f, fill = true)
                .wrapContentSize()
        ) {
            MessageComposerInput(
                isActive = true,
                messageText = activeMessageComposerInput.messageText,
                focusRequester = focusRequester,
                onMessageTextChanged = onMessageTextChanged,
                onFocusChanged = onInputFocusChanged,
                onSelectedLineIndexChanged = onSelectedLineIndexChanged,
                onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        when (activeMessageComposerInput.size) {
                            MessageComposeInputSize.COLLAPSED ->
                                Modifier.heightIn(max = dimensions().messageComposerActiveInputMaxHeight)

                            MessageComposeInputSize.EXPANDED ->
                                Modifier.fillMaxHeight()
                        }
                    )
                    .animateContentSize()
            )

            AnimatedVisibility(
                visible = activeMessageComposerInput.type is MessageComposeInputType.EditMessage,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MessageEditActions(
                    onEditSaveButtonClicked = onEditSaveButtonClicked,
                    onEditCancelButtonClicked = onEditCancelButtonClicked,
                    editButtonEnabled = activeMessageComposerInput.editSaveButtonEnabled
                )
            }
        }

        AnimatedVisibility(
            visible = activeMessageComposerInput.type is MessageComposeInputType.NewMessage,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            MessageSendActions(
                onSendButtonClicked = onSendButtonClicked,
                sendButtonEnabled = activeMessageComposerInput.sendButtonEnabled
            )
        }
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InActiveMessageComposerInputRow(
    messageComposeInputState: MessageComposeInputState,
    focusRequester: FocusRequester = FocusRequester(),
    onMessageTextChanged: (TextFieldValue) -> Unit = { },
    onInputFocusChanged: (Boolean) -> Unit = { },
    onSelectedLineIndexChanged: (Int) -> Unit = { },
    onLineBottomYCoordinateChanged: (Float) -> Unit = { },
    onAdditionalOptionButtonClicked: () -> Unit = { },
    isFileSharingEnabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(start = dimensions().spacing8x)) {
            AdditionalOptionButton(isSelected = messageComposeInputState.attachmentOptionsDisplayed, isEnabled = isFileSharingEnabled) {
                onAdditionalOptionButtonClicked()
            }
        }
        Column(
            modifier = Modifier
                .weight(weight = 1f, fill = true)
                .wrapContentSize()
        ) {
            MessageComposerInput(
                isActive = false,
                messageText = messageComposeInputState.messageText,
                focusRequester = focusRequester,
                onMessageTextChanged = onMessageTextChanged,
                onFocusChanged = onInputFocusChanged,
                onSelectedLineIndexChanged = onSelectedLineIndexChanged,
                onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .animateContentSize()
            )
        }
    }
}

//    @OptIn(ExperimentalAnimationApi::class)
//    @Composable
//    fun MessageComposerInputRow(
//        transition: Transition<MessageComposeInputState>,
//        messageComposeInputState: MessageComposeInputState,
//        focusRequester: FocusRequester = FocusRequester(),
//        onMessageTextChanged: (TextFieldValue) -> Unit = { },
//        onInputFocusChanged: (Boolean) -> Unit = { },
//        onSendButtonClicked: () -> Unit = { },
//        onSelectedLineIndexChanged: (Int) -> Unit = { },
//        onLineBottomYCoordinateChanged: (Float) -> Unit = { },
//        onAdditionalOptionButtonClicked: () -> Unit = { },
//        onEditSaveButtonClicked: () -> Unit = { },
//        onEditCancelButtonClicked: () -> Unit = { },
//        isFileSharingEnabled: Boolean = true,
//    ) {
//
//        transition.AnimatedContent() {
//            when (it) {
//                is MessageComposeInputState.Active -> TODO()
//                is MessageComposeInputState.Inactive -> TODO()
//            }
//        }
//    }

@Composable
private fun MessageComposerInput(
    messageText: TextFieldValue,
    isActive: Boolean,
    onMessageTextChanged: (TextFieldValue) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    focusRequester: FocusRequester,
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
        singleLine = isActive,
        maxLines = Int.MAX_VALUE,
        textStyle = MaterialTheme.wireTypography.body01,
        // Add an extra space so that the cursor is placed one space before "Type a message"
        placeholderText = " " + stringResource(R.string.label_type_a_message),
        modifier = modifier.then(
            Modifier
                .onFocusChanged { focusState ->
                    onFocusChanged(focusState.isFocused)
                }
                .focusRequester(focusRequester)
        ),
        onSelectedLineIndexChanged = onSelectedLineIndexChanged,
        onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged
    )
}

//@Preview
//@Composable
//fun PreviewMessageComposerInputRowInactive() {
//    val state = MessageComposeInputState.Inactive()
//    MessageComposerInputRow(updateTransition(targetState = state, label = ""), state)
//}
//
//@Preview
//@Composable
//fun PreviewMessageComposerInputRowActiveCollapsed() {
//    val state = MessageComposeInputState.Active(size = MessageComposeInputSize.COLLAPSED)
//    MessageComposerInputRow(updateTransition(targetState = state, label = ""), state)
//}
//
//@Preview
//@Composable
//fun PreviewMessageComposerInputRowActiveCollapsedSendEnabled() {
//    val state = MessageComposeInputState.Active(messageText = TextFieldValue("text"), size = MessageComposeInputSize.COLLAPSED)
//    MessageComposerInputRow(updateTransition(targetState = state, label = ""), state)
//}
//
//@Preview
//@Composable
//fun PreviewMessageComposerInputRowActiveExpanded() {
//    val state = MessageComposeInputState.Active(size = MessageComposeInputSize.EXPANDED)
//    MessageComposerInputRow(updateTransition(targetState = state, label = ""), state)
//}
//
//@Preview
//@Composable
//fun PreviewMessageComposerInputRowActiveEdit() {
//    val state = MessageComposeInputState.Active(
//        messageText = TextFieldValue("original text"),
//        type = MessageComposeInputType.EditMessage("", "original text")
//    )
//    MessageComposerInputRow(updateTransition(targetState = state, label = ""), state)
//}
//
//@Preview
//@Composable
//fun PreviewMessageComposerInputRowActiveEditSaveEnabled() {
//    val state = MessageComposeInputState.Active(
//        messageText = TextFieldValue("current text"),
//        type = MessageComposeInputType.EditMessage("", "original text")
//    )
//    MessageComposerInputRow(updateTransition(targetState = state, label = ""), state)
//}
