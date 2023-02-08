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
