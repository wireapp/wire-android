package com.wire.android.ui.calling

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.services.ServicesManager
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.call.Call
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OngoingCallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    qualifiedIdMapper: QualifiedIdMapper,
    private val navigationManager: NavigationManager,
    private val establishedCall: ObserveEstablishedCallsUseCase,
    private val currentScreenManager: CurrentScreenManager,
    private val servicesManager: ServicesManager
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
            .combine(currentScreenManager.observeCurrentScreen(viewModelScope), ::Pair)
            .collect { (calls, currentScreen) ->
                val currentCall = calls.find { call -> call.conversationId == conversationId }
                if (currentCall == null) {
                    navigateBack()
                    servicesManager.stopOngoingCallService()
                } else if (currentScreen is CurrentScreen.InBackground) {
                    servicesManager.startOngoingCallService(getNotificationTitle(currentCall), conversationId)
                } else if (currentScreen is CurrentScreen.OngoingCallScreen) {
                    servicesManager.stopOngoingCallService()
                }
            }
    }

    private suspend fun navigateBack() {
        navigationManager.navigateBack()
    }

    private fun getNotificationTitle(call: Call): String =
        when (call.conversationType) {
            Conversation.Type.GROUP -> call.conversationName.orEmpty()
            else -> {
                val name = call.callerName.orEmpty()
                call.callerTeamName?.let { "$name @$it" } ?: name
            }
        }
}
