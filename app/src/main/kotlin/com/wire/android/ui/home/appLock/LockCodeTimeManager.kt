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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
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
    val currentScreenManager: CurrentScreenManager,
    val observeAppLockConfigUseCase: ObserveAppLockConfigUseCase,
    val globalDataStore: GlobalDataStore,
) {

    private lateinit var isLockedFlow: MutableStateFlow<Boolean>

    init {
        runBlocking {
            val initialValue = globalDataStore.isAppLockPasscodeSetFlow().firstOrNull() ?: false
            isLockedFlow = MutableStateFlow(initialValue)
        }

        appCoroutineScope.launch {
            currentScreenManager.isAppVisibleFlow().collectLatest { isAppVisible ->
                // if app is not visible and not locked, set the time when app went to background
                if (!isAppVisible && !isLockedFlow.value) {
                    appLogger.i("$TAG setAppWentToBackgroundAt called")
                    globalDataStore.setAppWentToBackgroundAt(Clock.System.now().epochSeconds)
                }
            }
        }
    }

    private fun shouldLockApp(): Flow<Boolean> = currentScreenManager.isAppVisibleFlow().map {
        if (it) {
            return@map observeAppLockConfigUseCase().map { appLockConfig ->
                val now = Clock.System.now().epochSeconds
                when (appLockConfig) {
                    is AppLockConfig.Disabled -> {
                        appLogger.i("$TAG app lock config: Disabled")
                        false
                    }
                    else -> {
                        val applicationWentToBackgroundAt =
                            globalDataStore.getApplicationWentToBackgroundAt().first()
                        appLogger.i(
                            "$TAG diff: ${now - applicationWentToBackgroundAt} " +
                                    "timeout: ${appLockConfig.timeout.inWholeSeconds}"
                        )
                        now - applicationWentToBackgroundAt >= appLockConfig.timeout.inWholeSeconds
                    }
                }
            }
        } else flowOf(isLockedFlow.value)
    }.map {
        isLockedFlow = MutableStateFlow(it.first())
        it.first()
    }


    fun appUnlocked() {
        appLogger.i("$TAG app unlocked")
        // set future time to avoid app lock on next app start
        // 4858066827 = Sun Dec 12 2123 15:00:27 GMT+0000
        appCoroutineScope.launch {
            globalDataStore.setAppWentToBackgroundAt(4858066827)
        }
        isLockedFlow.value = false
    }

    fun isAppLocked(): Boolean = isLockedFlow.value

    fun observeAppLock(): Flow<Boolean> = shouldLockApp()

    companion object {
        private const val TAG = "LockCodeTimeManager"
    }
}
