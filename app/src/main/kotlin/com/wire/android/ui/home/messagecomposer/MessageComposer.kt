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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.KeyboardHelper
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldColors
import com.wire.android.ui.home.messagecomposer.attachment.AttachmentOptionsComponent
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionMenuState
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSubMenuState
import com.wire.android.ui.home.messagecomposer.state.MessageComposerState
import com.wire.android.ui.home.messagecomposer.state.MessageComposerStateHolder
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputSize
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputState
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputType
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.KeyboardHeight

@Composable
fun MessageComposer(
    messageComposerStateHolder: MessageComposerStateHolder,
    messageListContent: @Composable () -> Unit
) {
    when (val state = messageComposerStateHolder.messageComposerState) {
        is MessageComposerState.Active -> {
            ActiveMessageComposer(
                activeMessageComposerState = state,
                messageListContent = messageListContent,
                onTransitionToInActive = messageComposerStateHolder::toInActive
            )
        }

        is MessageComposerState.InActive -> {
            InActiveMessageComposer(
                inActiveComposerState = state,
                messageListContent = messageListContent,
                onTransistionToActive = messageComposerStateHolder::toComposing
            )
        }

        is MessageComposerState.AudioRecording -> {
            AudioRecordingComposer(
                onCloseAudioRecordingClicked = messageComposerStateHolder::toComposing
            )
        }
    }
}

// TODO:: simple audio recorder place holder for later
@Composable
fun AudioRecordingComposer(onCloseAudioRecordingClicked: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(Color.Red)
    ) {

    }
}

@Composable
private fun ActiveMessageComposer(
    activeMessageComposerState: MessageComposerState.Active,
    messageListContent: @Composable () -> Unit,
    onTransitionToInActive: () -> Unit
) {
    with(activeMessageComposerState) {
        Surface(color = colorsScheme().messageComposerBackgroundColor) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val currentScreenHeight: Dp = with(LocalDensity.current) { constraints.maxHeight.toDp() }

                // when MessageComposer is composed for the first time we do not know the height until users opens the keyboard
                var keyboardHeight: KeyboardHeight by remember { mutableStateOf(KeyboardHeight.NotKnown) }

                if (KeyboardHelper.isKeyboardVisible()) {
                    val calculatedKeyboardHeight = KeyboardHelper.getCalculatedKeyboardHeight()
                    val notKnownAndCalculated =
                        keyboardHeight is KeyboardHeight.NotKnown && calculatedKeyboardHeight > 0.dp
                    val knownAndDifferent =
                        keyboardHeight is KeyboardHeight.Known && keyboardHeight.height != calculatedKeyboardHeight
                    if (notKnownAndCalculated || knownAndDifferent) {
                        keyboardHeight = KeyboardHeight.Known(calculatedKeyboardHeight)
                    }
                }

                val makeTheContentAsBigAsScreenHeightWithoutKeyboard = Modifier
                    .fillMaxWidth()
                    .height(currentScreenHeight)

                Column(
                    makeTheContentAsBigAsScreenHeightWithoutKeyboard
                ) {
                    val fillRemainingSpaceBetweenThisAndAdditionalSubMenu = Modifier
                        .weight(1f)
                        .fillMaxWidth()

                    Column(fillRemainingSpaceBetweenThisAndAdditionalSubMenu) {
                        Box(
                            Modifier
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = { onTransitionToInActive() },
                                        onDoubleTap = { /* Called on Double Tap */ },
                                        onLongPress = { /* Called on Long Press */ },
                                        onTap = { /* Called on Tap */ }
                                    )
                                }
                                .background(color = colorsScheme().backgroundVariant)
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            messageListContent()
                        }
                        Column(
                            Modifier.wrapContentSize()
                        ) {
                            val fillRemainingSpaceOrWrapContent =
                                if (messageCompositionInputState.inputSize == MessageCompositionInputSize.COLLAPSED)
                                    Modifier.wrapContentHeight()
                                else Modifier.weight(1f)

                            MessageComposerInput(
                                messageCompositionInputState = messageCompositionInputState,
                                onMessageTextChanged = ::messageTextChanged,
                                onSendButtonClicked = { },
                                onFocused = ::onInputFocused,
                                modifier = fillRemainingSpaceOrWrapContent
                            )
                            AdditionalOptionsMenu(
                                onEphemeralOptionItemClicked = ::toEphemeralInputType,
                                onAttachmentOptionClicked = ::toggleAttachmentOptions,
                                onGifButtonClicked = ::toggleGifMenu,
                                onPingClicked = { },
                            )
                        }
                    }
                    val additionalOptionSubMenuVisible =
                        additionalOptionsSubMenuState != AdditionalOptionSubMenuState.Hidden
                                && !KeyboardHelper.isKeyboardVisible()

                    val isTransitionToOpenKeyboardOngoing =
                        additionalOptionsSubMenuState == AdditionalOptionSubMenuState.Hidden
                                && !KeyboardHelper.isKeyboardVisible()

                    if (additionalOptionSubMenuVisible) {
                        AdditionalOptionSubMenu(
                            additionalOptionsState = additionalOptionsSubMenuState,
                            modifier = Modifier
                                .height(keyboardHeight.height)
                                .fillMaxWidth()
                                .background(
                                    colorsScheme().messageComposerBackgroundColor
                                )
                        )
                    }
                    // This covers the situation when the user switches from attachment options to the input keyboard - there is a moment when
                    // both attachmentOptionsDisplayed and isKeyboardVisible are false, but right after that keyboard shows, so if we know that
                    // the input already has a focus, we can show an empty Box which has a height of the keyboard to prevent flickering.
                    else if (isTransitionToOpenKeyboardOngoing) {
                        Box(
                            modifier = Modifier
                                .height(keyboardHeight.height)
                                .fillMaxWidth()
                        )
                    }
                }
            }
            BackHandler(additionalOptionsSubMenuState != AdditionalOptionSubMenuState.Hidden) {
                onTransitionToInActive()
            }
        }
    }
}

