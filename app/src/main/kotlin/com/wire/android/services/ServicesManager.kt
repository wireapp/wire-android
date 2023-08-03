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
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
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
    dispatcherProvider: DispatcherProvider,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    private val ongoingCallServiceForUsers = MutableStateFlow<List<OngoingCallData>>(emptyList())

    suspend fun currentlyOngoingCall(): Flow<OngoingCallData?> = ongoingCallServiceForUsers
        .combine(
            coreLogic.getGlobalScope().observeValidAccounts()
        ) { ongoingCalls: List<OngoingCallData>, validAccounts: List<Pair<SelfUser, Team?>> ->
            ongoingCalls.lastOrNull { ongoingCallData ->
                // filter out calls that are for accounts that are not valid anymore
                validAccounts.any { (selfUser, _) -> selfUser.id == ongoingCallData.userId }
            }
        }
        .debounce { if (it == null) 0L else DEBOUNCE_TIME } // debounce to avoid starting and stopping service too fast
        .distinctUntilChanged()

    init {
        scope.launch {
            currentlyOngoingCall()
                // we only want to start the service for the first ongoing call and let it handle keeping itself alive
                .distinctUntilChangedBy { it != null }
                .collectLatest { ongoingCallData: OngoingCallData? ->
                    if (ongoingCallData == null) {
                        appLogger.i("ServicesManager: stopping OngoingCallService because there are no ongoing calls")
                        when (OngoingCallService.serviceState.get()) {
                            OngoingCallService.ServiceState.STARTED -> {
                                // Instead of simply calling stopService(OngoingCallService::class), which can end up with a crash if it
                                // happens before the service calls startForeground, we call the startService command with an empty data
                                // or some specific argument that tells the service that it should stop itself right after startForeground.
                                // This way, when this service is killed and recreated by the system, it will stop itself right after
                                // recreating so it won't cause any problems.
                                startService(OngoingCallService.newIntentToStop(context))
                                appLogger.i("ServicesManager: OngoingCallService stopped by passing stop argument")
                            }
                            OngoingCallService.ServiceState.FOREGROUND -> {
                                // we can just stop the service, because it's already in foreground
                                context.stopService(OngoingCallService.newIntent(context))
                                appLogger.i("ServicesManager: OngoingCallService stopped by calling stopService")
                            }
                            else -> {
                                appLogger.i("ServicesManager: OngoingCallService not running, nothing to stop")
                            }
                        }
                    } else {
                        appLogger.i("ServicesManager: starting OngoingCallService")
                        startService(OngoingCallService.newIntent(context))
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
        @VisibleForTesting
        internal const val DEBOUNCE_TIME = 200L
    }
}
