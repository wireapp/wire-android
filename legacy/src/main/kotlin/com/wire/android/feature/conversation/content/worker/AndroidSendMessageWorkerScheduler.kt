package com.wire.android.feature.conversation.content.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.wire.android.feature.conversation.content.usecase.SendMessageWorkerScheduler

class AndroidSendMessageWorkerScheduler(
    private val context: Context
) : SendMessageWorkerScheduler {
    override suspend fun scheduleMessageSendingWorker(senderUserId: String, messageId: String) {
        val workRequest = OneTimeWorkRequestBuilder<AndroidSendMessageWorker>()
            // TODO: Analyse possibility of adding user settings here. Not sending messages over roaming, or attachments over cel data
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(AndroidSendMessageWorker.workParameters(senderUserId, messageId))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "$senderUserId-$messageId",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }
}
