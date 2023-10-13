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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockCodeTimeManager @Inject constructor(
    @ApplicationScope private val appCoroutineScope: CoroutineScope,
    currentScreenManager: CurrentScreenManager,
    observeAppLockConfigUseCase: ObserveAppLockConfigUseCase,
) {

    private val isLockedFlow = MutableStateFlow(false)

    init {
        // first, set initial value - if app lock is enabled then app needs to be locked right away
        runBlocking {
            observeAppLockConfigUseCase().firstOrNull()?.let { appLockConfig ->
                if (appLockConfig !is AppLockConfig.Disabled) {
                    isLockedFlow.value = true
                }
            }
        }
        @Suppress("MagicNumber")
        // next, listen for app lock config and app visibility changes to determine if app should be locked
        appCoroutineScope.launch {
            combine(
                observeAppLockConfigUseCase(),
                currentScreenManager.isAppVisibleFlow(),
                ::Pair
            ).flatMapLatest { (appLockConfig, isInForeground) ->
                when {
                    appLockConfig is AppLockConfig.Disabled -> flowOf(false)

                    !isInForeground && !isLockedFlow.value -> flow {
                        delay(appLockConfig.timeoutInSeconds * 1000L)
                        emit(true)
                    }

                    else -> emptyFlow()
                }
            }.collectLatest {
                isLockedFlow.value = it
            }
        }
    }

    fun appUnlocked() {
        isLockedFlow.value = false
    }

    fun isLocked(): Flow<Boolean> = isLockedFlow
}
