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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.KeyboardHelper
import com.wire.android.ui.common.SecurityClassificationBanner
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.MessageComposerViewState
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionStateHolder
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSubMenuState
import com.wire.android.ui.home.messagecomposer.state.ComposableMessageBundle.AttachmentPickedBundle
import com.wire.android.ui.home.messagecomposer.state.ComposableMessageBundle.AudioMessageBundle
import com.wire.android.ui.home.messagecomposer.state.MessageBundle
import com.wire.android.ui.home.messagecomposer.state.MessageComposerStateHolder
import com.wire.android.ui.home.messagecomposer.state.MessageComposition
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionHolder
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputSize
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputState
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputStateHolder
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionType
import com.wire.android.ui.home.messagecomposer.state.Ping
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.KeyboardHeight
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import com.wire.kalium.logic.util.isPositiveNotNull
import kotlin.time.Duration

@Composable
fun MessageComposer(
    messageComposerStateHolder: MessageComposerStateHolder,
    snackbarHostState: SnackbarHostState,
    messageListContent: @Composable () -> Unit,
    onSendMessageBundle: (MessageBundle) -> Unit,
    onChangeSelfDeletionClicked: () -> Unit,
    onSearchMentionQueryChanged: (String) -> Unit,
    onClearMentionSearchResult: () -> Unit,
    tempWritableVideoUri: Uri?,
    tempWritableImageUri: Uri?
) {
    with(messageComposerStateHolder) {
        val securityClassificationType = messageComposerViewState.value.securityClassificationType

        when (messageComposerViewState.value.interactionAvailability) {
            InteractionAvailability.BLOCKED_USER -> {
                DisabledInteractionMessageComposer(
                    securityClassificationType = securityClassificationType,
                    warningText = LocalContext.current.resources.stringWithStyledArgs(
                        R.string.label_system_message_blocked_user,
                        MaterialTheme.wireTypography.body01,
                        MaterialTheme.wireTypography.body02,
                        colorsScheme().secondaryText,
                        colorsScheme().onBackground,
                        stringResource(id = R.string.member_name_you_label_titlecase)
                    ),
                    messageListContent = messageListContent
                )
            }

            InteractionAvailability.DELETED_USER -> DisabledInteractionMessageComposer(
                securityClassificationType = securityClassificationType,
                warningText = LocalContext.current.resources.stringWithStyledArgs(
                    R.string.label_system_message_user_not_available,
                    MaterialTheme.wireTypography.body01,
                    MaterialTheme.wireTypography.body02,
                    colorsScheme().secondaryText,
                    colorsScheme().onBackground,
                ),
                messageListContent = messageListContent
            )

            InteractionAvailability.NOT_MEMBER, InteractionAvailability.DISABLED -> DisabledInteractionMessageComposer(
                securityClassificationType = securityClassificationType,
                warningText = null,
                messageListContent = messageListContent
            )

            InteractionAvailability.ENABLED -> {
                EnabledMessageComposer(
                    messageComposerStateHolder = messageComposerStateHolder,
                    snackbarHostState = snackbarHostState,
                    messageListContent = messageListContent,
                    onSendButtonClicked = {
                        onSendMessageBundle(messageCompositionHolder.toMessageBundle())
                        onClearMentionSearchResult()
                        clearMessage()
                    },
                    onPingOptionClicked = { onSendMessageBundle(Ping) },
                    onAttachmentPicked = { onSendMessageBundle(AttachmentPickedBundle(it)) },
                    onAudioRecorded = { onSendMessageBundle(AudioMessageBundle(it)) },
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
private fun DisabledInteractionMessageComposer(
    warningText: AnnotatedString?,
    messageListContent: @Composable () -> Unit,
    securityClassificationType: SecurityClassificationType,
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
                    .background(color = colorsScheme().backgroundVariant)
                    .then(fillRemainingSpaceBetweenMessageListContentAndMessageComposer)
            ) {
                messageListContent()
            }
            if (warningText != null) {
                Divider(color = MaterialTheme.wireColorScheme.outline)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = colorsScheme().backgroundVariant)
                        .padding(dimensions().spacing16x)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_conversation),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = "",
                        modifier = Modifier
                            .padding(start = dimensions().spacing8x)
                            .size(dimensions().spacing12x)
                    )
                    Text(
                        text = warningText,
                        style = MaterialTheme.wireTypography.body01,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(weight = 1f, fill = false)
                            .padding(start = dimensions().spacing16x)
                    )
                }
            }
            SecurityClassificationBanner(securityClassificationType = securityClassificationType)
        }
    }
}

