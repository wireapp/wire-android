package com.wire.android.feature.conversation.data.remote

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler

//TODO add real implementation when authentication is in
class ConversationRemoteDataSource(
    override val networkHandler: NetworkHandler
) : ApiService() {
    fun conversationsByBatch(start: String, size: Int, ids: List<String>): Either<Failure, ConversationsResponse> =
        Either.Right(ConversationsResponse.EMPTY)
}
