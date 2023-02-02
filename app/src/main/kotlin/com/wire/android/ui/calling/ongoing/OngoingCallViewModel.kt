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

package com.wire.android.ui.calling.ongoing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.data.call.CallClient
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.RequestVideoStreamsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class OngoingCallViewModel @OptIn(ExperimentalCoroutinesApi::class)
@Inject constructor(
    savedStateHandle: SavedStateHandle,
    qualifiedIdMapper: QualifiedIdMapper,
    private val navigationManager: NavigationManager,
    private val establishedCall: ObserveEstablishedCallsUseCase,
    private val requestVideoStreams: RequestVideoStreamsUseCase,
    private val currentScreenManager: CurrentScreenManager,
) : ViewModel() {

    private val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!
    )

    init {
        viewModelScope.launch {
            establishedCall().first { it.isNotEmpty() }.run {
                // We start observing once we have an ongoing call
                observeCurrentCall()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observeCurrentCall() {
        establishedCall()
            .distinctUntilChanged()
            .collect { calls ->
                val currentCall = calls.find { call -> call.conversationId == conversationId }
                val currentScreen = currentScreenManager.observeCurrentScreen(viewModelScope).first()
                val isCurrentlyOnOngoingScreen = currentScreen is CurrentScreen.OngoingCallScreen
                val isOnBackground = currentScreen is CurrentScreen.InBackground
                if (currentCall == null && (isCurrentlyOnOngoingScreen || isOnBackground)) {
                    navigateBack()
                }
            }
    }

    fun requestVideoStreams(participants: List<UICallParticipant>) {
        viewModelScope.launch {
            val clients: List<CallClient> = participants.map {
                CallClient(it.id.toString(), it.clientId)
            }
            requestVideoStreams(conversationId, clients)
        }
    }

    private suspend fun navigateBack() {
        navigationManager.navigateBack()
    }
}
