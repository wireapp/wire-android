/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.ui.home.conversations.call

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.IsEligibleToStartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveConferenceCallingEnabledUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOngoingCallsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveDegradedConversationNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.SetUserInformedAboutVerificationUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@Suppress("LongParameterList", "TooManyFunctions")
class ConversationCallViewModel(
    val savedStateHandle: SavedStateHandle,
    currentAccount: UserId,
    private val observeOngoingCalls: ObserveOngoingCallsUseCase,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val observeParticipantsForConversation: ObserveParticipantsForConversationUseCase,
    private val answerCall: AnswerCallUseCase,
    private val endCall: EndCallUseCase,
    private val observeSyncState: ObserveSyncStateUseCase,
    private val isConferenceCallingEnabled: IsEligibleToStartCallUseCase,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val setUserInformedAboutVerification: SetUserInformedAboutVerificationUseCase,
    private val observeDegradedConversationNotified: ObserveDegradedConversationNotifiedUseCase,
    private val observeConferenceCallingEnabled: ObserveConferenceCallingEnabledUseCase,
    private val observeSelf: ObserveSelfUserUseCase
) : JoinOrStartCallViewModel(
    currentAccount = currentAccount,
    observeEstablishedCalls = observeEstablishedCalls,
    observeParticipantsForConversation = observeParticipantsForConversation,
    answerCall = answerCall,
    endCall = endCall,
    observeSyncState = observeSyncState,
    isEligibleToStartCall = isConferenceCallingEnabled,
    setUserInformedAboutVerification = setUserInformedAboutVerification,
    observeDegradedConversationNotified = observeDegradedConversationNotified,
    observeSelf = observeSelf,
) {
    private val conversationNavArgs: ConversationNavArgs = savedStateHandle.navArgs()
    val conversationId: QualifiedID = conversationNavArgs.conversationId
    var conversationCallViewState by mutableStateOf(ConversationCallViewState())
        private set
    val callingEnabled = MutableSharedFlow<Unit>(replay = 1)

    init {
        listenOngoingCall()
        observeParticipantsForConversation()
        observeCallingActivatedEvent()
    }

    private fun observeCallingActivatedEvent() {
        viewModelScope.launch {
            observeConferenceCallingEnabled()
                .collectLatest { callingEnabled.emit(Unit) }
        }
    }

    private fun observeParticipantsForConversation() {
        viewModelScope.launch {
            observeParticipantsForConversation(conversationId)
                .collectLatest {
                    conversationCallViewState = conversationCallViewState.copy(participantsCount = it.allCount)
                }
        }
    }

    override suspend fun participantsCountForConversation(conversationId: ConversationId): Int =
        conversationCallViewState.participantsCount // to avoid multiple calls to the use case, re-use the value from the state

    private fun listenOngoingCall() = viewModelScope.launch {
        combine(observeOngoingCalls(), observeConversationDetails(conversationId)) { calls, conversationDetailsResult ->
            val hasOngoingCall = calls.any { call -> call.conversationId == conversationId }
            // valid conversation is a conversation where the user is a member and it's not deleted
            val validConversation = when (conversationDetailsResult) {
                is ObserveConversationDetailsUseCase.Result.Success -> {
                    val conversationDetails = conversationDetailsResult.conversationDetails
                    val isGroup = conversationDetails is ConversationDetails.Group
                    val isSelfUserMember = if (isGroup) {
                        (conversationDetails as ConversationDetails.Group).isSelfUserMember
                    } else {
                        false
                    }
                    !(isGroup && !isSelfUserMember)
                }

                is ObserveConversationDetailsUseCase.Result.Failure -> false
            }
            hasOngoingCall && validConversation
        }.collectLatest {
            conversationCallViewState = conversationCallViewState.copy(hasOngoingCall = it)
        }
    }

    fun joinOngoingCall() = joinOngoingCall(conversationId)

    fun startCallIfPossible(conversationType: Conversation.Type) = startCallIfPossible(conversationId, conversationType)
}
