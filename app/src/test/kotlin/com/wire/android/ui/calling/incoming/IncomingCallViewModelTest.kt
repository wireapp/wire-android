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

package com.wire.android.ui.calling.incoming

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.media.CallRinger
import com.wire.android.ui.calling.CallingNavArgs
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.call.Call
import com.wire.kalium.logic.feature.call.CallStatus
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.GetIncomingCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.RejectCallUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class IncomingCallViewModelTest {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var rejectCall: RejectCallUseCase

    @MockK
    lateinit var incomingCalls: GetIncomingCallsUseCase

    @MockK
    lateinit var acceptCall: AnswerCallUseCase

    @MockK
    private lateinit var callRinger: CallRinger

    @MockK
    private lateinit var observeEstablishedCalls: ObserveEstablishedCallsUseCase

    @MockK
    private lateinit var endCall: EndCallUseCase

    @MockK
    private lateinit var muteCall: MuteCallUseCase

    @MockK(relaxed = true)
    private lateinit var onCompleted: () -> Unit

    private lateinit var viewModel: IncomingCallViewModel

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        every { savedStateHandle.navArgs<CallingNavArgs>() } returns CallingNavArgs(conversationId = dummyConversationId)

        // Default empty values
        coEvery { rejectCall(any()) } returns Unit
        coEvery { acceptCall(any()) } returns Unit
        coEvery { callRinger.ring(any(), any()) } returns Unit
        coEvery { callRinger.stop() } returns Unit

        viewModel = IncomingCallViewModel(
            savedStateHandle = savedStateHandle,
            incomingCalls = incomingCalls,
            rejectCall = rejectCall,
            acceptCall = acceptCall,
            callRinger = callRinger,
            observeEstablishedCalls = observeEstablishedCalls,
            endCall = endCall,
            muteCall = muteCall
        )
    }

    @Test
    fun `given an incoming call, when the user decline the call, then the reject call use case is called`() = runTest {
        coEvery { incomingCalls.invoke() } returns flowOf(listOf(provideCall()))
        coEvery { observeEstablishedCalls.invoke() } returns flowOf(listOf())

        viewModel.init {  }
        viewModel.declineCall(onCompleted)

        coVerify(exactly = 1) { rejectCall(conversationId = any()) }
        verify(exactly = 1) { callRinger.stop() }
        verify(exactly = 1) { onCompleted() }
    }

    @Test
    fun `given no ongoing call, when user tries to accept an incoming call, then invoke answerCall call use case`() = runTest {
        coEvery { incomingCalls.invoke() } returns flowOf(listOf(provideCall()))
        coEvery { observeEstablishedCalls.invoke() } returns flowOf(listOf())
        coEvery { acceptCall(conversationId = any()) } returns Unit
        every { callRinger.stop() } returns Unit

        viewModel.init {  }
        viewModel.acceptCall(onCompleted)

        coVerify(exactly = 1) { acceptCall(conversationId = any()) }
        verify(exactly = 1) { callRinger.stop() }
        coVerify(inverse = true) { endCall(any()) }
        assertEquals(false, viewModel.incomingCallState.shouldShowJoinCallAnywayDialog)
        verify(exactly = 1) { onCompleted() }
    }

    @Test
    fun `given an ongoing call, when user tries to accept an incoming call, then show JoinCallAnywayDialog`() = runTest {
        coEvery { incomingCalls.invoke() } returns flowOf(listOf(provideCall()))
        coEvery { observeEstablishedCalls.invoke() } returns flowOf(listOf(provideCall(ConversationId("value", "Domain"))))

        viewModel.init {  }
        viewModel.acceptCall(onCompleted)

        assertEquals(true, viewModel.incomingCallState.shouldShowJoinCallAnywayDialog)
        coVerify(inverse = true) { acceptCall(conversationId = any()) }
        verify(inverse = true) { callRinger.stop() }
        verify(inverse = true) { onCompleted() }
    }

    @Test
    fun `given an ongoing call, when user confirms dialog to accept an incoming call, then end current call and accept the newer one`()
    = runTest {
        val establishedCallsChannel = Channel<List<Call>>(capacity = Channel.UNLIMITED)
            .also { it.send(listOf(provideCall(ConversationId("value", "Domain")))) }
        coEvery { incomingCalls.invoke() } returns flowOf(listOf(provideCall()))
        coEvery { observeEstablishedCalls.invoke() } returns establishedCallsChannel.consumeAsFlow()
        coEvery { muteCall(any(), eq(false)) } returns Unit
        coEvery { endCall(any()) } coAnswers { establishedCallsChannel.send(listOf()).let { } }

        viewModel.init {  }
        viewModel.acceptCallAnyway(onCompleted)
        advanceUntilIdle()

        coVerify(exactly = 1) { endCall(any()) }
        coVerify(exactly = 1) { muteCall(any(), eq(false)) }
        verify(exactly = 1) { onCompleted() }
    }

    @Test
    fun `given join dialog displayed, when user dismisses it, then hide it`() = runTest {
        coEvery { incomingCalls.invoke() } returns flowOf(listOf(provideCall()))
        coEvery { observeEstablishedCalls.invoke() } returns flowOf(listOf(provideCall()))
        viewModel.init {  }
        viewModel.acceptCall {  }

        viewModel.dismissJoinCallAnywayDialog()

        assertEquals(false, viewModel.incomingCallState.shouldShowJoinCallAnywayDialog)
    }

    companion object {
        val dummyConversationId = ConversationId("some-dummy-value", "some.dummy.domain")

        private fun provideCall(id: ConversationId = dummyConversationId, status: CallStatus = CallStatus.INCOMING) = Call(
            conversationId = id,
            status = status,
            callerId = "caller_id",
            participants = listOf(),
            isMuted = false,
            isCameraOn = false,
            maxParticipants = 0,
            conversationName = "ONE_ON_ONE Name",
            conversationType = Conversation.Type.ONE_ON_ONE,
            callerName = "otherUsername",
            callerTeamName = "team_1",
            isCbrEnabled = false
        )
    }
}
