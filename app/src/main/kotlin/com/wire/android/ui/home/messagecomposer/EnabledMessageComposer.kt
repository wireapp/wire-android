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
 */
package com.wire.android.ui.home.messagecomposer

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationSource
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.banner.SecurityClassificationBannerForConversation
import com.wire.android.ui.common.bottombar.BottomNavigationBarHeight
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.home.conversations.UsersTypingIndicatorForConversation
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSelectItem
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSubMenuState
import com.wire.android.ui.home.messagecomposer.state.MessageComposerStateHolder
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionType
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.util.isPositiveNotNull

@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Suppress("ComplexMethod")
@Composable
fun EnabledMessageComposer(
    conversationId: ConversationId,
    messageComposerStateHolder: MessageComposerStateHolder,
    messageListContent: @Composable () -> Unit,
    onChangeSelfDeletionClicked: () -> Unit,
    onSearchMentionQueryChanged: (String) -> Unit,
    onTypingEvent: (Conversation.TypingIndicatorMode) -> Unit,
    onSendButtonClicked: () -> Unit,
    onAttachmentPicked: (UriAsset) -> Unit,
    onAudioRecorded: (UriAsset) -> Unit,
    onPingOptionClicked: () -> Unit,
    onClearMentionSearchResult: () -> Unit,
    tempWritableVideoUri: Uri?,
    tempWritableImageUri: Uri?
) {
    val density = LocalDensity.current
    val navBarHeight = BottomNavigationBarHeight()
    val isImeVisible = WindowInsets.isImeVisible
    val offsetY = WindowInsets.ime.getBottom(density)
    val isKeyboardMoving = isKeyboardMoving()
    val imeAnimationSource = WindowInsets.imeAnimationSource.getBottom(density)
    val imeAnimationTarget = WindowInsets.imeAnimationTarget.getBottom(density)

    with(messageComposerStateHolder) {
        val inputStateHolder = messageCompositionInputStateHolder

        LaunchedEffect(offsetY) {
            with(density) {
                inputStateHolder.handleOffsetChange(
                    offsetY.toDp(),
                    navBarHeight,
                    imeAnimationSource.toDp(),
                    imeAnimationTarget.toDp()
                )
            }
        }

        LaunchedEffect(isImeVisible) {
            inputStateHolder.handleIMEVisibility(isImeVisible)
        }

        LaunchedEffect(modalBottomSheetState.isVisible) {
            if (modalBottomSheetState.isVisible) {
                messageCompositionInputStateHolder.clearFocus()
            } else if (additionalOptionStateHolder.selectedOption == AdditionalOptionSelectItem.SelfDeleting) {
                messageCompositionInputStateHolder.requestFocus()
                additionalOptionStateHolder.hideAdditionalOptionsMenu()
            }
        }

        Surface(color = colorsScheme().messageComposerBackgroundColor) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                val expandOrHideMessagesModifier =
                    if (inputStateHolder.isTextExpanded) {
                        Modifier.height(0.dp)
                    } else {
                        Modifier.weight(1f)
                    }
                Box(
                    modifier = expandOrHideMessagesModifier.background(color = colorsScheme().backgroundVariant)
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
                    if (!inputStateHolder.isTextExpanded) {
                        Modifier.wrapContentHeight()
                    } else {
                        Modifier.weight(1f)
                    }
                Column(
                    modifier = fillRemainingSpaceOrWrapContent.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .background(color = colorsScheme().backgroundVariant)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        UsersTypingIndicatorForConversation(conversationId = conversationId)
                    }

                    Box(Modifier.wrapContentSize()) {
                        SecurityClassificationBannerForConversation(
                            conversationId = conversationId
                        )
                    }

                    if (additionalOptionStateHolder.additionalOptionsSubMenuState != AdditionalOptionSubMenuState.RecordAudio) {
                        Box(fillRemainingSpaceOrWrapContent) {
                            var currentSelectedLineIndex by remember { mutableStateOf(0) }
                            var cursorCoordinateY by remember { mutableStateOf(0F) }

                            ActiveMessageComposerInput(
                                messageComposition = messageComposition.value,
                                isTextExpanded = inputStateHolder.isTextExpanded,
                                inputType = messageCompositionInputStateHolder.inputType,
                                inputFocused = messageCompositionInputStateHolder.inputFocused,
                                onInputFocusedChanged = ::onInputFocusedChanged,
                                onToggleInputSize = messageCompositionInputStateHolder::toggleInputSize,
                                onCancelReply = messageCompositionHolder::clearReply,
                                onCancelEdit = ::cancelEdit,
                                onMessageTextChanged = {
                                    messageCompositionHolder.setMessageText(
                                        messageTextFieldValue = it,
                                        onSearchMentionQueryChanged = onSearchMentionQueryChanged,
                                        onClearMentionSearchResult = onClearMentionSearchResult,
                                        onTypingEvent = onTypingEvent
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
                                showOptions = inputStateHolder.optionsVisible,
                                onPlusClick = ::showAdditionalOptionsMenu,
                                modifier = fillRemainingSpaceOrWrapContent,
                            )

                            val mentionSearchResult = messageComposerViewState.value.mentionSearchResult
                            if (mentionSearchResult.isNotEmpty() &&
                                inputStateHolder.isTextExpanded
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
                    }

                    if (inputStateHolder.optionsVisible) {
                        if (additionalOptionStateHolder.additionalOptionsSubMenuState != AdditionalOptionSubMenuState.RecordAudio) {
                            AdditionalOptionsMenu(
                                additionalOptionsState = additionalOptionStateHolder.additionalOptionState,
                                selectedOption = additionalOptionStateHolder.selectedOption,
                                isEditing = messageCompositionInputStateHolder.inputType is MessageCompositionType.Editing,
                                isSelfDeletingSettingEnabled = isSelfDeletingSettingEnabled,
                                isSelfDeletingActive = messageComposerViewState.value.selfDeletionTimer.duration.isPositiveNotNull(),
                                isMentionActive = messageComposerViewState.value.mentionSearchResult.isNotEmpty(),
                                onMentionButtonClicked = {
                                    messageCompositionHolder.startMention(
                                        onSearchMentionQueryChanged,
                                        onClearMentionSearchResult,
                                        onTypingEvent
                                    )
                                },
                                onOnSelfDeletingOptionClicked = {
                                    additionalOptionStateHolder.toSelfDeletingOptionsMenu()
                                    onChangeSelfDeletionClicked()
                                },
                                onRichOptionButtonClicked = messageCompositionHolder::addOrRemoveMessageMarkdown,
                                onPingOptionClicked = onPingOptionClicked,
                                onAdditionalOptionsMenuClicked = {
                                    if (!isKeyboardMoving) {
                                        if (additionalOptionStateHolder.selectedOption == AdditionalOptionSelectItem.AttachFile) {
                                            additionalOptionStateHolder.hideAdditionalOptionsMenu()
                                            messageCompositionInputStateHolder.toComposing()
                                        } else {
                                            showAdditionalOptionsMenu()
                                        }
                                    }
                                },
                                onRichEditingButtonClicked = additionalOptionStateHolder::toRichTextEditing,
                                onCloseRichEditingButtonClicked = additionalOptionStateHolder::toAttachmentAndAdditionalOptionsMenu,
                            )
                        }

                        AdditionalOptionSubMenu(
                            isFileSharingEnabled = messageComposerViewState.value.isFileSharingEnabled,
                            additionalOptionsState = additionalOptionStateHolder.additionalOptionsSubMenuState,
                            onRecordAudioMessageClicked = ::toAudioRecording,
                            onCloseRecordAudio = ::toCloseAudioRecording,
                            onAttachmentPicked = onAttachmentPicked,
                            onAudioRecorded = onAudioRecorded,
                            tempWritableImageUri = tempWritableImageUri,
                            tempWritableVideoUri = tempWritableVideoUri,
                            modifier = Modifier
                                .height(
                                    inputStateHolder.calculateOptionsMenuHeight(
                                        additionalOptionStateHolder.additionalOptionsSubMenuState
                                    )
                                )
                                .fillMaxWidth()
                                .background(
                                    colorsScheme().messageComposerBackgroundColor
                                )
                                .animateContentSize()
                        )
                    }
                }
            }

            BackHandler(isImeVisible || inputStateHolder.optionsVisible) {
                inputStateHolder.handleBackPressed(
                    isImeVisible,
                    additionalOptionStateHolder.additionalOptionsSubMenuState
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun isKeyboardMoving(): Boolean {
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.isImeVisible
    val imeAnimationSource = WindowInsets.imeAnimationSource.getBottom(density)
    val imeAnimationTarget = WindowInsets.imeAnimationTarget.getBottom(density)
    return isImeVisible && imeAnimationSource != imeAnimationTarget
}
