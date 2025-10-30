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

package com.wire.android.ui.home.conversations.usecase

import com.wire.android.mapper.MessageMapper
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.conversations.model.mapToQuotedContent
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.toUIText
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.feature.message.ObserveMessageByIdUseCase
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveQuoteMessageForConversationUseCase @Inject constructor(
    private val observeMessageById: ObserveMessageByIdUseCase,
    private val getUsersForMessage: GetUsersForMessageUseCase,
    private val messageMapper: MessageMapper,
    private val dispatchers: DispatcherProvider,
) {

    suspend operator fun invoke(conversationId: ConversationId, quotedMessageId: String) =
        observeMessageById(conversationId, quotedMessageId)
            .map { result ->
                when (result) {
                    is ObserveMessageByIdUseCase.Result.Failure -> UIQuotedMessage.UnavailableData
                    is ObserveMessageByIdUseCase.Result.Success -> {
                        when (val message = result.message) {
                            is Message.Regular -> {
                                val usersForMessage = getUsersForMessage(message)
                                when (val uiMessage = messageMapper.toUIMessage(usersForMessage, message)) {
                                    is UIMessage.Regular -> uiMessage.mapToQuotedContent()?.let { quotedContent ->
                                        uiMessage.header.userId?.let { senderId ->
                                            UIQuotedMessage.UIQuotedData(
                                                messageId = uiMessage.header.messageId,
                                                senderId = senderId,
                                                senderName = uiMessage.header.username,
                                                originalMessageDateDescription = "".toUIText(),
                                                editedTimeDescription = "".toUIText(),
                                                quotedContent = quotedContent,
                                                senderAccent = uiMessage.header.accent
                                            )
                                        }
                                    } ?: UIQuotedMessage.UnavailableData

                                    is UIMessage.System -> UIQuotedMessage.UnavailableData
                                    null -> UIQuotedMessage.UnavailableData
                                }
                            }

                            is Message.Signaling -> UIQuotedMessage.UnavailableData
                            is Message.System -> UIQuotedMessage.UnavailableData
                        }
                    }
                }
            }.flowOn(dispatchers.io())
}
