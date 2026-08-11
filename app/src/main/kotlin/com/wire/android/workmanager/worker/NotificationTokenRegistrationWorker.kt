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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.wire.android.appLogger
import com.wire.android.session.AppUserSessionPreparationResult
import com.wire.android.session.UserSessionPreparationGate
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.session.GetAllSessionsResult

class NotificationTokenRegistrationWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val coreLogic: CoreLogic,
    private val userSessionPreparationGate: UserSessionPreparationGate,
) : CoroutineWorker(appContext, workerParameters) {

    @Suppress("ReturnCount")
    override suspend fun doWork(): Result {
        val token = inputData.getString(INPUT_TOKEN) ?: return Result.failure()
        val senderId = inputData.getString(INPUT_SENDER_ID) ?: return Result.failure()
        val sessions = when (val result = coreLogic.globalScope { getSessions() }) {
            is GetAllSessionsResult.Success -> result.sessions
            GetAllSessionsResult.Failure.NoSessionFound -> emptyList()
            is GetAllSessionsResult.Failure.Generic -> {
                appLogger.w("$TAG unable to read sessions: ${result.genericFailure}")
                return Result.retry()
            }
        }

        val preparationFailures = sessions.filter { it.isValid() }.mapNotNull { session ->
            when (val preparation = userSessionPreparationGate.prepare(session.userId)) {
                is AppUserSessionPreparationResult.Ready -> null
                is AppUserSessionPreparationResult.Failed -> preparation
            }
        }
        if (preparationFailures.any { it.canRetry }) return Result.retry()
        if (preparationFailures.isNotEmpty()) return Result.failure()

        return when (val result = coreLogic.globalScope { saveNotificationToken(token, TRANSPORT, senderId) }) {
            com.wire.kalium.logic.feature.notificationToken.Result.Success -> Result.success()
            is com.wire.kalium.logic.feature.notificationToken.Result.Failure.Generic -> {
                appLogger.w("$TAG token registration failed: ${result.failure}")
                Result.retry()
            }
        }
    }

    companion object {
        private const val TAG = "NotificationTokenRegistrationWorker"
        private const val WORK_NAME = "notification_token_registration"
        private const val INPUT_TOKEN = "token"
        private const val INPUT_SENDER_ID = "sender_id"
        private const val TRANSPORT = "GCM"

        fun enqueue(workManager: WorkManager, token: String, senderId: String) {
            val request = OneTimeWorkRequestBuilder<NotificationTokenRegistrationWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(workDataOf(INPUT_TOKEN to token, INPUT_SENDER_ID to senderId))
                .build()
            workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
