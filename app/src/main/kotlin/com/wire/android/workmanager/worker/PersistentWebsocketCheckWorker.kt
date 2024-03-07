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
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest.Companion.MIN_BACKOFF_MILLIS
import androidx.work.WorkerParameters
import com.wire.android.appLogger
import com.wire.android.feature.ShouldStartPersistentWebSocketServiceUseCase
import com.wire.android.services.PersistentWebSocketService
import com.wire.android.workmanager.worker.PersistentWebsocketCheckWorker.Companion.NAME
import com.wire.android.workmanager.worker.PersistentWebsocketCheckWorker.Companion.TAG
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.TimeUnit

@HiltWorker
class PersistentWebsocketCheckWorker
@AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val shouldStartPersistentWebSocketService: ShouldStartPersistentWebSocketServiceUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        appLogger.i("Periodic check for persistent websocket connection")
        val persistentWebSocketServiceIntent = PersistentWebSocketService.newIntent(appContext)
        // todo, move this logic out, maybe to a separate use case and reuse in the boot broadcast receiver.
        shouldStartPersistentWebSocketService().let {
            when (it) {
                is ShouldStartPersistentWebSocketServiceUseCase.Result.Failure -> {
                    appLogger.e("${TAG}: Failure while fetching persistent web socket status flow")
                }

                is ShouldStartPersistentWebSocketServiceUseCase.Result.Success -> {
                    if (it.shouldStartPersistentWebSocketService) {
                        if (PersistentWebSocketService.isServiceStarted) {
                            appLogger.i("${TAG}: PersistentWebsocketService already started, not starting again")
                        } else {
                            appLogger.i("${TAG}: Starting PersistentWebsocketService")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                appContext.startForegroundService(persistentWebSocketServiceIntent)
                            } else {
                                appContext.startService(persistentWebSocketServiceIntent)
                            }
                        }
                    } else {
                        appLogger.i("${TAG}: Stopping PersistentWebsocketService, no user with persistent web socket enabled found")
                        appContext.stopService(persistentWebSocketServiceIntent)
                    }
                }
            }
        }
        Result.success()
    }

    companion object {
        const val NAME = "ws_check_worker"
        const val TAG = "PersistentWebsocketCheckWorker"
    }
}

fun WorkManager.enqueuePeriodicPersistentWebsocketCheckWorker() {
    appLogger.i("Enqueueing periodic work for $TAG")
    val request = PeriodicWorkRequestBuilder<PersistentWebsocketCheckWorker>(24, TimeUnit.HOURS)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()

    enqueueUniquePeriodicWork(
        NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

fun WorkManager.cancelPeriodicPersistentWebsocketCheckWorker() {
    appLogger.i("Cancelling all periodic work for $TAG")
    cancelAllWorkByTag(TAG)
}

