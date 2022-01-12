package com.wire.android.feature.conversation.data

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.contact.DetailedContact
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.ConversationID

interface ConversationRepository {
    suspend fun fetchConversations(): Either<Failure, Unit>

    suspend fun conversationMemberIds(conversation: Conversation): Either<Failure, List<String>>

    suspend fun detailedConversationMembers(conversationId: String): Either<Failure, List<DetailedContact>>

    suspend fun allConversationMemberIds(): Either<Failure, List<String>>

    suspend fun updateConversations(conversations: List<Conversation>): Either<Failure, Unit>

    suspend fun numberOfConversations(): Either<Failure, Int>

    suspend fun currentOpenedConversationId(): Either<Failure, ConversationID>

    suspend fun updateCurrentConversationId(conversationId: ConversationID): Either<Failure, Unit>

    suspend fun conversationName(conversationId: String): Either<Failure, String>

    suspend fun restCurrentConversationId(): Either<Failure, Unit>
}
