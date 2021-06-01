package com.wire.android.feature.messaging.datasource.remote.remote

import com.wire.android.core.functional.suspending
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler
import com.wire.android.feature.messaging.ChatMessage
import com.wire.android.feature.messaging.datasource.remote.remote.api.MessageApi
import com.wire.messages.Otr

class MessageRemoteDataSource(
    override val networkHandler: NetworkHandler,
    private val chatMessageMapper: ChatMessageMapper,
    private val messageApi: MessageApi
) : ApiService() {

    suspend fun sendMessage(conversationId: String, message: ChatMessage<*>) = suspending {
        chatMessageMapper.toProtoBuf(message).map {
            Otr.NewOtrMessage.newBuilder()
                //TODO: Set recipients and other fields
                .setBlob(it.toByteString())
                .build()
        }.map { newOtrMessage ->
            messageApi.sendMessage(conversationId, newOtrMessage)
        }
    }
}
