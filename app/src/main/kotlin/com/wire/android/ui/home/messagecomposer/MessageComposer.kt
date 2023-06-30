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

import android.util.Log
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
import androidx.compose.runtime.LaunchedEffect
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
import com.wire.android.ui.home.messagecomposer.state.MessageComposerStateHolder
import com.wire.android.ui.home.messagecomposer.state.MessageComposition
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputSize
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputState
import com.wire.android.util.ui.KeyboardHeight
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType

@Composable
fun MessageComposer(
    messageComposerStateHolder: MessageComposerStateHolder,
    messageListContent: @Composable () -> Unit,
    onChangeSelfDeletionClicked: () -> Unit,
    onSearchMentionQueryChanged: (String) -> Unit,
    onClearMentionSearchResult: () -> Unit,
) {
    with(messageComposerStateHolder) {
        when (messageComposerViewState.value.interactionAvailability) {
            InteractionAvailability.BLOCKED_USER -> BlockedUserComposerInput(
                securityClassificationType = messageComposerViewState.value.securityClassificationType
            )

            InteractionAvailability.DELETED_USER -> DeletedUserComposerInput(
                securityClassificationType = messageComposerViewState.value.securityClassificationType
            )

            InteractionAvailability.NOT_MEMBER, InteractionAvailability.DISABLED ->
                MessageComposerClassifiedBanner(
                    securityClassificationType = messageComposerViewState.value.securityClassificationType,
                    paddingValues = PaddingValues(vertical = dimensions().spacing16x)
                )

            InteractionAvailability.ENABLED -> {
                EnabledMessageComposer(
                    messageComposerStateHolder = messageComposerStateHolder,
                    messageListContent = messageListContent,
                    onSendButtonClicked = {},
                    onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
                    onSearchMentionQueryChanged = onSearchMentionQueryChanged,
                    onClearMentionSearchResult = onClearMentionSearchResult
                )
            }
        }
    }
}

@Composable
private fun EnabledMessageComposer(
    messageComposerStateHolder: MessageComposerStateHolder,
    messageListContent: @Composable () -> Unit,
    onSendButtonClicked: () -> Unit,
    onChangeSelfDeletionClicked: () -> Unit,
    onSearchMentionQueryChanged: (String) -> Unit,
    onClearMentionSearchResult: () -> Unit
) {
    with(messageComposerStateHolder) {
        Row {
            val isClassifiedConversation = messageComposerViewState.value.securityClassificationType != SecurityClassificationType.NONE
            if (isClassifiedConversation) {
                Box(Modifier.wrapContentSize()) {
                    VerticalSpace.x8()
                    SecurityClassificationBanner(securityClassificationType = SecurityClassificationType.NONE)
                }
            }
            when (messageCompositionInputStateHolder.inputState) {
                MessageCompositionInputState.ACTIVE -> {
                    ActiveMessageComposer(
                        messageComposerStateHolder = messageComposerStateHolder,
                        messageListContent = messageListContent,
                        onTransitionToInActive = messageComposerStateHolder::toInActive,
                        onSendButtonClicked = onSendButtonClicked,
                        onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
                        onSearchMentionQueryChanged = onSearchMentionQueryChanged,
                        onClearMentionSearchResult = onClearMentionSearchResult
                    )
                }

                MessageCompositionInputState.INACTIVE -> {
                    InactiveMessageComposer(
                        messageComposition = messageComposerStateHolder.messageComposition.value,
                        isFileSharingEnabled = messageComposerViewState.value.isFileSharingEnabled,
                        messageListContent = messageListContent,
                        onTransitionToActive = messageComposerStateHolder::toActive
                    )
                }
            }
        }
    }
}

