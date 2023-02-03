/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.workmanager

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.migration.MigrationManager
import com.wire.android.notification.NotificationChannelsManager
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
    private val wireNotificationManager: WireNotificationManager,
    private val notificationChannelsManager: NotificationChannelsManager,
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
                NotificationFetchWorker(appContext, workerParameters, wireNotificationManager, notificationChannelsManager)
            MigrationWorker::class.java.canonicalName ->
                MigrationWorker(appContext, workerParameters, migrationManager, notificationChannelsManager)
            else -> null
        }
    }
}
