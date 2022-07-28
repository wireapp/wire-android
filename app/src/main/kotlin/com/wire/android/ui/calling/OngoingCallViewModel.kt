package com.wire.android.ui.calling

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.AssistedViewModel
import com.wire.android.navigation.NavQualifiedId
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class OngoingCallViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val establishedCall: ObserveEstablishedCallsUseCase
) : ViewModel(), AssistedViewModel<NavQualifiedId> {

    val conversationId: ConversationId = param.qualifiedId

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
