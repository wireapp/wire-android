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

import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.ApplicationScope
import com.wire.android.feature.AppLockConfig
import com.wire.android.feature.ObserveAppLockConfigUseCase
import com.wire.android.util.CurrentScreenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.take
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockCodeTimeManager @Inject constructor(
    @ApplicationScope private val appCoroutineScope: CoroutineScope,
    currentScreenManager: CurrentScreenManager,
    observeAppLockConfigUseCase: ObserveAppLockConfigUseCase,
    globalDataStore: GlobalDataStore,
) {

    @Suppress("MagicNumber")
    private val lockCodeRequiredFlow = globalDataStore.getAppLockTimestampFlow().take(1)
        .flatMapLatest { lastAppLockTimestamp ->
            combine(
                currentScreenManager.isAppVisibleFlow()
                    .scan(AppVisibilityTimestampData(lastAppLockTimestamp ?: -1, false)) { previousData, currentlyVisible ->
                        if (previousData.isAppVisible != currentlyVisible) {
                            val timestamp = if (!currentlyVisible) { // app moved to background
                                System.currentTimeMillis().also {
                                    globalDataStore.setAppLockTimestamp(it)
                                }
                            } else previousData.timestamp
                            AppVisibilityTimestampData(
                                timestamp = timestamp,
                                isAppVisible = currentlyVisible
                            )
                        } else previousData
                    },
                observeAppLockConfigUseCase()
            ) { appVisibilityTimestampData, appLockConfig ->
                appVisibilityTimestampData.isAppVisible
                        && appLockConfig !is AppLockConfig.Disabled
                        && appVisibilityTimestampData.timestamp >= 0
                        && (System.currentTimeMillis() - appVisibilityTimestampData.timestamp) > (appLockConfig.timeoutInSeconds * 1000)
            }
                .distinctUntilChanged()
        }
        .shareIn(scope = appCoroutineScope, started = SharingStarted.Eagerly, replay = 1)

    fun shouldLock(): Flow<Boolean> = lockCodeRequiredFlow

    private data class AppVisibilityTimestampData(
        val timestamp: Long,
        val isAppVisible: Boolean
    )
}
