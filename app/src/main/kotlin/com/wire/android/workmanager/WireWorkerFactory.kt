package com.wire.android.workmanager

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.migration.MigrationManager
import com.wire.android.notification.WireNotificationManager
import com.wire.android.workmanager.worker.MigrationWorker
import com.wire.android.workmanager.worker.NotificationFetchWorker
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.sync.WrapperWorker
import com.wire.kalium.logic.sync.WrapperWorkerFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class WireWorkerFactory @Inject constructor(
    private val notificationManagerCompat: NotificationManagerCompat,
    private val wireNotificationManager: WireNotificationManager,
    private val migrationManager: MigrationManager,
    @KaliumCoreLogic
    private val coreLogic: CoreLogic
) : WorkerFactory() {

    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? {
        return when (workerClassName) {
            WrapperWorker::class.java.canonicalName ->
                WrapperWorkerFactory(coreLogic, WireForegroundNotificationDetailsProvider)
                    .createWorker(appContext, workerClassName, workerParameters)
            NotificationFetchWorker::class.java.canonicalName ->
                NotificationFetchWorker(appContext, workerParameters, wireNotificationManager, notificationManagerCompat)
            MigrationWorker::class.java.canonicalName ->
                MigrationWorker(appContext, workerParameters, migrationManager, notificationManagerCompat)
            else -> null
        }
    }

}
