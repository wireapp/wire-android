/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.banner.SecurityClassificationBannerForConversation
import com.wire.android.ui.common.bottombar.BottomNavigationBarHeight
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.home.conversations.ConversationActionPermissionType
import com.wire.android.ui.home.conversations.UsersTypingIndicatorForConversation
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.messagecomposer.location.GeoLocatedAddress
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSelectItem
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSubMenuState
import com.wire.android.ui.home.messagecomposer.state.InputType
import com.wire.android.ui.home.messagecomposer.state.MessageComposerStateHolder
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer

@OptIn(ExperimentalLayoutApi::class)
@Suppress("ComplexMethod")
@Composable
fun EnabledMessageComposer(
    conversationId: ConversationId,
    messageComposerStateHolder: MessageComposerStateHolder,
    messageListContent: @Composable () -> Unit,
    onChangeSelfDeletionClicked: (currentlySelected: SelfDeletionTimer) -> Unit,
    onSendButtonClicked: () -> Unit,
    onImagesPicked: (List<Uri>) -> Unit,
    onAttachmentPicked: (UriAsset) -> Unit,
    onAudioRecorded: (UriAsset) -> Unit,
    onLocationPicked: (GeoLocatedAddress) -> Unit,
    onPermissionPermanentlyDenied: (type: ConversationActionPermissionType) -> Unit,
    onPingOptionClicked: () -> Unit,
    onClearMentionSearchResult: () -> Unit,
    openDrawingCanvas: () -> Unit,
    tempWritableVideoUri: Uri?,
    tempWritableImageUri: Uri?,
    modifier: Modifier = Modifier,
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

        LaunchedEffect(isImeVisible) {
            if (!isImeVisible) {
                inputStateHolder.clearFocus()
            }
        }

        LaunchedEffect(offsetY) {
            with(density) {
                inputStateHolder.handleImeOffsetChange(
                    offsetY.toDp(),
                    navBarHeight,
                    imeAnimationSource.toDp(),
                    imeAnimationTarget.toDp()
                )
            }
        }

        Surface(
            modifier = modifier,
            color = colorsScheme().messageComposerBackgroundColor
        ) {
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
                    contentAlignment = Alignment.BottomCenter,
                    modifier = expandOrHideMessagesModifier
                        .background(color = colorsScheme().backgroundVariant)
                ) {
                    messageListContent()
                    if (!inputStateHolder.isTextExpanded) {
                        UsersTypingIndicatorForConversation(conversationId = conversationId)
                    }
                    if (!inputStateHolder.isTextExpanded && messageComposerViewState.value.mentionSearchResult.isNotEmpty()) {
                        MembersMentionList(
                            membersToMention = messageComposerViewState.value.mentionSearchResult,
                            searchQuery = messageComposerViewState.value.mentionSearchQuery,
                            onMentionPicked = { pickedMention ->
                                messageCompositionHolder.addMention(pickedMention)
                                onClearMentionSearchResult()
                            },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
                val fillRemainingSpaceOrWrapContent =
                    if (inputStateHolder.isTextExpanded) {
                        Modifier.weight(1f)
                    } else {
                        Modifier.wrapContentHeight()
                    }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = fillRemainingSpaceOrWrapContent
                        .fillMaxWidth()
                        .background(color = colorsScheme().backgroundVariant)
                ) {
                    Box(Modifier.wrapContentSize()) {
                        SecurityClassificationBannerForConversation(
                            conversationId = conversationId
                        )
                    }

                    if (additionalOptionStateHolder.additionalOptionsSubMenuState != AdditionalOptionSubMenuState.RecordAudio) {
                        Box(fillRemainingSpaceOrWrapContent, contentAlignment = Alignment.BottomCenter) {
                            var currentSelectedLineIndex by remember { mutableStateOf(0) }
                            var cursorCoordinateY by remember { mutableStateOf(0F) }

                            ActiveMessageComposerInput(
                                conversationId = conversationId,
                                messageComposition = messageComposition.value,
                                messageTextState = inputStateHolder.messageTextState,
                                isTextExpanded = inputStateHolder.isTextExpanded,
                                inputType = messageCompositionInputStateHolder.inputType,
                                inputFocused = messageCompositionInputStateHolder.inputFocused,
                                onInputFocusedChanged = ::onInputFocusedChanged,
                                onToggleInputSize = messageCompositionInputStateHolder::toggleInputSize,
                                onTextCollapse = messageCompositionInputStateHolder::collapseText,
                                onCancelReply = messageCompositionHolder::clearReply,
                                onCancelEdit = ::cancelEdit,
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
                            if (mentionSearchResult.isNotEmpty() && inputStateHolder.isTextExpanded
                            ) {
                                DropDownMentionsSuggestions(
                                    currentSelectedLineIndex = currentSelectedLineIndex,
                                    cursorCoordinateY = cursorCoordinateY,
                                    membersToMention = mentionSearchResult,
                                    searchQuery = messageComposerViewState.value.mentionSearchQuery,
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
                                conversationId = conversationId,
                                additionalOptionsState = additionalOptionStateHolder.additionalOptionState,
                                selectedOption = additionalOptionStateHolder.selectedOption,
                                isEditing = messageCompositionInputStateHolder.inputType is InputType.Editing,
                                isMentionActive = messageComposerViewState.value.mentionSearchResult.isNotEmpty(),
                                onMentionButtonClicked = messageCompositionHolder::startMention,
                                onOnSelfDeletingOptionClicked = {
                                    additionalOptionStateHolder.toSelfDeletingOptionsMenu()
                                    onChangeSelfDeletionClicked(it)
                                },
                                onRichOptionButtonClicked = messageCompositionHolder::addOrRemoveMessageMarkdown,
                                onPingOptionClicked = onPingOptionClicked,
                                onAdditionalOptionsMenuClicked = {
                                    if (!isKeyboardMoving) {
                                        if (additionalOptionStateHolder.selectedOption == AdditionalOptionSelectItem.AttachFile) {
                                            additionalOptionStateHolder.unselectAdditionalOptionsMenu()
                                            messageCompositionInputStateHolder.toComposing()
                                        } else {
                                            showAdditionalOptionsMenu()
                                        }
                                    }
                                },
                                onRichEditingButtonClicked = {
                                    messageCompositionInputStateHolder.requestFocus()
                                    additionalOptionStateHolder.toRichTextEditing()
                                },
                                onCloseRichEditingButtonClicked = additionalOptionStateHolder::toAttachmentAndAdditionalOptionsMenu,
                                onDrawingModeClicked = {
                                    showAdditionalOptionsMenu()
                                    openDrawingCanvas()
                                }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .height(
                                    inputStateHolder.calculateOptionsMenuHeight(additionalOptionStateHolder.additionalOptionsSubMenuState)
                                )
                                .fillMaxWidth()
                                .background(colorsScheme().messageComposerBackgroundColor)
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = inputStateHolder.subOptionsVisible,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                AdditionalOptionSubMenu(
                                    isFileSharingEnabled = messageComposerViewState.value.isFileSharingEnabled,
                                    additionalOptionsState = additionalOptionStateHolder.additionalOptionsSubMenuState,
                                    onRecordAudioMessageClicked = ::toAudioRecording,
                                    onCloseAdditionalAttachment = ::toInitialAttachmentOptions,
                                    onLocationPickerClicked = ::toLocationPicker,
                                    onImagesPicked = onImagesPicked,
                                    onAttachmentPicked = onAttachmentPicked,
                                    onAudioRecorded = onAudioRecorded,
                                    onLocationPicked = onLocationPicked,
                                    onPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
                                    tempWritableImageUri = tempWritableImageUri,
                                    tempWritableVideoUri = tempWritableVideoUri,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            BackHandler(inputStateHolder.inputType is InputType.Editing) {
                cancelEdit()
            }
            BackHandler(isImeVisible || inputStateHolder.optionsVisible) {
                inputStateHolder.collapseComposer(additionalOptionStateHolder.additionalOptionsSubMenuState)
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
