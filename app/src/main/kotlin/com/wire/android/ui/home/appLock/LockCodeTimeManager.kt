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
 */
package com.wire.android.ui.home.appLock

import com.wire.android.appLogger
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.ApplicationScope
import com.wire.android.feature.AppLockConfig
import com.wire.android.feature.ObserveAppLockConfigUseCase
import com.wire.android.util.CurrentScreenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppLockManager provides a mechanism to determine if the app should be locked based on configuration and screen state.
 *
 * - [isLockedFlow] observes conditions and returns:
 *   - false if app lock is disabled.
 *   - true after a background delay if app lock is enabled.
 *   - false if brought back to the foreground before the delay.
 */

@Singleton
class LockCodeTimeManager @Inject constructor(
    @ApplicationScope private val appCoroutineScope: CoroutineScope,
    currentScreenManager: CurrentScreenManager,
    observeAppLockConfigUseCase: ObserveAppLockConfigUseCase,
    globalDataStore: GlobalDataStore,
    currentTimestamp: CurrentTimestampProvider,
) {

    private var isLockedFlow: MutableStateFlow<Boolean>
    private var lockTimeoutStarted: Long? = null

    init {
        runBlocking {
            val initialValue = globalDataStore.isAppLockPasscodeSetFlow().firstOrNull() ?: false
            isLockedFlow = MutableStateFlow(initialValue)
        }

        // next, listen for app lock config and app visibility changes to determine if app should be locked
        appCoroutineScope.launch {
            combine(
                observeAppLockConfigUseCase(),
                currentScreenManager.isAppVisibleFlow(),
                ::Pair
            )
                .distinctUntilChanged()
                .flatMapLatest { (appLockConfig, isInForeground) ->
                    when {
                        appLockConfig is AppLockConfig.Disabled -> flowOf(false)

                        !isInForeground && !isLockedFlow.value && appLockConfig is AppLockConfig.Enabled -> flow {
                            appLogger.i("$TAG lock is enabled and app in the background, lock count started")
                            if (lockTimeoutStarted == null) {
                                lockTimeoutStarted = currentTimestamp()
                            }
                            delay(appLockConfig.timeout.inWholeMilliseconds)
                            appLogger.i("$TAG lock count ended while app in the background, app state should be locked")
                            emit(true)
                        }

                        isInForeground && !isLockedFlow.value && appLockConfig is AppLockConfig.Enabled -> flow {
                            if (lockTimeoutStarted != null
                                && (lockTimeoutStarted!! + appLockConfig.timeout.inWholeMilliseconds) < currentTimestamp()
                            ) {
                                appLogger.i("$TAG app put into foreground and lock count ended, app state should be locked")
                                emit(true)
                            } else {
                                appLogger.i("$TAG app put into foreground but lock count hasn't ended, app state should not be changed")
                                emit(false)
                                lockTimeoutStarted = null
                            }
                        }

                        else -> {
                            appLogger.i("$TAG no change to lock state, isInForeground: $isInForeground, isLocked: ${isLockedFlow.value}")
                            emptyFlow()
                        }
                    }
                }.collectLatest {
                    isLockedFlow.value = it
                }
        }
    }

    fun appUnlocked() {
        appLogger.i("$TAG app unlocked")
        isLockedFlow.value = false
        lockTimeoutStarted = null
    }

    fun isAppLocked(): Boolean = isLockedFlow.value

    fun observeAppLock(): Flow<Boolean> = isLockedFlow

    companion object {
        private const val TAG = "LockCodeTimeManager"
    }
}

typealias CurrentTimestampProvider = () -> Long
