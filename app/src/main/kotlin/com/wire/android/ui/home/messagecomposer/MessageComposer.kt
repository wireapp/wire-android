package com.wire.android.ui.home.messagecomposer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.wireTextFieldColors
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.messagecomposer.attachment.AttachmentOptionsComponent
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MessageComposer(
    content: @Composable () -> Unit,
    messageText: String,
    onMessageChanged: (String) -> Unit,
    onSendButtonClicked: () -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onError: (String) -> Unit
) {
    val messageComposerState = rememberMessageComposerInnerState()

    LaunchedEffect(messageText) {
        messageComposerState.messageText = messageComposerState.messageText.copy(messageText)
    }

    MessageComposer(
        content = content,
        messageComposerState = messageComposerState,
        messageText = messageComposerState.messageText,
        onMessageChanged = {
            // we are setting it immediately in the UI first
            messageComposerState.messageText = it
            // we are hoisting the TextFieldValue text value up to the parent
            if (messageText != it.text) {
                onMessageChanged(it.text)
            }
        },
        onSendButtonClicked = onSendButtonClicked,
        onSendAttachment = onSendAttachment,
        onError = onError
    )
}

/*
* Message composer is a UI widget that handles the UI logic of sending messages,
* it is a wrapper around the "hosting" widget. It receives a [messageText] and
* exposes a [onMessageChanged] lambda, giving us the option to control its Message Text from outside the Widget.
* it also exposes [onSendButtonClicked] lambda's giving us the option to handle the different message actions
* */
@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
private fun MessageComposer(
    content: @Composable () -> Unit,
    messageComposerState: MessageComposerInnerState,
    messageText: TextFieldValue,
    onMessageChanged: (TextFieldValue) -> Unit,
    onSendButtonClicked: () -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onError: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Surface {
        val transition = updateTransition(
            targetState = messageComposerState.messageComposeInputState,
            label = stringResource(R.string.animation_label_messagecomposeinput_state_transistion)
        )
        // ConstraintLayout wrapping the whole content to give us the possibility to constrain SendButton to top of AdditionalOptions, which
        // constrains to bottom of MessageComposerInput
        // so that MessageComposerInput is the only component animating freely, when going to Fullscreen mode
        ConstraintLayout {
            val (additionalActions, sendActions, messageInput, additionalOptionsContent) = createRefs()
            // Column wrapping the content passed as Box with weight = 1f as @Composable lambda and the MessageComposerInput with
            // CollapseIconButton
            Column(
                Modifier.constrainAs(messageInput) {
                    bottom.linkTo(additionalActions.top)
                    top.linkTo(parent.top)

                    height = Dimension.preferredWrapContent
                }
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            focusManager.clearFocus()
                            messageComposerState.clickOutSideMessageComposer()
                        }
                ) {
                    content()
                }
                // Column wrapping CollapseIconButton
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Divider()
                    transition.AnimatedVisibility(visible = { state -> (state != MessageComposeInputState.Enabled) }) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        ) {
                            val collapseButtonRotationDegree by transition.animateFloat(
                                label = stringResource(R.string.animation_label_button_rotation_degree_transistion)
                            ) { state ->
                                when (state) {
                                    MessageComposeInputState.Active, MessageComposeInputState.Enabled -> 0f
                                    MessageComposeInputState.FullScreen -> 180f
                                }
                            }

                            CollapseIconButton(
                                onCollapseClick = { messageComposerState.toggleFullScreen() },
                                collapseRotation = collapseButtonRotationDegree
                            )
                        }
                    }
                    // Row wrapping the AdditionalOptionButton() when we are in Enabled state and MessageComposerInput()
                    // when we are in the Fullscreen state, we want to align the TextField to Top of the Row, when other we center it
                    // vertically. Once we go to Fullscreen, we set the weight to 1f so that it fills the whole Row which is =
                    // = height of the whole screen - height of TopBar - height of container with additional options
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
                            Box(modifier = Modifier.padding(start = MaterialTheme.wireDimensions.spacing8x)) {
                                AdditionalOptionButton(messageComposerState.attachmentOptionsDisplayed) {
                                    messageComposerState.attachmentOptionsDisplayed =
                                        !messageComposerState.attachmentOptionsDisplayed
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        // MessageComposerInput needs a padding on the end of it to give room for the SendOptions components, because
                        // it is "floating" freely with an absolute x-y position inside of the ConstrainLayout wrapping the whole content
                        // when in the FullScreen state we are giving it max height, when in active state we limit the height to max 82.dp
                        // other we let it wrap the content of the height, which will be equivalent to the text
                        MessageComposerInput(
                            messageText = messageText,
                            onMessageTextChanged = { value ->
                                onMessageChanged(value)
                            },
                            messageComposerInputState = messageComposerState.messageComposeInputState,
                            onFocusChanged = { messageComposerState.toActive() },
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
                                                    max = MaterialTheme.wireDimensions.messageComposerActiveInputMaxHeight
                                                )
                                                .padding(
                                                    end = dimensions().messageComposerPaddingEnd
                                                )
                                        }
                                        else -> Modifier.wrapContentHeight()
                                    }
                                )
                        )
                    }
                }
            }

            // Box wrapping the SendActions so that we do not include it in the animationContentSize changed which is applied only for
            // MessageComposerInput and CollapsingButton
            Box(
                Modifier.constrainAs(sendActions) {
                    end.linkTo(parent.end)
                    bottom.linkTo(additionalActions.top)
                }
            ) {
                Row {
                    if (messageComposerState.sendButtonEnabled) {
                        ScheduleMessageButton()
                    }
                    transition.AnimatedVisibility(
                        visible = { messageComposerState.messageComposeInputState != MessageComposeInputState.Enabled },
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        SendButton(
                            isEnabled = messageComposerState.sendButtonEnabled,
                            onSendButtonClicked = onSendButtonClicked
                        )
                    }
                }
            }

            // Box wrapping MessageComposeActions() so that we can constrain it to the bottom of MessageComposerInput and after that
            // constrain our SendActions to it
            Box(
                Modifier.constrainAs(additionalActions) {
                    bottom.linkTo(additionalOptionsContent.bottom)
                    top.linkTo(messageInput.bottom)
                }
            ) {
                Divider()
                transition.AnimatedVisibility(
                    visible = { messageComposerState.messageComposeInputState != MessageComposeInputState.Enabled },
                    // we are animating the exit, so that the MessageComposeActions go down
                    exit = slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight / 2 }
                    ) + fadeOut()
                ) {
                    MessageComposeActions(messageComposerState)
                }
            }

            // Box wrapping for additional options content
            Box(
                Modifier.constrainAs(additionalOptionsContent) {
                    bottom.linkTo(parent.bottom)
                    top.linkTo(additionalActions.bottom)
                }
            ) {
                if (messageComposerState.attachmentOptionsDisplayed) {
                    Divider()
                    AttachmentOptionsComponent(messageComposerState.attachmentInnerState, onSendAttachment, onError)
                }
            }
        }
    }
}

