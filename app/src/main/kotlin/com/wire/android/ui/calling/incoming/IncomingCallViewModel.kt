package com.wire.android.ui.calling.incoming

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.media.CallRinger
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.notification.CallNotificationManager
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.feature.call.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.GetIncomingCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.RejectCallUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val incomingCalls: GetIncomingCallsUseCase,
    private val rejectCall: RejectCallUseCase,
    private val acceptCall: AnswerCallUseCase,
    private val callRinger: CallRinger,
    private val notificationManager: CallNotificationManager
) : ViewModel() {

    private val conversationId: ConversationId = savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!.parseIntoQualifiedID()
    lateinit var observeIncomingCallJob: Job

    init {
        viewModelScope.launch {
            callRinger.ring(R.raw.ringing_from_them)
            observeIncomingCallJob = launch {
                observeIncomingCall()
            }
        }
    }

    private suspend fun observeIncomingCall() {
        incomingCalls().collect { calls ->
            calls.find { call -> call.conversationId == conversationId }.also {
                if (it == null)
                    onCallClosed()
            }
        }
    }

    private fun onCallClosed() {
        viewModelScope.launch {
            navigationManager.navigateBack()
            stopRinging()
        }
    }

    fun declineCall() {
        viewModelScope.launch {
            observeIncomingCallJob.cancel()
            rejectCall(conversationId = conversationId)
            navigationManager.navigateBack()
            stopRinging()
        }
    }

    fun acceptCall() {
        stopRinging()
        viewModelScope.launch {
            observeIncomingCallJob.cancel()
            acceptCall(conversationId = conversationId)
            navigationManager.navigateBack()
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.OngoingCall.getRouteWithArgs(listOf(conversationId))
                )
            )
        }
    }

    private fun stopRinging() {
        callRinger.stop()
        notificationManager.hideCallNotification()
    }
}