@Composable
private fun AdditionalOptionsMenu(
    onEphemeralOptionItemClicked: () -> Unit,
    onAttachmentOptionClicked: () -> Unit,
    onGifButtonClicked: () -> Unit,
    onPingClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var additionalOptionState: AdditionalOptionMenuState by remember { mutableStateOf(AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu) }

    Box(modifier) {
        when (additionalOptionState) {
            is AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu -> {
                AttachmentAndAdditionalOptionsMenuItems(
                    isMentionActive = true,
                    isFileSharingEnabled = true,
                    onMentionButtonClicked = onEphemeralOptionItemClicked,
                    onAttachmentOptionClicked = onAttachmentOptionClicked,
                    onGifButtonClicked = onGifButtonClicked,
                    onSelfDeletionOptionButtonClicked = onEphemeralOptionItemClicked,
                    onRichEditingButtonClicked = { additionalOptionState = AdditionalOptionMenuState.RichTextEditing },
                    onPingClicked = onPingClicked,
                    showSelfDeletingOption = true,
                    modifier = Modifier.background(Color.Black)
                )
            }

            is AdditionalOptionMenuState.RichTextEditing -> {
                RichTextOptions(
                    onRichTextHeaderButtonClicked = {},
                    onRichTextBoldButtonClicked = {},
                    onRichTextItalicButtonClicked = {},
                    onCloseRichTextEditingButtonClicked = {
                        additionalOptionState = AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu
                    }
                )
            }
        }
    }
}

@Composable
private fun AdditionalOptionSubMenu(
    additionalOptionsState: AdditionalOptionSubMenuState,
    modifier: Modifier
) {
    when (additionalOptionsState) {
        AdditionalOptionSubMenuState.AttachFile -> {
            AttachmentOptionsComponent(
                onAttachmentPicked = {},
                tempWritableImageUri = null,
                tempWritableVideoUri = null,
                isFileSharingEnabled = true,
                modifier = modifier
            )
        }

        AdditionalOptionSubMenuState.Emoji -> {}
        AdditionalOptionSubMenuState.Gif -> {}
        AdditionalOptionSubMenuState.RecordAudio -> {}
        AdditionalOptionSubMenuState.AttachImage -> {}
        AdditionalOptionSubMenuState.Hidden -> {}
    }
}

