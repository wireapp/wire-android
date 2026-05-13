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
package com.wire.android.ui.home.conversations.messages.draft

import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.usecase.GetQuoteMessageForConversationUseCase
import com.wire.kalium.logic.feature.message.draft.GetMessageDraftUseCase
import com.wire.kalium.logic.feature.message.draft.SaveMessageDraftUseCase
import dev.zacsweers.metro.Inject

@Inject
class MessageDraftViewModelFactory(
    private val getMessageDraft: GetMessageDraftUseCase,
    private val getQuotedMessage: GetQuoteMessageForConversationUseCase,
    private val saveMessageDraft: SaveMessageDraftUseCase,
) {
    fun create(conversationNavArgs: ConversationNavArgs): MessageDraftViewModel = MessageDraftViewModel(
        conversationNavArgs = conversationNavArgs,
        getMessageDraft = getMessageDraft,
        getQuotedMessage = getQuotedMessage,
        saveMessageDraft = saveMessageDraft,
    )
}
