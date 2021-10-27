package com.wire.android.feature.conversation.content.usecase

interface SendMessageWorkerScheduler {
    suspend fun scheduleMessageSendingWorker(senderUserId: String, messageId: String)
}
