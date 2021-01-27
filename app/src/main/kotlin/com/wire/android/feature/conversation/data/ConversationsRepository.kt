package com.wire.android.feature.conversation.data

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.Conversation

interface ConversationsRepository {
    suspend fun fetchConversations(): Either<Failure, Unit>

    suspend fun conversationMemberIds(conversation: Conversation): Either<Failure, List<String>>

    suspend fun allConversationMemberIds(): Either<Failure, List<String>>
}
