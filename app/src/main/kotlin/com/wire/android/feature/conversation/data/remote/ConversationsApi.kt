package com.wire.android.feature.conversation.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ConversationsApi {

    @GET(CONVERSATIONS_END_POINT)
    suspend fun conversationsByBatch(@Query(SIZE_QUERY_KEY) size: Int): Response<ConversationsResponse>

    companion object {
        private const val CONVERSATIONS_END_POINT = "/conversations"
        private const val SIZE_QUERY_KEY = "size"
    }
}
