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
