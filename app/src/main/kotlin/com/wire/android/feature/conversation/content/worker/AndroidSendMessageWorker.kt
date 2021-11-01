package com.wire.android.feature.conversation.content.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ListenableWorker.Result.failure
import androidx.work.ListenableWorker.Result.retry
import androidx.work.ListenableWorker.Result.success
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.wire.android.core.exception.NetworkFailure
import com.wire.android.feature.conversation.content.domain.MessageSender
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AndroidSendMessageWorker(
    appContext: Context, params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val messageSender: MessageSender by inject()

    override suspend fun doWork(): Result {
        val senderUserId = inputData.getString(DATA_SENDER_USER_ID)!!
        val messageId = inputData.getString(DATA_MESSAGE_ID)!!

        return messageSender.trySendingOutgoingMessage(senderUserId, messageId)
            .fold({
                when {
                    runAttemptCount > MAX_RETRIES -> {
                        //TODO Give up on sending this message, store this information
                        failure()
                    }
                    it is NetworkFailure -> retry()
                    else -> failure()
                }
            }, {
                success()
            })!!
    }

    companion object {
        private const val DATA_SENDER_USER_ID = "sender_user_id"
        private const val DATA_MESSAGE_ID = "message_id"
        private const val MAX_RETRIES = 4

        fun workParameters(senderUserId: String, messageId: String): Data = workDataOf(
            DATA_SENDER_USER_ID to senderUserId,
            DATA_MESSAGE_ID to messageId
        )
    }
}
