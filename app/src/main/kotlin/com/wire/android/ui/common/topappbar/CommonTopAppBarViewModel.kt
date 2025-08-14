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

package com.wire.android.ui.common.topappbar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.sync.SyncState.Failed
import com.wire.kalium.logic.data.sync.SyncState.GatheringPendingEvents
import com.wire.kalium.logic.data.sync.SyncState.Live
import com.wire.kalium.logic.data.sync.SyncState.SlowSync
import com.wire.kalium.logic.data.sync.SyncState.Waiting
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.network.NetworkState
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import javax.inject.Inject

@HiltViewModel
class CommonTopAppBarViewModel @Inject constructor(
    private val currentScreenManager: CurrentScreenManager,
    @KaliumCoreLogic private val coreLogic: Lazy<CoreLogic>,
) : ViewModel() {

    var state by mutableStateOf(CommonTopAppBarState())
        private set

    private suspend fun currentScreenFlow() =
        currentScreenManager.observeCurrentScreen(viewModelScope)

    private fun connectivityFlow(userId: UserId): Flow<Connectivity> =
        coreLogic.get().sessionScope(userId) {
            combine(observeSyncState(), coreLogic.get().networkStateObserver.observeNetworkState()) { syncState, networkState ->
                when (syncState) {
                    is Waiting -> Connectivity.WaitingConnection(null, null)
                    is Failed -> Connectivity.WaitingConnection(syncState.cause, syncState.retryDelay)
                    is GatheringPendingEvents,
                    is SlowSync -> Connectivity.Connecting

                    is Live ->
                        if (networkState is NetworkState.ConnectedWithInternet) {
                            Connectivity.Connected
                        } else {
                            Connectivity.WaitingConnection(null, null)
                        }
                }
            }
        }

    @VisibleForTesting
    internal suspend fun activeCallsFlow(userId: UserId): Flow<List<Call>> =
        coreLogic.get().sessionScope(userId) {
            combine(
                calls.establishedCall(),
                calls.getIncomingCalls(),
                calls.observeOutgoingCall(),
            ) { establishedCall, incomingCalls, outgoingCalls ->
                establishedCall + incomingCalls + outgoingCalls
            }.distinctUntilChanged()
        }

    init {
        viewModelScope.launch {
            coreLogic.get().globalScope {
                session.currentSessionFlow()
                    .flatMapLatest {
                        when (it) {
                            is CurrentSessionResult.Failure.Generic,
                            is CurrentSessionResult.Failure.SessionNotFound -> flowOf(
                                ConnectivityUIState.None
                            )

                            is CurrentSessionResult.Success -> {
                                val userId = it.accountInfo.userId
                                combine(
                                    activeCallsFlow(userId),
                                    currentScreenFlow(),
                                    connectivityFlow(userId),
                                ) { activeCalls, currentScreen, connectivity ->
                                    mapToConnectivityUIState(currentScreen, connectivity, userId, activeCalls)
                                }
                            }
                        }
                    }
                    .debounce { state ->
                        /**
                         * Adding some debounce here to avoid some bad UX and prevent from having blinking effect when the state changes
                         * quickly, e.g. when displaying ongoing call banner and hiding it in a short time when the user hangs up the call.
                         * Call events could take some time to be received and this function could be called when the screen is changed,
                         * so we delayed showing the banner until getting the correct calling values and for calls this debounce is bigger
                         * than for other states in order to allow for the correct handling of hanging up a call.
                         * When state changes to None, handle it immediately, that's why we return 0L debounce time in this case.
                         */
                        when {
                            state is ConnectivityUIState.None -> 0L
                            state is ConnectivityUIState.Calls && state.hasOngoingCall -> CONNECTIVITY_STATE_DEBOUNCE_ONGOING_CALL
                            else -> CONNECTIVITY_STATE_DEBOUNCE_DEFAULT
                        }
                    }
                    .collectLatest { connectivityUIState ->
                        state = state.copy(connectivityState = connectivityUIState)
                    }
            }
            coreLogic.get().networkStateObserver.observeNetworkState().collectLatest {
                state = state.copy(networkState = it)
            }
        }
    }

    private fun mapToConnectivityUIState(
        currentScreen: CurrentScreen,
        connectivity: Connectivity,
        userId: UserId,
        activeCalls: List<Call>,
    ): ConnectivityUIState {

        val canDisplayConnectivityIssues = currentScreen !is CurrentScreen.AuthRelated
        if (activeCalls.isNotEmpty()) {
            return ConnectivityUIState.Calls(
                calls = activeCalls.partition { it.status != CallStatus.INCOMING }
                    .let { (outgoingAndEstablished, incoming) ->
                        // outgoing and established first
                        (outgoingAndEstablished + incoming).map { call ->
                            when (call.status) {
                                CallStatus.INCOMING -> ConnectivityUIState.Call.Incoming(userId, call.conversationId, call.callerName)
                                CallStatus.STARTED -> ConnectivityUIState.Call.Outgoing(userId, call.conversationId, call.conversationName)
                                else -> ConnectivityUIState.Call.Established(userId, call.conversationId, call.isMuted)
                            }
                        }
                    }
            )
        }

        return if (canDisplayConnectivityIssues) {
            when (connectivity) {
                Connectivity.Connecting -> ConnectivityUIState.Connecting
                Connectivity.Connected -> ConnectivityUIState.None
                is Connectivity.WaitingConnection -> ConnectivityUIState.WaitingConnection(
                    connectivity.cause,
                    connectivity.retryDelay,
                )
            }
        } else {
            ConnectivityUIState.None
        }
    }

    private companion object {
        const val CONNECTIVITY_STATE_DEBOUNCE_ONGOING_CALL = 600L
        const val CONNECTIVITY_STATE_DEBOUNCE_DEFAULT = 200L
    }
}
