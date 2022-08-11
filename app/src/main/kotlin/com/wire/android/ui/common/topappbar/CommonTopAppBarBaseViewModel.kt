package com.wire.android.ui.common.topappbar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class CommonTopAppBarBaseViewModel: ViewModel()

@HiltViewModel
class CommonTopAppBarViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val establishedCalls: ObserveEstablishedCallsUseCase,
    private val currentScreenManager: CurrentScreenManager
) : CommonTopAppBarBaseViewModel() {

    var callState by mutableStateOf(EstablishedCallState())

    init {
        viewModelScope.launch {
            launch {
                observeEstablishedCall()
            }
            launch {
                observeScreenState()
            }
        }
    }

    fun openOngoingCallScreen() {
        callState.conversationId?.let { convId ->
            callState = callState.copy(
                shouldShow = false
            )

            viewModelScope.launch {
                navigationManager.navigate(
                    command = NavigationCommand(
                        destination = NavigationItem.OngoingCall.getRouteWithArgs(listOf(convId))
                    )
                )
            }
        }
    }

    private suspend fun observeEstablishedCall() {
        establishedCalls().collect { calls ->
            val call = calls.firstOrNull()
            val show = call?.let { true } ?: false

            callState = callState.copy(
                conversationId = call?.conversationId,
                isCallHappening = show,
                shouldShow = show,
                isMuted = call?.isMuted
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observeScreenState() {
        currentScreenManager.observeCurrentScreen(viewModelScope).collect {
            callState = callState.copy(
                shouldShow = (callState.isCallHappening && (it is CurrentScreen.Home || it is CurrentScreen.Conversation))
            )
        }
    }
}
