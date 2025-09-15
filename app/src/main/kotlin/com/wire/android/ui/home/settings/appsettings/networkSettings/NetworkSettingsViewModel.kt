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

package com.wire.android.ui.home.settings.appsettings.networkSettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.PersistPersistentWebSocketConnectionStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkSettingsViewModel
@Inject constructor(
    private val persistPersistentWebSocketConnectionStatus: PersistPersistentWebSocketConnectionStatusUseCase,
    private val observePersistentWebSocketConnectionStatus: ObservePersistentWebSocketConnectionStatusUseCase,
    private val currentSession: CurrentSessionUseCase
) : ViewModel() {
    var networkSettingsState by mutableStateOf(NetworkSettingsState())

    init {
        observePersistentWebSocketConnection()
    }

    private fun observePersistentWebSocketConnection() =
        viewModelScope.launch {

            when (val currentSession = currentSession()) {
                is CurrentSessionResult.Success -> {
                    val userId = currentSession.accountInfo.userId

                    observePersistentWebSocketConnectionStatus().let {
                        when (it) {
                            is ObservePersistentWebSocketConnectionStatusUseCase.Result.Failure -> {
                                appLogger.e("Failure while fetching persistent web socket status flow from network settings")
                            }
                            is ObservePersistentWebSocketConnectionStatusUseCase.Result.Success -> {
                                it.persistentWebSocketStatusListFlow.collect {
                                    it.map { persistentWebSocketStatus ->
                                        if (persistentWebSocketStatus.userId == userId) {
                                            networkSettingsState =
                                                networkSettingsState.copy(
                                                    isPersistentWebSocketConnectionEnabled =
                                                    persistentWebSocketStatus.isPersistentWebSocketEnabled
                                                )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    // NO SESSION - Nothing to do
                }
            }
        }

    fun setWebSocketState(isEnabled: Boolean) {
        viewModelScope.launch {
            persistPersistentWebSocketConnectionStatus(isEnabled)
        }
    }
}
