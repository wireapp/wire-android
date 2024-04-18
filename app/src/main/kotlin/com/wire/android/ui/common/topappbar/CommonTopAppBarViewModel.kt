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
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommonTopAppBarViewModel @Inject constructor(
    private val currentScreenManager: CurrentScreenManager,
    @KaliumCoreLogic
    private val coreLogic: CoreLogic,
) : ViewModel() {

    var state by mutableStateOf(CommonTopAppBarState())
        private set

    private suspend fun currentScreenFlow() = currentScreenManager.observeCurrentScreen(viewModelScope)

    private fun connectivityFlow(userId: UserId): Flow<Connectivity> = coreLogic.sessionScope(userId) {
        observeSyncState().map {
            when (it) {
                is SyncState.Failed, SyncState.Waiting -> Connectivity.WAITING_CONNECTION
                SyncState.GatheringPendingEvents, SyncState.SlowSync -> Connectivity.CONNECTING
                SyncState.Live -> Connectivity.CONNECTED
            }
        }
    }

    private suspend fun activeCallFlow(userId: UserId): Flow<Call?> = coreLogic.sessionScope(userId) {
        combine(
            calls.establishedCall(),
            calls.getIncomingCalls(),
            calls.observeOutgoingCall(),
        ) { establishedCall, incomingCalls, outgoingCalls ->
            incomingCalls + outgoingCalls + establishedCall
        }.map { calls ->
            calls.firstOrNull()
        }.distinctUntilChanged()
    }

    init {
        viewModelScope.launch {
            coreLogic.globalScope {
                session.currentSessionFlow().flatMapLatest {
                    when (it) {
                        is CurrentSessionResult.Failure.Generic,
                        is CurrentSessionResult.Failure.SessionNotFound -> flowOf(ConnectivityUIState.None)

                        is CurrentSessionResult.Success -> {
                            val userId = it.accountInfo.userId
                            combine(
                                activeCallFlow(userId),
                                currentScreenFlow(),
                                connectivityFlow(userId),
                            ) { activeCall, currentScreen, connectivity ->
                                mapToConnectivityUIState(currentScreen, connectivity, activeCall)
                            }
                        }
                    }
                }.collectLatest { connectivityUIState ->
                    /**
                     * Adding some delay here to avoid some bad UX : ongoing call banner displayed and
                     * hided in a short time when the user hangs up the call
                     * Call events could take some time to be received and this function
                     * could be called when the screen is changed, so we delayed
                     * showing the banner until getting the correct calling values
                     */
                    if (connectivityUIState is ConnectivityUIState.EstablishedCall) {
                        delay(WAITING_TIME_TO_SHOW_ONGOING_CALL_BANNER)
                    }
                    state = state.copy(connectivityState = connectivityUIState)
                }
            }
        }
    }

    private fun mapToConnectivityUIState(
        currentScreen: CurrentScreen,
        connectivity: Connectivity,
        activeCall: Call?
    ): ConnectivityUIState {

        val canDisplayConnectivityIssues = currentScreen !is CurrentScreen.AuthRelated

        if (activeCall != null) {
            return if(activeCall.status == CallStatus.INCOMING) {
                ConnectivityUIState.IncomingCall(activeCall.conversationId, activeCall.callerName)
            } else if(activeCall.status == CallStatus.STARTED) {
                ConnectivityUIState.OutgoingCall(activeCall.conversationId, activeCall.callerName)
            } else {
                ConnectivityUIState.EstablishedCall(activeCall.conversationId, activeCall.isMuted)
            }
        }

        return if (canDisplayConnectivityIssues) {
            when (connectivity) {
                Connectivity.WAITING_CONNECTION -> ConnectivityUIState.WaitingConnection
                Connectivity.CONNECTING -> ConnectivityUIState.Connecting
                Connectivity.CONNECTED -> ConnectivityUIState.None
            }
        } else {
            ConnectivityUIState.None
        }
    }

    private companion object {
        const val WAITING_TIME_TO_SHOW_ONGOING_CALL_BANNER = 600L
    }
}
