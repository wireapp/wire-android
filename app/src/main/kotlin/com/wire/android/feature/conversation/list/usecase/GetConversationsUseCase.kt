package com.wire.android.feature.conversation.list.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.ConversationsRepository

@Deprecated(
    message = "Getting conversations on demand is deprecated. All conversations are fetched during slow sync.",
    replaceWith = ReplaceWith("SlowSyncWorkHandler().enqueueWork()")
)
class GetConversationsUseCase(
    private val conversationsRepository: ConversationsRepository
) : UseCase<List<Conversation>, GetConversationsParams> {

    override suspend fun run(params: GetConversationsParams): Either<Failure, List<Conversation>> =
        Either.Right(emptyList())
}

data class GetConversationsParams(val start: String?, val size: Int)
