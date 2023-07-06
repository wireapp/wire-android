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

import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.KeyboardHelper
import com.wire.android.ui.common.SecurityClassificationBanner
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversations.MessageComposerViewState
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionStateHolder
import com.wire.android.ui.home.messagecomposer.state.ComposableMessageBundle.AttachmentPickedBundle
import com.wire.android.ui.home.messagecomposer.state.MessageBundle
import com.wire.android.ui.home.messagecomposer.state.MessageComposerStateHolder
import com.wire.android.ui.home.messagecomposer.state.MessageComposition
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionHolder
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputSize
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputState
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputStateHolder
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionType
import com.wire.android.ui.home.messagecomposer.state.Ping
import com.wire.android.util.ui.KeyboardHeight
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import kotlin.time.Duration

@Composable
fun MessageComposer(
    messageComposerStateHolder: MessageComposerStateHolder,
    messageListContent: @Composable () -> Unit,
    onSendMessageBundle: (MessageBundle) -> Unit,
    onChangeSelfDeletionClicked: () -> Unit,
    onSearchMentionQueryChanged: (String) -> Unit,
    onClearMentionSearchResult: () -> Unit,
    tempWritableVideoUri: Uri?,
    tempWritableImageUri: Uri?
) {
    with(messageComposerStateHolder) {
        val interActionAvailability = messageComposerViewState.value.interactionAvailability
        val securityClassificationType = messageComposerViewState.value.securityClassificationType

        when (interActionAvailability) {
            InteractionAvailability.BLOCKED_USER -> BlockedUserComposerInput(
                securityClassificationType = securityClassificationType
            )

            InteractionAvailability.DELETED_USER -> DeletedUserComposerInput(
                securityClassificationType = securityClassificationType
            )

            InteractionAvailability.NOT_MEMBER, InteractionAvailability.DISABLED ->
                MessageComposerClassifiedBanner(
                    securityClassificationType = securityClassificationType,
                    paddingValues = PaddingValues(vertical = dimensions().spacing16x)
                )

            InteractionAvailability.ENABLED -> {
                EnabledMessageComposer(
                    messageComposerStateHolder = messageComposerStateHolder,
                    messageListContent = messageListContent,
                    onSendButtonClicked = {
                        onSendMessageBundle(messageCompositionHolder.toMessageBundle())
                        onMessageSend()
                    },
                    onPingOptionClicked = {
                        onSendMessageBundle(Ping)
                    },
                    onAttachmentPicked = {
                        onSendMessageBundle(AttachmentPickedBundle(it))
                    },
                    onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
                    onSearchMentionQueryChanged = onSearchMentionQueryChanged,
                    onClearMentionSearchResult = onClearMentionSearchResult,
                    tempWritableVideoUri = tempWritableVideoUri,
                    tempWritableImageUri = tempWritableImageUri
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
    onPingOptionClicked: () -> Unit,
    onAttachmentPicked: (UriAsset) -> Unit,
    onChangeSelfDeletionClicked: () -> Unit,
    onSearchMentionQueryChanged: (String) -> Unit,
    onClearMentionSearchResult: () -> Unit,
    tempWritableVideoUri: Uri?,
    tempWritableImageUri: Uri?,
) {
    with(messageComposerStateHolder) {
        Row {
            val securityClassificationType = messageComposerViewState.value.securityClassificationType
            if (securityClassificationType != SecurityClassificationType.NONE) {
                Box(Modifier.wrapContentSize()) {
                    VerticalSpace.x8()
                    SecurityClassificationBanner(securityClassificationType)
                }
            }
            when (messageCompositionInputStateHolder.inputState) {
                MessageCompositionInputState.ACTIVE -> {
                    ActiveMessageComposer(
                        messageComposerStateHolder = messageComposerStateHolder,
                        tempWritableVideoUri = tempWritableVideoUri,
                        tempWritableImageUri = tempWritableImageUri,
                        messageListContent = messageListContent,
                        onTransitionToInActive = messageComposerStateHolder::toInActive,
                        onSendButtonClicked = onSendButtonClicked,
                        onAttachmentPicked = onAttachmentPicked,
                        onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
                        onSearchMentionQueryChanged = onSearchMentionQueryChanged,
                        onClearMentionSearchResult = onClearMentionSearchResult,
                        onPingOptionClicked = onPingOptionClicked,
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
                            onPress = { onTransitionToActive(false) }
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
                InactiveMessageComposerInput(
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
    onAttachmentPicked: (UriAsset) -> Unit,
    onPingOptionClicked: () -> Unit,
    onClearMentionSearchResult: () -> Unit,
    tempWritableVideoUri: Uri?,
    tempWritableImageUri: Uri?
) {
    with(messageComposerStateHolder) {
        Surface(color = colorsScheme().messageComposerBackgroundColor) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val currentScreenHeight: Dp = with(LocalDensity.current) { constraints.maxHeight.toDp() }

                // when MessageComposer is composed for the first time we do not know the height until users opens the keyboard
                var keyboardHeight: KeyboardHeight by remember { mutableStateOf(KeyboardHeight.NotKnown) }

                val isKeyboardVisible = KeyboardHelper.isKeyboardVisible()
                if (isKeyboardVisible) {
                    val calculatedKeyboardHeight = KeyboardHelper.getCalculatedKeyboardHeight()
                    val notKnownAndCalculated =
                        keyboardHeight is KeyboardHeight.NotKnown && calculatedKeyboardHeight > 0.dp
                    val knownAndDifferent =
                        keyboardHeight is KeyboardHeight.Known && keyboardHeight.height != calculatedKeyboardHeight
                    if (notKnownAndCalculated || knownAndDifferent) {
                        keyboardHeight = KeyboardHeight.Known(calculatedKeyboardHeight)
                    }
                }

                LaunchedEffect(isKeyboardVisible) {
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
                                        onClearMentionSearchResult()
                                    }
                                )
                            }
                        }

                        val fillRemainingSpaceOrWrapContent =
                            if (messageCompositionInputStateHolder.inputSize == MessageCompositionInputSize.COLLAPSED) {
                                Modifier.wrapContentHeight()
                            } else {
                                Modifier.weight(1f)
                            }
                        Column(
                            Modifier.wrapContentSize()
                        ) {
                            Column {
                                val isClassifiedConversation =
                                    messageComposerViewState.value.securityClassificationType != SecurityClassificationType.NONE
                                if (isClassifiedConversation) {
                                    Box(Modifier.wrapContentSize()) {
                                        VerticalSpace.x8()
                                        SecurityClassificationBanner(securityClassificationType = messageComposerViewState.value.securityClassificationType)
                                    }
                                }

                                Box(fillRemainingSpaceOrWrapContent) {
                                    var currentSelectedLineIndex by remember { mutableStateOf(0) }
                                    var cursorCoordinateY by remember { mutableStateOf(0F) }

                                    ActiveMessageComposerInput(
                                        messageComposition = messageComposition.value,
                                        inputSize = messageCompositionInputStateHolder.inputSize,
                                        inputType = messageCompositionInputStateHolder.inputType,
                                        inputVisiblity = messageCompositionInputStateHolder.inputVisibility,
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
                                            onMentionPicked = {
                                                messageCompositionHolder.addMention(it)
                                                onClearMentionSearchResult()
                                            }
                                        )
                                    }
                                }
                                AdditionalOptionsMenu(
                                    additionalOptionsState = additionalOptionStateHolder.additionalOptionState,
                                    selectedOption = additionalOptionStateHolder.selectedOption,
                                    isEditing = messageCompositionInputStateHolder.inputType is MessageCompositionType.Editing,
                                    onRichOptionButtonClicked = messageCompositionHolder::addOrRemoveMessageMarkdown,
                                    isFileSharingEnabled = messageComposerViewState.value.isFileSharingEnabled,
                                    isSelfDeletingSettingEnabled = isSelfDeletingSettingEnabled,
                                    onMentionButtonClicked = messageCompositionHolder::startMention,
                                    onOnSelfDeletingOptionClicked = onChangeSelfDeletionClicked,
                                    onPingOptionClicked = onPingOptionClicked,
                                    onAdditionalOptionsMenuClicked = ::showAdditionalOptionsMenu,
                                    onRichEditingButtonClicked = additionalOptionStateHolder::toRichTextEditing,
                                    onCloseRichEditingButtonClicked = additionalOptionStateHolder::toAttachmentAndAdditionalOptionsMenu,
                                )
                            }
                        }
                    }

                    if (additionalOptionSubMenuVisible) {
                        AdditionalOptionSubMenu(
                            isFileSharingEnabled = messageComposerViewState.value.isFileSharingEnabled,
                            additionalOptionsState = additionalOptionStateHolder.additionalOptionsSubMenuState,
                            onRecordAudioMessageClicked = ::toAudioRecording,
                            onAttachmentPicked = onAttachmentPicked,
                            tempWritableImageUri = tempWritableImageUri,
                            tempWritableVideoUri = tempWritableVideoUri,
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


@Preview
@Composable
fun MessageComposerPreview() {
    val messageComposerViewState = remember { mutableStateOf(MessageComposerViewState()) }
    val messageComposition = remember { mutableStateOf(MessageComposition.DEFAULT) }
    val selfDeletionTimer = remember { mutableStateOf(SelfDeletionTimer.Enabled(Duration.ZERO)) }

    MessageComposer(
        messageComposerStateHolder = MessageComposerStateHolder(
            messageComposerViewState = messageComposerViewState,
            messageCompositionInputStateHolder = MessageCompositionInputStateHolder(
                messageComposition = messageComposition,
                selfDeletionTimer = selfDeletionTimer
            ),
            messageCompositionHolder = MessageCompositionHolder(
                context = LocalContext.current
            ),
            additionalOptionStateHolder = AdditionalOptionStateHolder(),
            modalBottomSheetState = WireModalSheetState(),
        ),
        messageListContent = { },
        onChangeSelfDeletionClicked = { },
        onSearchMentionQueryChanged = { },
        onClearMentionSearchResult = { },
        onSendMessageBundle = { },
        tempWritableVideoUri = null,
        tempWritableImageUri = null
    )
}
