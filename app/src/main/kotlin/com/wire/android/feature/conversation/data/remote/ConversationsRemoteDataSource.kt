package com.wire.android.feature.conversation.data.remote

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler

class ConversationsRemoteDataSource(
    override val networkHandler: NetworkHandler,
    private val conversationsApi: ConversationsApi
) : ApiService() {

    suspend fun conversationsByBatch(start: String, size: Int, ids: List<String>): Either<Failure, ConversationsResponse> =
        request { conversationsApi.conversationsByBatch(size) }
}
