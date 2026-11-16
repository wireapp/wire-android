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

package com.wire.android.ui.debug

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.os.Build
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.metro.wireApplicationGraph
import com.wire.android.feature.StartPersistentWebsocketIfNecessaryUseCase
import com.wire.android.notification.broadcastreceivers.CoroutineReceiver
import com.wire.android.services.RestartService
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.server.UpdateApiVersionsUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Restores the persistent WebSocket after boot or an app update, negotiates API versions within the package-replacement broadcast.
 */
class StartServiceReceiver : CoroutineReceiver() {
    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @Inject
    lateinit var startPersistentWebSocketService: StartPersistentWebsocketIfNecessaryUseCase

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: Lazy<CoreLogic>

    private var reopenApp = false
    private var preparedRestart: RestartService.PreparedRestart? = null

    override fun onReceive(context: Context, intent: Intent?) {
        context.wireApplicationGraph.inject(this)
        appLogger.i("$TAG: onReceive called with action ${intent?.action}")
        super.onReceive(context, intent)
    }

    // Keep unexpected negotiation/service failures from crashing the broadcast; cancellation still propagates.
    @Suppress("TooGenericExceptionCaught")
    override suspend fun receive(context: Context, intent: Intent): Unit = withContext(dispatcherProvider.io()) {
        preparedRestart = null
        reopenApp = false
        try {
            if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                val restartRequired = withTimeoutOrNull(API_REFRESH_TIMEOUT_MS) {
                    coreLogic.value.getGlobalScope().updateApiVersions() == UpdateApiVersionsUseCase.Result.Changed
                } ?: false
                if (restartRequired) {
                    reopenApp = wasVisibleBeforeUpdate(context)
                    preparedRestart = try {
                        RestartService.prepareRestart(context)
                    } catch (exception: IllegalStateException) {
                        appLogger.e("$TAG: RestartService could not start; keeping sync running", exception)
                        null
                    }
                    if (preparedRestart == null) appLogger.w("$TAG: RestartService unavailable; deferring restart")
                }
                if (preparedRestart != null) {
                    val stopped = withTimeoutOrNull(SYNC_STOP_TIMEOUT_MS) {
                        coreLogic.value.stopSyncForRestart()
                        true
                    } ?: false
                    if (!stopped) appLogger.w("$TAG: Sync shutdown exceeded the receiver deadline; terminating process")
                }
            }
            if (preparedRestart == null) {
                startPersistentWebSocketService()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            appLogger.e("$TAG: Package replacement handling failed", exception)
        }
    }

    override suspend fun onReceiveFinished(context: Context, intent: Intent) {
        preparedRestart?.restart(reopenApp = reopenApp)
    }

    internal fun wasVisibleBeforeUpdate(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        return try {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val exit = manager.getHistoricalProcessExitReasons(context.packageName, 0, 0)
                .filter { it.processName == context.packageName }
                .maxByOrNull { it.timestamp }

            @Suppress("DEPRECATION")
            val updateTime = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
            if (exit == null) {
                false
            } else {
                // Exit history has no version identifier. Match the latest main-process exit to this update's time.
                val belongsToUpdate = exit.timestamp in (updateTime - UPDATE_EXIT_WINDOW_MS)..(updateTime + UPDATE_EXIT_WINDOW_MS)
                val wasVisible = exit.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                    exit.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
                val reopen = exit.reason == ApplicationExitInfo.REASON_PACKAGE_UPDATED && belongsToUpdate && wasVisible
                appLogger.i("$TAG: Previous main-process exit: reason=${exit.reason}, importance=${exit.importance}, reopen=$reopen")
                reopen
            }
        } catch (exception: SecurityException) {
            appLogger.w("$TAG: Cannot read exit history; skipping UI relaunch", exception)
            false
        } catch (exception: PackageManager.NameNotFoundException) {
            appLogger.w("$TAG: Cannot read package update time; skipping UI relaunch", exception)
            false
        }
    }

    companion object {
        const val TAG = "StartServiceReceiver"
        private const val UPDATE_EXIT_WINDOW_MS = 120_000L
        private const val API_REFRESH_TIMEOUT_MS = 4_000L
        private const val SYNC_STOP_TIMEOUT_MS = 2_000L
    }
}
