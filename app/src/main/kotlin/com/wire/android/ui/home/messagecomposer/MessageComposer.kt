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
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages
import com.wire.android.ui.home.conversations.mention.MemberItemToMention
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.messagecomposer.attachment.AttachmentOptions
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.wireColorScheme
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import okio.Path

@Composable
fun MessageComposer(
    messageComposerState: MessageComposerInnerState,
    messageContent: @Composable () -> Unit,
    onSendTextMessage: (String, List<UiMention>, messageId: String?) -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onMentionMember: (String?) -> Unit,
    onMessageComposerError: (ConversationSnackbarMessages) -> Unit,
    isFileSharingEnabled: Boolean,
    interactionAvailability: InteractionAvailability,
    tempCachePath: Path,
    securityClassificationType: SecurityClassificationType,
    membersToMention: List<Contact>
) {
    BoxWithConstraints {
        val onSendButtonClicked = remember {
            {
                onSendTextMessage(
                    messageComposerState.messageComposeInputState.messageText.text,
                    messageComposerState.mentions,
                    messageComposerState.quotedMessageData?.messageId,
                )
                messageComposerState.quotedMessageData = null
                messageComposerState.setMessageTextValue(TextFieldValue(""))
            }
        }

        val onSendAttachmentClicked = remember {
            { attachmentBundle: AttachmentBundle? ->
                onSendAttachment(attachmentBundle)
                messageComposerState.hideAttachmentOptions()
            }
        }

        val onMentionPicked = remember {
            { contact: Contact ->
                messageComposerState.addMention(contact)
            }
        }

        LaunchedEffect(Unit) {
            messageComposerState.mentionQueryFlowState
                .collect { onMentionMember(it) }
        }

        MessageComposer(
            messagesContent = messageContent,
            messageComposerState = messageComposerState,
            isFileSharingEnabled = isFileSharingEnabled,
            tempCachePath = tempCachePath,
            interactionAvailability = interactionAvailability,
            membersToMention = membersToMention,
            onMessageComposerError = onMessageComposerError,
            onSendAttachmentClicked = onSendAttachmentClicked,
            securityClassificationType = securityClassificationType,
            onSendButtonClicked = onSendButtonClicked,
            onMentionPicked = onMentionPicked,
        )
    }
}

