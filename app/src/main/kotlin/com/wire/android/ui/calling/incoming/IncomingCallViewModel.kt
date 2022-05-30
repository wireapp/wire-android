package com.wire.android.ui.calling.incoming

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.media.CallRinger
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.navigation.BackStackMode
import com.wire.android.notification.CallNotificationManager
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.take
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
    private val callRinger: CallRinger,
    private val notificationManager: CallNotificationManager
) : ViewModel() {
    var callState by mutableStateOf(IncomingCallState())
        private set

    val conversationId: ConversationId = savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!.parseIntoQualifiedID()

    init {
        viewModelScope.launch {
            callRinger.ring(R.raw.ringing_from_them)
            val conversationDetailsFlow = conversationDetails(conversationId = conversationId)
                .shareIn(this, SharingStarted.WhileSubscribed(), 1)

            launch {
                conversationDetailsFlow.collect { initializeScreenState(conversationDetails = it) }
            }
            launch {
                observeIncomingCall()
            }
            launch {
                conversationDetailsFlow
                    .take(1)
                    .onEach { details ->
                        // if user does nothing on incoming call for some time, app stops calling
                        val delayTimeMs = if (details is ConversationDetails.Group) GROUP_RINGING_TIME
                        else ONO_ON_ONE_RINGING_TIME

                        delay(delayTimeMs)

                        declineCall()
                    }
                    .collect()
            }
        }
    }

    private suspend fun observeIncomingCall() {
        allCalls()
            .collect { calls ->
                val currentCall = calls.firstOrNull { call -> call.conversationId == conversationId }

                when (currentCall?.status) {
                    CallStatus.CLOSED -> onCallClosed()
                    else -> println("DO NOTHING")
                }
            }
    }

    private fun onCallClosed() {
        stopRinging()
        viewModelScope.launch {
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.Home.getRouteWithArgs(),
                    backStackMode = BackStackMode.CLEAR_TILL_START
                )
            )
        }
    }

    fun declineCall() {
        stopRinging()
        viewModelScope.launch {
            rejectCall(conversationId = conversationId)
        }
    }

    fun acceptCall() {
        stopRinging()
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

    private fun stopRinging() {
        callRinger.stop()
        notificationManager.hideCallNotification()
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

    companion object {
        private const val ONO_ON_ONE_RINGING_TIME = 60_000L
        private const val GROUP_RINGING_TIME = 30_000L
    }
}
