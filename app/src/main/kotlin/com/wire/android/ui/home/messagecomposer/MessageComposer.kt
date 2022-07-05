package com.wire.android.ui.home.messagecomposer

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireIconButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.wireTextFieldColors
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.messagecomposer.attachment.AttachmentOptionsComponent
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

private val DEFAULT_KEYBOARD_TOP_SCREEN_OFFSET = 250.dp

@Composable
fun MessageComposer(
    content: @Composable () -> Unit,
    messageText: String,
    onMessageChanged: (String) -> Unit,
    onSendButtonClicked: () -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onMessageComposerError: (ConversationSnackbarMessages) -> Unit,
    onMessageComposerInputStateChange: (MessageComposerStateTransition) -> Unit,
    isFileSharingEnabled: Boolean
) {
    BoxWithConstraints {
        val messageComposerState = rememberMessageComposerInnerState(
            fullScreenHeight = with(LocalDensity.current) { constraints.maxHeight.toDp() },
            onMessageComposeInputStateChanged = onMessageComposerInputStateChange
        )

        LaunchedEffect(messageText) {
            messageComposerState.messageText = messageComposerState.messageText.copy(messageText)
        }

        MessageComposer(
            content = content,
            screenHeight = with(LocalDensity.current) { constraints.maxHeight.toDp() },
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
            onSendAttachment = {
                onSendAttachment(it)
                messageComposerState.toggleAttachmentOptionsVisibility()
            },
            onMessageComposerError = onMessageComposerError,
            isFileSharingEnabled = isFileSharingEnabled
        )
    }
}

