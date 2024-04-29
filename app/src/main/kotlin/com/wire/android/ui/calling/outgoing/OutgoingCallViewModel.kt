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

package com.wire.android.ui.calling.outgoing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.media.CallRinger
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.IsLastCallClosedUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOutgoingCallUseCase
import com.wire.kalium.logic.feature.call.usecase.StartCallUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import java.util.Calendar

@Suppress("LongParameterList")
@HiltViewModel(assistedFactory = OutgoingCallViewModel.Factory::class)
class OutgoingCallViewModel @AssistedInject constructor(
    @Assisted val conversationId: ConversationId,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val observeOutgoingCall: ObserveOutgoingCallUseCase,
    private val startCall: StartCallUseCase,
    private val endCall: EndCallUseCase,
    private val isLastCallClosed: IsLastCallClosedUseCase,
    private val callRinger: CallRinger
) : ViewModel() {

    private val callStartTime: Long = Calendar.getInstance().timeInMillis
    private var wasCallHangUp: Boolean = false

    var state by mutableStateOf(OutgoingCallState())
        private set

    init {
        viewModelScope.launch {
            launch { initiateCall() }
            launch { observeStartedCall() }
            launch { observeClosedCall() }
        }
    }

    private suspend fun observeStartedCall() {
        observeEstablishedCalls()
            .map { calls -> calls.map { it.conversationId } }
            .distinctUntilChanged()
            .collect { conversationIds ->
                conversationIds.find { convId ->
                    convId == conversationId
                }?.let {
                    onCallEstablished()
                }
            }
    }

    private suspend fun observeClosedCall() {
        isLastCallClosed(
            conversationId = conversationId,
            startedTime = callStartTime
        ).collect { isCurrentCallClosed ->
            if (isCurrentCallClosed && wasCallHangUp.not()) {
                stopRingerAndMarkCallAsHungUp()
                state = state.copy(flowState = OutgoingCallState.FlowState.CallClosed)
            }
        }
    }

    private fun stopRingerAndMarkCallAsHungUp() {
        wasCallHangUp = true
        callRinger.stop()
    }

    private fun onCallEstablished() {
        callRinger.ring(R.raw.ready_to_talk, isLooping = false, isIncomingCall = false)
        state = state.copy(flowState = OutgoingCallState.FlowState.CallEstablished)
    }

    @VisibleForTesting
    suspend fun initiateCall() {
        observeOutgoingCall().first().run {
            if (isEmpty()) {
                val result = startCall(
                    conversationId = conversationId
                )
                when (result) {
                    StartCallUseCase.Result.Success -> callRinger.ring(
                        resource = R.raw.ringing_from_me,
                        isIncomingCall = false
                    )

                    StartCallUseCase.Result.SyncFailure -> {
                        // TODO: handle case where start call fails
                        appLogger.i("Failed to start call")
                    }
                }
            }
        }
    }

    fun hangUpCall() = viewModelScope.launch {
        launch { endCall(conversationId) }
        launch {
            stopRingerAndMarkCallAsHungUp()
            state = state.copy(flowState = OutgoingCallState.FlowState.CallClosed)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(conversationId: ConversationId): OutgoingCallViewModel
    }
}
