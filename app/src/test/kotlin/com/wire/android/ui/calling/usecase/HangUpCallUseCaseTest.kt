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

import com.wire.android.media.CallRinger
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToFrontCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveSpeakerUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOffUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class HangUpCallUseCaseTest {

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun `given an active call, when hanging up, then end call and stop ringer`() = runTest(dispatcher) {
        // given
        val call = CALL
        val (arrangement, hangUpCallUseCase) = Arrangement()
            .withEstablishedCall(call)
            .arrange()
        // when
        hangUpCallUseCase(call.conversationId)
        advanceUntilIdle()
        // then
        coVerify(exactly = 1) { arrangement.endCallUseCase(call.conversationId) }
        coVerify(exactly = 1) { arrangement.callRinger.stop() }
    }

    @Test
    fun `given an active call, when hanging up, then reset call mute status`() = runTest(dispatcher) {
        // given
        val call = CALL
        val (arrangement, hangUpCallUseCase) = Arrangement()
            .withEstablishedCall(call)
            .arrange()
        // when
        hangUpCallUseCase(call.conversationId)
        advanceUntilIdle()
        // then
        coVerify(exactly = 1) { arrangement.muteCallUseCase(call.conversationId, false) }
    }

    @Test
    fun `given an active call with speaker on, when hanging up, then reset speaker status to false`() = runTest(dispatcher) {
        // given
        val call = CALL
        val (arrangement, hangUpCallUseCase) = Arrangement()
            .withEstablishedCall(call)
            .withSpeaker(true)
            .arrange()
        // when
        hangUpCallUseCase(call.conversationId)
        advanceUntilIdle()
        // then
        coVerify(exactly = 1) { arrangement.turnLoudSpeakerOffUseCase() }
    }

    @Test
    fun `given an active call with speaker off, when hanging up, then do not reset speaker status to false`() = runTest(dispatcher) {
        // given
        val call = CALL
        val (arrangement, hangUpCallUseCase) = Arrangement()
            .withEstablishedCall(call)
            .withSpeaker(false)
            .arrange()
        // when
        hangUpCallUseCase(call.conversationId)
        advanceUntilIdle()
        // then
        coVerify(exactly = 0) { arrangement.turnLoudSpeakerOffUseCase() }
    }

    @Test
    fun `given an active call with camera on, when hanging up, then reset call camera status to front`() = runTest(dispatcher) {
        // given
        val call = CALL.copy(isCameraOn = true)
        val (arrangement, hangUpCallUseCase) = Arrangement()
            .withEstablishedCall(call)
            .arrange()
        // when
        hangUpCallUseCase(call.conversationId)
        advanceUntilIdle()
        // then
        coVerify(exactly = 1) { arrangement.flipToFrontCameraUseCase(call.conversationId) }
    }

    @Test
    fun `given an active call with camera off, when hanging up, then do not reset call camera status to front`() = runTest(dispatcher) {
        // given
        val call = CALL.copy(isCameraOn = false)
        val (arrangement, hangUpCallUseCase) = Arrangement()
            .withEstablishedCall(call)
            .arrange()
        // when
        hangUpCallUseCase(call.conversationId)
        advanceUntilIdle()
        // then
        coVerify(exactly = 0) { arrangement.flipToFrontCameraUseCase(call.conversationId) }
    }

    inner class Arrangement {

        private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

        @MockK
        lateinit var observeEstablishedCallsUseCase: ObserveEstablishedCallsUseCase

        @MockK
        lateinit var observeSpeakerUseCase: ObserveSpeakerUseCase

        @MockK
        lateinit var endCallUseCase: EndCallUseCase

        @MockK
        lateinit var muteCallUseCase: MuteCallUseCase

        @MockK
        lateinit var turnLoudSpeakerOffUseCase: TurnLoudSpeakerOffUseCase

        @MockK
        lateinit var flipToFrontCameraUseCase: FlipToFrontCameraUseCase

        @MockK
        lateinit var callRinger: CallRinger

        init {
            MockKAnnotations.init(this, relaxed = true)
            withEstablishedCall(CALL)
            withSpeaker(false)
        }

        fun withEstablishedCall(call: Call) = apply {
            coEvery { observeEstablishedCallsUseCase() } returns flowOf(listOf(call))
        }

        fun withSpeaker(isOn: Boolean) = apply {
            every { observeSpeakerUseCase() } returns flowOf(isOn)
        }

        fun arrange() = this to HangUpCallUseCase(
            coroutineScope = scope,
            observeEstablishedCalls = observeEstablishedCallsUseCase,
            observeSpeaker = observeSpeakerUseCase,
            endCall = endCallUseCase,
            muteCall = muteCallUseCase,
            turnLoudSpeakerOff = turnLoudSpeakerOffUseCase,
            flipToFrontCamera = flipToFrontCameraUseCase,
            callRinger = callRinger
        )
    }

    companion object {
        private val CALL = Call(
            conversationId = ConversationId("ongoing_call_id", "some_domain"),
            status = CallStatus.ESTABLISHED,
            isMuted = false,
            isCameraOn = true,
            isCbrEnabled = false,
            callerId = QualifiedID("some_id", "some_domain"),
            conversationName = "some_name",
            conversationType = Conversation.Type.Group.Regular,
            callerName = "some_name",
            callerTeamName = "some_team_name"
        )
    }
}
