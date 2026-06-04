/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.privateReply

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.kalium.logic.data.message.draft.MessageDraft
import com.wire.kalium.logic.feature.conversation.CreateConversationResult
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.message.draft.SaveMessageDraftUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ReplyInPrivateViewModel(
    private val getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase,
    private val saveMessageDraft: SaveMessageDraftUseCase,
) : ViewModel() {

    private val _actions = MutableSharedFlow<ReplyInPrivateAction>()
    val actions = _actions.asSharedFlow()

    fun replyInPrivate(message: UIMessage.Regular) {
        val senderId = message.header.userId ?: return emitFailure()

        viewModelScope.launch {
            when (val result = getOrCreateOneToOneConversation(senderId)) {
                is CreateConversationResult.Success -> {
                    val oneToOneConversationId = result.conversation.id
                    saveMessageDraft(
                        MessageDraft(
                            conversationId = oneToOneConversationId,
                            text = "",
                            editMessageId = null,
                            quotedMessageId = message.header.messageId,
                            selectedMentionList = emptyList(),
                            quotedMessageConversationId = message.conversationId,
                        )
                    )
                    _actions.emit(ReplyInPrivateAction.Navigate(oneToOneConversationId))
                }

                is CreateConversationResult.Failure -> {
                    _actions.emit(ReplyInPrivateAction.Failure)
                }
            }
        }
    }

    private fun emitFailure() {
        viewModelScope.launch {
            _actions.emit(ReplyInPrivateAction.Failure)
        }
    }
}
