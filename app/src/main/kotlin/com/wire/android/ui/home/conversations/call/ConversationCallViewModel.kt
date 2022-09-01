package com.wire.android.ui.home.conversations.call

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.SavedStateViewModel
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.feature.call.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOngoingCallsUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationCallViewModel @Inject constructor(
    qualifiedIdMapper: QualifiedIdMapper,
    override val savedStateHandle: SavedStateHandle,
    private val observeOngoingCalls: ObserveOngoingCallsUseCase,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val navigationManager: NavigationManager,
    private val answerCall: AnswerCallUseCase,
    private val endCall: EndCallUseCase,
    private val observeSyncState: ObserveSyncStateUseCase,
    ) : SavedStateViewModel(savedStateHandle) {

    val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!
    )

    var conversationCallViewState by mutableStateOf(ConversationCallViewState())
        private set

    var establishedCallConversationId: QualifiedID? = null

    init {
        listenOngoingCall()
        observeEstablishedCall()
    }

    private fun listenOngoingCall() = viewModelScope.launch {
        observeOngoingCalls()
            .collect {
                val hasOngoingCall = it.any { call -> call.conversationId == conversationId }

                conversationCallViewState = conversationCallViewState.copy(hasOngoingCall = hasOngoingCall)
            }
    }

    private fun observeEstablishedCall() = viewModelScope.launch {
        observeEstablishedCalls().collect {
            val hasEstablishedCall = it.isNotEmpty()
            establishedCallConversationId = if (it.isNotEmpty()) {
                it.first().conversationId
            } else null
            conversationCallViewState = conversationCallViewState.copy(hasEstablishedCall = hasEstablishedCall)
        }
    }

    fun joinOngoingCall() {
        viewModelScope.launch {
            answerCall(conversationId = conversationId)
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.OngoingCall.getRouteWithArgs(listOf(conversationId))
                )
            )
        }
    }

    fun navigateToInitiatingCallScreen() {
        viewModelScope.launch {
            establishedCallConversationId?.let {
                endCall(it)
            }
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.InitiatingCall.getRouteWithArgs(listOf(conversationId))
                )
            )
        }
    }

    suspend fun hasStableConnectivity(): Boolean {
        var hasConnection = false
        observeSyncState().firstOrNull()?.let {
            hasConnection = when (it) {
                is SyncState.Failed, SyncState.Waiting -> false
                SyncState.GatheringPendingEvents, SyncState.SlowSync, SyncState.Live -> true
            }
        }
        return hasConnection
    }
}
