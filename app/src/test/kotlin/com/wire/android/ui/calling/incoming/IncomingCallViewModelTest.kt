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

package com.wire.android.ui.calling.incoming

import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.framework.TestUser
import com.wire.android.notification.CallNotificationManager
import com.wire.android.ui.home.appLock.LockCodeTimeManager
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class IncomingCallViewModelTest {

    class Arrangement {

        @MockK
        lateinit var rejectCall: RejectCallUseCase

        @MockK
        lateinit var incomingCalls: GetIncomingCallsUseCase

        @MockK
        lateinit var acceptCall: AnswerCallUseCase

        @MockK
        lateinit var observeEstablishedCalls: ObserveEstablishedCallsUseCase

        @MockK
        lateinit var endCall: EndCallUseCase

        @MockK
        lateinit var muteCall: MuteCallUseCase

        @MockK
        lateinit var lockCodeTimeManager: LockCodeTimeManager

        @MockK
        lateinit var callNotificationManager: CallNotificationManager

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)

            // Default empty values
            coEvery { rejectCall(any()) } returns Unit
            coEvery { acceptCall(any()) } returns Unit
            coEvery { incomingCalls.invoke() } returns flowOf(listOf(provideCall()))
            coEvery { observeEstablishedCalls.invoke() } returns flowOf(emptyList())
            coEvery { muteCall(any(), any()) } returns Unit
        }

        fun withAppNotLocked() = apply {
            every { lockCodeTimeManager.observeAppLock() } returns flowOf(false)
        }

        fun withAppLocked() = apply {
            every { lockCodeTimeManager.observeAppLock() } returns flowOf(true)
        }

        fun withLockStateLockedAndThenUnlocked() = apply {
            every { lockCodeTimeManager.observeAppLock() } returns flowOf(true) andThen flowOf(false)
        }

        fun withEstablishedCalls(flow: Flow<List<Call>>) = apply {
            coEvery { observeEstablishedCalls.invoke() } returns flow
        }

        fun withEndCall(action: suspend () -> Unit) = apply {
            coEvery { endCall(any()) } coAnswers { action() }
        }

        fun arrange() = this to IncomingCallViewModel(
            conversationId = dummyConversationId,
            currentAccount = TestUser.SELF_USER_ID,
            incomingCalls = incomingCalls,
            rejectCall = rejectCall,
            acceptCall = acceptCall,
            observeEstablishedCalls = observeEstablishedCalls,
            endCall = endCall,
            muteCall = muteCall,
            lockCodeTimeManager = lockCodeTimeManager,
            callNotificationManager = callNotificationManager,
        )
    }

    @Test
    fun `given app Locked, when the user decline the call, then do not reject the call`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withAppLocked()
            .arrange()

        viewModel.actions.test {
            viewModel.declineCall()

            coVerify(inverse = true) { arrangement.rejectCall(conversationId = any()) }
            assertEquals(IncomingCallViewActions.AppLocked, awaitItem())
        }
    }

    @Test
    fun `given an incoming call, when the user decline the call, then the reject call use case is called`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withAppNotLocked()
            .arrange()

        viewModel.actions.test {
            viewModel.declineCall()

            coVerify(exactly = 1) { arrangement.rejectCall(conversationId = any()) }
            assertTrue { viewModel.incomingCallState.flowState is IncomingCallState.FlowState.CallClosed }
            assertEquals(IncomingCallViewActions.RejectedCall(dummyConversationId), awaitItem())
        }
    }

    @Test
    fun `given app locked, when user tries to accept an incoming call, then do not accept the call`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withAppLocked()
            .arrange()

        viewModel.actions.test {
            viewModel.acceptCall()

            coVerify(inverse = true) { arrangement.acceptCall(conversationId = any()) }
            assertEquals(IncomingCallViewActions.AppLocked, awaitItem())
        }
    }

    @Test
    fun `given no ongoing call, when user tries to accept an incoming call, then invoke answerCall call use case`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withAppNotLocked()
            .arrange()

        viewModel.acceptCall()
        advanceUntilIdle()

        coVerify(exactly = 1) { arrangement.acceptCall(conversationId = any()) }
        coVerify(inverse = true) { arrangement.endCall(any()) }
        assertEquals(false, viewModel.incomingCallState.shouldShowJoinCallAnywayDialog)
        assertTrue { viewModel.incomingCallState.flowState is IncomingCallState.FlowState.CallAccepted }
    }

    @Test
    fun `given an ongoing call, when user tries to accept an incoming call, then show JoinCallAnywayDialog`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withAppNotLocked()
            .withEstablishedCalls(flowOf(listOf(provideCall(ConversationId("value", "Domain")))))
            .arrange()

        viewModel.acceptCall()

        assertTrue { viewModel.incomingCallState.flowState is IncomingCallState.FlowState.Default }
        assertEquals(true, viewModel.incomingCallState.shouldShowJoinCallAnywayDialog)
        coVerify(inverse = true) { arrangement.acceptCall(conversationId = any()) }
    }

    @Test
    fun `given an ongoing call, when user confirms dialog to accept an incoming call, then end current call and accept the newer one`() =
        runTest {
            val establishedCallsChannel = Channel<List<Call>>(capacity = Channel.UNLIMITED)
                .also { it.send(listOf(provideCall(ConversationId("value", "Domain")))) }
            val (arrangement, viewModel) = Arrangement()
                .withAppNotLocked()
                .withEstablishedCalls(establishedCallsChannel.consumeAsFlow())
                .withEndCall { establishedCallsChannel.send(listOf()) }
                .arrange()

            viewModel.acceptCallAnyway()
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.endCall(any()) }
            coVerify(exactly = 1) { arrangement.muteCall(any(), eq(false)) }
            assertTrue { viewModel.incomingCallState.flowState is IncomingCallState.FlowState.CallAccepted }
        }

    @Test
    fun `given join dialog displayed, when user dismisses it, then hide it`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withAppNotLocked()
            .withEstablishedCalls(flowOf(listOf(provideCall())))
            .arrange()

        viewModel.acceptCall()

        viewModel.dismissJoinCallAnywayDialog()

        assertEquals(false, viewModel.incomingCallState.shouldShowJoinCallAnywayDialog)
    }

    @Test
    fun `given app locked, when user tries to accept an incoming call, then do not accept the call until is unlocked`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withLockStateLockedAndThenUnlocked()
            .arrange()

        viewModel.acceptCall()

        coVerify { arrangement.acceptCall(conversationId = any()) }
    }

    @Test
    fun `given app locked, when user tries to accept an second incoming call, then do not accept the call until is unlocked`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withLockStateLockedAndThenUnlocked()
            .arrange()

        viewModel.acceptCallAnyway()

        coVerify { arrangement.acceptCall(conversationId = any()) }
    }

    @Test
    fun `given app Locked, when the user decline the call, then do not reject the call until is unlocked`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withLockStateLockedAndThenUnlocked()
            .arrange()

        viewModel.declineCall()

        coVerify { arrangement.rejectCall(conversationId = any()) }
    }

    @Test
    fun `given call not accepted, when bringing back notification, then bring back notification`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withAppNotLocked()
            .arrange()
        // when
        viewModel.bringBackNotificationIfNeeded()
        // then
        coVerify(exactly = 1) {
            arrangement.callNotificationManager.bringBackIncomingCallNotification(
                TestUser.SELF_USER_ID.toString(),
                dummyConversationId.toString(),
            )
        }
    }

    @Test
    fun `given call being accepted but unlock required, when bringing back notification, then do not bring back notification`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withAppNotLocked()
            .arrange()
        // when
        viewModel.bringBackNotificationIfNeeded()
        // then
        coVerify(exactly = 1) {
            arrangement.callNotificationManager.bringBackIncomingCallNotification(
                TestUser.SELF_USER_ID.toString(),
                dummyConversationId.toString(),
            )
        }
    }

    @Test
    fun `given call accepted, when bringing back notification, then bring back notification`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withAppLocked()
            .arrange()
        viewModel.acceptCall()
        advanceUntilIdle()
        // when
        viewModel.bringBackNotificationIfNeeded()
        // then
        coVerify(exactly = 0) {
            arrangement.callNotificationManager.bringBackIncomingCallNotification(
                dummyConversationId.toString(),
                TestUser.SELF_USER_ID.toString()
            )
        }
    }

    companion object {
        val dummyConversationId = ConversationId("some-dummy-value", "some.dummy.domain")

        private fun provideCall(id: ConversationId = dummyConversationId, status: CallStatus = CallStatus.INCOMING) = Call(
            conversationId = id,
            status = status,
            callerId = UserId("caller", "domain"),
            participants = listOf(),
            isMuted = false,
            isCameraOn = false,
            maxParticipants = 0,
            conversationName = "ONE_ON_ONE Name",
            conversationType = Conversation.Type.OneOnOne,
            callerName = "otherUsername",
            callerTeamName = "team_1",
            isCbrEnabled = false
        )
    }
}
