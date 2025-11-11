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
package com.wire.android.ui.home.conversations.messages.draft

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.conversations.model.toUiMention
import com.wire.android.ui.home.conversations.usecase.GetQuoteMessageForConversationUseCase
import com.wire.android.ui.home.messagecomposer.model.MessageComposition
import com.wire.android.ui.home.messagecomposer.model.toDraft
import com.wire.android.ui.home.messagecomposer.model.update
import com.wire.android.ui.navArgs
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.draft.MessageDraft
import com.wire.kalium.logic.feature.message.draft.GetMessageDraftUseCase
import com.wire.kalium.logic.feature.message.draft.SaveMessageDraftUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageDraftViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val getMessageDraft: GetMessageDraftUseCase,
    private val getQuotedMessage: GetQuoteMessageForConversationUseCase,
    private val saveMessageDraft: SaveMessageDraftUseCase,
) : ViewModel() {

    private val conversationNavArgs: ConversationNavArgs = savedStateHandle.navArgs()
    val conversationId: QualifiedID = conversationNavArgs.conversationId

    var state = mutableStateOf(MessageComposition(conversationId, String.EMPTY))
        private set

    init {
        loadMessageDraft()
    }

    fun clearDraft() {
        viewModelScope.launch {
            if (state.value.quotedMessageId != null) {
                state.update { messageComposition ->
                    messageComposition.copy(
                        quotedMessageId = null,
                        quotedMessage = null,
                        draftText = String.EMPTY,
                    )
                }
                saveDraft(state.value.toDraft(""))
            }
        }
    }

    private fun loadMessageDraft() = viewModelScope.launch {
        getMessageDraft(conversationId)?.let { draft ->

            val quotedMessage = draft.quotedMessageId?.let { quotedMessageId ->
                getQuotedMessage(conversationId, quotedMessageId)
            }

            state.update { messageComposition ->
                messageComposition.copy(
                    draftText = draft.text,
                    selectedMentions = draft.selectedMentionList.mapNotNull { it.toUiMention(draft.text) },
                    editMessageId = draft.editMessageId,
                    quotedMessage = quotedMessage as? UIQuotedMessage.UIQuotedData,
                    quotedMessageId = (quotedMessage as? UIQuotedMessage.UIQuotedData)?.messageId,
                )
            }
        } ?: run {
            state.update { messageComposition ->
                MessageComposition(conversationId = conversationId)
            }
        }
    }

    fun saveDraft(messageDraft: MessageDraft) {
        viewModelScope.launch {
            saveMessageDraft(messageDraft)
        }
    }

    fun onMessageTextUpdate(newText: String) {
        if (state.value.draftText != newText) {
            saveDraft(state.value.toDraft(newText))
        }
    }
}
