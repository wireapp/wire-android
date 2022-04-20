package com.wire.android.ui.calling.incoming

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.parseIntoQualifiedID
import com.wire.kalium.logic.feature.call.usecase.RejectCallUseCase
import com.wire.kalium.logic.feature.call.AnswerCallUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.Conversation.Type.GROUP
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

    val conversationId: ConversationId? = savedStateHandle
        .getLiveData<String>(EXTRA_CONVERSATION_ID)
        .value
        ?.parseIntoQualifiedID()

    init {
        viewModelScope.launch {
            appLogger.d("ICVM_0 -> Start")
            conversationId?.run {
                conversationDetails(conversationId = conversationId)
                    .collect {
                        val a = it.conversation
                        appLogger.d("ICVM_1 -> Collect")
                        val conversationName = when (it) {
                            is ConversationDetails.Self -> "Self"
                            is ConversationDetails.Group -> "Group"
                            is ConversationDetails.OneOne -> it.otherUser.name ?: "OneToOne"
                            else -> "Unknown"
                        }
                        appLogger.d("ICVM_2 -> Collect response: ${conversationName}")
                        callState = callState.copy(
                            conversationName = conversationName
                        )
                        appLogger.d("ICVM_3 -> Update callState")
                    }
            }
        }
    }

    fun declineCall() {
        conversationId?.run {
            viewModelScope.launch {
                rejectCall(conversationId = conversationId)
                navigationManager.navigateBack()
            }
        }
    }

    fun acceptCall() {
        conversationId?.run {
            viewModelScope.launch {
                acceptCall(conversationId = conversationId)

                navigationManager.navigate(
                    command = NavigationCommand(
                        destination = NavigationItem.OngoingCall.getRouteWithArgs(),
                        backStackMode = BackStackMode.CLEAR_TILL_START
                    )
                )
            }
        }
    }
}
