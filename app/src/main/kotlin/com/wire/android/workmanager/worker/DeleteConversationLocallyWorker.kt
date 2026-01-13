/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.wire.android.R
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.NotificationConstants
import com.wire.android.notification.NotificationIds
import com.wire.android.notification.openAppPendingIntent
import com.wire.kalium.common.functional.fold
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.DoesValidSessionExistResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

/**
 * A worker responsible for performing local deletion of conversations in the background.
 *
 * This worker is used to delete conversations along with their associated assets. Since the
 * deletion process can involve large amounts of data (e.g., clearing files, database entries),
 * it is executed as a background task to avoid blocking the main thread or user interactions.
 *
 * @param appContext The application context, provided by WorkManager.
 * @param workerParams Parameters associated with this work request, including input data.
 * @param coreLogic A utility object that handles core application logic, such as session and conversation management.
 * @param notificationChannelsManager Manages notification channels for the application.
 */
@HiltWorker
class DeleteConversationLocallyWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val coreLogic: CoreLogic,
    private val notificationChannelsManager: NotificationChannelsManager,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = coroutineScope {
        val userIdString = inputData.getString(USER_ID)
        val conversationIdString = inputData.getString(CONVERSATION_ID)

        if (userIdString == null || conversationIdString == null) {
            return@coroutineScope Result.failure() // If either ID is not provided, fail the work
        }
        val qualifiedIdMapper = QualifiedIdMapper(null)
        val conversationId = qualifiedIdMapper.fromStringToQualifiedID(conversationIdString)
        val userId = qualifiedIdMapper.fromStringToQualifiedID(userIdString)
        coreLogic.getGlobalScope().doesValidSessionExist(userId).let {
            if (it is DoesValidSessionExistResult.Failure || (it is DoesValidSessionExistResult.Success && !it.doesValidSessionExist)) {
                return@coroutineScope Result.failure() // If no valid session exists, fail the work
            }
        }

        coreLogic.getSessionScope(userId).conversations.deleteConversationLocallyUseCase(conversationId)
            .fold({ Result.retry() }, { Result.success() })
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        notificationChannelsManager.createRegularChannel(
            NotificationConstants.OTHER_CHANNEL_ID,
            NotificationConstants.OTHER_CHANNEL_NAME
        )

        val notification = NotificationCompat.Builder(applicationContext, NotificationConstants.OTHER_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon_small)
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentTitle(applicationContext.getString(R.string.notification_deleting_conversation))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(openAppPendingIntent(applicationContext))
            .build()

        return ForegroundInfo(NotificationIds.DELETING_CONVERSATION_NOTIFICATION_ID.ordinal, notification)
    }

    companion object {
        private const val NAME = "delete_conversation_locally"
        const val CONVERSATION_ID = "delete_conversation_locally_conversation_id"
        const val USER_ID = "delete_conversation_locally_user_id"

        fun createUniqueWorkName(conversationId: ConversationId, userId: UserId): String {
            return listOf(NAME, conversationId, userId).joinToString(separator = "-")
        }
    }
}

enum class ConversationDeletionLocallyStatus {
    IDLE, RUNNING, SUCCEEDED, FAILED
}

/**
 * Enqueues a background task to delete a conversation and its associated assets locally.
 *
 * This function uses the WorkManager API to create and enqueue a one-time work request for
 * the `DeleteConversationLocallyWorker`. The work request is configured to run either as
 * expedited (if quota allows) or as non-expedited work. If a deletion task for the same
 * conversation is already running, the existing task will be kept or replaced based on the
 * work policy.
 *
 * @param conversationId The ID of the conversation to be deleted.
 * @param userId The ID of the user who owns the conversation.
 */
fun WorkManager.enqueueConversationDeletionLocally(
    conversationId: ConversationId,
    userId: UserId
): Flow<ConversationDeletionLocallyStatus> {
    val workName = DeleteConversationLocallyWorker.createUniqueWorkName(conversationId, userId)
    val request = OneTimeWorkRequestBuilder<DeleteConversationLocallyWorker>()
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .setInputData(
            workDataOf(
                DeleteConversationLocallyWorker.CONVERSATION_ID to conversationId.toString(),
                DeleteConversationLocallyWorker.USER_ID to userId.toString(),
            )
        )
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    val isAlreadyRunning = getWorkInfosForUniqueWorkLiveData(workName)
        .value
        ?.firstOrNull()
        ?.state == WorkInfo.State.RUNNING
    enqueueUniqueWork(
        workName,
        if (isAlreadyRunning) ExistingWorkPolicy.KEEP else ExistingWorkPolicy.REPLACE,
        request
    )

    return observeConversationDeletionStatusLocally(conversationId, userId)
}

/**
 * Observes the status of a conversation deletion task running locally.
 *
 * This function returns a [Flow] that emits the current status of the conversation deletion
 * process for the given conversation ID. The status is determined by monitoring the
 * [WorkInfo] associated with the unique work name of the `DeleteConversationLocallyWorker`.
 *
 * The returned statuses include:
 * - [ConversationDeletionLocallyStatus.RUNNING]: The deletion task is currently in progress.
 * - [ConversationDeletionLocallyStatus.SUCCEEDED]: The deletion task completed successfully.
 * - [ConversationDeletionLocallyStatus.FAILED]: The deletion task failed, was blocked, or cancelled.
 * - [ConversationDeletionLocallyStatus.IDLE]: No active work is found for the given conversation ID.
 */
fun WorkManager.observeConversationDeletionStatusLocally(
    conversationId: ConversationId,
    userId: UserId,
): Flow<ConversationDeletionLocallyStatus> {
    return getWorkInfosForUniqueWorkFlow(DeleteConversationLocallyWorker.createUniqueWorkName(conversationId, userId))
        .mapNotNull { workInfos ->
            workInfos.lastOrNull()?.let { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.RUNNING -> ConversationDeletionLocallyStatus.RUNNING

                    WorkInfo.State.SUCCEEDED -> ConversationDeletionLocallyStatus.SUCCEEDED
                    WorkInfo.State.FAILED,
                    WorkInfo.State.BLOCKED,
                    WorkInfo.State.CANCELLED -> ConversationDeletionLocallyStatus.FAILED
                }
            } ?: ConversationDeletionLocallyStatus.IDLE
        }
}
