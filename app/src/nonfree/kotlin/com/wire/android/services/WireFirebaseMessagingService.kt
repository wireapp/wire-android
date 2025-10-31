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

package com.wire.android.services

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.NetworkUtil
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.workmanager.worker.NotificationFetchWorker
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.notificationToken.Result
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class WireFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var networkUtil: NetworkUtil

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    private val scope by lazy {
        // There's no UI, no need to run anything using the Main/UI Dispatcher
        CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        appLogger.i(
            String.format(
                Locale.US,
                "$TAG: onMessageReceived(), ID: %s, Delay: %d, Priority: %d, Original Priority: %d, Network: %s",
                message.messageId,
                System.currentTimeMillis() - message.sentTime,
                message.priority,
                message.originalPriority,
                networkUtil.getNetworkStatus()
            )
        )
        enqueueNotificationFetchWorker(extractUserId(message))

        appLogger.i("$TAG: onMessageReceived End")
    }

    private fun enqueueNotificationFetchWorker(userId: String) {
        val requestBuilder = OneTimeWorkRequestBuilder<NotificationFetchWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(workDataOf(NotificationFetchWorker.USER_ID_INPUT_DATA to userId))

        // Only add network constraints if background notification retry feature is enabled
        if (BuildConfig.BACKGROUND_NOTIFICATION_RETRY_ENABLED) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            requestBuilder.setConstraints(constraints)
            appLogger.d("$TAG: Enqueued NotificationFetchWorker with network constraints")
        } else {
            appLogger.d("$TAG: Enqueued NotificationFetchWorker without network constraints")
        }

        val request = requestBuilder.build()
        val workManager = WorkManager.getInstance(applicationContext)

        workManager.enqueueUniqueWork(
            NotificationFetchWorker.WORK_NAME_PREFIX_PER_USER + userId,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun extractUserId(message: RemoteMessage): String {
        var userIdValue = ""

        for (items in message.data) {
            if (items.key == "user") {
                userIdValue = items.value
                break
            }
        }

        return userIdValue
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            coreLogic.globalScope {
                saveNotificationToken(token, "GCM", BuildConfig.FIREBASE_PUSH_SENDER_ID)
            }.let { result ->
                when (result) {
                    is Result.Failure.Generic ->
                        appLogger.e("$TAG: token registration has an issue : ${result.failure} ")
                    Result.Success ->
                        appLogger.i("$TAG: token registered successfully")
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        appLogger.i("$TAG: onDestroy")
        super.onDestroy()
    }

    companion object {
        private const val TAG = "WireFirebaseMessagingService"
    }
}
