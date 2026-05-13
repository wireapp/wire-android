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
package com.wire.android.ui.calling.incoming

import com.wire.android.di.CurrentAccount
import com.wire.android.notification.CallNotificationManager
import com.wire.android.ui.home.appLock.LockCodeTimeManager
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.GetIncomingCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.RejectCallUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class IncomingCallViewModelFactory(
    @CurrentAccount private val currentAccount: UserId,
    private val callNotificationManager: CallNotificationManager,
    private val incomingCalls: GetIncomingCallsUseCase,
    private val rejectCall: RejectCallUseCase,
    private val acceptCall: AnswerCallUseCase,
    private val muteCall: MuteCallUseCase,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val endCall: EndCallUseCase,
    private val lockCodeTimeManager: LockCodeTimeManager,
) {
    fun create(conversationId: ConversationId): IncomingCallViewModel = IncomingCallViewModel(
        conversationId = conversationId,
        currentAccount = currentAccount,
        callNotificationManager = callNotificationManager,
        incomingCalls = incomingCalls,
        rejectCall = rejectCall,
        acceptCall = acceptCall,
        muteCall = muteCall,
        observeEstablishedCalls = observeEstablishedCalls,
        endCall = endCall,
        lockCodeTimeManager = lockCodeTimeManager,
    )
}
