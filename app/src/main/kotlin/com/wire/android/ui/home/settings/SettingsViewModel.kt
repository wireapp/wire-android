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
package com.wire.android.ui.home.settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.logging.LogFileWriter
import com.wire.kalium.logic.data.mfa.MFAOverview
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppLockEditableUseCase
import com.wire.kalium.logic.feature.mfa.ObserveMFAOverviewUseCase
import com.wire.kalium.logic.feature.mfa.RefreshMFAOverviewUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import dev.zacsweers.metro.Inject
class SettingsViewModel @Inject constructor(
    private val globalDataStore: GlobalDataStore,
    private val observeIsAppLockEditable: ObserveIsAppLockEditableUseCase,
    private val getSelf: ObserveSelfUserUseCase,
    private val dispatchers: DispatcherProvider,
    private val logFileWriter: LogFileWriter,
    private val observeMFAOverview: ObserveMFAOverviewUseCase,
    private val refreshMFAOverview: RefreshMFAOverviewUseCase,
) : ViewModel() {
    var state by mutableStateOf(SettingsState())
        private set
    init {
        viewModelScope.launch {
            combine(
                observeIsAppLockEditable(),
                globalDataStore.isAppLockPasscodeSetFlow()
            ) {
                isAppLockEditable, isAppLockEnabled ->
                state.copy(
                    isAppLockEditable = isAppLockEditable,
                    isAppLockEnabled = isAppLockEnabled
                )
            }.collect { state = it }
        }
        viewModelScope.launch {
            fetchSelfUser()
        }
        if (BuildConfig.MULTI_FACTOR_AUTHENTICATION_ENABLED) {
            observeMFASettings()
        }
    }
    fun disableAppLock() {
        viewModelScope.launch {
            globalDataStore.clearAppLockPasscode()
        }
    }

    fun flushLogs(): Deferred<Unit> = viewModelScope.async {
        logFileWriter.forceFlush()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun observeMFASettings() {
        viewModelScope.launch {
            observeMFAOverview().collect { overview ->
                state = state.copy(
                    isMFAEnabled = when (overview) {
                        null -> null
                        MFAOverview.Disabled -> false
                        is MFAOverview.Enabled -> true
                    }
                )
            }
        }
        viewModelScope.launch {
            try {
                refreshMFAOverview()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Keep the last known status, or no badge if the status has not loaded yet.
            }
        }
    }

    private suspend fun fetchSelfUser() {
        viewModelScope.launch {
            val self =
                getSelf().flowOn(dispatchers.io()).shareIn(this, SharingStarted.WhileSubscribed(1))
            self.collect { user ->
                state = state.copy(userName = user.name ?: "")
            }
        }
    }
}
