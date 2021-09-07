package com.wire.android.core.events.datasource.remote

import com.google.gson.annotations.SerializedName

data class NotificationPageResponse(
    @SerializedName("time") val time: String,
    @SerializedName("has_more") val hasMore: Boolean,
    @SerializedName("notifications") val notifications: List<NotificationResponse>
)

data class NotificationResponse(
    @SerializedName("payload") val payload: List<Payload>?,
    @SerializedName("id") val id: String
)
