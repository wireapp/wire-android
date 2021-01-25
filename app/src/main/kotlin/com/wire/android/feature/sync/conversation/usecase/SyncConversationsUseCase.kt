package com.wire.android.feature.sync.conversation.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.conversation.data.ConversationsRepository

class SyncConversationsUseCase(private val conversationsRepository: ConversationsRepository) : UseCase<Unit, Unit> {

    //TODO: load conversation roles as well
    override suspend fun run(params: Unit): Either<Failure, Unit> =
        conversationsRepository.fetchConversations()
}
