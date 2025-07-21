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
import com.wire.android.ui.home.messagecomposer.model.update
import com.wire.android.ui.navArgs
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.message.draft.GetMessageDraftUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageDraftViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val getMessageDraft: GetMessageDraftUseCase,
    private val getQuotedMessage: GetQuoteMessageForConversationUseCase,
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
            state.update {
                MessageComposition(conversationId, String.EMPTY)
            }
        }
    }

    private fun loadMessageDraft() {
        viewModelScope.launch {
            val draftResult = getMessageDraft(conversationId)

            draftResult?.let { draft ->
                val quotedData = draftResult.quotedMessageId?.let { quotedMessageId ->
                    when (val quotedData = getQuotedMessage(conversationId, quotedMessageId)) {
                        is UIQuotedMessage.UIQuotedData -> quotedData
                        UIQuotedMessage.UnavailableData -> null
                    }
                }
                state.update { messageComposition ->
                    messageComposition.copy(
                        draftText = draft.text,
                        selectedMentions = draft.selectedMentionList.mapNotNull { it.toUiMention(draft.text) },
                        editMessageId = draft.editMessageId,
                        quotedMessage = quotedData,
                        quotedMessageId = quotedData?.messageId
                    )
                }
            }
        }
    }
}
