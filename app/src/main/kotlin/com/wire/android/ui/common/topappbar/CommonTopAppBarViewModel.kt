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
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.Call
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

abstract class CommonTopAppBarBaseViewModel : ViewModel()

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CommonTopAppBarViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val currentScreenManager: CurrentScreenManager,
    @KaliumCoreLogic
    private val coreLogic: CoreLogic,
) : CommonTopAppBarBaseViewModel() {

    var connectivityState by mutableStateOf(ConnectivityUIState(ConnectivityUIState.Info.None))

    init {
        viewModelScope.launch {
            coreLogic.globalScope {
                session.currentSessionFlow().flatMapLatest {
                    when (it) {
                        is CurrentSessionResult.Failure.Generic,
                        CurrentSessionResult.Failure.SessionNotFound -> flowOf(ConnectivityUIState.Info.None)
                        is CurrentSessionResult.Success -> {
                            val userId = it.accountInfo.userId
                            combine(
                                activeCallFlow(userId),
                                currentScreenFlow(),
                                connectivityFlow(userId)
                            ) { activeCall, currentScreen, connectivity ->
                                mapToUIState(currentScreen, connectivity, activeCall)
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
                    if (connectivityUIState is ConnectivityUIState.Info.EstablishedCall) {
                        delay(WAITING_TIME_TO_SHOW_ONGOING_CALL_BANNER)
                    }
                    connectivityState = connectivityState.copy(info = connectivityUIState)
                }
            }
        }
    }

    private fun mapToUIState(
        currentScreen: CurrentScreen,
        connectivity: Connectivity,
        activeCall: Call?
    ): ConnectivityUIState.Info {
        val canDisplayActiveCall = currentScreen !is CurrentScreen.OngoingCallScreen

        if (activeCall != null && canDisplayActiveCall) {
            return ConnectivityUIState.Info.EstablishedCall(activeCall.conversationId, activeCall.isMuted)
        }

        return when (connectivity) {
            Connectivity.WAITING_CONNECTION -> ConnectivityUIState.Info.WaitingConnection
            Connectivity.CONNECTING -> ConnectivityUIState.Info.Connecting
            Connectivity.CONNECTED -> ConnectivityUIState.Info.None
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
        calls.establishedCall().distinctUntilChanged().map { calls ->
            calls.firstOrNull()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun currentScreenFlow() = currentScreenManager.observeCurrentScreen(viewModelScope)

    private companion object {
        const val WAITING_TIME_TO_SHOW_ONGOING_CALL_BANNER = 600L
    }
}
