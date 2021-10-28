package com.wire.android.feature.messaging.datasource.remote.api

import com.google.gson.annotations.SerializedName

data class MessageSendingErrorBody(
    @SerializedName("missing") val missingClientsOfUsers: Map<String, List<String>>,
    @SerializedName("redundant") val redundantClientsOfUsers: Map<String, List<String>>,
    @SerializedName("deleted") val deletedClientsOfUsers: Map<String, List<String>>
)
