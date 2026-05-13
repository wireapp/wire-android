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
package com.wire.android.ui.calling.outgoing

import com.wire.android.media.CallRinger
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.IsLastCallClosedUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOutgoingCallUseCase
import com.wire.kalium.logic.feature.call.usecase.StartCallUseCase
import dev.zacsweers.metro.Inject

@Inject
class OutgoingCallViewModelFactory(
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val observeOutgoingCall: ObserveOutgoingCallUseCase,
    private val startCall: StartCallUseCase,
    private val endCall: EndCallUseCase,
    private val isLastCallClosed: IsLastCallClosedUseCase,
    private val callRinger: CallRinger,
) {
    fun create(conversationId: ConversationId): OutgoingCallViewModel = OutgoingCallViewModel(
        conversationId = conversationId,
        observeEstablishedCalls = observeEstablishedCalls,
        observeOutgoingCall = observeOutgoingCall,
        startCall = startCall,
        endCall = endCall,
        isLastCallClosed = isLastCallClosed,
        callRinger = callRinger,
    )
}
