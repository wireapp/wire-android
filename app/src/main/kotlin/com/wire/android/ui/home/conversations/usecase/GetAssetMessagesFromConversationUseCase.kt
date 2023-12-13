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
import com.wire.android.ui.common.toGroupedByMonthAndYear
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetAssetMessagesForConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveUserListByIdUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.datetime.TimeZone
import javax.inject.Inject

class GetAssetMessagesFromConversationUseCase @Inject constructor(
    private val getAssetMessages: GetAssetMessagesForConversationUseCase,
    private val observeMemberDetailsByIds: ObserveUserListByIdUseCase,
    private val messageMapper: MessageMapper,
    private val dispatchers: DispatcherProvider // TODO(Media): to be used with pagination to be returned as flow
) {

    /**
     * This operation combines asset messages from a conversation and its respective user to UI
     * @param conversationId The conversation ID that it will look for asset messages in.
     *
     * String stands for the Month/Year header, and List<UIMessage> of messages to be displayed.
     * @return A [Map<String, List<UIMessage>>] indicating the success of the operation.
     *
     */
    suspend operator fun invoke(
        conversationId: ConversationId
    ): Map<String, List<UIMessage>> {
        return getAssetMessages(
            conversationId = conversationId,
            limit = 35,
            offset = 0
        ).let {
            val timeZone = TimeZone.currentSystemDefault()
            it.toGroupedByMonthAndYear(timeZone = timeZone)
        }.map {

            val values: List<UIMessage> = it.value.map { message ->
                observeMemberDetailsByIds(messageMapper.memberIdList(listOf(message)))
                    .mapLatest { usersList ->
                        messageMapper.toUIMessage(usersList, message)?.let { listOf(it) }
                            ?: emptyList()
                    }
                    .first()
                    .first()
            }
            mapOf(it.key to values)
        }.first()
    }
}
