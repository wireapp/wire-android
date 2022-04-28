package com.wire.android.ui.calling.incoming

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.parseIntoQualifiedID
import com.wire.android.ui.calling.getConversationName
import com.wire.kalium.logic.feature.call.usecase.RejectCallUseCase
import com.wire.kalium.logic.feature.call.AnswerCallUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val conversationDetails: ObserveConversationDetailsUseCase,
    private val rejectCall: RejectCallUseCase,
    private val acceptCall: AnswerCallUseCase
) : ViewModel() {
    var callState by mutableStateOf(IncomingCallState())
        private set

    val conversationId: ConversationId = savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!.parseIntoQualifiedID()

    init {
        viewModelScope.launch {
            conversationDetails(conversationId = conversationId)
                .collect { initializeScreenState(conversationDetails = it) }
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
            is ConversationDetails.Group -> {
                callState.copy(
                    conversationName = getConversationName(conversationDetails.conversation.name)
                )
            }
            is ConversationDetails.OneOne -> {
                callState.copy(
                    conversationName = getConversationName(conversationDetails.otherUser.name)
                )
            }
            else -> throw IllegalStateException("Invalid conversation type")
        }
    }
}
