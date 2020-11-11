package com.wire.android.feature.conversation.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.paging.DataSource
import androidx.paging.PagedList
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.suspending
import com.wire.android.core.ui.SingleLiveEvent
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.local.ConversationLocalDataSource
import com.wire.android.feature.conversation.data.remote.ConversationsRemoteDataSource

class ConversationDataSource(
    private val conversationMapper: ConversationMapper,
    private val conversationRemoteDataSource: ConversationsRemoteDataSource,
    private val conversationLocalDataSource: ConversationLocalDataSource
) : ConversationsRepository {

    override fun conversationsByBatch(
        pagingDelegate: ConversationsPagingDelegate
    ): LiveData<Either<Failure, PagedList<Conversation>>> {
        val failureLiveData = SingleLiveEvent<Failure>()

        val pagingLiveData = pagingDelegate.conversationList(conversationsDataFactory()) { lastConvId, size ->
            fetchConversations(lastConvId, size).onFailure {
                failureLiveData.value = it
            }
        }

        return MediatorLiveData<Either<Failure, PagedList<Conversation>>>().apply {
            addSource(failureLiveData) { value = Either.Left(it) }
            addSource(pagingLiveData) { value = Either.Right(it) }
        }
    }

    private fun conversationsDataFactory(): DataSource.Factory<Int, Conversation> =
        conversationLocalDataSource.conversationsDataFactory().map {
            conversationMapper.fromEntity(it)
        }

    /*
    TODO: get real list from conversationRemoteDataSource, update tests
        conversationRemoteDataSource.conversationsByBatch(start, size, ids).map {
            conversationMapper.fromConversationsResponse(it)
        }
     */
    private suspend fun fetchConversations(start: String?, size: Int): Either<Failure, Unit> =
        suspending {
            getDummyConversations(start, size).map {
                conversationMapper.toEntityList(it)
            }.flatMap {
                conversationLocalDataSource.saveConversations(it)
            }
        }

    private fun getDummyConversations(start: String?, size: Int): Either<Failure, List<Conversation>> {
        val startId = start?.toInt() ?: 1
        return Either.Right((0..size).map {
            val convId = "${startId + it}"
            Conversation(id = convId, name = "Conversation #$convId")
        })
    }
}
