package com.wire.android.feature.conversation.content.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class AndroidSendMessageWorker(
    appContext: Context, params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val senderUserId = inputData.getString(DATA_SENDER_USER_ID)!!
        val messageId = inputData.getString(DATA_MESSAGE_ID)!!

        TODO("Not yet implemented")
    }

    companion object {
        private const val DATA_SENDER_USER_ID = "sender_user_id"
        private const val DATA_MESSAGE_ID = "message_id"

        fun workParameters(senderUserId: String, messageId: String): Data = workDataOf(
            DATA_SENDER_USER_ID to senderUserId,
            DATA_MESSAGE_ID to messageId
        )
    }
}
