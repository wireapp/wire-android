package com.wire.android.ui.calling

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.kalium.logic.data.call.CallClient
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.RequestVideoStreamsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class OngoingCallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    qualifiedIdMapper: QualifiedIdMapper,
    private val navigationManager: NavigationManager,
    private val establishedCall: ObserveEstablishedCallsUseCase,
    private val requestVideoStreams: RequestVideoStreamsUseCase
) : ViewModel() {

    private val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!
    )

    init {
        viewModelScope.launch {
            establishedCall().first { it.isNotEmpty() }.run {
                // We start observing once we have an ongoing call
                observeCurrentCall()
            }
        }
    }

    private suspend fun observeCurrentCall() {
        establishedCall()
            .distinctUntilChanged()
            .collect { calls ->
                val currentCall = calls.find { call -> call.conversationId == conversationId }
                if (currentCall == null) navigateBack()
            }
    }

    fun requestVideoStreams(participants: List<UICallParticipant>) {
        viewModelScope.launch {
            val clients : List<CallClient> = participants.map {
                CallClient(it.id.toString(), it.clientId)
            }
            requestVideoStreams(conversationId, clients)
        }
    }

    private suspend fun navigateBack() {
        navigationManager.navigateBack()
    }

}
