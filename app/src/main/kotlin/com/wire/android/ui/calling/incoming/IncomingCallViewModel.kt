package com.wire.android.ui.calling.incoming

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.media.CallRinger
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.calling.getConversationName
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.call.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.CallStatus
import com.wire.kalium.logic.feature.call.usecase.GetAllCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.RejectCallUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val conversationDetails: ObserveConversationDetailsUseCase,
    private val allCalls: GetAllCallsUseCase,
    private val rejectCall: RejectCallUseCase,
    private val acceptCall: AnswerCallUseCase,
    private val callRinger: CallRinger
) : ViewModel() {
    var callState by mutableStateOf(IncomingCallState())
        private set

    private val conversationIdFlow = MutableStateFlow<ConversationId?>(null)

    fun setConversationId(id: ConversationId) {
        conversationIdFlow.value = id
    }

    init {
        viewModelScope.launch {
            callRinger.ring(R.raw.ringing_from_them)
            val conversationDetailsFlow = conversationIdFlow
                .filterNotNull()
                .flatMapLatest { conversationDetails(conversationId = it) }
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
            .combine(conversationIdFlow.filterNotNull()) { calls, conversationId ->
                val currentCall = calls.firstOrNull { call -> call.conversationId == conversationId }

                when (currentCall?.status) {
                    CallStatus.CLOSED -> onCallClosed()
                    else -> println("DO NOTHING")
                }
            }
            .collect()
    }

    private fun onCallClosed() {
        callRinger.stop()
        viewModelScope.launch { navigationManager.navigateBack() }
    }

    fun declineCall() {
        callRinger.stop()
        viewModelScope.launch {
            conversationIdFlow.value?.let {
                rejectCall(conversationId = it)
            }
        }
    }

    fun acceptCall() {
        callRinger.stop()
        viewModelScope.launch {
            navigationManager.navigateBack()
            conversationIdFlow.value?.let {
                acceptCall(conversationId = it)
                navigationManager.navigate(
                    command = NavigationCommand(
                        destination = NavigationItem.OngoingCall.getRouteWithArgs(listOf(it))
                    )
                )
            }
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
