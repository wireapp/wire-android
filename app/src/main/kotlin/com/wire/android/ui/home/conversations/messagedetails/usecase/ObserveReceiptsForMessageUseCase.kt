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

package com.wire.android.ui.home.conversations.messagedetails.usecase

import com.wire.android.mapper.UIParticipantMapper
import com.wire.android.ui.home.conversations.messagedetails.model.MessageDetailsReadReceiptsData
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.receipt.ReceiptType
import com.wire.kalium.logic.feature.message.ObserveMessageReceiptsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveReceiptsForMessageUseCase @Inject constructor(
    private val observeMessageReceipts: ObserveMessageReceiptsUseCase,
    private val uiParticipantMapper: UIParticipantMapper,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke(
        conversationId: ConversationId,
        messageId: String,
        type: ReceiptType
    ): Flow<MessageDetailsReadReceiptsData> =
        observeMessageReceipts(
            conversationId = conversationId,
            messageId = messageId,
            type = type
        ).map {
            MessageDetailsReadReceiptsData(
                readReceipts = it.map(uiParticipantMapper::toUIParticipant)
            )
        }.flowOn(dispatchers.io())
}
