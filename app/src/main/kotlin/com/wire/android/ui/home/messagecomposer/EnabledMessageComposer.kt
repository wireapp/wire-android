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
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.wire.android.appLogger
import com.wire.android.ui.common.banner.SecurityClassificationBanner
import com.wire.android.ui.common.bottombar.BottomNavigationBarHeight
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSelectItem
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSubMenuState
import com.wire.android.ui.home.messagecomposer.state.MessageComposerStateHolder
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputSize
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionType
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.util.isPositiveNotNull

@OptIn(ExperimentalLayoutApi::class)
@Suppress("ComplexMethod")
@Composable
fun EnabledMessageComposer(
    conversationId: ConversationId,
    messageComposerStateHolder: MessageComposerStateHolder,
    snackbarHostState: SnackbarHostState,
    messageListContent: @Composable () -> Unit,
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
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val navBarHeight = BottomNavigationBarHeight()

    val stateHolder = messageComposerStateHolder.enabledMessageComposerStateHolder

    val isImeVisible = WindowInsets.isImeVisible
    val offsetY = WindowInsets.ime.getBottom(density)

    with(density) {
        stateHolder.handleOffsetChange(offsetY.toDp(), navBarHeight, focusManager)
    }

    LaunchedEffect(isImeVisible) {
        stateHolder.handleIMEVisibility(isImeVisible)
    }
    LaunchedEffect(messageComposerStateHolder.modalBottomSheetState.isVisible) {
        if (!messageComposerStateHolder.modalBottomSheetState.isVisible
            && messageComposerStateHolder.additionalOptionStateHolder.selectedOption == AdditionalOptionSelectItem.SelfDeleting
        ) {
            messageComposerStateHolder.messageCompositionInputStateHolder.requestFocus()
            messageComposerStateHolder.additionalOptionStateHolder.hideAdditionalOptionsMenu()
        }
    }

    with(messageComposerStateHolder) {
        Surface(color = colorsScheme().messageComposerBackgroundColor) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                val expandOrHideMessages =
                    if (stateHolder.isTextExpanded) {
                        Modifier.height(0.dp)
                    } else {
                        Modifier.weight(1f)
                    }
                Box(
                    modifier = expandOrHideMessages
                        .background(color = colorsScheme().backgroundVariant)
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
                    if (!stateHolder.isTextExpanded) {
                        Modifier.wrapContentHeight()
                    } else {
                        Modifier.weight(1f)
                    }
                Column(
                    modifier = fillRemainingSpaceOrWrapContent
                        .fillMaxWidth()
                ) {
                    Box(Modifier.wrapContentSize()) {
                        SecurityClassificationBanner(
                            conversationId = conversationId
                        )
                    }

                    if (additionalOptionStateHolder.additionalOptionsSubMenuState != AdditionalOptionSubMenuState.RecordAudio) {
                        Box(fillRemainingSpaceOrWrapContent) {
                            var currentSelectedLineIndex by remember { mutableStateOf(0) }
                            var cursorCoordinateY by remember { mutableStateOf(0F) }

                            ActiveMessageComposerInput(
                                messageComposition = messageComposition.value,
                                inputSize = if (stateHolder.isTextExpanded) MessageCompositionInputSize.EXPANDED else MessageCompositionInputSize.COLLAPSED,
                                inputType = messageCompositionInputStateHolder.inputType,
                                inputFocused = messageCompositionInputStateHolder.inputFocused,
                                onInputFocusedChanged = ::onInputFocusedChanged,
                                onToggleInputSize = { stateHolder.isTextExpanded = !stateHolder.isTextExpanded },
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
                                showOptions = stateHolder.showOptions,
                                onPlusClick = {
                                    stateHolder.showOptions = true
                                    stateHolder.showSubOptions = true
                                    showAdditionalOptionsMenu()
                                    stateHolder.optionsHeight = stateHolder.keyboardHeight
                                },
                                modifier = fillRemainingSpaceOrWrapContent,
                            )

                            val mentionSearchResult = messageComposerViewState.value.mentionSearchResult
                            if (mentionSearchResult.isNotEmpty() &&
                                stateHolder.isTextExpanded
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
                    }

                    if (stateHolder.showOptions) {
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
                                        onClearMentionSearchResult
                                    )
                                },
                                onOnSelfDeletingOptionClicked = {
                                    messageComposerStateHolder.additionalOptionStateHolder.toSelfDeletingOptionsMenu()
                                    onChangeSelfDeletionClicked()
                                },
                                onRichOptionButtonClicked = messageCompositionHolder::addOrRemoveMessageMarkdown,
                                onPingOptionClicked = onPingOptionClicked,
                                onAdditionalOptionsMenuClicked = {
                                    if (stateHolder.showSubOptions) {
                                        messageCompositionInputStateHolder.toComposing()
                                    } else {
                                        stateHolder.showSubOptions = true
                                        focusManager.clearFocus()
                                        showAdditionalOptionsMenu()
                                    }
                                },
                                onRichEditingButtonClicked = additionalOptionStateHolder::toRichTextEditing,
                                onCloseRichEditingButtonClicked = additionalOptionStateHolder::toAttachmentAndAdditionalOptionsMenu,
                            )
                        }

                        AdditionalOptionSubMenu(
                            isFileSharingEnabled = messageComposerViewState.value.isFileSharingEnabled,
                            additionalOptionsState = additionalOptionStateHolder.additionalOptionsSubMenuState,
                            snackbarHostState = snackbarHostState,
                            onRecordAudioMessageClicked = {
                                stateHolder.showOptions = true
                                toAudioRecording()
                            },
                            onCloseRecordAudio = ::toCloseAudioRecording,
                            onAttachmentPicked = onAttachmentPicked,
                            onAudioRecorded = onAudioRecorded,
                            tempWritableImageUri = tempWritableImageUri,
                            tempWritableVideoUri = tempWritableVideoUri,
                            modifier = Modifier
                                .height(stateHolder.calculateOptionsMenuHeight(additionalOptionStateHolder.additionalOptionsSubMenuState))
                                .fillMaxWidth()
                                .background(
                                    colorsScheme().messageComposerBackgroundColor
                                )
                                .animateContentSize()
                        )
                    }
                }
            }

            BackHandler(isImeVisible || stateHolder.showOptions) {
                stateHolder.handleBackPressed(
                    isImeVisible,
                    additionalOptionStateHolder.additionalOptionsSubMenuState,
                    focusManager
                )
            }
        }
    }
}
