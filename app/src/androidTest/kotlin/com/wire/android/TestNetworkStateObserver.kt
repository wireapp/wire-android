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

package com.wire.android

import com.wire.kalium.network.CurrentNetwork
import com.wire.kalium.network.NetworkState
import com.wire.kalium.network.NetworkStateObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TestNetworkStateObserver(
    initialState: NetworkState = NetworkState.ConnectedWithInternet,
    currentNetwork: CurrentNetwork? = CurrentNetwork("0", CurrentNetwork.Type.UNKNOWN, true)
) : NetworkStateObserver {

    private val networkState = MutableStateFlow(initialState)
    private val currentNetwork = MutableStateFlow(currentNetwork)

    override fun observeNetworkState(): MutableStateFlow<NetworkState> = networkState
    override fun observeCurrentNetwork(): StateFlow<CurrentNetwork?> = currentNetwork

    suspend fun updateNetworkState(state: NetworkState) { networkState.emit(state) }
    suspend fun updateCurrentNetwork(network: CurrentNetwork?) { currentNetwork.emit(network) }

    companion object {
        val DEFAULT_TEST_NETWORK_STATE_OBSERVER = TestNetworkStateObserver()
    }
}