@Suppress("ComplexMethod", "ComplexCondition")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageComposer(
    messagesContent: @Composable () -> Unit,
    messageComposerState: MessageComposerInnerState,
    isFileSharingEnabled: Boolean,
    tempCachePath: Path,
    interactionAvailability: InteractionAvailability,
    membersToMention: List<Contact>,
    onMessageComposerError: (ConversationSnackbarMessages) -> Unit,
    onSendAttachmentClicked: (AttachmentBundle?) -> Unit,
    securityClassificationType: SecurityClassificationType,
    onSendButtonClicked: () -> Unit,
    onMentionPicked: (Contact) -> Unit,
) {
    Surface(color = colorsScheme().messageComposerBackgroundColor) {
        val transition = updateTransition(
            targetState = messageComposerState.messageComposeInputState,
            label = stringResource(R.string.animation_label_message_compose_input_state_transition)
        )

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val currentScreenHeight: Dp = with(LocalDensity.current) { constraints.maxHeight.toDp() }

            Column(Modifier.fillMaxWidth().height(currentScreenHeight)) {

                // when MessageComposer is composed for the first time we do not know the height until users opens the keyboard
                var keyboardHeight: KeyboardHeight by remember { mutableStateOf(KeyboardHeight.NotKnown) }
                val isKeyboardVisible = WindowInsets.isImeVisible
                if (isKeyboardVisible) {
                    // ime covers also the navigation bar so we need to subtract navigation bars height
                    val calculatedImeHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                    val calculatedNavBarHeight = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
                    val calculatedKeyboardHeight = calculatedImeHeight - calculatedNavBarHeight
                    val notKnownAndCalculated = keyboardHeight is KeyboardHeight.NotKnown && calculatedKeyboardHeight > 0.dp
                    val knownAndDifferent = keyboardHeight is KeyboardHeight.Known && keyboardHeight.height != calculatedKeyboardHeight
                    if (notKnownAndCalculated || knownAndDifferent) {
                        keyboardHeight = KeyboardHeight.Known(calculatedKeyboardHeight)
                    }
                }
                val attachmentOptionsVisible = messageComposerState.messageComposeInputState.attachmentOptionsDisplayed
                        && !isKeyboardVisible
                        && interactionAvailability == InteractionAvailability.ENABLED

                LaunchedEffect(isKeyboardVisible) {
                    if (!isKeyboardVisible && !messageComposerState.messageComposeInputState.attachmentOptionsDisplayed) {
                        messageComposerState.toInactive()
                        messageComposerState.focusManager.clearFocus()
                    }
                }

                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Box(
                        Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        messageComposerState.focusManager.clearFocus()
                                        messageComposerState.toInactive()
                                    },
                                    onDoubleTap = { /* Called on Double Tap */ },
                                    onLongPress = { /* Called on Long Press */ },
                                    onTap = { /* Called on Tap */ }
                                )
                            }
                            .background(color = colorsScheme().backgroundVariant)
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        messagesContent()
                        if (membersToMention.isNotEmpty()) {
                            MembersMentionList(
                                membersToMention = membersToMention,
                                onMentionPicked = onMentionPicked
                            )
                        }
                    }

                    MessageComposerInput(
                        transition = transition,
                        interactionAvailability = interactionAvailability,
                        securityClassificationType = securityClassificationType,
                        messageComposeInputState = messageComposerState.messageComposeInputState,
                        quotedMessageData = messageComposerState.quotedMessageData,
                        membersToMention = membersToMention,
                        actions = remember(messageComposerState) {
                            MessageComposerInputActions(
                                onMessageTextChanged = messageComposerState::setMessageTextValue,
                                onMentionPicked = onMentionPicked,
                                onSendButtonClicked = onSendButtonClicked,
                                onToggleFullScreen = messageComposerState::toggleFullScreen,
                                onCancelReply = messageComposerState::cancelReply,
                                startMention = messageComposerState::startMention,
                                onInputFocusChanged = { isFocused ->
                                    messageComposerState.messageComposeInputFocusChange(isFocused)
                                    if (isFocused) {
                                        messageComposerState.toActive()
                                        messageComposerState.hideAttachmentOptions()
                                    }
                                },
                                onAdditionalOptionButtonClicked = {
                                    messageComposerState.focusManager.clearFocus()
                                    messageComposerState.toActive()
                                    messageComposerState.showAttachmentOptions()
                                },
                                onEditSaveButtonClicked = { /* TODO */ },
                                onEditCancelButtonClicked = messageComposerState::toInactive
                            )
                        }
                    )
                }

                // Box wrapping for additional options content
                // we want to offset the AttachmentOptionsComponent equal to where
                // the device keyboard is displayed, so that when the keyboard is closed,
                // we get the effect of overlapping it
                if (attachmentOptionsVisible) {
                    AttachmentOptions(
                        attachmentInnerState = messageComposerState.attachmentInnerState,
                        onSendAttachment = onSendAttachmentClicked,
                        onMessageComposerError = onMessageComposerError,
                        isFileSharingEnabled = isFileSharingEnabled,
                        tempCachePath = tempCachePath,
                        modifier = Modifier
                            .height(keyboardHeight.height)
                            .fillMaxWidth()
                            .background(colorsScheme().messageComposerBackgroundColor)
                    )
                }
                // This covers the situation when the user switches from attachment options to the input keyboard - there is a moment when
                // both attachmentOptionsDisplayed and isKeyboardVisible are false, but right after that keyboard shows, so if we know that
                // the input already has a focus, we can show an empty Box which has a height of the keyboard to prevent flickering.
                else if (!messageComposerState.messageComposeInputState.attachmentOptionsDisplayed && !isKeyboardVisible &&
                    keyboardHeight is KeyboardHeight.Known && messageComposerState.messageComposeInputState.inputFocused &&
                    interactionAvailability == InteractionAvailability.ENABLED
                ) {
                    Box(
                        modifier = Modifier
                            .height(keyboardHeight.height)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

    BackHandler(messageComposerState.messageComposeInputState.attachmentOptionsDisplayed) {
        messageComposerState.hideAttachmentOptions()
        messageComposerState.toInactive()
    }
}

@Composable
private fun MembersMentionList(
    membersToMention: List<Contact>,
    onMentionPicked: (Contact) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Bottom
    ) {
        if (membersToMention.isNotEmpty()) Divider()
        LazyColumn(
            modifier = Modifier.background(colorsScheme().background),
            reverseLayout = true
        ) {
            membersToMention.forEach {
                if (it.membership != Membership.Service) {
                    item {
                        MemberItemToMention(
                            avatarData = it.avatarData,
                            name = it.name,
                            label = it.label,
                            membership = it.membership,
                            clickable = Clickable(enabled = true) { onMentionPicked(it) },
                            modifier = Modifier
                        )
                        Divider(
                            color = MaterialTheme.wireColorScheme.divider,
                            thickness = Dp.Hairline
                        )
                    }
                }
            }
        }
    }
}

sealed class KeyboardHeight(open val height: Dp) {
    object NotKnown : KeyboardHeight(DEFAULT_KEYBOARD_TOP_SCREEN_OFFSET)
    data class Known(override val height: Dp) : KeyboardHeight(height)

    companion object {
        val DEFAULT_KEYBOARD_TOP_SCREEN_OFFSET = 250.dp
    }
}
