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

import com.wire.kalium.network.NetworkState
import com.wire.kalium.network.NetworkStateObserver
import kotlinx.coroutines.flow.MutableStateFlow

class TestNetworkStateObserver(initialState: NetworkState = NetworkState.ConnectedWithInternet) : NetworkStateObserver {

    private val networkState = MutableStateFlow(initialState)

    override fun observeNetworkState(): MutableStateFlow<NetworkState> = networkState

    suspend fun updateNetworkState(state: NetworkState) { networkState.emit(state) }

    companion object {
        val DEFAULT_TEST_NETWORK_STATE_OBSERVER = TestNetworkStateObserver()
    }
}
