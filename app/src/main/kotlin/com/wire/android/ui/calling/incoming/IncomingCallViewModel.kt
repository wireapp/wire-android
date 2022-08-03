package com.wire.android.ui.calling.incoming

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.media.CallRinger
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.notification.CallNotificationManager
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.call.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.GetIncomingCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.RejectCallUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    qualifiedIdMapper: QualifiedIdMapper,
    private val incomingCalls: GetIncomingCallsUseCase,
    private val rejectCall: RejectCallUseCase,
    private val acceptCall: AnswerCallUseCase,
    private val callRinger: CallRinger,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val endCall: EndCallUseCase,
    notificationManager: CallNotificationManager
) : ViewModel() {

    private val incomingCallConversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!
    )

    lateinit var observeIncomingCallJob: Job
    var establishedCallConversationId: ConversationId? = null

    init {
        notificationManager.hideCallNotification()

        viewModelScope.launch {
            callRinger.ring(R.raw.ringing_from_them)
            observeIncomingCallJob = launch {
                observeIncomingCall()
            }
            launch {
                observeEstablishedCall()
            }
        }
    }

    private fun observeEstablishedCall() = viewModelScope.launch {
        observeEstablishedCalls().collect {
            if (it.isNotEmpty()) {
                establishedCallConversationId = it.first().conversationId
            }
        }
    }

    private suspend fun observeIncomingCall() {
        incomingCalls().collect { calls ->
            calls.find { call -> call.conversationId == incomingCallConversationId }.also {
                if (it == null) {
                    onCallClosed()
                }
            }
        }
    }

    private fun onCallClosed() {
        viewModelScope.launch {
            navigationManager.navigateBack()
            callRinger.stop()
        }
    }

    fun declineCall() {
        viewModelScope.launch {
            observeIncomingCallJob.cancel()
            launch { rejectCall(conversationId = incomingCallConversationId) }
            launch {
                navigationManager.navigateBack()
                callRinger.stop()
            }
        }
    }

    fun acceptCall() {
        callRinger.stop()
        viewModelScope.launch {
            var backStackNode = ACCEPT_CALL_DEFAULT_BACKSTACK_NODE
            var delayTime = ACCEPT_CALL_DEFAULT_DELAY_TIME
            // if there is already an active call, then end it to accept the new incoming call
            establishedCallConversationId?.let {
                endCall(it)
                backStackNode = BackStackMode.UPDATE_EXISTED
                delayTime = DELAY_TIME_AFTER_ENDING_CALL
            }
            observeIncomingCallJob.cancel()

            acceptCall(conversationId = incomingCallConversationId)
            // We need to add some delay, for the case of joining a call while there is an active one,
            // to get all values returned by avs callbacks
            delay(delayTime)
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.OngoingCall.getRouteWithArgs(listOf(incomingCallConversationId)),
                    backStackMode = backStackNode
                )
            )
        }
    }

    companion object {
        private const val DELAY_TIME_AFTER_ENDING_CALL = 1000L
        private val ACCEPT_CALL_DEFAULT_BACKSTACK_NODE = BackStackMode.REMOVE_CURRENT
        private const val ACCEPT_CALL_DEFAULT_DELAY_TIME = 0L
    }
}
