package com.wire.android.feature.conversation.data.local

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.storage.cache.CacheService
import com.wire.android.core.storage.cache.InMemoryCache
import com.wire.android.core.storage.db.DatabaseService
import com.wire.android.feature.conversation.members.datasources.local.ConversationMemberEntity
import com.wire.android.feature.conversation.members.datasources.local.ConversationMembersDao

class ConversationLocalDataSource(
    private val conversationDao: ConversationDao,
    private val conversationMembersDao: ConversationMembersDao,
    private val inMemoryCache: InMemoryCache
) : DatabaseService, CacheService {

    suspend fun saveConversations(conversations: List<ConversationEntity>): Either<Failure, Unit> = request {
        conversationDao.insertAll(conversations)
    }

    suspend fun updateConversations(conversations: List<ConversationEntity>): Either<Failure, Unit> = request {
        conversationDao.updateConversations(conversations)
    }

    suspend fun saveMemberIdsForConversations(conversationMemberEntityList: List<ConversationMemberEntity>) = request {
        conversationMembersDao.insertAll(conversationMemberEntityList)
    }

    suspend fun conversationMemberIds(conversationId: String): Either<Failure, List<String>> = request {
        conversationMembersDao.conversationMembers(conversationId)
    }

    suspend fun allConversationMemberIds(): Either<Failure, List<String>> = request {
        conversationMembersDao.allConversationMemberIds()
    }

    suspend fun numberOfConversations(): Either<Failure, Int> = request { conversationDao.count() }

    suspend fun currentOpenedConversationId(): Either<Failure, String> = requestCache {
        inMemoryCache.currentOpenedConversationId()
    }

    suspend fun updateCurrentConversationId(conversationId: String) : Either<Failure, Unit> = requestCache {
        inMemoryCache.updateConversationId(conversationId)
    }
}
