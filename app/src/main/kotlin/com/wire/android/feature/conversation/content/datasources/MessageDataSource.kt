package com.wire.android.feature.conversation.content.datasources

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.content.datasources.local.MessageLocalDataSource
import com.wire.android.feature.conversation.content.mapper.MessageMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessageDataSource(
    private val messageLocalDataSource: MessageLocalDataSource,
    private val messageMapper: MessageMapper
) : MessageRepository {

    override suspend fun save(message: Message): Either<Failure, Unit> {
        val messageEntity = messageMapper.fromMessageToEntity(message)
        return messageLocalDataSource.save(messageEntity)
    }

    override suspend fun conversationMessages(conversationId: String): Flow<List<Message>> =
        messageLocalDataSource.messagesByConversationId(conversationId).map { messages ->
            messages.map { messageMapper.fromEntityToMessage(it) }
        }
}
