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
package com.wire.android.migration

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.workmanager.worker.enqueueMigrationWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UpdateReceiver : BroadcastReceiver() {

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider
    @Inject
    lateinit var migrationManager: MigrationManager
    @Inject
    lateinit var workManager: WorkManager

    private val scope by lazy {
        CoroutineScope(SupervisorJob() + dispatcherProvider.io())
    }

    override fun onReceive(context: Context, intent: Intent?) {
        appLogger.i("App updated to ${BuildConfig.VERSION_NAME}")
        scope.launch {
            if (migrationManager.shouldMigrate()) {
                appLogger.i("Migration worker enqueued")
                workManager.enqueueMigrationWorker()
            }
        }
    }
}