/*
* Message composer is a UI widget that handles the UI logic of sending messages,
* it is a wrapper around the "hosting" widget. It receives a [messageText] and
* exposes a [onMessageChanged] lambda, giving us the option to control its Message Text from outside the Widget.
* it also exposes [onSendButtonClicked] lambda's giving us the option to handle the different message actions
* */
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MessageComposer(
    content: @Composable () -> Unit,
    screenHeight: Dp,
    messageComposerState: MessageComposerInnerState,
    messageText: TextFieldValue,
    onMessageChanged: (TextFieldValue) -> Unit,
    onSendButtonClicked: () -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onMessageComposerError: (ConversationSnackbarMessages) -> Unit,
    isFileSharingEnabled: Boolean
) {
    val focusManager = LocalFocusManager.current
    // when MessageComposer is composed for the first time we do not know the height
    // until users opens the keyboard
    var keyboardHeightOffSet: KeyboardHeight by remember {
        mutableStateOf(KeyboardHeight.NotKnown)
    }
    // if the currentScreenHeight is smaller than the initial fullScreenHeight
    // calculated at the first composition of the MessageComposer, then we know the keyboard size
    if (screenHeight < messageComposerState.fullScreenHeight) {
        keyboardHeightOffSet = KeyboardHeight.Known(messageComposerState.fullScreenHeight - screenHeight)
    }

    Surface {
        val transition = updateTransition(
            targetState = messageComposerState.messageComposeInputState,
            label = stringResource(R.string.animation_label_messagecomposeinput_state_transistion)
        )

        BackHandler(enabled = messageComposerState.attachmentOptionsDisplayed) {
            messageComposerState.toggleAttachmentOptionsVisibility()
        }


        // ConstraintLayout wrapping the whole content to give us the possibility to constrain SendButton to top of AdditionalOptions, which
        // constrains to bottom of MessageComposerInput
        // so that MessageComposerInput is the only component animating freely, when going to Fullscreen mode
        ConstraintLayout(Modifier.fillMaxSize()) {
            // This guide line is used was when the attachment options are visible
            // we need to use it to correctly offset the MessageComposerInput so that it is on a static place on the screen
            // to avoid reposition when the keyboard is hiding, this guideline makes space for the keyboard as well as for the
            // AttachmentOptions, the offset is set to DEFAULT_KEYBOARD_TOP_SCREEN_OFFSET as default, whenever the keyboard pops up
            // we are able to calculate the actual needed offset, so that it is equal to the height of the keyboard the user is using
            val topOfKeyboardGuideLine = createGuidelineFromTop(
                offset = messageComposerState.fullScreenHeight - keyboardHeightOffSet.height
            )

            val messageComposer = createRef()

            ConstraintLayout(
                Modifier
                    .wrapContentSize()
                    .constrainAs(messageComposer) {
                        top.linkTo(parent.top)

                        if (messageComposerState.attachmentOptionsDisplayed) {
                            bottom.linkTo(topOfKeyboardGuideLine)
                        } else {
                            bottom.linkTo(parent.bottom)
                        }

                        height = Dimension.fillToConstraints
                    }) {

                val (additionalActions, sendActions, messageInput) = createRefs()
                // Column wrapping the content passed as Box with weight = 1f as @Composable lambda and the MessageComposerInput with
                // CollapseIconButton
                Column(
                    Modifier.constrainAs(messageInput) {
                        top.linkTo(parent.top)
                        // we want to align the elements to the guideline only when we display attachmentOptions
                        // or we are having focus on the TextInput field
                        bottom.linkTo(additionalActions.top)

                        height = Dimension.preferredWrapContent
                    }
                ) {
                    Box(
                        Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        focusManager.clearFocus()
                                        messageComposerState.clickOutSideMessageComposer()
                                    },
                                    onDoubleTap = { /* Called on Double Tap */ },
                                    onLongPress = { /* Called on Long Press */ },
                                    onTap = {  /* Called on Tap */ }
                                )
                            }
                            .background(color = MaterialTheme.wireColorScheme.backgroundVariant)
                            .weight(1f)) {
                        content()
                    }
                    // Column wrapping CollapseIconButton and MessageComposerInput
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
                                        messageComposerState.toggleAttachmentOptionsVisibility()
                                    }
                                }
                            }
                            // MessageComposerInput needs a padding on the end of it to give room for the SendOptions components,
                            // because it is "floating" freely with an absolute x-y position inside of the ConstrainLayout
                            // wrapping the whole content when in the FullScreen state we are giving it max height
                            // when in active state we limit the height to max 82.dp
                            // other we let it wrap the content of the height, which will be equivalent to the text
                            MessageComposerInput(
                                messageText = messageText,
                                onMessageTextChanged = { value ->
                                    onMessageChanged(value)
                                },
                                messageComposerInputState = messageComposerState.messageComposeInputState,
                                onIsFocused = {
                                    messageComposerState.toActive()
                                },
                                onNotFocused = {
                                    messageComposerState.hasFocus = false
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
                // Box wrapping the SendActions so that we do not include it in the animationContentSize
                // changed which is applied only for
                // MessageComposerInput and CollapsingButton
                Box(
                    Modifier.constrainAs(sendActions) {
                        bottom.linkTo(additionalActions.top)
                        end.linkTo(parent.end)
                    }
                ) {
                    Row(Modifier.padding(end = dimensions().spacing8x)) {
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
                Column(
                    Modifier
                        .constrainAs(additionalActions) {
                            top.linkTo(messageInput.bottom)
                            bottom.linkTo(parent.bottom)
                        }
                        .wrapContentSize()) {
                    Divider()
                    Box(Modifier.wrapContentSize()) {
                        transition.AnimatedVisibility(
                            visible = { messageComposerState.messageComposeInputState != MessageComposeInputState.Enabled },
                            // we are animating the exit, so that the MessageComposeActions go down
                            exit = slideOutVertically(
                                targetOffsetY = { fullHeight -> fullHeight / 2 }
                            ) + fadeOut()
                        ) {
                            MessageComposeActions(messageComposerState, focusManager)
                        }
                    }
                }
            }
            // Box wrapping for additional options content
            // we want to offset the AttachmentOptionsComponent equal to where
            // the device keyboard is displayed, so that when the keyboard is closed,
            // we get the effect of overlapping it
            if (messageComposerState.attachmentOptionsDisplayed) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(keyboardHeightOffSet.height)
                        .absoluteOffset(y = messageComposerState.fullScreenHeight - keyboardHeightOffSet.height)
                ) {
                    Divider()
                    AttachmentOptionsComponent(
                        messageComposerState.attachmentInnerState,
                        onSendAttachment,
                        onMessageComposerError,
                        isFileSharingEnabled,
                        Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

//if attachment is visible we want to align the bottom of the compose actions
// to top of the guide line
@Composable
private fun CollapseIconButton(onCollapseClick: () -> Unit, modifier: Modifier = Modifier, collapseRotation: Float = 0f) {
    IconButton(
        onClick = onCollapseClick,
        modifier = modifier.size(20.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_collapse),
            contentDescription = stringResource(R.string.content_description_drop_down_icon),
            tint = MaterialTheme.wireColorScheme.onSecondaryButtonDisabled,
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
    onIsFocused: () -> Unit,
    onNotFocused: () -> Unit,
    modifier: Modifier = Modifier
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
            Modifier.onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onIsFocused()
                } else {
                    onNotFocused()
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
private fun MessageComposeActions(
    messageComposerState: MessageComposerInnerState,
    focusManager: FocusManager
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        AdditionalOptionButton(messageComposerState.attachmentOptionsDisplayed) {
            focusManager.clearFocus()
            messageComposerState.toggleAttachmentOptionsVisibility()
        }
        RichTextEditingAction()
        AddEmojiAction()
        AddGifAction()
        AddMentionAction()
        TakePictureAction()
    }
}

@Composable
private fun AdditionalOptionButton(isSelected: Boolean = false, onClick: () -> Unit) {
    WireIconButton(
        onButtonClicked = onClick,
        iconResource = R.drawable.ic_add,
        contentDescription = R.string.content_description_conversation_search_icon,
        state = if (isSelected) WireButtonState.Selected else WireButtonState.Default,
    )
}

@Composable
private fun RichTextEditingAction() {
    WireIconButton(
        onButtonClicked = {},
        iconResource = R.drawable.ic_rich_text,
        contentDescription = R.string.content_description_conversation_search_icon
    )
}

@Composable
private fun AddEmojiAction() {
    WireIconButton(
        onButtonClicked = {},
        iconResource = R.drawable.ic_emoticon,
        contentDescription = R.string.content_description_conversation_search_icon
    )
}

@Composable
private fun AddGifAction() {
    WireIconButton(
        onButtonClicked = {},
        iconResource = R.drawable.ic_gif,
        contentDescription = R.string.content_description_conversation_search_icon
    )
}

@Composable
private fun AddMentionAction() {
    WireIconButton(
        onButtonClicked = {},
        iconResource = R.drawable.ic_mention,
        contentDescription = R.string.content_description_conversation_search_icon
    )
}

@Composable
private fun TakePictureAction() {
    WireIconButton(
        onButtonClicked = {},
        iconResource = R.drawable.ic_ping,
        contentDescription = R.string.content_description_ping_everyone
    )
}

sealed class KeyboardHeight(open val height: Dp) {
    object NotKnown : KeyboardHeight(DEFAULT_KEYBOARD_TOP_SCREEN_OFFSET)
    data class Known(override val height: Dp) : KeyboardHeight(height)
}
