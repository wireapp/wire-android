package com.wire.android.feature.conversation.data

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.core.functional.suspending
import com.wire.android.feature.contact.DetailedContact
import com.wire.android.feature.contact.datasources.mapper.ContactMapper
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.local.ConversationLocalDataSource
import com.wire.android.feature.conversation.data.remote.ConversationResponse
import com.wire.android.feature.conversation.data.remote.ConversationsRemoteDataSource
import com.wire.android.feature.conversation.data.remote.ConversationsResponse

class ConversationDataSource(
    private val conversationMapper: ConversationMapper,
    private val contactMapper: ContactMapper,
    private val conversationRemoteDataSource: ConversationsRemoteDataSource,
    private val conversationLocalDataSource: ConversationLocalDataSource
) : ConversationRepository {

    override suspend fun fetchConversations(): Either<Failure, Unit> = fetchRemoteConversations()

    private suspend fun fetchRemoteConversations(start: String? = null): Either<Failure, Unit> = suspending {
        conversationRemoteDataSource.conversationsByBatch(start, CONVERSATION_REQUEST_PAGE_SIZE).flatMap { response ->
            saveRemoteConversations(response.conversations).flatMap {
                fetchConversationsNextPageIfExists(response)
            }
        }
    }

    private suspend fun saveRemoteConversations(conversations: List<ConversationResponse>) = suspending {
        val entities = conversationMapper.fromConversationResponseListToEntityList(conversations)
        conversationLocalDataSource.saveConversations(entities).flatMap {
            saveConversationMemberIds(conversations)
        }
    }

    private suspend fun saveConversationMemberIds(conversations: List<ConversationResponse>): Either<Failure, Unit> {
        val conversationMembers =
            conversationMapper.fromConversationResponseListToConversationMembers(conversations)
        return conversationLocalDataSource.saveMemberIdsForConversations(conversationMembers)
    }

    private suspend fun fetchConversationsNextPageIfExists(response: ConversationsResponse) =
        if (response.hasMore) {
            val nextPageStartId = response.conversations.last().id
            fetchRemoteConversations(nextPageStartId)
        } else {
            Either.Right(Unit)
        }

    override suspend fun conversationMemberIds(conversation: Conversation): Either<Failure, List<String>> =
        conversationLocalDataSource.conversationMemberIds(conversation.id)

    override suspend fun allConversationMemberIds(): Either<Failure, List<String>> =
        conversationLocalDataSource.allConversationMemberIds()

    override suspend fun detailedConversationMembers(conversationId: String): Either<Failure, List<DetailedContact>> =
        conversationLocalDataSource.detailedMembersOfConversation(conversationId)
            .map { contacts -> contacts.map(contactMapper::fromContactWithClients) }

    override suspend fun updateConversations(conversations: List<Conversation>): Either<Failure, Unit> {
        val entities = conversationMapper.toEntityList(conversations)
        return conversationLocalDataSource.updateConversations(entities)
    }

    override suspend fun numberOfConversations(): Either<Failure, Int> =
        conversationLocalDataSource.numberOfConversations()

    override suspend fun currentOpenedConversationId(): Either<Failure, String> =
        conversationLocalDataSource.currentOpenedConversationId()

    override suspend fun updateCurrentConversationId(conversationId: String): Either<Failure, Unit> =
        conversationLocalDataSource.updateCurrentConversationId(conversationId)

    override suspend fun conversationName(conversationId: String): Either<Failure, String> =
        conversationLocalDataSource.conversationNameById(conversationId)

    companion object {
        private const val CONVERSATION_REQUEST_PAGE_SIZE = 100
    }
}
