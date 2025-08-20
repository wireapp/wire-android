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
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.feature.message.ObserveMessageByIdUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveMessageForConversationUseCase @Inject constructor(
    private val observeMessage: ObserveMessageByIdUseCase,
    private val getUsersForMessage: GetUsersForMessageUseCase,
    private val messageMapper: MessageMapper,
    private val dispatchers: DispatcherProvider,
) {
    suspend operator fun invoke(conversationId: ConversationId, messageId: String): Flow<UIMessage?> =
        observeMessage(conversationId, messageId)
            .map { result ->
                if (result is ObserveMessageByIdUseCase.Result.Success && result.message is Message.Standalone) {
                    val usersForMessage = getUsersForMessage(result.message)
                    messageMapper.toUIMessage(usersForMessage, result.message as Message.Standalone)
                } else {
                    null
                }
            }
            .flowOn(dispatchers.io())
}