@Composable
private fun EnabledMessageComposer(
    messageComposerStateHolder: MessageComposerStateHolder,
    snackbarHostState: SnackbarHostState,
    messageListContent: @Composable () -> Unit,
    onSendButtonClicked: () -> Unit,
    onPingOptionClicked: () -> Unit,
    onAttachmentPicked: (UriAsset) -> Unit,
    onAudioRecorded: (UriAsset) -> Unit,
    onChangeSelfDeletionClicked: () -> Unit,
    onSearchMentionQueryChanged: (String) -> Unit,
    onClearMentionSearchResult: () -> Unit,
    tempWritableVideoUri: Uri?,
    tempWritableImageUri: Uri?,
) {
    with(messageComposerStateHolder) {
        Column {
            when (messageCompositionInputStateHolder.inputState) {
                MessageCompositionInputState.ACTIVE -> {
                    ActiveMessageComposer(
                        messageComposerStateHolder = messageComposerStateHolder,
                        snackbarHostState = snackbarHostState,
                        tempWritableVideoUri = tempWritableVideoUri,
                        tempWritableImageUri = tempWritableImageUri,
                        messageListContent = messageListContent,
                        onTransitionToInActive = messageComposerStateHolder::toInActive,
                        onSendButtonClicked = onSendButtonClicked,
                        onAttachmentPicked = onAttachmentPicked,
                        onAudioRecorded = onAudioRecorded,
                        onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
                        onSearchMentionQueryChanged = onSearchMentionQueryChanged,
                        onClearMentionSearchResult = onClearMentionSearchResult,
                        onPingOptionClicked = onPingOptionClicked
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
                    .background(color = colorsScheme().backgroundVariant)
                    .then(fillRemainingSpaceBetweenMessageListContentAndMessageComposer)
            ) {
                messageListContent()
            }
            Divider(color = MaterialTheme.wireColorScheme.outline)
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

@Suppress("ComplexMethod")
@Composable
private fun ActiveMessageComposer(
    messageComposerStateHolder: MessageComposerStateHolder,
    snackbarHostState: SnackbarHostState,
    messageListContent: @Composable () -> Unit,
    onTransitionToInActive: () -> Unit,
    onChangeSelfDeletionClicked: () -> Unit,
    onSearchMentionQueryChanged: (String) -> Unit,
    onSendButtonClicked: () -> Unit,
    onAttachmentPicked: (UriAsset) -> Unit,
    onAudioRecorded: (UriAsset) -> Unit,
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
                                    searchQuery = messageComposition.value.messageText,
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
                                        SecurityClassificationBanner(
                                            securityClassificationType = messageComposerViewState.value.securityClassificationType
                                        )
                                    }
                                }

                                Box(fillRemainingSpaceOrWrapContent) {
                                    var currentSelectedLineIndex by remember { mutableStateOf(0) }
                                    var cursorCoordinateY by remember { mutableStateOf(0F) }

                                    ActiveMessageComposerInput(
                                        messageComposition = messageComposition.value,
                                        inputSize = messageCompositionInputStateHolder.inputSize,
                                        inputType = messageCompositionInputStateHolder.inputType,
                                        inputVisibility = messageCompositionInputStateHolder.inputVisibility,
                                        inputFocused = messageCompositionInputStateHolder.inputFocused,
                                        onInputFocusedChanged = ::onInputFocusedChanged,
                                        onToggleInputSize = messageCompositionInputStateHolder::toggleInputSize,
                                        onCancelReply = messageCompositionHolder::clearReply,
                                        onCancelEdit = ::cancelEdit,
                                        onMessageTextChanged = {
                                            messageCompositionHolder.setMessageText(
                                                messageTextFieldValue = it,
                                                onSearchMentionQueryChanged = onSearchMentionQueryChanged,
                                                onClearMentionSearchResult = onClearMentionSearchResult
                                            )
                                        },
                                        onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
                                        onSendButtonClicked = onSendButtonClicked,
                                        onEditButtonClicked = {
                                            onSendButtonClicked()
                                            messageCompositionInputStateHolder.toComposing()
                                        },
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
                                            searchQuery = messageComposition.value.messageText,
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
                                    isFileSharingEnabled = messageComposerViewState.value.isFileSharingEnabled,
                                    isSelfDeletingSettingEnabled = isSelfDeletingSettingEnabled,
                                    isSelfDeletingActive = messageComposerViewState.value.selfDeletionTimer.duration.isPositiveNotNull(),
                                    isMentionActive = messageComposerViewState.value.mentionSearchResult.isNotEmpty(),
                                    onMentionButtonClicked = {
                                        messageCompositionHolder.startMention(
                                            onSearchMentionQueryChanged,
                                            onClearMentionSearchResult
                                        )
                                    },
                                    onOnSelfDeletingOptionClicked = onChangeSelfDeletionClicked,
                                    onRichOptionButtonClicked = messageCompositionHolder::addOrRemoveMessageMarkdown,
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
                            snackbarHostState = snackbarHostState,
                            onRecordAudioMessageClicked = ::toAudioRecording,
                            onCloseRecordAudio = ::toCloseAudioRecording,
                            onAttachmentPicked = onAttachmentPicked,
                            onAudioRecorded = onAudioRecorded,
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
                    // This covers the situation when the user switches from attachment options to the input keyboard -
                    // there is a moment when both attachmentOptionsDisplayed and isKeyboardVisible are false,
                    // but right after that keyboard shows, so if we know that
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
                if (additionalOptionStateHolder
                        .additionalOptionsSubMenuState != AdditionalOptionSubMenuState.RecordAudio
                ) {
                    onTransitionToInActive()
                }
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
        tempWritableImageUri = null,
        snackbarHostState = SnackbarHostState()
    )
}
