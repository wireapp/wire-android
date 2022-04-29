package com.wire.android.ui.calling.incoming

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.model.UserAvatarAsset
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.parseIntoQualifiedID
import com.wire.kalium.logic.feature.call.usecase.RejectCallUseCase
import com.wire.kalium.logic.feature.call.AnswerCallUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.call.CallStatus
import com.wire.kalium.logic.feature.call.usecase.GetAllCallsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.lang.IllegalStateException
import javax.inject.Inject

@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val conversationDetails: ObserveConversationDetailsUseCase,
    private val allCalls: GetAllCallsUseCase,
    private val rejectCall: RejectCallUseCase,
    private val acceptCall: AnswerCallUseCase
) : ViewModel() {
    var callState by mutableStateOf(IncomingCallState())
        private set

    val conversationId: ConversationId = savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!.parseIntoQualifiedID()

    init {
        viewModelScope.launch {
            launch {
                conversationDetails(conversationId = conversationId)
                    .collect { initializeScreenState(conversationDetails = it) }
            }
            launch {
                observeIncomingCall()
            }
        }
    }

    private suspend fun observeIncomingCall() {
        allCalls().collect {
            if (it.first().conversationId == conversationId)
                when (it.first().status) {
                    CallStatus.CLOSED -> navigationManager.navigateBack()
                    else -> print("DO NOTHING")
                }
        }
    }

    fun declineCall() {
        viewModelScope.launch {
            rejectCall(conversationId = conversationId)
            navigationManager.navigateBack()
        }
    }

    fun acceptCall() {
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

    private fun initializeScreenState(conversationDetails: ConversationDetails) {
        callState = when (conversationDetails) {
            is ConversationDetails.Group -> callState.copy(conversationName = conversationDetails.conversation.name)
            is ConversationDetails.OneOne -> {
                callState.copy(
                    conversationName = conversationDetails.otherUser.name,
                    avatarAssetId = conversationDetails.otherUser.completePicture?.let { UserAvatarAsset(it) }
                )
            }
            else -> throw IllegalStateException("Invalid conversation type")
        }
    }
}
