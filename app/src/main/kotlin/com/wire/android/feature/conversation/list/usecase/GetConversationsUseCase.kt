package com.wire.android.feature.conversation.list.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.conversation.Conversation

class GetConversationsUseCase : UseCase<List<Conversation>, Unit> {

    //TODO: real implementation
    //TODO: implement paging
    @Suppress("MagicNumber") //TODO delete after real impl is done
    override suspend fun run(params: Unit): Either<Failure, List<Conversation>> =
        Either.Right((1..20).map { Conversation("Conv #$it") })
}
