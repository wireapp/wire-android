package com.wire.android.ui.home.messagecomposer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Transition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.wireTextFieldColors
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.feature.conversation.InteractionAvailability

@ExperimentalAnimationApi
@Composable
fun ColumnScope.MessageComposerInputRow(
    transition: Transition<MessageComposeInputState>,
    interactionAvailability: InteractionAvailability,
    messageComposerState: MessageComposerInnerState,
    membersToMention: List<Contact>,
    onMentionPicked: (Contact) -> Unit,
    onSendButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {

    var currentSelectedLineIndex by remember {
        mutableStateOf(0)
    }

    var cursorCoordinateY by remember {
        mutableStateOf(0F)
    }

    Row(
        verticalAlignment =
        if (messageComposerState.messageComposeInputState == MessageComposeInputState.FullScreen)
            Alignment.Top
        else
            Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (messageComposerState.messageComposeInputState == MessageComposeInputState.FullScreen)
                    Modifier.weight(1f)
                else
                    Modifier
            )
    ) {
        transition.AnimatedVisibility(
            visible = { messageComposerState.messageComposeInputState == MessageComposeInputState.Enabled }
        ) {
            Box(modifier = Modifier.padding(start = dimensions().spacing8x)) {
                AdditionalOptionButton(messageComposerState.attachmentOptionsDisplayed) {
                    messageComposerState.toActive()
                    messageComposerState.showAttachmentOptions()
                }
            }
        }
        // MessageComposerInput needs a padding on the end of it to give room for the SendOptions components,
        // because it is "floating" freely with an absolute x-y position inside of the ConstrainLayout
        // wrapping the whole content when in the FullScreen state we are giving it max height
        // when in active state we limit the height to max 82.dp
        // other we let it wrap the content of the height, which will be equivalent to the text
        Box(
            Modifier
                .wrapContentSize()
        ) {
            MessageComposerInput(
                messageText = messageComposerState.messageText,
                onMessageTextChanged = messageComposerState::setMessageTextValue,
                messageComposerInputState = messageComposerState.messageComposeInputState,
                onIsFocused = {
                    messageComposerState.toActive()
                    messageComposerState.hideAttachmentOptions()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        when (messageComposerState.messageComposeInputState) {
                            MessageComposeInputState.FullScreen ->
                                Modifier
                                    .fillMaxHeight()
                                    .padding(end = dimensions().messageComposerPaddingEnd)

                            MessageComposeInputState.Active -> {
                                Modifier
                                    .heightIn(
                                        max = dimensions().messageComposerActiveInputMaxHeight
                                    )
                                    .padding(
                                        end = dimensions().messageComposerPaddingEnd
                                    )
                            }

                            else -> Modifier.wrapContentHeight()
                        }
                    )
                    .animateContentSize(),
                onSelectedLineIndexChanged = {
                    currentSelectedLineIndex = it
                },
                onLineBottomYCoordinateChanged = {
                    cursorCoordinateY = it
                }
            )

            // This is a SEND ACTIONS
            if (interactionAvailability == InteractionAvailability.ENABLED) {
                // Box wrapping the SendActions so that we do not include it in the animationContentSize
                // changed which is applied only for
                // MessageComposerInput and CollapsingButton
                SendActions(
                    messageComposerState = messageComposerState,
                    transition = transition,
                    onSendButtonClicked = onSendButtonClicked,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
        if (membersToMention.isNotEmpty() && messageComposerState.messageComposeInputState == MessageComposeInputState.FullScreen)
            DropDownMentionsSuggestions(currentSelectedLineIndex, cursorCoordinateY, membersToMention, onMentionPicked)

    }
}

@Composable
private fun MessageComposerInput(
    messageText: TextFieldValue,
    onMessageTextChanged: (TextFieldValue) -> Unit,
    messageComposerInputState: MessageComposeInputState,
    onIsFocused: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectedLineIndexChanged: (Int) -> Unit = { },
    onLineBottomYCoordinateChanged: (Float) -> Unit = { }
) {
    WireTextField(
        value = messageText,
        onValueChange = onMessageTextChanged,
        colors = wireTextFieldColors(
            borderColor = Color.Transparent,
            focusColor = Color.Transparent
        ),
        singleLine = messageComposerInputState == MessageComposeInputState.Enabled,
        maxLines = Int.MAX_VALUE,
        textStyle = MaterialTheme.wireTypography.body01,
        // Add a extra space so that the a cursor is placed one space before "Type a message"
        placeholderText = " " + stringResource(R.string.label_type_a_message),
        modifier = modifier.then(
            Modifier
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onIsFocused()
                    }
                }
        ),
        onSelectedLineIndexChanged = onSelectedLineIndexChanged,
        onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged
    )
}
