package com.wire.android.feature.messaging.datasource.remote.api

import com.wire.android.core.network.either.EitherResponse
import com.wire.messages.Otr
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface MessageApi {

    @POST(MESSAGE_IN_CONVERSATION)
    suspend fun sendMessage(
        @Path(CONVERSATION_ID) conversationId: String,
        @Body otrMessage: Otr.NewOtrMessage
    ): EitherResponse<MessageSendingErrorBody, Unit>

    companion object {
        private const val MESSAGE_IN_CONVERSATION = "/conversations/{conversationId}/otr/messages"
        private const val CONVERSATION_ID = "conversationId"
    }
}
