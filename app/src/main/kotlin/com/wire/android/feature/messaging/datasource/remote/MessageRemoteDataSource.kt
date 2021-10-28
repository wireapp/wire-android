package com.wire.android.feature.messaging.datasource.remote

import com.wire.android.core.functional.Either
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler
import com.wire.android.core.network.either.EitherResponse
import com.wire.android.feature.messaging.ChatMessageEnvelope
import com.wire.android.feature.messaging.datasource.remote.api.MessageApi
import com.wire.android.feature.messaging.datasource.remote.api.MessageSendingErrorBody
import com.wire.android.feature.messaging.datasource.remote.mapper.OtrNewMessageMapper

class MessageRemoteDataSource(
    override val networkHandler: NetworkHandler,
    private val otrNewMessageMapper: OtrNewMessageMapper,
    private val messageApi: MessageApi
) : ApiService() {

    suspend fun sendMessage(conversationId: String, chatMessageEnvelope: ChatMessageEnvelope):
            Either<EitherResponse.Failure<MessageSendingErrorBody>, Unit> {
        val otrMessage = otrNewMessageMapper.fromMessageEnvelope(chatMessageEnvelope)
        return messageApi.sendMessage(conversationId, otrMessage).asEither()
    }

}
