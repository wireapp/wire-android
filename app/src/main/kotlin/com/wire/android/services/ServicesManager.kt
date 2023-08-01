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

package com.wire.android.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wire.android.appLogger
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * This is helper class that should be used for starting/stopping any services.
 * The idea is that we don't want to inject, or provide any context into ViewModel,
 * but to have an ability start Service from it.
 */
@Singleton
class ServicesManager @Inject constructor(
    private val context: Context,
    dispatcherProvider: DispatcherProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    private val ongoingCallServiceForUsers = MutableStateFlow<List<OngoingCallData>>(emptyList())

    init {
        scope.launch {
            ongoingCallServiceForUsers
                .debounce { if (it.isEmpty()) 0L else DEBOUNCE_TIME } // debounce to avoid starting and stopping service too fast
                .distinctUntilChanged()
                .collectLatest {
                    if (it.isEmpty()) {
                        appLogger.i("ServicesManager: stopping OngoingCallService because there are no ongoing calls")
                        // Instead of simply calling stopService(OngoingCallService::class), which can end up with a crash if it happens
                        // before the service calls startForeground, we call the startService command with an empty data or some specific
                        // argument that tells the service that it should stop itself right after calling startForeground.
                        // This way, when this service is killed and recreated by the system, it will stop itself right after recreating,
                        // so it won't cause any problems.
                        startService(OngoingCallService.newIntentToStop(context))
                    } else {
                        it.last().let { ongoingCallData ->
                            appLogger.i("ServicesManager: starting OngoingCallService")
                            startService(OngoingCallService.newIntent(context, ongoingCallData))
                        }
                    }
                }
        }
    }

    // Ongoing call
    fun startOngoingCallService(ongoingCallData: OngoingCallData) {
        scope.launch {
            appLogger.i("ServicesManager: starting OngoingCallService for user:${ongoingCallData.userId.toLogString()}")
            ongoingCallServiceForUsers.update { it.filter { it.userId != ongoingCallData.userId } + ongoingCallData }
        }
    }

    fun stopOngoingCallServiceForUser(userId: UserId) {
        scope.launch {
            appLogger.i("ServicesManager: stopping OngoingCallService for user:${userId.toLogString()}")
            ongoingCallServiceForUsers.update { it.filter { it.userId != userId } }
        }
    }

    fun stopOngoingCallServiceForAll() {
        scope.launch {
            appLogger.i("ServicesManager: stopping OngoingCallService for all users")
            ongoingCallServiceForUsers.emit(emptyList())
        }
    }

    // Persistent WebSocket
    fun startPersistentWebSocketService() {
        startService(PersistentWebSocketService.newIntent(context))
    }

    fun stopPersistentWebSocketService() {
        stopService(PersistentWebSocketService::class)
    }

    fun isPersistentWebSocketServiceRunning(): Boolean =
        PersistentWebSocketService.isServiceStarted

    private fun startService(intent: Intent) {
        appLogger.i("ServicesManager: starting service for $intent")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopService(serviceClass: KClass<out Service>) {
        context.stopService(Intent(context, serviceClass.java))
    }

    companion object {
        private const val DEBOUNCE_TIME = 200L
    }
}
