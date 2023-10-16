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
package com.wire.android.ui.home.conversations.usecase

import com.wire.android.mapper.MessageMapper
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.conversation.ObserveUserListByIdUseCase
import com.wire.kalium.logic.feature.message.GetConversationMessagesFromSearchQueryUseCase
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetSearchMessagesForConversationUseCase @Inject constructor(
    private val getConversationMessagesFromSearch: GetConversationMessagesFromSearchQueryUseCase,
    private val observeMemberDetailsByIds: ObserveUserListByIdUseCase,
    private val messageMapper: MessageMapper
) {

    /**
     * This operation combines messages searched from a conversation and its respective user to UI
     * @param searchQuery The search term used to define which messages will be returned.
     * @param conversationId The conversation ID that it will look for messages in.
     * @return A [Either<CoreFailure, List<UIMessage>>] indicating the success of the operation.
     */
    suspend operator fun invoke(
        searchTerm: String,
        conversationId: ConversationId
    ): Either<CoreFailure, List<UIMessage>> =
        if (searchTerm.length >= MINIMUM_CHARACTERS_TO_SEARCH) {
            getConversationMessagesFromSearch(
                searchQuery = searchTerm,
                conversationId = conversationId
            ).map { foundMessages ->
                foundMessages.flatMap { messageItem ->
                    observeMemberDetailsByIds(
                        userIdList = messageMapper.memberIdList(
                            messages = foundMessages
                        )
                    ).map { usersList ->
                        messageMapper.toUIMessage(
                            userList = usersList,
                            message = messageItem
                        )?.let { listOf(it) } ?: emptyList()
                    }.first()
                }
            }
        } else {
            Either.Right(value = listOf())
        }

    private companion object {
        const val MINIMUM_CHARACTERS_TO_SEARCH = 2
    }
}