@Composable
private fun InactiveMessageComposer(
    messageComposition: MessageComposition,
    isFileSharingEnabled: Boolean,
    messageListContent: @Composable () -> Unit,
    onTransitionToActive: (Boolean) -> Unit
) {
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
                        isSelected = false,
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

@Composable
private fun ActiveMessageComposer(
    messageComposerStateHolder: MessageComposerStateHolder,
    messageListContent: @Composable () -> Unit,
    onTransitionToInActive: () -> Unit,
    onChangeSelfDeletionClicked: () -> Unit,
    onSearchMentionQueryChanged: (String) -> Unit,
    onSendButtonClicked: () -> Unit,
    onClearMentionSearchResult: () -> Unit
) {
    Log.d("TEST", "recomposing active message composer")
    with(messageComposerStateHolder) {
        Surface(color = colorsScheme().messageComposerBackgroundColor) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val currentScreenHeight: Dp = with(LocalDensity.current) { constraints.maxHeight.toDp() }

                // when MessageComposer is composed for the first time we do not know the height until users opens the keyboard
                var keyboardHeight: KeyboardHeight by remember { mutableStateOf(KeyboardHeight.NotKnown) }

                val isKeyboardVisible = KeyboardHelper.isKeyboardVisible()
                if (isKeyboardVisible) {
                    val calculatedKeyboardHeight = KeyboardHelper.getCalculatedKeyboardHeight()
                    Log.d("TEST", "calculating keyboard height: $calculatedKeyboardHeight")
                    val notKnownAndCalculated =
                        keyboardHeight is KeyboardHeight.NotKnown && calculatedKeyboardHeight > 0.dp
                    val knownAndDifferent =
                        keyboardHeight is KeyboardHeight.Known && keyboardHeight.height != calculatedKeyboardHeight
                    if (notKnownAndCalculated || knownAndDifferent) {
                        Log.d("TEST", "keyboard height is known: $keyboardHeight")
                        keyboardHeight = KeyboardHeight.Known(calculatedKeyboardHeight)
                    }
                }

                LaunchedEffect(isKeyboardVisible) {
                    Log.d("TEST", "keyboard height: $keyboardHeight")
                    messageComposerStateHolder.onKeyboardVisibilityChanged(isKeyboardVisible)
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
                            if (messageComposerViewState.value.mentionSearchResult.isNotEmpty()) {
                                MembersMentionList(
                                    membersToMention = messageComposerViewState.value.mentionSearchResult,
                                    onMentionPicked = { pickedMention ->
                                        messageCompositionHolder.addMention(pickedMention)
                                    }
                                )
                            }
                        }
                        Column(
                            Modifier.wrapContentSize()
                        ) {
                            val fillRemainingSpaceOrWrapContent =
                                if (messageCompositionInputStateHolder.inputSize == MessageCompositionInputSize.COLLAPSED) {
                                    Modifier.wrapContentHeight()
                                } else {
                                    Modifier.weight(1f)
                                }

                            Column {
                                val isClassifiedConversation =
                                    messageComposerViewState.value.securityClassificationType != SecurityClassificationType.NONE
                                if (isClassifiedConversation) {
                                    Box(Modifier.wrapContentSize()) {
                                        VerticalSpace.x8()
                                        SecurityClassificationBanner(securityClassificationType = messageComposerViewState.value.securityClassificationType)
                                    }
                                }
                                Box {
                                    var currentSelectedLineIndex by remember { mutableStateOf(0) }
                                    var cursorCoordinateY by remember { mutableStateOf(0F) }

                                    ActiveMessageComposerInput(
                                        messageComposition = messageComposition.value,
                                        inputSize = messageCompositionInputStateHolder.inputSize,
                                        inputType = messageCompositionInputStateHolder.inputType,
                                        inputFocused = messageCompositionInputStateHolder.inputFocused,
                                        onInputFocusedChanged = ::onInputFocusedChanged,
                                        onToggleInputSize = messageCompositionInputStateHolder::toggleInputSize,
                                        onCancelReply = messageCompositionHolder::clearReply,
                                        onMessageTextChanged = {
                                            messageCompositionHolder.setMessageText(
                                                messageTextFieldValue = it,
                                                onSearchMentionQueryChanged = onSearchMentionQueryChanged,
                                                onClearMentionSearchResult = onClearMentionSearchResult
                                            )
                                        },
                                        onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
                                        onSendButtonClicked = onSendButtonClicked,
                                        onLineBottomYCoordinateChanged = { yCoordinate ->
                                            cursorCoordinateY = yCoordinate
                                        },
                                        onSelectedLineIndexChanged = { index ->
                                            currentSelectedLineIndex = index
                                        },
                                        modifier = fillRemainingSpaceOrWrapContent,
                                    )

                                    val mentionSearchResult = messageComposerViewState.value.mentionSearchResult
                                    if (mentionSearchResult.isNotEmpty() &&
                                        messageCompositionInputStateHolder.inputSize == MessageCompositionInputSize.EXPANDED
                                    ) {
                                        DropDownMentionsSuggestions(
                                            currentSelectedLineIndex = currentSelectedLineIndex,
                                            cursorCoordinateY = cursorCoordinateY,
                                            membersToMention = mentionSearchResult,
                                            onMentionPicked = messageCompositionHolder::addMention
                                        )
                                    }
                                }
                            }
                            AdditionalOptionsMenu(
                                isFileSharingEnabled = messageComposerViewState.value.isFileSharingEnabled,
                                onOnSelfDeletingOptionClicked = onChangeSelfDeletionClicked,
                                onMentionButtonClicked = messageCompositionHolder::startMention,
                                onRichOptionButtonClicked = messageCompositionHolder::addOrRemoveMessageMarkdown,
                                onAdditionalOptionsMenuClicked = ::showAdditionalOptionsMenu,
                            )
                        }
                    }

                    if (additionalOptionSubMenuVisible) {
                        AdditionalOptionSubMenu(
                            isFileSharingEnabled = messageComposerViewState.value.isFileSharingEnabled,
                            additionalOptionsState = additionalOptionStateHolder.additionalOptionsSubMenuState,
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
                    else if (isTransitionToKeyboardOnGoing) {
                        Box(
                            modifier = Modifier
                                .height(keyboardHeight.height)
                                .fillMaxWidth()
                        )
                    }
                }
            }

            BackHandler {
                onTransitionToInActive()
            }
        }
    }
}
