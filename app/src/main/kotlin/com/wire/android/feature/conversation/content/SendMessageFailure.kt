package com.wire.android.feature.conversation.content

sealed class SendMessageFailure {
    object NetworkFailure : SendMessageFailure()
    class ClientsHaveChanged(
        val missingClientsOfUsers: Map<String, List<String>>,
        val redundantClientsOfUsers: Map<String, List<String>>,
        val deletedClientsOfUsers: Map<String, List<String>>
    ) : SendMessageFailure()
}
