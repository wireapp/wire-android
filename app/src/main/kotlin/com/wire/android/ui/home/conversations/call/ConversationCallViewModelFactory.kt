/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

import com.wire.android.di.CurrentAccount
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
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
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class ConversationCallViewModelFactory(
    @CurrentAccount private val currentAccount: UserId,
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
    private val observeSelf: ObserveSelfUserUseCase,
) {
    fun create(args: ConversationNavArgs): ConversationCallViewModel = ConversationCallViewModel(
        conversationNavArgs = args,
        currentAccount = currentAccount,
        observeOngoingCalls = observeOngoingCalls,
        observeEstablishedCalls = observeEstablishedCalls,
        observeParticipantsForConversation = observeParticipantsForConversation,
        answerCall = answerCall,
        endCall = endCall,
        observeSyncState = observeSyncState,
        isConferenceCallingEnabled = isConferenceCallingEnabled,
        observeConversationDetails = observeConversationDetails,
        setUserInformedAboutVerification = setUserInformedAboutVerification,
        observeDegradedConversationNotified = observeDegradedConversationNotified,
        observeConferenceCallingEnabled = observeConferenceCallingEnabled,
        observeSelf = observeSelf,
    )
}