@Composable
private fun CollapseIconButton(onCollapseClick: () -> Unit, modifier: Modifier = Modifier, collapseRotation: Float = 0f) {
    IconButton(
        onClick = onCollapseClick,
        modifier = modifier.size(20.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_collapse),
            contentDescription = stringResource(R.string.content_description_drop_down_icon),
            modifier = Modifier.rotate(collapseRotation)
        )
    }
}

@Composable
private fun ScheduleMessageButton() {
    IconButton(onClick = { }) {
        Icon(
            painter = painterResource(id = R.drawable.ic_timer),
            contentDescription = stringResource(R.string.content_description_back_button),
        )
    }
}

@Composable
private fun MessageComposerInput(
    messageText: TextFieldValue,
    onMessageTextChanged: (TextFieldValue) -> Unit,
    messageComposerInputState: MessageComposeInputState,
    onFocusChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    WireTextField(
        value = messageText,
        onValueChange = onMessageTextChanged,
        colors = wireTextFieldColors(backgroundColor = Color.Transparent),
        singleLine = messageComposerInputState == MessageComposeInputState.Enabled,
        textStyle = MaterialTheme.wireTypography.body01,
        // Add a extra space so that the a cursor is placed one space before "Type a message"
        placeholderText = " " + stringResource(R.string.label_type_a_message),
        modifier = modifier.then(
            Modifier.onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onFocusChanged()
                }
            }
        )
    )
}

@Composable
private fun SendButton(
    isEnabled: Boolean,
    onSendButtonClicked: () -> Unit
) {
    IconButton(
        onClick = { if (isEnabled) onSendButtonClicked() },
        enabled = isEnabled
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    animateColorAsState(
                        when {
                            isEnabled -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.wireColorScheme.onSecondaryButtonDisabled
                        }
                    ).value
                )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_send),
                contentDescription = stringResource(R.string.content_description_back_button),
                tint = MaterialTheme.wireColorScheme.surface
            )
        }
    }
}

@Composable
private fun MessageComposeActions(messageComposerState: MessageComposerInnerState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        AdditionalOptionButton(messageComposerState.attachmentOptionsDisplayed) {
            messageComposerState.attachmentOptionsDisplayed = !messageComposerState.attachmentOptionsDisplayed
        }
        RichTextEditingAction()
        AddEmojiAction()
        AddGifAction()
        AddMentionAction()
    }
}

@Composable
private fun AdditionalOptionButton(isSelected: Boolean = false, onClick: () -> Unit) {
    WireSecondaryButton(
        onClick = { onClick() },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_add),
                contentDescription = stringResource(R.string.content_description_conversation_search_icon)
            )
        },
        state = if (isSelected) WireButtonState.Selected else WireButtonState.Default,
        fillMaxWidth = false,
        minHeight = 32.dp,
        minWidth = 40.dp,
    )
}

@Composable
private fun RichTextEditingAction() {
    WireSecondaryButton(
        onClick = { },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_rich_text),
                contentDescription = stringResource(R.string.content_description_conversation_search_icon),
            )
        },
        fillMaxWidth = false,
        minHeight = 32.dp,
        minWidth = 40.dp
    )
}

@Composable
private fun AddEmojiAction() {
    WireSecondaryButton(
        onClick = { },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_emoticon),
                contentDescription = stringResource(R.string.content_description_conversation_search_icon),
            )
        },
        fillMaxWidth = false,
        minHeight = 32.dp,
        minWidth = 40.dp
    )
}

@Composable
private fun AddGifAction() {
    WireSecondaryButton(
        onClick = { },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_gif),
                contentDescription = stringResource(R.string.content_description_conversation_search_icon),
            )
        },
        fillMaxWidth = false,
        minHeight = 32.dp,
        minWidth = 40.dp,
    )
}

@Composable
private fun AddMentionAction() {
    WireSecondaryButton(
        onClick = { },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_mention),
                contentDescription = stringResource(R.string.content_description_conversation_search_icon),
            )
        },
        fillMaxWidth = false,
        minHeight = 32.dp,
        minWidth = 40.dp,
    )
}
