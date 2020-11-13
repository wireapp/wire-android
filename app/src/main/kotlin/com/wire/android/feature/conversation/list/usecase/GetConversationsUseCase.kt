package com.wire.android.feature.conversation.list.usecase

import androidx.paging.DataSource
import com.wire.android.core.exception.Failure
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.ConversationsRepository

class GetConversationsUseCase(private val conversationsRepository: ConversationsRepository) :
    UseCase<List<Conversation>, GetConversationsParams> {

    fun conversationsDataFactory() : DataSource.Factory<Int, Conversation> =
        conversationsRepository.conversationsDataFactory()

    override suspend fun run(params: GetConversationsParams): Either<Failure, List<Conversation>> =
        conversationsRepository.conversationsByBatch(params.start, params.size, params.ids)
}

data class GetConversationsParams(val start: String = String.EMPTY, val size: Int, val ids: List<String> = emptyList())
