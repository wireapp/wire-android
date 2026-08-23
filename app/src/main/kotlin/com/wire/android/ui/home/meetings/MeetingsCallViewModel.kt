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

package com.wire.android.ui.home.meetings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.CurrentAccount
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.home.conversations.call.JoinOrStartCallManager
import com.wire.android.ui.home.conversations.call.KaliumObserveConversationParticipantCount
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.IsEligibleToStartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import com.wire.kalium.logic.feature.conversation.ObserveDegradedConversationNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.SetUserInformedAboutVerificationUseCase
import com.wire.kalium.logic.feature.meeting.EnsureMeetingIsMLSEstablishedUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

@Suppress("LongParameterList", "TooManyFunctions")
class MeetingsCallViewModel @Inject constructor(
    @CurrentAccount currentAccount: UserId,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val observeConversationMembers: ObserveConversationMembersUseCase,
    private val answerCall: AnswerCallUseCase,
    private val endCall: EndCallUseCase,
    private val observeSyncState: ObserveSyncStateUseCase,
    private val isConferenceCallingEnabled: IsEligibleToStartCallUseCase,
    private val setUserInformedAboutVerification: SetUserInformedAboutVerificationUseCase,
    private val observeDegradedConversationNotified: ObserveDegradedConversationNotifiedUseCase,
    private val observeSelf: ObserveSelfUserUseCase,
    private val ensureMeetingIsMLSEstablished: EnsureMeetingIsMLSEstablishedUseCase,
) : ViewModel() {
    val notEstablishedDialogState: VisibilityState<Unit> = VisibilityState()

    val callManager = JoinOrStartCallManager(
        scope = viewModelScope,
        currentAccount = currentAccount,
        observeEstablishedCalls = observeEstablishedCalls,
        observeConversationParticipantCount = KaliumObserveConversationParticipantCount(observeConversationMembers),
        answerCall = answerCall,
        endCall = endCall,
        observeSyncState = observeSyncState,
        isEligibleToStartCall = isConferenceCallingEnabled,
        setUserInformedAboutVerification = setUserInformedAboutVerification,
        observeDegradedConversationNotified = observeDegradedConversationNotified,
        observeSelf = observeSelf,
    )

    fun joinOngoingCall(conversationId: ConversationId) {
        viewModelScope.launch {
            ensureMLSEstablished(conversationId) {
                callManager.joinOngoingCall(conversationId)
            }
        }
    }

    fun startCallIfPossible(conversationId: ConversationId) {
        viewModelScope.launch {
            ensureMLSEstablished(conversationId) {
                callManager.startCallIfPossible(
                    conversationId = conversationId,
                    conversationType = Conversation.Type.Group.Meeting,
                    shouldCheckParticipantCount = false, // since this is a meeting, we don't need to check participant count
                )
            }
        }
    }

    private suspend fun ensureMLSEstablished(conversationId: ConversationId, actionIfEstablished: suspend () -> Unit) =
        if (ensureMeetingIsMLSEstablished(conversationId)) {
            actionIfEstablished()
        } else {
            notEstablishedDialogState.show(Unit)
        }
}
