package com.wire.android.ui.calling.incoming

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.media.CallRinger
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.feature.call.AnswerCallUseCase
import com.wire.android.ui.calling.getConversationName
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.feature.call.CallStatus
import com.wire.kalium.logic.feature.call.usecase.GetAllCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.RejectCallUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val allCalls: GetAllCallsUseCase,
    private val rejectCall: RejectCallUseCase,
    private val acceptCall: AnswerCallUseCase,
    private val callRinger: CallRinger
) : ViewModel() {

    val conversationId: ConversationId = savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!.parseIntoQualifiedID()

    init {
        viewModelScope.launch {
            callRinger.ring(R.raw.ringing_from_them)
            launch {
                observeIncomingCall()
            }
        }
    }

    private suspend fun observeIncomingCall() {
        allCalls().collect {
            if (it.first().conversationId == conversationId)
                when (it.first().status) {
                    CallStatus.CLOSED -> onCallClosed()
                    else -> print("DO NOTHING")
                }
        }
    }

    private fun onCallClosed() {
        callRinger.stop()
        viewModelScope.launch { navigationManager.navigateBack() }
    }

    fun declineCall() {
        callRinger.stop()
        viewModelScope.launch {
            rejectCall(conversationId = conversationId)
        }
    }

    fun acceptCall() {
        callRinger.stop()
        viewModelScope.launch {
            acceptCall(conversationId = conversationId)

            navigationManager.navigateBack()
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.OngoingCall.getRouteWithArgs(listOf(conversationId))
                )
            )
        }
    }

}
