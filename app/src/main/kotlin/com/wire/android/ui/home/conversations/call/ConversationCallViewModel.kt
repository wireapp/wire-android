/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

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
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ConferenceCallingResult
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.IsEligibleToStartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOngoingCallsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList")
class ConversationCallViewModel @Inject constructor(
    qualifiedIdMapper: QualifiedIdMapper,
    override val savedStateHandle: SavedStateHandle,
    private val observeOngoingCalls: ObserveOngoingCallsUseCase,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val navigationManager: NavigationManager,
    private val answerCall: AnswerCallUseCase,
    private val endCall: EndCallUseCase,
    private val observeSyncState: ObserveSyncStateUseCase,
    private val isConferenceCallingEnabled: IsEligibleToStartCallUseCase,
    private val observeConversationDetails: ObserveConversationDetailsUseCase
) : SavedStateViewModel(savedStateHandle) {

    val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!
    )
    var conversationCallViewState by mutableStateOf(ConversationCallViewState())

    var establishedCallConversationId: QualifiedID? = null

    init {
        listenOngoingCall()
        observeEstablishedCall()
    }

    private fun listenOngoingCall() = viewModelScope.launch {
        combine(observeOngoingCalls(), observeConversationDetails(conversationId)) { calls, conversationDetailsResult ->
            val hasOngoingCall = calls.any { call -> call.conversationId == conversationId }
            // valid conversation is a conversation where the user is a member and it's not deleted
            val validConversation = when (conversationDetailsResult) {
                is ObserveConversationDetailsUseCase.Result.Success -> {
                    !(conversationDetailsResult.conversationDetails is ConversationDetails.Group &&
                            !(conversationDetailsResult.conversationDetails as ConversationDetails.Group).isSelfUserMember)
                }

                is ObserveConversationDetailsUseCase.Result.Failure -> false
            }
            hasOngoingCall && validConversation
        }.collectLatest {
            conversationCallViewState = conversationCallViewState.copy(hasOngoingCall = it)
        }
    }

    private fun observeEstablishedCall() = viewModelScope.launch {
        observeEstablishedCalls()
            .distinctUntilChanged()
            .collect {
                val hasEstablishedCall = it.isNotEmpty()
                establishedCallConversationId = if (it.isNotEmpty()) {
                    it.first().conversationId
                } else null
                conversationCallViewState = conversationCallViewState.copy(hasEstablishedCall = hasEstablishedCall)
            }
    }

    fun joinAnyway() {
        viewModelScope.launch {
            establishedCallConversationId?.let {
                endCall(it)
                delay(DELAY_END_CALL)
            }
            joinOngoingCall()
        }
    }

    fun joinOngoingCall() {
        viewModelScope.launch {
            if (conversationCallViewState.hasEstablishedCall) {
                showJoinCallAnywayDialog()
            } else {
                dismissJoinCallAnywayDialog()
                answerCall(conversationId = conversationId)
                navigationManager.navigate(
                    command = NavigationCommand(
                        destination = NavigationItem.OngoingCall.getRouteWithArgs(listOf(conversationId))
                    )
                )
            }
        }
    }

    private fun showJoinCallAnywayDialog() {
        conversationCallViewState = conversationCallViewState.copy(shouldShowJoinAnywayDialog = true)
    }

    fun dismissJoinCallAnywayDialog() {
        conversationCallViewState = conversationCallViewState.copy(shouldShowJoinAnywayDialog = false)
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

    suspend fun isConferenceCallingEnabled(conversationType: Conversation.Type): ConferenceCallingResult =
        isConferenceCallingEnabled.invoke(conversationId, conversationType)

    companion object {
        const val DELAY_END_CALL = 200L
    }
}
