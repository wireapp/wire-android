package com.wire.android.feature.conversation.data.remote

import com.google.gson.annotations.SerializedName

data class ConversationsResponse(
    @SerializedName(HAS_MORE_KEY)
    val hasMore: Boolean,

    @SerializedName(CONVERSATION_KEY)
    val conversationResponses: List<ConversationResponse>
) {
    companion object {
        private const val CONVERSATION_KEY = "conversations"
        private const val HAS_MORE_KEY = "has_more"

        val EMPTY = ConversationsResponse(false, emptyList())
    }
}

data class ConversationResponse(
    @SerializedName("creator")
    val creator: String,

    @SerializedName("members")
    val members: ConversationMembersResponse,

    @SerializedName("name")
    val name: String?,

    @SerializedName("id")
    val id: String,

    @SerializedName("type")
    val type: Int,

    @SerializedName("message_timer")
    val messageTimer: Int
)

data class ConversationMembersResponse(
    @SerializedName("self")
    val self: ConversationSelfMemberResponse,

    @SerializedName("others")
    val otherMembers: List<ConversationOtherMembersResponse>
)

data class ConversationSelfMemberResponse(
    @SerializedName("hidden_ref")
    val hiddenReference: String?,

    @SerializedName("service")
    val service: ServiceReferenceResponse?,

    @SerializedName("otr_muted_ref")
    val otrMutedReference: String?,

    @SerializedName("hidden")
    val hidden: Boolean?,

    @SerializedName("id")
    val userId: String,

    @SerializedName("otr_archived")
    val otrArchived: Boolean?,

    @SerializedName("otr_muted")
    val otrMuted: Boolean?,

    @SerializedName("otr_archived_ref")
    val otrArchiveReference: String?
)

data class ConversationOtherMembersResponse(
    @SerializedName("service")
    val service: ServiceReferenceResponse?,

    @SerializedName("id")
    val userId: String,
)

data class ServiceReferenceResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("provider")
    val provider: String
)
