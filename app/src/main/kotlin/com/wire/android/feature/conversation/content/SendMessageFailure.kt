package com.wire.android.feature.conversation.content

import com.wire.android.core.exception.FeatureFailure

sealed class SendMessageFailure: FeatureFailure() {
    object NetworkFailure : SendMessageFailure()
    class ClientsHaveChanged(
        val missingClientsOfUsers: Map<String, List<String>>,
        val redundantClientsOfUsers: Map<String, List<String>>,
        val deletedClientsOfUsers: Map<String, List<String>>
    ) : SendMessageFailure()
}
