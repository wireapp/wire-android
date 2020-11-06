package com.wire.android.feature.conversation.list.usecase

import com.google.gson.annotations.SerializedName
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler
import com.wire.android.core.usecase.UseCase
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ConversationApi {
    @GET("/Conversations")
    fun conversationsByBatch(
        @Query("start") start: String,
        @Query("size") size: Int,
        @Query("ids") ids: List<String>
    ): Response<ConversationsResponse>
}

interface ConversationsRepository {
    suspend fun conversationsByBatch(start: String, size: Int, ids: List<String>): Either<Failure, ConversationsResponse>
}

class ConversationDataSource(private val conversationRemoteDataSource: ConversationRemoteDataSource) : ConversationsRepository {
    override suspend fun conversationsByBatch(start: String, size: Int, ids: List<String>): Either<Failure, ConversationsResponse> =
        conversationRemoteDataSource.conversationsByBatch(start, size, ids)
}

class ConversationRemoteDataSource(
    override val networkHandler: NetworkHandler,
    private val conversationApi: ConversationApi
) : ApiService() {
    suspend fun conversationsByBatch(start: String, size: Int, ids: List<String>): Either<Failure, ConversationsResponse> =
        request { conversationApi.conversationsByBatch(start, size, ids) }
}

data class ConversationsResponse(
    @SerializedName("has_more")
    val hasMore: Boolean,

    @SerializedName("conversations")
    val conversations: List<Conversation>
)

data class Conversation(
    @SerializedName("creator")
    val creator: String,

    @SerializedName("members")
    val members: ConversationMembers,

    @SerializedName("name")
    val name: String,

    @SerializedName("id")
    val id: String,

    @SerializedName("type")
    val type: Int,

    @SerializedName("message_timer")
    val messageTimer: Int
)

data class ConversationMembers(
    @SerializedName("self")
    val self: ConversationSelfMember,
    @SerializedName("others")
    val otherMembers: List<ConversationOtherMembers>
)

data class ConversationSelfMember(
    @SerializedName("hidden_ref")
    val hiddenReference: String? = null,

    @SerializedName("service")
    val service: ServiceReference? = null,

    @SerializedName("otr_muted_ref")
    val otrMutedReference: String? = null,

    @SerializedName("hidden")
    val hidden: Boolean,

    @SerializedName("id")
    val userId: String,

    @SerializedName("otr_archived")
    val otrArchived: Boolean,

    @SerializedName("otr_muted")
    val otrMuted: Boolean,

    @SerializedName("otr_archived_ref")
    val otrArchiveReference: String? = null
)

data class ConversationOtherMembers(
    @SerializedName("service")
    val service: ServiceReference? = null,

    @SerializedName("id")
    val userId: String,
)

data class ServiceReference(
    @SerializedName("id")
    val id: String,

    @SerializedName("provider")
    val provider: String
)

class GetConversationsUseCase(private val conversationsRepository: ConversationsRepository) :
    UseCase<ConversationsResponse, Unit> {

    //TODO: real implementation
    //TODO: implement paging
    override suspend fun run(params: Unit): Either<Failure, ConversationsResponse> =
        conversationsRepository.conversationsByBatch("", 10, emptyList())
}
