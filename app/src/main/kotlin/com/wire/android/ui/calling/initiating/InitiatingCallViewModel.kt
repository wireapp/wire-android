package com.wire.android.ui.calling.initiating

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.media.CallRinger
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.ui.calling.getConversationName
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.call.ConversationType
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.feature.call.CallStatus
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.GetAllCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.StartCallUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class InitiatingCallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val conversationDetails: ObserveConversationDetailsUseCase,
    private val allCalls: GetAllCallsUseCase,
    private val startCall: StartCallUseCase,
    private val endCall: EndCallUseCase,
    private val callRinger: CallRinger
) : ViewModel() {

    var callInitiatedState by mutableStateOf(InitiatingCallState())
        private set

    val conversationId: QualifiedID = savedStateHandle
        .get<String>(EXTRA_CONVERSATION_ID)!!
        .parseIntoQualifiedID()

    init {
        viewModelScope.launch {
            launch { initiateCall() }
            launch { initializeScreenState() }
            launch { observeStartedCall() }
        }
    }

    private suspend fun observeStartedCall() {
        allCalls().collect {
            if (it.isNotEmpty() && it.first().conversationId == conversationId)
                when (it.first().status) {
//                    CallStatus.CLOSED -> onCallClosed()
                    CallStatus.ESTABLISHED -> onCallEstablished()
                    else -> print("DO NOTHING")
                }
        }
    }

    private fun onCallClosed() {
        callRinger.stop()
        navigateBack()
    }

    private suspend fun onCallEstablished() {
        callRinger.ring(R.raw.ready_to_talk, isLooping = false)
        navigateBack()
        navigationManager.navigate(
            command = NavigationCommand(
                destination = NavigationItem.OngoingCall.getRouteWithArgs(listOf(conversationId))
            )
        )
    }

    private suspend fun initializeScreenState() {
        conversationDetails(conversationId = conversationId)
            .collect {
                callInitiatedState = when (it) {
                    is ConversationDetails.Group -> {
                        callInitiatedState.copy(
                            conversationName = getConversationName(it.conversation.name),
                            conversationType = ConversationType.Conference
                        )
                    }
                    is ConversationDetails.OneOne -> {
                        callInitiatedState.copy(
                            conversationName = getConversationName(it.otherUser.name),
                            avatarAssetId = UserAvatarAsset(it.otherUser.completePicture ?: ""),
                            conversationType = ConversationType.OneOnOne
                        )
                    }
                    is ConversationDetails.Self -> throw IllegalStateException("Invalid conversation type")
                    is ConversationDetails.Connection -> throw IllegalStateException("Invalid conversation type")
                }
            }
    }

    private suspend fun initiateCall() {
        startCall(
            conversationId = conversationId,
            conversationType = callInitiatedState.conversationType
        )
        callRinger.ring(R.raw.ringing_from_me)
    }

    fun hangUpCall() {
        callRinger.stop()
        viewModelScope.launch { endCall(conversationId) }
        navigateBack()
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

}
