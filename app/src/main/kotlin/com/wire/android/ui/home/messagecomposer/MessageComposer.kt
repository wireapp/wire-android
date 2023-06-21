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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.KeyboardHelper
import com.wire.android.ui.common.SecurityClassificationBanner
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSubMenuState
import com.wire.android.ui.home.messagecomposer.state.MessageComposerState
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputSize
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputState
import com.wire.android.util.ui.KeyboardHeight
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType

@Composable
fun MessageComposer(
    messageComposerState: MessageComposerState,
    messageListContent: @Composable () -> Unit,
    onSendMessage: (com.wire.android.ui.home.messagecomposer.state.SendMessageBundle) -> Unit
) {
    with(messageComposerState) {
        when (messageComposerState.interactionAvailability) {
            InteractionAvailability.BLOCKED_USER -> BlockedUserComposerInput(securityClassificationType)
            InteractionAvailability.DELETED_USER -> DeletedUserComposerInput(securityClassificationType)
            InteractionAvailability.NOT_MEMBER, InteractionAvailability.DISABLED ->
                MessageComposerClassifiedBanner(
                    securityClassificationType = securityClassificationType,
                    paddingValues = PaddingValues(vertical = dimensions().spacing16x)
                )

            InteractionAvailability.ENABLED -> {
                EnabledMessageComposer(
                    messageComposerState = messageComposerState,
                    messageListContent = messageListContent,
                    onSendButtonClicked = { onSendMessage(messageComposition.toMessageBundle()) }
                )
            }
        }
    }
}

@Composable
private fun EnabledMessageComposer(
    messageComposerState: MessageComposerState,
    messageListContent: @Composable () -> Unit,
    onSendButtonClicked: () -> Unit
) {
    with(messageComposerState) {
        Row {
            val isClassifiedConversation = securityClassificationType != SecurityClassificationType.NONE
            if (isClassifiedConversation) {
                Box(Modifier.wrapContentSize()) {
                    VerticalSpace.x8()
                    SecurityClassificationBanner(securityClassificationType = securityClassificationType)
                }
            }
            when (messageComposerState.inputState) {
                MessageCompositionInputState.ACTIVE -> {
                    ActiveMessageComposer(
                        messageComposerState = messageComposerState,
                        messageListContent = messageListContent,
                        onTransitionToInActive = messageComposerState::toInActive,
                        onSendButtonClicked = onSendButtonClicked
                    )
                }

                MessageCompositionInputState.INACTIVE -> {
                    InActiveMessageComposer(
                        messageComposerState = messageComposerState,
                        messageListContent = messageListContent,
                        onTransitionToActive = messageComposerState::toActive
                    )
                }
            }
        }
    }
}

@Composable
private fun InActiveMessageComposer(
    messageComposerState: MessageComposerState,
    messageListContent: @Composable () -> Unit,
    onTransitionToActive: (Boolean) -> Unit
) {
    with(messageComposerState) {
        Surface(color = colorsScheme().messageComposerBackgroundColor) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                val fillRemainingSpaceBetweenMessageListContentAndMessageComposer = Modifier
                    .fillMaxWidth()
                    .weight(1f)

                Box(
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { onTransitionToActive(false) },
                                onDoubleTap = { /* Called on Double Tap */ },
                                onLongPress = { /* Called on Long Press */ },
                                onTap = { /* Called on Tap */ }
                            )
                        }
                        .background(color = colorsScheme().backgroundVariant)
                        .then(fillRemainingSpaceBetweenMessageListContentAndMessageComposer)
                ) {
                    messageListContent()
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Box(modifier = Modifier.padding(start = dimensions().spacing8x)) {
                        AdditionalOptionButton(
                            isSelected = additionalOptionsSubMenuState == AdditionalOptionSubMenuState.AttachFile,
                            isEnabled = isFileSharingEnabled,
                            onClick = { onTransitionToActive(true) }
                        )
                    }

                    InActiveMessageComposerInput(
                        messageText = messageComposition.messageTextFieldValue,
                        onMessageComposerFocused = { onTransitionToActive(false) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveMessageComposer(
    messageComposerState: MessageComposerState,
    messageListContent: @Composable () -> Unit,
    onTransitionToInActive: () -> Unit,
    onSendButtonClicked: () -> Unit
) {
    with(messageComposerState) {
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
                        val fillRemainingSpaceBetweenMessageListContentAndMessageComposer = Modifier
                            .fillMaxWidth()
                            .weight(1f)

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
                                .then(fillRemainingSpaceBetweenMessageListContentAndMessageComposer)

                        ) {
                            messageListContent()
                            if (messageComposition.mentionSearchResult.isNotEmpty()) {
                                MembersMentionList(
                                    membersToMention = messageComposition.mentionSearchResult,
                                    onMentionPicked = { }
                                )
                            }
                        }
                        Column(
                            Modifier.wrapContentSize()
                        ) {
                            val fillRemainingSpaceOrWrapContent =
                                if (inputSize == MessageCompositionInputSize.COLLAPSED) {
                                    Modifier.wrapContentHeight()
                                } else {
                                    Modifier.weight(1f)
                                }

                            ActiveMessageComposerInput(
                                messageCompositionInputStateHolder = messageCompositionInputStateHolder,
                                onMessageTextChanged = ::onMessageTextChanged,
                                onSendButtonClicked = onSendButtonClicked,
                                modifier = fillRemainingSpaceOrWrapContent
                            )
                            AdditionalOptionsMenu(
                                onOnSelfDeletingOptionClicked = ::toSelfDeleting,
                                onAttachmentOptionClicked = if (messageComposerState.isFileSharingEnabled) {
                                    { toggleAttachmentOptions() }
                                } else {
                                    null
                                },
                                onGifOptionClicked = { },
                                onPingOptionClicked = { },
                            )
                        }
                    }

                    val additionalOptionSubMenuVisible =
                        additionalOptionsSubMenuState != AdditionalOptionSubMenuState.Hidden &&
                                !KeyboardHelper.isKeyboardVisible()

                    val isTransitionToOpenKeyboardOngoing =
                        additionalOptionsSubMenuState == AdditionalOptionSubMenuState.Hidden &&
                                !KeyboardHelper.isKeyboardVisible()

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
