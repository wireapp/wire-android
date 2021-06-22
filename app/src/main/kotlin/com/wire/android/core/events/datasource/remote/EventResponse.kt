package com.wire.android.core.events.datasource.remote

import com.google.gson.annotations.SerializedName

data class EventResponse(
    @SerializedName("id") val id: String,
    @SerializedName("transient") val transient: Boolean,
    @SerializedName("payload") val payload: List<Payload>?
)

data class Payload(
    @SerializedName("qualified_conversation") val qualifiedConversation: QualifiedConversation,
    @SerializedName("conversation") val conversation: String,
    @SerializedName("time") val time: String,
    @SerializedName("data") val data: Data?,
    @SerializedName("from") val from: String,
    @SerializedName("qualified_from") val qualifiedFrom: QualifiedFrom,
    @SerializedName("type") val type: String
)

data class QualifiedConversation(
    @SerializedName("domain") val domain: String,
    @SerializedName("id") val id: String
)

data class Data(
    @SerializedName("text") val text: String,
    @SerializedName("sender") val sender: String,
    @SerializedName("recipient") val recipient: String
)

data class QualifiedFrom(
    @SerializedName("domain") val domain: String,
    @SerializedName("id") val id: String
)
