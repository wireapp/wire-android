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

package com.wire.android.ui.calling.incoming

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.calling.incoming.IncomingCallState.WaitingUnlockState
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.home.appLock.LockCodeTimeManager
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.GetIncomingCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.RejectCallUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
@HiltViewModel(assistedFactory = IncomingCallViewModel.Factory::class)
class IncomingCallViewModel @AssistedInject constructor(
    @Assisted val conversationId: ConversationId,
    private val incomingCalls: GetIncomingCallsUseCase,
    private val rejectCall: RejectCallUseCase,
    private val acceptCall: AnswerCallUseCase,
    private val muteCall: MuteCallUseCase,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val endCall: EndCallUseCase,
    private val lockCodeTimeManager: LockCodeTimeManager
) : ActionsViewModel<IncomingCallViewActions>() {

    private lateinit var observeIncomingCallJob: Job
    private var establishedCallConversationId: ConversationId? = null

    var incomingCallState by mutableStateOf(IncomingCallState())
        private set

    init {
        viewModelScope.launch {
            observeIncomingCallJob = launch {
                observeIncomingCall()
            }
            launch {
                observeEstablishedCall()
            }
            launch {
                observeAppLockStatus()
            }
        }
    }

    private suspend fun observeAppLockStatus() {
        lockCodeTimeManager.observeAppLock().distinctUntilChanged().collectLatest { isLocked ->
            if (!isLocked) {
                when (incomingCallState.waitingUnlockState) {
                    WaitingUnlockState.DEFAULT -> {
                        // do nothing
                    }

                    WaitingUnlockState.JOIN_CALL -> acceptCall()
                    WaitingUnlockState.JOIN_CALL_ANYWAY -> acceptCallAnyway()
                    WaitingUnlockState.DECLINE_CALL -> declineCall()
                }
                incomingCallState = incomingCallState.copy(waitingUnlockState = WaitingUnlockState.DEFAULT)
            }
        }
    }

    private fun observeEstablishedCall() = viewModelScope.launch {
        observeEstablishedCalls()
            .distinctUntilChanged()
            .collect {
                val hasEstablishedCall = it.isNotEmpty()
                establishedCallConversationId = if (it.isNotEmpty()) {
                    it.first().conversationId
                } else null
                incomingCallState = incomingCallState.copy(hasEstablishedCall = hasEstablishedCall)
            }
    }

    private suspend fun observeIncomingCall() {
        incomingCalls().distinctUntilChanged().collect { calls ->
            calls.find { call -> call.conversationId == conversationId }.also {
                if (it == null) {
                    incomingCallState =
                        incomingCallState.copy(flowState = IncomingCallState.FlowState.CallClosed)
                }
            }
        }
    }

    fun declineCall() {
        viewModelScope.launch {
            lockCodeTimeManager.observeAppLock().first().let {
                if (it) {
                    incomingCallState = incomingCallState.copy(waitingUnlockState = WaitingUnlockState.DECLINE_CALL)
                    sendAction(IncomingCallViewActions.AppLocked)
                } else {
                    observeIncomingCallJob.cancel()
                    launch { rejectCall(conversationId = conversationId) }
                    launch {
                        incomingCallState =
                            incomingCallState.copy(flowState = IncomingCallState.FlowState.CallClosed)
                    }
                    sendAction(IncomingCallViewActions.RejectedCall(conversationId))
                }
            }
        }
    }

    private fun showJoinCallAnywayDialog() {
        incomingCallState = incomingCallState.copy(shouldShowJoinCallAnywayDialog = true)
    }

    fun dismissJoinCallAnywayDialog() {
        incomingCallState = incomingCallState.copy(shouldShowJoinCallAnywayDialog = false)
    }

    fun acceptCallAnyway() {
        viewModelScope.launch {
            lockCodeTimeManager.observeAppLock().first().let {
                if (it) {
                    incomingCallState = incomingCallState.copy(waitingUnlockState = WaitingUnlockState.JOIN_CALL_ANYWAY)
                    sendAction(IncomingCallViewActions.AppLocked)
                } else {
                    establishedCallConversationId?.let {
                        endCall(it)
                        // we need to update mute state to false, so if the user re-join the call te mic will will be muted
                        muteCall(it, false)
                        delay(DELAY_END_CALL)
                    }
                    acceptCall()
                }
            }
        }
    }

    fun acceptCall() {
        viewModelScope.launch {
            lockCodeTimeManager.observeAppLock().first().let {
                if (it) {
                    incomingCallState =
                        incomingCallState.copy(waitingUnlockState = WaitingUnlockState.JOIN_CALL)
                    sendAction(IncomingCallViewActions.AppLocked)
                } else {
                    if (incomingCallState.hasEstablishedCall) {
                        showJoinCallAnywayDialog()
                    } else {
                        dismissJoinCallAnywayDialog()
                        observeIncomingCallJob.cancel()

                        acceptCall(conversationId = conversationId)
                        incomingCallState = incomingCallState.copy(
                            flowState = IncomingCallState.FlowState.CallAccepted(conversationId)
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val DELAY_END_CALL = 200L
    }

    @AssistedFactory
    interface Factory {
        fun create(conversationId: ConversationId): IncomingCallViewModel
    }
}

sealed interface IncomingCallViewActions {
    data object AppLocked : IncomingCallViewActions
    data class RejectedCall(val conversationId: ConversationId) : IncomingCallViewActions
}
