/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.calling.usecase

import com.wire.android.di.ApplicationScope
import com.wire.android.media.CallRinger
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToFrontCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveSpeakerUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOffUseCase
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@ViewModelScoped
class HangUpCallUseCase @Inject constructor(
    @ApplicationScope private val coroutineScope: CoroutineScope,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val observeSpeaker: ObserveSpeakerUseCase,
    private val endCall: EndCallUseCase,
    private val muteCall: MuteCallUseCase,
    private val turnLoudSpeakerOff: TurnLoudSpeakerOffUseCase,
    private val flipToFrontCamera: FlipToFrontCameraUseCase,
    private val callRinger: CallRinger,
) {

    operator fun invoke(conversationId: ConversationId) {
        coroutineScope.launch {
            resetCallConfig(conversationId)
            endCall(conversationId)
            callRinger.stop()
        }
    }

    private suspend fun resetCallConfig(conversationId: ConversationId) {
        // we need to update mute state to false, so if the user re-join the call te mic will will be muted
        muteCall(conversationId, false)
        // we need to update speaker state to false, so if the user re-join the call the speaker will be off
        observeSpeaker().firstOrNull()?.let { isSpeakerOn ->
            if (isSpeakerOn) {
                turnLoudSpeakerOff()
            }
        }
        // we need to flip the camera to front, so if the user re-join the call the camera will be on front
        observeEstablishedCalls().firstOrNull()?.firstOrNull { it.conversationId == conversationId }?.let { call ->
            if (call.isCameraOn) {
                flipToFrontCamera(conversationId)
            }
        }
    }
}
