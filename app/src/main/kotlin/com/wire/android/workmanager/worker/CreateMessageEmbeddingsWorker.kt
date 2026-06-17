/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.workmanager.worker

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.wire.android.R
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.NotificationConstants
import com.wire.android.notification.NotificationIds
import com.wire.android.notification.openAppPendingIntent
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.message.TextEmbeddingModel
import com.wire.kalium.logic.feature.message.embedding.CreateEmbeddingsForExistingMessagesUseCase
import com.wire.kalium.logic.feature.message.embedding.CreateEmbeddingsForExistingMessagesUseCaseImpl
import com.wire.kalium.logic.feature.message.messageSemanticIndexer
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CreateMessageEmbeddingsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val coreLogic: CoreLogic,
    private val textEmbeddingModel: TextEmbeddingModel,
    private val notificationChannelsManager: NotificationChannelsManager,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userIdString = inputData.getString(USER_ID_KEY) ?: return Result.failure()
        val userId = QualifiedIdMapper(null).fromStringToQualifiedID(userIdString)

        setForeground(createForegroundInfo(progress = null))

        val useCase = CreateEmbeddingsForExistingMessagesUseCaseImpl(
            messageSemanticIndexer = coreLogic.getSessionScope(userId)
                .messages
                .messageSemanticIndexer(textEmbeddingModel),
            modelId = textEmbeddingModel.modelId
        )

        return when (val result = useCase(::updateProgress)) {
            is CreateEmbeddingsForExistingMessagesUseCase.Result.Success ->
                Result.success(result.toOutputData(totalMessages = lastProgress?.totalMessages ?: result.processedMessages))

            is CreateEmbeddingsForExistingMessagesUseCase.Result.Failure ->
                Result.failure(workDataOf(FAILURE_CAUSE_KEY to result.cause.toString()))
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = createForegroundInfo(progress = null)

    private var lastProgress: CreateEmbeddingsForExistingMessagesUseCase.Progress? = null

    private suspend fun updateProgress(progress: CreateEmbeddingsForExistingMessagesUseCase.Progress) {
        lastProgress = progress
        setProgress(progress.toData())
        setForeground(createForegroundInfo(progress))
    }

    private fun createForegroundInfo(progress: CreateEmbeddingsForExistingMessagesUseCase.Progress?): ForegroundInfo {
        notificationChannelsManager.createRegularChannel(
            NotificationConstants.OTHER_CHANNEL_ID,
            NotificationConstants.OTHER_CHANNEL_NAME
        )

        val totalMessages = progress?.totalMessages ?: 0
        val processedMessages = progress?.processedMessages ?: 0
        val notification = NotificationCompat.Builder(applicationContext, NotificationConstants.OTHER_CHANNEL_ID)
            .setSmallIcon(com.wire.android.feature.notification.R.drawable.notification_icon_small)
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentTitle(applicationContext.getString(R.string.notification_creating_message_embeddings))
            .setContentText(
                applicationContext.getString(
                    R.string.notification_creating_message_embeddings_progress,
                    processedMessages,
                    totalMessages
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setProgress(
                totalMessages,
                processedMessages.coerceAtMost(totalMessages),
                progress == null || totalMessages <= 0
            )
            .setContentIntent(openAppPendingIntent(applicationContext))
            .build()

        return ForegroundInfo(
            NotificationIds.CREATING_MESSAGE_EMBEDDINGS_NOTIFICATION_ID.ordinal,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    companion object {
        private const val NAME = "create_message_embeddings"
        const val USER_ID_KEY = "create_message_embeddings_user_id"
        const val TOTAL_MESSAGES_KEY = "create_message_embeddings_total_messages"
        const val PROCESSED_MESSAGES_KEY = "create_message_embeddings_processed_messages"
        const val CREATED_EMBEDDINGS_KEY = "create_message_embeddings_created_embeddings"
        const val SKIPPED_MESSAGES_KEY = "create_message_embeddings_skipped_messages"
        const val FAILED_MESSAGES_KEY = "create_message_embeddings_failed_messages"
        const val MODEL_ID_KEY = "create_message_embeddings_model_id"
        const val FAILURE_CAUSE_KEY = "create_message_embeddings_failure_cause"

        fun createUniqueWorkName(userId: UserId): String = "$NAME-$userId"
    }
}

interface CreateMessageEmbeddingsWorkScheduler {
    fun enqueue(userId: UserId): Flow<CreateMessageEmbeddingsWorkStatus>
    fun observe(userId: UserId): Flow<CreateMessageEmbeddingsWorkStatus>
}

class DefaultCreateMessageEmbeddingsWorkScheduler @Inject constructor(
    private val workManager: WorkManager
) : CreateMessageEmbeddingsWorkScheduler {

    override fun enqueue(userId: UserId): Flow<CreateMessageEmbeddingsWorkStatus> = flow {
        val workName = CreateMessageEmbeddingsWorker.createUniqueWorkName(userId)
        val request = OneTimeWorkRequestBuilder<CreateMessageEmbeddingsWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(workDataOf(CreateMessageEmbeddingsWorker.USER_ID_KEY to userId.toString()))
            .build()
        val isAlreadyActive = workManager.getWorkInfosForUniqueWork(workName)
            .await()
            .firstOrNull()
            ?.state
            ?.isActive == true

        workManager.enqueueUniqueWork(
            workName,
            if (isAlreadyActive) ExistingWorkPolicy.KEEP else ExistingWorkPolicy.REPLACE,
            request
        )
        emitAll(observe(userId))
    }

    override fun observe(userId: UserId): Flow<CreateMessageEmbeddingsWorkStatus> =
        workManager.getWorkInfosForUniqueWorkFlow(CreateMessageEmbeddingsWorker.createUniqueWorkName(userId))
            .mapNotNull { workInfos ->
                workInfos.lastOrNull()?.toCreateMessageEmbeddingsWorkStatus() ?: CreateMessageEmbeddingsWorkStatus.Idle
            }

    private val WorkInfo.State.isActive: Boolean
        get() = this == WorkInfo.State.ENQUEUED || this == WorkInfo.State.RUNNING
}

sealed interface CreateMessageEmbeddingsWorkStatus {
    data object Idle : CreateMessageEmbeddingsWorkStatus
    data class Running(val progress: Progress?) : CreateMessageEmbeddingsWorkStatus
    data class Succeeded(val summary: Summary) : CreateMessageEmbeddingsWorkStatus
    data class Failed(val cause: String?) : CreateMessageEmbeddingsWorkStatus

    data class Progress(
        val totalMessages: Int,
        val processedMessages: Int,
        val createdEmbeddings: Int,
        val skippedMessages: Int,
        val failedMessages: Int,
        val modelId: String?
    )

    data class Summary(
        val totalMessages: Int,
        val processedMessages: Int,
        val createdEmbeddings: Int,
        val skippedMessages: Int,
        val failedMessages: Int,
        val modelId: String?
    )
}

private fun WorkInfo.toCreateMessageEmbeddingsWorkStatus(): CreateMessageEmbeddingsWorkStatus =
    when (state) {
        WorkInfo.State.ENQUEUED,
        WorkInfo.State.RUNNING -> CreateMessageEmbeddingsWorkStatus.Running(progress.toProgressOrNull())

        WorkInfo.State.SUCCEEDED -> CreateMessageEmbeddingsWorkStatus.Succeeded(outputData.toSummary())
        WorkInfo.State.FAILED,
        WorkInfo.State.BLOCKED,
        WorkInfo.State.CANCELLED -> CreateMessageEmbeddingsWorkStatus.Failed(
            outputData.getString(CreateMessageEmbeddingsWorker.FAILURE_CAUSE_KEY)
        )
    }

private fun CreateEmbeddingsForExistingMessagesUseCase.Progress.toData(): Data =
    workDataOf(
        CreateMessageEmbeddingsWorker.TOTAL_MESSAGES_KEY to totalMessages,
        CreateMessageEmbeddingsWorker.PROCESSED_MESSAGES_KEY to processedMessages,
        CreateMessageEmbeddingsWorker.CREATED_EMBEDDINGS_KEY to createdEmbeddings,
        CreateMessageEmbeddingsWorker.SKIPPED_MESSAGES_KEY to skippedMessages,
        CreateMessageEmbeddingsWorker.FAILED_MESSAGES_KEY to failedMessages,
        CreateMessageEmbeddingsWorker.MODEL_ID_KEY to modelId
    )

private fun CreateEmbeddingsForExistingMessagesUseCase.Result.Success.toOutputData(totalMessages: Int): Data =
    workDataOf(
        CreateMessageEmbeddingsWorker.TOTAL_MESSAGES_KEY to totalMessages,
        CreateMessageEmbeddingsWorker.PROCESSED_MESSAGES_KEY to processedMessages,
        CreateMessageEmbeddingsWorker.CREATED_EMBEDDINGS_KEY to createdEmbeddings,
        CreateMessageEmbeddingsWorker.SKIPPED_MESSAGES_KEY to skippedMessages,
        CreateMessageEmbeddingsWorker.FAILED_MESSAGES_KEY to failedMessages,
        CreateMessageEmbeddingsWorker.MODEL_ID_KEY to modelId
    )

private fun Data.toProgressOrNull(): CreateMessageEmbeddingsWorkStatus.Progress? =
    takeIf { keyValueMap.containsKey(CreateMessageEmbeddingsWorker.TOTAL_MESSAGES_KEY) }
        ?.let {
            CreateMessageEmbeddingsWorkStatus.Progress(
                totalMessages = getInt(CreateMessageEmbeddingsWorker.TOTAL_MESSAGES_KEY, 0),
                processedMessages = getInt(CreateMessageEmbeddingsWorker.PROCESSED_MESSAGES_KEY, 0),
                createdEmbeddings = getInt(CreateMessageEmbeddingsWorker.CREATED_EMBEDDINGS_KEY, 0),
                skippedMessages = getInt(CreateMessageEmbeddingsWorker.SKIPPED_MESSAGES_KEY, 0),
                failedMessages = getInt(CreateMessageEmbeddingsWorker.FAILED_MESSAGES_KEY, 0),
                modelId = getString(CreateMessageEmbeddingsWorker.MODEL_ID_KEY)
            )
        }

private fun Data.toSummary(): CreateMessageEmbeddingsWorkStatus.Summary =
    CreateMessageEmbeddingsWorkStatus.Summary(
        totalMessages = getInt(CreateMessageEmbeddingsWorker.TOTAL_MESSAGES_KEY, 0),
        processedMessages = getInt(CreateMessageEmbeddingsWorker.PROCESSED_MESSAGES_KEY, 0),
        createdEmbeddings = getInt(CreateMessageEmbeddingsWorker.CREATED_EMBEDDINGS_KEY, 0),
        skippedMessages = getInt(CreateMessageEmbeddingsWorker.SKIPPED_MESSAGES_KEY, 0),
        failedMessages = getInt(CreateMessageEmbeddingsWorker.FAILED_MESSAGES_KEY, 0),
        modelId = getString(CreateMessageEmbeddingsWorker.MODEL_ID_KEY)
    )

private suspend fun <T> ListenableFuture<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addListener(
            {
                try {
                    continuation.resume(get())
                } catch (exception: ExecutionException) {
                    continuation.resumeWithException(exception.cause ?: exception)
                } catch (throwable: Throwable) {
                    continuation.resumeWithException(throwable)
                }
            },
            MoreExecutors.directExecutor()
        )
        continuation.invokeOnCancellation { cancel(true) }
    }
