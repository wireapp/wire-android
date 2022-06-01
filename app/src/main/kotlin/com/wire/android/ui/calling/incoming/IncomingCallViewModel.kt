package com.wire.android.ui.calling.incoming

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.media.CallRinger
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.calling.getConversationName
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.feature.call.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.CallStatus
import com.wire.kalium.logic.feature.call.usecase.GetAllCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.RejectCallUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val conversationDetails: ObserveConversationDetailsUseCase,
    private val allCalls: GetAllCallsUseCase,
    private val rejectCall: RejectCallUseCase,
    private val acceptCall: AnswerCallUseCase,
    private val callRinger: CallRinger
) : ViewModel() {
    var callState by mutableStateOf(IncomingCallState())
        private set

    private val conversationId: ConversationId = savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!.parseIntoQualifiedID()

    init {
        viewModelScope.launch {
            callRinger.ring(R.raw.ringing_from_them)
            val conversationDetailsFlow = conversationDetails(conversationId)
                .shareIn(this, SharingStarted.WhileSubscribed(), 1)

            launch {
                conversationDetailsFlow.collect { initializeScreenState(conversationDetails = it) }
            }
            launch {
                observeIncomingCall()
            }
        }
    }

    private suspend fun observeIncomingCall() {
        allCalls()
            .collect { calls ->
                val currentCall = calls.firstOrNull { call -> call.conversationId == conversationId }

                when (currentCall?.status) {
                    CallStatus.CLOSED -> onCallClosed()
                    else -> appLogger.i("Incoming call: call status was changed to ${currentCall?.status}, DO NOTHING")
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
            navigationManager.navigateBack()
            acceptCall(conversationId = conversationId)
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.OngoingCall.getRouteWithArgs(listOf(conversationId))
                )
            )
        }
    }

    private fun initializeScreenState(conversationDetails: ConversationDetails) {
        callState = when (conversationDetails) {
            is ConversationDetails.Group -> callState.copy(
                conversationName = getConversationName(conversationDetails.conversation.name)
            )
            is ConversationDetails.OneOne -> {
                callState.copy(
                    conversationName = getConversationName(conversationDetails.otherUser.name),
                    avatarAssetId = conversationDetails.otherUser.completePicture?.let { UserAvatarAsset(it) }
                )
            }
            is ConversationDetails.Self -> throw IllegalStateException("Invalid conversation type")
        }
    }
}
