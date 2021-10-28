package com.wire.android.feature.conversation.content.mapper

import com.wire.android.feature.conversation.content.SendMessageFailure
import com.wire.android.feature.messaging.datasource.remote.api.MessageSendingErrorBody

class MessageFailureMapper {

    fun fromMessageSendingErrorBody(messageSendingErrorBody: MessageSendingErrorBody): SendMessageFailure.ClientsHaveChanged =
        messageSendingErrorBody.run {
            SendMessageFailure.ClientsHaveChanged(missingClientsOfUsers, redundantClientsOfUsers, deletedClientsOfUsers)
        }

}
