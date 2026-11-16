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

import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.Process
import android.os.RemoteException
import android.os.ResultReceiver
import com.wire.android.appLogger
import com.wire.android.ui.WireActivity
import com.wire.android.ui.debug.StartServiceReceiver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/** Short-lived helper in :restart. It must not initialize Kalium or open user databases. */
class RestartService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var restartRequested = false
    private var reopenApp = false
    private var previousProcessToken: IBinder? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (previousProcessToken == null) prepareRestart(intent, startId)
        return START_NOT_STICKY
    }

    private fun prepareRestart(intent: Intent?, startId: Int) {
        val previousPid = intent?.getIntExtra(EXTRA_PID, -1) ?: -1
        val previousProcess = intent?.extras?.getBinder(EXTRA_PROCESS)

        @Suppress("DEPRECATION")
        val ready = intent?.getParcelableExtra<ResultReceiver>(EXTRA_READY)
        val invalidPid = previousPid <= 0 || previousPid == Process.myPid()
        if (ready == null || invalidPid || previousProcess == null) {
            stopSelf(startId)
            return
        }
        // Keep the Binder proxy alive until its death notification is delivered.
        previousProcessToken = previousProcess
        // Binder death confirms the old process is gone before asking Android to recreate it.
        val onDeath = IBinder.DeathRecipient {
            handler.post {
                restoreServices()
            }
        }
        try {
            previousProcess.linkToDeath(onDeath, 0)
            // The main process signals only after sync shutdown and broadcast completion.
            val proceed = resultReceiver { data ->
                if (!restartRequested) {
                    restartRequested = true
                    reopenApp = data?.getBoolean(EXTRA_REOPEN_APP) == true
                    Process.killProcess(previousPid)
                }
            }
            ready.send(0, Bundle().apply { putParcelable(EXTRA_PROCEED, proceed) })
        } catch (_: RemoteException) {
            restoreServices()
        }
    }

    private fun restoreServices() {
        try {
            // The receiver runs in the main process and checks account/MDM settings before starting the socket service.
            sendBroadcast(Intent(this, StartServiceReceiver::class.java).setAction(ACTION_RESTORE_SERVICES))
            if (reopenApp) {
                try {
                    startActivity(
                        Intent(this, WireActivity::class.java).apply {
                            action = Intent.ACTION_MAIN
                            addCategory(Intent.CATEGORY_LAUNCHER)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } catch (_: ActivityNotFoundException) {
                    // UI relaunch is best-effort; service recovery was already requested.
                } catch (_: SecurityException) {
                    // Background activity restrictions may reject this best-effort launch.
                }
            }
        } finally {
            stopSelf()
            Process.killProcess(Process.myPid())
        }
    }

    /** A ready helper that can be told to terminate the main process after its cleanup completes. */
    class PreparedRestart internal constructor(private val proceed: ResultReceiver) {
        fun restart(reopenApp: Boolean) {
            appLogger.i("$TAG: Signalling restart service to proceed after main-process cleanup")
            proceed.send(0, Bundle().apply { putBoolean(EXTRA_REOPEN_APP, reopenApp) })
        }
    }

    companion object {
        private const val TAG = "RestartService"
        private const val EXTRA_REOPEN_APP = "reopen_app"
        private const val EXTRA_PROCEED = "restart_helper_proceed"
        private const val EXTRA_READY = "restart_helper_ready"
        private const val READY_TIMEOUT_MS = 2_000L
        private const val EXTRA_PID = "previous_process_id"
        private const val EXTRA_PROCESS = "previous_process_binder"
        private const val ACTION_RESTORE_SERVICES = "com.wire.android.action.RESTORE_SERVICES_AFTER_RESTART"
        private val processToken = Binder()

        private fun resultReceiver(onResult: (Bundle?) -> Unit): ResultReceiver {
            val receiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) = onResult(resultData)
            }
            // Send the framework type, not an anonymous app class that the remote Bundle cannot load.
            val parcel = Parcel.obtain()
            return try {
                receiver.writeToParcel(parcel, 0)
                parcel.setDataPosition(0)
                ResultReceiver.CREATOR.createFromParcel(parcel)
            } finally {
                parcel.recycle()
            }
        }

        suspend fun prepareRestart(context: Context): PreparedRestart? {
            val ready = CompletableDeferred<PreparedRestart?>()
            val callback = resultReceiver { data ->
                if (!ready.isActive) return@resultReceiver
                @Suppress("DEPRECATION")
                val proceed = data?.getParcelable<ResultReceiver>(EXTRA_PROCEED)
                ready.complete(proceed?.let(::PreparedRestart))
            }
            val extras = Bundle().apply {
                putParcelable(EXTRA_READY, callback)
                putInt(EXTRA_PID, Process.myPid())
                putBinder(EXTRA_PROCESS, processToken)
            }
            // Called during MY_PACKAGE_REPLACED's temporary background-service allowance.
            appLogger.i("$TAG: Starting restart service; waiting up to ${READY_TIMEOUT_MS}ms for readiness")
            val intent = Intent(context, RestartService::class.java).putExtras(extras)
            if (context.startService(intent) == null) {
                appLogger.w("$TAG: Restart service could not be resolved; skipping restart")
                return null
            }
            val preparedRestart = try {
                withTimeoutOrNull(READY_TIMEOUT_MS) { ready.await() }
            } finally {
                ready.cancel() // Ignore replies delivered after timeout or cancellation.
            }
            if (preparedRestart == null) {
                appLogger.w("$TAG: No usable readiness reply within ${READY_TIMEOUT_MS}ms; stopping restart service")
                context.stopService(intent)
            }
            return preparedRestart
        }
    }
}
