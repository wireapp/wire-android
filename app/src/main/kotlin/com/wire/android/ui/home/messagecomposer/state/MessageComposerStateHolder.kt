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

package com.wire.android.ui.home.messagecomposer.state

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalDensity
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.home.conversations.MessageComposerViewState
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.messagecomposer.model.MessageComposition
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.data.message.draft.MessageDraft
import com.wire.kalium.logic.data.message.mention.MessageMention

@Suppress("LongParameterList")
@Composable
fun rememberMessageComposerStateHolder(
    messageComposerViewState: State<MessageComposerViewState>,
    modalBottomSheetState: WireModalSheetState,
    draftMessageComposition: MessageComposition,
    onSaveDraft: (MessageDraft) -> Unit,
    onSearchMentionQueryChanged: (String) -> Unit,
    onClearMentionSearchResult: () -> Unit,
    onTypingEvent: (Conversation.TypingIndicatorMode) -> Unit,
): MessageComposerStateHolder {
    val density = LocalDensity.current

    val messageComposition = remember(draftMessageComposition) {
        mutableStateOf(draftMessageComposition)
    }
    val messageTextState = rememberTextFieldState()
    LaunchedEffect(draftMessageComposition.draftText) {
        if (draftMessageComposition.draftText.isNotBlank()) {
            messageTextState.setTextAndPlaceCursorAtEnd(draftMessageComposition.draftText)
        }
    }

    val messageCompositionHolder = remember {
        MessageCompositionHolder(
            messageComposition = messageComposition,
            messageTextState = messageTextState,
            onSaveDraft = onSaveDraft,
            onSearchMentionQueryChanged = onSearchMentionQueryChanged,
            onClearMentionSearchResult = onClearMentionSearchResult,
            onTypingEvent = onTypingEvent,
        )
    }
    LaunchedEffect(Unit) {
        messageCompositionHolder.handleMessageTextUpdates()
    }

    // we derive the selfDeletionTimer from the messageCompositionHolder as a state in order to "observe" the changes to it
    // which are made "externally" and not inside the MessageComposer.
    val selfDeletionTimer = remember {
        derivedStateOf {
            messageComposerViewState.value.selfDeletionTimer
        }
    }

    val messageCompositionInputStateHolder = rememberSaveable(
        saver = MessageCompositionInputStateHolder.saver(
            messageTextState = messageTextState,
            selfDeletionTimer = selfDeletionTimer,
            density = density
        )
    ) {
        MessageCompositionInputStateHolder(
            messageTextState = messageTextState,
            selfDeletionTimer = selfDeletionTimer
        )
    }

    val additionalOptionStateHolder = rememberSaveable(
        saver = AdditionalOptionStateHolder.saver()
    ) {
        AdditionalOptionStateHolder()
    }

    return remember {
        MessageComposerStateHolder(
            messageComposerViewState = messageComposerViewState,
            modalBottomSheetState = modalBottomSheetState,
            messageCompositionInputStateHolder = messageCompositionInputStateHolder,
            messageCompositionHolder = messageCompositionHolder,
            additionalOptionStateHolder = additionalOptionStateHolder,
        )
    }
}

/**
 * Class holding the whole UI state for the MessageComposer, this is the class that is used by the out-side world to give the control
 * of the state to the parent Composables
 */
class MessageComposerStateHolder(
    val messageComposerViewState: State<MessageComposerViewState>,
    val messageCompositionInputStateHolder: MessageCompositionInputStateHolder,
    val messageCompositionHolder: MessageCompositionHolder,
    val additionalOptionStateHolder: AdditionalOptionStateHolder,
    val modalBottomSheetState: WireModalSheetState
) {
    val messageComposition = messageCompositionHolder.messageComposition

    val isSelfDeletingSettingEnabled = messageComposerViewState.value.selfDeletionTimer !is SelfDeletionTimer.Disabled &&
            messageComposerViewState.value.selfDeletionTimer !is SelfDeletionTimer.Enforced

    fun toEdit(messageId: String, editMessageText: String, mentions: List<MessageMention>) {
        messageCompositionHolder.setEditText(messageId, editMessageText, mentions)
        messageCompositionInputStateHolder.toEdit(editMessageText)
    }

    fun toReply(message: UIMessage.Regular) {
        messageCompositionHolder.setReply(message)
        messageCompositionInputStateHolder.toComposing()
    }

    fun onInputFocusedChanged(onFocused: Boolean) {
        if (onFocused) {
            additionalOptionStateHolder.unselectAdditionalOptionsMenu()
            messageCompositionInputStateHolder.requestFocus()
        } else {
            messageCompositionInputStateHolder.clearFocus()
        }
    }

    fun toAudioRecording() {
        messageCompositionInputStateHolder.showOptions()
        additionalOptionStateHolder.toAudioRecording()
    }

    fun toLocationPicker() {
        messageCompositionInputStateHolder.showOptions()
        additionalOptionStateHolder.toLocationPicker()
    }

    fun toInitialAttachmentOptions() {
        additionalOptionStateHolder.toInitialAttachmentOptionsMenu()
    }

    fun cancelEdit() {
        messageCompositionInputStateHolder.toComposing()
        messageCompositionHolder.clearMessage()
    }

    fun showAdditionalOptionsMenu() {
        messageCompositionInputStateHolder.showOptions()
        additionalOptionStateHolder.showAdditionalOptionsMenu()
    }

    fun clearMessage() {
        messageCompositionHolder.clearMessage()
    }
}
