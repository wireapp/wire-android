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
package com.wire.android.ui.calling.common

import com.wire.android.mapper.UserTypeMapper
import com.wire.android.ui.calling.usecase.HangUpCallUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.call.usecase.FlipToBackCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToFrontCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveLastActiveCallWithSortedParticipantsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveSpeakerUseCase
import com.wire.kalium.logic.feature.call.usecase.SetUIRotationUseCase
import com.wire.kalium.logic.feature.call.usecase.SetVideoPreviewUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOffUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOnUseCase
import com.wire.kalium.logic.feature.call.usecase.UnMuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.video.UpdateVideoStateUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class SharedCallingViewModelFactory(
    private val conversationDetails: ObserveConversationDetailsUseCase,
    private val observeLastActiveCallWithSortedParticipants: ObserveLastActiveCallWithSortedParticipantsUseCase,
    private val hangUpCall: HangUpCallUseCase,
    private val muteCall: MuteCallUseCase,
    private val unMuteCall: UnMuteCallUseCase,
    private val updateVideoState: UpdateVideoStateUseCase,
    private val setVideoPreview: SetVideoPreviewUseCase,
    private val setUIRotationUseCase: SetUIRotationUseCase,
    private val turnLoudSpeakerOff: TurnLoudSpeakerOffUseCase,
    private val turnLoudSpeakerOn: TurnLoudSpeakerOnUseCase,
    private val flipToFrontCamera: FlipToFrontCameraUseCase,
    private val flipToBackCamera: FlipToBackCameraUseCase,
    private val observeSpeaker: ObserveSpeakerUseCase,
    private val userTypeMapper: UserTypeMapper,
    private val dispatchers: DispatcherProvider,
) {
    fun create(conversationId: ConversationId): SharedCallingViewModel = SharedCallingViewModel(
        conversationId = conversationId,
        conversationDetails = conversationDetails,
        observeLastActiveCallWithSortedParticipants = observeLastActiveCallWithSortedParticipants,
        hangUpCall = hangUpCall,
        muteCall = muteCall,
        unMuteCall = unMuteCall,
        updateVideoState = updateVideoState,
        setVideoPreview = setVideoPreview,
        setUIRotationUseCase = setUIRotationUseCase,
        turnLoudSpeakerOff = turnLoudSpeakerOff,
        turnLoudSpeakerOn = turnLoudSpeakerOn,
        flipToFrontCamera = flipToFrontCamera,
        flipToBackCamera = flipToBackCamera,
        observeSpeaker = observeSpeaker,
        userTypeMapper = userTypeMapper,
        dispatchers = dispatchers,
    )
}