@Composable
private fun AttachmentAndAdditionalOptionsMenuItems(
    isMentionActive: Boolean,
    isFileSharingEnabled: Boolean,
    onMentionButtonClicked: () -> Unit,
    onAttachmentOptionClicked: () -> Unit = {},
    onPingClicked: () -> Unit = {},
    onSelfDeletionOptionButtonClicked: () -> Unit,
    showSelfDeletingOption: Boolean,
    onGifButtonClicked: () -> Unit = {},
    onRichEditingButtonClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier.wrapContentSize()) {
        Divider(color = MaterialTheme.wireColorScheme.outline)
        MessageComposeActions(
            false,
            isMentionActive,
            false,
            isEditMessage = false,
            isFileSharingEnabled,
            onMentionButtonClicked = onMentionButtonClicked,
            onAdditionalOptionButtonClicked = onAttachmentOptionClicked,
            onPingButtonClicked = onPingClicked,
            onSelfDeletionOptionButtonClicked = onSelfDeletionOptionButtonClicked,
            showSelfDeletingOption = showSelfDeletingOption,
            onGifButtonClicked = onGifButtonClicked,
            onRichEditingButtonClicked = onRichEditingButtonClicked
        )
    }
}

@Composable
private fun MessageComposerInput(
    messageCompositionInputState: MessageCompositionInputState,
    onMessageTextChanged: (TextFieldValue) -> Unit,
    onSendButtonClicked: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(messageCompositionInputState.inputFocused) {
        if (messageCompositionInputState.inputFocused) focusRequester.requestFocus()
        else focusManager.clearFocus()
    }

    with(messageCompositionInputState) {
        Column(
            modifier = modifier
        ) {
            CollapseButton(
                onCollapseClick = {
                    inputSize = if (inputSize == MessageCompositionInputSize.COLLAPSED) MessageCompositionInputSize.EXPANDED
                    else MessageCompositionInputSize.COLLAPSED
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalAlignment = Alignment.Bottom
            ) {
                val stretchToMaxParentConstraintHeightOrWithInBoundary = when (inputSize) {
                    MessageCompositionInputSize.COLLAPSED -> Modifier.heightIn(max = dimensions().messageComposerActiveInputMaxHeight)
                    MessageCompositionInputSize.EXPANDED -> Modifier.fillMaxHeight()
                }.weight(1f)

                MessageComposerTextInput(
                    colors = type.inputTextColor(),
                    messageText = type.messageCompositionState.value.textFieldValue,
                    onMessageTextChanged = onMessageTextChanged,
                    singleLine = false,
                    onFocusChanged = { isFocused ->
                        if (isFocused) onFocused()
                    },
                    focusRequester = focusRequester,
                    modifier = stretchToMaxParentConstraintHeightOrWithInBoundary
                )
                Row(Modifier.wrapContentSize()) {
                    when (val inputType = type) {
                        is MessageCompositionInputType.Composing -> MessageSendActions(
                            onSendButtonClicked = onSendButtonClicked,
                            sendButtonEnabled = inputType.isSendButtonEnabled
                        )

                        is MessageCompositionInputType.SelfDeleting -> SelfDeletingActions(
                            sendButtonEnabled = true,
                            onSendButtonClicked = onSendButtonClicked,
                            onChangeSelfDeletionClicked = inputType::showSelfDeletingTimeOption
                        )

                        else -> {}
                    }
                }
            }
            when (val inputType = type) {
                is MessageCompositionInputType.Editing -> {
                    MessageEditActions(
                        onEditSaveButtonClicked = { },
                        onEditCancelButtonClicked = ::toComposing,
                        editButtonEnabled = inputType.isEditButtonEnabled
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
fun MessageComposerTextInput(
    focusRequester: FocusRequester,
    colors: WireTextFieldColors,
    singleLine: Boolean,
    messageText: TextFieldValue,
    onMessageTextChanged: (TextFieldValue) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    onSelectedLineIndexChanged: (Int) -> Unit = { },
    onLineBottomYCoordinateChanged: (Float) -> Unit = { },
    modifier: Modifier = Modifier
) {
    WireTextField(
        value = messageText,
        onValueChange = onMessageTextChanged,
        colors = colors,
        singleLine = singleLine,
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

//@Suppress("ComplexMethod", "ComplexCondition")
//@Composable
//private fun MessageComposer(
//    messagesContent: @Composable () -> Unit,
//    messageComposerState: MessageComposerState,
//    isFileSharingEnabled: Boolean,
//    interactionAvailability: InteractionAvailability,
//    membersToMention: List<Contact>,
//    onAttachmentPicked: (UriAsset, Duration?) -> Unit,
//    securityClassificationType: SecurityClassificationType,
//    tempWritableImageUri: Uri?,
//    tempWritableVideoUri: Uri?,
//    onSendButtonClicked: () -> Unit,
//    onEditSaveButtonClicked: () -> Unit,
//    onMentionPicked: (Contact) -> Unit,
//    onPingClicked: () -> Unit,
//    onShowSelfDeletionOption: () -> Unit,
//    showSelfDeletingOption: Boolean
//) {
//    Surface(color = colorsScheme().messageComposerBackgroundColor) {
//        val transition = updateTransition(
//            targetState = messageComposerState.messageComposeInputState,
//            label = stringResource(R.string.animation_label_message_compose_input_state_transition)
//        )
//
//        BoxWithConstraints(Modifier.fillMaxSize()) {
//            val currentScreenHeight: Dp = with(LocalDensity.current) { constraints.maxHeight.toDp() }
//
//            Column(
//                Modifier
//                    .fillMaxWidth()
//                    .height(currentScreenHeight)
//            ) {
//
//                // when MessageComposer is composed for the first time we do not know the height until users opens the keyboard
//                var keyboardHeight: KeyboardHeight by remember { mutableStateOf(KeyboardHeight.NotKnown) }
//                val isKeyboardVisible = KeyboardHelper.isKeyboardVisible()
//                if (isKeyboardVisible) {
//                    val calculatedKeyboardHeight = KeyboardHelper.getCalculatedKeyboardHeight()
//                    val notKnownAndCalculated = keyboardHeight is KeyboardHeight.NotKnown && calculatedKeyboardHeight > 0.dp
//                    val knownAndDifferent = keyboardHeight is KeyboardHeight.Known && keyboardHeight.height != calculatedKeyboardHeight
//                    if (notKnownAndCalculated || knownAndDifferent) {
//                        keyboardHeight = KeyboardHeight.Known(calculatedKeyboardHeight)
//                    }
//                }
//                val attachmentOptionsVisible = messageComposerState.messageComposeInputState.attachmentOptionsDisplayed
//                        && !isKeyboardVisible
//                        && interactionAvailability == InteractionAvailability.ENABLED
//
//                // Whenever the user closes the keyboard manually that is not clicking outside of the input text field
//                // but for example pressing the back button when the keyboard is visible
//                LaunchedEffect(isKeyboardVisible) {
//                    if (!isKeyboardVisible && !messageComposerState.messageComposeInputState.attachmentOptionsDisplayed) {
//                        if (!messageComposerState.messageComposeInputState.isEditMessage) {
//                            messageComposerState.toInactive()
//                        }
//                        messageComposerState.focusManager.clearFocus()
//                    }
//                }
//
//                Column(
//                    Modifier
//                        .weight(1f)
//                        .fillMaxWidth()
//                ) {
//                    Box(
//                        Modifier
//                            .pointerInput(Unit) {
//                                detectTapGestures(
//                                    onPress = {
//                                        messageComposerState.focusManager.clearFocus()
//                                        messageComposerState.toInactive()
//                                    },
//                                    onDoubleTap = { /* Called on Double Tap */ },
//                                    onLongPress = { /* Called on Long Press */ },
//                                    onTap = { /* Called on Tap */ }
//                                )
//                            }
//                            .background(color = colorsScheme().backgroundVariant)
//                            .fillMaxWidth()
//                            .weight(1f)
//                    ) {
//                        messagesContent()
//                        if (membersToMention.isNotEmpty()) {
//                            MembersMentionList(
//                                membersToMention = membersToMention,
//                                onMentionPicked = onMentionPicked
//                            )
//                        }
//                    }
//
//                    MessageComposerInput(
//                        transition = transition,
//                        interactionAvailability = interactionAvailability,
//                        isFileSharingEnabled = isFileSharingEnabled,
//                        securityClassificationType = securityClassificationType,
//                        messageComposeInputState = messageComposerState.messageComposeInputState,
//                        quotedMessageData = messageComposerState.quotedMessageData,
//                        membersToMention = membersToMention,
//                        inputFocusRequester = messageComposerState.inputFocusRequester,
//                        showSelfDeletingOption = showSelfDeletingOption,
//                        actions = remember(messageComposerState) {
//                            MessageComposerInputActions(
//                                onMessageTextChanged = messageComposerState::setMessageTextValue,
//                                onMentionPicked = onMentionPicked,
//                                onSendButtonClicked = onSendButtonClicked,
//                                onToggleFullScreen = messageComposerState::toggleFullScreen,
//                                onCancelReply = messageComposerState::cancelReply,
//                                startMention = messageComposerState::startMention,
//                                onPingClicked = onPingClicked,
//                                onInputFocusChanged = { isFocused ->
//                                    messageComposerState.messageComposeInputFocusChange(isFocused)
//                                    if (isFocused) {
//                                        messageComposerState.toActive()
//                                        messageComposerState.hideAttachmentOptions()
//                                    }
//                                },
//                                onAdditionalOptionButtonClicked = {
//                                    messageComposerState.focusManager.clearFocus()
//                                    messageComposerState.toActive()
//                                    messageComposerState.showAttachmentOptions()
//                                },
//                                onEditSaveButtonClicked = onEditSaveButtonClicked,
//                                onEditCancelButtonClicked = messageComposerState::closeEditToInactive,
//                                onSelfDeletionOptionButtonClicked = onShowSelfDeletionOption
//                            )
//                        }
//                    )
//                }
//
//                // Box wrapping for additional options content
//                // we want to offset the AttachmentOptionsComponent equal to where
//                // the device keyboard is displayed, so that when the keyboard is closed,
//                // we get the effect of overlapping it
//                if (attachmentOptionsVisible) {
//                    AttachmentOptions(
//                        onAttachmentPicked = remember {
//                            {
//                                val expireAfter = (messageComposerState.messageComposeInputState as? MessageComposeInputState.Active)?.let {
//                                    (it.type as? MessageComposeInputType.SelfDeletingMessage)
//                                }?.selfDeletionDuration?.value
//                                onAttachmentPicked(it, expireAfter)
//                            }
//                        },
//                        isFileSharingEnabled = isFileSharingEnabled,
//                        tempWritableImageUri = tempWritableImageUri,
//                        tempWritableVideoUri = tempWritableVideoUri,
//                        modifier = Modifier
//                            .height(keyboardHeight.height)
//                            .fillMaxWidth()
//                            .background(colorsScheme().messageComposerBackgroundColor)
//                    )
//                }
//                // This covers the situation when the user switches from attachment options to the input keyboard - there is a moment when
//                // both attachmentOptionsDisplayed and isKeyboardVisible are false, but right after that keyboard shows, so if we know that
//                // the input already has a focus, we can show an empty Box which has a height of the keyboard to prevent flickering.
//                else if (!messageComposerState.messageComposeInputState.attachmentOptionsDisplayed && !isKeyboardVisible &&
//                    keyboardHeight is KeyboardHeight.Known && messageComposerState.messageComposeInputState.inputFocused &&
//                    interactionAvailability == InteractionAvailability.ENABLED
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .height(keyboardHeight.height)
//                            .fillMaxWidth()
//                    )
//                }
//            }
//        }
//    }
//
//    BackHandler(messageComposerState.messageComposeInputState.attachmentOptionsDisplayed) {
//        messageComposerState.hideAttachmentOptions()
//        messageComposerState.toInactive()
//    }
//}
//
//@Composable
//private fun MembersMentionList(
//    membersToMention: List<Contact>,
//    onMentionPicked: (Contact) -> Unit
//) {
//    Column(
//        modifier = Modifier.fillMaxHeight(),
//        verticalArrangement = Arrangement.Bottom
//    ) {
//        if (membersToMention.isNotEmpty()) Divider()
//        LazyColumn(
//            modifier = Modifier.background(colorsScheme().background),
//            reverseLayout = true
//        ) {
//            membersToMention.forEach {
//                if (it.membership != Membership.Service) {
//                    item {
//                        MemberItemToMention(
//                            avatarData = it.avatarData,
//                            name = it.name,
//                            label = it.label,
//                            membership = it.membership,
//                            clickable = Clickable(enabled = true) { onMentionPicked(it) },
//                            modifier = Modifier
//                        )
//                        Divider(
//                            color = MaterialTheme.wireColorScheme.divider,
//                            thickness = Dp.Hairline
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
