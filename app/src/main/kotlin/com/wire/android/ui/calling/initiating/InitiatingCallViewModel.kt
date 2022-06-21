package com.wire.android.ui.calling.initiating

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
import com.wire.kalium.logic.data.call.ConversationType
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.feature.call.CallStatus
import com.wire.kalium.logic.feature.call.usecase.GetAllCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.StartCallUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
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
    private val callRinger: CallRinger
) : ViewModel() {

    val conversationId: QualifiedID = savedStateHandle
        .get<String>(EXTRA_CONVERSATION_ID)!!
        .parseIntoQualifiedID()

    init {
        viewModelScope.launch {
            initiateCall()
            observeStartedCall()
        }
    }

    private fun retrieveConversationTypeAsync() = viewModelScope.async {

        val conversationDetails = conversationDetails(conversationId = conversationId).first { details ->
            details.conversation.id == conversationId
        }

        when (conversationDetails) {
            is ConversationDetails.Group -> {
                return@async ConversationType.Conference
            }
            is ConversationDetails.OneOne -> {
                return@async ConversationType.OneOnOne
            }
            else -> throw IllegalStateException("Invalid conversation type")
        }
    }


    private suspend fun observeStartedCall() {
        allCalls().collect { calls ->
            calls.find { call -> call.conversationId == conversationId }.also {
                it?.let {
                    if (it.status == CallStatus.ESTABLISHED) {
                        onCallEstablished()
                    }
                } ?: run {
                    onCallClosed()
                }
            }
        }
    }

    private fun onCallClosed() {
        navigateBack()
        callRinger.stop()
    }

    private suspend fun onCallEstablished() {
        callRinger.ring(R.raw.ready_to_talk, isLooping = false)
        navigationManager.navigate(
            command = NavigationCommand(
                destination = NavigationItem.OngoingCall.getRouteWithArgs(listOf(conversationId)),
                backStackMode = BackStackMode.REMOVE_CURRENT
            )
        )
    }

    private suspend fun initiateCall() {
        val conversationType = retrieveConversationTypeAsync().await()
        startCall(
            conversationId = conversationId,
            conversationType = conversationType
        )
        callRinger.ring(R.raw.ringing_from_me)
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

}
