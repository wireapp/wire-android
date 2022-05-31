package com.wire.android.ui.calling

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.feature.call.usecase.GetOngoingCallUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class OngoingCallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val ongoingCall: GetOngoingCallUseCase
) : ViewModel() {

    val conversationId: QualifiedID = savedStateHandle
        .get<String>(EXTRA_CONVERSATION_ID)!!
        .parseIntoQualifiedID()

    init {
        viewModelScope.launch {
            val job = launch {
                ongoingCall().first { it.isNotEmpty() }
            }
            job.join()
            // We start observing once we have an ongoing call
            observeCurrentCall()
        }
    }

    private suspend fun observeCurrentCall() {
        ongoingCall().collect { calls ->
            calls.find { call -> call.conversationId == conversationId }.also {
                if (it == null)
                    navigateBack()
            }
        }
    }

    private suspend fun navigateBack() {
        navigationManager.navigateBack()
    }
}
