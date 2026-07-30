/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.services

import com.wire.kalium.network.CurrentNetwork
import com.wire.kalium.network.NetworkState
import com.wire.kalium.network.NetworkStateObserver
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class PendingMessagesNetworkStateObserverTest {

    @Test
    fun givenNetworkIsAlreadyConnectedWithInternet_whenWaitingForConnection_thenReturnsConnected() = runTest {
        val observer = TestNetworkStateObserver(NetworkState.ConnectedWithInternet)

        val result = observer.waitUntilConnectedWithInternet(1.seconds)

        assertTrue(result)
    }

    @Test
    fun givenNetworkBecomesConnectedWithInternet_whenWaitingForConnection_thenReturnsConnected() = runTest {
        val observer = TestNetworkStateObserver(NetworkState.NotConnected)
        val result = async {
            observer.waitUntilConnectedWithInternet(1.seconds)
        }

        runCurrent()
        observer.updateNetworkState(NetworkState.ConnectedWithInternet)

        assertTrue(result.await())
    }

    @Test
    fun givenNetworkStaysNotConnected_whenWaitingForConnection_thenReturnsNotConnected() = runTest {
        val observer = TestNetworkStateObserver(NetworkState.NotConnected)
        val result = async {
            observer.waitUntilConnectedWithInternet(1.seconds)
        }

        advanceTimeBy(1.seconds.inWholeMilliseconds + 1)

        assertFalse(result.await())
    }

    @Test
    fun givenNetworkStaysConnectedWithoutInternet_whenWaitingForConnection_thenReturnsNotConnected() = runTest {
        val observer = TestNetworkStateObserver(NetworkState.ConnectedWithoutInternet)
        val result = async {
            observer.waitUntilConnectedWithInternet(1.seconds)
        }

        advanceTimeBy(1.seconds.inWholeMilliseconds + 1)

        assertFalse(result.await())
    }

    private class TestNetworkStateObserver(
        initialState: NetworkState,
    ) : NetworkStateObserver {
        private val networkState = MutableStateFlow(initialState)

        override fun observeNetworkState(): StateFlow<NetworkState> = networkState

        override fun observeCurrentNetwork(): StateFlow<CurrentNetwork?> = MutableStateFlow(null)

        fun updateNetworkState(state: NetworkState) {
            networkState.value = state
        }
    }
}
