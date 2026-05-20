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
package com.wire.android.ui.calling.ongoing

import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.CurrentAccount
import com.wire.android.mapper.UICallParticipantMapper
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.ObserveCallModerationActionsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveCallQualityDataUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveInCallReactionsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveLastActiveCallWithSortedParticipantsUseCase
import com.wire.kalium.logic.feature.call.usecase.RequestVideoStreamsUseCase
import com.wire.kalium.logic.feature.call.usecase.SetCallQualityIntervalUseCase
import com.wire.kalium.logic.feature.call.usecase.video.SetVideoSendStateUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.incallreaction.SendInCallReactionUseCase
import com.wire.kalium.network.NetworkStateObserver
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class OngoingCallViewModelFactory(
    @CurrentAccount private val currentUserId: UserId,
    private val globalDataStore: GlobalDataStore,
    private val networkStateObserver: NetworkStateObserver,
    private val observeLastActiveCall: ObserveLastActiveCallWithSortedParticipantsUseCase,
    private val requestVideoStreams: RequestVideoStreamsUseCase,
    private val setVideoSendState: SetVideoSendStateUseCase,
    private val observeCallQualityData: ObserveCallQualityDataUseCase,
    private val setCallQualityInterval: SetCallQualityIntervalUseCase,
    private val getCurrentClientId: ObserveCurrentClientIdUseCase,
    private val observeInCallReactionsUseCase: ObserveInCallReactionsUseCase,
    private val sendInCallReactionUseCase: SendInCallReactionUseCase,
    private val observeCallModerationActions: ObserveCallModerationActionsUseCase,
    private val uiCallParticipantMapper: UICallParticipantMapper,
    private val dispatchers: DispatcherProvider,
) {
    fun create(conversationId: ConversationId): OngoingCallViewModel = OngoingCallViewModel(
        conversationId = conversationId,
        currentUserId = currentUserId,
        globalDataStore = globalDataStore,
        networkStateObserver = networkStateObserver,
        observeLastActiveCall = observeLastActiveCall,
        requestVideoStreams = requestVideoStreams,
        setVideoSendState = setVideoSendState,
        observeCallQualityData = observeCallQualityData,
        setCallQualityInterval = setCallQualityInterval,
        getCurrentClientId = getCurrentClientId,
        observeInCallReactionsUseCase = observeInCallReactionsUseCase,
        sendInCallReactionUseCase = sendInCallReactionUseCase,
        observeCallModerationActions = observeCallModerationActions,
        uiCallParticipantMapper = uiCallParticipantMapper,
        dispatchers = dispatchers,
    )
}
