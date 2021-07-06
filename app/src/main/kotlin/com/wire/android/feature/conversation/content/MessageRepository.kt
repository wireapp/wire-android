package com.wire.android.feature.conversation.content

import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun decryptMessage(message: Message)
    suspend fun conversationMessages(conversationId: String): Flow<List<Message>>
}
