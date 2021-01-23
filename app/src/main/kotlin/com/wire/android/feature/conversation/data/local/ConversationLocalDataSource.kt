package com.wire.android.feature.conversation.data.local

import androidx.paging.DataSource
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.storage.db.DatabaseService
import com.wire.android.feature.conversation.members.datasources.local.ConversationMemberEntity
import com.wire.android.feature.conversation.members.datasources.local.ConversationMembersDao

class ConversationLocalDataSource(
    private val conversationDao: ConversationDao,
    private val conversationMembersDao: ConversationMembersDao
) : DatabaseService {

    fun conversationsDataFactory(): DataSource.Factory<Int, ConversationEntity> = conversationDao.conversationsInBatch()

    suspend fun saveConversations(conversations: List<ConversationEntity>): Either<Failure, Unit> = request {
        conversationDao.insertAll(conversations)
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
}
