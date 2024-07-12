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

package com.wire.android.ui.settings.devices

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.CurrentAccount
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.settings.devices.model.SelfDevicesState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.FetchSelfClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.ObserveClientsByUserIdUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetUserE2eiCertificatesUseCase
import com.wire.kalium.logic.feature.user.IsE2EIEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelfDevicesViewModel @Inject constructor(
    @CurrentAccount val currentAccountId: UserId,
    private val fetchSelfClientsFromRemote: FetchSelfClientsFromRemoteUseCase,
    private val observeClientList: ObserveClientsByUserIdUseCase,
    private val currentClientIdUseCase: ObserveCurrentClientIdUseCase,
    private val getUserE2eiCertificates: GetUserE2eiCertificatesUseCase,
    isE2EIEnabledUseCase: IsE2EIEnabledUseCase
) : ViewModel() {

    var state: SelfDevicesState by mutableStateOf(
        SelfDevicesState(deviceList = listOf(), isLoadingClientsList = true, currentDevice = null, isE2EIEnabled = isE2EIEnabledUseCase())
    )
        private set

    private val refreshE2eiCertificates: MutableSharedFlow<Unit> = MutableSharedFlow<Unit>()
    private val observeUserE2eiCertificates = refreshE2eiCertificates.map { getUserE2eiCertificates(currentAccountId) }

    init {
        observeClientList()
        updateSelfClientsListFromRemote()
    }

    private fun updateSelfClientsListFromRemote() {
        viewModelScope.launch {
            fetchSelfClientsFromRemote()
        }
    }

    private fun observeClientList() {
        viewModelScope.launch {
            observeClientList(currentAccountId)
                .combine(observeUserE2eiCertificates, ::Pair)
                .collect { (result, e2eiCertificates) ->
                    state = when (result) {
                        is ObserveClientsByUserIdUseCase.Result.Failure -> state.copy(isLoadingClientsList = false)
                        is ObserveClientsByUserIdUseCase.Result.Success -> {
                            val currentClientId = currentClientIdUseCase().firstOrNull()
                            state.copy(
                                isLoadingClientsList = false,
                                currentDevice = result.clients
                                    .firstOrNull { it.id == currentClientId }
                                    ?.let { Device(it, e2eiCertificates[it.id.value]) },
                                deviceList = result.clients
                                    .filter { it.id != currentClientId }
                                    .map { Device(it, e2eiCertificates[it.id.value]) }
                            )
                        }
                    }
                }
        }
    }

    fun loadCertificates() {
        viewModelScope.launch {
            refreshE2eiCertificates.emit(Unit)
        }
    }
}
