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
 *
 *
 */

package com.wire.android.ui.common.topappbar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.feature.call.Call
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class CommonTopAppBarBaseViewModel : ViewModel()

@HiltViewModel
class CommonTopAppBarViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val establishedCalls: ObserveEstablishedCallsUseCase,
    private val currentScreenManager: CurrentScreenManager,
    private val observeSyncState: ObserveSyncStateUseCase,
) : CommonTopAppBarBaseViewModel() {

    var connectivityState by mutableStateOf(ConnectivityUIState(ConnectivityUIState.Info.None))

    init {
        viewModelScope.launch {
            combine(activeCallFlow(), currentScreenFlow(), connectivityFlow()) { activeCall, currentScreen, connectivity ->
                mapToUIState(currentScreen, connectivity, activeCall)
            }.collectLatest {
                /**
                 * Adding some delay here to avoid some bad UX : ongoing call banner displayed and
                 * hided in a short time when the user hangs up the call
                 * Call events could take some time to be received and this function
                 * could be called when the screen is changed, so we delayed
                 * showing the banner until getting the correct calling values
                 */
                if (it is ConnectivityUIState.Info.EstablishedCall)
                    delay(WAITING_TIME_TO_SHOW_ONGOING_CALL_BANNER)
                connectivityState = connectivityState.copy(info = it)
            }
        }
    }

    private fun mapToUIState(
        currentScreen: CurrentScreen,
        connectivity: Connectivity,
        activeCall: Call?
    ): ConnectivityUIState.Info {
        val canDisplayActiveCall = currentScreen is CurrentScreen.Home || currentScreen is CurrentScreen.Conversation

        // If, for whatever reason Sync is dropped during an active call, the user can see it as well
        val canDisplayConnectivityIssues = canDisplayActiveCall || currentScreen is CurrentScreen.OngoingCallScreen

        val hasConnectivityIssues = connectivity in setOf(Connectivity.CONNECTING, Connectivity.WAITING_CONNECTION)

        return when {
            // Prioritise active call
            activeCall != null && canDisplayActiveCall -> {
                ConnectivityUIState.Info.EstablishedCall(activeCall.conversationId, activeCall.isMuted)
            }

            hasConnectivityIssues && canDisplayConnectivityIssues -> {
                if (connectivity == Connectivity.WAITING_CONNECTION) {
                    ConnectivityUIState.Info.WaitingConnection
                } else {
                    ConnectivityUIState.Info.Connecting
                }
            }

            else -> ConnectivityUIState.Info.None
        }
    }

    fun openOngoingCallScreen() {
        (connectivityState.info as? ConnectivityUIState.Info.EstablishedCall)?.conversationId?.let { convId ->
            viewModelScope.launch {
                navigationManager.navigate(
                    command = NavigationCommand(
                        destination = NavigationItem.OngoingCall.getRouteWithArgs(listOf(convId))
                    )
                )
            }
        }
    }

    private fun connectivityFlow() = observeSyncState().map {
        when (it) {
            is SyncState.Failed, SyncState.Waiting -> Connectivity.WAITING_CONNECTION
            SyncState.GatheringPendingEvents, SyncState.SlowSync -> Connectivity.CONNECTING
            SyncState.Live -> Connectivity.CONNECTED
        }
    }

    private suspend fun activeCallFlow() = establishedCalls().distinctUntilChanged().map { calls ->
        calls.firstOrNull()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun currentScreenFlow() = currentScreenManager.observeCurrentScreen(viewModelScope)

    private companion object {
        const val WAITING_TIME_TO_SHOW_ONGOING_CALL_BANNER = 600L
    }
}
