package com.wire.android.workmanager

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.notification.WireNotificationManager
import com.wire.android.workmanager.worker.NotificationFetchWorker
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.sync.WrapperWorkerFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class WireWorkerFactory @Inject constructor(
    private val notificationManagerCompat: NotificationManagerCompat,
    private val wireNotificationManager: WireNotificationManager,
    @KaliumCoreLogic
    private val coreLogic: CoreLogic
) : WorkerFactory() {

    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? {
        val kaliumWorker = WrapperWorkerFactory(
            coreLogic
        ).createWorker(appContext, workerClassName, workerParameters)

        return kaliumWorker ?: NotificationFetchWorker(
            appContext,
            workerParameters,
            wireNotificationManager,
            notificationManagerCompat
        )
    }

}
